const STORAGE_KEY = "tripsplit.web.v1";
const WEB_VERSION = "0.1.8";
const CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const EXPENSE_TYPE_PRESETS = [
  "Groceries",
  "Food",
  "Gas",
  "Rent",
  "Lodging",
  "Transport",
  "Activities",
  "Tickets",
  "Drinks",
  "Supplies",
  "Other",
];

const app = document.querySelector("#app");
let trips = loadTrips();
let currentTripId = trips[0]?.id ?? null;
let currentMemberId = trips[0]?.members[0]?.id ?? null;
let selectedTab = "expenses";
let receiptItemSequence = 0;

importTripFromHash();
render();

if ("serviceWorker" in navigator) {
  navigator.serviceWorker.register("./sw.js").catch(() => {});
}

document.addEventListener("submit", (event) => {
  const form = event.target;
  if (!(form instanceof HTMLFormElement)) return;
  event.preventDefault();
  const data = Object.fromEntries(new FormData(form).entries());

  if (form.dataset.action === "create-trip") {
    createTrip(data.tripName, data.adminName);
  }

  if (form.dataset.action === "join-trip") {
    joinTrip(data.inviteCode, data.guestName);
  }

  if (form.dataset.action === "add-expense") {
    addExpense(form);
  }

  if (form.dataset.action === "add-guest") {
    addGuest(data.guestName);
  }
});

document.addEventListener("click", (event) => {
  const button = event.target.closest("[data-action]");
  if (!(button instanceof HTMLElement)) return;
  const action = button.dataset.action;
  const id = button.dataset.id;

  if (action === "add-receipt-item") {
    addReceiptItemRow(button.closest("form"));
    return;
  }
  if (action === "remove-receipt-item") {
    const form = button.closest("form");
    button.closest(".receipt-item-editor")?.remove();
    updateReceiptDifference(form);
    return;
  }
  if (button.closest("form")) return;

  if (action === "open-trip") openTrip(id);
  if (action === "show-entry") {
    currentTripId = null;
    currentMemberId = null;
    selectedTab = "expenses";
    render();
  }
  if (action === "set-tab") {
    selectedTab = id;
    render();
  }
  if (action === "promote") promote(id);
  if (action === "end-trip") endTrip();
  if (action === "copy-code") copyTripCode();
  if (action === "copy-link") copyShareLink();
});

document.addEventListener("change", (event) => {
  const target = event.target;
  if (target instanceof HTMLSelectElement && target.dataset.action === "switch-member") {
    currentMemberId = target.value;
    render();
  }
  if (target instanceof HTMLInputElement && target.name === "splitMode") {
    syncReceiptMode(target.form);
  }
  if (target instanceof HTMLInputElement && target.name === "receiptImage" && target.files?.[0]) {
    scanReceiptImage(target.form, target.files[0]);
  }
  if (target instanceof HTMLInputElement && target.name === "participant") {
    syncGroupSelection(target.form, target);
  }
  if (target instanceof HTMLInputElement && target.name === "itemParticipant") {
    updatePeopleSummary(target.closest(".people-menu"), "assigned");
  }
});

document.addEventListener("input", (event) => {
  const form = event.target.closest?.('form[data-action="add-expense"]');
  if (form) updateReceiptDifference(form);
});

function render() {
  const trip = currentTrip();
  const member = currentMember();

  if (!trip || !member) {
    app.innerHTML = entryTemplate();
    return;
  }

  app.innerHTML = homeTemplate(trip, member);
}

function entryTemplate() {
  return `
    <section class="topbar glass">
      <div class="brand">
        <div>
          <h1>TripSplit</h1>
          <span>Tropical group wallet</span>
        </div>
      </div>
    </section>
    <section class="entry-grid">
      <form class="card glass field-grid" data-action="create-trip">
        <h2>Create trip</h2>
        <label>Trip name<input name="tripName" autocomplete="off" required /></label>
        <label>Your name<input name="adminName" autocomplete="name" required /></label>
        <button type="submit">Create</button>
      </form>
      <form class="card glass field-grid" data-action="join-trip">
        <h2>Join by code</h2>
        <label>Invite code<input name="inviteCode" autocomplete="off" required /></label>
        <label>Your name<input name="guestName" autocomplete="name" required /></label>
        <button class="secondary" type="submit">Join</button>
      </form>
      ${savedTripsTemplate()}
    </section>
  `;
}

function savedTripsTemplate() {
  if (!trips.length) return "";
  return `
    <section class="card glass wide">
      <h2>Saved trips</h2>
      ${trips
        .map(
          (trip) => `
            <div class="saved-trip">
              <div class="row">
                <div>
                  <h3>${escapeHtml(trip.name)}</h3>
                  <span class="muted">${trip.members.length} people | ${trip.expenses.length} expenses</span>
                </div>
                <button class="secondary" data-action="open-trip" data-id="${trip.id}">Open</button>
              </div>
            </div>
          `,
        )
        .join("")}
    </section>
  `;
}

function homeTemplate(trip, member) {
  return `
    <section class="topbar glass">
      <div class="brand">
        <div>
          <h1>${escapeHtml(trip.name)}</h1>
          <span>Code ${escapeHtml(trip.code)}</span>
        </div>
      </div>
      <div class="actions">
        <span class="pill ${trip.isEnded ? "" : "live"}">${trip.isEnded ? "Ended" : "Active"}</span>
        <button class="ghost" data-action="show-entry">Trips</button>
      </div>
    </section>

    <section class="home-grid">
      <aside class="card glass stack">
        <label>Acting as
          <select data-action="switch-member">
            ${trip.members
              .map(
                (person) => `
                  <option value="${person.id}" ${person.id === member.id ? "selected" : ""}>
                    ${escapeHtml(person.name)}${person.isAdmin ? " Admin" : ""}
                  </option>
                `,
              )
              .join("")}
          </select>
        </label>
        <nav class="tabs glass">
          ${["expenses", "summary", "admin"]
            .map(
              (tab) => `
                <button class="${selectedTab === tab ? "active" : ""}" data-action="set-tab" data-id="${tab}">
                  ${titleCase(tab)}
                </button>
              `,
            )
            .join("")}
        </nav>
      </aside>
      <section class="stack">
        ${selectedTab === "expenses" ? expensesTemplate(trip, member) : ""}
        ${selectedTab === "summary" ? summaryTemplate(trip) : ""}
        ${selectedTab === "admin" ? adminTemplate(trip, member) : ""}
      </section>
    </section>
  `;
}

function expensesTemplate(trip, member) {
  return `
    <form class="card glass field-grid" data-action="add-expense">
      <h2>New expense</h2>
      <label>Expense type
        <div class="preset-grid">
          ${expenseTypePresetsTemplate(trip.isEnded)}
        </div>
      </label>
      <label>Custom note (optional)<input name="title" autocomplete="off" ${trip.isEnded ? "disabled" : ""} /></label>
      <label>Amount<input name="amount" inputmode="decimal" ${trip.isEnded ? "disabled" : "required"} /></label>
      <fieldset class="split-method">
        <legend>Split method</legend>
        <div class="mode-switch">
          <label>
            <input type="radio" name="splitMode" value="equal" checked ${trip.isEnded ? "disabled" : ""} />
            <span>Equal split</span>
          </label>
          <label>
            <input type="radio" name="splitMode" value="items" ${trip.isEnded ? "disabled" : ""} />
            <span>Receipt items</span>
          </label>
        </div>
      </fieldset>
      ${payerPickerTemplate(trip, member)}
      <details class="people-menu">
        <summary>${trip.members.length} people selected</summary>
        <div class="people-list">
          ${trip.members
            .map(
              (person) => `
                <label class="check-row">
                  <input type="checkbox" name="participant" value="${person.id}" checked ${trip.isEnded ? "disabled" : ""} />
                  <span>${escapeHtml(person.name)}</span>
                </label>
              `,
            )
            .join("")}
        </div>
      </details>
      <section class="receipt-builder" hidden>
        <div>
          <h3>Receipt items</h3>
          <p class="muted receipt-help">Every item starts shared by the selected group. Change only the exceptions.</p>
        </div>
        <div class="receipt-tools">
          <label class="upload-button">
            <span>Scan receipt</span>
            <input type="file" name="receiptImage" accept="image/*" capture="environment" ${trip.isEnded ? "disabled" : ""} />
          </label>
          <button class="secondary" type="button" data-action="add-receipt-item" ${trip.isEnded ? "disabled" : ""}>Add item</button>
        </div>
        <p class="scan-status muted" aria-live="polite"></p>
        <div class="receipt-items"></div>
        <div class="receipt-difference" aria-live="polite"></div>
      </section>
      <p class="form-status" role="alert"></p>
      <button type="submit" ${trip.isEnded ? "disabled" : ""}>Add expense</button>
    </form>

    <section class="card glass">
      <h2>Expenses</h2>
      ${
        trip.expenses.length
          ? trip.expenses
              .slice()
              .sort((a, b) => b.createdAt - a.createdAt)
              .map((expense) => expenseTemplate(trip, expense))
              .join("")
          : `<div class="empty">No expenses yet</div>`
      }
    </section>
  `;
}

function payerPickerTemplate(trip, member) {
  if (!member.isAdmin) {
    return `
      <input type="hidden" name="payerId" value="${member.id}" />
      <span class="muted">Paid by ${escapeHtml(member.name)}</span>
    `;
  }

  return `
    <label>Paid by
      <select name="payerId" ${trip.isEnded ? "disabled" : ""}>
        ${trip.members
          .map(
            (person) => `
              <option value="${person.id}" ${person.id === member.id ? "selected" : ""}>
                ${escapeHtml(person.name)}${person.isAdmin ? " Admin" : ""}
              </option>
            `,
          )
          .join("")}
      </select>
    </label>
  `;
}

function expenseTypePresetsTemplate(disabled) {
  return EXPENSE_TYPE_PRESETS.map(
    (type, index) => `
      <label class="preset-chip">
        <input
          type="radio"
          name="expenseType"
          value="${escapeHtml(type)}"
          ${index === 0 ? "checked" : ""}
          ${disabled ? "disabled" : ""}
        />
        <span>${escapeHtml(type)}</span>
      </label>
    `,
  ).join("");
}

function expenseTemplate(trip, expense) {
  return `
    <div class="expense">
      <div class="row">
        <div>
          <h3>${escapeHtml(expense.title)}</h3>
          <span class="muted">Paid by ${escapeHtml(memberName(trip, expense.payerId))}</span><br />
          <span class="muted">For ${expense.participantIds.map((id) => escapeHtml(memberName(trip, id))).join(", ")}</span>
          ${expense.items?.length ? `<br /><span class="receipt-count">${expense.items.length} receipt items</span>` : ""}
        </div>
        <span class="amount">${formatMoney(expense.amountCents)}</span>
      </div>
      ${expense.items?.length ? `
        <details class="expense-items">
          <summary>View item split</summary>
          <div>
            ${expense.items.map((item) => `
              <div class="expense-item-line">
                <span>
                  <strong>${escapeHtml(item.name)}</strong><br />
                  <small>${item.participantIds.map((id) => escapeHtml(memberName(trip, id))).join(", ")}</small>
                </span>
                <span class="item-amount">${formatMoney(item.amountCents)}</span>
              </div>
            `).join("")}
          </div>
        </details>
      ` : ""}
    </div>
  `;
}

function summaryTemplate(trip) {
  const balances = memberBalances(trip);
  const movements = transfers(trip);
  return `
    <section class="card glass">
      <h2>${trip.isEnded ? "Final settlement" : "Current settlement"}</h2>
      ${
        movements.length
          ? movements
              .map(
                (transfer) => `
                  <div class="settlement">
                    <h3>${escapeHtml(memberName(trip, transfer.fromMemberId))} pays ${escapeHtml(memberName(trip, transfer.toMemberId))}</h3>
                    <span class="amount">${formatMoney(transfer.amountCents)}</span>
                  </div>
                `,
              )
              .join("")
          : `<div class="empty">Everyone is even</div>`
      }
    </section>

    <section class="card glass">
      <h2>Balances</h2>
      ${balances
        .map((balance) => {
          const label =
            balance.balanceCents > 0
              ? `<span class="credit">Gets ${formatMoney(balance.balanceCents)}</span>`
              : balance.balanceCents < 0
                ? `<span class="debt">Pays ${formatMoney(-balance.balanceCents)}</span>`
                : `<span class="muted">Even</span>`;
          return `
            <div class="member-row row">
              <span>${escapeHtml(memberName(trip, balance.memberId))}</span>
              <strong>${label}</strong>
            </div>
          `;
        })
        .join("")}
    </section>
  `;
}

function adminTemplate(trip, member) {
  if (!member.isAdmin) {
    return `<section class="card glass"><h2>Only admins can manage this trip</h2></section>`;
  }

  return `
    <section class="card glass field-grid">
      <h2>Invite code</h2>
      <div class="code">${escapeHtml(trip.code)}</div>
      <div class="form-actions">
        <button class="secondary" data-action="copy-code" ${trip.isEnded ? "disabled" : ""}>Copy code</button>
        <button class="secondary" data-action="copy-link" ${trip.isEnded ? "disabled" : ""}>Copy web link</button>
      </div>
    </section>

    <form class="card glass field-grid" data-action="add-guest">
      <h2>Add guest</h2>
      <label>Guest name<input name="guestName" autocomplete="name" ${trip.isEnded ? "disabled" : "required"} /></label>
      <button type="submit" ${trip.isEnded ? "disabled" : ""}>Add guest</button>
    </form>

    <section class="card glass">
      <h2>Admins</h2>
      ${
        trip.members.filter((person) => !person.isAdmin).length
          ? trip.members
              .filter((person) => !person.isAdmin)
              .map(
                (person) => `
                  <div class="member-row row">
                    <span>${escapeHtml(person.name)}</span>
                    <button class="secondary" data-action="promote" data-id="${person.id}" ${trip.isEnded ? "disabled" : ""}>Promote</button>
                  </div>
                `,
              )
              .join("")
          : `<div class="empty">Everyone is an admin</div>`
      }
    </section>

    <button class="danger" data-action="end-trip" ${trip.isEnded ? "disabled" : ""}>
      ${trip.isEnded ? "Trip ended" : "End trip"}
    </button>
  `;
}

function createTrip(rawTripName, rawAdminName) {
  const name = rawTripName?.trim();
  const adminName = rawAdminName?.trim();
  if (!name || !adminName) return;

  const admin = { id: newId("member"), name: adminName, isAdmin: true };
  const trip = {
    id: newId("trip"),
    name,
    code: randomCode(new Set(trips.map((item) => item.code))),
    createdAt: Date.now(),
    isEnded: false,
    members: [admin],
    expenses: [],
  };

  trips = [...trips, trip];
  currentTripId = trip.id;
  currentMemberId = admin.id;
  selectedTab = "expenses";
  saveTrips();
  render();
}

function joinTrip(rawCode, rawName) {
  const code = rawCode?.trim().toUpperCase();
  const name = rawName?.trim();
  const trip = trips.find((item) => item.code.toUpperCase() === code);
  if (!trip || !name || trip.isEnded || trip.members.some((person) => person.name.toLowerCase() === name.toLowerCase())) return;

  const member = { id: newId("member"), name, isAdmin: false };
  trip.members.push(member);
  currentTripId = trip.id;
  currentMemberId = member.id;
  selectedTab = "expenses";
  saveTrips();
  render();
}

function addExpense(form) {
  const trip = currentTrip();
  const member = currentMember();
  if (!trip || !member || trip.isEnded) return;
  const data = new FormData(form);
  const expenseType = String(data.get("expenseType") ?? EXPENSE_TYPE_PRESETS[0]).trim();
  const customTitle = String(data.get("title") ?? "").trim();
  const title = customTitle || expenseType;
  const amountCents = parseAmountCents(String(data.get("amount") ?? ""));
  const requestedPayerId = String(data.get("payerId") ?? member.id);
  const payerId = member.isAdmin ? requestedPayerId : member.id;
  const participantIds = data.getAll("participant").map(String);
  const itemized = data.get("splitMode") === "items";
  const payerExists = trip.members.some((person) => person.id === payerId);
  if (!payerExists) return;
  if (!title || amountCents <= 0 || participantIds.length === 0) {
    setFormStatus(form, "Add an amount and choose who shared this expense.");
    return;
  }

  let items = [];
  if (itemized) {
    const parsed = [...form.querySelectorAll(".receipt-item-editor")].map((row) => ({
      id: row.dataset.itemId,
      name: row.querySelector('[name="itemName"]')?.value.trim() ?? "",
      amountCents: parseAmountCents(row.querySelector('[name="itemAmount"]')?.value ?? ""),
      participantIds: [...row.querySelectorAll('[name="itemParticipant"]:checked')].map((input) => input.value),
    }));
    if (!parsed.length) {
      setFormStatus(form, "Scan a receipt or add at least one item.");
      return;
    }
    if (parsed.some((item) => !item.name || item.amountCents <= 0 || !item.participantIds.length)) {
      setFormStatus(form, "Complete every item and choose who used it.");
      return;
    }
    const itemTotal = parsed.reduce((sum, item) => sum + item.amountCents, 0);
    if (itemTotal > amountCents) {
      setFormStatus(form, "Item totals are higher than the receipt total.");
      return;
    }
    items = parsed;
    if (itemTotal < amountCents) {
      items.push({
        id: newId("item"),
        name: "Tax, fees, and rounding",
        amountCents: amountCents - itemTotal,
        participantIds,
      });
    }
  }

  trip.expenses.push({
    id: newId("expense"),
    title,
    amountCents,
    payerId,
    participantIds,
    createdAt: Date.now(),
    items,
  });

  saveTrips();
  render();
}

function addGuest(rawName) {
  const trip = currentTrip();
  const member = currentMember();
  const name = rawName?.trim();
  if (!trip || !member?.isAdmin || trip.isEnded || !name) return;
  if (trip.members.some((person) => person.name.toLowerCase() === name.toLowerCase())) return;

  trip.members.push({ id: newId("member"), name, isAdmin: false });
  saveTrips();
  render();
}

function promote(memberId) {
  const trip = currentTrip();
  const member = currentMember();
  if (!trip || !member?.isAdmin || trip.isEnded) return;

  trip.members = trip.members.map((person) => (person.id === memberId ? { ...person, isAdmin: true } : person));
  saveTrips();
  render();
}

function endTrip() {
  const trip = currentTrip();
  const member = currentMember();
  if (!trip || !member?.isAdmin) return;

  trip.isEnded = true;
  saveTrips();
  render();
}

function openTrip(tripId) {
  const trip = trips.find((item) => item.id === tripId);
  if (!trip) return;
  currentTripId = trip.id;
  currentMemberId = trip.members[0]?.id ?? null;
  selectedTab = "expenses";
  render();
}

async function copyTripCode() {
  const trip = currentTrip();
  if (!trip) return;
  await navigator.clipboard?.writeText(trip.code);
}

async function copyShareLink() {
  const trip = currentTrip();
  if (!trip) return;
  const base = `${location.origin}${location.pathname}?v=${WEB_VERSION}`;
  await navigator.clipboard?.writeText(`${base}#trip=${encodeTrip(trip)}`);
}

function currentTrip() {
  return trips.find((trip) => trip.id === currentTripId) ?? null;
}

function currentMember() {
  return currentTrip()?.members.find((member) => member.id === currentMemberId) ?? null;
}

function memberName(trip, memberId) {
  return trip.members.find((member) => member.id === memberId)?.name ?? "Unknown";
}

function memberBalances(trip) {
  const balances = Object.fromEntries(trip.members.map((member) => [member.id, 0]));

  for (const expense of trip.expenses) {
    const participants = expense.participantIds.filter((id) => Object.hasOwn(balances, id));
    if (!participants.length || !Object.hasOwn(balances, expense.payerId) || expense.amountCents <= 0) continue;

    balances[expense.payerId] += expense.amountCents;
    const validItems = (expense.items ?? []).filter(
      (item) => item.amountCents > 0 && item.participantIds?.some((id) => Object.hasOwn(balances, id)),
    );
    const itemTotal = validItems.reduce((sum, item) => sum + item.amountCents, 0);
    if (validItems.length && itemTotal === expense.amountCents) {
      for (const item of validItems) subtractShares(balances, item.amountCents, item.participantIds);
    } else {
      subtractShares(balances, expense.amountCents, participants);
    }
  }

  return trip.members.map((member) => ({
    memberId: member.id,
    balanceCents: balances[member.id] ?? 0,
  }));
}

function subtractShares(balances, amountCents, participantIds) {
  const participants = [...new Set(participantIds)].filter((id) => Object.hasOwn(balances, id));
  if (!participants.length) return;
  const baseShare = Math.floor(amountCents / participants.length);
  let remainder = amountCents % participants.length;
  for (const participantId of participants) {
    const share = baseShare + (remainder > 0 ? 1 : 0);
    remainder = Math.max(0, remainder - 1);
    balances[participantId] -= share;
  }
}

function syncReceiptMode(form) {
  if (!form) return;
  const receiptBuilder = form.querySelector(".receipt-builder");
  if (receiptBuilder) receiptBuilder.hidden = new FormData(form).get("splitMode") !== "items";
}

function syncGroupSelection(form, changedInput) {
  if (!form) return;
  const currentGroup = [...form.querySelectorAll('[name="participant"]:checked')].map((input) => input.value);
  const previousGroup = changedInput.checked
    ? currentGroup.filter((id) => id !== changedInput.value)
    : [...currentGroup, changedInput.value];
  for (const row of form.querySelectorAll(".receipt-item-editor")) {
    const itemChecks = [...row.querySelectorAll('[name="itemParticipant"]')];
    const selected = itemChecks.filter((input) => input.checked).map((input) => input.value);
    if (sameIds(selected, previousGroup)) {
      itemChecks.forEach((input) => { input.checked = currentGroup.includes(input.value); });
      updatePeopleSummary(row.querySelector(".people-menu"), "assigned");
    }
  }
  updatePeopleSummary(changedInput.closest(".people-menu"), "selected");
}

function updatePeopleSummary(menu, suffix) {
  if (!menu) return;
  const count = menu.querySelectorAll('input[type="checkbox"]:checked').length;
  const noun = count === 1 ? "person" : "people";
  const summary = menu.querySelector("summary");
  if (summary) summary.textContent = `${count} ${noun} ${suffix}`;
}

function sameIds(left, right) {
  return left.length === right.length && left.every((id) => right.includes(id));
}

function addReceiptItemRow(form, item = {}) {
  if (!form) return;
  const trip = currentTrip();
  if (!trip) return;
  const groupIds = [...form.querySelectorAll('[name="participant"]:checked')].map((input) => input.value);
  const participantIds = item.participantIds?.length ? item.participantIds : groupIds;
  const itemId = item.id ?? `draft_${receiptItemSequence += 1}`;
  form.querySelector(".receipt-items")?.insertAdjacentHTML(
    "beforeend",
    `<article class="receipt-item-editor" data-item-id="${escapeHtml(itemId)}">
      <div class="receipt-item-fields">
        <label>Item<input name="itemName" value="${escapeHtml(item.name ?? "")}" autocomplete="off" /></label>
        <label>Item total<input name="itemAmount" value="${escapeHtml(item.amount ?? "")}" inputmode="decimal" /></label>
      </div>
      <details class="people-menu">
        <summary>${participantIds.length} people assigned</summary>
        <div class="people-list">
          ${trip.members.map((person) => `
            <label class="check-row">
              <input type="checkbox" name="itemParticipant" value="${person.id}" ${participantIds.includes(person.id) ? "checked" : ""} />
              <span>${escapeHtml(person.name)}</span>
            </label>
          `).join("")}
        </div>
      </details>
      <button class="remove-item" type="button" data-action="remove-receipt-item">Remove item</button>
    </article>`,
  );
  updateReceiptDifference(form);
}

async function scanReceiptImage(form, file) {
  if (!form) return;
  const status = form.querySelector(".scan-status");
  if (!window.Tesseract) {
    if (status) status.textContent = "Receipt scanner could not load. Add items manually.";
    return;
  }
  if (status) status.textContent = "Reading receipt on this device...";
  try {
    const result = await window.Tesseract.recognize(file, "eng", {
      logger: (progress) => {
        if (status && progress.status === "recognizing text") {
          status.textContent = `Reading receipt ${Math.round((progress.progress ?? 0) * 100)}%`;
        }
      },
    });
    const parsedItems = parseReceiptText(result.data.text);
    const detectedTotal = findReceiptTotalCents(result.data.text);
    form.querySelector(".receipt-items").innerHTML = "";
    parsedItems.forEach((item) => addReceiptItemRow(form, item));
    const amountInput = form.querySelector('[name="amount"]');
    if (amountInput && !amountInput.value && detectedTotal) amountInput.value = formatEditableAmount(detectedTotal);
    if (status) {
      status.textContent = parsedItems.length
        ? `Found ${parsedItems.length} items. Check every line before saving.`
        : "No item lines found. Add them manually.";
    }
    updateReceiptDifference(form);
  } catch {
    if (status) status.textContent = "Could not read this receipt. Try a clearer photo or add items manually.";
  }
}

function parseReceiptText(text) {
  const ignored = ["subtotal", "sub total", "total", "vat", "tax", "cash", "card", "visa", "mastercard", "change", "balance", "discount", "receipt", "invoice", "amount due", "tender", "payment"];
  return text.split(/\r?\n/).map((line) => line.trim()).map((line) => {
    const normalized = line.toLowerCase();
    if (line.length < 3 || ignored.some((word) => normalized.includes(word))) return null;
    const match = line.match(/(?:EGP|LE|L\.E\.?|\$)?\s*(-?\d[\d,]*(?:\.\d{1,2})?)\s*(?:EGP|LE|L\.E\.?)?$/i);
    if (!match) return null;
    const amountCents = parseAmountCents(match[1]);
    const name = line.slice(0, match.index).trim().replace(/^[\d]+\s*[xX]\s*/, "").replace(/^[\s\-:.*]+|[\s\-:.*]+$/g, "");
    return name.length >= 2 && /[A-Za-z]/.test(name) && amountCents > 0
      ? { name, amount: formatEditableAmount(amountCents) }
      : null;
  }).filter(Boolean);
}

function findReceiptTotalCents(text) {
  const totals = text.split(/\r?\n/).filter((line) => {
    const normalized = line.toLowerCase();
    return normalized.includes("total") && !normalized.includes("subtotal") && !normalized.includes("sub total");
  }).map((line) => line.match(/(?:EGP|LE|L\.E\.?|\$)?\s*(\d[\d,]*(?:\.\d{1,2})?)\s*(?:EGP|LE|L\.E\.?)?$/i))
    .filter(Boolean).map((match) => parseAmountCents(match[1])).filter((amount) => amount > 0);
  return totals.at(-1) ?? 0;
}

function updateReceiptDifference(form) {
  if (!form) return;
  const total = parseAmountCents(form.querySelector('[name="amount"]')?.value ?? "");
  const itemTotal = [...form.querySelectorAll('[name="itemAmount"]')]
    .reduce((sum, input) => sum + parseAmountCents(input.value), 0);
  const output = form.querySelector(".receipt-difference");
  if (!output || total <= 0) return;
  const difference = total - itemTotal;
  output.classList.toggle("error", difference < 0);
  output.textContent = difference > 0
    ? `Tax, fees, or unlisted difference: ${formatMoney(difference)}`
    : difference < 0
      ? `Items exceed the receipt by ${formatMoney(-difference)}`
      : "Items match the receipt total";
}

function setFormStatus(form, message) {
  const output = form.querySelector(".form-status");
  if (output) output.textContent = message;
}

function formatEditableAmount(cents) {
  return (cents / 100).toFixed(2).replace(/\.00$/, "");
}

function transfers(trip) {
  const debtors = memberBalances(trip)
    .filter((balance) => balance.balanceCents < 0)
    .map((balance) => ({ memberId: balance.memberId, amountCents: -balance.balanceCents }))
    .sort((a, b) => b.amountCents - a.amountCents);
  const creditors = memberBalances(trip)
    .filter((balance) => balance.balanceCents > 0)
    .map((balance) => ({ memberId: balance.memberId, amountCents: balance.balanceCents }))
    .sort((a, b) => b.amountCents - a.amountCents);
  const output = [];
  let debtorIndex = 0;
  let creditorIndex = 0;

  while (debtorIndex < debtors.length && creditorIndex < creditors.length) {
    const debtor = debtors[debtorIndex];
    const creditor = creditors[creditorIndex];
    const amount = Math.min(debtor.amountCents, creditor.amountCents);
    if (amount > 0) {
      output.push({
        fromMemberId: debtor.memberId,
        toMemberId: creditor.memberId,
        amountCents: amount,
      });
    }
    debtor.amountCents -= amount;
    creditor.amountCents -= amount;
    if (debtor.amountCents === 0) debtorIndex += 1;
    if (creditor.amountCents === 0) creditorIndex += 1;
  }

  return output;
}

function loadTrips() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? "[]");
  } catch {
    return [];
  }
}

function saveTrips() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(trips));
}

function importTripFromHash() {
  const match = location.hash.match(/trip=([^&]+)/);
  if (!match) return;
  try {
    const trip = decodeTrip(match[1]);
    const existingIndex = trips.findIndex((item) => item.id === trip.id || item.code === trip.code);
    if (existingIndex >= 0) {
      trips[existingIndex] = trip;
    } else {
      trips.push(trip);
    }
    currentTripId = trip.id;
    currentMemberId = trip.members[0]?.id ?? null;
    saveTrips();
  } catch {
    location.hash = "";
  }
}

function encodeTrip(trip) {
  return btoa(unescape(encodeURIComponent(JSON.stringify(trip))));
}

function decodeTrip(encoded) {
  return JSON.parse(decodeURIComponent(escape(atob(encoded))));
}

function randomCode(existingCodes) {
  for (let attempt = 0; attempt < 100; attempt += 1) {
    let code = "";
    for (let index = 0; index < 6; index += 1) {
      code += CODE_CHARS[Math.floor(Math.random() * CODE_CHARS.length)];
    }
    if (!existingCodes.has(code)) return code;
  }
  return newId("code").slice(-6).toUpperCase();
}

function newId(prefix) {
  return `${prefix}_${Date.now()}_${Math.floor(1000 + Math.random() * 9000)}`;
}

function parseAmountCents(value) {
  const amount = Number(value.trim().replaceAll(",", ""));
  if (!Number.isFinite(amount) || amount <= 0) return 0;
  return Math.round(amount * 100);
}

function formatMoney(cents) {
  return new Intl.NumberFormat(undefined, {
    style: "currency",
    currency: "USD",
  }).format(cents / 100);
}

function titleCase(value) {
  return value.charAt(0).toUpperCase() + value.slice(1);
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (char) => {
    const map = {
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#039;",
    };
    return map[char];
  });
}
