package com.udea.parcial_equipo_4.controller;

import com.udea.parcial_equipo_4.dto.ReservaResponse;
import com.udea.parcial_equipo_4.dto.ReservaV2Response;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Agrega los enlaces HATEOAS (self, espectaculo, cliente) a una reserva.
 */
@Component
public class ReservaModelAssembler implements RepresentationModelAssembler<ReservaResponse, EntityModel<ReservaResponse>> {

    @Override
    public EntityModel<ReservaResponse> toModel(ReservaResponse reserva) {
        return EntityModel.of(reserva,
                linkTo(methodOn(ReservaController.class).consultar(reserva.codigoReserva())).withSelfRel(),
                linkEspectaculo(reserva.espectaculo().id()),
                linkCliente(reserva.cliente().id()));
    }

    public EntityModel<ReservaV2Response> toModel(ReservaV2Response reserva) {
        return EntityModel.of(reserva,
                linkTo(methodOn(ReservaController.class).consultarV2(reserva.codigoReserva())).withSelfRel(),
                linkEspectaculo(reserva.espectaculo().id()),
                linkCliente(reserva.cliente().id()));
    }

    private static Link linkEspectaculo(Integer espectaculoId) {
        return linkTo(methodOn(EspectaculoController.class).consultar(espectaculoId)).withRel("espectaculo");
    }

    private static Link linkCliente(Integer clienteId) {
        return linkTo(methodOn(ClienteController.class).consultar(clienteId)).withRel("cliente");
    }
}
