import { api } from "../api.js";
import {
  loadingLine, errorBanner, renderTable, panelHead, openFormModal, field,
  toast, confirmAction, fmtMoney, fmtDate, statusChip, escapeHtml
} from "../ui.js";

const TEST_STATUSES = ["ORDERED", "IN_PROGRESS", "COMPLETED"];
let activeTab = "tests";
let patientsCache = [];
let testsCache = [];

export async function renderLaboratory(container) {
  container.innerHTML = `
    <div class="folder-tabs">
      <button data-tab="tests" class="${activeTab === "tests" ? "active" : ""}">Lab Tests</button>
      <button data-tab="reports" class="${activeTab === "reports" ? "active" : ""}">Reports</button>
    </div>
    <div class="tab-panel" id="lab-tab-body">${loadingLine()}</div>
  `;
  container.querySelectorAll(".folder-tabs button").forEach(btn =>
    btn.addEventListener("click", () => { activeTab = btn.dataset.tab; renderLaboratory(container); }));

  const body = container.querySelector("#lab-tab-body");
  if (activeTab === "tests") await renderTests(body);
  else await renderReports(body);
}

async function ensurePatients() {
  if (!patientsCache.length) patientsCache = (await api.get("/api/patients")) || [];
  return patientsCache;
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------
async function renderTests(body) {
  try {
    await ensurePatients();
    const tests = await api.get("/api/lab/tests");
    testsCache = tests || [];
    body.innerHTML =
      panelHead("Lab Test Orders", "Tests ordered for patients and their current status.",
        `<button class="btn btn-primary" id="add-test">+ Order test</button>`) +
      renderTable({
        columns: [
          { key: "id", label: "ID", mono: true },
          { key: "testName", label: "Test" },
          { key: "patientName", label: "Patient" },
          { key: "price", label: "Price", render: r => fmtMoney(r.price) },
          { key: "status", label: "Status", render: r => statusChip(r.status) },
        ],
        rows: testsCache,
        actions: (row) => `
          <select class="btn-sm status-select" data-id="${row.id}" style="padding:5px 8px; font-size:12px;">
            ${TEST_STATUSES.map(s => `<option value="${s}" ${s === row.status ? "selected" : ""}>${s.replace("_", " ")}</option>`).join("")}
          </select>
          <button class="btn btn-ghost btn-sm edit-test" data-id="${row.id}">Edit</button>
          <button class="btn btn-danger btn-sm del-test" data-id="${row.id}">Delete</button>`,
        emptyMessage: "No lab tests ordered yet.",
      });

    body.querySelector("#add-test").addEventListener("click", () => openTestModal());
    body.querySelectorAll(".edit-test").forEach(b =>
      b.addEventListener("click", () => openTestModal(testsCache.find(t => String(t.id) === b.dataset.id))));
    body.querySelectorAll(".del-test").forEach(b =>
      b.addEventListener("click", () => deleteTest(b.dataset.id, body)));
    body.querySelectorAll(".status-select").forEach(sel => {
      sel.addEventListener("change", async () => {
        try {
          await api.patch(`/api/lab/tests/${sel.dataset.id}/status`, undefined, { status: sel.value });
          toast("Test status updated.", "success");
          await renderTests(body);
        } catch (err) {
          toast(err.message, "error");
        }
      });
    });
  } catch (err) {
    body.innerHTML = errorBanner(err.message);
  }
}

function openTestModal(existing) {
  if (!existing && !patientsCache.length) {
    toast("Register a patient first (Appointments tab) before ordering a test.", "error");
    return;
  }
  openFormModal({
    title: existing ? `Edit test #${existing.id}` : "Order a lab test",
    submitLabel: existing ? "Save changes" : "Order test",
    fieldsHtml:
      field({ name: "testName", label: "Test name", required: true, full: true, value: existing?.testName, hint: "e.g. Complete Blood Count" }) +
      field({ name: "description", label: "Description", type: "textarea", full: true, value: existing?.description }) +
      field({ name: "price", label: "Price (LKR)", type: "number", step: "0.01", required: true, value: existing?.price }) +
      field({
        name: "patientId", label: "Patient", type: "select", required: true, value: existing?.patientId,
        options: patientsCache.map(p => ({ value: p.id, label: `${p.name} (#${p.id})` })),
      }),
    onSubmit: async (fd, close) => {
      const payload = {
        testName: fd.get("testName"),
        description: fd.get("description"),
        price: Number(fd.get("price")),
        patientId: Number(fd.get("patientId")),
      };
      if (existing) {
        await api.put(`/api/lab/tests/${existing.id}`, payload);
        toast("Lab test updated.", "success");
      } else {
        payload.status = "ORDERED";
        await api.post("/api/lab/tests", payload);
        toast("Lab test ordered.", "success");
      }
      close();
      await renderLaboratory(document.getElementById("page-content"));
    },
  });
}

async function deleteTest(id, body) {
  if (!confirmAction("Delete this lab test? Its report (if any) will be deleted too.")) return;
  try {
    await api.del(`/api/lab/tests/${id}`);
    toast("Lab test deleted.", "success");
    await renderTests(body);
  } catch (err) {
    toast(err.message, "error");
  }
}

// ---------------------------------------------------------------------------
// Reports
// ---------------------------------------------------------------------------
async function renderReports(body) {
  try {
    const tests = testsCache.length ? testsCache : await api.get("/api/lab/tests");
    testsCache = tests || [];

    body.innerHTML =
      panelHead("Lab Reports", "Generate a report against an ordered test, then look it up here to view, edit, or delete it.") +
      `<div class="two-col">
        <div>
          <h4 class="section-title">Generate report</h4>
          <form id="report-form" class="form-grid">
            ${field({
              name: "testId", label: "Lab test", type: "select", required: true, full: true,
              options: testsCache.map(t => ({ value: t.id, label: `#${t.id} — ${t.testName} (${t.patientName})` })),
            })}
            ${field({ name: "resultDetails", label: "Result details", type: "textarea", required: true, full: true })}
            ${field({ name: "remarks", label: "Remarks", type: "textarea", full: true })}
            <div class="field full"><button type="submit" class="btn btn-primary">Generate report</button></div>
          </form>
        </div>
        <div>
          <h4 class="section-title">Look up a report</h4>
          <form id="lookup-form" class="form-grid" style="margin-bottom:14px;">
            ${field({ name: "testId", label: "Lab test ID", type: "number", required: true })}
            <div class="field" style="align-self:end;"><button type="submit" class="btn btn-ghost">Fetch report</button></div>
          </form>
          <div id="report-result" class="small-note">No report loaded yet.</div>
        </div>
      </div>`;

    body.querySelector("#report-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      const fd = new FormData(e.target);
      try {
        await api.post("/api/lab/reports", undefined, {
          testId: fd.get("testId"),
          resultDetails: fd.get("resultDetails"),
          remarks: fd.get("remarks") || "",
        });
        toast("Lab report generated.", "success");
        e.target.reset();
      } catch (err) {
        toast(err.message, "error");
      }
    });

    const runLookup = async (testId) => {
      const out = body.querySelector("#report-result");
      out.innerHTML = loadingLine("Fetching report…");
      try {
        const report = await api.get(`/api/lab/reports/test/${testId}`);
        out.innerHTML = `
          <div class="panel" style="padding:14px;">
            <div><strong>${escapeHtml(report.testName)}</strong> — ${escapeHtml(report.patientName)}</div>
            <div class="small-note" style="margin:8px 0;">Generated ${fmtDate(report.generatedDate)}</div>
            <div style="margin-top:8px;"><strong>Result:</strong> ${escapeHtml(report.resultDetails)}</div>
            ${report.remarks ? `<div style="margin-top:6px;"><strong>Remarks:</strong> ${escapeHtml(report.remarks)}</div>` : ""}
            <div style="margin-top:12px;">
              <button class="btn btn-ghost btn-sm" id="edit-report">Edit</button>
              <button class="btn btn-danger btn-sm" id="del-report">Delete</button>
            </div>
          </div>`;
        out.querySelector("#edit-report").addEventListener("click", () => openReportEditModal(report, () => runLookup(testId)));
        out.querySelector("#del-report").addEventListener("click", () => deleteReport(report.id, out));
      } catch (err) {
        out.innerHTML = errorBanner(err.message);
      }
    };

    body.querySelector("#lookup-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      const fd = new FormData(e.target);
      await runLookup(fd.get("testId"));
    });
  } catch (err) {
    body.innerHTML = errorBanner(err.message);
  }
}

function openReportEditModal(report, onDone) {
  openFormModal({
    title: `Edit report for test #${report.labTestId}`,
    submitLabel: "Save changes",
    fieldsHtml:
      field({ name: "resultDetails", label: "Result details", type: "textarea", required: true, full: true, value: report.resultDetails }) +
      field({ name: "remarks", label: "Remarks", type: "textarea", full: true, value: report.remarks }),
    onSubmit: async (fd, close) => {
      await api.put(`/api/lab/reports/${report.id}`, undefined, {
        resultDetails: fd.get("resultDetails"),
        remarks: fd.get("remarks") || "",
      });
      toast("Lab report updated.", "success");
      close();
      await onDone();
    },
  });
}

async function deleteReport(reportId, out) {
  if (!confirmAction("Delete this lab report? The test will go back to 'in progress'.")) return;
  try {
    await api.del(`/api/lab/reports/${reportId}`);
    toast("Lab report deleted.", "success");
    out.innerHTML = `<div class="small-note">Report deleted.</div>`;
  } catch (err) {
    toast(err.message, "error");
  }
}
