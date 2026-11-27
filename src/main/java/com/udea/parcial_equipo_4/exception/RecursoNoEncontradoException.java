package com.udea.parcial_equipo_4.exception;

/**
 * Base de las excepciones que se responden con 404.
 */
public abstract class RecursoNoEncontradoException extends RuntimeException {

    protected RecursoNoEncontradoException(String message) {
        super(message);
    }
}
