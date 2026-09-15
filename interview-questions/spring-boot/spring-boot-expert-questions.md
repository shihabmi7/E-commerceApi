# Spring Boot — Expert-Level Interview Questions

Builds on `spring-boot-basic-questions.md`. Past single-service CRUD into microservices, resilience, and production-scale concerns.

## 1. How do you configure multiple datasources (Postgres + Oracle + MongoDB)?
Boot auto-configures only **one** primary `DataSource`. Each extra relational source needs its own `@Configuration` class (own `DataSource`/`EntityManagerFactory`/`TransactionManager`, scoped via `@EnableJpaRepositories`), with exactly one marked `@Primary`.
```java
@Configuration
@EnableJpaRepositories(basePackages = "com.company.orders.postgres",
        entityManagerFactoryRef = "postgresEmf", transactionManagerRef = "postgresTx")
public class PostgresConfig {
    @Primary @Bean @ConfigurationProperties("spring.datasource.postgres")
    public DataSource postgresDataSource() { return DataSourceBuilder.create().build(); }
    // + EntityManagerFactory, TransactionManager beans, same pattern repeated for Oracle
}
```
MongoDB is separate again — `spring-boot-starter-data-mongodb` auto-configures its own `MongoTemplate`/`MongoRepository` stack independently, since it isn't JPA/relational at all.

Why three stores: Postgres for transactional core data, Oracle because a legacy system already owns it, MongoDB for flexible/high-volume documents (logs, audit trails) that don't fit a fixed schema.

## 2. How does Actuator tie into Kubernetes health checks?
Since Boot 2.3+: **liveness** (`/actuator/health/liveness` — is the process fundamentally broken?) vs **readiness** (`/actuator/health/readiness` — can it serve traffic right now, e.g. DB up?).
```yaml
livenessProbe:  { httpGet: { path: /actuator/health/liveness,  port: 8080 } }
readinessProbe: { httpGet: { path: /actuator/health/readiness, port: 8080 } }
```
Gotcha: tying liveness to a flaky downstream dependency causes needless pod restarts — liveness should only reflect "this process is broken," not "a dependency is slow."

## 3. What's the circuit breaker pattern (Resilience4j)?
Stops calling a failing downstream service for a while instead of piling up timeouts.
```java
@CircuitBreaker(name = "employeeService", fallbackMethod = "fallback")
public EmployeeDTO getEmployeeById(Long id) { ... }
public EmployeeDTO fallback(Long id, Throwable t) { return EmployeeDTO.unknown(id); }
```
States: **closed** (normal) → **open** (failures exceed threshold, fail fast, no network call) → **half-open** (test a few calls) → closed or open again. Often paired with `@Retry` and `@Bulkhead` (limits concurrent calls).

## 4. How does distributed tracing work across microservices?
A **trace ID** (whole request) + **span ID** (per hop) propagate via headers across services, so logs/spans correlate into one timeline. Micrometer Tracing auto-instruments HTTP/DB calls, exports to Zipkin/Jaeger — no manual span code needed for the common case. It's the tool for "why is this one request slow" when logs alone can't reconstruct the call graph.

## 5. Spring MVC vs. WebFlux?
MVC: thread-per-request, blocking — a thread is held until the response completes. WebFlux: non-blocking (`Mono`/`Flux`, Project Reactor), a small fixed thread pool handles many concurrent requests.
```java
// MVC: EmployeeDTO getById(...)      WebFlux: Mono<EmployeeDTO> getById(...)
```
WebFlux pays off under high concurrency + slow I/O (gateways, streaming). For typical CRUD, MVC is simpler and the right default — and WebFlux only helps if the *whole* stack is non-blocking (standard JDBC/JPA defeats the purpose; needs R2DBC).

## 6. How do you build a custom Spring Boot starter?
A dependency bundling a library + an `@AutoConfiguration` class, activated conditionally:
```java
@AutoConfiguration
@ConditionalOnClass(SomeAuditClient.class)      // only if the library's on the classpath
public class AuditAutoConfiguration {
    @Bean @ConditionalOnMissingBean              // let consumers override
    public AuditService auditService(AuditProperties props) { ... }
}
```
Registered via `META-INF/spring/...AutoConfiguration.imports`. `@ConditionalOn...` is the real mechanism behind "how does auto-configuration work" — it's exactly how Boot decides whether to wire a `DataSource` at all.

## 7. How do you handle distributed transactions across services/datastores?
`@Transactional` only covers one datasource — it doesn't reach across process boundaries. Standard pattern: **Saga** — a sequence of local transactions, each firing an event that triggers the next step, with **compensating transactions** to undo prior steps on failure (e.g. "cancel order" undoes "reserve inventory").

Two flavors: **choreography** (services react to each other's events, no coordinator — simple, harder to trace) vs **orchestration** (a central saga coordinator calls each step explicitly — visible, more complex). Bottom line for Postgres+MongoDB consistency: you generally design for *eventual* consistency, not strong.

## 8. `@Async` vs. messaging (Kafka/RabbitMQ) for background work?
`@Async` runs on a thread pool in the *same JVM* — fire-and-forget, lost on crash, doesn't scale past one instance.
```java
@Async
public void sendWelcomeEmail(Employee e) { ... }
```
A broker persists the work item outside the JVM — survives a crash, can be consumed by another instance/service, load-balances across consumers. Use `@Async` for cheap in-process work; use messaging when it must survive a crash or cross service boundaries.

## 9. How do you tune HikariCP for production?
```properties
spring.datasource.hikari.maximum-pool-size=10   # bounded by DB max_connections & CPU cores, not "as high as possible"
spring.datasource.hikari.connection-timeout=3000  # fail fast, don't queue forever
spring.datasource.hikari.max-lifetime=1800000     # recycle before DB/LB kills it first
```
Trap: bigger pool ≠ more throughput past a point — more connections just means more DB-side contention. Also remember it's *per instance*: 10 pods × pool size 10 = 100 DB connections.

## 10. How do you test against a real database instead of H2?
**Testcontainers** — spins up real Postgres/MongoDB/Kafka in Docker for the test's lifetime.
```java
@Testcontainers @SpringBootTest
class EmployeeRepositoryIT {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) { r.add("spring.datasource.url", postgres::getJdbcUrl); }
}
```
H2 is fine for fast, generic query-shape tests; Testcontainers is for confidence against the *actual* engine (Oracle-specific SQL, Postgres JSONB, etc.).

## 11. How does stateless JWT auth work in microservices?
Client sends a signed JWT on every request; a filter validates it and populates the security context — nothing stored server-side.
```java
public class JwtAuthFilter extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest req, ...) {
        if (jwtService.isValid(token)) SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
```
One auth service issues tokens; every downstream service only needs the shared signing key to *validate* locally — no callback per request, which is what makes it scale horizontally. `@PreAuthorize("hasRole('ADMIN')")` layers authorization on top.

## 12. `@Cacheable` vs. manually caching with Redis?
`@Cacheable` is a declarative abstraction over whatever backend is configured (in-memory by default, Redis if configured):
```java
@Cacheable(value = "employees", key = "#id")
public EmployeeDTO getEmployeeById(Long id) { ... }
@CacheEvict(value = "employees", key = "#id")
public void updateEmployee(Long id, EmployeeDTO dto) { ... }  // must evict, or reads go stale
```
Gotcha: forgetting `@CacheEvict` on a new write path = stale data bugs. In-memory cache is also per-instance (pods don't see each other's writes) — the reason to use Redis once you're running more than one instance.

## 13. How do you do graceful shutdown, and why does K8s care?
Without it, `SIGTERM` can kill the JVM mid-request, dropping in-flight calls.
```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```
Stops accepting new requests, lets in-flight ones finish (up to the timeout). Must line up with K8s' `terminationGracePeriodSeconds` — set that a bit higher, or K8s force-kills before Spring finishes draining.

## 14. How do you reduce startup time / memory footprint (Kubernetes autoscaling)?
Slow-starting pods delay absorbing traffic spikes.
- **Lazy initialization** — beans created on first use, not all at startup.
- **Exclude unused auto-configuration** explicitly.
- **GraalVM native image** — ahead-of-time compiled binary; startup drops to milliseconds, smaller memory footprint, at the cost of longer builds and reflection/proxy limitations needing explicit hints.

## 15. How do you rate-limit a Spring Boot API across multiple pods?
A per-instance in-memory counter under-counts across pods (100 req/min *per pod*, not per user overall). Centralize the counter in Redis (sliding window/token bucket shared across instances), or push it up to an API Gateway (Spring Cloud Gateway, Kong) so no individual service reimplements it. Rate limiting is a cross-cutting concern — centralizing it avoids inconsistent per-service implementations.
