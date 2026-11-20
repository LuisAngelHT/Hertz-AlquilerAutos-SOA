-- SVC-04 ServicioContrato
-- Ejecutar en Supabase SQL Editor como administrador.
-- Reemplaza la clave de ejemplo y no la subas a Git.

create schema if not exists contrato;

do $$
begin
  if not exists (select 1 from pg_roles where rolname = 'svc_contrato') then
    create role svc_contrato login password 'CAMBIA_ESTA_CLAVE';
  end if;
end
$$;

alter role svc_contrato set search_path = contrato;
grant usage on schema contrato to svc_contrato;
revoke all on schema contrato from anon, authenticated;

create table if not exists contrato.contrato (
  id                   uuid primary key default gen_random_uuid(),
  id_reserva           uuid not null unique,
  id_cliente           uuid not null,
  placa                varchar(10) not null,
  sucursal_recojo      varchar(40) not null,
  sucursal_devolucion  varchar(40) not null,
  fecha_inicio         date not null,
  fecha_fin_prevista   date not null,
  fecha_devolucion     date,
  estado               varchar(15) not null default 'VIGENTE'
                       check (estado in ('VIGENTE', 'FINALIZADO')),
  registrado_por       varchar(120) not null,
  creado_en            timestamptz not null default now(),
  actualizado_en       timestamptz not null default now(),
  check (fecha_fin_prevista >= fecha_inicio),
  check (fecha_devolucion is null or fecha_devolucion >= fecha_inicio)
);

create index if not exists idx_contrato_cliente
  on contrato.contrato (id_cliente, creado_en desc);

create index if not exists idx_contrato_placa_estado
  on contrato.contrato (placa, estado);

create index if not exists idx_contrato_sucursal_recojo_estado
  on contrato.contrato (sucursal_recojo, estado, creado_en desc);

create index if not exists idx_contrato_sucursal_devolucion_estado
  on contrato.contrato (sucursal_devolucion, estado, creado_en desc);

grant select, insert, update on contrato.contrato to svc_contrato;
revoke delete, truncate on contrato.contrato from svc_contrato;

alter default privileges in schema contrato
  grant select, insert, update on tables to svc_contrato;

alter table contrato.contrato enable row level security;
drop policy if exists svc_contrato_contrato on contrato.contrato;
create policy svc_contrato_contrato
  on contrato.contrato
  for all to svc_contrato
  using (true)
  with check (true);
