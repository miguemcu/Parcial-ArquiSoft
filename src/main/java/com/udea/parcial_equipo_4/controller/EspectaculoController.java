package com.udea.parcial_equipo_4.controller;

import com.udea.parcial_equipo_4.dto.CrearEspectaculoRequest;
import com.udea.parcial_equipo_4.dto.EspectaculoResponse;
import com.udea.parcial_equipo_4.service.EspectaculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Endpoints de apoyo de espectáculos ({@code /api/espectaculos}): registrar, listar paginado y consultar por id.
 */
@RestController
@RequestMapping("/api/espectaculos")
@RequiredArgsConstructor
@Tag(name = "Espectáculos", description = "Endpoints de apoyo para pruebas: registrar, listar y consultar espectáculos")
public class EspectaculoController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final EspectaculoService espectaculoService;
    private final PagedResourcesAssembler<EspectaculoResponse> pagedResourcesAssembler;

    @GetMapping(path = "/{id}", version = "1+")
    @Operation(summary = "Consultar un espectáculo por su id")
    @ApiResponse(responseCode = "200", description = "Espectáculo encontrado")
    @ApiResponse(responseCode = "400", description = "Versión de API faltante o no soportada",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No existe un espectáculo con ese id",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<EntityModel<EspectaculoResponse>> consultar(
            @Parameter(description = "Id del espectáculo", example = "1") @PathVariable Integer id) {
        return ResponseEntity.ok(toModel(espectaculoService.consultarPorId(id)));
    }

    @GetMapping(version = "1+")
    @Operation(summary = "Listar espectáculos (paginado)",
            description = "Ordenados por fecha ascendente. size por defecto 10 y máximo 50 (un valor mayor se limita a 50).")
    @ApiResponse(responseCode = "200", description = "Página de espectáculos con enlaces self, first, prev, next y last")
    @ApiResponse(responseCode = "400", description = "Parámetros de paginación inválidos o versión de API faltante/no soportada",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<PagedModel<EntityModel<EspectaculoResponse>>> listar(
            @Parameter(description = "Número de página (desde 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "debe ser mayor o igual a 0") int page,
            @Parameter(description = "Tamaño de página (máximo 50)", example = "10")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "debe ser al menos 1") int size) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(
                espectaculoService.listar(page, size), EspectaculoController::toModel));
    }

    @PostMapping(version = "1+")
    @Operation(summary = "Registrar un espectáculo",
            description = "Endpoint de apoyo: crea el espectáculo que luego se usa en POST /api/reservas (espectaculoId).")
    @ApiResponse(responseCode = "201", description = "Espectáculo creado",
            headers = @Header(name = "Location", description = "URL para consultar el espectáculo creado"))
    @ApiResponse(responseCode = "400", description = "Datos inválidos o versión de API faltante/no soportada",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<EntityModel<EspectaculoResponse>> crear(@Valid @RequestBody CrearEspectaculoRequest request) {
        EntityModel<EspectaculoResponse> model = toModel(espectaculoService.crear(request));
        return ResponseEntity.created(model.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(model);
    }

    private static EntityModel<EspectaculoResponse> toModel(EspectaculoResponse espectaculo) {
        return EntityModel.of(espectaculo,
                linkTo(methodOn(EspectaculoController.class).consultar(espectaculo.id())).withSelfRel(),
                linkTo(methodOn(ReservaController.class).listarPorEspectaculo(espectaculo.id(), 0, 10))
                        .withRel("reservas"));
    }
}
