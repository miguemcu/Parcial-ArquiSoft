INSERT INTO espectaculo (nombre, fecha, cantidad_sillas) VALUES
    ('Concierto Sinfónico de Navidad', '2026-12-20', 100),
    ('Noche de Rock Acústico', '2027-02-14', 5);

INSERT INTO cliente (nombre, documento, telefono) VALUES
    ('Ana María Restrepo', '1017123456', '3001234567'),
    ('Carlos Gómez', '1036987654', '3109876543'),
    ('Laura Henao', '43123987', NULL);

INSERT INTO reserva (codigo_reserva, cliente_id, espectaculo_id, cantidad_sillas)
SELECT 'RES-ABC12345', c.id, e.id, 2
FROM cliente c
JOIN espectaculo e ON e.nombre = 'Concierto Sinfónico de Navidad'
WHERE c.documento = '1017123456';
