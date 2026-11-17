import { ApiRequestError, requestJson } from "@/lib/api-client";

export interface LoginResponse {
  token: string;
  tipo: string;
  expiraEn: string;
  rol: string;
  nombre: string;
}

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
  try {
    const payload = await requestJson<unknown>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify({ correo: correo.trim(), password }),
    });

    if (!isLoginResponse(payload)) {
      throw new Error(
        "La respuesta de inicio de sesión no tiene el formato esperado.",
      );
    }

    return payload;
  } catch (error) {
    if (!(error instanceof ApiRequestError)) {
      throw error;
    }
    if (error.status >= 500) {
      throw new Error(
        `El servidor respondió con HTTP ${error.status}. Revisa la disponibilidad del gateway y del servicio de autenticación.`,
      );
    }
    if (error.code === "CREDENCIALES_INVALIDAS") {
      throw new Error("El correo o la contraseña no son correctos.");
    }
    if (error.code === "USUARIO_INACTIVO") {
      throw new Error("Tu cuenta está inactiva. Contacta al administrador.");
    }
    if (error.code === "SOLICITUD_INVALIDA") {
      throw new Error(error.message || "Revisa los datos ingresados.");
    }
    throw new Error(getErrorMessage({ mensaje: error.message }));
  }
}
