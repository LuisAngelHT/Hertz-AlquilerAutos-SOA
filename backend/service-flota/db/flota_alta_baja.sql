-- =====================================================
-- SVC-03 ServicioFlota - alta y baja de vehículos
-- Ejecutar en Supabase > SQL Editor (como postgres), DESPUÉS de flota.sql y flota_historial.sql.
-- =====================================================
-- 1. Nuevo estado FUERA_DE_SERVICIO: el vehículo se dio de baja (vendido, siniestro total, fin de vida útil).
--    No se borra: conserva su historial y sus reservas pasadas. Solo el ADMINISTRADOR general lo da de baja.
alter table flota.vehiculo
  drop constraint vehiculo_estado_check,
  add constraint vehiculo_estado_check
      check (estado in ('DISPONIBLE', 'ALQUILADO', 'MANTENIMIENTO', 'FUERA_DE_SERVICIO'));

-- 2. El historial acepta el nuevo estado, y el alta de un vehículo no tiene estado anterior.
alter table flota.historial_vehiculo
  drop constraint historial_vehiculo_estado_anterior_check,
  drop constraint historial_vehiculo_estado_nuevo_check,
  add constraint historial_vehiculo_estado_anterior_check
      check (estado_anterior in ('DISPONIBLE', 'ALQUILADO', 'MANTENIMIENTO', 'FUERA_DE_SERVICIO')),
  add constraint historial_vehiculo_estado_nuevo_check
      check (estado_nuevo in ('DISPONIBLE', 'ALQUILADO', 'MANTENIMIENTO', 'FUERA_DE_SERVICIO')),
  alter column estado_anterior drop not null;
