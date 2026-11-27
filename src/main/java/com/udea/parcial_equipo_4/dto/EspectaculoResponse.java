package com.udea.parcial_equipo_4.dto;

import java.time.LocalDate;

public record EspectaculoResponse(
        Integer id,
        String nombre,
        LocalDate fecha,
        Integer cantidadSillas
) {
}
