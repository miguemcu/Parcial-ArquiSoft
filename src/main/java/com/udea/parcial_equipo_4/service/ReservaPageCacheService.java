package com.udea.parcial_equipo_4.service;

import com.udea.parcial_equipo_4.exception.EspectaculoNoEncontradoException;
import com.udea.parcial_equipo_4.model.Reserva;
import com.udea.parcial_equipo_4.repository.EspectaculoRepository;
import com.udea.parcial_equipo_4.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listado paginado de reservas por espectáculo, cacheado en {@code reservas-por-espectaculo}.
 */
@Service
@RequiredArgsConstructor
public class ReservaPageCacheService {

    private final ReservaRepository reservaRepository;
    private final EspectaculoRepository espectaculoRepository;
    private final ReservaMapper reservaMapper;

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "reservas-por-espectaculo",
            key = "#espectaculoId + ':' + #page + ':' + T(com.udea.parcial_equipo_4.service.ReservaPageCacheService).normalizarTamanoPagina(#size)"
    )
    public ReservaPageSnapshot listarPorEspectaculo(Integer espectaculoId, int page, int size) {
        if (!espectaculoRepository.existsById(espectaculoId)) {
            throw new EspectaculoNoEncontradoException(espectaculoId);
        }

        int tamanoEfectivo = normalizarTamanoPagina(size);
        PageRequest pageRequest = PageRequest.of(page, tamanoEfectivo,
                Sort.by(Sort.Direction.DESC, "id"));
        Page<Reserva> reservas = reservaRepository.findByEspectaculoId(espectaculoId, pageRequest);
        return ReservaPageSnapshot.from(reservas.map(reservaMapper::toResponse));
    }

    public static int normalizarTamanoPagina(int size) {
        return Math.min(size, ReservaService.TAMANO_MAXIMO_PAGINA);
    }
}
