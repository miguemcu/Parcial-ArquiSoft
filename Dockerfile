# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------
# Etapa 1: compilación con el wrapper de Maven y JDK 21
# ---------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Primero solo lo necesario para resolver dependencias: esta capa se
# reutiliza mientras no cambie el pom.xml.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Luego el código. Los tests y la cobertura los corre el CI.
COPY lombok.config ./
COPY src src
RUN ./mvnw -B -q package -DskipTests -Djacoco.skip=true \
    && cp target/*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --launcher --destination extracted

# ---------------------------------------------------------------------
# Etapa 2: runtime con JRE 21 ligero y usuario no root
# ---------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S -G app app

# Capas de Spring Boot, de la que menos cambia a la que más cambia.
COPY --from=build --chown=app:app /workspace/extracted/dependencies/ ./
COPY --from=build --chown=app:app /workspace/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /workspace/extracted/application/ ./

USER app

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

# La imagen alpine trae wget (busybox); no hace falta instalar curl.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q -O /dev/null http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
