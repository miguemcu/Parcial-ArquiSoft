package com.udea.parcial_equipo_4.service;

import com.udea.parcial_equipo_4.exception.EspectaculoNoEncontradoException;
import com.udea.parcial_equipo_4.repository.EspectaculoRepository;
import com.udea.parcial_equipo_4.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServicePaginacionTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private EspectaculoRepository espectaculoRepository;

    @InjectMocks
    private ReservaPageCacheService reservaPageCacheService;

    @Test
    void sizeMayorA50SeLimitaA50YOrdenaPorIdDescendente() {
        when(espectaculoRepository.existsById(1)).thenReturn(true);
        when(reservaRepository.findByEspectaculoId(eq(1), any(Pageable.class)))
                .thenAnswer(invocation -> Page.empty(invocation.getArgument(1)));

        reservaPageCacheService.listarPorEspectaculo(1, 2, 100);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(reservaRepository).findByEspectaculoId(eq(1), captor.capture());
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(ReservaService.TAMANO_MAXIMO_PAGINA);
        assertThat(pageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void sizeDentroDelLimiteSeRespeta() {
        when(espectaculoRepository.existsById(1)).thenReturn(true);
        when(reservaRepository.findByEspectaculoId(eq(1), any(Pageable.class)))
                .thenAnswer(invocation -> Page.empty(invocation.getArgument(1)));

        reservaPageCacheService.listarPorEspectaculo(1, 0, 10);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(reservaRepository).findByEspectaculoId(eq(1), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void espectaculoInexistenteLanzaNoEncontrado() {
        when(espectaculoRepository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> reservaPageCacheService.listarPorEspectaculo(99, 0, 10))
                .isInstanceOf(EspectaculoNoEncontradoException.class);
        verify(reservaRepository, never()).findByEspectaculoId(any(), any());
    }
}
