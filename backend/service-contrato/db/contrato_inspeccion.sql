-- SVC-04 ServicioContrato: revision del vehiculo en la ENTREGA y en la DEVOLUCION.
-- Ejecutar en Supabase SQL Editor como administrador. Es seguro repetirlo (if not exists).
-- Solo AGREGA dos tablas: no modifica nada de lo existente.
--
-- Por cada contrato hay como maximo una revision de ENTREGA (como sale el vehiculo, incluidos los danos
-- que ya tenia) y una de DEVOLUCION (como vuelve; sus danos son los NUEVOS, causados durante el alquiler,
-- y quedan a cargo del cliente).

create table if not exists contrato.contrato_inspeccion (
  id               uuid primary key,
  id_contrato      uuid not null references contrato.contrato (id),
  tipo             varchar(10) not null check (tipo in ('ENTREGA', 'DEVOLUCION')),
  kilometraje      integer not null check (kilometraje >= 0 and kilometraje <= 2000000),
  combustible      varchar(15) not null
                   check (combustible in ('VACIO', 'UN_CUARTO', 'MEDIO', 'TRES_CUARTOS', 'LLENO')),
  observaciones    varchar(500),
  cliente_conforme boolean,                       -- solo en la devolucion: el cliente acepto lo registrado
  registrado_por   varchar(120) not null,
  registrado_en    timestamptz not null default now(),
  unique (id_contrato, tipo)
);

create table if not exists contrato.contrato_dano (
  id              uuid primary key,
  id_inspeccion   uuid not null references contrato.contrato_inspeccion (id),
  zona            varchar(20) not null
                  check (zona in ('FRONTAL', 'POSTERIOR', 'LATERAL_IZQUIERDO', 'LATERAL_DERECHO',
                                  'TECHO', 'LUNAS', 'LLANTAS', 'INTERIOR', 'OTRO')),
  descripcion     varchar(300) not null,
  gravedad        varchar(10) not null check (gravedad in ('LEVE', 'MODERADO', 'GRAVE')),
  costo_estimado  numeric(10, 2) check (costo_estimado is null or costo_estimado >= 0)
);

create index if not exists idx_contrato_dano_inspeccion
  on contrato.contrato_dano (id_inspeccion);

grant select, insert, update on contrato.contrato_inspeccion to svc_contrato;
grant select, insert, update on contrato.contrato_dano to svc_contrato;
revoke delete, truncate on contrato.contrato_inspeccion from svc_contrato;
revoke delete, truncate on contrato.contrato_dano from svc_contrato;

-- Las demas tablas del esquema contrato tienen RLS con una politica para svc_contrato:
-- sin politica, las consultas fallan en silencio.
alter table contrato.contrato_inspeccion enable row level security;
drop policy if exists svc_contrato_contrato_inspeccion on contrato.contrato_inspeccion;
create policy svc_contrato_contrato_inspeccion
  on contrato.contrato_inspeccion
  for all to svc_contrato
  using (true)
  with check (true);

alter table contrato.contrato_dano enable row level security;
drop policy if exists svc_contrato_contrato_dano on contrato.contrato_dano;
create policy svc_contrato_contrato_dano
  on contrato.contrato_dano
  for all to svc_contrato
  using (true)
  with check (true);
