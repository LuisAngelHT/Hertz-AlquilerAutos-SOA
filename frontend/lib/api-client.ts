export const API_BASE_URL = (
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"
).replace(/\/+$/, "");

export class ApiRequestError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code?: string,
  ) {
    super(message);
    this.name = "ApiRequestError";
  }
}

export async function requestJson<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    credentials: "omit",
  });

  const body = await response.text();
  let payload: unknown;

  try {
    payload = body ? JSON.parse(body) : null;
  } catch {
    if (!response.ok) {
      throw new ApiRequestError(
        `El servidor respondió con HTTP ${response.status}.`,
        response.status,
      );
    }
    throw new Error("El servidor devolvió una respuesta que no se pudo leer.");
  }

  if (!response.ok) {
    const error = isRecord(payload) ? payload : {};
    throw new ApiRequestError(
      typeof error.mensaje === "string"
        ? error.mensaje
        : `La solicitud falló con HTTP ${response.status}.`,
      response.status,
      typeof error.codigo === "string" ? error.codigo : undefined,
    );
  }

  return payload as T;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}
