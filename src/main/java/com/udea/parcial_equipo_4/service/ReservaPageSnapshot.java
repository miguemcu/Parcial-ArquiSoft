package com.udea.parcial_equipo_4.service;

import com.udea.parcial_equipo_4.dto.ReservaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

public record ReservaPageSnapshot(
        List<ReservaResponse> contenido,
        int numeroPagina,
        int tamanoPagina,
        long totalElementos
) {

    public ReservaPageSnapshot {
        contenido = List.copyOf(contenido);
    }

    static ReservaPageSnapshot from(Page<ReservaResponse> page) {
        return new ReservaPageSnapshot(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }

    Page<ReservaResponse> toPage() {
        return new PageImpl<>(
                contenido,
                PageRequest.of(numeroPagina, tamanoPagina, Sort.by(Sort.Direction.DESC, "id")),
                totalElementos
        );
    }
}
