# Spring Boot — Basic Interview Questions

Paired with the runnable example at `src/com/shihab/springboot` (an Employee
Management REST API). Each question below points to where that concept
shows up in the code.

## 1. What is Spring Boot and how is it different from the Spring Framework?
Spring Boot is an opinionated layer on top of the Spring Framework that removes boilerplate configuration: auto-configuration, embedded servers (Tomcat/Jetty), starter dependencies, and sensible defaults so you can run a production-ready app with minimal setup. Plain Spring requires you to wire most of this manually (XML or Java config).

**Concretely, what does "removes boilerplate" mean?** With plain Spring (pre-Boot), running this project's Employee API would require: manually declaring a `DispatcherServlet` in `web.xml`, wiring an embedded Tomcat yourself (or deploying a WAR to an external server), configuring a `DataSource` bean, an `EntityManagerFactory` bean, a `TransactionManager` bean, and a Jackson `ObjectMapper` for JSON — all by hand, typically in XML.

With Spring Boot, `build.gradle` declares one starter dependency (`spring-boot-starter-web`), and `@SpringBootApplication` on `SpringBootInterviewApplication.java` does the rest: it detects Tomcat and spring-webmvc on the classpath and auto-configures the whole web stack, detects the H2 driver + Spring Data JPA and auto-configures the `DataSource`/`EntityManagerFactory`/`TransactionManager`, and `mvn`/`gradle bootRun` gives you a runnable, embedded-server app with `public static void main` — no external app server, no XML. That's the "opinionated defaults" trade: less control over each bean's wiring, in exchange for almost none of it being your problem until you need to override something.

## 2. What is a bean?
A bean is an object that the Spring container (the `ApplicationContext`) creates, configures, and manages the lifecycle of, instead of your code doing `new SomeClass()` yourself. Once a class is registered as a bean, Spring can inject it wherever it's needed — that's the mechanism dependency injection (Q4) actually runs on.

Every one of `EmployeeServiceImpl`, `EmployeeController`, and `DataLoader` in this project is a bean. None of them is ever constructed with `new` anywhere in the codebase — Spring creates each one once, at startup, because the class is annotated `@Service`/`@RestController`/`@Component` and sits in a component-scanned package (Q3). Once created, Spring hands the right instance to whatever constructor asks for it:
```java
@RestController
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {  // Spring injects the EmployeeServiceImpl bean here
        this.employeeService = employeeService;
    }
}
```
Spring sees `EmployeeController` needs an `EmployeeService`, finds the `EmployeeServiceImpl` bean it already created, and passes it in — no code anywhere says "use `EmployeeServiceImpl`" explicitly; the wiring is entirely up to the container.

A few details worth knowing:
- **Default scope is singleton** — Spring creates exactly one instance of a given bean per application context and reuses it everywhere, rather than a new instance per injection or per HTTP request.
- **Two ways to register a bean**: component scanning (`@Component`/`@Service`/`@Repository`/`@Controller` on a class you own, like everything in this project), or an explicit `@Bean`-annotated method inside a `@Configuration` class (typically for third-party classes you don't own the source of, e.g. a `RestTemplate` or a custom `ObjectMapper`).
- **The container is the `ApplicationContext`** — you can ask it directly for a bean (`applicationContext.getBean(EmployeeService.class)`), though in practice you almost never do this manually; you declare what you need as a constructor parameter and let Spring inject it instead.

## 3. What does `@SpringBootApplication` do?
It's a combination of three annotations: `@Configuration` (this class can define beans), `@ComponentScan` (scan this package and sub-packages for components), and `@EnableAutoConfiguration` (configure beans automatically based on what's on the classpath). See `SpringBootInterviewApplication.java`.

Since it sits at `com.shihab.springboot`, `@ComponentScan` picks up `EmployeeController`, `EmployeeServiceImpl`, and `DataLoader` from the sub-packages automatically — which is also why this class isn't placed directly under `com.shihab` (that would scan every unrelated package in the repo too). `@EnableAutoConfiguration` is what sees `spring-boot-starter-web`/`spring-boot-starter-data-jpa` on the classpath and wires up the embedded Tomcat, `DataSource`, and `EntityManagerFactory` for you.

## 4. What is dependency injection, and what are the ways to do it in Spring?
Instead of a class creating its own dependencies, the framework supplies them. Three ways: constructor injection (recommended — see `EmployeeServiceImpl`, `EmployeeController`), setter injection, and field injection (`@Autowired` on a field — discouraged because it hides dependencies and makes unit testing without Spring harder).

`EmployeeServiceImpl` uses constructor injection without even needing an explicit `@Autowired`:
```java
@Service
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeRepository employeeRepository;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }
    // ...
}
```
(Since Spring 4.3, `@Autowired` is optional on a constructor if the class has exactly one.)

**Why this matters in practice — testability.** Because the dependency is a `final` field set through the constructor, you can unit test `EmployeeServiceImpl` with a plain Mockito mock and zero Spring context:
```java
@Test
void getEmployeeById_throwsWhenMissing() {
    EmployeeRepository mockRepo = mock(EmployeeRepository.class);
    when(mockRepo.findById(99L)).thenReturn(Optional.empty());

    EmployeeServiceImpl service = new EmployeeServiceImpl(mockRepo);

    assertThrows(ResourceNotFoundException.class, () -> service.getEmployeeById(99L));
}
```
That test runs in milliseconds with no `@SpringBootTest`, no embedded database, nothing. If `employeeRepository` were instead a private field with `@Autowired` (field injection), you couldn't construct `EmployeeServiceImpl` with a mock at all without reflection tricks — you'd be forced into a slower, heavier Spring-context-based test just to check one branch of logic. That's the concrete cost field injection has, beyond just "it's discouraged."

## 5. What's the difference between `@Component`, `@Service`, `@Repository`, and `@Controller`?
All four register a class as a Spring bean via component scanning — `@Service`, `@Repository`, and `@Controller` are just `@Component` under the hood, with extra meaning layered on:

- `@Component` — generic bean, no special role (`DataLoader`)
- `@Service` — business logic (`EmployeeServiceImpl`)
- `@Controller`/`@RestController` — web layer (`EmployeeController`)
- `@Repository` — data access; also auto-translates low-level DB exceptions (e.g. `SQLException`) into Spring's `DataAccessException` — so callers don't need to know if they're on H2, MySQL, or Postgres

`EmployeeRepository` gets that `@Repository` behavior for free, just by extending `JpaRepository`.

## 6. What is Spring Data JPA, and how do "derived query methods" work?
An abstraction over JPA that generates repository implementations at runtime — you write an interface, Spring provides the SQL/JPQL behind it.

```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByDepartment(Department department);
    Optional<Employee> findByEmail(String email);
}
```
No implementation exists anywhere — Spring parses the method name (`findBy` + `Department`) and generates `SELECT e FROM Employee e WHERE e.department = :department` automatically.

More examples the parser understands: `findByLastNameOrderByJoinDateDesc(...)` (sorting), `countByDepartment(...)` (aggregate), `existsByEmail(...)` (boolean).

**Limit:** once a query needs a join, subquery, or DB-specific function, drop to `@Query("...")` (JPQL) or `@Query(nativeQuery = true)` (raw SQL) instead.

## 7. What is the difference between `@Entity` and a DTO? Why not return entities directly from a controller?
`@Entity` (`Employee.java`) maps to a DB table and stays inside the persistence layer. A DTO (`EmployeeDTO.java`) is the shape exposed to clients. `EmployeeServiceImpl` converts between them explicitly on the way in and out:

```java
// Entity -> DTO (out)          // DTO -> Entity (in, see createEmployee)
new EmployeeDTO(employee.getId(), ...);   new Employee(dto.getFirstName(), ...);
```

Why bother with two classes for the same data? A few concrete reasons: `EmployeeDTO` carries validation annotations (`@NotBlank`, `@Email`) that shouldn't apply to every path creating an `Employee` (e.g. `DataLoader` seeding data); returning entities risks Jackson serializing a lazy-loading proxy if the entity later gains a relationship (`LazyInitializationException`); and it decouples your API contract from your DB schema — change the table, don't break every client.

## 8. What does `@Transactional` do?
Wraps a method in a database transaction: commits on success, rolls back on unchecked exceptions.

```java
@Transactional(readOnly = true)                       // hint: no writes, skip dirty-checking overhead
public List<EmployeeDTO> getAllEmployees() { ... }

@Transactional                                        // full read-write transaction
public EmployeeDTO createEmployee(EmployeeDTO dto) { ... }
```

Two gotchas worth knowing:
- **Rollback is unchecked-exceptions-only by default** — a checked exception thrown inside a `@Transactional` method still commits. Use `@Transactional(rollbackFor = SomeCheckedException.class)` to override.
- **It's a proxy, so self-calls don't work** — calling a `@Transactional` method from another method in the *same* class (`this.someMethod()`) skips the proxy, silently losing the transaction. Only calls from outside the bean apply it (e.g., `EmployeeController` → `EmployeeServiceImpl`).

## 9. What is "dirty checking" in JPA/Hibernate?
Within an open transaction, Hibernate tracks changes to **managed** entities and issues an `UPDATE` automatically at commit — no explicit `save()` needed.

```java
@Transactional
public EmployeeDTO updateEmployee(Long id, EmployeeDTO dto) {
    Employee employee = findEmployeeOrThrow(id);   // managed — came from findById in this same transaction
    employee.setFirstName(dto.getFirstName());
    // ...more setters, no employeeRepository.save() call anywhere
    return toDTO(employee);
}
```
Hibernate compares the entity's values at commit time against the snapshot taken when it was loaded, and writes an `UPDATE` for whatever changed.

**Managed vs. detached is the key distinction:** this only works because `employee` is still tracked (loaded in the same transaction). A detached entity (loaded in a closed transaction, or built manually) needs an explicit `employeeRepository.save(employee)` — which is exactly why `createEmployee` (a brand-new entity) calls `save()` but `updateEmployee` doesn't.

## 10. How do you validate incoming request bodies in Spring Boot?
Annotate DTO fields with Bean Validation constraints (`@NotBlank`, `@Email`, `@Positive` — see `EmployeeDTO`), then add `@Valid` to the controller parameter:
```java
@PostMapping
public ResponseEntity<EmployeeDTO> createEmployee(@Valid @RequestBody EmployeeDTO employeeDTO) { ... }
```
An invalid body (blank name, bad email, negative salary) fails all three rules at once, and Spring collects every violation into a `MethodArgumentNotValidException` — handled centrally by `GlobalExceptionHandler` (Q11), returning one response listing every failing field instead of a raw 500.

Requires `spring-boot-starter-validation` on the classpath (already in `build.gradle`) — without it, `@Valid` is silently ignored.

## 11. How do you handle exceptions/errors globally in Spring Boot?
With `@RestControllerAdvice` + `@ExceptionHandler` methods (see `GlobalExceptionHandler.java`) — intercepts exceptions from any controller and converts them into a consistent JSON error, instead of a raw stack trace.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)       public ... handleNotFound(...) { ... }   // -> 404
    @ExceptionHandler(MethodArgumentNotValidException.class) public ... handleValidation(...) { ... } // -> 400
    @ExceptionHandler(Exception.class)                       public ... handleGeneric(...) { ... }    // -> 500
}
```
Spring picks the *most specific* matching handler for a thrown exception's type. Doing this globally (instead of try/catch in every controller method) means one place to maintain, and every error comes back in the same shape (`ApiError`: timestamp, status, message, details) — regardless of which handler produced it.

## 12. What is `CommandLineRunner` used for?
A functional interface whose `run()` method fires once, right after the Spring context starts — used here in `DataLoader` to seed 3 demo employees, so `GET /api/employees` already returns data with no manual setup.

```java
@Component
public class DataLoader implements CommandLineRunner {
    public void run(String... args) {
        employeeRepository.save(new Employee("Ada", "Lovelace", ...));
    }
}
```

`ApplicationRunner` is the alternative — same idea, but gives you parsed `ApplicationArguments` instead of a raw `String[]`, handy if startup logic needs to branch on flags.

Gotcha: with multiple runner beans, execution order isn't guaranteed unless you add `@Order(1)`, `@Order(2)`, etc.

## 13. What embedded server does Spring Boot use by default, and can you change it?
Tomcat, via `spring-boot-starter-web`. Swap it for Jetty or Undertow by excluding the Tomcat starter and adding the alternative:
```groovy
implementation('org.springframework.boot:spring-boot-starter-web') {
    exclude group: 'org.springframework.boot', module: 'spring-boot-starter-tomcat'
}
implementation 'org.springframework.boot:spring-boot-starter-jetty'
```
No application code changes — the server is purely an auto-configured infrastructure concern (Q1). Teams usually only switch for a specific reason (Undertow's smaller memory footprint, etc.), not by default.

## 14. What is `application.properties` (or `.yml`) for?
Externalized configuration — datasource URLs, ports, logging, feature flags — without touching code:
```properties
spring.datasource.url=jdbc:h2:mem:employeedb;DB_CLOSE_DELAY=-1
spring.jpa.hibernate.ddl-auto=create-drop
server.port=8080
```
None of this appears in Java code — auto-configuration reads it at startup and wires the `DataSource`/`EntityManagerFactory`/Tomcat accordingly.

**Profiles:** real projects usually add `application-prod.properties` etc., overriding a subset of values (e.g. a real Postgres URL instead of H2). `spring.profiles.active=prod` picks which one loads, so the same jar runs anywhere.

## 15. What's the difference between `@PathVariable` and `@RequestParam`?
`@PathVariable` reads a value from the URL path; `@RequestParam` reads it from the query string.

```java
@GetMapping("/{id}")
public EmployeeDTO getEmployeeById(@PathVariable Long id) { ... }     // GET /api/employees/7

@GetMapping
public List<EmployeeDTO> getEmployees(@RequestParam(required = false) Department department) { ... }
                                                                        // GET /api/employees?department=ENGINEERING
```
Rule of thumb: `@PathVariable` when the value identifies *which resource* (the URL is meaningless without it, like an id). `@RequestParam` for optional filters/sorting — the request is still valid without it.

## 16. How would you test this layered architecture?
Each layer gets a progressively wider (and slower) test slice:

- **Unit test** `EmployeeServiceImpl` with a mocked `EmployeeRepository` (Mockito) — no Spring context, milliseconds. See Q4's example.
- **`@WebMvcTest(EmployeeController.class)`** — loads only the web layer, service mocked via `@MockBean`. Good for verifying routing, JSON, validation, and `GlobalExceptionHandler` wiring (e.g. asserting a missing employee returns 404).
- **`@DataJpaTest`** — loads only the JPA layer against a real in-memory H2 DB. Verifies a derived query like `findByDepartment` actually returns the right rows, not just that it compiles.
- **`@SpringBootTest`** — the full context, real HTTP round-trip through controller → service → repository → H2.

A healthy suite leans on the first two for speed, uses `@DataJpaTest` for query-specific logic, and reserves `@SpringBootTest` for a handful of critical end-to-end paths.

## How to run the example
```bash
./gradlew bootRun
```
Then hit `http://localhost:8080/api/employees`. The H2 console is available at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:employeedb`, user `sa`, empty password).
