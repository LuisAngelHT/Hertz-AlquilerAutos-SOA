-- SVC-04 ServicioContrato: dibujo del vehiculo e inventario (ficha de entrega y recepcion).
-- Ejecutar en Supabase SQL Editor como administrador. Es seguro repetirlo (if not exists).
-- NO modifica datos: agrega columnas opcionales a contrato_dano y una tabla nueva (contrato_item).
--
--   contrato_dano: ahora guarda DONDE esta el dano en el dibujo (vista y posicion de 0 a 100) y de que tipo es.
--                  Los danos anteriores quedan con esas columnas vacias.
--   contrato_item: estado de cada elemento del inventario (luces, radio, gata, extintor, SOAT...) en la entrega y
--                  en la devolucion. Lo que falta o vuelve danado en la devolucion queda a cargo del cliente.

alter table contrato.contrato_dano
  add column if not exists vista varchar(10)
    check (vista is null or vista in ('IZQUIERDA', 'DERECHA', 'FRENTE', 'ATRAS', 'SUPERIOR')),
  add column if not exists pos_x numeric(5, 2) check (pos_x is null or (pos_x >= 0 and pos_x <= 100)),
  add column if not exists pos_y numeric(5, 2) check (pos_y is null or (pos_y >= 0 and pos_y <= 100)),
  add column if not exists tipo varchar(12)
    check (tipo is null or tipo in ('RAYON', 'ABOLLADURA', 'ROTURA', 'MANCHA', 'OTRO'));

create table if not exists contrato.contrato_item (
  id             uuid primary key,
  id_inspeccion  uuid not null references contrato.contrato_inspeccion (id),
  item           varchar(30) not null,
  estado         varchar(8) not null check (estado in ('OK', 'FALTA', 'DANADO')),
  costo_estimado numeric(10, 2) check (costo_estimado is null or costo_estimado >= 0),
  unique (id_inspeccion, item)
);

create index if not exists idx_contrato_item_inspeccion on contrato.contrato_item (id_inspeccion);

grant select, insert, update on contrato.contrato_item to svc_contrato;
revoke delete, truncate on contrato.contrato_item from svc_contrato;

alter table contrato.contrato_item enable row level security;
drop policy if exists svc_contrato_contrato_item on contrato.contrato_item;
create policy svc_contrato_contrato_item on contrato.contrato_item for all to svc_contrato using (true) with check (true);
