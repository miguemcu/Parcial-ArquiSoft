package com.udea.parcial_equipo_4.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CrearReservaRequest(
        @NotNull(message = "es obligatorio")
        Integer clienteId,

        @NotNull(message = "es obligatorio")
        Integer espectaculoId,

        @NotNull(message = "es obligatorio")
        @Min(value = 1, message = "debe ser al menos 1")
        @Max(value = 10, message = "no puede ser mayor que 10")
        Integer cantidadSillas
) {
}
