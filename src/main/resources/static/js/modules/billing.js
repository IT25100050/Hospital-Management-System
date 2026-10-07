import { api } from "../api.js";
import {
  loadingLine, errorBanner, renderTable, panelHead, field,
  toast, confirmAction, fmtMoney, fmtDate, escapeHtml, el, nowForBackend
} from "../ui.js";

let activeTab = "bills";
let patientsCache = [];
let billsCache = [];

export async function renderBilling(container) {
  container.innerHTML = `
    <div class="folder-tabs">
      <button data-tab="bills" class="${activeTab === "bills" ? "active" : ""}">Bills</button>
      <button data-tab="payments" class="${activeTab === "payments" ? "active" : ""}">Payments</button>
    </div>
    <div class="tab-panel" id="bill-tab-body">${loadingLine()}</div>
  `;
  container.querySelectorAll(".folder-tabs button").forEach(btn =>
    btn.addEventListener("click", () => { activeTab = btn.dataset.tab; renderBilling(container); }));

  const body = container.querySelector("#bill-tab-body");
  if (activeTab === "bills") await renderBills(body);
  else await renderPayments(body);
}

async function ensurePatients() {
  if (!patientsCache.length) patientsCache = (await api.get("/api/patients/lookup")) || [];
  return patientsCache;
}

function billStatusChip(status) {
  const cls = status === "PAID" ? "green" : status === "PARTIAL" ? "amber" : status === "REFUNDED" ? "grey" : "red";
  return `<span class="chip chip-${cls}">${escapeHtml(status || "—")}</span>`;
}

// ---------------------------------------------------------------------------
// Bills
// ---------------------------------------------------------------------------
async function renderBills(body) {
  try {
    await ensurePatients();
    billsCache = (await api.get("/api/bills")) || [];
  } catch (err) {
    body.innerHTML = errorBanner(err.message);
    return;
  }

  body.innerHTML =
    panelHead("Bills", "Create an invoice for a patient with line items, or manage bills already issued.",
      `<button class="btn btn-primary" id="add-bill">+ New bill</button>`) +
    `<div class="form-grid" style="margin-bottom:14px;">
      ${field({ name: "patientFilter", label: "Filter by patient (optional)", type: "select", options: [{ value: "", label: "All patients" }, ...patientsCache.map(p => ({ value: p.id, label: `${p.name} (#${p.id})` }))] })}
      <div class="field"><button type="button" class="btn btn-ghost" id="filter-bills-btn" style="margin-top:20px;">Filter</button></div>
    </div>
    <div id="bills-result">${renderBillsTable(billsCache)}</div>`;

  attachBillRowHandlers(body);

  body.querySelector("#add-bill").addEventListener("click", () => openBillModal());

  body.querySelector("#filter-bills-btn").addEventListener("click", async () => {
    const select = body.querySelector("[name=patientFilter]");
    const pid = select.value;
    const out = body.querySelector("#bills-result");
    out.innerHTML = loadingLine("Fetching bills…");
    try {
      billsCache = pid ? await api.get(`/api/bills/patient/${pid}`) : await api.get("/api/bills");
      out.innerHTML = renderBillsTable(billsCache);
      attachBillRowHandlers(body);
    } catch (err) {
      out.innerHTML = errorBanner(err.message);
    }
  });
}

function renderBillsTable(bills) {
  return renderTable({
    columns: [
      { key: "invoiceNumber", label: "Invoice #", mono: true },
      { key: "patientId", label: "Patient ID", mono: true },
      { key: "totalAmount", label: "Total", render: r => fmtMoney(r.totalAmount) },
      { key: "paidAmount", label: "Paid", render: r => fmtMoney(r.paidAmount) },
      { key: "status", label: "Status", render: r => billStatusChip(r.status) },
      { key: "createdDate", label: "Created", render: r => fmtDate(r.createdDate) },
    ],
    rows: bills,
    actions: (row) => `
      <button class="btn btn-ghost btn-sm edit-bill" data-id="${row.id}">Edit</button>
      <button class="btn btn-danger btn-sm del-bill" data-id="${row.id}">Delete</button>`,
    emptyMessage: "No bills found.",
  });
}

function attachBillRowHandlers(body) {
  body.querySelectorAll(".edit-bill").forEach(b =>
    b.addEventListener("click", () => openBillModal(billsCache.find(x => String(x.id) === b.dataset.id))));
  body.querySelectorAll(".del-bill").forEach(b =>
    b.addEventListener("click", () => deleteBill(b.dataset.id, body)));
}

function openBillModal(existing) {
  if (!existing && !patientsCache.length) {
    toast("Register a patient first before creating a bill.", "error");
    return;
  }
  const overlay = el(`
    <div class="modal-overlay">
      <div class="modal-box">
        <div class="modal-head"><h3>${existing ? `Edit bill ${escapeHtml(existing.invoiceNumber)}` : "New bill"}</h3><button class="modal-close">&times;</button></div>
        <form class="modal-form">
          <div class="modal-body">
            ${existing ? "" : `<div class="form-grid">
              ${field({ name: "patientId", label: "Patient", type: "select", required: true, options: patientsCache.map(p => ({ value: p.id, label: `${p.name} (#${p.id})` })) })}
            </div>`}
            <h4 class="section-title" style="margin-top:${existing ? "0" : "18px"};">Line items</h4>
            <div id="bill-items"></div>
            <button type="button" class="btn btn-ghost btn-sm" id="add-item" style="margin-top:8px;">+ Add item</button>
          </div>
          <div class="modal-foot">
            <button type="button" class="btn btn-ghost cancel-btn">Cancel</button>
            <button type="submit" class="btn btn-primary">${existing ? "Save changes" : "Create bill"}</button>
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

  const itemsWrap = overlay.querySelector("#bill-items");

  function addItemRow(item) {
    const row = el(`
      <div class="form-grid rx-item-row" style="margin-bottom:10px; align-items:end;">
        <div class="field"><label>Description</label><input type="text" name="description" value="${escapeHtml(item?.description || "")}" required /></div>
        <div class="field"><label>Quantity</label><input type="number" name="quantity" min="1" value="${item?.quantity ?? 1}" required /></div>
        <div class="field"><label>Unit price (LKR)</label><input type="number" name="unitPrice" min="0" step="0.01" value="${item?.unitPrice ?? ""}" required /></div>
        <div class="field"><button type="button" class="btn btn-danger btn-sm remove-item">Remove</button></div>
      </div>`);
    row.querySelector(".remove-item").addEventListener("click", () => row.remove());
    itemsWrap.appendChild(row);
  }

  if (existing && existing.items && existing.items.length) {
    existing.items.forEach(addItemRow);
  } else {
    addItemRow();
  }
  overlay.querySelector("#add-item").addEventListener("click", () => addItemRow());

  overlay.querySelector(".modal-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const items = Array.from(itemsWrap.querySelectorAll(".rx-item-row")).map(row => ({
      description: row.querySelector("[name=description]").value,
      quantity: Number(row.querySelector("[name=quantity]").value),
      unitPrice: Number(row.querySelector("[name=unitPrice]").value),
    }));
    if (!items.length) { toast("Add at least one line item.", "error"); return; }
    const submitBtn = e.target.querySelector("button[type=submit]");
    submitBtn.disabled = true;
    try {
      if (existing) {
        await api.put(`/api/bills/${existing.id}`, { items });
        toast("Bill updated.", "success");
      } else {
        await api.post("/api/bills", { patientId: Number(fd.get("patientId")), items });
        toast("Bill created.", "success");
      }
      close();
      await renderBilling(document.getElementById("page-content"));
    } catch (err) {
      toast(err.message, "error");
    } finally {
      submitBtn.disabled = false;
    }
  });
}

async function deleteBill(id, body) {
  if (!confirmAction("Delete this bill? This is only possible if no payments have been made against it.")) return;
  try {
    await api.del(`/api/bills/${id}`);
    toast("Bill deleted.", "success");
    await renderBills(body);
  } catch (err) {
    toast(err.message, "error");
  }
}

// ---------------------------------------------------------------------------
// Payments
// ---------------------------------------------------------------------------
async function renderPayments(body) {
  body.innerHTML =
    panelHead("Payments", "Record a payment against a bill, or review and correct payments already made.") +
    `<div class="two-col">
      <div>
        <h4 class="section-title">Process a payment</h4>
        <form id="pay-form" class="form-grid">
          ${field({ name: "billId", label: "Bill ID", type: "number", required: true })}
          ${field({ name: "amount", label: "Amount (LKR)", type: "number", step: "0.01", required: true })}
          ${field({
            name: "paymentMethod", label: "Method", type: "select", required: true,
            options: [
              { value: "CASH", label: "Cash" },
              { value: "CREDIT_CARD", label: "Credit card" },
              { value: "INSURANCE", label: "Insurance" },
            ],
          })}
          ${field({ name: "transactionReference", label: "Transaction reference", full: true })}
          <div class="field full"><button type="submit" class="btn btn-primary">Process payment</button></div>
        </form>
      </div>
      <div>
        <h4 class="section-title">Payments for a bill</h4>
        <form id="lookup-pay-form" class="form-grid" style="margin-bottom:14px;">
          ${field({ name: "billId", label: "Bill ID", type: "number", required: true })}
          <div class="field"><button type="submit" class="btn btn-ghost" style="margin-top:20px;">Search</button></div>
        </form>
        <div id="pay-result" class="small-note">No search yet.</div>
      </div>
    </div>`;

  body.querySelector("#pay-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    try {
      await api.post("/api/payments", {
        billId: Number(fd.get("billId")),
        amount: Number(fd.get("amount")),
        paymentMethod: fd.get("paymentMethod"),
        transactionReference: fd.get("transactionReference"),
        paymentDate: nowForBackend(),
      });
      toast("Payment processed.", "success");
      e.target.reset();
    } catch (err) {
      toast(err.message, "error");
    }
  });

  const runPaymentsLookup = async (billId) => {
    const out = body.querySelector("#pay-result");
    out.innerHTML = loadingLine("Fetching payments…");
    try {
      const payments = await api.get(`/api/payments/bill/${billId}`);
      out.innerHTML = renderTable({
        columns: [
          { key: "id", label: "ID", mono: true },
          { key: "amount", label: "Amount", render: r => fmtMoney(r.amount) },
          { key: "paymentMethod", label: "Method" },
          { key: "transactionReference", label: "Reference", mono: true },
          { key: "paymentDate", label: "Date", render: r => fmtDate(r.paymentDate) },
        ],
        rows: payments,
        actions: (row) => `
          <button class="btn btn-ghost btn-sm edit-pay" data-id="${row.id}">Edit</button>
          <button class="btn btn-danger btn-sm del-pay" data-id="${row.id}">Delete</button>`,
        emptyMessage: "No payments recorded for this bill.",
      });
      out.querySelectorAll(".edit-pay").forEach(b =>
        b.addEventListener("click", () => openPaymentEditModal(payments.find(p => String(p.id) === b.dataset.id), () => runPaymentsLookup(billId))));
      out.querySelectorAll(".del-pay").forEach(b =>
        b.addEventListener("click", () => deletePayment(b.dataset.id, () => runPaymentsLookup(billId))));
    } catch (err) {
      out.innerHTML = errorBanner(err.message);
    }
  };

  body.querySelector("#lookup-pay-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    await runPaymentsLookup(fd.get("billId"));
  });
}

function openPaymentEditModal(payment, onDone) {
  const overlay = el(`
    <div class="modal-overlay">
      <div class="modal-box">
        <div class="modal-head"><h3>Edit payment #${payment.id}</h3><button class="modal-close">&times;</button></div>
        <form class="modal-form">
          <div class="modal-body">
            <div class="form-grid">
              ${field({ name: "amount", label: "Amount (LKR)", type: "number", step: "0.01", required: true, value: payment.amount })}
              ${field({
                name: "paymentMethod", label: "Method", type: "select", required: true, value: payment.paymentMethod,
                options: [
                  { value: "CASH", label: "Cash" },
                  { value: "CREDIT_CARD", label: "Credit card" },
                  { value: "INSURANCE", label: "Insurance" },
                ],
              })}
              ${field({ name: "transactionReference", label: "Transaction reference", full: true, value: payment.transactionReference })}
            </div>
          </div>
          <div class="modal-foot">
            <button type="button" class="btn btn-ghost cancel-btn">Cancel</button>
            <button type="submit" class="btn btn-primary">Save changes</button>
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

  overlay.querySelector(".modal-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    try {
      await api.put(`/api/payments/${payment.id}`, {
        amount: Number(fd.get("amount")),
        paymentMethod: fd.get("paymentMethod"),
        transactionReference: fd.get("transactionReference"),
      });
      toast("Payment updated.", "success");
      close();
      await onDone();
    } catch (err) {
      toast(err.message, "error");
    }
  });
}

async function deletePayment(id, onDone) {
  if (!confirmAction("Delete this payment? The bill's paid balance will be adjusted back.")) return;
  try {
    await api.del(`/api/payments/${id}`);
    toast("Payment deleted.", "success");
    await onDone();
  } catch (err) {
    toast(err.message, "error");
  }
}
