package com.udea.parcial_equipo_4;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo sobre H2 con las migraciones de Flyway: registrar cliente y
 * espectáculo, crear la reserva (POST requerido) y consultarla por código (GET requerido).
 */
@SpringBootTest
@AutoConfigureMockMvc
class FlujoReservaIntegrationTest {

    private static final String HEADER_VERSION = "X-API-Version";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registrarClienteYEspectaculoCrearReservaYConsultarla() throws Exception {
        Integer clienteId = crear("/api/clientes", """
                {"nombre": "Laura Henao", "documento": "43123987", "telefono": "3001112233"}
                """);
        Integer espectaculoId = crear("/api/espectaculos", """
                {"nombre": "Concierto de prueba", "fecha": "%s", "cantidadSillas": 3}
                """.formatted(LocalDate.now().plusDays(10)));

        String reserva = mockMvc.perform(post("/api/reservas").header(HEADER_VERSION, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clienteId": %d, "espectaculoId": %d, "cantidadSillas": 2}
                                """.formatted(clienteId, espectaculoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoReserva", startsWith("RES-")))
                .andReturn().getResponse().getContentAsString();
        String codigo = JsonPath.read(reserva, "$.codigoReserva");

        mockMvc.perform(get("/api/reservas").param("codigo", codigo).header(HEADER_VERSION, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadSillas").value(2))
                .andExpect(jsonPath("$.cliente.id").value(clienteId))
                .andExpect(jsonPath("$.espectaculo.id").value(espectaculoId));

        // Quedan 1 silla: pedir 2 más es un conflicto.
        mockMvc.perform(post("/api/reservas").header(HEADER_VERSION, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clienteId": %d, "espectaculoId": %d, "cantidadSillas": 2}
                                """.formatted(clienteId, espectaculoId)))
                .andExpect(status().isConflict());

        // El documento es único.
        mockMvc.perform(post("/api/clientes").header(HEADER_VERSION, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Otra persona", "documento": "43123987"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void healthRespondeUpSinHeaderDeVersion() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void openApiDocumentaLosEndpointsDeApoyo() throws Exception {
        mockMvc.perform(get("/v3/api-docs/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/clientes'].post").exists())
                .andExpect(jsonPath("$.paths['/api/espectaculos'].post").exists())
                .andExpect(jsonPath("$.paths['/api/espectaculos'].get").exists());
    }

    private Integer crear(String ruta, String cuerpo) throws Exception {
        String respuesta = mockMvc.perform(post(ruta).header(HEADER_VERSION, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }
}
