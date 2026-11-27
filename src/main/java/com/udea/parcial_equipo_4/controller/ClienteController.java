package com.udea.parcial_equipo_4.controller;

import com.udea.parcial_equipo_4.dto.ClienteResponse;
import com.udea.parcial_equipo_4.dto.CrearClienteRequest;
import com.udea.parcial_equipo_4.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Endpoints de apoyo de clientes ({@code /api/clientes}): registrar y consultar por id.
 */
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Endpoints de apoyo para pruebas: registrar y consultar clientes")
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
        return ResponseEntity.ok(toModel(clienteService.consultarPorId(id)));
    }

    @PostMapping(version = "1+")
    @Operation(summary = "Registrar un cliente",
            description = "Endpoint de apoyo: crea el cliente que luego se usa en POST /api/reservas (clienteId). "
                    + "La respuesta no incluye documento ni teléfono.")
    @ApiResponse(responseCode = "201", description = "Cliente creado",
            headers = @Header(name = "Location", description = "URL para consultar el cliente creado"))
    @ApiResponse(responseCode = "400", description = "Datos inválidos o versión de API faltante/no soportada",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Ya existe un cliente con ese documento",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<EntityModel<ClienteResponse>> crear(@Valid @RequestBody CrearClienteRequest request) {
        EntityModel<ClienteResponse> model = toModel(clienteService.crear(request));
        return ResponseEntity.created(model.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(model);
    }

    private static EntityModel<ClienteResponse> toModel(ClienteResponse cliente) {
        return EntityModel.of(cliente,
                linkTo(methodOn(ClienteController.class).consultar(cliente.id())).withSelfRel());
    }
}
