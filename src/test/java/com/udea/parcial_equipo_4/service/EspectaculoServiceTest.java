package com.udea.parcial_equipo_4.service;

import com.udea.parcial_equipo_4.dto.CrearEspectaculoRequest;
import com.udea.parcial_equipo_4.dto.EspectaculoResponse;
import com.udea.parcial_equipo_4.model.Espectaculo;
import com.udea.parcial_equipo_4.repository.EspectaculoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EspectaculoServiceTest {

    private static final LocalDate FECHA = LocalDate.of(2027, 2, 14);

    @Mock
    private EspectaculoRepository espectaculoRepository;

    @InjectMocks
    private EspectaculoService espectaculoService;

    @Test
    void crearGuardaElEspectaculo() {
        when(espectaculoRepository.save(any(Espectaculo.class))).thenAnswer(inv -> {
            Espectaculo e = inv.getArgument(0);
            e.setId(9);
            return e;
        });

        EspectaculoResponse respuesta = espectaculoService.crear(new CrearEspectaculoRequest(" Rock ", FECHA, 50));

        assertThat(respuesta).isEqualTo(new EspectaculoResponse(9, "Rock", FECHA, 50));
    }

    @Test
    void listarLimitaElTamanoDePaginaYOrdenaPorFecha() {
        when(espectaculoRepository.findAll(any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(new Espectaculo(1, "Rock", FECHA, 50))));

        Page<EspectaculoResponse> pagina = espectaculoService.listar(2, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(espectaculoRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(EspectaculoService.TAMANO_MAXIMO_PAGINA);
        assertThat(captor.getValue().getSort().getOrderFor("fecha")).isNotNull();
        assertThat(pagina.getContent()).containsExactly(new EspectaculoResponse(1, "Rock", FECHA, 50));
    }
}
