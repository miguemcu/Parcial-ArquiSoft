package com.udea.parcial_equipo_4.config;

import com.udea.parcial_equipo_4.dto.EspectaculoResponse;
import com.udea.parcial_equipo_4.dto.ReservaResponse;
import com.udea.parcial_equipo_4.dto.ReservaV2Response;
import com.udea.parcial_equipo_4.service.ReservaPageSnapshot;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.time.Duration;
import java.time.LocalDate;

/**
 * Caché Redis: TTL y serializador JSON tipado por cada caché, y manejo de errores
 * que deja seguir sin caché si Redis no responde.
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheConfig.class);
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .addModule(new SimpleModule("cache-local-date")
                    .addSerializer(LocalDate.class, new ValueSerializer<>() {
                        @Override
                        public void serialize(LocalDate value, JsonGenerator generator,
                                              SerializationContext context) throws JacksonException {
                            generator.writeString(value.toString());
                        }
                    })
                    .addDeserializer(LocalDate.class, new ValueDeserializer<>() {
                        @Override
                        public LocalDate deserialize(JsonParser parser, DeserializationContext context)
                                throws JacksonException {
                            return LocalDate.parse(parser.getString());
                        }
                    }))
            .build();

    @Bean
    RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> builder
                .transactionAware()
                .withCacheConfiguration("reservas",
                        cacheConfiguration(ReservaResponse.class, Duration.ofMinutes(10)))
                .withCacheConfiguration("espectaculos",
                        cacheConfiguration(EspectaculoResponse.class, Duration.ofMinutes(10)))
                .withCacheConfiguration("reservas-v2",
                        cacheConfiguration(ReservaV2Response.class, Duration.ofMinutes(2)))
                .withCacheConfiguration("reservas-por-espectaculo",
                        cacheConfiguration(ReservaPageSnapshot.class, Duration.ofMinutes(2)));
    }

    static <T> RedisCacheConfiguration cacheConfiguration(Class<T> type, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(jsonSerializer(type)));
    }

    static <T> RedisSerializer<T> jsonSerializer(Class<T> type) {
        return new JacksonJsonRedisSerializer<>(JSON_MAPPER, type);
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                logCacheFailure("lectura", cache, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                logCacheFailure("escritura", cache, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                logCacheFailure("invalidación", cache, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                logCacheFailure("limpieza", cache, exception);
            }
        };
    }

    private static void logCacheFailure(String operation, Cache cache, RuntimeException exception) {
        LOGGER.warn("Falló la {} de la caché '{}'; se continúa sin caché ({})",
                operation, cache.getName(), exception.getClass().getSimpleName());
    }
}
