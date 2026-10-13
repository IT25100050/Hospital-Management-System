import { api } from "../api.js";
import {
  loadingLine, errorBanner, renderTable, panelHead, openFormModal, field,
  toast, confirmAction, escapeHtml
} from "../ui.js";

const ROLES = ["ADMIN", "DOCTOR_RECORDS_MANAGER", "DOCTOR", "PHARMACIST", "LAB_TECHNICIAN", "RECEPTIONIST", "FINANCE_OFFICER", "PATIENT"];
const ROLE_LABELS = {
  ADMIN: "ADMIN — Full system access",
  DOCTOR_RECORDS_MANAGER: "Doctor & Medical Record Manager (restricted)",
  DOCTOR: "DOCTOR",
  PHARMACIST: "PHARMACIST",
  LAB_TECHNICIAN: "LAB TECHNICIAN",
  RECEPTIONIST: "RECEPTIONIST",
  FINANCE_OFFICER: "FINANCE OFFICER",
  PATIENT: "PATIENT",
};

let activeTab = "departments";
let departmentsCache = [];

export async function renderAdmin(container) {
  container.innerHTML = `
    <div class="folder-tabs">
      <button data-tab="departments" class="${activeTab === "departments" ? "active" : ""}">Departments</button>
      <button data-tab="staff" class="${activeTab === "staff" ? "active" : ""}">Staff</button>
      <button data-tab="users" class="${activeTab === "users" ? "active" : ""}">Login Accounts</button>
    </div>
    <div class="tab-panel" id="admin-tab-body">${loadingLine()}</div>
  `;

  container.querySelectorAll(".folder-tabs button").forEach(btn => {
    btn.addEventListener("click", () => {
      activeTab = btn.dataset.tab;
      renderAdmin(container);
    });
  });

  const body = container.querySelector("#admin-tab-body");
  if (activeTab === "departments") await renderDepartments(body);
  else if (activeTab === "staff") await renderStaff(body);
  else await renderUsers(body);
}

// ---------------------------------------------------------------------------
// Login accounts (users) — manage roles and force-reset forgotten passwords
// ---------------------------------------------------------------------------
async function renderUsers(body) {
  try {
    const users = await api.get("/api/auth/admin/users");
    body.innerHTML =
      panelHead("Login Accounts", "Create accounts with assigned roles, manage access, and reset forgotten passwords. Doctor & Medical Record Manager is limited to doctor profiles, registration approvals, and medical records. ADMIN retains full system access; DOCTOR accounts are created through approved doctor registration.",
        `<button class="btn btn-primary" id="add-user">+ Create account</button>`) +
      renderTable({
        columns: [
          { key: "id", label: "ID", mono: true },
          { key: "username", label: "Username" },
          { key: "email", label: "Email" },
          { key: "role", label: "Role", render: r => `<span class="chip chip-blue">${escapeHtml(ROLE_LABELS[r.role] || r.role)}</span>` },
        ],
        rows: users,
        actions: (row) => `
          <button class="btn btn-ghost btn-sm change-role" data-username="${escapeHtml(row.username)}" data-role="${escapeHtml(row.role)}">Change role</button>
          <button class="btn btn-ghost btn-sm reset-pw" data-username="${escapeHtml(row.username)}">Reset password</button>
          <button class="btn btn-danger btn-sm delete-user" data-username="${escapeHtml(row.username)}">Delete account</button>`,
        emptyMessage: "No user accounts found.",
      });

    body.querySelector("#add-user").addEventListener("click", () => openAdminCreateUserModal(body));
    body.querySelectorAll(".reset-pw").forEach(b =>
      b.addEventListener("click", () => openAdminResetModal(b.dataset.username)));
    body.querySelectorAll(".change-role").forEach(b =>
      b.addEventListener("click", () => openRoleModal(b.dataset.username, b.dataset.role)));
    body.querySelectorAll(".delete-user").forEach(button =>
      button.addEventListener("click", () => deleteUserAccount(button.dataset.username, body)));
  } catch (err) {
    body.innerHTML = errorBanner(err.message);
  }
}

async function deleteUserAccount(username, body) {
  if (!confirmAction(`Delete the login account for ${username}? Any linked patient or doctor profile and clinical history will be retained, but that account will no longer be able to sign in.`)) {
    return;
  }
  try {
    await api.del(`/api/auth/admin/users/${encodeURIComponent(username)}`);
    toast(`Login account ${username} deleted. Linked profile/history was retained.`, "success");
    await renderUsers(body);
  } catch (error) {
    toast(error.message, "error");
  }
}

function openAdminCreateUserModal(body) {
  const creatableRoles = ROLES.filter(role => role !== "DOCTOR");
  openFormModal({
    title: "Create login account",
    submitLabel: "Create account",
    fieldsHtml:
      field({ name: "username", label: "Username", required: true }) +
      field({ name: "email", label: "Email", type: "email", required: true }) +
      field({ name: "password", label: "Temporary password", type: "password", required: true, hint: "At least 8 characters." }) +
      field({
        name: "role", label: "Account role", type: "select", required: true,
        options: creatableRoles.map(role => ({ value: role, label: ROLE_LABELS[role] })),
      }) +
      `<div class="small-note full">Doctor accounts are activated only through approval of a doctor registration request.</div>`,
    onSubmit: async (fd, close) => {
      const account = await api.post("/api/auth/admin/users", {
        username: fd.get("username"),
        email: fd.get("email"),
        password: fd.get("password"),
        role: fd.get("role"),
      });
      toast(`Account created for ${account.username} with role ${account.role}.`, "success");
      close();
      await renderUsers(body);
    },
  });
}

function openRoleModal(username, currentRole) {
  openFormModal({
    title: `Change role — ${username}`,
    submitLabel: "Save role",
    fieldsHtml:
      `<div class="small-note" style="margin-bottom:10px;">Doctor & Medical Record Manager grants access only to doctor profiles, doctor registration approvals, and medical records. ADMIN retains full system access. DOCTOR is available only to accounts with an approved doctor profile.</div>` +
      field({
        name: "role",
        label: "Account role",
        type: "select",
        required: true,
        value: currentRole,
        full: true,
        options: ROLES.map(role => ({ value: role, label: ROLE_LABELS[role] })),
      }),
    onSubmit: async (fd, close) => {
      await api.put(`/api/auth/admin/users/${encodeURIComponent(username)}/role`, {
        role: fd.get("role"),
      });
      toast(`Role updated for ${username}.`, "success");
      close();
      await renderUsers(document.getElementById("admin-tab-body"));
    },
  });
}

function openAdminResetModal(username) {
  openFormModal({
    title: `Reset password — ${username}`,
    submitLabel: "Reset password",
    fieldsHtml:
      `<div class="small-note" style="margin-bottom:10px;">This sets a new password immediately — no need to know the old one. Share it with ${escapeHtml(username)} securely.</div>` +
      field({ name: "newPassword", label: "New password", type: "password", required: true, full: true, hint: "At least 6 characters." }),
    onSubmit: async (fd, close) => {
      await api.post(`/api/auth/admin/users/${encodeURIComponent(username)}/reset-password`, {
        newPassword: fd.get("newPassword"),
      });
      toast(`Password reset for ${username}.`, "success");
      close();
    },
  });
}

// ---------------------------------------------------------------------------
// Departments
// ---------------------------------------------------------------------------
async function renderDepartments(body) {
  try {
    const departments = await api.get("/api/admin/departments");
    departmentsCache = departments || [];
    body.innerHTML =
      panelHead("Departments", "Clinical and operational departments in the hospital.",
        `<button class="btn btn-primary" id="add-dept">+ New department</button>`) +
      renderTable({
        columns: [
          { key: "id", label: "ID", mono: true },
          { key: "name", label: "Name" },
          { key: "description", label: "Description" },
        ],
        rows: departmentsCache,
        actions: (row) => `
          <button class="btn btn-ghost btn-sm edit-dept" data-id="${row.id}">Edit</button>
          <button class="btn btn-danger btn-sm del-dept" data-id="${row.id}">Delete</button>`,
        emptyMessage: "No departments yet — add the first one.",
      });

    body.querySelector("#add-dept").addEventListener("click", () => openDeptModal());
    body.querySelectorAll(".edit-dept").forEach(b =>
      b.addEventListener("click", () => openDeptModal(departmentsCache.find(d => String(d.id) === b.dataset.id))));
    body.querySelectorAll(".del-dept").forEach(b =>
      b.addEventListener("click", () => deleteDept(b.dataset.id, body)));
  } catch (err) {
    body.innerHTML = errorBanner(err.message);
  }
}

function openDeptModal(existing) {
  openFormModal({
    title: existing ? `Edit department #${existing.id}` : "New department",
    submitLabel: existing ? "Save changes" : "Create department",
    fieldsHtml:
      field({ name: "name", label: "Name", required: true, value: existing?.name, full: true }) +
      field({ name: "description", label: "Description", type: "textarea", value: existing?.description, full: true }),
    onSubmit: async (fd, close) => {
      const payload = { name: fd.get("name"), description: fd.get("description") };
      if (existing) await api.put(`/api/admin/departments/${existing.id}`, payload);
      else await api.post("/api/admin/departments", payload);
      toast(existing ? "Department updated." : "Department created.", "success");
      close();
      await renderAdmin(document.getElementById("page-content"));
    },
  });
}

async function deleteDept(id, body) {
  if (!confirmAction("Delete this department? This cannot be undone.")) return;
  try {
    await api.del(`/api/admin/departments/${id}`);
    toast("Department deleted.", "success");
    await renderDepartments(body);
  } catch (err) {
    toast(err.message, "error");
  }
}

// ---------------------------------------------------------------------------
// Staff
// ---------------------------------------------------------------------------
async function renderStaff(body) {
  try {
    const [staff, departments] = await Promise.all([
      api.get("/api/admin/staff"),
      api.get("/api/admin/departments"),
    ]);
    departmentsCache = departments || [];

    body.innerHTML =
      panelHead("Staff Directory", "Everyone employed across the hospital's departments.",
        `<button class="btn btn-primary" id="add-staff">+ New staff member</button>`) +
      renderTable({
        columns: [
          { key: "id", label: "ID", mono: true },
          { key: "name", label: "Name" },
          { key: "role", label: "Role", render: r => `<span class="chip chip-blue">${escapeHtml(r.role)}</span>` },
          { key: "departmentName", label: "Department" },
          { key: "email", label: "Email" },
          { key: "phone", label: "Phone" },
        ],
        rows: staff,
        actions: (row) => `
          <button class="btn btn-ghost btn-sm edit-staff" data-id="${row.id}">Edit</button>
          <button class="btn btn-danger btn-sm del-staff" data-id="${row.id}">Delete</button>`,
        emptyMessage: "No staff members yet — add the first one.",
      });

    body.querySelector("#add-staff").addEventListener("click", () => openStaffModal(null, staff));
    body.querySelectorAll(".edit-staff").forEach(b =>
      b.addEventListener("click", () => openStaffModal(staff.find(s => String(s.id) === b.dataset.id))));
    body.querySelectorAll(".del-staff").forEach(b =>
      b.addEventListener("click", () => deleteStaff(b.dataset.id, body)));
  } catch (err) {
    body.innerHTML = errorBanner(err.message);
  }
}

function openStaffModal(existing) {
  if (!departmentsCache.length) {
    toast("Create a department first before adding staff.", "error");
    return;
  }
  openFormModal({
    title: existing ? `Edit staff #${existing.id}` : "New staff member",
    submitLabel: existing ? "Save changes" : "Create staff member",
    fieldsHtml:
      field({ name: "name", label: "Full name", required: true, value: existing?.name }) +
      field({ name: "email", label: "Email", type: "email", required: true, value: existing?.email }) +
      field({ name: "phone", label: "Phone", value: existing?.phone }) +
      field({
        name: "role", label: "Role", type: "select", required: true,
        value: existing?.role, options: ROLES.map(r => ({ value: r, label: r.replace("_", " ") })),
      }) +
      field({
        name: "departmentId", label: "Department", type: "select", required: true, full: true,
        value: existing?.departmentId,
        options: departmentsCache.map(d => ({ value: d.id, label: d.name })),
      }),
    onSubmit: async (fd, close) => {
      const payload = {
        name: fd.get("name"),
        email: fd.get("email"),
        phone: fd.get("phone"),
        role: fd.get("role"),
        departmentId: Number(fd.get("departmentId")),
      };
      if (existing) await api.put(`/api/admin/staff/${existing.id}`, payload);
      else await api.post("/api/admin/staff", payload);
      toast(existing ? "Staff member updated." : "Staff member created.", "success");
      close();
      await renderAdmin(document.getElementById("page-content"));
    },
  });
}

async function deleteStaff(id, body) {
  if (!confirmAction("Remove this staff member?")) return;
  try {
    await api.del(`/api/admin/staff/${id}`);
    toast("Staff member removed.", "success");
    await renderStaff(body);
  } catch (err) {
    toast(err.message, "error");
  }
}
