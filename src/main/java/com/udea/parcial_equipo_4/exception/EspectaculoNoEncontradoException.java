package com.udea.parcial_equipo_4.exception;

public class EspectaculoNoEncontradoException extends RecursoNoEncontradoException {

    public EspectaculoNoEncontradoException(Integer id) {
        super("No existe un espectáculo con id %d".formatted(id));
    }
}
