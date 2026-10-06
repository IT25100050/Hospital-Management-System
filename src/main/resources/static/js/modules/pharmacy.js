import { api } from "../api.js";
import {
  loadingLine, errorBanner, renderTable, panelHead, openFormModal, field,
  toast, confirmAction, fmtMoney, fmtDate, escapeHtml, el, nowForBackend
} from "../ui.js";

let activeTab = "medicines";
let medicinesCache = [];
let patientsCache = [];
let doctorsCache = [];
let prescriptionsCache = [];

export async function renderPharmacy(container) {
  container.innerHTML = `
    <div class="folder-tabs">
      <button data-tab="medicines" class="${activeTab === "medicines" ? "active" : ""}">Medicine Inventory</button>
      <button data-tab="prescriptions" class="${activeTab === "prescriptions" ? "active" : ""}">Prescriptions</button>
    </div>
    <div class="tab-panel" id="pharm-tab-body">${loadingLine()}</div>
  `;
  container.querySelectorAll(".folder-tabs button").forEach(btn =>
    btn.addEventListener("click", () => { activeTab = btn.dataset.tab; renderPharmacy(container); }));

  const body = container.querySelector("#pharm-tab-body");
  if (activeTab === "medicines") await renderMedicines(body);
  else await renderPrescriptions(body);
}

// ---------------------------------------------------------------------------
// Medicines
// ---------------------------------------------------------------------------
async function renderMedicines(body) {
  try {
    const medicines = await api.get("/api/medicines");
    medicinesCache = medicines || [];
    body.innerHTML =
      panelHead("Medicine Inventory", "Stock, pricing, and expiry for every medicine on hand.",
        `<button class="btn btn-primary" id="add-med">+ Add medicine</button>`) +
      renderTable({
        columns: [
          { key: "code", label: "Code", mono: true },
          { key: "name", label: "Name" },
          { key: "category", label: "Category" },
          { key: "stockQuantity", label: "Stock", render: r => stockBadge(r.stockQuantity) },
          { key: "unitPrice", label: "Unit price", render: r => fmtMoney(r.unitPrice) },
          { key: "expiryDate", label: "Expiry" },
        ],
        rows: medicinesCache,
        actions: (row) => `
          <button class="btn btn-ghost btn-sm stock-med" data-id="${row.id}">Adjust stock</button>
          <button class="btn btn-ghost btn-sm edit-med" data-id="${row.id}">Edit</button>
          <button class="btn btn-danger btn-sm del-med" data-id="${row.id}">Delete</button>`,
        emptyMessage: "No medicines in inventory yet.",
      });

    body.querySelector("#add-med").addEventListener("click", () => openMedicineModal());
    body.querySelectorAll(".edit-med").forEach(b =>
      b.addEventListener("click", () => openMedicineModal(medicinesCache.find(m => String(m.id) === b.dataset.id))));
    body.querySelectorAll(".del-med").forEach(b =>
      b.addEventListener("click", () => deleteMedicine(b.dataset.id, body)));
    body.querySelectorAll(".stock-med").forEach(b =>
      b.addEventListener("click", () => openStockModal(medicinesCache.find(m => String(m.id) === b.dataset.id), body)));
  } catch (err) {
    body.innerHTML = errorBanner(err.message);
  }
}

function stockBadge(qty) {
  const n = Number(qty);
  const cls = n <= 10 ? "chip-red" : n <= 30 ? "chip-amber" : "chip-green";
  return `<span class="chip ${cls}">${n}</span>`;
}

function openMedicineModal(existing) {
  openFormModal({
    title: existing ? `Edit ${existing.name}` : "Add medicine",
    submitLabel: existing ? "Save changes" : "Add to inventory",
    fieldsHtml:
      field({ name: "code", label: "Code", required: true, value: existing?.code, hint: "Unique SKU / code" }) +
      field({ name: "name", label: "Name", required: true, value: existing?.name }) +
      field({ name: "category", label: "Category", value: existing?.category }) +
      field({ name: "stockQuantity", label: "Initial stock quantity", type: "number", required: true, value: existing?.stockQuantity ?? 0 }) +
      field({ name: "unitPrice", label: "Unit price (LKR)", type: "number", step: "0.01", required: true, value: existing?.unitPrice }) +
      field({ name: "expiryDate", label: "Expiry date", type: "date", value: existing?.expiryDate }),
    onSubmit: async (fd, close) => {
      const payload = {
        code: fd.get("code"),
        name: fd.get("name"),
        category: fd.get("category"),
        stockQuantity: Number(fd.get("stockQuantity")),
        unitPrice: Number(fd.get("unitPrice")),
        expiryDate: fd.get("expiryDate") || null,
      };
      if (existing) await api.put(`/api/medicines/${existing.id}`, payload);
      else await api.post("/api/medicines", payload);
      toast(existing ? "Medicine updated." : "Medicine added.", "success");
      close();
      await renderPharmacy(document.getElementById("page-content"));
    },
  });
}

function openStockModal(medicine, body) {
  // NOTE: the backend's PATCH /api/medicines/{id}/stock treats "quantity" as a CHANGE
  // (delta) applied to the current stock, not the new absolute total — so this form
  // asks for a +/- adjustment, not a new total.
  openFormModal({
    title: `Adjust stock — ${medicine.name}`,
    submitLabel: "Apply adjustment",
    fieldsHtml:
      `<div class="small-note" style="margin-bottom:10px;">Current stock: <strong>${medicine.stockQuantity}</strong></div>` +
      field({
        name: "quantity", label: "Quantity change", type: "number", required: true,
        value: 0, full: true,
        hint: "Positive number to receive new stock (e.g. 50), negative to remove damaged/expired stock (e.g. -5).",
      }),
    onSubmit: async (fd, close) => {
      const delta = Number(fd.get("quantity"));
      await api.patch(`/api/medicines/${medicine.id}/stock`, undefined, { quantity: delta });
      toast("Stock adjusted.", "success");
      close();
      await renderMedicines(body);
    },
  });
}

async function deleteMedicine(id, body) {
  if (!confirmAction("Remove this medicine from inventory?")) return;
  try {
    await api.del(`/api/medicines/${id}`);
    toast("Medicine removed.", "success");
    await renderMedicines(body);
  } catch (err) {
    toast(err.message, "error");
  }
}

// ---------------------------------------------------------------------------
// Prescriptions
// ---------------------------------------------------------------------------
async function renderPrescriptions(body) {
  try {
    const [prescriptions, patients, doctors, medicines] = await Promise.all([
      api.get("/api/prescriptions"), api.get("/api/patients"), api.get("/api/doctors"), api.get("/api/medicines"),
    ]);
    prescriptionsCache = prescriptions || [];
    patientsCache = patients || [];
    doctorsCache = doctors || [];
    medicinesCache = medicines || [];

    body.innerHTML =
      panelHead("Prescriptions", "Medicines prescribed to patients by doctors.",
        `<button class="btn btn-primary" id="add-rx">+ New prescription</button>`) +
      renderTable({
        columns: [
          { key: "id", label: "ID", mono: true },
          { key: "patientId", label: "Patient ID", mono: true },
          { key: "doctorId", label: "Doctor ID", mono: true },
          { key: "items", label: "Items", render: r => (r.items || []).map(i => `${escapeHtml(i.medicineName || ("#" + i.medicineId))} ×${i.quantity}`).join(", ") || "—" },
          { key: "prescribedDate", label: "Date", render: r => fmtDate(r.prescribedDate) },
        ],
        rows: prescriptionsCache,
        actions: (row) => `
          <button class="btn btn-ghost btn-sm edit-rx" data-id="${row.id}">Edit</button>
          <button class="btn btn-ghost btn-sm fulfill-rx" data-id="${row.id}">Fulfill</button>
          <button class="btn btn-danger btn-sm del-rx" data-id="${row.id}">Delete</button>`,
        emptyMessage: "No prescriptions yet.",
      });

    body.querySelector("#add-rx").addEventListener("click", () => openPrescriptionModal());
    body.querySelectorAll(".edit-rx").forEach(b =>
      b.addEventListener("click", () => openPrescriptionModal(prescriptionsCache.find(r => String(r.id) === b.dataset.id))));
    body.querySelectorAll(".fulfill-rx").forEach(b =>
      b.addEventListener("click", () => fulfillPrescription(b.dataset.id, body)));
    body.querySelectorAll(".del-rx").forEach(b =>
      b.addEventListener("click", () => deletePrescription(b.dataset.id, body)));
  } catch (err) {
    body.innerHTML = errorBanner(err.message);
  }
}

function openPrescriptionModal(existing) {
  if (!patientsCache.length || !doctorsCache.length || !medicinesCache.length) {
    toast("You need at least one patient, one doctor, and one medicine before writing a prescription.", "error");
    return;
  }

  const overlay = el(`
    <div class="modal-overlay">
      <div class="modal-box">
        <div class="modal-head"><h3>${existing ? `Edit prescription #${existing.id}` : "New prescription"}</h3><button class="modal-close">&times;</button></div>
        <form class="modal-form">
          <div class="modal-body">
            <div class="form-grid">
              ${field({ name: "patientId", label: "Patient", type: "select", required: true, value: existing?.patientId, options: patientsCache.map(p => ({ value: p.id, label: `${p.name} (#${p.id})` })) })}
              ${field({ name: "doctorId", label: "Doctor", type: "select", required: true, value: existing?.doctorId, options: doctorsCache.map(d => ({ value: d.id, label: `Dr. ${d.name} (#${d.id})` })) })}
              ${field({ name: "notes", label: "Notes", type: "textarea", full: true, value: existing?.notes })}
            </div>
            <h4 class="section-title" style="margin-top:18px;">Items</h4>
            <div id="rx-items"></div>
            <button type="button" class="btn btn-ghost btn-sm" id="add-item" style="margin-top:8px;">+ Add medicine</button>
          </div>
          <div class="modal-foot">
            <button type="button" class="btn btn-ghost cancel-btn">Cancel</button>
            <button type="submit" class="btn btn-primary">${existing ? "Save changes" : "Create prescription"}</button>
          </div>
        </form>
      </div>
    </div>
  `);
  document.body.appendChild(overlay);
  const close = () => overlay.remove();
  overlay.querySelector(".modal-close").addEventListener("click", close);
  overlay.querySelector(".cancel-btn").addEventListener("click", close);
  overlay.addEventListener("click", (e) => { if (e.target === overlay) close(); });

  const itemsWrap = overlay.querySelector("#rx-items");
  const medOptionsHtml = (selectedId) => medicinesCache.map(m =>
    `<option value="${m.id}" ${String(m.id) === String(selectedId) ? "selected" : ""}>${escapeHtml(m.name)} (${escapeHtml(m.code)})</option>`).join("");

  function addItemRow(item) {
    const row = el(`
      <div class="form-grid rx-item-row" style="margin-bottom:10px; align-items:end;">
        <div class="field"><label>Medicine</label><select name="medicineId">${medOptionsHtml(item?.medicineId)}</select></div>
        <div class="field"><label>Quantity</label><input type="number" name="quantity" min="1" value="${item?.quantity ?? 1}" required /></div>
        <div class="field"><label>Dosage</label><input type="text" name="dosage" placeholder="e.g. 1 tab twice daily" value="${escapeHtml(item?.dosage || "")}" /></div>
        <div class="field"><button type="button" class="btn btn-danger btn-sm remove-item">Remove</button></div>
      </div>`);
    row.querySelector(".remove-item").addEventListener("click", () => row.remove());
    itemsWrap.appendChild(row);
  }

  if (existing && existing.items && existing.items.length) existing.items.forEach(addItemRow);
  else addItemRow();
  overlay.querySelector("#add-item").addEventListener("click", () => addItemRow());

  overlay.querySelector(".modal-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const items = Array.from(itemsWrap.querySelectorAll(".rx-item-row")).map(row => ({
      medicineId: Number(row.querySelector("[name=medicineId]").value),
      quantity: Number(row.querySelector("[name=quantity]").value),
      dosage: row.querySelector("[name=dosage]").value,
    }));
    if (!items.length) { toast("Add at least one medicine.", "error"); return; }
    const submitBtn = e.target.querySelector("button[type=submit]");
    submitBtn.disabled = true;
    try {
      if (existing) {
        await api.put(`/api/prescriptions/${existing.id}`, {
          patientId: Number(fd.get("patientId")),
          doctorId: Number(fd.get("doctorId")),
          notes: fd.get("notes"),
          items,
        });
        toast("Prescription updated.", "success");
      } else {
        await api.post("/api/prescriptions", {
          patientId: Number(fd.get("patientId")),
          doctorId: Number(fd.get("doctorId")),
          notes: fd.get("notes"),
          prescribedDate: nowForBackend(),
          items,
        });
        toast("Prescription created.", "success");
      }
      close();
      await renderPharmacy(document.getElementById("page-content"));
    } catch (err) {
      toast(err.message, "error");
    } finally {
      submitBtn.disabled = false;
    }
  });
}

async function fulfillPrescription(id, body) {
  if (!confirmAction("Mark this prescription as fulfilled? This will deduct the prescribed quantities from medicine stock.")) return;
  try {
    await api.patch(`/api/prescriptions/${id}/fulfill`);
    toast("Prescription fulfilled — stock updated.", "success");
    await renderPrescriptions(body);
  } catch (err) {
    toast(err.message, "error");
  }
}

async function deletePrescription(id, body) {
  if (!confirmAction("Delete this prescription?")) return;
  try {
    await api.del(`/api/prescriptions/${id}`);
    toast("Prescription deleted.", "success");
    await renderPrescriptions(body);
  } catch (err) {
    toast(err.message, "error");
  }
}
