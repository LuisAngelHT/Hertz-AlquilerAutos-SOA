-- SVC-04 ServicioContrato: contrato firmado y escaneado (respaldo digital del contrato fisico).
-- Ejecutar en Supabase SQL Editor como administrador. Es seguro repetirlo (if not exists).
-- Solo AGREGA una tabla: no modifica nada de lo existente.
--
-- El archivo vive en su propia tabla para que listar contratos nunca cargue los archivos.
-- Se guarda un archivo por contrato (reemplazarlo sobrescribe el anterior y deja quien/cuando).

create table if not exists contrato.contrato_documento (
  id_contrato     uuid primary key references contrato.contrato (id),
  nombre_archivo  varchar(200) not null,
  tipo_contenido  varchar(30)  not null
                  check (tipo_contenido in ('application/pdf', 'image/jpeg', 'image/png')),
  tamano_bytes    integer      not null
                  check (tamano_bytes > 0 and tamano_bytes <= 5242880),   -- maximo 5 MB
  contenido       bytea        not null,
  subido_por      varchar(120) not null,
  subido_en       timestamptz  not null default now()
);

grant select, insert, update on contrato.contrato_documento to svc_contrato;
revoke delete, truncate on contrato.contrato_documento from svc_contrato;

-- Las demas tablas del esquema contrato tienen RLS con una politica para svc_contrato:
-- sin politica, las consultas fallan en silencio.
alter table contrato.contrato_documento enable row level security;
drop policy if exists svc_contrato_contrato_documento on contrato.contrato_documento;
create policy svc_contrato_contrato_documento
  on contrato.contrato_documento
  for all to svc_contrato
  using (true)
  with check (true);
