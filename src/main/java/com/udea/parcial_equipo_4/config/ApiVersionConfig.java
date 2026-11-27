package com.udea.parcial_equipo_4.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.MissingApiVersionException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Exige el header de versión solo en los endpoints de negocio (/api/**).
 * La propiedad spring.mvc.apiversion.required aplica a todos los controladores,
 * incluidos los de springdoc, por eso la obligatoriedad se controla aquí.
 */
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    public static final String HEADER_VERSION = "X-API-Version";

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (!StringUtils.hasText(request.getHeader(HEADER_VERSION))) {
                    throw new MissingApiVersionException();
                }
                return true;
            }
        }).addPathPatterns("/api/**");
    }
}
