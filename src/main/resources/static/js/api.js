// ============================================================================
// Medicore HMS — API client
// Talks to the real Spring Boot backend. No dummy/mock data anywhere: every
// screen in this app renders exactly what the backend returns.
// ============================================================================

// Change this if your backend runs on a different host/port.
export const API_BASE = window.MEDICORE_API_BASE || "http://localhost:8080";

const TOKEN_KEY = "medicore_token";
const USER_KEY = "medicore_user";
const ROLE_KEY = "medicore_role";

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}
export function getUsername() {
  return localStorage.getItem(USER_KEY);
}
export function getRole() {
  return localStorage.getItem(ROLE_KEY);
}
export function isAuthenticated() {
  return !!getToken();
}
export function setSession(token, username, role) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, username || "");
  localStorage.setItem(ROLE_KEY, role || "");
}
export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(ROLE_KEY);
}

class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

/**
 * Core request helper.
 * @param {string} path - e.g. "/api/patients"
 * @param {object} opts - { method, body, params, isSessionExpiredRedirect }
 */
async function request(path, opts = {}) {
  const { method = "GET", body, params } = opts;

  let url = API_BASE + path;
  if (params && Object.keys(params).length) {
    const qs = new URLSearchParams(
      Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== "")
    ).toString();
    if (qs) url += (url.includes("?") ? "&" : "?") + qs;
  }

  const headers = {};
  const token = getToken();
  if (token) headers["Authorization"] = "Bearer " + token;

  let fetchOpts = { method, headers };
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    fetchOpts.body = JSON.stringify(body);
  }

  let res;
  try {
    res = await fetch(url, fetchOpts);
  } catch (networkErr) {
    throw new ApiError(
      `Could not reach the backend at ${API_BASE}. Is the Spring Boot app running?`,
      0
    );
  }

  let payload = null;
  const text = await res.text();
  if (text) {
    try {
      payload = JSON.parse(text);
    } catch (e) {
      payload = null;
    }
  }

  if (res.status === 401) {
    if (!["/api/auth/login", "/api/auth/register", "/api/auth/reset-password"].includes(path)) {
      clearSession();
      window.location.hash = "#/login";
    }
    throw new ApiError(
      (payload && payload.message) || "Your session has expired. Please log in again.",
      res.status
    );
  }

  if (res.status === 403) {
    // Authenticated, just not allowed to do this particular thing — don't log
    // the person out, let the calling screen decide how to show the error.
    throw new ApiError(
      (payload && payload.message) || "You don't have permission to do that.",
      res.status
    );
  }

  if (!res.ok) {
    throw new ApiError((payload && payload.message) || `Request failed (${res.status})`, res.status);
  }

  if (payload && payload.success === false) {
    throw new ApiError(payload.message || "Request failed", res.status);
  }

  return payload ? payload.data : null;
}

export const api = {
  get: (path, params) => request(path, { method: "GET", params }),
  post: (path, body, params) => request(path, { method: "POST", body, params }),
  put: (path, body, params) => request(path, { method: "PUT", body, params }),
  patch: (path, body, params) => request(path, { method: "PATCH", body, params }),
  del: (path, params) => request(path, { method: "DELETE", params }),
};

export { ApiError };
