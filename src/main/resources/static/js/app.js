import { isAuthenticated, getUsername, getRole, clearSession, api } from "./api.js";
import { renderLanding, renderLogin, renderRegister, renderResetPassword } from "./modules/auth.js";
import { renderDashboard } from "./modules/dashboard.js";
import { renderAdmin } from "./modules/admin.js";
import { renderLaboratory } from "./modules/laboratory.js";
import { renderPharmacy } from "./modules/pharmacy.js";
import { renderBilling } from "./modules/billing.js";
import { renderDoctors } from "./modules/doctors.js";
import { renderAppointments } from "./modules/appointments.js";
import { renderAuditLogs } from "./modules/auditlogs.js";
import { openFormModal, field, toast } from "./ui.js";

// Every nav item lists which roles may see and open it — this must mirror
// the backend's SecurityConfig role rules, or a role will hit a 403 after clicking.
const NAV = [
  { path: "dashboard", label: "Dashboard", icon: iconGrid(), render: renderDashboard, roles: ["ADMIN"] },
  { path: "admin", label: "Hospital Admin", icon: iconBuilding(), render: renderAdmin, roles: ["ADMIN"] },
  { path: "laboratory", label: "Laboratory", icon: iconFlask(), render: renderLaboratory, roles: ["ADMIN", "LAB_TECHNICIAN"] },
  { path: "pharmacy", label: "Pharmacy", icon: iconPill(), render: renderPharmacy, roles: ["ADMIN", "PHARMACIST"] },
  { path: "billing", label: "Billing", icon: iconReceipt(), render: renderBilling, roles: ["ADMIN", "FINANCE_OFFICER"] },
  { path: "doctors", label: "Doctors & Records", icon: iconStethoscope(), render: renderDoctors, roles: ["ADMIN", "DOCTOR"] },
  { path: "appointments", label: "Appointments", icon: iconCalendar(), render: renderAppointments, roles: ["ADMIN", "RECEPTIONIST", "DOCTOR", "PATIENT"] },
  { path: "audit", label: "Audit Log", icon: iconShield(), render: renderAuditLogs, roles: ["ADMIN"] },
];

// The page each role lands on right after login — ADMIN gets the global
// dashboard, every other role goes straight to their own module.
const ROLE_HOME = {
  ADMIN: "dashboard",
  DOCTOR: "doctors",
  PHARMACIST: "pharmacy",
  LAB_TECHNICIAN: "laboratory",
  FINANCE_OFFICER: "billing",
  RECEPTIONIST: "appointments",
  PATIENT: "appointments",
};

function navForRole(role) {
  return NAV.filter(n => n.roles.includes(role));
}
function homeForRole(role) {
  return ROLE_HOME[role] || (navForRole(role)[0] || {}).path || "dashboard";
}

const appEl = document.getElementById("app");

function currentPath() {
  return (window.location.hash || "#/").replace(/^#\//, "");
}

async function route() {
  const path = currentPath();
  const role = getRole();

  if (!isAuthenticated()) {
    stopTopbarClock();
    if (path === "" || path === "home") renderLanding(appEl);
    else if (path === "register") renderRegister(appEl);
    else if (path === "reset-password") renderResetPassword(appEl);
    else renderLogin(appEl);
    return;
  }

  if (path === "login" || path === "register" || path === "reset-password" || path === "home" || path === "") {
    window.location.hash = "#/" + homeForRole(role);
    return;
  }

  const navItem = NAV.find(n => n.path === path) || NAV[0];

  if (!navItem.roles.includes(role)) {
    // This role has no permission for this module on the backend either —
    // send them to their own home page instead of letting the API 403 confuse them.
    window.location.hash = "#/" + homeForRole(role);
    return;
  }

  renderShell(navItem);
  const contentEl = document.getElementById("page-content");
  await navItem.render(contentEl);
}

function renderShell(navItem) {
  const visibleNav = navForRole(getRole());
  appEl.innerHTML = `
    <div class="shell">
      <aside class="sidebar">
        <div class="side-brand">Medi<span>core</span></div>
        <ul class="side-nav">
          ${visibleNav.map((n, i) => `
            <li>
              <a href="#/${n.path}" class="${n.path === navItem.path ? "active" : ""}">
                <span class="nav-num">${String(i).padStart(2, "0")}</span>
                <span class="ico">${n.icon}</span>
                <span class="side-label">${n.label}</span>
              </a>
            </li>`).join("")}
        </ul>
        <div class="side-user">
          <div class="u-avatar">${(getUsername() || "?").slice(0, 1).toUpperCase()}</div>
          <div class="u-meta">
            <div class="u-name">${getUsername() || "Staff"}</div>
            <div class="u-role">${getRole() || ""}</div>
          </div>
        </div>
        <button class="logout-btn side-label" id="change-pw-btn" style="margin-bottom:6px;">Change password</button>
        <button class="logout-btn side-label" id="logout-btn">Log out</button>
      </aside>
      <div class="main">
        <div class="topbar">
          <div class="tb-eyebrow"><span id="tb-date"></span><span class="tb-dot">·</span><span id="tb-time"></span></div>
          <h2 class="tb-greet" id="tb-greet"></h2>
          <p class="tb-sub">${navItem.path === "dashboard"
            ? "Here's how Medicore is running across every department right now."
            : "Medicore HMS · " + navItem.label}</p>
        </div>
        <div class="content" id="page-content"></div>
      </div>
    </div>
  `;

  startTopbarClock();

  document.getElementById("logout-btn").addEventListener("click", () => {
    stopTopbarClock();
    clearSession();
    window.location.hash = "#/";
  });

  document.getElementById("change-pw-btn").addEventListener("click", () => openChangePasswordModal());
}

// ----------------------------------------------------------------------------
// Top bar: greeting (morning / afternoon / evening) + live date and time
// ----------------------------------------------------------------------------
let clockTimer = null;

function greetingFor(date) {
  const h = date.getHours();
  if (h < 12) return "Good morning";
  if (h < 17) return "Good afternoon";
  return "Good evening";
}

function stopTopbarClock() {
  if (clockTimer) { clearInterval(clockTimer); clockTimer = null; }
}

function startTopbarClock() {
  stopTopbarClock();
  const tick = () => {
    const dateEl = document.getElementById("tb-date");
    const timeEl = document.getElementById("tb-time");
    const greetEl = document.getElementById("tb-greet");
    if (!dateEl || !timeEl || !greetEl) { stopTopbarClock(); return; }  // page changed
    const now = new Date();
    dateEl.textContent = now.toLocaleDateString("en-US", {
      weekday: "long", year: "numeric", month: "long", day: "numeric",
    });
    timeEl.textContent = now.toLocaleTimeString("en-US", {
      hour: "2-digit", minute: "2-digit", second: "2-digit",
    });
    greetEl.textContent = `${greetingFor(now)}, ${getUsername() || "Staff"}.`;
  };
  tick();
  clockTimer = setInterval(tick, 1000);
}

function openChangePasswordModal() {
  openFormModal({
    title: "Change password",
    submitLabel: "Update password",
    fieldsHtml:
      field({ name: "oldPassword", label: "Current password", type: "password", required: true, full: true }) +
      field({ name: "newPassword", label: "New password", type: "password", required: true, full: true, hint: "At least 6 characters." }),
    onSubmit: async (fd, close) => {
      await api.post("/api/auth/reset-password", {
        username: getUsername(),
        oldPassword: fd.get("oldPassword"),
        newPassword: fd.get("newPassword"),
      });
      toast("Password updated. Use it next time you log in.", "success");
      close();
    },
  });
}

window.addEventListener("hashchange", route);
window.addEventListener("DOMContentLoaded", route);
route();

// ----------------------------------------------------------------------------
// Minimal inline icon set (stroke-based, matches sidebar currentColor)
// ----------------------------------------------------------------------------
function iconGrid() {
  return `<svg viewBox="0 0 20 20" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="2.5" y="2.5" width="6" height="6" rx="1"/><rect x="11.5" y="2.5" width="6" height="6" rx="1"/><rect x="2.5" y="11.5" width="6" height="6" rx="1"/><rect x="11.5" y="11.5" width="6" height="6" rx="1"/></svg>`;
}
function iconBuilding() {
  return `<svg viewBox="0 0 20 20" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="3" y="2.5" width="10" height="15" rx="1"/><path d="M13 8h4v9.5h-4M6 6h1M9 6h1M6 9h1M9 9h1M6 12h1M9 12h1"/></svg>`;
}
function iconFlask() {
  return `<svg viewBox="0 0 20 20" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M8 2.5h4M8.5 2.5v5.2L4.2 15a1.5 1.5 0 0 0 1.3 2.3h9a1.5 1.5 0 0 0 1.3-2.3l-4.3-7.3V2.5"/><path d="M6 12.5h8"/></svg>`;
}
function iconPill() {
  return `<svg viewBox="0 0 20 20" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="2.5" y="8.7" width="15" height="6.3" rx="3.15" transform="rotate(-35 10 12)"/><path d="M9 8l2 5"/></svg>`;
}
function iconReceipt() {
  return `<svg viewBox="0 0 20 20" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M5 2.5h10v15l-2-1.3-1.7 1.3L9.6 16l-1.7 1.3L6 16l-1 1V2.5z"/><path d="M7 6.5h6M7 9.5h6M7 12.5h4"/></svg>`;
}
function iconStethoscope() {
  return `<svg viewBox="0 0 20 20" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M5 2.5v5a3.5 3.5 0 0 0 7 0v-5"/><path d="M6.5 2.5h-2M13.5 2.5h-2M12 10.5v2a4 4 0 0 1-8 0v-1.7"/><circle cx="15.5" cy="12.5" r="1.8"/></svg>`;
}
function iconCalendar() {
  return `<svg viewBox="0 0 20 20" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="2.5" y="3.5" width="15" height="14" rx="1.4"/><path d="M2.5 7.5h15M6 2v3M14 2v3"/><path d="M6 11h2M9.5 11h2M13 11h2M6 14h2M9.5 14h2"/></svg>`;
}
function iconShield() {
  return `<svg viewBox="0 0 20 20" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M10 2.2 16.5 4.6v5.1c0 4-2.8 6.6-6.5 8.1-3.7-1.5-6.5-4.1-6.5-8.1V4.6L10 2.2Z"/><path d="M7.3 10.1l1.9 1.9 3.5-3.9"/></svg>`;
}
