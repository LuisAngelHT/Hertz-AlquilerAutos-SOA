-- SVC-05 ServicioIncidencia: ficha tecnica del taller (lo que registra el mecanico mientras el vehiculo esta en
-- MANTENIMIENTO). Ejecutar en Supabase SQL Editor como administrador. Es seguro repetirlo (if not exists).
-- Solo AGREGA tablas: no modifica nada de lo existente.
--
--   ficha_taller          diagnostico, dias en taller, costos y, al cerrar, lo que se reparo y el costo final
--   ficha_dano_interno    danos mecanicos (motor, frenos, transmision...)
--   ficha_dano_externo    danos de carroceria que el mecanico agrega sobre el dibujo (los de la devolucion
--                         se leen del contrato)
--   ficha_seguimiento     una nota por avance, con quien y cuando

create table if not exists incidencia.ficha_taller (
  id_incidencia         uuid primary key references incidencia.incidencia (id),
  diagnostico           varchar(1000),
  dias_estimados        integer check (dias_estimados is null or (dias_estimados >= 0 and dias_estimados <= 365)),
  fecha_salida_prevista date,
  costo_repuestos       numeric(10, 2) check (costo_repuestos is null or costo_repuestos >= 0),
  costo_mano_obra       numeric(10, 2) check (costo_mano_obra is null or costo_mano_obra >= 0),
  trabajo_realizado     varchar(1000),
  costo_final           numeric(10, 2) check (costo_final is null or costo_final >= 0),
  actualizado_por       varchar(120) not null,
  actualizado_en        timestamptz not null default now()
);

create table if not exists incidencia.ficha_dano_interno (
  id             uuid primary key,
  id_incidencia  uuid not null references incidencia.incidencia (id),
  orden          integer not null,
  sistema        varchar(20) not null
                 check (sistema in ('MOTOR', 'TRANSMISION', 'FRENOS', 'SUSPENSION', 'DIRECCION', 'ELECTRICO',
                                    'ENFRIAMIENTO', 'AIRE_ACONDICIONADO', 'ESCAPE', 'OTRO')),
  descripcion    varchar(300) not null,
  gravedad       varchar(10) not null check (gravedad in ('LEVE', 'MODERADO', 'GRAVE')),
  costo_estimado numeric(10, 2) check (costo_estimado is null or costo_estimado >= 0)
);

create table if not exists incidencia.ficha_dano_externo (
  id             uuid primary key,
  id_incidencia  uuid not null references incidencia.incidencia (id),
  orden          integer not null,
  vista          varchar(10) not null check (vista in ('IZQUIERDA', 'DERECHA', 'FRENTE', 'ATRAS', 'SUPERIOR')),
  pos_x          numeric(5, 2) not null check (pos_x between 0 and 100),
  pos_y          numeric(5, 2) not null check (pos_y between 0 and 100),
  tipo           varchar(12) not null check (tipo in ('RAYON', 'ABOLLADURA', 'ROTURA', 'MANCHA', 'OTRO')),
  descripcion    varchar(300) not null,
  gravedad       varchar(10) not null check (gravedad in ('LEVE', 'MODERADO', 'GRAVE')),
  costo_estimado numeric(10, 2) check (costo_estimado is null or costo_estimado >= 0)
);

create table if not exists incidencia.ficha_seguimiento (
  id             uuid primary key,
  id_incidencia  uuid not null references incidencia.incidencia (id),
  nota           varchar(500) not null,
  registrado_por varchar(120) not null,
  registrado_en  timestamptz not null default now()
);

create index if not exists idx_ficha_dano_interno_incidencia on incidencia.ficha_dano_interno (id_incidencia, orden);
create index if not exists idx_ficha_dano_externo_incidencia on incidencia.ficha_dano_externo (id_incidencia, orden);
create index if not exists idx_ficha_seguimiento_incidencia on incidencia.ficha_seguimiento (id_incidencia, registrado_en desc);

grant select, insert, update on incidencia.ficha_taller to svc_incidencia;
grant select, insert on incidencia.ficha_seguimiento to svc_incidencia;
-- Las listas de danos se reescriben completas al guardar la ficha (mientras la incidencia esta abierta): necesitan DELETE.
grant select, insert, update, delete on incidencia.ficha_dano_interno to svc_incidencia;
grant select, insert, update, delete on incidencia.ficha_dano_externo to svc_incidencia;
revoke truncate on incidencia.ficha_taller, incidencia.ficha_dano_interno, incidencia.ficha_dano_externo,
                   incidencia.ficha_seguimiento from svc_incidencia;

-- Igual que incidencia.incidencia: RLS con una politica para svc_incidencia (sin politica, las consultas fallan en silencio).
alter table incidencia.ficha_taller enable row level security;
drop policy if exists svc_incidencia_ficha_taller on incidencia.ficha_taller;
create policy svc_incidencia_ficha_taller on incidencia.ficha_taller for all to svc_incidencia using (true) with check (true);

alter table incidencia.ficha_dano_interno enable row level security;
drop policy if exists svc_incidencia_ficha_dano_interno on incidencia.ficha_dano_interno;
create policy svc_incidencia_ficha_dano_interno on incidencia.ficha_dano_interno for all to svc_incidencia using (true) with check (true);

alter table incidencia.ficha_dano_externo enable row level security;
drop policy if exists svc_incidencia_ficha_dano_externo on incidencia.ficha_dano_externo;
create policy svc_incidencia_ficha_dano_externo on incidencia.ficha_dano_externo for all to svc_incidencia using (true) with check (true);

alter table incidencia.ficha_seguimiento enable row level security;
drop policy if exists svc_incidencia_ficha_seguimiento on incidencia.ficha_seguimiento;
create policy svc_incidencia_ficha_seguimiento on incidencia.ficha_seguimiento for all to svc_incidencia using (true) with check (true);
