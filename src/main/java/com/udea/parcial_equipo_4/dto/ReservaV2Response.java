package com.udea.parcial_equipo_4.dto;

/**
 * Respuesta de la versión 2 de la consulta por código: los campos de v1 más
 * datos calculados del espectáculo (no requieren cambios en el esquema).
 */
public record ReservaV2Response(
        String codigoReserva,
        Integer cantidadSillas,
        ReservaResponse.ClienteResumen cliente,
        ReservaResponse.EspectaculoResumen espectaculo,
        Integer totalSillasEspectaculo,
        Integer sillasDisponiblesEspectaculo
) {
}
