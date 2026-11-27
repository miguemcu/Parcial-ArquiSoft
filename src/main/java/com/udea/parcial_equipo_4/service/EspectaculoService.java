package com.udea.parcial_equipo_4.service;

import com.udea.parcial_equipo_4.dto.EspectaculoResponse;
import com.udea.parcial_equipo_4.exception.EspectaculoNoEncontradoException;
import com.udea.parcial_equipo_4.model.Espectaculo;
import com.udea.parcial_equipo_4.repository.EspectaculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EspectaculoService {

    private final EspectaculoRepository espectaculoRepository;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "espectaculos", key = "#id")
    public EspectaculoResponse consultarPorId(Integer id) {
        return espectaculoRepository.findById(id)
                .map(EspectaculoService::toResponse)
                .orElseThrow(() -> new EspectaculoNoEncontradoException(id));
    }

    private static EspectaculoResponse toResponse(Espectaculo espectaculo) {
        return new EspectaculoResponse(
                espectaculo.getId(),
                espectaculo.getNombre(),
                espectaculo.getFecha(),
                espectaculo.getCantidadSillas()
        );
    }
}
