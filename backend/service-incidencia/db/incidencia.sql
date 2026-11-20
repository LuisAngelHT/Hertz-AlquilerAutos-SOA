-- SVC-05 ServicioIncidencia
-- Ejecutar en Supabase SQL Editor como administrador.
-- Reemplaza la clave de ejemplo y no la subas a Git.
-- Es repetible: usa "if not exists" y vuelve a aplicar permisos y políticas sin romper nada.
-- Refleja la tabla que ya existe en Supabase (se leyó con information_schema) más el permiso
-- para cerrar incidencias (UPDATE solo sobre la columna "estado").

create schema if not exists incidencia;

do $$
begin
  if not exists (select 1 from pg_roles where rolname = 'svc_incidencia') then
    create role svc_incidencia login password 'CAMBIA_ESTA_CLAVE';
  end if;
end
$$;

alter role svc_incidencia set search_path = incidencia;
grant usage on schema incidencia to svc_incidencia;
revoke all on schema incidencia from anon, authenticated;

create table if not exists incidencia.incidencia (
  id                      uuid primary key default gen_random_uuid(),
  placa                   varchar(10) not null,
  sucursal                varchar(40) not null,
  id_contrato             uuid,
  tipo                    varchar(15) not null
                          check (tipo in ('DAÑO', 'MANTENIMIENTO', 'SINIESTRO')),
  descripcion             varchar(500) not null,
  costo_estimado          numeric check (costo_estimado >= 0),
  estado                  varchar(15) not null default 'ABIERTA'
                          check (estado in ('ABIERTA', 'CERRADA')),
  estado_vehiculo_previo  varchar(20),
  registrada_por          varchar(120) not null,
  creado_en               timestamptz not null default now()
);

create index if not exists idx_incidencia_placa
  on incidencia.incidencia (placa, creado_en desc);

create index if not exists idx_incidencia_sucursal
  on incidencia.incidencia (sucursal, estado, creado_en desc);

-- Permisos: registrar (INSERT) y consultar (SELECT). Cerrar una incidencia solo cambia "estado",
-- así que el UPDATE se concede únicamente sobre esa columna. Nunca se borra.
grant select, insert on incidencia.incidencia to svc_incidencia;
grant update (estado) on incidencia.incidencia to svc_incidencia;
revoke delete, truncate on incidencia.incidencia from svc_incidencia;

alter table incidencia.incidencia enable row level security;
drop policy if exists svc_incidencia_incidencia on incidencia.incidencia;
create policy svc_incidencia_incidencia
  on incidencia.incidencia
  for all to svc_incidencia
  using (true)
  with check (true);
