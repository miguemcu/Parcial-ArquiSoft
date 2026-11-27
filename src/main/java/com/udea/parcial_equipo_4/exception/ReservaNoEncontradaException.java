package com.udea.parcial_equipo_4.exception;

/**
 * No existe una reserva con el código indicado (404).
 */
public class ReservaNoEncontradaException extends RecursoNoEncontradoException {

    public ReservaNoEncontradaException(String codigo) {
        super("No existe una reserva con código '%s'".formatted(codigo));
    }
}
