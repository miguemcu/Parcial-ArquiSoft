# AGENTS.md

Guía para agentes de código (GitHub Copilot, etc.) y para el equipo. Léela completa antes de generar o modificar código.

## 1. Contexto del proyecto

API RESTful para **reservas de sillas de conciertos**. Proyecto grupal (4 personas) del curso de Arquitectura de Software, Universidad de Antioquia.

Requisitos que el proyecto debe cumplir (el código debe converger hacia ellos):

- GET para consultar una reserva por **código de reserva** y POST para crear una reserva.
- Respuestas en JSON, con **códigos de estado HTTP** correctos.
- **Versionamiento** de la API con un *custom request header*.
- **HATEOAS** en las respuestas.
- **Paginación** en los listados.
- Caché local con **Redis**.
- Documentación **OpenAPI/Swagger**.
- **Docker** (docker-compose) y **observabilidad** (Prometheus + Grafana).
- Evidencias de pruebas, diagrama de arquitectura (C4) y repositorio en GitHub.

## 2. Stack

| Área | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.2.0 (Maven) |
| Persistencia | Spring Data JPA + PostgreSQL |
| Migraciones | Flyway |
| Validación | Jakarta Validation |
| Caché | Spring Cache + Redis |
| API | Spring Web, Spring HATEOAS, springdoc-openapi |
| Observabilidad | Actuator + Micrometer + Prometheus + Grafana |
| Pruebas | JUnit 5, Mockito, H2 (modo PostgreSQL), Testcontainers (opcional) |
| CI | GitHub Actions, JaCoCo, SonarCloud |
| Utilidades | Lombok |
| Configuración | `application.properties` (no usar YAML) |

No agregues dependencias nuevas sin que el equipo lo acuerde. Si una API de Spring Boot 4 / Hibernate 7 / Jackson 3 difiere de lo que conoces de versiones anteriores, verifica contra la documentación oficial antes de asumir.

## 3. Modelo de dominio

- **espectaculo**: `id`, `nombre`, `fecha`, `cantidad_sillas` (capacidad total).
- **cliente**: `id`, `nombre`, `documento` (único), `telefono`.
- **reserva**: `id`, `codigo_reserva` (único, máx. 20 caracteres), `cliente_id`, `espectaculo_id`, `cantidad_sillas` (> 0).

Reglas de negocio:

- No hay sillas individuales: la disponibilidad de un espectáculo es `cantidad_sillas − SUM(reserva.cantidad_sillas)`.
- Una reserva nunca puede superar la disponibilidad. Para evitar sobreventa bajo concurrencia, la creación bloquea la fila del espectáculo con `PESSIMISTIC_WRITE` dentro de una transacción.
- El código de reserva tiene el formato `RES-` + 8 caracteres alfanuméricos en mayúscula, y se verifica su unicidad antes de guardar.

## 4. Arquitectura por capas

Paquetes bajo el paquete base del proyecto:

```
controller  →  service  →  repository  →  (PostgreSQL / Redis)
     ↓            ↓
    dto        model (entidades JPA)
exception (excepciones propias + @RestControllerAdvice)
config (beans de configuración: caché, OpenAPI, etc.)
```

Reglas estrictas:

1. **controller**: delgado. Solo recibe la petición, valida (`@Valid`), delega al servicio y arma la respuesta HTTP. Sin lógica de negocio ni acceso a repositorios.
2. **service**: toda la lógica de negocio y las transacciones (`@Transactional`). Es el único que habla con los repositorios.
3. **repository**: solo interfaces Spring Data y consultas. Sin lógica de negocio.
4. **model**: entidades JPA. Sin `@Data` (usa `@Getter`/`@Setter`). Relaciones `LAZY`.
5. **dto**: `record`s de Java. **Nunca expongas entidades JPA en la API.** Entidad ↔ DTO se convierte en el servicio o en una clase `Mapper`.
6. Las dependencias van hacia abajo. Un repositorio nunca llama a un servicio y un servicio nunca depende de un controlador.
7. Inyección de dependencias por **constructor** (`@RequiredArgsConstructor`), no con `@Autowired` en campos.

## 5. Convenciones de la API

- Base: `/api/...`. Recursos en plural y en minúscula (`/api/reservas`).
- **Versionamiento**: header `X-API-Version` (por ejemplo `X-API-Version: 1`). El enrutamiento por versión se hace en el mapeo de los controladores. Una petición sin versión o con una versión no soportada recibe `400`.
- **Errores**: siempre `ProblemDetail` (RFC 9457) desde un `@RestControllerAdvice`. Nunca devuelvas stack traces.
- Códigos de estado:

| Código | Uso |
|---|---|
| 200 | Consulta exitosa |
| 201 | Recurso creado (con header `Location`) |
| 400 | Validación fallida, parámetro faltante, versión inválida |
| 404 | Recurso inexistente |
| 409 | Conflicto de negocio (sillas insuficientes) |
| 500 | Error inesperado (mensaje genérico) |

- **HATEOAS**: las respuestas usan `EntityModel` / `PagedModel` con enlaces (`self`, `espectaculo`, `cliente`, etc.).
- **Paginación**: los listados reciben `page` y `size` (con valores por defecto y un máximo para `size`) y devuelven `PagedModel`.
- **OpenAPI**: cada endpoint lleva `@Operation` y `@ApiResponse` para sus respuestas, y el header de versión debe verse documentado.
- Nombres: dominio en **español** (`Reserva`, `cantidadSillas`), términos técnicos en **inglés** (`Service`, `Repository`, `Mapper`).

## 6. Base de datos y Flyway

**El esquema lo gestiona Flyway. Hibernate nunca lo modifica.**

- `spring.jpa.hibernate.ddl-auto=validate`. Prohibido `create`, `create-drop` y `update`.
- Las migraciones viven en `src/main/resources/db/migration`, con el nombre `V{n}__{descripcion_en_snake_case}.sql` (por ejemplo `V1__esquema_inicial.sql`, `V2__indice_reserva_espectaculo.sql`).
- **Nunca edites una migración ya aplicada o ya mergeada a `main`.** Para cambiar algo, crea una migración nueva con el siguiente número.
- Una migración = un cambio coherente. Debe poder ejecutarse sobre una base vacía.
- Antes de crear una migración, revisa el último número en `main` y coordina con el equipo para evitar versiones duplicadas.
- `V1` es el esquema inicial: tablas `espectaculo`, `cliente` y `reserva` con sus claves, `UNIQUE` y `CHECK`.
- Si la base ya fue creada a mano, usa `spring.flyway.baseline-on-migrate=true` solo en desarrollo local. En Docker Compose la base parte vacía y Flyway crea todo.
- Los datos de prueba van en `docs/datos-prueba.sql` o en una migración de perfil `dev`, **nunca** mezclados en las migraciones de esquema.
- Con Flyway y PostgreSQL se necesitan `flyway-core` y `flyway-database-postgresql`.
- Cada cambio de esquema debe ir acompañado del ajuste de la entidad JPA correspondiente en el mismo PR.

## 7. Caché (Redis)

- Patrón *cache-aside* con `@Cacheable` en el servicio (no en el controlador ni en el repositorio).
- `reservas`: consulta por código v1, TTL 10 minutos. `espectaculos`: consulta por ID, TTL 10 minutos.
- `reservas-v2`: consulta por código que incluye disponibilidad, TTL 2 minutos.
- `reservas-por-espectaculo`: páginas de reservas, clave por espectáculo, página y tamaño efectivo (máximo 50), TTL 2 minutos. Se cachea un snapshot DTO y se reconstruye el `Page`.
- La creación invalida todas las entradas de `reservas-v2` y `reservas-por-espectaculo` después del commit. No invalida `reservas` ni `espectaculos`.
- Las cachés Redis usan serializadores JSON tipados por DTO, deshabilitan valores nulos y registran estadísticas para Micrometer.
- Cachea DTOs serializables, no entidades JPA.
- El `CacheErrorHandler` registra un `warn` sin claves ni datos personales y permite que la aplicación siga funcionando (más lento) si Redis falla.
- Las pruebas usan caché simple en memoria (`spring.cache.type=simple`), sin Redis ni Docker.

## 8. Observabilidad

- Actuator expone `health`, `info`, `metrics` y `prometheus`.
- Métricas propias con Micrometer: por ejemplo `reservas_creadas_total` y aciertos/fallos de caché.
- Logs estructurados, sin datos sensibles (no registres documentos ni teléfonos de clientes).
- Los dashboards de Grafana se versionan en el repo, en `observability/`.

## 9. Docker

- `Dockerfile` multi-stage (build con Maven, runtime con JRE 21 ligero), ejecutando como usuario no root.
- `docker-compose.yml` con: app, postgres, redis, prometheus y grafana.
- Configuración por variables de entorno. Ningún secreto en el repositorio: usa `.env` (ignorado por git) y un `.env.example` documentado.
- `docker compose up` debe levantar todo el sistema desde cero.

## 10. Pruebas

- Las pruebas no necesitan Postgres, Redis, Docker ni el `.env`. Usan **H2 en memoria en modo PostgreSQL** y Flyway aplica las migraciones reales de `db/migration` (con `ddl-auto=validate`), así cada migración se prueba en cada build.
- La configuración de pruebas vive en `src/test/resources/application.properties`. Es autocontenida: reemplaza al `application.properties` principal durante los tests. Si agregas una propiedad nueva que use `${...}` en el principal, agrega su valor de prueba allí.
- Si una migración usa sintaxis exclusiva de PostgreSQL que H2 no soporta, avisa al equipo antes de cambiar la estrategia de pruebas.
- Servicios: pruebas unitarias con JUnit 5 + Mockito.
- Repositorios e integración: `@SpringBootTest` / `@DataJpaTest` sobre H2.
- Testcontainers (las dependencias siguen en el `pom.xml`) queda como opción para pruebas específicas que necesiten PostgreSQL o Redis reales (por ejemplo, comportamiento de locks propio de PostgreSQL). Esas pruebas requieren Docker.
- Controladores: `@WebMvcTest` / MockMvc para códigos de estado, validación y formato de errores.
- Casos obligatorios del POST: éxito (`201`), validación (`400`), cliente o espectáculo inexistente (`404`) y sillas insuficientes (`409`).
- Debe haber una prueba de concurrencia que verifique que no se sobrevenden sillas.

## 11. CI

- Workflow en `.github/workflows/ci.yml` (GitHub Actions). Corre en cada push a `main` y `develop` y en cada PR hacia esas ramas.
- Pasos: compila y ejecuta las pruebas con `./mvnw -B verify`, genera la cobertura con JaCoCo (`target/site/jacoco/`), analiza en SonarCloud esperando el quality gate y sube como artefactos el reporte de JaCoCo y los de Surefire.
- Antes de abrir un PR, `./mvnw verify` debe pasar en local.
- `SONAR_TOKEN` vive **solo** como secreto del repositorio en GitHub. Nunca en el `pom.xml`, el `.env` ni otro archivo. Sin el secreto (por ejemplo, PRs desde forks) el paso de Sonar se omite.
- SonarCloud (plan gratuito) solo analiza `main` y los pull requests. En los push a `develop` el paso de Sonar se omite; el código de `develop` ya se analizó en su PR.
- El umbral mínimo de cobertura está desactivado (`jacoco.check.skip=true` en el `pom.xml`). Se activa con `./mvnw verify -Djacoco.check.skip=false` o cambiando la propiedad.
- Aún no hay CD (publicación de imagen Docker): se agregará cuando exista el `Dockerfile`.

## 12. Git: commits y ramas

### Conventional Commits

Formato: `<tipo>(<alcance>): <descripción>`

- Descripción en **español**, en imperativo, minúscula inicial, **sin punto final**, máximo 72 caracteres.
- Un commit = un cambio lógico. No mezcles funcionalidad, refactor y formato en el mismo commit.

| Tipo | Cuándo |
|---|---|
| `feat` | Nueva funcionalidad |
| `fix` | Corrección de un error |
| `docs` | Documentación (README, diagramas, OpenAPI) |
| `refactor` | Cambio de código sin alterar comportamiento |
| `test` | Agregar o corregir pruebas |
| `build` | Maven, Dockerfile, dependencias |
| `ci` | Pipelines |
| `chore` | Tareas varias que no tocan código de producción |
| `perf` | Mejora de rendimiento |
| `style` | Formato (espacios, imports) sin cambio de lógica |

Alcances sugeridos: `reserva`, `cliente`, `espectaculo`, `api`, `db`, `cache`, `docker`, `observabilidad`, `docs`.

Ejemplos:

```
feat(reserva): crear reserva con validación de sillas disponibles
fix(reserva): bloquear espectáculo para evitar sobreventa
feat(db): agregar migración V2 con índice en reserva(espectaculo_id)
build(docker): agregar docker-compose con postgres y redis
docs(api): documentar header X-API-Version en OpenAPI
```

Cambio incompatible: agrega `!` después del alcance (`feat(api)!: ...`) y un pie `BREAKING CHANGE: ...` explicando el impacto.

### Ramas y PRs

- `main` siempre compila y funciona; no se hace push directo.
- Ramas: `feat/<descripcion-corta>`, `fix/<...>`, `docs/<...>`, `chore/<...>`.
- Todo cambio entra por Pull Request con al menos una revisión de otra persona del equipo.
- Los PRs son pequeños y con un propósito claro. El título sigue Conventional Commits.

## 13. Definición de terminado

Un cambio está terminado cuando:

1. Compila (`./mvnw clean verify`), las pruebas pasan y el CI está en verde.
2. Respeta las capas de la sección 4.
3. Si toca la base de datos, incluye su migración Flyway.
4. Los endpoints nuevos o modificados están documentados en OpenAPI.
5. Los errores usan `ProblemDetail` con el código HTTP correcto.
6. No introduce secretos ni datos sensibles.
7. Los commits siguen Conventional Commits.

## 14. Lo que NO debes hacer

- No expongas entidades JPA en los controladores.
- No pongas lógica de negocio en controladores ni repositorios.
- No uses `ddl-auto` distinto de `validate`.
- No edites migraciones ya mergeadas.
- No uses `@Autowired` en campos ni `@Data` en entidades.
- No agregues dependencias, frameworks ni cambies la versión de Spring Boot sin acuerdo del equipo.
- No hagas refactors masivos ni cambios fuera del alcance de la tarea pedida.
- No inventes endpoints, campos o tablas que no estén definidos aquí o en el esquema; si algo falta, pregunta o propónlo explícitamente.