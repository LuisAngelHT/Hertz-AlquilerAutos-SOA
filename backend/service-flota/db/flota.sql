-- =====================================================
-- SVC-03 ServicioFlota - esquema "flota" en Supabase
-- Ejecutar en Supabase > SQL Editor (como postgres)
-- =====================================================

-- 1. Esquema y rol del servicio
--    ANTES de ejecutar: reemplaza CAMBIA_ESTA_CLAVE por la clave que elijas
--    para svc_flota (distinta a la de los otros servicios). No la subas a git.
create schema if not exists flota;

do $$
begin
  if not exists (select 1 from pg_roles where rolname = 'svc_flota') then
    create role svc_flota login password 'CAMBIA_ESTA_CLAVE';
  end if;
end
$$;

alter role svc_flota set search_path = flota;
grant usage on schema flota to svc_flota;
revoke all on schema flota from anon, authenticated;

-- Permisos por defecto para las tablas que cree postgres en este esquema
alter default privileges in schema flota
  grant select, insert, update, delete on tables to svc_flota;

-- 2. Vehiculos
create table if not exists flota.vehiculo (
  placa          varchar(10) primary key,
  categoria      varchar(20) not null
                 check (categoria in ('ECONOMICO', 'SUV', 'CAMIONETA', 'PREMIUM')),
  marca          varchar(40) not null,
  modelo         varchar(40) not null,
  anio           smallint    not null check (anio between 2000 and 2100),
  estado         varchar(20) not null default 'DISPONIBLE'
                 check (estado in ('DISPONIBLE', 'ALQUILADO', 'MANTENIMIENTO')),
  sucursal       varchar(40) not null,
  creado_en      timestamptz not null default now(),
  actualizado_en timestamptz not null default now()
);

create index if not exists idx_vehiculo_busqueda
  on flota.vehiculo (sucursal, categoria, estado);

-- 3. Bloqueos de fechas (reservas, contratos, mantenimientos)
create table if not exists flota.bloqueo_vehiculo (
  id           uuid primary key default gen_random_uuid(),
  placa        varchar(10) not null references flota.vehiculo (placa),
  fecha_inicio date        not null,
  fecha_fin    date        not null,
  motivo       varchar(20) not null
               check (motivo in ('RESERVA', 'CONTRATO', 'MANTENIMIENTO')),
  referencia   varchar(50),
  creado_en    timestamptz not null default now(),
  check (fecha_fin >= fecha_inicio)
);

create index if not exists idx_bloqueo_placa_fechas
  on flota.bloqueo_vehiculo (placa, fecha_inicio, fecha_fin);

-- 4. Permisos sobre las tablas ya creadas
grant select, insert, update, delete on all tables in schema flota to svc_flota;

-- 5. Datos de prueba
insert into flota.vehiculo (placa, categoria, marca, modelo, anio, estado, sucursal) values
  ('ABC-101', 'ECONOMICO', 'Kia',        'Rio',      2024, 'DISPONIBLE',    'LIMA_AEROPUERTO'),
  ('ABC-102', 'ECONOMICO', 'Hyundai',    'Accent',   2023, 'DISPONIBLE',    'LIMA_AEROPUERTO'),
  ('ABC-103', 'SUV',       'Toyota',     'RAV4',     2024, 'DISPONIBLE',    'LIMA_AEROPUERTO'),
  ('ABC-104', 'SUV',       'Nissan',     'Kicks',    2023, 'ALQUILADO',     'LIMA_AEROPUERTO'),
  ('ABC-105', 'PREMIUM',   'BMW',        'X3',       2024, 'DISPONIBLE',    'LIMA_AEROPUERTO'),
  ('LMA-201', 'ECONOMICO', 'Chevrolet',  'Sail',     2022, 'DISPONIBLE',    'LIMA_MIRAFLORES'),
  ('LMA-202', 'CAMIONETA', 'Toyota',     'Hilux',    2024, 'DISPONIBLE',    'LIMA_MIRAFLORES'),
  ('LMA-203', 'SUV',       'Hyundai',    'Tucson',   2023, 'MANTENIMIENTO', 'LIMA_MIRAFLORES'),
  ('CUS-301', 'CAMIONETA', 'Toyota',     'Hilux',    2023, 'DISPONIBLE',    'CUSCO_AEROPUERTO'),
  ('CUS-302', 'SUV',       'Suzuki',     'Vitara',   2024, 'DISPONIBLE',    'CUSCO_AEROPUERTO'),
  ('CUS-303', 'ECONOMICO', 'Kia',        'Picanto',  2022, 'ALQUILADO',     'CUSCO_AEROPUERTO'),
  ('AQP-401', 'SUV',       'Toyota',     'Corolla Cross', 2024, 'DISPONIBLE', 'AREQUIPA_AEROPUERTO'),
  ('AQP-402', 'CAMIONETA', 'Mitsubishi', 'L200',     2023, 'DISPONIBLE',    'AREQUIPA_AEROPUERTO'),
  ('AQP-403', 'PREMIUM',   'Audi',       'Q5',       2024, 'DISPONIBLE',    'AREQUIPA_AEROPUERTO'),
  ('AQP-404', 'ECONOMICO', 'Hyundai',    'Accent',   2023, 'DISPONIBLE',    'AREQUIPA_AEROPUERTO')
on conflict (placa) do nothing;

insert into flota.bloqueo_vehiculo (placa, fecha_inicio, fecha_fin, motivo, referencia)
select 'ABC-102', current_date + 3, current_date + 7, 'RESERVA', 'RES-DEMO-1'
where not exists (select 1 from flota.bloqueo_vehiculo where referencia = 'RES-DEMO-1');

-- =====================================================
-- 6. Si el rol svc_flota ya existia sin clave, asignala aparte:
--    alter role svc_flota password 'TU_CLAVE_AQUI';
-- =====================================================

-- =====================================================
-- 7. PRUEBAS DE AISLAMIENTO (ejecutar aparte)
-- =====================================================
-- set role svc_flota;
-- select placa, estado from flota.vehiculo;            -- OK
-- select correo from autenticacion.usuario;            -- ERROR: permission denied
-- reset role;
