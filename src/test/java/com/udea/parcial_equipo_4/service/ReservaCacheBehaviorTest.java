package com.udea.parcial_equipo_4.service;

import com.udea.parcial_equipo_4.dto.CrearReservaRequest;
import com.udea.parcial_equipo_4.dto.ReservaResponse;
import com.udea.parcial_equipo_4.model.Cliente;
import com.udea.parcial_equipo_4.model.Espectaculo;
import com.udea.parcial_equipo_4.model.Reserva;
import com.udea.parcial_equipo_4.repository.ClienteRepository;
import com.udea.parcial_equipo_4.repository.EspectaculoRepository;
import com.udea.parcial_equipo_4.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class ReservaCacheBehaviorTest {

    private static final int ESPECTACULO_ID = 7;
    private static final String CODIGO = "RES-CACHE01";

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private EspectaculoService espectaculoService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private ReservaRepository reservaRepository;

    @MockitoBean
    private ClienteRepository clienteRepository;

    @MockitoBean
    private EspectaculoRepository espectaculoRepository;

    @BeforeEach
    void limpiarCaches() {
        List.of("reservas", "reservas-v2", "reservas-por-espectaculo", "espectaculos")
                .forEach(cacheName -> cacheManager.getCache(cacheName).clear());
    }

    @Test
    void consultarPorCodigoUsaElValorCacheadoEnLaSegundaLlamada() {
        Reserva reserva = reserva();
        when(reservaRepository.findByCodigoReserva(CODIGO)).thenReturn(Optional.of(reserva));

        assertThat(reservaService.consultarPorCodigo(CODIGO)).isEqualTo(reservaResponse());
        assertThat(reservaService.consultarPorCodigo(CODIGO)).isEqualTo(reservaResponse());

        verify(reservaRepository, times(1)).findByCodigoReserva(CODIGO);
    }

    @Test
    void consultarEspectaculoUsaElValorCacheadoEnLaSegundaLlamada() {
        when(espectaculoRepository.findById(ESPECTACULO_ID)).thenReturn(Optional.of(espectaculo()));

        assertThat(espectaculoService.consultarPorId(ESPECTACULO_ID))
                .isEqualTo(espectaculoService.consultarPorId(ESPECTACULO_ID));

        verify(espectaculoRepository, times(1)).findById(ESPECTACULO_ID);
    }

    @Test
    void tamanosDePaginaEquivalentesCompartenLaMismaClaveEfectiva() {
        when(espectaculoRepository.existsById(ESPECTACULO_ID)).thenReturn(true);
        when(reservaRepository.findByEspectaculoId(eq(ESPECTACULO_ID), any(Pageable.class)))
                .thenAnswer(invocation -> page(invocation.getArgument(1)));

        Page<?> pageDeCien = reservaService.listarPorEspectaculo(ESPECTACULO_ID, 0, 100);
        Page<?> pageDeCincuenta = reservaService.listarPorEspectaculo(ESPECTACULO_ID, 0, 50);

        assertThat(pageDeCien.getSize()).isEqualTo(ReservaService.TAMANO_MAXIMO_PAGINA);
        assertThat(pageDeCincuenta.getSize()).isEqualTo(ReservaService.TAMANO_MAXIMO_PAGINA);
        verify(reservaRepository, times(1))
                .findByEspectaculoId(eq(ESPECTACULO_ID), any(Pageable.class));
    }

    @Test
    void crearInvalidaLaDisponibilidadV2YElListado() {
        when(reservaRepository.findByCodigoReserva(CODIGO)).thenReturn(Optional.of(reserva()));
        when(reservaRepository.sumarSillasReservadas(ESPECTACULO_ID)).thenReturn(2);
        when(reservaRepository.existsByCodigoReserva(anyString())).thenReturn(false);
        when(reservaRepository.findByEspectaculoId(eq(ESPECTACULO_ID), any(Pageable.class)))
                .thenAnswer(invocation -> page(invocation.getArgument(1)));
        when(reservaRepository.save(any(Reserva.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(clienteRepository.findById(1)).thenReturn(Optional.of(cliente()));
        when(espectaculoRepository.findByIdForUpdate(ESPECTACULO_ID))
                .thenReturn(Optional.of(espectaculo()));
        when(espectaculoRepository.existsById(ESPECTACULO_ID)).thenReturn(true);

        reservaService.consultarPorCodigoV2(CODIGO);
        reservaService.listarPorEspectaculo(ESPECTACULO_ID, 0, 4);
        reservaService.crear(new CrearReservaRequest(1, ESPECTACULO_ID, 1));
        reservaService.consultarPorCodigoV2(CODIGO);
        reservaService.listarPorEspectaculo(ESPECTACULO_ID, 0, 4);

        verify(reservaRepository, times(2)).findByCodigoReserva(CODIGO);
        verify(reservaRepository, times(2))
                .findByEspectaculoId(eq(ESPECTACULO_ID), any(Pageable.class));
    }

    private static Page<Reserva> page(Pageable pageable) {
        return new PageImpl<>(List.of(reserva()), pageable, 1);
    }

    private static Reserva reserva() {
        return new Reserva(11, CODIGO, cliente(), espectaculo(), 2);
    }

    private static Cliente cliente() {
        return new Cliente(1, "Ana", "DOC-123", "3001234567");
    }

    private static Espectaculo espectaculo() {
        return new Espectaculo(ESPECTACULO_ID, "Festival", LocalDate.of(2026, 12, 15), 100);
    }

    private static ReservaResponse reservaResponse() {
        return new ReservaResponse(
                CODIGO,
                2,
                new ReservaResponse.ClienteResumen(1, "Ana", "DOC-123"),
                new ReservaResponse.EspectaculoResumen(
                        ESPECTACULO_ID, "Festival", LocalDate.of(2026, 12, 15))
        );
    }
}
