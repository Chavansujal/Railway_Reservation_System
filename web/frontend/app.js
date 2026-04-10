const loginTab = document.getElementById("loginTab");
const registerTab = document.getElementById("registerTab");
const authTabs = document.getElementById("authTabs");

const loginForm = document.getElementById("loginForm");
const registerForm = document.getElementById("registerForm");
const dashboard = document.getElementById("dashboard");

const bookingForm = document.getElementById("bookingForm");
const bookingBody = document.getElementById("bookingBody");
const refreshBtn = document.getElementById("refreshBtn");
const logoutBtn = document.getElementById("logoutBtn");
const welcomeText = document.getElementById("welcomeText");
const message = document.getElementById("message");

const totalBookings = document.getElementById("totalBookings");
const nextJourney = document.getElementById("nextJourney");
const dashTabs = Array.from(document.querySelectorAll(".dash-tab"));
const dashSections = Array.from(document.querySelectorAll(".dash-section"));

const SESSION_KEY = "railway_user";
let currentUser = loadUser();

function setMessage(text, error = true) {
  message.textContent = text;
  message.classList.toggle("ok", !error && Boolean(text));
}

function showAuth(view) {
  [loginForm, registerForm, dashboard].forEach((el) => el.classList.remove("show"));
  authTabs.style.display = "grid";
  view.classList.add("show");
  loginTab.classList.toggle("active", view === loginForm);
  registerTab.classList.toggle("active", view === registerForm);
}

function showDashboard() {
  [loginForm, registerForm, dashboard].forEach((el) => el.classList.remove("show"));
  authTabs.style.display = "none";
  dashboard.classList.add("show");
}

function activateDashSection(sectionId) {
  dashSections.forEach((section) => section.classList.toggle("show", section.id === sectionId));
  dashTabs.forEach((tab) => tab.classList.toggle("active", tab.dataset.section === sectionId));
}

function loadUser() {
  try {
    const value = localStorage.getItem(SESSION_KEY);
    return value ? JSON.parse(value) : null;
  } catch {
    return null;
  }
}

function saveUser(user) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(user));
  currentUser = user;
}

function clearUser() {
  localStorage.removeItem(SESSION_KEY);
  currentUser = null;
}

async function postForm(url, data) {
  const body = new URLSearchParams(data);
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
    body
  });

  const payload = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(payload.error || "Request failed");
  }
  return payload;
}

function renderBookings(bookings) {
  bookingBody.innerHTML = "";

  if (!bookings.length) {
    bookingBody.innerHTML = `<tr><td colspan="6">No bookings found.</td></tr>`;
    return;
  }

  bookings.forEach((booking) => {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${booking.id}</td>
      <td>${booking.source}</td>
      <td>${booking.destination}</td>
      <td>${booking.journeyDate}</td>
      <td>${booking.seats}</td>
      <td><button type="button" class="cancel-btn" data-booking-id="${booking.id}">Cancel</button></td>
    `;
    bookingBody.appendChild(row);
  });
}

async function cancelBooking(bookingId) {
  if (!currentUser) return;

  const res = await fetch(`/api/bookings?userId=${encodeURIComponent(currentUser.id)}&bookingId=${encodeURIComponent(bookingId)}`, {
    method: "DELETE"
  });

  const payload = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(payload.error || "Could not cancel booking");
  }
}

function updateOverview(bookings) {
  totalBookings.textContent = String(bookings.length);

  if (!bookings.length) {
    nextJourney.textContent = "-";
    return;
  }

  const upcoming = [...bookings].sort((a, b) => new Date(a.journeyDate) - new Date(b.journeyDate));
  nextJourney.textContent = upcoming[0].journeyDate;
}

async function loadBookings() {
  if (!currentUser) return [];

  const res = await fetch(`/api/bookings?userId=${encodeURIComponent(currentUser.id)}`);
  const payload = await res.json();

  if (!res.ok) {
    throw new Error(payload.error || "Could not load bookings");
  }

  const bookings = payload.bookings || [];
  renderBookings(bookings);
  updateOverview(bookings);
  return bookings;
}

async function enterDashboard() {
  welcomeText.textContent = `Welcome, ${currentUser.name}`;
  showDashboard();
  activateDashSection("overviewSection");
  await loadBookings();
  setMessage("", false);
}

loginTab.addEventListener("click", () => {
  setMessage("", false);
  showAuth(loginForm);
});

registerTab.addEventListener("click", () => {
  setMessage("", false);
  showAuth(registerForm);
});

for (const tab of dashTabs) {
  tab.addEventListener("click", () => {
    activateDashSection(tab.dataset.section);
  });
}

bookingBody.addEventListener("click", async (event) => {
  const target = event.target;
  if (!(target instanceof HTMLElement)) return;

  if (!target.classList.contains("cancel-btn")) return;

  const bookingId = target.dataset.bookingId;
  if (!bookingId) return;

  const confirmed = window.confirm("Do you want to cancel this booking?");
  if (!confirmed) return;

  try {
    await cancelBooking(bookingId);
    await loadBookings();
    setMessage("Booking cancelled successfully.", false);
  } catch (err) {
    setMessage(err.message);
  }
});
registerForm.addEventListener("submit", async (event) => {
  event.preventDefault();

  const formData = Object.fromEntries(new FormData(registerForm).entries());
  try {
    await postForm("/api/register", formData);
    setMessage("Registration successful. Please login.", false);
    registerForm.reset();
    showAuth(loginForm);
  } catch (err) {
    setMessage(err.message);
  }
});

loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();

  const formData = Object.fromEntries(new FormData(loginForm).entries());
  try {
    const data = await postForm("/api/login", formData);
    saveUser(data.user);
    await enterDashboard();
  } catch (err) {
    setMessage(err.message);
  }
});

bookingForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!currentUser) return;

  const formData = Object.fromEntries(new FormData(bookingForm).entries());
  formData.userId = currentUser.id;

  try {
    await postForm("/api/bookings", formData);
    bookingForm.reset();
    await loadBookings();
    activateDashSection("historySection");
    setMessage("Ticket booked successfully.", false);
  } catch (err) {
    setMessage(err.message);
  }
});

refreshBtn.addEventListener("click", async () => {
  try {
    await loadBookings();
    setMessage("Booking history updated.", false);
  } catch (err) {
    setMessage(err.message);
  }
});

logoutBtn.addEventListener("click", () => {
  clearUser();
  setMessage("Logged out.", false);
  showAuth(loginForm);
});

(async function init() {
  if (currentUser) {
    try {
      await enterDashboard();
      return;
    } catch {
      clearUser();
    }
  }

  showAuth(loginForm);
})();

