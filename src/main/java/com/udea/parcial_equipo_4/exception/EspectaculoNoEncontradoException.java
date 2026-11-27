package com.udea.parcial_equipo_4.exception;

/**
 * No existe un espectáculo con el id indicado (404).
 */
public class EspectaculoNoEncontradoException extends RecursoNoEncontradoException {

    public EspectaculoNoEncontradoException(Integer id) {
        super("No existe un espectáculo con id %d".formatted(id));
    }
}
