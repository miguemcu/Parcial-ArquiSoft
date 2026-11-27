package com.udea.parcial_equipo_4.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Documentación OpenAPI: un grupo de Swagger UI por versión de la API (v1, v2).
 * En cada grupo solo aparecen los endpoints que atienden esa versión y el header
 * X-API-Version figura como obligatorio con el valor de la versión.
 */
@Configuration
public class OpenApiConfig {

    private static final String PATRON_API = "/api/**";
    private static final String RUTA_RESERVAS = "/api/reservas";

    @Bean
    OpenAPI reservasOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de reservas de sillas para conciertos")
                        .description("""
                                Consulta y creación de reservas de sillas por espectáculo.

                                **Endpoints requeridos por el enunciado**
                                1. `GET /api/reservas?codigo=RES-XXXXXXXX`: consulta la reserva de sillas                                 de un espectáculo usando el código de reserva como parámetro de consulta.
                                2. `POST /api/reservas`: registra una nueva reserva de sillas                                 (`clienteId`, `espectaculoId`, `cantidadSillas`).

                                **Endpoints de apoyo para probar**
                                - `POST /api/clientes` y `GET /api/clientes/{id}`: registrar y consultar el cliente que reserva.
                                - `POST /api/espectaculos`, `GET /api/espectaculos` y `GET /api/espectaculos/{id}`:                                 registrar, listar y consultar espectáculos.
                                - `GET /api/reservas?espectaculoId=1`: listar las reservas de un espectáculo (paginado).
                                - `GET /actuator/health`: estado de la aplicación (Actuator, sin header de versión).

                                Todas las peticiones a /api/** requieren el header %s (versiones soportadas: 1, 2)."""
                                .formatted(ApiVersionConfig.HEADER_VERSION))
                        .version("1"));
    }

    @Bean
    GroupedOpenApi apiV1() {
        return grupoPorVersion(1);
    }

    @Bean
    GroupedOpenApi apiV2() {
        return grupoPorVersion(2);
    }

    private static GroupedOpenApi grupoPorVersion(int version) {
        return GroupedOpenApi.builder()
                .group("v" + version)
                .displayName("Versión " + version)
                .pathsToMatch(PATRON_API)
                .addOpenApiMethodFilter(metodo -> atiendeVersion(metodo, version))
                .addOpenApiCustomizer(documentarVersion(version))
                .build();
    }

    /**
     * Interpreta el atributo version del mapeo: "2" es exacta y "1+" es esa versión o superiores.
     */
    private static boolean atiendeVersion(Method metodo, int version) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(metodo, RequestMapping.class);
        if (mapping == null || mapping.version().isBlank()) {
            return true;
        }
        String declarada = mapping.version().trim();
        if (declarada.endsWith("+")) {
            return version >= Integer.parseInt(declarada.substring(0, declarada.length() - 1));
        }
        return version == Integer.parseInt(declarada);
    }

    private static OpenApiCustomizer documentarVersion(int version) {
        return openApi -> {
            openApi.getInfo().setVersion(String.valueOf(version));
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((ruta, item) -> item.readOperations()
                    .forEach(operacion -> exigirHeaderVersion(operacion, version)));

            var reservas = openApi.getPaths().get(RUTA_RESERVAS);
            if (reservas != null && reservas.getGet() != null) {
                documentarGetReservas(reservas.getGet(), version);
            }
        };
    }

    private static void exigirHeaderVersion(Operation operacion, int version) {
        List<Parameter> parametros = operacion.getParameters() == null
                ? new ArrayList<>() : new ArrayList<>(operacion.getParameters());
        parametros.removeIf(p -> ApiVersionConfig.HEADER_VERSION.equalsIgnoreCase(p.getName()));

        StringSchema schema = new StringSchema();
        schema.setEnum(List.of(String.valueOf(version)));
        schema.setDefault(String.valueOf(version));
        parametros.addFirst(new HeaderParameter()
                .name(ApiVersionConfig.HEADER_VERSION)
                .required(true)
                .description("Versión de la API. Si falta o no está soportada, la respuesta es 400.")
                .schema(schema));
        operacion.setParameters(parametros);
    }

    /**
     * OpenAPI solo admite una operación por ruta y método, así que la consulta por código y el
     * listado paginado de GET /api/reservas se documentan juntos, con sus parámetros opcionales.
     */
    private static void documentarGetReservas(Operation operacion, int version) {
        operacion.setOperationId("consultarOListarReservas");
        operacion.setSummary("Consultar una reserva por código o listar las reservas de un espectáculo");
        String detalleV2 = version >= 2
                ? " En la versión 2 la consulta por código agrega totalSillasEspectaculo y sillasDisponiblesEspectaculo."
                : "";
        operacion.setDescription("""
                Dos modos excluyentes:
                - `?codigo=RES-XXXXXXXX`: devuelve una reserva (EntityModel).
                - `?espectaculoId=1&page=0&size=10`: devuelve una página de reservas (PagedModel), \
                ordenada por id descendente; size por defecto 10 y máximo 50.

                Si llegan ambos parámetros, o ninguno, la respuesta es 400.""" + detalleV2);
        if (operacion.getParameters() != null) {
            operacion.getParameters().stream()
                    .filter(p -> "codigo".equals(p.getName()) || "espectaculoId".equals(p.getName()))
                    .forEach(p -> p.setRequired(false));
        }
    }
}
