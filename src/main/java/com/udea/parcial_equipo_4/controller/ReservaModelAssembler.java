package com.udea.parcial_equipo_4.controller;

import com.udea.parcial_equipo_4.dto.ReservaResponse;
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
                linkTo(methodOn(EspectaculoController.class).consultar(reserva.espectaculo().id())).withRel("espectaculo"),
                linkTo(methodOn(ClienteController.class).consultar(reserva.cliente().id())).withRel("cliente"));
    }
}
