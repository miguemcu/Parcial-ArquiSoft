package com.udea.parcial_equipo_4.controller;

import com.udea.parcial_equipo_4.dto.ReservaResponse;
import com.udea.parcial_equipo_4.dto.ReservaV2Response;
import com.udea.parcial_equipo_4.service.ReservaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import com.udea.parcial_equipo_4.dto.CrearReservaRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservaController.class)
@Import(ReservaModelAssembler.class)
class ReservaControllerTest {

    private static final String HEADER_VERSION = "X-API-Version";
    private static final String CODIGO = "RES-ABC12345";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaService reservaService;

    @Test
    void sinHeaderDeVersionResponde400ConVersionesSoportadas() throws Exception {
        mockMvc.perform(get("/api/reservas").param("codigo", CODIGO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Versión de API inválida"))
                .andExpect(jsonPath("$.versionesSoportadas[0]").value("1"));
    }

    @Test
    void conVersionNoSoportadaResponde400() throws Exception {
        mockMvc.perform(get("/api/reservas").param("codigo", CODIGO).header(HEADER_VERSION, "9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La versión '9' no está soportada. Versiones soportadas: 1, 2"));
    }

    @Test
    void conVersion1ConsultaLaReserva() throws Exception {
        when(reservaService.consultarPorCodigo(CODIGO)).thenReturn(reserva());

        mockMvc.perform(get("/api/reservas").param("codigo", CODIGO).header(HEADER_VERSION, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoReserva").value(CODIGO));
    }

    @Test
    void v1YV2DevuelvenCuerposDistintos() throws Exception {
        when(reservaService.consultarPorCodigo(CODIGO)).thenReturn(reserva());
        ReservaResponse v1 = reserva();
        when(reservaService.consultarPorCodigoV2(CODIGO)).thenReturn(new ReservaV2Response(
                v1.codigoReserva(), v1.cantidadSillas(), v1.cliente(), v1.espectaculo(), 100, 40));

        mockMvc.perform(get("/api/reservas").param("codigo", CODIGO).header(HEADER_VERSION, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoReserva").value(CODIGO))
                .andExpect(jsonPath("$.totalSillasEspectaculo").doesNotExist())
                .andExpect(jsonPath("$.sillasDisponiblesEspectaculo").doesNotExist());

        mockMvc.perform(get("/api/reservas").param("codigo", CODIGO).header(HEADER_VERSION, "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoReserva").value(CODIGO))
                .andExpect(jsonPath("$.totalSillasEspectaculo").value(100))
                .andExpect(jsonPath("$.sillasDisponiblesEspectaculo").value(40))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/reservas?codigo=" + CODIGO));
    }

    @Test
    void laRespuestaIncluyeEnlacesHateoas() throws Exception {
        when(reservaService.consultarPorCodigo(CODIGO)).thenReturn(reserva());

        mockMvc.perform(get("/api/reservas").param("codigo", CODIGO).header(HEADER_VERSION, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/reservas?codigo=" + CODIGO))
                .andExpect(jsonPath("$._links.espectaculo.href").value("http://localhost/api/espectaculos/1"))
                .andExpect(jsonPath("$._links.cliente.href").value("http://localhost/api/clientes/1"));
    }

    @Test
    void crearResponde201ConLocationYEnlaces() throws Exception {
        when(reservaService.crear(any(CrearReservaRequest.class))).thenReturn(reserva());

        mockMvc.perform(post("/api/reservas").header(HEADER_VERSION, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clienteId": 1, "espectaculoId": 1, "cantidadSillas": 2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/reservas?codigo=" + CODIGO))
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    void listarPorEspectaculoDevuelvePaginaConEnlaces() throws Exception {
        when(reservaService.listarPorEspectaculo(1, 1, 2))
                .thenReturn(new PageImpl<>(List.of(reserva()), PageRequest.of(1, 2), 5));

        mockMvc.perform(get("/api/reservas").param("espectaculoId", "1").param("page", "1").param("size", "2")
                        .header(HEADER_VERSION, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.reservaResponseList[0].codigoReserva").value(CODIGO))
                .andExpect(jsonPath("$._embedded.reservaResponseList[0]._links.self.href").exists())
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(5))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.first.href").exists())
                .andExpect(jsonPath("$._links.prev.href").exists())
                .andExpect(jsonPath("$._links.next.href").exists())
                .andExpect(jsonPath("$._links.last.href").exists());
    }

    @Test
    void listarSinPageNiSizeUsaValoresPorDefecto() throws Exception {
        when(reservaService.listarPorEspectaculo(1, 0, 10))
                .thenReturn(new PageImpl<>(List.of(reserva()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/reservas").param("espectaculoId", "1").header(HEADER_VERSION, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.number").value(0));
    }

    @Test
    void conCodigoYEspectaculoIdResponde400() throws Exception {
        mockMvc.perform(get("/api/reservas").param("codigo", CODIGO).param("espectaculoId", "1")
                        .header(HEADER_VERSION, "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parámetros inválidos"));
    }

    @Test
    void sinCodigoNiEspectaculoIdResponde400() throws Exception {
        mockMvc.perform(get("/api/reservas").header(HEADER_VERSION, "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parámetros inválidos"));
    }

    @Test
    void conPageNegativoResponde400() throws Exception {
        mockMvc.perform(get("/api/reservas").param("espectaculoId", "1").param("page", "-1")
                        .header(HEADER_VERSION, "1"))
                .andExpect(status().isBadRequest());
    }

    private static ReservaResponse reserva() {
        return new ReservaResponse(CODIGO, 2,
                new ReservaResponse.ClienteResumen(1, "Ana", "123"),
                new ReservaResponse.EspectaculoResumen(1, "Concierto", LocalDate.of(2026, 12, 1)));
    }
}
