import { api } from "../api.js";
import { loadingLine, errorBanner, renderTable, panelHead, field, fmtDate, escapeHtml } from "../ui.js";

function moduleChip(module) {
  return `<span class="chip chip-blue">${escapeHtml(module || "—")}</span>`;
}
function actionChip(action) {
  const map = { CREATE: "green", UPDATE: "amber", DELETE: "red", VIEW: "grey", OTHER: "grey" };
  const cls = map[action] || "grey";
  return `<span class="chip chip-${cls}">${escapeHtml(action || "—")}</span>`;
}

export async function renderAuditLogs(container) {
  container.innerHTML = loadingLine();
  await loadLogs(container, "/api/audit/logs", { limit: 150 });
}

async function loadLogs(container, path, params) {
  try {
    const logs = await api.get(path, params);
    container.innerHTML =
      panelHead("Audit Log", "Every create, update, and delete across the system — who did what, where, and when.") +
      `<div class="form-grid" style="margin-bottom:14px;">
        ${field({ name: "username", label: "Filter by username" })}
        ${field({ name: "module", label: "Filter by module", hint: "e.g. Billing, Pharmacy, Doctor" })}
        <div class="field"><button type="button" class="btn btn-ghost" id="filter-btn" style="margin-top:20px;">Filter</button></div>
        <div class="field"><button type="button" class="btn btn-ghost" id="reset-filter-btn" style="margin-top:20px;">Show recent</button></div>
      </div>
      <div id="audit-result">${renderLogsTable(logs)}</div>`;

    container.querySelector("#filter-btn").addEventListener("click", async () => {
      const username = container.querySelector("[name=username]").value.trim();
      const module = container.querySelector("[name=module]").value.trim();
      const out = container.querySelector("#audit-result");
      out.innerHTML = loadingLine();
      try {
        let result;
        if (username) result = await api.get(`/api/audit/logs/user/${encodeURIComponent(username)}`);
        else if (module) result = await api.get(`/api/audit/logs/module/${encodeURIComponent(module)}`);
        else result = await api.get("/api/audit/logs", { limit: 150 });
        out.innerHTML = renderLogsTable(result);
      } catch (err) {
        out.innerHTML = errorBanner(err.message);
      }
    });

    container.querySelector("#reset-filter-btn").addEventListener("click", () => loadLogs(container, "/api/audit/logs", { limit: 150 }));
  } catch (err) {
    container.innerHTML = errorBanner(err.message);
  }
}

function renderLogsTable(logs) {
  return renderTable({
    columns: [
      { key: "timestamp", label: "When", render: r => fmtDate(r.timestamp) },
      { key: "username", label: "User" },
      { key: "module", label: "Module", render: r => moduleChip(r.module) },
      { key: "action", label: "Action", render: r => actionChip(r.action) },
      { key: "details", label: "Details" },
    ],
    rows: logs,
    emptyMessage: "No audit activity recorded yet.",
  });
}
