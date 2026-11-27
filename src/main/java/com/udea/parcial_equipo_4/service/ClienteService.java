package com.udea.parcial_equipo_4.service;

import com.udea.parcial_equipo_4.dto.ClienteResponse;
import com.udea.parcial_equipo_4.dto.CrearClienteRequest;
import com.udea.parcial_equipo_4.exception.ClienteDuplicadoException;
import com.udea.parcial_equipo_4.exception.ClienteNoEncontradoException;
import com.udea.parcial_equipo_4.model.Cliente;
import com.udea.parcial_equipo_4.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Lógica de negocio de clientes.
 */
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional(readOnly = true)
    public ClienteResponse consultarPorId(Integer id) {
        return clienteRepository.findById(id)
                .map(ClienteService::toResponse)
                .orElseThrow(() -> new ClienteNoEncontradoException(id));
    }

    /**
     * Registra un cliente. El documento es único: si ya existe se lanza
     * {@link ClienteDuplicadoException} (409). La restricción UNIQUE de la base
     * cubre el caso de dos registros simultáneos con el mismo documento.
     */
    @Transactional
    public ClienteResponse crear(CrearClienteRequest request) {
        String documento = request.documento().trim();
        if (clienteRepository.existsByDocumento(documento)) {
            throw new ClienteDuplicadoException();
        }
        Cliente cliente = new Cliente(null, request.nombre().trim(), documento, normalizarTelefono(request.telefono()));
        try {
            return toResponse(clienteRepository.saveAndFlush(cliente));
        } catch (DataIntegrityViolationException ex) {
            throw new ClienteDuplicadoException();
        }
    }

    private static String normalizarTelefono(String telefono) {
        return StringUtils.hasText(telefono) ? telefono.trim() : null;
    }

    private static ClienteResponse toResponse(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNombre());
    }
}
