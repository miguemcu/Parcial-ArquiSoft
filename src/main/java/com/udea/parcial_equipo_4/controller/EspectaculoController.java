package com.udea.parcial_equipo_4.controller;

import com.udea.parcial_equipo_4.dto.EspectaculoResponse;
import com.udea.parcial_equipo_4.service.EspectaculoService;
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
@RequestMapping("/api/espectaculos")
@RequiredArgsConstructor
@Tag(name = "Espectáculos", description = "Consulta de espectáculos (solo lectura)")
public class EspectaculoController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final EspectaculoService espectaculoService;

    @GetMapping(path = "/{id}", version = "1+")
    @Operation(summary = "Consultar un espectáculo por su id")
    @ApiResponse(responseCode = "200", description = "Espectáculo encontrado")
    @ApiResponse(responseCode = "400", description = "Versión de API faltante o no soportada",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No existe un espectáculo con ese id",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<EntityModel<EspectaculoResponse>> consultar(
            @Parameter(description = "Id del espectáculo", example = "1") @PathVariable Integer id) {
        EspectaculoResponse espectaculo = espectaculoService.consultarPorId(id);
        return ResponseEntity.ok(EntityModel.of(espectaculo,
                linkTo(methodOn(EspectaculoController.class).consultar(id)).withSelfRel()));
    }
}
