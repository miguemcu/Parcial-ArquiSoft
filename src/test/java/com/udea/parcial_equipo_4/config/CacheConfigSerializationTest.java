package com.udea.parcial_equipo_4.config;

import com.udea.parcial_equipo_4.dto.EspectaculoResponse;
import com.udea.parcial_equipo_4.dto.ReservaResponse;
import com.udea.parcial_equipo_4.dto.ReservaV2Response;
import com.udea.parcial_equipo_4.service.ReservaPageSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigSerializationTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 12, 15);
    private static final ReservaResponse RESERVA = new ReservaResponse(
            "RES-ABC12345",
            3,
            new ReservaResponse.ClienteResumen(10, "Juana Pérez", "1020304050"),
            new ReservaResponse.EspectaculoResumen(7, "Festival de Jazz", FECHA)
    );

    @Test
    void reservaResponseCompletaIdaYVueltaJsonTipada() {
        assertRoundTrip(CacheConfig.jsonSerializer(ReservaResponse.class), RESERVA);
    }

    @Test
    void reservaV2ResponseCompletaIdaYVueltaJsonTipada() {
        ReservaV2Response response = new ReservaV2Response(
                RESERVA.codigoReserva(),
                RESERVA.cantidadSillas(),
                RESERVA.cliente(),
                RESERVA.espectaculo(),
                120,
                42
        );

        assertRoundTrip(CacheConfig.jsonSerializer(ReservaV2Response.class), response);
    }

    @Test
    void espectaculoResponseCompletaIdaYVueltaJsonTipada() {
        EspectaculoResponse response = new EspectaculoResponse(7, "Festival de Jazz", FECHA, 120);

        assertRoundTrip(CacheConfig.jsonSerializer(EspectaculoResponse.class), response);
    }

    @Test
    void snapshotDePaginaCompletaIdaYVueltaJsonTipada() {
        ReservaPageSnapshot snapshot = new ReservaPageSnapshot(List.of(RESERVA), 2, 10, 25);

        assertRoundTrip(CacheConfig.jsonSerializer(ReservaPageSnapshot.class), snapshot);
    }

    private static <T> void assertRoundTrip(RedisSerializer<T> serializer, T value) {
        assertThat(serializer.deserialize(serializer.serialize(value))).isEqualTo(value);
    }
}
