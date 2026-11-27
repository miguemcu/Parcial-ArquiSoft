package com.udea.parcial_equipo_4.exception;

/**
 * No existe un cliente con el id indicado (404).
 */
public class ClienteNoEncontradoException extends RecursoNoEncontradoException {

    public ClienteNoEncontradoException(Integer id) {
        super("No existe un cliente con id %d".formatted(id));
    }
}
