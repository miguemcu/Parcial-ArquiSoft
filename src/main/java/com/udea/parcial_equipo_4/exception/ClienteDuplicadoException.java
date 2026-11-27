package com.udea.parcial_equipo_4.exception;

/**
 * Ya existe un cliente con el mismo documento. El mensaje no incluye el
 * documento para no exponer ni registrar datos sensibles.
 */
public class ClienteDuplicadoException extends RuntimeException {

    public ClienteDuplicadoException() {
        super("Ya existe un cliente registrado con ese documento");
    }
}
