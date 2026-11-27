package com.udea.parcial_equipo_4.exception;

public class ReservaNoEncontradaException extends RecursoNoEncontradoException {

    public ReservaNoEncontradaException(String codigo) {
        super("No existe una reserva con código '%s'".formatted(codigo));
    }
}
