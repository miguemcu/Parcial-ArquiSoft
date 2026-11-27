package com.udea.parcial_equipo_4.controller;

import com.udea.parcial_equipo_4.dto.CrearReservaRequest;
import com.udea.parcial_equipo_4.dto.ReservaResponse;
import com.udea.parcial_equipo_4.dto.ReservaV2Response;
import com.udea.parcial_equipo_4.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
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

/**
 * Endpoints de reservas ({@code /api/reservas}). Los requeridos por el enunciado son la consulta
 * por código ({@code GET ?codigo=}) y la creación ({@code POST}); además lista por espectáculo.
 */
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Consulta y creación de reservas de sillas")
public class ReservaController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final ReservaService reservaService;
    private final ReservaModelAssembler reservaModelAssembler;
    private final PagedResourcesAssembler<ReservaResponse> pagedResourcesAssembler;

    @GetMapping(params = {"codigo", "!espectaculoId"}, version = "1")
    @Operation(summary = "Consultar una reserva por su código",
            description = "Usa el parámetro codigo. No se puede combinar con espectaculoId: "
                    + "si llegan ambos, o ninguno, la respuesta es 400.")
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

    @GetMapping(params = {"codigo", "!espectaculoId"}, version = "2")
    @Operation(summary = "Consultar una reserva por su código (versión 2)",
            description = "Igual que la versión 1, más los campos calculados totalSillasEspectaculo "
                    + "y sillasDisponiblesEspectaculo. Requiere X-API-Version: 2.")
    @ApiResponse(responseCode = "200", description = "Reserva encontrada")
    @ApiResponse(responseCode = "400", description = "Falta el parámetro codigo, combinación de parámetros no permitida o versión inválida",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No existe una reserva con ese código",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<EntityModel<ReservaV2Response>> consultarV2(
            @Parameter(description = "Código de la reserva", example = "RES-ABC12345")
            @RequestParam String codigo) {
        return ResponseEntity.ok(reservaModelAssembler.toModel(reservaService.consultarPorCodigoV2(codigo)));
    }

    @GetMapping(params = {"espectaculoId", "!codigo"}, version = "1+")
    @Operation(summary = "Listar las reservas de un espectáculo (paginado)",
            description = "Ordenadas por id descendente. size por defecto 10 y máximo 50 (un valor mayor se limita a 50). "
                    + "No se puede combinar con codigo: si llegan ambos, o ninguno, la respuesta es 400.")
    @ApiResponse(responseCode = "200", description = "Página de reservas con enlaces self, first, prev, next y last")
    @ApiResponse(responseCode = "400", description = "Parámetros de paginación inválidos o combinación de parámetros no permitida",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No existe un espectáculo con ese id",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<PagedModel<EntityModel<ReservaResponse>>> listarPorEspectaculo(
            @Parameter(description = "Id del espectáculo", example = "1")
            @RequestParam Integer espectaculoId,
            @Parameter(description = "Número de página (desde 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "debe ser mayor o igual a 0") int page,
            @Parameter(description = "Tamaño de página (máximo 50)", example = "10")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "debe ser al menos 1") int size) {
        Page<ReservaResponse> reservas = reservaService.listarPorEspectaculo(espectaculoId, page, size);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(reservas, reservaModelAssembler));
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
