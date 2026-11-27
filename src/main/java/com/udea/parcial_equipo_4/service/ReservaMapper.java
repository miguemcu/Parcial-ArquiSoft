package com.udea.parcial_equipo_4.service;

import com.udea.parcial_equipo_4.dto.ReservaResponse;
import com.udea.parcial_equipo_4.model.Reserva;
import org.springframework.stereotype.Component;

/**
 * Convierte la entidad {@link com.udea.parcial_equipo_4.model.Reserva} en su DTO de respuesta.
 */
@Component
public class ReservaMapper {

    public ReservaResponse toResponse(Reserva reserva) {
        return new ReservaResponse(
                reserva.getCodigoReserva(),
                reserva.getCantidadSillas(),
                new ReservaResponse.ClienteResumen(
                        reserva.getCliente().getId(),
                        reserva.getCliente().getNombre(),
                        reserva.getCliente().getDocumento()
                ),
                new ReservaResponse.EspectaculoResumen(
                        reserva.getEspectaculo().getId(),
                        reserva.getEspectaculo().getNombre(),
                        reserva.getEspectaculo().getFecha()
                )
        );
    }
}
