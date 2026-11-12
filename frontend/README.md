# Hertz frontend

Aplicación Next.js para el inicio de sesión del sistema Hertz. Utiliza Tailwind CSS 4 y componentes shadcn/ui.

## Desarrollo local

1. Instala dependencias con `npm install`.
2. Copia `.env.example` a `.env.local`.
3. Inicia el servidor con `npm run dev`.

El frontend envía `POST /api/v1/auth/login` al gateway. Por defecto, la API apunta a `http://localhost:8080`; para otros entornos configura `NEXT_PUBLIC_API_URL` con el origen público del gateway.

En desarrollo local, el gateway debe permitir el origen `http://localhost:3000` mediante `FRONTEND_URL`. En Render, configura `NEXT_PUBLIC_API_URL` con la URL del gateway y `FRONTEND_URL` con el origen público del frontend.

## Comprobaciones

```bash
npm run lint
npm run build
```
