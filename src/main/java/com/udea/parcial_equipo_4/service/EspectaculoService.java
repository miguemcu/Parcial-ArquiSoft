package com.udea.parcial_equipo_4.service;

import com.udea.parcial_equipo_4.dto.CrearEspectaculoRequest;
import com.udea.parcial_equipo_4.dto.EspectaculoResponse;
import com.udea.parcial_equipo_4.exception.EspectaculoNoEncontradoException;
import com.udea.parcial_equipo_4.model.Espectaculo;
import com.udea.parcial_equipo_4.repository.EspectaculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de negocio de espectáculos; la consulta por id usa la caché {@code espectaculos}.
 */
@Service
@RequiredArgsConstructor
public class EspectaculoService {

    public static final int TAMANO_MAXIMO_PAGINA = 50;

    private final EspectaculoRepository espectaculoRepository;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "espectaculos", key = "#id")
    public EspectaculoResponse consultarPorId(Integer id) {
        return espectaculoRepository.findById(id)
                .map(EspectaculoService::toResponse)
                .orElseThrow(() -> new EspectaculoNoEncontradoException(id));
    }

    /**
     * Lista los espectáculos ordenados por fecha (y por id para desempatar).
     * Un tamaño de página mayor a {@value #TAMANO_MAXIMO_PAGINA} se limita a ese máximo.
     */
    @Transactional(readOnly = true)
    public Page<EspectaculoResponse> listar(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, TAMANO_MAXIMO_PAGINA),
                Sort.by("fecha").ascending().and(Sort.by("id").ascending()));
        return espectaculoRepository.findAll(pageRequest).map(EspectaculoService::toResponse);
    }

    /**
     * Registra un espectáculo y deja su consulta por id en caché.
     */
    @Transactional
    @CachePut(cacheNames = "espectaculos", key = "#result.id")
    public EspectaculoResponse crear(CrearEspectaculoRequest request) {
        Espectaculo espectaculo = new Espectaculo(null, request.nombre().trim(), request.fecha(), request.cantidadSillas());
        return toResponse(espectaculoRepository.save(espectaculo));
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
