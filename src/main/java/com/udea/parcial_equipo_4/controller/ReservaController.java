package com.udea.parcial_equipo_4.controller;

import com.udea.parcial_equipo_4.dto.CrearReservaRequest;
import com.udea.parcial_equipo_4.dto.ReservaResponse;
import com.udea.parcial_equipo_4.service.ReservaService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Consulta y creación de reservas de sillas")
public class ReservaController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final ReservaService reservaService;
    private final ReservaModelAssembler reservaModelAssembler;

    @GetMapping(version = "1")
    @Operation(summary = "Consultar una reserva por su código")
    @ApiResponse(responseCode = "200", description = "Reserva encontrada")
    @ApiResponse(responseCode = "400", description = "Falta el parámetro codigo",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No existe una reserva con ese código",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<EntityModel<ReservaResponse>> consultar(
            @Parameter(description = "Código de la reserva", example = "RES-ABC12345")
            @RequestParam String codigo) {
        return ResponseEntity.ok(reservaModelAssembler.toModel(reservaService.consultarPorCodigo(codigo)));
    }

    @PostMapping(version = "1+")
    @Operation(summary = "Crear una reserva de sillas para un espectáculo")
    @ApiResponse(responseCode = "201", description = "Reserva creada",
            headers = @Header(name = "Location", description = "URL para consultar la reserva creada"))
    @ApiResponse(responseCode = "400", description = "Datos de la reserva inválidos",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Cliente o espectáculo inexistente",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "No hay sillas suficientes disponibles",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<EntityModel<ReservaResponse>> crear(@Valid @RequestBody CrearReservaRequest request) {
        EntityModel<ReservaResponse> model = reservaModelAssembler.toModel(reservaService.crear(request));
        return ResponseEntity.created(model.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(model);
    }
}
