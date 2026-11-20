-- Arreglos pendientes en Supabase (se detectaron probando la web desplegada el 2026-11-20).
-- Ejecutar en el SQL Editor como administrador. Es repetible: se puede correr más de una vez.
-- No contiene claves.

-- 1) CONTRATOS NO SE CREABAN.
--    La tabla contrato.contrato tenía la seguridad por filas (RLS) activada pero ninguna política
--    para svc_contrato, así que la base rechazaba el INSERT ("new row violates row-level security
--    policy for table contrato") y cualquier SELECT devolvía siempre 0 filas.
--    Es la misma política que ya define backend/service-contrato/db/contrato.sql.
drop policy if exists svc_contrato_contrato on contrato.contrato;
create policy svc_contrato_contrato
  on contrato.contrato
  for all to svc_contrato
  using (true)
  with check (true);

-- 2) CERRAR INCIDENCIAS.
--    svc_incidencia solo tenía INSERT y SELECT. Cerrar una incidencia cambia únicamente la columna
--    "estado", así que se concede UPDATE solo sobre esa columna (mínimo privilegio, y nunca DELETE).
grant update (estado) on incidencia.incidencia to svc_incidencia;

-- Verificación (opcional): debe mostrar la política de contrato y el permiso de UPDATE.
select 'politica' as k, policyname as v from pg_policies where schemaname = 'contrato'
union all
select 'permiso_update_incidencia', column_name || ':' || grantee
from information_schema.column_privileges
where table_schema = 'incidencia' and table_name = 'incidencia'
  and grantee = 'svc_incidencia' and privilege_type = 'UPDATE';
