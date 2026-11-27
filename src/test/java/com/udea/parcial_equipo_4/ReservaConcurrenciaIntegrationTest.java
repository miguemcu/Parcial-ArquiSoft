package com.udea.parcial_equipo_4;

import com.udea.parcial_equipo_4.dto.CrearReservaRequest;
import com.udea.parcial_equipo_4.exception.SillasInsuficientesException;
import com.udea.parcial_equipo_4.model.Cliente;
import com.udea.parcial_equipo_4.model.Espectaculo;
import com.udea.parcial_equipo_4.repository.ClienteRepository;
import com.udea.parcial_equipo_4.repository.EspectaculoRepository;
import com.udea.parcial_equipo_4.repository.ReservaRepository;
import com.udea.parcial_equipo_4.service.ReservaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Varias reservas simultáneas sobre el mismo espectáculo: el bloqueo
 * PESSIMISTIC_WRITE impide que se vendan más sillas de las que hay.
 */
@SpringBootTest
class ReservaConcurrenciaIntegrationTest {

    private static final int CAPACIDAD = 5;
    private static final int PETICIONES = 10;

    @Autowired
    private ReservaService reservaService;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private EspectaculoRepository espectaculoRepository;
    @Autowired
    private ReservaRepository reservaRepository;

    @Test
    void reservasSimultaneasNoSobrevendenSillas() throws Exception {
        Cliente cliente = clienteRepository.save(new Cliente(null, "Concurrente", "CONC-0001", null));
        Espectaculo espectaculo = espectaculoRepository.save(
                new Espectaculo(null, "Concierto concurrido", LocalDate.now().plusDays(5), CAPACIDAD));

        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(PETICIONES)) {
            for (int i = 0; i < PETICIONES; i++) {
                resultados.add(pool.submit(() -> {
                    salida.await();
                    try {
                        reservaService.crear(new CrearReservaRequest(cliente.getId(), espectaculo.getId(), 1));
                        return true;
                    } catch (SillasInsuficientesException ex) {
                        return false;
                    }
                }));
            }
            salida.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();
        }

        long exitosas = 0;
        for (Future<Boolean> resultado : resultados) {
            if (resultado.get()) {
                exitosas++;
            }
        }
        assertThat(exitosas).isEqualTo(CAPACIDAD);
        assertThat(reservaRepository.sumarSillasReservadas(espectaculo.getId())).isEqualTo(CAPACIDAD);
    }
}
