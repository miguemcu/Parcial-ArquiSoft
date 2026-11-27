package com.udea.parcial_equipo_4.service;

import com.udea.parcial_equipo_4.dto.ClienteResponse;
import com.udea.parcial_equipo_4.dto.CrearClienteRequest;
import com.udea.parcial_equipo_4.exception.ClienteDuplicadoException;
import com.udea.parcial_equipo_4.model.Cliente;
import com.udea.parcial_equipo_4.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    @Test
    void crearGuardaElClienteNormalizado() {
        when(clienteRepository.existsByDocumento("1017123456")).thenReturn(false);
        when(clienteRepository.saveAndFlush(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(5);
            return c;
        });

        ClienteResponse respuesta = clienteService.crear(new CrearClienteRequest("  Ana  ", " 1017123456 ", " "));

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Ana");
        assertThat(captor.getValue().getDocumento()).isEqualTo("1017123456");
        assertThat(captor.getValue().getTelefono()).isNull();
        assertThat(respuesta).isEqualTo(new ClienteResponse(5, "Ana"));
    }

    @Test
    void crearConDocumentoExistenteLanzaDuplicadoSinGuardar() {
        when(clienteRepository.existsByDocumento("1017123456")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.crear(new CrearClienteRequest("Ana", "1017123456", null)))
                .isInstanceOf(ClienteDuplicadoException.class);
        verify(clienteRepository, never()).saveAndFlush(any());
    }

    @Test
    void crearConViolacionDeUnicidadConcurrenteLanzaDuplicado() {
        when(clienteRepository.existsByDocumento("1017123456")).thenReturn(false);
        when(clienteRepository.saveAndFlush(any(Cliente.class)))
                .thenThrow(new DataIntegrityViolationException("uk_documento"));

        assertThatThrownBy(() -> clienteService.crear(new CrearClienteRequest("Ana", "1017123456", null)))
                .isInstanceOf(ClienteDuplicadoException.class);
    }
}
