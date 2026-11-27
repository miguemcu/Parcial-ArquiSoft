package com.udea.parcial_equipo_4.config;

import com.udea.parcial_equipo_4.model.Cliente;
import com.udea.parcial_equipo_4.model.Espectaculo;
import com.udea.parcial_equipo_4.model.Reserva;
import com.udea.parcial_equipo_4.repository.ClienteRepository;
import com.udea.parcial_equipo_4.repository.EspectaculoRepository;
import com.udea.parcial_equipo_4.repository.ReservaRepository;
import com.udea.parcial_equipo_4.service.ReservaPageCacheService;
import com.udea.parcial_equipo_4.service.ReservaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(CacheErrorHandlerFallbackTest.TestConfiguration.class)
class CacheErrorHandlerFallbackTest {

    private static final String CODIGO = "RES-CACHE01";

    @Autowired
    private ReservaService reservaService;

    @MockitoBean
    private ReservaRepository reservaRepository;

    @MockitoBean
    private ClienteRepository clienteRepository;

    @MockitoBean
    private EspectaculoRepository espectaculoRepository;

    @Test
    void consultaSigueAlRepositorioCuandoLaLecturaDeCacheFalla() {
        Reserva reserva = new Reserva(
                11,
                CODIGO,
                new Cliente(1, "Ana", "DOC-123", "3001234567"),
                new Espectaculo(7, "Festival", LocalDate.of(2026, 12, 15), 100),
                2
        );
        when(reservaRepository.findByCodigoReserva(CODIGO)).thenReturn(Optional.of(reserva));

        assertThat(reservaService.consultarPorCodigo(CODIGO).codigoReserva()).isEqualTo(CODIGO);

        verify(reservaRepository).findByCodigoReserva(CODIGO);
    }

    @Configuration(proxyBeanMethods = false)
    @Import({CacheConfig.class, ReservaService.class, ReservaPageCacheService.class})
    static class TestConfiguration {

        @Bean
        CacheManager cacheManager() {
            Cache failingCache = new ConcurrentMapCache("reservas") {
                @Override
                public ValueWrapper get(Object key) {
                    throw new IllegalStateException("Redis no disponible");
                }
            };
            SimpleCacheManager cacheManager = new SimpleCacheManager();
            cacheManager.setCaches(List.of(failingCache));
            return cacheManager;
        }
    }
}
