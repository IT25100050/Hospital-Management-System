import { api } from "../api.js";
import {
  loadingLine, errorBanner, renderTable, panelHead, openFormModal, field,
  toast, confirmAction, fmtDate, escapeHtml, nowForBackend
} from "../ui.js";

let activeTab = "doctors";
let doctorsCache = [];
let departmentsCache = [];
let patientsCache = [];
let recordsCache = [];

export async function renderDoctors(container) {
  container.innerHTML = `
    <div class="folder-tabs">
      <button data-tab="doctors" class="${activeTab === "doctors" ? "active" : ""}">Doctors</button>
      <button data-tab="records" class="${activeTab === "records" ? "active" : ""}">Medical Records</button>
    </div>
    <div class="tab-panel" id="doc-tab-body">${loadingLine()}</div>
  `;
  container.querySelectorAll(".folder-tabs button").forEach(btn =>
    btn.addEventListener("click", () => { activeTab = btn.dataset.tab; renderDoctors(container); }));

  const body = container.querySelector("#doc-tab-body");
  if (activeTab === "doctors") await renderDoctorList(body);
  else await renderRecords(body);
}

// ---------------------------------------------------------------------------
// Doctors
// ---------------------------------------------------------------------------
async function renderDoctorList(body) {
  try {
    const [doctors, departments] = await Promise.all([
      api.get("/api/doctors"), api.get("/api/admin/departments"),
    ]);
    doctorsCache = doctors || [];
    departmentsCache = departments || [];

    body.innerHTML =
      panelHead("Doctors", "Clinical staff who see patients and write records.",
        `<button class="btn btn-primary" id="add-doc">+ New doctor</button>`) +
      renderTable({
        columns: [
          { key: "id", label: "ID", mono: true },
          { key: "name", label: "Name", render: r => `Dr. ${escapeHtml(r.name)}` },
          { key: "specialization", label: "Specialization" },
          { key: "departmentName", label: "Department" },
          { key: "email", label: "Email" },
          { key: "phone", label: "Phone" },
        ],
        rows: doctorsCache,
        actions: (row) => `
          <button class="btn btn-ghost btn-sm edit-doc" data-id="${row.id}">Edit</button>
          <button class="btn btn-danger btn-sm del-doc" data-id="${row.id}">Delete</button>`,
        emptyMessage: "No doctors yet — add the first one.",
      });

    body.querySelector("#add-doc").addEventListener("click", () => openDoctorModal());
    body.querySelectorAll(".edit-doc").forEach(b =>
      b.addEventListener("click", () => openDoctorModal(doctorsCache.find(d => String(d.id) === b.dataset.id))));
    body.querySelectorAll(".del-doc").forEach(b =>
      b.addEventListener("click", () => deleteDoctor(b.dataset.id, body)));
  } catch (err) {
    body.innerHTML = errorBanner(err.message);
  }
}

function openDoctorModal(existing) {
  openFormModal({
    title: existing ? `Edit Dr. ${existing.name}` : "New doctor",
    submitLabel: existing ? "Save changes" : "Create doctor profile",
    fieldsHtml:
      field({ name: "name", label: "Full name", required: true, value: existing?.name }) +
      field({ name: "specialization", label: "Specialization", required: true, value: existing?.specialization }) +
      field({ name: "email", label: "Email", type: "email", required: true, value: existing?.email }) +
      field({ name: "phone", label: "Phone", value: existing?.phone }) +
      field({
        name: "departmentId", label: "Department", type: "select", full: true,
        value: existing?.departmentId,
        options: departmentsCache.map(d => ({ value: d.id, label: d.name })),
      }),
    onSubmit: async (fd, close) => {
      const payload = {
        name: fd.get("name"),
        specialization: fd.get("specialization"),
        email: fd.get("email"),
        phone: fd.get("phone"),
        departmentId: fd.get("departmentId") ? Number(fd.get("departmentId")) : null,
      };
      if (existing) await api.put(`/api/doctors/${existing.id}`, payload);
      else await api.post("/api/doctors", payload);
      toast(existing ? "Doctor profile updated." : "Doctor profile created.", "success");
      close();
      await renderDoctors(document.getElementById("page-content"));
    },
  });
}

async function deleteDoctor(id, body) {
  if (!confirmAction("Delete this doctor's profile?")) return;
  try {
    await api.del(`/api/doctors/${id}`);
    toast("Doctor deleted.", "success");
    await renderDoctorList(body);
  } catch (err) {
    toast(err.message, "error");
  }
}

// ---------------------------------------------------------------------------
// Medical records
// ---------------------------------------------------------------------------
async function renderRecords(body) {
  try {
    const [patients, doctors, records] = await Promise.all([
      api.get("/api/patients"), doctorsCache.length ? doctorsCache : api.get("/api/doctors"),
      api.get("/api/medical-records"),
    ]);
    patientsCache = patients || [];
    doctorsCache = doctors || [];
    recordsCache = records || [];

    body.innerHTML =
      panelHead("Medical Records", "Diagnoses and treatment notes written by doctors for patients.",
        `<button class="btn btn-primary" id="add-record">+ New medical record</button>`) +
      `<div class="two-col" style="margin-top:4px;">
        <form id="lookup-by-patient" class="form-grid">
          ${field({ name: "patientId", label: "Records for patient", type: "select", options: patientsCache.map(p => ({ value: p.id, label: `${p.name} (#${p.id})` })) })}
          <div class="field"><button type="submit" class="btn btn-ghost" style="margin-top:20px;">View</button></div>
        </form>
        <form id="lookup-by-doctor" class="form-grid">
          ${field({ name: "doctorId", label: "Records by doctor", type: "select", options: doctorsCache.map(d => ({ value: d.id, label: `Dr. ${d.name} (#${d.id})` })) })}
          <div class="field"><button type="submit" class="btn btn-ghost" style="margin-top:20px;">View</button></div>
        </form>
      </div>
      <div style="margin-top:18px; display:flex; align-items:center; justify-content:space-between;">
        <h4 class="section-title" style="margin:0;">All records</h4>
        <button class="btn btn-ghost btn-sm" id="show-all-records">Show all</button>
      </div>
      <div id="records-result" style="margin-top:10px;">${renderRecordsTable(recordsCache)}</div>`;

    attachRecordRowHandlers(body);

    body.querySelector("#add-record").addEventListener("click", () => openRecordModal());

    body.querySelector("#show-all-records").addEventListener("click", async () => {
      const out = body.querySelector("#records-result");
      out.innerHTML = loadingLine();
      try {
        recordsCache = await api.get("/api/medical-records");
        out.innerHTML = renderRecordsTable(recordsCache);
        attachRecordRowHandlers(body);
      } catch (err) { out.innerHTML = errorBanner(err.message); }
    });

    body.querySelector("#lookup-by-patient").addEventListener("submit", async (e) => {
      e.preventDefault();
      const pid = new FormData(e.target).get("patientId");
      const out = body.querySelector("#records-result");
      out.innerHTML = loadingLine();
      try {
        recordsCache = await api.get(`/api/medical-records/patient/${pid}`);
        out.innerHTML = renderRecordsTable(recordsCache);
        attachRecordRowHandlers(body);
      } catch (err) { out.innerHTML = errorBanner(err.message); }
    });

    body.querySelector("#lookup-by-doctor").addEventListener("submit", async (e) => {
      e.preventDefault();
      const did = new FormData(e.target).get("doctorId");
      const out = body.querySelector("#records-result");
      out.innerHTML = loadingLine();
      try {
        recordsCache = await api.get(`/api/medical-records/doctor/${did}`);
        out.innerHTML = renderRecordsTable(recordsCache);
        attachRecordRowHandlers(body);
      } catch (err) { out.innerHTML = errorBanner(err.message); }
    });
  } catch (err) {
    body.innerHTML = errorBanner(err.message);
  }
}

function renderRecordsTable(records) {
  return renderTable({
    columns: [
      { key: "id", label: "ID", mono: true },
      { key: "patientName", label: "Patient" },
      { key: "doctorName", label: "Doctor" },
      { key: "diagnosis", label: "Diagnosis" },
      { key: "treatment", label: "Treatment" },
      { key: "recordDate", label: "Date", render: r => fmtDate(r.recordDate) },
    ],
    rows: records,
    actions: (row) => `
      <button class="btn btn-ghost btn-sm edit-record" data-id="${row.id}">Edit</button>
      <button class="btn btn-danger btn-sm del-record" data-id="${row.id}">Delete</button>`,
    emptyMessage: "No medical records found.",
  });
}

function attachRecordRowHandlers(body) {
  body.querySelectorAll(".edit-record").forEach(b =>
    b.addEventListener("click", () => openRecordModal(recordsCache.find(r => String(r.id) === b.dataset.id))));
  body.querySelectorAll(".del-record").forEach(b =>
    b.addEventListener("click", () => deleteRecord(b.dataset.id, body)));
}

function openRecordModal(existing) {
  if (!existing && (!patientsCache.length || !doctorsCache.length)) {
    toast("You need at least one patient and one doctor before writing a record.", "error");
    return;
  }
  openFormModal({
    title: existing ? `Edit medical record #${existing.id}` : "New medical record",
    submitLabel: existing ? "Save changes" : "Save record",
    fieldsHtml:
      field({ name: "patientId", label: "Patient", type: "select", required: true, value: existing?.patientId, options: patientsCache.map(p => ({ value: p.id, label: `${p.name} (#${p.id})` })) }) +
      field({ name: "doctorId", label: "Doctor", type: "select", required: true, value: existing?.doctorId, options: doctorsCache.map(d => ({ value: d.id, label: `Dr. ${d.name} (#${d.id})` })) }) +
      field({ name: "diagnosis", label: "Diagnosis", type: "textarea", required: true, full: true, value: existing?.diagnosis }) +
      field({ name: "treatment", label: "Treatment", type: "textarea", full: true, value: existing?.treatment }) +
      field({ name: "notes", label: "Notes", type: "textarea", full: true, value: existing?.notes }),
    onSubmit: async (fd, close) => {
      const payload = {
        patientId: Number(fd.get("patientId")),
        doctorId: Number(fd.get("doctorId")),
        diagnosis: fd.get("diagnosis"),
        treatment: fd.get("treatment"),
        notes: fd.get("notes"),
      };
      if (existing) {
        await api.put(`/api/medical-records/${existing.id}`, payload);
      } else {
        payload.recordDate = nowForBackend();
        await api.post("/api/medical-records", payload);
      }
      toast(existing ? "Medical record updated." : "Medical record saved.", "success");
      close();
      await renderDoctors(document.getElementById("page-content"));
    },
  });
}

async function deleteRecord(id, body) {
  if (!confirmAction("Delete this medical record? This cannot be undone.")) return;
  try {
    await api.del(`/api/medical-records/${id}`);
    toast("Medical record deleted.", "success");
    await renderRecords(body);
  } catch (err) {
    toast(err.message, "error");
  }
}
