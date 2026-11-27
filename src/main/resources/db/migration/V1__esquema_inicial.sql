CREATE TABLE espectaculo (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    fecha DATE NOT NULL,
    cantidad_sillas INTEGER NOT NULL CHECK (cantidad_sillas >= 0)
);

CREATE TABLE cliente (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    documento VARCHAR(30) NOT NULL UNIQUE,
    telefono VARCHAR(20)
);

CREATE TABLE reserva (
    id SERIAL PRIMARY KEY,
    codigo_reserva VARCHAR(20) NOT NULL UNIQUE,
    cliente_id INTEGER NOT NULL,
    espectaculo_id INTEGER NOT NULL,
    cantidad_sillas INTEGER NOT NULL CHECK (cantidad_sillas > 0),

    CONSTRAINT fk_reserva_cliente
        FOREIGN KEY (cliente_id)
        REFERENCES cliente (id),

    CONSTRAINT fk_reserva_espectaculo
        FOREIGN KEY (espectaculo_id)
        REFERENCES espectaculo (id)
);
