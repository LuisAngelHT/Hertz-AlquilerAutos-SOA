-- =====================================================
-- SVC-02 ServicioReserva - esquema "reserva" en Supabase
-- Ejecutar en Supabase > SQL Editor (como postgres)
-- =====================================================

-- 1. Esquema y rol del servicio
--    ANTES de ejecutar: reemplaza CAMBIA_ESTA_CLAVE por la clave que elijas
--    para svc_reserva (distinta a la de los otros servicios). No la subas a git.
create schema if not exists reserva;

do $$
begin
  if not exists (select 1 from pg_roles where rolname = 'svc_reserva') then
    create role svc_reserva login password 'CAMBIA_ESTA_CLAVE';
  end if;
end
$$;

alter role svc_reserva set search_path = reserva;
grant usage on schema reserva to svc_reserva;
revoke all on schema reserva from anon, authenticated;

-- 2. Reservas
--    id_cliente apunta a cliente.cliente (otro servicio): se guarda sin llave foranea a proposito,
--    porque cada servicio es dueno de sus datos y se valida por la API de ServicioCliente.
--    "version" es el control de concurrencia optimista: dos cambios simultaneos a la misma
--    reserva no se pisan (el segundo recibe 409).
create table if not exists reserva.reserva (
  id                   uuid primary key,
  id_cliente           uuid        not null,
  categoria            varchar(20) not null
                       check (categoria in ('ECONOMICO', 'SUV', 'CAMIONETA', 'PREMIUM')),
  sucursal_recojo      varchar(40) not null,
  sucursal_devolucion  varchar(40) not null,
  fecha_inicio         date        not null,
  fecha_fin            date        not null,
  estado               varchar(20) not null default 'CONFIRMADA'
                       check (estado in ('CONFIRMADA', 'EN_CURSO', 'FINALIZADA', 'CANCELADA')),
  placa_asignada       varchar(10),
  creada_por           varchar(120) not null,
  creado_en            timestamptz not null default now(),
  actualizado_en       timestamptz not null default now(),
  version              bigint      not null default 0,
  check (fecha_fin >= fecha_inicio)
);

-- Consultas frecuentes: reservas de un cliente, y listados por estado / sucursal / fecha.
create index if not exists idx_reserva_cliente on reserva.reserva (id_cliente, fecha_inicio desc);
create index if not exists idx_reserva_estado_sucursal on reserva.reserva (estado, sucursal_recojo, fecha_inicio desc);

-- 3. Historial (auditoria): quien hizo cada cambio, con que rol y cuando. Solo se inserta.
create table if not exists reserva.historial_reserva (
  id              uuid primary key default gen_random_uuid(),
  id_reserva      uuid        not null references reserva.reserva (id),
  accion          varchar(20) not null,
  estado_anterior varchar(20),
  estado_nuevo    varchar(20) not null,
  usuario         varchar(120) not null,
  rol             varchar(30) not null,
  detalle         varchar(300),
  fecha           timestamptz not null default now()
);

create index if not exists idx_historial_reserva on reserva.historial_reserva (id_reserva, fecha desc);

-- 4. Permisos: el servicio lee y escribe reservas, pero el historial es solo de insertar y leer.
grant select, insert, update, delete on reserva.reserva to svc_reserva;
grant select, insert on reserva.historial_reserva to svc_reserva;
revoke update, delete, truncate on reserva.historial_reserva from svc_reserva;

-- 5. Seguridad por filas (RLS): Supabase avisa si una tabla queda sin RLS. Se activa y se da una politica
--    solo al rol del servicio; sin politica, ni el propio servicio podria leer ni escribir.
alter table reserva.reserva enable row level security;
alter table reserva.historial_reserva enable row level security;
drop policy if exists svc_reserva_reserva on reserva.reserva;
create policy svc_reserva_reserva on reserva.reserva for all to svc_reserva using (true) with check (true);
drop policy if exists svc_reserva_historial on reserva.historial_reserva;
create policy svc_reserva_historial on reserva.historial_reserva for all to svc_reserva using (true) with check (true);

-- 6. Datos del cliente en la reserva (quien alquilo): ver reserva_datos_cliente.sql
