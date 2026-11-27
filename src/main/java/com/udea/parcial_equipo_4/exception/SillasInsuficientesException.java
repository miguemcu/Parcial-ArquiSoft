package com.udea.parcial_equipo_4.exception;

/**
 * La reserva pide más sillas de las disponibles en el espectáculo (409).
 */
public class SillasInsuficientesException extends RuntimeException {

    public SillasInsuficientesException(int solicitadas, int disponibles) {
        super("Se solicitaron %d sillas pero solo quedan %d disponibles".formatted(solicitadas, disponibles));
    }
}
