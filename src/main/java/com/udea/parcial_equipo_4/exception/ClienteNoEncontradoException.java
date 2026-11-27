package com.udea.parcial_equipo_4.exception;

public class ClienteNoEncontradoException extends RecursoNoEncontradoException {

    public ClienteNoEncontradoException(Integer id) {
        super("No existe un cliente con id %d".formatted(id));
    }
}
