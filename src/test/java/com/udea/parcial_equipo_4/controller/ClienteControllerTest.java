package com.udea.parcial_equipo_4.controller;

import com.udea.parcial_equipo_4.dto.ClienteResponse;
import com.udea.parcial_equipo_4.dto.CrearClienteRequest;
import com.udea.parcial_equipo_4.exception.ClienteDuplicadoException;
import com.udea.parcial_equipo_4.service.ClienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClienteController.class)
class ClienteControllerTest {

    private static final String HEADER_VERSION = "X-API-Version";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService clienteService;

    @Test
    void crearClienteValidoResponde201ConLocationYSinDatosSensibles() throws Exception {
        when(clienteService.crear(any(CrearClienteRequest.class))).thenReturn(new ClienteResponse(7, "Ana María Restrepo"));

        mockMvc.perform(post("/api/clientes").header(HEADER_VERSION, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Ana María Restrepo", "documento": "1017123456", "telefono": "3001234567"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/clientes/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.nombre").value("Ana María Restrepo"))
                .andExpect(jsonPath("$.documento").doesNotExist())
                .andExpect(jsonPath("$.telefono").doesNotExist())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/clientes/7"));
    }

    @Test
    void crearClienteSinNombreNiDocumentoResponde400() throws Exception {
        mockMvc.perform(post("/api/clientes").header(HEADER_VERSION, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": " ", "telefono": "abc"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud inválida"))
                .andExpect(jsonPath("$.errores.nombre").value("es obligatorio"))
                .andExpect(jsonPath("$.errores.documento").value("es obligatorio"))
                .andExpect(jsonPath("$.errores.telefono").exists());
        verifyNoInteractions(clienteService);
    }

    @Test
    void crearClienteConDocumentoRepetidoResponde409() throws Exception {
        when(clienteService.crear(any(CrearClienteRequest.class))).thenThrow(new ClienteDuplicadoException());

        mockMvc.perform(post("/api/clientes").header(HEADER_VERSION, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Ana", "documento": "1017123456"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Cliente duplicado"));
    }

    @Test
    void crearClienteSinHeaderDeVersionResponde400() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Ana", "documento": "1017123456"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Versión de API inválida"));
        verifyNoInteractions(clienteService);
    }
}
