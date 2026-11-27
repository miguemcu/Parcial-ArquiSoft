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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private static final String PREFIJO_CODIGO = "RES-";
    private static final String ALFABETO_CODIGO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int LONGITUD_SUFIJO_CODIGO = 8;
    private static final int MAX_INTENTOS_CODIGO = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;
    private final EspectaculoRepository espectaculoRepository;

    @Transactional(readOnly = true)
    public ReservaResponse consultarPorCodigo(String codigo) {
        return reservaRepository.findByCodigoReserva(codigo)
                .map(this::toResponse)
                .orElseThrow(() -> new ReservaNoEncontradaException(codigo));
    }

    @Transactional
    public ReservaResponse crear(CrearReservaRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException(request.clienteId()));

        Espectaculo espectaculo = espectaculoRepository.findByIdForUpdate(request.espectaculoId())
                .orElseThrow(() -> new EspectaculoNoEncontradoException(request.espectaculoId()));

        int reservadas = reservaRepository.sumarSillasReservadas(espectaculo.getId());
        int disponibles = Math.max(espectaculo.getCantidadSillas() - reservadas, 0);
        if (request.cantidadSillas() > disponibles) {
            throw new SillasInsuficientesException(request.cantidadSillas(), disponibles);
        }

        Reserva reserva = new Reserva(null, generarCodigoUnico(), cliente, espectaculo, request.cantidadSillas());
        return toResponse(reservaRepository.save(reserva));
    }

    private String generarCodigoUnico() {
        for (int intento = 0; intento < MAX_INTENTOS_CODIGO; intento++) {
            String codigo = generarCodigo();
            if (!reservaRepository.existsByCodigoReserva(codigo)) {
                return codigo;
            }
        }
        throw new IllegalStateException("No fue posible generar un código de reserva único");
    }

    private String generarCodigo() {
        StringBuilder codigo = new StringBuilder(PREFIJO_CODIGO);
        for (int i = 0; i < LONGITUD_SUFIJO_CODIGO; i++) {
            codigo.append(ALFABETO_CODIGO.charAt(RANDOM.nextInt(ALFABETO_CODIGO.length())));
        }
        return codigo.toString();
    }

    private ReservaResponse toResponse(Reserva reserva) {
        Cliente cliente = reserva.getCliente();
        Espectaculo espectaculo = reserva.getEspectaculo();
        return new ReservaResponse(
                reserva.getCodigoReserva(),
                reserva.getCantidadSillas(),
                new ReservaResponse.ClienteResumen(cliente.getId(), cliente.getNombre(), cliente.getDocumento()),
                new ReservaResponse.EspectaculoResumen(espectaculo.getId(), espectaculo.getNombre(), espectaculo.getFecha())
        );
    }
}
