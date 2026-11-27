package com.udea.parcial_equipo_4.service;

import com.udea.parcial_equipo_4.dto.ClienteResponse;
import com.udea.parcial_equipo_4.exception.ClienteNoEncontradoException;
import com.udea.parcial_equipo_4.model.Cliente;
import com.udea.parcial_equipo_4.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static ClienteResponse toResponse(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNombre());
    }
}
