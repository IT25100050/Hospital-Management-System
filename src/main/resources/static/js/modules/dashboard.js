import { api } from "../api.js";
import { loadingLine, errorBanner, fmtMoney } from "../ui.js";

export async function renderDashboard(container) {
  container.innerHTML = loadingLine("Fetching dashboard summary…");
  try {
    const [summary, recentLogs] = await Promise.all([
      api.get("/api/dashboard/summary"),
      api.get("/api/audit/logs", { limit: 8 }).catch(() => []),
    ]);

    container.innerHTML = `
      <div class="kpi-row">
        <div class="kpi-card">
          <div class="kpi-label">Total Patients</div>
          <div class="kpi-val">${summary.totalPatients ?? 0}</div>
          <div class="kpi-foot">Registered in the system</div>
        </div>
        <div class="kpi-card blue">
          <div class="kpi-label">Total Doctors</div>
          <div class="kpi-val">${summary.totalDoctors ?? 0}</div>
          <div class="kpi-foot">Across all departments</div>
        </div>
        <div class="kpi-card amber">
          <div class="kpi-label">Today's Appointments</div>
          <div class="kpi-val">${summary.todayAppointments ?? 0}</div>
          <div class="kpi-foot">${summary.totalAppointments ?? 0} booked all-time</div>
        </div>
        <div class="kpi-card green">
          <div class="kpi-label">Revenue Collected</div>
          <div class="kpi-val" style="font-size:22px;">${fmtMoney(summary.totalRevenue)}</div>
          <div class="kpi-foot">Across ${summary.totalBills ?? 0} bills</div>
        </div>
      </div>

      <div class="two-col" style="margin-top:18px; align-items:stretch;">
        <div class="panel" style="padding:18px;">
          <h3 class="section-title" style="margin-bottom:12px;">Things that need attention</h3>
          <div class="attn-list">
            <div class="attn-row ${summary.outstandingAmount > 0 ? "warn" : "ok"}">
              <span>Outstanding balance across unpaid/partial bills</span>
              <strong>${fmtMoney(summary.outstandingAmount)}</strong>
            </div>
            <div class="attn-row ${summary.lowStockMedicineCount > 0 ? "warn" : "ok"}">
              <span>Medicines at or below reorder level (≤10 units)</span>
              <strong>${summary.lowStockMedicineCount ?? 0}</strong>
            </div>
          </div>
          <p class="small-note" style="margin-top:14px;">
            Open <strong>Pharmacy</strong> to restock low medicines, or <strong>Billing</strong> to chase outstanding payments.
          </p>
        </div>

        <div class="panel" style="padding:18px;">
          <h3 class="section-title" style="margin-bottom:12px;">Recent activity</h3>
          ${renderRecentActivity(recentLogs)}
          <p class="small-note" style="margin-top:14px;">Open <strong>Audit Log</strong> for the full history, filterable by user or module.</p>
        </div>
      </div>

      <div class="panel" style="margin-top:18px;">
        <h3 class="section-title" style="margin-bottom:8px;">Welcome to Medicore</h3>
        <p class="small-note" style="font-size:13.5px; line-height:1.7; color: var(--ink-soft);">
          Use the sidebar to work across the hospital's core areas: Hospital Admin,
          Laboratory, Pharmacy, Billing, Doctors &amp; Medical Records, Appointments, and
          the Audit Log. Every figure above is read live from the backend — nothing here
          is sample data.
        </p>
      </div>`;
  } catch (err) {
    container.innerHTML = errorBanner(err.message);
  }
}

function renderRecentActivity(logs) {
  if (!logs || !logs.length) {
    return `<div class="small-note">No activity recorded yet.</div>`;
  }
  return `<ul class="activity-list">
    ${logs.slice(0, 8).map(l => `
      <li>
        <span class="chip chip-${actionColor(l.action)}">${l.action || "—"}</span>
        <span class="activity-text"><strong>${escapeHtml(l.username)}</strong> — ${escapeHtml(l.module)}</span>
        <span class="activity-time">${fmtShortTime(l.timestamp)}</span>
      </li>`).join("")}
  </ul>`;
}

function actionColor(action) {
  const map = { CREATE: "green", UPDATE: "amber", DELETE: "red", VIEW: "grey", OTHER: "grey" };
  return map[action] || "grey";
}

function fmtShortTime(value) {
  if (!value) return "";
  const d = new Date(value);
  if (isNaN(d.getTime())) return "";
  return d.toLocaleString(undefined, { month: "short", day: "2-digit", hour: "2-digit", minute: "2-digit" });
}

function escapeHtml(str) {
  if (str === null || str === undefined) return "";
  return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}
