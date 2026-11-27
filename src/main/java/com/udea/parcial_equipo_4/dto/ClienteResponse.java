package com.udea.parcial_equipo_4.dto;

/**
 * Vista pública del cliente: no expone documento ni teléfono (datos sensibles).
 */
public record ClienteResponse(
        Integer id,
        String nombre
) {
}
