-- =====================================================
-- SVC-07 ServicioAutenticacion - bloqueo de cuenta por intentos fallidos de login
-- Ejecutar en Supabase > SQL Editor (como postgres)
-- =====================================================
-- Tras 5 contraseñas incorrectas seguidas la cuenta se bloquea 15 minutos; un login correcto
-- reinicia el contador. Los usuarios existentes empiezan con 0 intentos y sin bloqueo.
alter table autenticacion.usuario
  add column if not exists intentos_fallidos integer not null default 0,
  add column if not exists bloqueado_hasta   timestamptz;
