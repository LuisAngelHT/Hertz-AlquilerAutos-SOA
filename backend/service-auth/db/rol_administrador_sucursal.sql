-- =====================================================
-- SVC-07 ServicioAutenticacion - rol ADMINISTRADOR_SUCURSAL y sucursal del usuario
-- Ejecutar en Supabase > SQL Editor (como postgres). Es seguro volver a ejecutarlo.
-- 1) La columna rol admitia 20 caracteres y ADMINISTRADOR_SUCURSAL tiene 22: se amplia a 30.
-- 2) El check usuario_rol_check solo aceptaba AGENTE, ADMINISTRADOR y CLIENTE: se reemplaza.
-- 3) Se agrega la columna sucursal (opcional): es la sede del AGENTE o ADMINISTRADOR_SUCURSAL.
--    Los usuarios existentes quedan con sucursal NULL; el ADMINISTRADOR general no la necesita.
-- =====================================================
alter table autenticacion.usuario alter column rol type varchar(30);

alter table autenticacion.usuario drop constraint if exists usuario_rol_check;
alter table autenticacion.usuario add constraint usuario_rol_check
  check (rol in ('AGENTE', 'ADMINISTRADOR', 'ADMINISTRADOR_SUCURSAL', 'CLIENTE'));

alter table autenticacion.usuario add column if not exists sucursal varchar(40);
