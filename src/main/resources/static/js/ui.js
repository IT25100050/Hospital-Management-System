// ============================================================================
// Medicore HMS — shared UI building blocks
// ============================================================================

export function el(html) {
  const t = document.createElement("template");
  t.innerHTML = html.trim();
  return t.content.firstElementChild;
}

export function toast(message, type = "success") {
  const root = document.getElementById("toast-root");
  const node = el(`<div class="toast ${type}">${escapeHtml(message)}</div>`);
  root.appendChild(node);
  setTimeout(() => {
    node.style.opacity = "0";
    node.style.transition = "opacity .25s ease";
    setTimeout(() => node.remove(), 260);
  }, 3200);
}

export function escapeHtml(str) {
  if (str === null || str === undefined) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

export function fmtDate(value) {
  if (!value) return "—";
  try {
    const d = new Date(value);
    if (isNaN(d.getTime())) return value;
    return d.toLocaleString(undefined, {
      year: "numeric", month: "short", day: "2-digit",
      hour: "2-digit", minute: "2-digit"
    });
  } catch (e) {
    return value;
  }
}

export function fmtMoney(value) {
  if (value === null || value === undefined || value === "") return "—";
  const num = Number(value);
  if (isNaN(num)) return value;
  return "LKR " + num.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export function toLocalDateTimeInputValue(date = new Date()) {
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

// The backend stores java.time.LocalDateTime (no timezone offset), so we must
// NOT send Date#toISOString() (which appends milliseconds + "Z"). This produces
// a plain "yyyy-MM-ddTHH:mm:ss" string that Jackson's LocalDateTime deserializer
// accepts directly.
export function nowForBackend(date = new Date()) {
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

/**
 * Renders a data table.
 * columns: [{ key, label, render?(row) => html, mono? }]
 * rows: array of objects
 * actions(row) => html string for the trailing actions cell (optional)
 */
export function renderTable({ columns, rows, actions, emptyMessage = "No records yet." }) {
  const head = columns.map(c => `<th>${escapeHtml(c.label)}</th>`).join("") + (actions ? `<th></th>` : "");
  let body;
  if (!rows || rows.length === 0) {
    body = `<tr class="empty-row"><td colspan="${columns.length + (actions ? 1 : 0)}">${escapeHtml(emptyMessage)}</td></tr>`;
  } else {
    body = rows.map(row => {
      const cells = columns.map(c => {
        const val = c.render ? c.render(row) : escapeHtml(row[c.key] ?? "—");
        return `<td class="${c.mono ? "mono" : ""}">${val}</td>`;
      }).join("");
      const actionCell = actions ? `<td><div class="row-actions">${actions(row)}</div></td>` : "";
      return `<tr data-id="${escapeHtml(row.id ?? "")}">${cells}${actionCell}</tr>`;
    }).join("");
  }
  return `<div class="table-wrap"><table><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table></div>`;
}

export function statusChip(status) {
  const map = {
    PENDING: "amber", CONFIRMED: "blue", COMPLETED: "green", CANCELLED: "red",
    ORDERED: "amber", IN_PROGRESS: "blue",
    PAID: "green", PARTIAL: "amber", REFUNDED: "grey",
  };
  const cls = map[status] || "grey";
  return `<span class="chip chip-${cls}">${escapeHtml(status || "—")}</span>`;
}

/**
 * Opens a modal with a form.
 * fieldsHtml: raw HTML for the form-grid fields
 * onSubmit(formData: FormData, closeFn) — call closeFn() on success
 */
export function openFormModal({ title, fieldsHtml, submitLabel = "Save", onSubmit }) {
  const overlay = el(`
    <div class="modal-overlay">
      <div class="modal-box">
        <div class="modal-head">
          <h3>${escapeHtml(title)}</h3>
          <button class="modal-close" aria-label="Close">&times;</button>
        </div>
        <form class="modal-form">
          <div class="modal-body">
            <div class="form-grid">${fieldsHtml}</div>
            <div class="form-error" style="display:none" class="error-banner"></div>
          </div>
          <div class="modal-foot">
            <button type="button" class="btn btn-ghost cancel-btn">Cancel</button>
            <button type="submit" class="btn btn-primary submit-btn">${escapeHtml(submitLabel)}</button>
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

  const form = overlay.querySelector(".modal-form");
  const submitBtn = overlay.querySelector(".submit-btn");
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    submitBtn.disabled = true;
    const originalLabel = submitBtn.textContent;
    submitBtn.textContent = "Saving…";
    try {
      await onSubmit(new FormData(form), close);
    } catch (err) {
      toast(err.message || "Something went wrong.", "error");
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = originalLabel;
    }
  });

  return { close, overlay };
}

export function confirmAction(message) {
  return window.confirm(message);
}

export function field({ name, label, type = "text", required = false, value = "", options, hint, step, full = false }) {
  const req = required ? "required" : "";
  if (type === "select") {
    const opts = (options || []).map(o =>
      `<option value="${escapeHtml(o.value)}" ${String(o.value) === String(value) ? "selected" : ""}>${escapeHtml(o.label)}</option>`
    ).join("");
    return `
      <div class="field ${full ? "full" : ""}">
        <label for="f-${name}">${escapeHtml(label)}</label>
        <select id="f-${name}" name="${name}" ${req}>${opts}</select>
        ${hint ? `<span class="hint">${escapeHtml(hint)}</span>` : ""}
      </div>`;
  }
  if (type === "textarea") {
    return `
      <div class="field ${full ? "full" : ""}">
        <label for="f-${name}">${escapeHtml(label)}</label>
        <textarea id="f-${name}" name="${name}" ${req}>${escapeHtml(value)}</textarea>
        ${hint ? `<span class="hint">${escapeHtml(hint)}</span>` : ""}
      </div>`;
  }
  return `
    <div class="field ${full ? "full" : ""}">
      <label for="f-${name}">${escapeHtml(label)}</label>
      <input id="f-${name}" name="${name}" type="${type}" ${req} value="${escapeHtml(value)}" ${step ? `step="${step}"` : ""} />
      ${hint ? `<span class="hint">${escapeHtml(hint)}</span>` : ""}
    </div>`;
}

export function panelHead(title, desc, actionHtml) {
  return `
    <div class="panel-head">
      <div>
        <h3>${escapeHtml(title)}</h3>
        ${desc ? `<div class="desc">${escapeHtml(desc)}</div>` : ""}
      </div>
      ${actionHtml || ""}
    </div>`;
}

export function loadingLine(msg = "Loading…") {
  return `<div class="loading-line">${escapeHtml(msg)}</div>`;
}

export function errorBanner(msg) {
  return `<div class="error-banner">${escapeHtml(msg)}</div>`;
}
