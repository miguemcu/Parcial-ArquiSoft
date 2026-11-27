package com.udea.parcial_equipo_4.controller;

import com.udea.parcial_equipo_4.dto.CrearEspectaculoRequest;
import com.udea.parcial_equipo_4.dto.EspectaculoResponse;
import com.udea.parcial_equipo_4.service.EspectaculoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EspectaculoController.class)
class EspectaculoControllerTest {

    private static final String HEADER_VERSION = "X-API-Version";
    private static final LocalDate FECHA = LocalDate.now().plusDays(30);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EspectaculoService espectaculoService;

    @Test
    void crearEspectaculoValidoResponde201ConLocationYEnlaces() throws Exception {
        when(espectaculoService.crear(any(CrearEspectaculoRequest.class)))
                .thenReturn(new EspectaculoResponse(3, "Noche de Rock", FECHA, 100));

        mockMvc.perform(post("/api/espectaculos").header(HEADER_VERSION, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Noche de Rock", "fecha": "%s", "cantidadSillas": 100}
                                """.formatted(FECHA)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/espectaculos/3"))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.fecha").value(FECHA.toString()))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/espectaculos/3"))
                .andExpect(jsonPath("$._links.reservas.href").exists());
    }

    @Test
    void crearEspectaculoConDatosInvalidosResponde400() throws Exception {
        mockMvc.perform(post("/api/espectaculos").header(HEADER_VERSION, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "", "fecha": "2000-01-01", "cantidadSillas": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").value("es obligatorio"))
                .andExpect(jsonPath("$.errores.fecha").value("no puede ser una fecha pasada"))
                .andExpect(jsonPath("$.errores.cantidadSillas").value("debe ser al menos 1"));
        verifyNoInteractions(espectaculoService);
    }

    @Test
    void crearEspectaculoConFechaMalFormadaResponde400() throws Exception {
        mockMvc.perform(post("/api/espectaculos").header(HEADER_VERSION, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Rock", "fecha": "20-12-2026", "cantidadSillas": 10}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(espectaculoService);
    }

    @Test
    void listarEspectaculosDevuelvePaginaConEnlaces() throws Exception {
        when(espectaculoService.listar(0, 10)).thenReturn(new PageImpl<>(
                List.of(new EspectaculoResponse(1, "Concierto", FECHA, 100)), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/espectaculos").header(HEADER_VERSION, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.espectaculoResponseList[0].id").value(1))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void listarEspectaculosConPaginaNegativaResponde400() throws Exception {
        mockMvc.perform(get("/api/espectaculos").param("page", "-1").header(HEADER_VERSION, "1"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(espectaculoService);
    }
}
