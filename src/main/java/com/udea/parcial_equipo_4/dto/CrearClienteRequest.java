package com.udea.parcial_equipo_4.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar un cliente. El documento debe ser único.
 */
public record CrearClienteRequest(
        @Schema(description = "Nombre completo", example = "Ana María Restrepo")
        @NotBlank(message = "es obligatorio")
        @Size(max = 150, message = "no puede tener más de 150 caracteres")
        String nombre,

        @Schema(description = "Documento de identidad (único)", example = "1017123456")
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9-]{5,30}$",
                message = "debe tener entre 5 y 30 caracteres alfanuméricos o guiones")
        String documento,

        @Schema(description = "Teléfono de contacto (opcional)", example = "3001234567")
        @Pattern(regexp = "^\\+?[0-9 ]{7,20}$",
                message = "debe tener entre 7 y 20 dígitos, con + inicial opcional")
        String telefono
) {
}
