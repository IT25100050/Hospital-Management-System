import { api, setSession } from "../api.js";
import { toast, escapeHtml } from "../ui.js";

// Same mapping used in app.js — kept here too so the login redirect
// doesn't need to import the router module.
const ROLE_HOME = {
  ADMIN: "dashboard",
  DOCTOR: "doctor-dashboard",
  DOCTOR_RECORDS_MANAGER: "doctors",
  PHARMACIST: "pharmacy",
  LAB_TECHNICIAN: "laboratory",
  FINANCE_OFFICER: "billing",
  RECEPTIONIST: "appointments",
  PATIENT: "patient-home",
};

const CROSS_SVG = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round" stroke-linecap="round" aria-hidden="true"><path d="M9 3h6v6h6v6h-6v6H9v-6H3V9h6z"/></svg>`;

// Small stroke icon set used on the public home page.
const ICONS = {
  heart: '<path d="M12 21s-8-5.2-8-11a4.5 4.5 0 0 1 8-2.8A4.5 4.5 0 0 1 20 10c0 5.8-8 11-8 11z"/>',
  brain: '<path d="M9 4a3 3 0 0 0-3 3 3 3 0 0 0-2 5 3 3 0 0 0 2 5 3 3 0 0 0 3 3V4zM15 4a3 3 0 0 1 3 3 3 3 0 0 1 2 5 3 3 0 0 1-2 5 3 3 0 0 1-3 3V4z"/>',
  cells: '<circle cx="8" cy="9" r="4"/><circle cx="16" cy="15" r="4"/><circle cx="17" cy="6" r="2"/><circle cx="6" cy="18" r="1.5"/>',
  bolt: '<path d="M13 2 4 14h7l-1 8 9-12h-7z"/>',
  scan: '<rect x="3" y="4" width="18" height="12" rx="2"/><path d="M12 8v4M8 20h8M12 16v4"/>',
  family: '<circle cx="9" cy="7" r="3"/><path d="M3 21v-3a5 5 0 0 1 10 0v3"/><circle cx="18" cy="14" r="2"/><path d="M15 21v-1a3 3 0 0 1 6 0v1"/>',
  kidney: '<path d="M9 3C5 3 3 7 3 11s2 10 6 10c3 0 3-4 3-6 0-3-2-3-2-6s2-6-1-6zM15 3c4 0 6 4 6 8s-2 10-6 10"/>',
  drop: '<path d="M12 3s6 6.5 6 11a6 6 0 0 1-12 0c0-4.5 6-11 6-11z"/>',
  report: '<rect x="5" y="3" width="14" height="18" rx="2"/><path d="M9 8h6M9 12h6M9 16h3"/>',
  doctor: '<circle cx="12" cy="7" r="4"/><path d="M4 21v-2a6 6 0 0 1 6-6h4a6 6 0 0 1 6 6v2M12 14v4"/>',
  queue: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  form: '<rect x="5" y="3" width="14" height="18" rx="2"/><circle cx="12" cy="10" r="2.5"/><path d="M8 17c1-2 7-2 8 0"/>',
  pill: '<rect x="2.5" y="8.7" width="19" height="7" rx="3.5" transform="rotate(-35 12 12)"/><path d="m10 9 4 6"/>',
  pulse: '<path d="M3 12h4l2-5 4 10 2-5h6"/>',
  card: '<rect x="3" y="6" width="18" height="12" rx="2"/><path d="M3 10h18M7 15h4"/>',
  chat: '<path d="M4 5h16v11H9l-5 4z"/><path d="M8 9h8M8 12h5"/>',
  bone: '<path d="M7 7a2.5 2.5 0 1 1 3.5-3.5L20 13a2.5 2.5 0 1 1 3.5 3.5L14 7"/>',
  knife: '<path d="M4 20 16 8l3 3L7 23zM15 4l5 5"/>',
  child: '<circle cx="12" cy="6" r="3"/><path d="M12 9v6M7 11l5 2 5-2M9 22l3-7 3 7"/>',
  steth: '<path d="M5 3v5a4 4 0 0 0 8 0V3"/><path d="M9 12v2a5 5 0 0 0 10 0v-1"/><circle cx="19" cy="11" r="2"/>',
  phone: '<path d="M5 3h4l2 5-2.5 1.5a11 11 0 0 0 6 6L16 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 5a2 2 0 0 1 2-2z"/>',
  mail: '<rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/>',
  arrow: '<path d="M5 12h14M13 6l6 6-6 6"/>',
};
const ico = (name) =>
  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICONS[name] || ""}</svg>`;

// Public pages: contact strip + white nav bar (Home / Login / Register at the right).
// NOTE: phone numbers / e-mail below are placeholders — replace with the hospital's real ones.
function publicShell(active, innerHtml) {
  const link = (path, label) =>
    `<a href="#/${path}" class="pub-btn ${active === path ? "active" : ""}">${label}</a>`;
  return `
    <div class="public-shell">
      <div class="pub-utility">
        <div class="pub-utility-left">
          <span>${ico("phone")} 011 000 0000</span>
          <span class="sep"></span>
          <span>${ico("phone")} 011 000 0001</span>
          <span class="sep"></span>
          <span>${ico("mail")} info@medicore.lk</span>
        </div>
        <div class="pub-utility-right">
          <a href="#/login" class="util-emergency">${ico("phone")} Emergency</a>
          <a href="#/login" class="util-quick">Quick Contacts</a>
          <span class="util-social"><b>f</b><b>X</b><b>in</b></span>
        </div>
      </div>
      <header class="pub-topbar">
        <a href="#/" class="pub-brand">
          <span class="pub-brand-mark">${CROSS_SVG}</span>
          <span>Medi<em>Core</em></span>
        </a>
        <nav class="pub-actions">
          <a href="#/" class="pub-link ${active === "home" ? "active" : ""}">Home</a>
          ${link("login", "Login")}
          ${link("register", "Register")}
        </nav>
      </header>
      <main class="pub-main">${innerHtml}</main>
    </div>`;
}

// Public home page — shown when the app is opened and nobody is signed in.
// Section order follows the reference PDF.
export function renderLanding(container) {
  const centres = [
    ["heart", "MediCore Heart Centres"],
    ["brain", "MediCore Brain & Spine Centre"],
    ["cells", "MediCore Bone Marrow Transplant Centre"],
    ["bolt", "MediCore Stroke Centre"],
    ["scan", "MediCore Centre for Interventional Radiology"],
    ["family", "MediCore Mother & Baby Care"],
    ["kidney", "MediCore Kidney Transplant Centre"],
    ["drop", "MediCore Urology Services"],
  ];
  // Sample figures — replace with the hospital's real quality data.
  const quality = [
    ["94.82%", "Patient Satisfaction Rate on Services"],
    ["97.30%", "Compliance to Correct Patient Identification"],
    ["88.33%", "Hand Hygiene Compliance"],
    ["0.09%", "Rate of Hospital Acquired Infections"],
    ["0.17%", "Adverse Drug Reaction"],
    ["0.00%", "Rate of Hospital Acquired Bed Sores"],
    ["0.00%", "Rate of Patient Falls"],
  ];
  const stats = [
    ["800+", "Consultants"], ["3500+", "Consultations Per Day"], ["4250+", "Tests Offered"],
    ["14500+", "Tests Per Day"], ["800+", "Beds"],
  ];
  const services = [
    ["report", "DOWNLOAD LAB REPORTS", "Access your laboratory test results here with ease.", "login"],
    ["doctor", "CONSULTATION BOOKINGS", "Make your consultant channeling appointment here.", "login"],
    ["queue", "ONGOING NUMBER", "Monitor the ongoing number for your consultant to schedule your arrival at the hospital.", "login"],
    ["form", "PRE-REGISTRATION", "Save time by completing your registration here before your visit.", "register"],
    ["pill", "ONLINE PHARMACY", "Order your prescription medication and have them delivered to your doorstep.", "login"],
    ["pulse", "WELLNESS PACKAGES", "Explore our menu of health check packages to maintain your good health & wellbeing.", "login"],
    ["card", "PAYMENT PORTAL", "Make secure online payments for your medical bills.", "login"],
    ["chat", "PATIENT FEEDBACK", "Please share your experience with us to help ensure it's a good one always.", "login"],
  ];
  const specialties = [
    ["heart", "Cardiology"], ["bone", "Orthopaedics"], ["brain", "Neurology"], ["knife", "General Surgery"],
    ["child", "Paediatrics"], ["family", "Gynaecology & Maternity"], ["steth", "Internal Medicine"],
  ];

  container.innerHTML = publicShell("home", `
    <div class="home">

      <h1 class="home-heading">Channel Doctors at MediCore Hospital | 24/7 Online Booking Available</h1>

      <section class="centres">
        ${centres.map(([i, t]) => `
          <a href="#/login" class="centre-card">${ico(i)}<span>${t.toUpperCase()}</span></a>`).join("")}
      </section>

      <section class="home-wrap quality">
        <h3>Quality Data</h3>
        <div class="quality-grid">
          ${quality.map(([v, l]) => `
            <div class="quality-tile"><strong>${v}</strong><span>${l}</span></div>`).join("")}
        </div>
      </section>

      <section class="home-wrap why-us">
        <h3 class="why-title">WHY PATIENTS CHOOSE US</h3>
        <p>MediCore Health employs, consults, and partners with the most dedicated, skilled, and
        experienced healthcare professionals to offer some of the country's most advanced,
        evidence based clinical programmes for treating complex diseases, through our Centres of
        Excellence. We have a sound record for offering outstanding outcomes.</p>
        <p>MediCore Health also offers treatment for increasingly common lifestyle-based diseases,
        preventive healthcare, and the most complete menu of diagnostic tests.</p>
        <p>All MediCore Health Hospitals have international accreditation.</p>
      </section>

      <section class="stats-strip">
        ${stats.map(([v, l]) => `<div><strong>${v}</strong><span>${l}</span></div>`).join("")}
      </section>

      <section class="online">
        <div class="home-wrap">
          <p class="online-eyebrow">Caring for the health of you and your family</p>
          <h2>Use The Convenience Of Our Online Services Here</h2>
          <div class="online-grid">
            ${services.map(([i, t, d, to]) => `
              <a href="#/${to}" class="online-card">${ico(i)}<h4>${t}</h4><p>${d}</p></a>`).join("")}
          </div>
        </div>
      </section>

      <section class="find-doc">
        <div class="find-card">
          <h2>Find A Doctor. Book An Appointment. Pay Easy.</h2>
          <p>Simplify your healthcare experience with ease.</p>
          <a href="#/login" class="find-cta">${ico("doctor")} MAKE AN APPOINTMENT</a>
        </div>
      </section>

      <section class="home-wrap specialties">
        <div class="spec-head">
          <div>
            <span class="spec-eyebrow">MEDICAL SPECIALTIES</span>
            <h2>Find the right specialist for your care</h2>
          </div>
          <p>Explore our key medical specialties and connect with an experienced consultant through MediCore Hospital.</p>
        </div>
        <div class="spec-grid">
          ${specialties.map(([i, t]) => `
            <a href="#/login" class="spec-card"><span class="spec-ico">${ico(i)}</span><span class="spec-name">${t}</span><span class="spec-go">${ico("arrow")}</span></a>`).join("")}
        </div>
      </section>

      <footer class="home-footer">
        <div class="home-wrap foot-cols">
          <div class="foot-about">
            <div class="foot-brand"><span class="pub-brand-mark">${CROSS_SVG}</span><span>Medi<em>Core</em></span><small>HEALTH</small></div>
            <p>With international accreditation for patient safety and care, MediCore Health is a leading healthcare provider.
            Our unwavering commitment to compassionate patient care, innovation and outstanding patient outcomes has earned us
            the high position of trust we enjoy.</p>
            <a href="#/login" class="foot-contact">Contact us ${ico("arrow")}</a>
            <div class="foot-follow"><span>FOLLOW US ON</span><span class="util-social"><b>f</b><b>X</b><b>in</b></span></div>
            <a href="#/" class="foot-privacy">Privacy Policy</a>
            <div class="foot-copy">© MEDICORE HEALTH 2026 All Rights Reserved.</div>
          </div>
          <div>
            <h4>MEDICORE HEALTH NETWORK</h4>
            <div class="foot-links two">
              <a href="#/">MediCore Colombo</a><a href="#/">MediCore Kandy</a>
              <a href="#/">MediCore Galle</a><a href="#/">MediCore Matara</a>
              <a href="#/">MediCore Laboratories</a>
            </div>
            <h4 class="gap">NETWORK CONTACTS</h4>
            <div class="foot-links">
              <span>MediCore Colombo : 011 000 0000</span><span>MediCore Kandy : 081 000 0000</span>
              <span>MediCore Galle : 091 000 0000</span><span>MediCore Matara : 041 000 0000</span>
              <span>MediCore Laboratories : 011 000 0002</span>
            </div>
          </div>
          <div>
            <h4>CENTRES OF EXCELLENCE</h4>
            <div class="foot-links two">
              <a href="#/login" class="hot">Accident and Emergency</a><a href="#/login">Interventional Radiology</a>
              <a href="#/login">Heart Centres</a><a href="#/login">Kidney Transplant Centres</a>
              <a href="#/login">Brain and Spine Centre</a><a href="#/login">Urology Services</a>
              <a href="#/login">Blood Cancer & Bone Marrow</a><a href="#/login">Mother and Baby Care</a>
              <a href="#/login">Stroke Centre</a>
            </div>
          </div>
        </div>
        <div class="home-wrap quick-contacts">
          <h3>Quick Contacts</h3>
          <p>If you have any questions or need help, feel free to contact us for medical assistance.</p>
          <div class="qc-emerg">24/7 Emergency</div>
          <div class="qc-hotline">Hotline: 0000</div>
        </div>
      </footer>

    </div>`);
}

export function renderLogin(container) {
  container.innerHTML = publicShell("login", `
    <div class="auth-main">
        <div class="auth-card">
          <h1>Sign in</h1>
          <p class="sub">Sign in to your MediCore account.</p>
          <div id="auth-error"></div>
          <form id="login-form">
            <div class="field" style="margin-bottom:14px;">
              <label for="username">Username</label>
              <input id="username" name="username" type="text" required autofocus />
            </div>
            <div class="field" style="margin-bottom:14px;">
              <label for="password">Password</label>
              <input id="password" name="password" type="password" required />
            </div>
            <button type="submit" class="btn btn-primary" style="width:100%; justify-content:center;">Sign in</button>
          </form>
          <div class="auth-switch">
            New to MediCore? <button id="go-register">Create a patient account</button>
          </div>
        </div>
    </div>`);

  container.querySelector("#login-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const errBox = container.querySelector("#auth-error");
    errBox.innerHTML = "";
    const btn = e.target.querySelector("button[type=submit]");
    btn.disabled = true;
    btn.textContent = "Signing in…";
    try {
      const data = await api.post("/api/auth/login", {
        username: fd.get("username"),
        password: fd.get("password"),
      });
      setSession(data.token, data.username, data.role);
      toast(`Welcome back, ${data.username}.`, "success");
      window.location.hash = "#/" + (ROLE_HOME[data.role] || "dashboard");
    } catch (err) {
      errBox.innerHTML = `<div class="error-banner">${escapeHtml(err.message)}</div>`;
    } finally {
      btn.disabled = false;
      btn.textContent = "Sign in";
    }
  });

  container.querySelector("#go-register").addEventListener("click", () => {
    window.location.hash = "#/register";
  });
}

export function renderRegister(container) {
  container.innerHTML = publicShell("register", `
    <div class="auth-main">
        <div class="auth-card">
          <h1>Create patient account</h1>
          <p class="sub">Register securely to book and manage your appointments.</p>
          <div id="auth-error"></div>
          <form id="register-form">
            <div class="field" style="margin-bottom:14px;">
              <label for="username">Full name</label>
              <input id="username" name="username" type="text" required minlength="2" maxlength="100" autofocus />
            </div>
            <div class="field" style="margin-bottom:14px;">
              <label for="email">Email</label>
              <input id="email" name="email" type="email" required />
            </div>
            <div class="field" style="margin-bottom:14px;">
              <label for="password">Password</label>
              <input id="password" name="password" type="password" required minlength="8" />
            </div>
            <button type="submit" class="btn btn-primary" style="width:100%; justify-content:center;">Create patient account</button>
          </form>
          <div class="auth-switch" style="margin-top:16px;">
            Medical professional? <a href="#/doctor-register">Register as a Doctor</a>
          </div>
          <div class="auth-switch">
            Already registered? <button id="go-login">Sign in</button>
          </div>
        </div>
    </div>`);

  container.querySelector("#register-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const errBox = container.querySelector("#auth-error");
    errBox.innerHTML = "";
    const btn = e.target.querySelector("button[type=submit]");
    btn.disabled = true;
    btn.textContent = "Creating…";
    try {
      await api.post("/api/auth/register", {
        username: fd.get("username"),
        email: fd.get("email"),
        password: fd.get("password"),
      });
      toast("Patient account created. Please sign in.", "success");
      window.location.hash = "#/login";
    } catch (err) {
      errBox.innerHTML = `<div class="error-banner">${escapeHtml(err.message)}</div>`;
    } finally {
      btn.disabled = false;
      btn.textContent = "Create patient account";
    }
  });

  container.querySelector("#go-login").addEventListener("click", () => {
    window.location.hash = "#/login";
  });
}

export function renderDoctorRegister(container) {
  container.innerHTML = publicShell("register", `
    <div class="auth-main">
      <div class="auth-card">
        <h1>Register as a Doctor</h1>
        <p class="sub">Your account will be created after hospital management approves your application.</p>
        <div id="auth-error"></div>
        <form id="doctor-register-form">
          <div class="field" style="margin-bottom:14px;"><label for="name">Full name</label><input id="name" name="name" required maxlength="100" /></div>
          <div class="field" style="margin-bottom:14px;"><label for="email">Email</label><input id="email" name="email" type="email" required maxlength="254" /></div>
          <div class="field" style="margin-bottom:14px;"><label for="password">Password</label><input id="password" name="password" type="password" required minlength="8" /></div>
          <div class="field" style="margin-bottom:14px;"><label for="phoneNumber">Phone number</label><input id="phoneNumber" name="phoneNumber" required pattern="[0-9+() -]{7,20}" /></div>
          <div class="field" style="margin-bottom:14px;"><label for="medicalRegistrationNumber">Medical registration number</label><input id="medicalRegistrationNumber" name="medicalRegistrationNumber" required maxlength="100" /></div>
          <div class="field" style="margin-bottom:14px;"><label for="specialty">Specialty</label><input id="specialty" name="specialty" required maxlength="100" /></div>
          <div class="field" style="margin-bottom:14px;"><label for="qualifications">Qualifications</label><textarea id="qualifications" name="qualifications" required maxlength="500"></textarea></div>
          <div class="field" style="margin-bottom:18px;"><label for="experience">Experience (years)</label><input id="experience" name="experience" type="number" min="0" max="80" required /></div>
          <button type="submit" class="btn btn-primary" style="width:100%;justify-content:center;">Submit application</button>
        </form>
        <div class="auth-switch">Already have an account? <a href="#/login">Sign in</a></div>
      </div>
    </div>`);

  container.querySelector("#doctor-register-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const error = container.querySelector("#auth-error");
    const button = form.querySelector("button[type=submit]");
    error.innerHTML = "";
    button.disabled = true;
    button.textContent = "Submitting…";
    try {
      const formData = new FormData(form);
      await api.post("/api/doctor-applications", {
        name: formData.get("name"),
        email: formData.get("email"),
        password: formData.get("password"),
        phoneNumber: formData.get("phoneNumber"),
        medicalRegistrationNumber: formData.get("medicalRegistrationNumber"),
        specialty: formData.get("specialty"),
        qualifications: formData.get("qualifications"),
        experience: Number(formData.get("experience")),
      });
      toast("Your doctor registration is pending approval.", "success");
      container.innerHTML = publicShell("register", `<div class="auth-main"><div class="auth-card"><h1>Application submitted</h1><p class="sub">Your doctor registration is pending approval.</p><a class="btn btn-primary" href="#/login">Return to sign in</a></div></div>`);
    } catch (err) {
      error.innerHTML = `<div class="error-banner">${escapeHtml(err.message)}</div>`;
    } finally {
      if (button.isConnected) {
        button.disabled = false;
        button.textContent = "Submit application";
      }
    }
  });
}

export function renderResetPassword(container) {
  container.innerHTML = publicShell("", `
    <div class="auth-main">
        <div class="auth-card">
          <h1>Reset password</h1>
          <p class="sub">Confirm your current password, then choose a new one.</p>
          <div id="auth-error"></div>
          <form id="reset-form">
            <div class="field" style="margin-bottom:14px;">
              <label for="username">Username</label>
              <input id="username" name="username" type="text" required autofocus />
            </div>
            <div class="field" style="margin-bottom:14px;">
              <label for="oldPassword">Current password</label>
              <input id="oldPassword" name="oldPassword" type="password" required />
            </div>
            <div class="field" style="margin-bottom:14px;">
              <label for="newPassword">New password</label>
              <input id="newPassword" name="newPassword" type="password" required minlength="6" />
              <span class="hint">At least 6 characters.</span>
            </div>
            <div class="field" style="margin-bottom:18px;">
              <label for="confirmPassword">Confirm new password</label>
              <input id="confirmPassword" name="confirmPassword" type="password" required minlength="6" />
            </div>
            <button type="submit" class="btn btn-primary" style="width:100%; justify-content:center;">Update password</button>
          </form>
          <p class="small-note" style="margin-top:14px; text-align:center;">
            Forgot your current password? Ask a hospital administrator to reset it for you.
          </p>
          <div class="auth-switch">
            Remembered it? <button id="go-login">Back to sign in</button>
          </div>
        </div>
    </div>`);

  container.querySelector("#reset-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const errBox = container.querySelector("#auth-error");
    errBox.innerHTML = "";
    if (fd.get("newPassword") !== fd.get("confirmPassword")) {
      errBox.innerHTML = `<div class="error-banner">New password and confirmation do not match.</div>`;
      return;
    }
    const btn = e.target.querySelector("button[type=submit]");
    btn.disabled = true;
    btn.textContent = "Updating…";
    try {
      await api.post("/api/auth/reset-password", {
        username: fd.get("username"),
        oldPassword: fd.get("oldPassword"),
        newPassword: fd.get("newPassword"),
      });
      toast("Password updated. Please sign in with your new password.", "success");
      window.location.hash = "#/login";
    } catch (err) {
      errBox.innerHTML = `<div class="error-banner">${escapeHtml(err.message)}</div>`;
    } finally {
      btn.disabled = false;
      btn.textContent = "Update password";
    }
  });

  container.querySelector("#go-login").addEventListener("click", () => {
    window.location.hash = "#/login";
  });
}
