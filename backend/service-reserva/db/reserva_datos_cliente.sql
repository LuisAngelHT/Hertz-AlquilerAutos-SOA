-- =====================================================
-- SVC-02 ServicioReserva - guarda quién alquiló (foto de los datos del cliente al reservar)
-- Ejecutar en Supabase > SQL Editor (como postgres), DESPUÉS de reserva.sql
-- =====================================================
-- Así el personal ve el nombre, documento y teléfono de quien alquiló sin consultar a
-- ServicioCliente por cada reserva de una lista. Las reservas anteriores quedan en null.
alter table reserva.reserva
  add column if not exists cliente_nombre    varchar(200),
  add column if not exists cliente_documento varchar(30),
  add column if not exists cliente_telefono  varchar(20);
