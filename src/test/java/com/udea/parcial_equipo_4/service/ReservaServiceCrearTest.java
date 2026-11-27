package com.udea.parcial_equipo_4.service;

import com.udea.parcial_equipo_4.dto.CrearReservaRequest;
import com.udea.parcial_equipo_4.dto.ReservaResponse;
import com.udea.parcial_equipo_4.exception.ClienteNoEncontradoException;
import com.udea.parcial_equipo_4.exception.EspectaculoNoEncontradoException;
import com.udea.parcial_equipo_4.exception.ReservaNoEncontradaException;
import com.udea.parcial_equipo_4.exception.SillasInsuficientesException;
import com.udea.parcial_equipo_4.model.Cliente;
import com.udea.parcial_equipo_4.model.Espectaculo;
import com.udea.parcial_equipo_4.model.Reserva;
import com.udea.parcial_equipo_4.repository.ClienteRepository;
import com.udea.parcial_equipo_4.repository.EspectaculoRepository;
import com.udea.parcial_equipo_4.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceCrearTest {

    private static final Cliente CLIENTE = new Cliente(1, "Ana", "1017123456", null);
    private static final Espectaculo ESPECTACULO = new Espectaculo(2, "Rock", LocalDate.of(2027, 2, 14), 10);

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private EspectaculoRepository espectaculoRepository;
    @Mock
    private ReservaPageCacheService reservaPageCacheService;
    @Spy
    private ReservaMapper reservaMapper = new ReservaMapper();

    @InjectMocks
    private ReservaService reservaService;

    @Test
    void crearConSillasDisponiblesGuardaLaReservaConCodigoValido() {
        when(clienteRepository.findById(1)).thenReturn(Optional.of(CLIENTE));
        when(espectaculoRepository.findByIdForUpdate(2)).thenReturn(Optional.of(ESPECTACULO));
        when(reservaRepository.sumarSillasReservadas(2)).thenReturn(7);
        when(reservaRepository.existsByCodigoReserva(anyString())).thenReturn(true, false);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservaResponse respuesta = reservaService.crear(new CrearReservaRequest(1, 2, 3));

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());
        assertThat(captor.getValue().getCodigoReserva()).matches("RES-[A-Z0-9]{8}");
        assertThat(respuesta.cantidadSillas()).isEqualTo(3);
        assertThat(respuesta.cliente().id()).isEqualTo(1);
        assertThat(respuesta.espectaculo().id()).isEqualTo(2);
    }

    @Test
    void crearConMasSillasQueLasDisponiblesLanzaConflicto() {
        when(clienteRepository.findById(1)).thenReturn(Optional.of(CLIENTE));
        when(espectaculoRepository.findByIdForUpdate(2)).thenReturn(Optional.of(ESPECTACULO));
        when(reservaRepository.sumarSillasReservadas(2)).thenReturn(8);

        assertThatThrownBy(() -> reservaService.crear(new CrearReservaRequest(1, 2, 3)))
                .isInstanceOf(SillasInsuficientesException.class)
                .hasMessageContaining("solo quedan 2");
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void crearConClienteInexistenteLanzaNoEncontrado() {
        when(clienteRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.crear(new CrearReservaRequest(1, 2, 3)))
                .isInstanceOf(ClienteNoEncontradoException.class);
        verify(espectaculoRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void crearConEspectaculoInexistenteLanzaNoEncontrado() {
        when(clienteRepository.findById(1)).thenReturn(Optional.of(CLIENTE));
        when(espectaculoRepository.findByIdForUpdate(2)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.crear(new CrearReservaRequest(1, 2, 3)))
                .isInstanceOf(EspectaculoNoEncontradoException.class);
    }

    @Test
    void crearSinCodigoUnicoTrasVariosIntentosFalla() {
        when(clienteRepository.findById(1)).thenReturn(Optional.of(CLIENTE));
        when(espectaculoRepository.findByIdForUpdate(2)).thenReturn(Optional.of(ESPECTACULO));
        when(reservaRepository.sumarSillasReservadas(2)).thenReturn(0);
        when(reservaRepository.existsByCodigoReserva(anyString())).thenReturn(true);

        assertThatThrownBy(() -> reservaService.crear(new CrearReservaRequest(1, 2, 1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void consultarV2CalculaLasSillasDisponibles() {
        Reserva reserva = new Reserva(5, "RES-ABC12345", CLIENTE, ESPECTACULO, 2);
        when(reservaRepository.findByCodigoReserva("RES-ABC12345")).thenReturn(Optional.of(reserva));
        when(reservaRepository.sumarSillasReservadas(2)).thenReturn(6);

        var respuesta = reservaService.consultarPorCodigoV2("RES-ABC12345");

        assertThat(respuesta.totalSillasEspectaculo()).isEqualTo(10);
        assertThat(respuesta.sillasDisponiblesEspectaculo()).isEqualTo(4);
    }

    @Test
    void consultarCodigoInexistenteLanzaNoEncontrado() {
        when(reservaRepository.findByCodigoReserva("RES-NOEXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.consultarPorCodigo("RES-NOEXISTE"))
                .isInstanceOf(ReservaNoEncontradaException.class);
    }
}
