-- =====================================================
-- SVC-03 ServicioFlota - historial de cambios de estado
-- Ejecutar en Supabase > SQL Editor (como postgres), DESPUES de flota.sql.
-- Cada cambio de estado de un vehiculo deja una fila: quien lo hizo, cuando y por que.
-- =====================================================
create table if not exists flota.historial_vehiculo (
  id              uuid primary key default gen_random_uuid(),
  placa           varchar(10) not null references flota.vehiculo (placa),
  estado_anterior varchar(20) not null
                  check (estado_anterior in ('DISPONIBLE', 'ALQUILADO', 'MANTENIMIENTO')),
  estado_nuevo    varchar(20) not null
                  check (estado_nuevo in ('DISPONIBLE', 'ALQUILADO', 'MANTENIMIENTO')),
  usuario         varchar(120) not null,   -- quien hizo el cambio (header X-Usuario del gateway)
  rol             varchar(30)  not null,   -- rol con el que lo hizo (X-Rol)
  motivo          varchar(200),            -- texto libre opcional (por que va a mantenimiento, etc.)
  fecha           timestamptz not null default now()
);

create index if not exists idx_historial_placa_fecha
  on flota.historial_vehiculo (placa, fecha desc);

-- El servicio solo puede LEER e INSERTAR: el historial no se edita ni se borra.
grant select, insert on flota.historial_vehiculo to svc_flota;
revoke update, delete, truncate on flota.historial_vehiculo from svc_flota;
