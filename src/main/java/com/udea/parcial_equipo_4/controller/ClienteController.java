package com.udea.parcial_equipo_4.controller;

import com.udea.parcial_equipo_4.dto.ClienteResponse;
import com.udea.parcial_equipo_4.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Consulta de clientes (solo lectura)")
public class ClienteController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final ClienteService clienteService;

    @GetMapping(path = "/{id}", version = "1+")
    @Operation(summary = "Consultar un cliente por su id")
    @ApiResponse(responseCode = "200", description = "Cliente encontrado")
    @ApiResponse(responseCode = "400", description = "Versión de API faltante o no soportada",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No existe un cliente con ese id",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<EntityModel<ClienteResponse>> consultar(
            @Parameter(description = "Id del cliente", example = "1") @PathVariable Integer id) {
        ClienteResponse cliente = clienteService.consultarPorId(id);
        return ResponseEntity.ok(EntityModel.of(cliente,
                linkTo(methodOn(ClienteController.class).consultar(id)).withSelfRel()));
    }
}
