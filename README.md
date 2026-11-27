# API de Reservas de Sillas para Conciertos

API RESTful para la gestión y consulta de reservas de sillas por espectáculo, desarrollada por el **Equipo 4** para el curso de **Arquitectura de Software** (Universidad de Antioquia).

---

## Stack tecnológico

| Área | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.2.0 (Maven) |
| Persistencia | Spring Data JPA + PostgreSQL + HikariCP |
| Migraciones | Flyway (`V1__esquema_inicial.sql`) |
| Caché | Spring Cache + Redis (serialización JSON tipada) |
| API y contratos | Spring Web MVC, Spring HATEOAS, Jakarta Validation, springdoc-openapi |
| Observabilidad | Spring Boot Actuator + Micrometer (Prometheus) |
| Pruebas | JUnit 5, Mockito, H2 (modo PostgreSQL), Testcontainers |
| CI y calidad | GitHub Actions, JaCoCo, SonarCloud |
| Utilidades | Lombok |

---

## Modelo de dominio y reglas de negocio

El esquema relacional (gestionado por Flyway en [`src/main/resources/db/migration/V1__esquema_inicial.sql`](src/main/resources/db/migration/V1__esquema_inicial.sql)) define tres entidades principales:

- **`espectaculo`**: `id`, `nombre`, `fecha`, `cantidad_sillas` (capacidad total `>= 0`).
- **`cliente`**: `id`, `nombre`, `documento` (único), `telefono`.
- **`reserva`**: `id`, `codigo_reserva` (único, máx. 20 caracteres), `cliente_id`, `espectaculo_id`, `cantidad_sillas` (`> 0`).

Reglas principales:
- **Disponibilidad agregada**: no se asignan sillas individuales; los cupos disponibles se calculan como `espectaculo.cantidad_sillas - SUM(reserva.cantidad_sillas)`.
- **Control de concurrencia**: al crear una reserva, la fila del espectáculo se bloquea con `PESSIMISTIC_WRITE` dentro de una transacción (`@Transactional`) para evitar sobreventa.
- **Código de reserva**: se genera con el formato `RES-` seguido de 8 caracteres alfanuméricos en mayúscula, validando unicidad antes de persistir.

---

## Arquitectura

El proyecto sigue una arquitectura en capas bajo el paquete base `com.udea.parcial_equipo_4`:

```text
controller  ──▶  service  ──▶  repository  ──▶  PostgreSQL / Redis
     │              │
     ▼              ▼
    dto           model (entidades JPA)
```

- **`controller`**: controladores REST delgados (`ReservaController`, `ClienteController`, `EspectaculoController`) y ensamblador HATEOAS (`ReservaModelAssembler`).
- **`service`**: lógica de negocio, transacciones, mapeo DTO (`ReservaMapper`) y caché (`ReservaService`, `ClienteService`, `EspectaculoService`, `ReservaPageCacheService`).
- **`repository`**: interfaces Spring Data JPA (`ReservaRepository`, `ClienteRepository`, `EspectaculoRepository`).
- **`dto`**: `record`s inmutables de entrada/salida con validaciones Jakarta.
- **`model`**: entidades JPA con relaciones `LAZY`.
- **`exception`**: excepciones de dominio y manejo centralizado con `GlobalExceptionHandler` (`@RestControllerAdvice`) usando `ProblemDetail` (RFC 9457).
- **`config`**: configuración de versionamiento (`ApiVersionConfig`), caché Redis (`CacheConfig`), CORS (`CorsConfig`) y OpenAPI (`OpenApiConfig`).

### Diagrama de componentes

El diagrama de componentes UML del proyecto (módulos, puertos, interfaces proporcionadas/requeridas y dependencias) se encuentra en [`docs/diagrama-componentes-uml.drawio.png`](docs/diagrama-componentes-uml.drawio.png):

![Diagrama de componentes UML](docs/diagrama-componentes-uml.drawio.png)

---

## Endpoints principales

Todas las peticiones bajo `/api/**` requieren el header **`X-API-Version`** (`1` o `2`). Una petición sin este header o con una versión no soportada responde `400 Bad Request`.

| Método | Ruta | Versión | Descripción |
|---|---|---|---|
| `GET` | `/api/reservas?codigo={codigo}` | `1` | Consulta una reserva por código con enlaces HATEOAS |
| `GET` | `/api/reservas?codigo={codigo}` | `2` | Igual que v1, incluyendo `totalSillasEspectaculo` y `sillasDisponiblesEspectaculo` |
| `GET` | `/api/reservas?espectaculoId={id}&page=0&size=10` | `1+` | Lista paginada de reservas de un espectáculo (`size` máx. `50`, orden descendente por `id`) |
| `POST` | `/api/reservas` | `1+` | Crea una reserva (`1` a `10` sillas por solicitud) y retorna `201 Created` con header `Location` |
| `GET` | `/api/clientes/{id}` | `1+` | Consulta un cliente por su identificador |
| `GET` | `/api/espectaculos/{id}` | `1+` | Consulta un espectáculo por su identificador |

### Documentación y recursos adicionales

- **Swagger UI**: `/swagger-ui.html`
- **Especificación OpenAPI (JSON)**: `/v3/api-docs`
- **Interfaz web estática**: disponible en `/` (`src/main/resources/static/index.html`)

### Códigos de respuesta

| Código | Significado |
|---|---|
| `200` | Consulta exitosa |
| `201` | Reserva creada (incluye header `Location`) |
| `400` | Datos inválidos, parámetros incompatibles/faltantes o versión de API inválida/faltante |
| `404` | Reserva, cliente o espectáculo inexistente |
| `409` | No hay sillas suficientes disponibles |
| `500` | Error inesperado (respuesta genérica `ProblemDetail`) |

---

## Caché con Redis

La configuración en [`CacheConfig`](src/main/java/com/udea/parcial_equipo_4/config/CacheConfig.java) utiliza serializadores JSON tipados por DTO, deshabilita valores nulos y define TTL específicos por caché:

| Caché | Contenido | TTL |
|---|---|---|
| `reservas` | Consulta de reserva por código (v1) | 10 minutos |
| `espectaculos` | Consulta de espectáculo por id | 10 minutos |
| `reservas-v2` | Consulta de reserva con disponibilidad (v2) | 2 minutos |
| `reservas-por-espectaculo` | Snapshot paginado de reservas por espectáculo | 2 minutos |

Al crear una reserva se invalidan `reservas-v2` y `reservas-por-espectaculo` después del commit (`transactionAware()`). Si Redis falla, el `CacheErrorHandler` registra un `warn` y permite que la aplicación continúe consultando la base de datos.

---

## Configuración y ejecución local

### 1. Variables de entorno

Copia [`.env.example`](.env.example) como `.env` y completa las credenciales (el archivo `.env` está ignorado por git):

```properties
DB_URL=jdbc:postgresql://<host>:5432/<db>?sslmode=require
DB_USERNAME=<usuario>
DB_PASSWORD=<clave>
DB_POOL_MAX_SIZE=5
FLYWAY_BASELINE_ON_MIGRATE=false
REDIS_HOST=localhost
REDIS_PORT=6379
FRONTEND_URL=http://localhost:5500
```

> Si necesitas datos iniciales para probar en local, puedes ejecutar [`docs/datos-prueba.sql`](docs/datos-prueba.sql).

### 2. Ejecutar la aplicación

En Linux/macOS:

```bash
./mvnw spring-boot:run
```

En Windows (PowerShell):

```powershell
.\mvnw.cmd spring-boot:run
```

---

## Pruebas y CI

Las pruebas están configuradas en [`src/test/resources/application.properties`](src/test/resources/application.properties) de forma autocontenida usando **H2 en memoria en modo PostgreSQL**, **Flyway** (`ddl-auto=validate`) y **caché `simple` en memoria**, por lo que no requieren PostgreSQL, Redis ni Docker para ejecutarse:

```bash
./mvnw verify
```

El pipeline de GitHub Actions ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) se ejecuta en `push` y `pull_request` hacia `main` y `develop`, corriendo `./mvnw -B verify`, generando reportes de cobertura con **JaCoCo** y ejecutando el análisis en **SonarCloud**.
