import { api } from "../api.js";
import {
  errorBanner, field, fmtDate, loadingLine, panelHead, renderTable, statusChip, toast,
  openFormModal
} from "../ui.js";

let patientTab = "book";

export async function renderPatientPortal(container) {
  container.innerHTML = `
    <div class="folder-tabs">
      <button data-tab="book" class="${patientTab === "book" ? "active" : ""}">Book an Appointment</button>
      <button data-tab="mine" class="${patientTab === "mine" ? "active" : ""}">My Appointments</button>
    </div>
    <div id="patient-workflow">${loadingLine()}</div>`;
  container.querySelectorAll(".folder-tabs button").forEach(button =>
    button.addEventListener("click", () => {
      patientTab = button.dataset.tab;
      renderPatientPortal(container);
    }));
  const body = container.querySelector("#patient-workflow");
  if (patientTab === "mine") await renderMyAppointments(body);
  else renderBooking(body);
}

function nextWeekday() {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  while (date.getDay() === 0 || date.getDay() === 6) date.setDate(date.getDate() + 1);
  const pad = value => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function renderBooking(body) {
  const date = nextWeekday();
  body.innerHTML = panelHead(
    "Book an Appointment",
    "Choose an approved doctor and one of their available weekday consultation slots."
  ) + `
    <form id="availability-date" class="form-grid" style="max-width:420px;margin:12px 0 22px;">
      ${field({ name: "date", label: "Consultation date", type: "date", required: true, value: date })}
      <div class="field"><label>&nbsp;</label><button class="btn btn-ghost" type="submit">Show doctors</button></div>
    </form>
    <div id="available-doctors">${loadingLine("Finding approved doctors and open slots…")}</div>`;

  const dateForm = body.querySelector("#availability-date");
  dateForm.querySelector('[name="date"]').min = nextWeekday();
  dateForm.addEventListener("submit", event => {
    event.preventDefault();
    loadAvailableDoctors(body, dateForm.elements.date.value);
  });
  loadAvailableDoctors(body, date);
}

async function loadAvailableDoctors(body, date) {
  const result = body.querySelector("#available-doctors");
  result.innerHTML = loadingLine("Finding approved doctors and open slots…");
  try {
    const doctors = await api.get("/api/doctors/available", { date });
    if (!doctors.length) {
      result.innerHTML = `<div class="empty-state">No approved doctors are currently available. Please try another date.</div>`;
      return;
    }
    result.innerHTML = `
      <form id="appointment-booking">
        <div class="workflow-doctor-grid">
          ${doctors.map(doctor => `
            <section class="workflow-doctor-card">
              <h3>Dr. ${escape(doctor.name)}</h3>
              <p><strong>Specialty:</strong> ${escape(doctor.specialty)}</p>
              <p><strong>Qualifications:</strong> ${escape(doctor.qualifications || "Not provided")}</p>
              <p><strong>Experience:</strong> ${doctor.experience ?? "—"} years</p>
              <fieldset class="slot-list">
                <legend>Available consultation slots</legend>
                ${doctor.availableSlots.length ? doctor.availableSlots.map(slot => `
                  <label class="slot-option">
                    <input type="radio" name="slot" value="${escape(slot)}" data-doctor="${doctor.id}" required />
                    <span>${formatSlot(slot)}</span>
                  </label>`).join("") : `<p class="empty-state">No available slots on this date.</p>`}
              </fieldset>
            </section>`).join("")}
        </div>
        <div class="field" style="max-width:720px;margin-top:18px;">
          <label for="visit-reason">Reason for visit</label>
          <textarea id="visit-reason" name="reason" maxlength="1000" required></textarea>
        </div>
        <button class="btn btn-primary" type="submit">Request appointment</button>
      </form>`;
    const bookingForm = result.querySelector("#appointment-booking");
    bookingForm.addEventListener("submit", async event => {
      event.preventDefault();
      const slot = bookingForm.querySelector('input[name="slot"]:checked');
      if (!slot) return;
      const submit = bookingForm.querySelector('button[type="submit"]');
      submit.disabled = true;
      submit.textContent = "Booking…";
      try {
        await api.post("/api/appointments", {
          doctorId: Number(slot.dataset.doctor),
          appointmentTime: slot.value,
          reason: bookingForm.elements.reason.value.trim(),
        });
        toast("Appointment requested. Your booking is PENDING doctor approval.", "success");
        patientTab = "mine";
        await renderPatientPortal(document.getElementById("page-content"));
      } catch (error) {
        toast(error.message, "error");
        submit.disabled = false;
        submit.textContent = "Request appointment";
      }
    });
  } catch (error) {
    result.innerHTML = errorBanner(error.message);
  }
}

async function renderMyAppointments(body) {
  body.innerHTML = loadingLine("Loading your appointments…");
  try {
    const appointments = await api.get("/api/appointments/mine");
    body.innerHTML = panelHead("My Appointments", "Your appointment requests and their latest status.") +
      renderTable({
        columns: [
          { key: "doctorName", label: "Doctor", render: row => `Dr. ${escape(row.doctorName || "—")}` },
          { key: "doctorSpecialty", label: "Specialty" },
          { key: "appointmentTime", label: "Date & time", render: row => fmtDate(row.appointmentTime) },
          { key: "reason", label: "Reason" },
          { key: "status", label: "Status", render: row => statusChip(row.status) },
          { key: "rejectionReason", label: "Decision / reason", render: row => escape(row.rejectionReason || (row.decisionTime ? `Reviewed ${fmtDate(row.decisionTime)}` : "Awaiting doctor review")) },
        ],
        rows: appointments,
        actions: row => row.status === "PENDING" || row.status === "APPROVED"
          ? `<button class="btn btn-danger btn-sm cancel-booking" data-id="${row.id}">Cancel</button>` : "",
        emptyMessage: "You have no appointments yet. Book a consultation to get started.",
      });
    body.querySelectorAll(".cancel-booking").forEach(button => button.addEventListener("click", async () => {
      button.disabled = true;
      try {
        await api.post(`/api/appointments/mine/${button.dataset.id}/cancel`, {});
        toast("Appointment cancelled.", "success");
        await renderMyAppointments(body);
      } catch (error) {
        toast(error.message, "error");
        button.disabled = false;
      }
    }));
  } catch (error) {
    body.innerHTML = errorBanner(error.message);
  }
}

export async function renderDoctorDashboard(container) {
  container.innerHTML = panelHead(
    "Doctor Dashboard",
    "Review and decide only appointments assigned to your doctor profile."
  ) + `<div id="doctor-appointments">${loadingLine("Loading your appointments…")}</div>
    <section style="margin-top:28px;">
      ${panelHead("My Medical Records", "View, edit, or delete records assigned to your doctor profile.")}
      <div id="doctor-records">${loadingLine("Loading your medical records…")}</div>
    </section>
    <section style="margin-top:28px;">
      ${panelHead("Medical Record Change Requests", "Approve or reject manager-proposed changes to your records.")}
      <div id="doctor-record-requests">${loadingLine("Loading record change requests…")}</div>
    </section>`;
  await loadDoctorAppointments(container.querySelector("#doctor-appointments"));
  await loadDoctorRecords(container.querySelector("#doctor-records"));
  await loadDoctorRecordRequests(container.querySelector("#doctor-record-requests"));
}

async function loadDoctorAppointments(body) {
  try {
    const appointments = await api.get("/api/appointments/doctor/mine");
    body.innerHTML = renderTable({
      columns: [
        { key: "patientName", label: "Patient", render: row => `
          <strong>${escape(row.patientName)}</strong><br />
          <span>${escape(row.patientEmail || "")}</span><br />
          <span>${escape(row.patientPhoneNumber || "")}</span>` },
        { key: "appointmentTime", label: "Date & time", render: row => fmtDate(row.appointmentTime) },
        { key: "reason", label: "Reason for visit" },
        { key: "status", label: "Status", render: row => statusChip(row.status) },
        { key: "rejectionReason", label: "Decision / reason", render: row => escape(row.rejectionReason || (row.decisionTime ? `Reviewed ${fmtDate(row.decisionTime)}` : "Awaiting review")) },
      ],
      rows: appointments,
      actions: row => row.status === "PENDING" ? `
        <button class="btn btn-primary btn-sm approve-booking" data-id="${row.id}">Approve</button>
        <button class="btn btn-danger btn-sm reject-booking" data-id="${row.id}">Reject</button>` :
        row.status === "APPROVED" ? `
        <button class="btn btn-primary btn-sm create-record" data-id="${row.id}">Create medical record</button>` : "",
      emptyMessage: "No appointments have been assigned to you.",
    });
    body.querySelectorAll(".create-record").forEach(button => button.addEventListener("click", () => {
      const appointment = appointments.find(row => String(row.id) === button.dataset.id);
      if (appointment) openAppointmentRecordForm(appointment, body);
    }));
    body.querySelectorAll(".approve-booking").forEach(button => button.addEventListener("click", () =>
      decide(button.dataset.id, "APPROVED", null, body)));
    body.querySelectorAll(".reject-booking").forEach(button => button.addEventListener("click", () =>
      openFormModal({
        title: "Reject appointment",
        submitLabel: "Reject booking",
        fieldsHtml: field({ name: "reason", label: "Reason for rejection (optional)", type: "textarea", full: true }),
        onSubmit: async (formData, close) => {
          if (await decide(button.dataset.id, "REJECTED", formData.get("reason"), body)) close();
        },
      })));
  } catch (error) {
    body.innerHTML = errorBanner(error.message);
  }
}

function openAppointmentRecordForm(appointment, body) {
  openFormModal({
    title: `Medical record — ${appointment.patientName}`,
    submitLabel: "Save medical record",
    fieldsHtml:
      `<div class="small-note" style="margin-bottom:10px;">Approved visit: ${escape(fmtDate(appointment.appointmentTime))}<br />Reason: ${escape(appointment.reason)}<br />Patient: ${escape(appointment.patientEmail || "")} ${escape(appointment.patientPhoneNumber || "")}</div>` +
      field({ name: "diagnosis", label: "Diagnosis", type: "textarea", required: true, full: true }) +
      field({ name: "treatment", label: "Treatment", type: "textarea", full: true }) +
      field({ name: "notes", label: "Notes", type: "textarea", full: true }),
    onSubmit: async (formData, close) => {
      await api.post(`/api/medical-records/appointment/${appointment.id}`, {
        diagnosis: formData.get("diagnosis"),
        treatment: formData.get("treatment"),
        notes: formData.get("notes"),
      });
      toast("Medical record created for the approved appointment.", "success");
      close();
      await loadDoctorAppointments(body);
      await loadDoctorRecords(document.getElementById("doctor-records"));
    },
  });
}

async function loadDoctorRecords(body) {
  if (!body) return;
  try {
    const records = await api.get("/api/medical-records/mine");
    body.innerHTML = renderTable({
      columns: [
        { key: "id", label: "ID", mono: true },
        { key: "patientName", label: "Patient" },
        { key: "diagnosis", label: "Diagnosis" },
        { key: "treatment", label: "Treatment" },
        { key: "notes", label: "Notes" },
        { key: "recordDate", label: "Date", render: row => fmtDate(row.recordDate) },
      ],
      rows: records,
      actions: row => `<button class="btn btn-ghost btn-sm edit-own-record" data-id="${row.id}">Edit</button>
        <button class="btn btn-danger btn-sm delete-own-record" data-id="${row.id}">Delete</button>`,
      emptyMessage: "You have not created any medical records yet. Records appear here after creation from an approved appointment.",
    });
    body.querySelectorAll(".edit-own-record").forEach(button => button.addEventListener("click", () => {
      const record = records.find(value => String(value.id) === button.dataset.id);
      if (record) openDoctorRecordEdit(record, body);
    }));
    body.querySelectorAll(".delete-own-record").forEach(button => button.addEventListener("click", async () => {
      if (!window.confirm("Delete this medical record? This cannot be undone.")) return;
      try {
        await api.del(`/api/medical-records/${button.dataset.id}`);
        toast("Medical record deleted.", "success");
        await loadDoctorRecords(body);
      } catch (error) {
        toast(error.message, "error");
      }
    }));
  } catch (error) {
    body.innerHTML = errorBanner(error.message);
  }
}

function openDoctorRecordEdit(record, body) {
  openFormModal({
    title: `Edit medical record #${record.id}`,
    submitLabel: "Save changes",
    fieldsHtml:
      `<div class="small-note" style="margin-bottom:10px;">Patient: ${escape(record.patientName)}</div>` +
      field({ name: "diagnosis", label: "Diagnosis", type: "textarea", required: true, full: true, value: record.diagnosis }) +
      field({ name: "treatment", label: "Treatment", type: "textarea", full: true, value: record.treatment }) +
      field({ name: "notes", label: "Notes", type: "textarea", full: true, value: record.notes }),
    onSubmit: async (formData, close) => {
      await api.put(`/api/medical-records/${record.id}`, {
        diagnosis: formData.get("diagnosis"),
        treatment: formData.get("treatment"),
        notes: formData.get("notes"),
      });
      toast("Medical record updated.", "success");
      close();
      await loadDoctorRecords(body);
    },
  });
}

async function loadDoctorRecordRequests(body) {
  if (!body) return;
  try {
    const requests = await api.get("/api/medical-records/change-requests/mine");
    body.innerHTML = renderTable({
      columns: [
        { key: "id", label: "Request", mono: true },
        { key: "medicalRecordId", label: "Record", mono: true },
        { key: "patientName", label: "Patient" },
        { key: "requestedBy", label: "Requested by" },
        { key: "changeType", label: "Requested action" },
        { key: "currentDiagnosis", label: "Current diagnosis" },
        { key: "diagnosis", label: "Proposed diagnosis" },
        { key: "currentTreatment", label: "Current treatment" },
        { key: "treatment", label: "Proposed treatment" },
        { key: "currentNotes", label: "Current notes" },
        { key: "notes", label: "Proposed notes" },
        { key: "requestReason", label: "Reason" },
        { key: "status", label: "Status", render: row => statusChip(row.status) },
        { key: "reviewReason", label: "Review note" },
      ],
      rows: requests,
      actions: row => row.status === "PENDING" ? `
        <button class="btn btn-primary btn-sm approve-record-change" data-id="${row.id}">Approve</button>
        <button class="btn btn-danger btn-sm reject-record-change" data-id="${row.id}">Reject</button>` : "",
      emptyMessage: "There are no medical-record changes awaiting your review.",
    });
    body.querySelectorAll(".approve-record-change").forEach(button => button.addEventListener("click", () =>
      reviewRecordChange(button.dataset.id, "APPROVED", null, body)));
    body.querySelectorAll(".reject-record-change").forEach(button => button.addEventListener("click", () =>
      openFormModal({
        title: "Reject medical-record change",
        submitLabel: "Reject request",
        fieldsHtml: field({ name: "reviewReason", label: "Reason (optional)", type: "textarea", full: true }),
        onSubmit: async (formData, close) => {
          if (await reviewRecordChange(button.dataset.id, "REJECTED", formData.get("reviewReason"), body)) close();
        },
      })));
  } catch (error) {
    body.innerHTML = errorBanner(error.message);
  }
}

async function reviewRecordChange(id, status, reviewReason, body) {
  try {
    await api.post(`/api/medical-records/change-requests/${id}/review`, { status, reviewReason });
    toast(`Medical record change ${status.toLowerCase()}.`, "success");
    await loadDoctorRecordRequests(body);
    await loadDoctorRecords(document.getElementById("doctor-records"));
    return true;
  } catch (error) {
    toast(error.message, "error");
    return false;
  }
}

async function decide(id, status, rejectionReason, body) {
  try {
    await api.post(`/api/appointments/doctor/${id}/decision`, { status, rejectionReason });
    toast(`Appointment ${status.toLowerCase()}.`, "success");
    await loadDoctorAppointments(body);
    return true;
  } catch (error) {
    toast(error.message, "error");
    return false;
  }
}

function formatSlot(value) {
  const [date, time] = value.split("T");
  const [year, month, day] = date.split("-").map(Number);
  const [hour, minute] = time.split(":").map(Number);
  return `${new Date(year, month - 1, day).toLocaleDateString()} · ${new Date(2000, 0, 1, hour, minute).toLocaleTimeString([], { hour: "numeric", minute: "2-digit" })}`;
}

function escape(value) {
  const node = document.createElement("span");
  node.textContent = value ?? "";
  return node.innerHTML;
}
