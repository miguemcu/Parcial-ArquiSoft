package com.udea.parcial_equipo_4.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Datos para registrar un espectáculo: nombre, fecha (hoy o posterior) y capacidad de sillas.
 */
public record CrearEspectaculoRequest(
        @Schema(description = "Nombre del espectáculo", example = "Concierto Sinfónico de Navidad")
        @NotBlank(message = "es obligatorio")
        @Size(max = 150, message = "no puede tener más de 150 caracteres")
        String nombre,

        @Schema(description = "Fecha del espectáculo (hoy o posterior)", example = "2026-12-20")
        @NotNull(message = "es obligatoria")
        @FutureOrPresent(message = "no puede ser una fecha pasada")
        LocalDate fecha,

        @Schema(description = "Capacidad total de sillas", example = "100")
        @NotNull(message = "es obligatorio")
        @Min(value = 1, message = "debe ser al menos 1")
        @Max(value = 100000, message = "no puede ser mayor que 100000")
        Integer cantidadSillas
) {
}
