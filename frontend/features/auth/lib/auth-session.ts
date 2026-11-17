import type { LoginResponse } from "@/features/auth/lib/auth-api";

export const SESSION_KEY = "hertz-auth-session";
const SESSION_EVENT = "hertz-auth-session-change";
let cachedRaw: string | null | undefined;
let cachedSession: LoginResponse | null = null;

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isLoginResponse(value: unknown): value is LoginResponse {
  return (
    isRecord(value) &&
    typeof value.token === "string" &&
    typeof value.tipo === "string" &&
    typeof value.expiraEn === "string" &&
    typeof value.rol === "string" &&
    typeof value.nombre === "string"
  );
}

export function saveSession(session: LoginResponse): void {
  const raw = JSON.stringify(session);
  window.sessionStorage.setItem(SESSION_KEY, raw);
  cachedRaw = raw;
  cachedSession = session;
  window.dispatchEvent(new Event(SESSION_EVENT));
}

export function getSessionSnapshot(): LoginResponse | null {
  if (typeof window === "undefined") {
    return null;
  }

  const raw = window.sessionStorage.getItem(SESSION_KEY);
  if (raw === cachedRaw) {
    return cachedSession;
  }

  cachedRaw = raw;
  cachedSession = null;
  if (!raw) {
    return cachedSession;
  }

  try {
    const value: unknown = JSON.parse(raw);
    if (
      !isLoginResponse(value) ||
      !Number.isFinite(Date.parse(value.expiraEn)) ||
      Date.parse(value.expiraEn) <= Date.now()
    ) {
      return null;
    }

    cachedSession = value;
    return cachedSession;
  } catch {
    return null;
  }
}

export function getServerSessionSnapshot(): LoginResponse | null {
  return null;
}

export function subscribeToSession(callback: () => void): () => void {
  window.addEventListener(SESSION_EVENT, callback);
  window.addEventListener("storage", callback);
  return () => {
    window.removeEventListener(SESSION_EVENT, callback);
    window.removeEventListener("storage", callback);
  };
}

export function clearSession(): void {
  window.sessionStorage.removeItem(SESSION_KEY);
  cachedRaw = null;
  cachedSession = null;
  window.dispatchEvent(new Event(SESSION_EVENT));
}
