package com.udea.parcial_equipo_4.dto;

import java.time.LocalDate;

public record ReservaResponse(
        String codigoReserva,
        Integer cantidadSillas,
        ClienteResumen cliente,
        EspectaculoResumen espectaculo
) {

    public record ClienteResumen(Integer id, String nombre, String documento) {
    }

    public record EspectaculoResumen(Integer id, String nombre, LocalDate fecha) {
    }
}
