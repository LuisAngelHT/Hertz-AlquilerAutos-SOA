export interface LoginResponse {
  token: string;
  tipo: string;
  expiraEn: string;
  rol: string;
  nombre: string;
}

const API_BASE_URL = (
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"
).replace(/\/+$/, "");

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

function getErrorMessage(error: Record<string, unknown>): string {
  const codigo = typeof error.codigo === "string" ? error.codigo : undefined;
  const mensaje = typeof error.mensaje === "string" ? error.mensaje : undefined;

  switch (codigo) {
    case "CREDENCIALES_INVALIDAS":
      return "El correo o la contraseña no son correctos.";
    case "USUARIO_INACTIVO":
      return "Tu cuenta está inactiva. Contacta al administrador.";
    case "SOLICITUD_INVALIDA":
      return mensaje ?? "Revisa los datos ingresados.";
    default:
      return mensaje ?? "No se pudo iniciar sesión. Inténtalo de nuevo.";
  }
}

export async function loginWithCredentials(
  correo: string,
  password: string,
): Promise<LoginResponse> {
  const response = await fetch(`${API_BASE_URL}/api/v1/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ correo: correo.trim(), password }),
    credentials: "omit",
  });

  const responseBody = await response.text();
  let payload: unknown;
  try {
    payload = responseBody ? JSON.parse(responseBody) : null;
  } catch {
    if (!response.ok) {
      throw new Error(
        `El servidor respondió con HTTP ${response.status}. Revisa la disponibilidad del gateway y del servicio de autenticación.`,
      );
    }
    throw new Error("El servidor devolvió una respuesta que no se pudo leer.");
  }

  if (!response.ok) {
    if (response.status >= 500) {
      throw new Error(
        `El servidor respondió con HTTP ${response.status}. Revisa la disponibilidad del gateway y del servicio de autenticación.`,
      );
    }
    throw new Error(getErrorMessage(isRecord(payload) ? payload : {}));
  }

  if (!isLoginResponse(payload)) {
    throw new Error("La respuesta de inicio de sesión no tiene el formato esperado.");
  }

  return payload;
}
