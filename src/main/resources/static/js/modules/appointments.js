import { api, getRole } from "../api.js";
import {
  loadingLine, errorBanner, renderTable, panelHead, openFormModal, field,
  toast, confirmAction, fmtDate, statusChip, toLocalDateTimeInputValue
} from "../ui.js";

// Mirrors the backend's SecurityConfig rules for these endpoints —
// keep both in sync if permissions ever change.
const CAN_BOOK_APPOINTMENT = ["PATIENT", "RECEPTIONIST", "ADMIN"];
const CAN_MANAGE_APPOINTMENT = ["RECEPTIONIST", "ADMIN"]; // status change / cancel
const CAN_REGISTER_PATIENT = ["RECEPTIONIST", "ADMIN"];
let activeTab = "appointments";
let patientsCache = [];
let doctorsCache = [];

export async function renderAppointments(container) {
  container.innerHTML = `
    <div class="folder-tabs">
      <button data-tab="appointments" class="${activeTab === "appointments" ? "active" : ""}">Appointments</button>
      <button data-tab="patients" class="${activeTab === "patients" ? "active" : ""}">Patients</button>
    </div>
    <div class="tab-panel" id="appt-tab-body">${loadingLine()}</div>
  `;
  container.querySelectorAll(".folder-tabs button").forEach(btn =>
    btn.addEventListener("click", () => { activeTab = btn.dataset.tab; renderAppointments(container); }));

  const body = container.querySelector("#appt-tab-body");
  if (activeTab === "appointments") await renderAppts(body);
  else await renderPatients(body);
}

// ---------------------------------------------------------------------------
// Appointments
// ---------------------------------------------------------------------------
async function renderAppts(body) {
  try {
    const [appts, patients, doctors] = await Promise.all([
      api.get("/api/appointments"), api.get("/api/patients"), api.get("/api/doctors"),
    ]);
    patientsCache = patients || [];
    doctorsCache = doctors || [];

    const role = getRole();
    const canManage = CAN_MANAGE_APPOINTMENT.includes(role);

    body.innerHTML =
      panelHead("Appointments", "Every booking across all doctors, newest first.",
        CAN_BOOK_APPOINTMENT.includes(role) ? `<button class="btn btn-primary" id="add-appt">+ Book appointment</button>` : "") +
      renderTable({
        columns: [
          { key: "id", label: "ID", mono: true },
          { key: "patientName", label: "Patient" },
          { key: "doctorId", label: "Doctor ID", mono: true },
          { key: "appointmentTime", label: "When", render: r => fmtDate(r.appointmentTime) },
          { key: "status", label: "Status", render: r => statusChip(r.status) },
        ],
        rows: [...(appts || [])].sort((a, b) => new Date(b.appointmentTime) - new Date(a.appointmentTime)),
        actions: canManage ? (row) => `
          ${row.status === "PENDING" ? `<button class="btn btn-ghost btn-sm edit-appt" data-id="${row.id}">Edit</button>` : ""}
          ${["PENDING", "APPROVED"].includes(row.status) ? `<button class="btn btn-danger btn-sm cancel-appt" data-id="${row.id}">Cancel</button>` : ""}` : undefined,
        emptyMessage: "No appointments booked yet.",
      });

    const addBtn = body.querySelector("#add-appt");
    if (addBtn) addBtn.addEventListener("click", () => openApptModal());
    body.querySelectorAll(".edit-appt").forEach(b =>
      b.addEventListener("click", () => {
        const appt = (appts || []).find(a => String(a.id) === b.dataset.id);
        if (appt) openApptModal(appt);
      }));
    body.querySelectorAll(".cancel-appt").forEach(b =>
      b.addEventListener("click", async () => {
        if (!confirmAction("Cancel this appointment?")) return;
        try {
          await api.del(`/api/appointments/${b.dataset.id}`);
          toast("Appointment cancelled.", "success");
          await renderAppts(body);
        } catch (err) { toast(err.message, "error"); }
      }));
  } catch (err) {
    body.innerHTML = errorBanner(err.message);
  }
}

function openApptModal(existing) {
  if (!patientsCache.length || !doctorsCache.length) {
    toast("Register a patient and add a doctor before booking an appointment.", "error");
    return;
  }
  const defaultTime = existing ? new Date(existing.appointmentTime) : new Date(Date.now() + 60 * 60 * 1000);
  openFormModal({
    title: existing ? `Edit appointment #${existing.id}` : "Book an appointment",
    submitLabel: existing ? "Save changes" : "Book appointment",
    fieldsHtml:
      field({ name: "patientId", label: "Patient", type: "select", required: true, full: true, value: existing?.patientId, options: patientsCache.map(p => ({ value: p.id, label: `${p.name} (#${p.id})` })) }) +
      field({ name: "doctorId", label: "Doctor", type: "select", required: true, full: true, value: existing?.doctorId, options: doctorsCache.map(d => ({ value: d.id, label: `Dr. ${d.name} — ${d.specialization}` })) }) +
      field({ name: "appointmentTime", label: "Date & time", type: "datetime-local", required: true, full: true, value: toLocalDateTimeInputValue(defaultTime), hint: "Must be in the future." }) +
      field({ name: "reason", label: "Reason for visit", type: "textarea", required: true, full: true, value: existing?.reason || "" }),
    onSubmit: async (fd, close) => {
      const payload = {
        patientId: Number(fd.get("patientId")),
        doctorId: Number(fd.get("doctorId")),
        appointmentTime: fd.get("appointmentTime"),
        reason: fd.get("reason"),
      };
      if (existing) await api.put(`/api/appointments/${existing.id}`, payload);
      else await api.post("/api/appointments", payload);
      toast(existing ? "Appointment updated." : "Appointment booked.", "success");
      close();
      await renderAppointments(document.getElementById("page-content"));
    },
  });
}

// ---------------------------------------------------------------------------
// Patients
// ---------------------------------------------------------------------------
async function renderPatients(body) {
  try {
    const patients = await api.get("/api/patients");
    patientsCache = patients || [];
    const canManage = CAN_REGISTER_PATIENT.includes(getRole());
    body.innerHTML =
      panelHead("Patients", "The hospital's patient registry.",
        canManage ? `<button class="btn btn-primary" id="add-patient">+ Register patient</button>` : "") +
      renderTable({
        columns: [
          { key: "id", label: "ID", mono: true },
          { key: "name", label: "Name" },
          { key: "email", label: "Email" },
          { key: "phoneNumber", label: "Phone" },
        ],
        rows: patientsCache,
        actions: canManage ? (row) => `
          <button class="btn btn-ghost btn-sm edit-patient" data-id="${row.id}">Edit</button>
          <button class="btn btn-danger btn-sm del-patient" data-id="${row.id}">Delete</button>` : undefined,
        emptyMessage: "No patients registered yet.",
      });

    const addBtn = body.querySelector("#add-patient");
    if (addBtn) addBtn.addEventListener("click", () => openPatientModal());
    body.querySelectorAll(".edit-patient").forEach(b =>
      b.addEventListener("click", () => openPatientModal(patientsCache.find(p => String(p.id) === b.dataset.id))));
    body.querySelectorAll(".del-patient").forEach(b =>
      b.addEventListener("click", () => deletePatient(b.dataset.id, body)));
  } catch (err) {
    body.innerHTML = errorBanner(err.message);
  }
}

function openPatientModal(existing) {
  openFormModal({
    title: existing ? `Edit ${existing.name}` : "Register patient",
    submitLabel: existing ? "Save changes" : "Register",
    fieldsHtml:
      field({ name: "name", label: "Full name", required: true, full: true, value: existing?.name }) +
      field({ name: "email", label: "Email", type: "email", required: true, value: existing?.email }) +
      field({ name: "phoneNumber", label: "Phone number", required: true, hint: "10 digits", value: existing?.phoneNumber }),
    onSubmit: async (fd, close) => {
      const payload = {
        name: fd.get("name"),
        email: fd.get("email"),
        phoneNumber: fd.get("phoneNumber"),
      };
      if (existing) await api.put(`/api/patients/${existing.id}`, payload);
      else await api.post("/api/patients", payload);
      toast(existing ? "Patient updated." : "Patient registered.", "success");
      close();
      await renderAppointments(document.getElementById("page-content"));
    },
  });
}

async function deletePatient(id, body) {
  if (!confirmAction("Delete this patient? This cannot be undone.")) return;
  try {
    await api.del(`/api/patients/${id}`);
    toast("Patient deleted.", "success");
    await renderPatients(body);
  } catch (err) {
    toast(err.message, "error");
  }
}
