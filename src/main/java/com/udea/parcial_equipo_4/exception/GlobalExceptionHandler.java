package com.udea.parcial_equipo_4.exception;

import com.udea.parcial_equipo_4.config.ApiVersionConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.accept.MissingApiVersionException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final List<String> versionesSoportadas;

    public GlobalExceptionHandler(@Value("${spring.mvc.apiversion.supported}") List<String> versionesSoportadas) {
        this.versionesSoportadas = List.copyOf(versionesSoportadas);
    }

    @ExceptionHandler(MissingApiVersionException.class)
    public ProblemDetail handleVersionFaltante(MissingApiVersionException ex) {
        return problemVersion("Falta el header '%s'".formatted(ApiVersionConfig.HEADER_VERSION));
    }

    @ExceptionHandler(InvalidApiVersionException.class)
    public ProblemDetail handleVersionInvalida(InvalidApiVersionException ex, HttpServletRequest request) {
        return problemVersion("La versión '%s' no está soportada".formatted(request.getHeader(ApiVersionConfig.HEADER_VERSION)));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail handleNotFound(RecursoNoEncontradoException ex) {
        return problem(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(SillasInsuficientesException.class)
    public ProblemDetail handleSillasInsuficientes(SillasInsuficientesException ex) {
        return problem(HttpStatus.CONFLICT, "Sillas insuficientes", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Error inesperado procesando la solicitud", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado. Intente nuevamente más tarde.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));

        String detalle = errores.entrySet().stream()
                .map(e -> e.getKey() + " " + e.getValue())
                .collect(Collectors.joining("; ", "Campos inválidos: ", ""));

        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Solicitud inválida", detalle);
        body.setProperty("errores", errores);
        return ResponseEntity.badRequest().headers(headers).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers,
                                                                          HttpStatusCode status,
                                                                          WebRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Parámetro requerido",
                "El parámetro '%s' es obligatorio".formatted(ex.getParameterName()));
        return ResponseEntity.badRequest().headers(headers).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException ex,
                                                                          HttpHeaders headers,
                                                                          HttpStatusCode status,
                                                                          WebRequest request) {
        if (ex instanceof UnsatisfiedServletRequestParameterException) {
            ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Parámetros inválidos",
                    "Envíe exactamente uno de los parámetros 'codigo' o 'espectaculoId'");
            return ResponseEntity.badRequest().headers(headers).body(body);
        }
        return super.handleServletRequestBindingException(ex, headers, status, request);
    }

    private ProblemDetail problemVersion(String motivo) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Versión de API inválida",
                "%s. Versiones soportadas: %s".formatted(motivo, String.join(", ", versionesSoportadas)));
        body.setProperty("versionesSoportadas", versionesSoportadas);
        return body;
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
