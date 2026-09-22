# Spring Boot Interview Questions - Self-Test (Basic + Expert)

85 questions, each followed directly by its answer and a short explanation.

**Based on:** Spring Boot 3.4 / Spring Framework 6.2 / Spring Security 6 / Hibernate 6 / Jakarta EE 10. Answers tagged `repo:` point at real code in this e-commerce project.

**Related notes (same folder / repo):** [spring-boot-basic-questions.md](spring-boot-basic-questions.md), [spring-boot-expert-questions.md](spring-boot-expert-questions.md), [jpa-fetching-questions.md](jpa-fetching-questions.md), [microservices-communication-questions.md](microservices-communication-questions.md), [../rest-api/rest-api-basic-questions.md](../rest-api/rest-api-basic-questions.md), [../database/relational-db/relational-db-basics.md](../database/relational-db/relational-db-basics.md).

## Contents

- Basic: Core Spring Boot (Q1-Q12)
- Basic: Security & Testing Basics (Q13-Q18)
- Basic: MVC & REST (Q19-Q30)
- Basic: Spring Data JPA Basics (Q31-Q40)
- Expert: JPA & Hibernate Deep Dive (Q41-Q54)
- Expert: Concurrency & Data Integrity (Q55-Q60)
- Expert: Microservices with Spring (Q61-Q73)
- Expert: Production Readiness (Q74-Q81)
- Expert: Advanced Security & Testing (Q82-Q85)

---

# Part 1 - Basic

## Core Spring Boot (Q1-Q12)

**1. SecurityConfig takes a JwtFilter in its constructor, yet no code in the project ever calls new JwtFilter(...). What makes this work?**

This is inversion of control: the container, not your code, creates and wires objects. JwtFilter is a @Component in a scanned package, so it becomes a singleton bean that Spring passes to any constructor asking for it (see spring-boot-basic Q2). *(repo: SecurityConfig / JwtFilter)*

**2. UserDetailsServiceImpl declares `private final UserRepository userRepository;` and sets it in its constructor. What is a concrete benefit over putting @Autowired on a private field?**

With constructor injection the object cannot exist without its dependencies, the field can be final, and plain Mockito tests can build it directly. Field injection hides the dependency and forces reflection or a Spring context in tests (see spring-boot-basic Q4). *(repo: UserDetailsServiceImpl)*

**3. ECommerceApiApplication carries a single @SpringBootApplication annotation. Which annotations does it combine?**

@SpringBootApplication is meta-annotated with @SpringBootConfiguration (a specialization of @Configuration), @EnableAutoConfiguration, and @ComponentScan. The scan starts at the annotated class's package, com.shihab.ecommerceapi, and covers its sub-packages. *(repo: ECommerceApiApplication)*

**4. Boot's UserDetailsServiceAutoConfiguration (default in-memory user with a generated password) is guarded by @ConditionalOnMissingBean for types such as AuthenticationProvider and UserDetailsService. This project defines both beans. What happens?**

Auto-configuration classes are processed after user configuration, so @ConditionalOnMissingBean can see your beans and skips its own definition. This back-off is how Boot lets you override its defaults simply by declaring a bean. *(repo: ApplicationConfiguration)*

**5. The project's pom.xml declares spring-boot-starter-web, and the built jar runs with plain `java -jar` on a machine with no Tomcat installed. Why?**

A starter is a dependency descriptor with no code of its own; spring-boot-starter-web transitively brings in spring-boot-starter-tomcat and Spring MVC. Boot's web server auto-configuration then starts embedded Tomcat within the application process (see spring-boot-basic Q13). *(repo: pom.xml)*

**6. application.properties sets `security.jwt.secret-key=${JWT_SECRET_KEY}` with no default. application-test.properties sets the same key to a literal value. You start with the test profile active and JWT_SECRET_KEY unset. What happens?**

Profile-specific files always override the non-specific ones for the same key. Without that override, the unresolvable placeholder makes the app fail fast at startup instead of running with a missing secret (see spring-boot-basic Q14). *(repo: application-test.properties)*

**7. The jar's application.properties has server.port=8080. The container sets the environment variable SERVER_PORT=9090, and the app is launched with `java -jar app.jar --server.port=7070`. Which port is used?**

Boot's externalized configuration lets later sources override earlier ones: packaged application.properties is overridden by OS environment variables, which are overridden by command-line arguments. This is what lets one jar run unchanged in any environment.

**8. JwtService reads security.jwt.secret-key and security.jwt.expiration-time with separate @Value fields. What is a real advantage of @ConfigurationProperties(prefix = "security.jwt") for this?**

@ConfigurationProperties gives type-safe binding of a prefix-grouped set of properties, full relaxed binding (e.g. env-var style names), and Bean Validation via @Validated. @Value is fine for a single value but supports SpEL instead and only limited relaxed binding. *(repo: JwtService)*

**9. JwtService is annotated only with @Service and no @Scope. What is its scope, and what follows from that?**

Singleton is the default bean scope: the container creates one instance and reuses it everywhere. Any mutable field in such a bean is shared across all concurrent requests, so per-request state does not belong there (see spring-boot-basic Q2). *(repo: JwtService)*

**10. JwtService declares `@Value("${security.jwt.secret-key}") private String secretKey;` on a field. Why would reading secretKey inside a constructor of JwtService give null?**

Bean creation is instantiate, then populate properties and injected fields, then initialization callbacks (@PostConstruct, InitializingBean). Constructor parameters are available immediately, which is another reason to prefer constructor injection. *(repo: JwtService)*

**11. BCryptPasswordEncoder is a Spring Security class the project cannot annotate. How does ApplicationConfiguration make it available for injection?**

Stereotype annotations like @Component only apply to classes whose source you own. For third-party classes, a @Bean method inside a @Configuration class is the way to register an instance (see spring-boot-basic Q2). *(repo: ApplicationConfiguration.passwordEncoder())*

**12. Two beans depend on each other through constructors:**

```java
@Service class OrderService { OrderService(PaymentService p) {} }
@Service class PaymentService { PaymentService(OrderService o) {} }
```

**What happens at startup on Spring Boot 3.4 with default settings?**

A constructor cycle cannot be satisfied because each object needs the other to exist first, and this fails even if spring.main.allow-circular-references is enabled. Since Boot 2.6, setter and field cycles are also rejected by default; the fix is to redesign the dependency (or, as a last resort, use @Lazy).

## Security & Testing Basics (Q13-Q18)

**13. SecurityConfig calls `.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)`. What does this do?**

Spring Security runs an ordered list of filters inside a SecurityFilterChain, and addFilterBefore positions a custom filter relative to a known one. The JWT filter must run before AuthorizationFilter so the request is already authenticated when access rules such as anyRequest().authenticated() are evaluated. *(repo: SecurityConfig)*

**14. A caller sends a valid JWT for an existing account to an endpoint that requires a role the account does not have. Under standard Spring Security behavior, what is the outcome?**

Authentication establishes who the caller is; authorization decides what that identity may do. An authenticated caller who fails an access check gets 403 from the access-denied handler, whereas the entry point (HttpStatusEntryPoint returning 401 here) is used for callers who are not authenticated. *(repo: SecurityConfig)*

**15. JwtService signs tokens with HS256 using the configured secret key. Which statement about the resulting JWT is correct?**

A signed JWT (JWS) is encoded, not encrypted: the claims can be decoded by anyone, so never put secrets in them. The server validates the signature and the expiry claim itself, which is why no server-side session is needed (stateless auth). *(repo: JwtService)*

**16. ApplicationConfiguration exposes a BCryptPasswordEncoder, and calling encode("secret") twice returns two different strings. How should a login check verify a password?**

BCrypt is a one-way, salted, adaptive hash: each encode() generates a fresh random salt and stores it inside the resulting string. matches() extracts that salt, re-hashes the raw password, and compares the results, so hashes are never decrypted or compared directly. *(repo: ApplicationConfiguration.passwordEncoder())*

**17. CartControllerTest uses @WebMvcTest(controllers = CartController.class) and declares `@MockBean CartService cartService`. Why is that mock needed?**

A test slice builds only the beans relevant to one layer: @WebMvcTest covers controllers, advice, converters, and filters, but not @Service or @Repository beans. @DataJpaTest slices the JPA layer, and @SpringBootTest loads the full context (see spring-boot-basic Q16). *(repo: CartControllerTest)*

**18. CartServiceTest uses @Mock with MockitoExtension, while CartControllerTest uses @MockBean. What is the difference?**

@Mock has no Spring involvement, so it suits fast unit tests with @InjectMocks. @MockBean puts the mock into the Spring test context so that MockMvc and other beans use it. Since Boot 3.4 it is deprecated in favour of Spring Framework 6.2's @MockitoBean, which does the same job but is not identical (for example, it is not supported on @Configuration classes). *(repo: CartServiceTest / CartControllerTest)*

## MVC & REST (Q19-Q30)

**19. What does the @RestController annotation add compared with a plain @Controller?**

@RestController is a meta-annotation combining @Controller and @ResponseBody. Every handler's return value is serialized by an HttpMessageConverter (Jackson for JSON) instead of being treated as a view name. *(repo: ProductController)*

**20. CartController.getCart declares `@RequestParam Integer userId`. A client calls GET /api/v1/cart with no query string. With default settings, what happens?**

@RequestParam is required by default, so a missing parameter raises MissingServletRequestParameterException before the method body runs; the repo's GlobalExceptionHandler maps it to 400 as well. Use required = false or a defaultValue to make it optional. *(repo: CartController.getCart)*

**21. ProductController.create builds a URI as shown and returns it via ResponseEntity.created(location). For a POST to /api/v1/products that saves id 7, what does the client receive?**

```java
ServletUriComponentsBuilder.fromCurrentRequest()
    .path("/{id}")
    .buildAndExpand(saved.getId())
    .toUri();
```

The builder starts from the current request URL (/api/v1/products), appends /{id} and expands it with the saved id. ResponseEntity.created(location) sets status 201 and the Location header, telling the client where the new resource lives. *(repo: ProductController.create)*

**22. ProductController.delete returns `ResponseEntity.noContent().build()`. What does a client receive after a successful DELETE /api/v1/products/7?**

noContent() sets status 204, which by definition carries no response body. The ResponseEntity<Void> return type matches that: there is nothing to serialize. *(repo: ProductController.delete)*

**23. ProductController.create takes `@Valid @RequestBody Product product`, and Product.name is @NotBlank. A client POSTs `{"name": ""}`. What happens by default?**

@Valid on a @RequestBody makes Spring run Bean Validation right after deserialization; on failure it throws MethodArgumentNotValidException and the handler never executes. In this repo the exception is caught by GlobalExceptionHandler and returned as a 400 ValidationErrorResponse. *(repo: ProductController.create; GlobalExceptionHandler.handleValidationException)*

**24. What is the difference between @Valid and @Validated?**

@Valid comes from the Jakarta Bean Validation spec and has no groups attribute. @Validated is Spring's own annotation, which adds group selection (and, at class level, method-level validation).

**25. ProductController.getById calls `productService.findById(id).orElseThrow(() -> new EntityNotFoundException(...))`. How does the client end up with a 404 response?**

@RestControllerAdvice is @ControllerAdvice plus @ResponseBody, so its @ExceptionHandler methods apply to all controllers and return serialized bodies. GlobalExceptionHandler maps EntityNotFoundException to a 404 ApiErrorResponse. *(repo: GlobalExceptionHandler.handleEntityNotFoundException)*

**26. In Spring Framework 6 and Spring Boot 3, what is ProblemDetail?**

ProblemDetail is Spring's container for the RFC 9457 (formerly RFC 7807) standard error body, rendered with the application/problem+json media type. Setting spring.mvc.problemdetails.enabled=true makes Boot use it for built-in MVC exceptions.

**27. An endpoint produces JSON only (no XML converter is on the classpath) and a client sends `Accept: application/xml`. With default Spring MVC behaviour and no custom exception handlers, what status is returned?**

Content negotiation matches the Accept header against the media types the handler can produce; with no match Spring throws HttpMediaTypeNotAcceptableException, which maps to 406. 415 is for an unsupported request Content-Type instead.

**28. A controller method is declared `getAll(Pageable pageable)`. For `GET /products?page=0&size=10&sort=name,desc`, where do the Pageable values come from?**

Spring Data's PageableHandlerMethodArgumentResolver, auto-configured by Boot when Spring Data web support is present, builds the Pageable from the page (zero-based), size and sort=property,direction query parameters.

**29. All CRUD controllers in this repo are mounted under /api/v1/. What is the main trade-off of this URI-path versioning strategy?**

Path versioning makes the version visible in every URL, which is simple to route, cache and try from a browser. The cost is that the resource URLs change per version. See rest-api Q8 for header and media-type alternatives. *(repo: All controllers under /api/v1/ (e.g. ProductController))*

**30. A client times out and retries the same request. Comparing PUT /api/v1/products/42 (full body) with POST /api/v1/products, which statement is correct?**

Idempotent means repeating the call leaves the same server state; PUT replaces the resource at a known URI, so repeats converge. POST typically creates a new resource each time. Idempotent is not the same as safe (see rest-api Q2). *(repo: ProductController.update / create)*

## Spring Data JPA Basics (Q31-Q40)

**31. Spring Data JPA sees this method on CartRepository. What does it do with it?**

```java
Optional<Cart> findByUserIdAndProductId(
    Integer userId, Integer productId);
```

Spring Data parses the method name at startup: Cart has no userId property, so UserId is resolved as the nested path user.id (same for product.id), joined with AND. The Optional return type yields empty when no row matches. *(repo: CartRepository.findByUserIdAndProductId)*

**32. CartRepository extends JpaRepository<Cart, Integer> and overrides `List<Cart> findAll()`. What does JpaRepository offer beyond CrudRepository?**

JpaRepository (which extends ListCrudRepository and ListPagingAndSortingRepository) adds flush(), saveAllAndFlush(), deleteAllInBatch() and similar, and its finders return List rather than Iterable. Derived queries work with any repository interface. *(repo: CartRepository)*

**33. A method annotated with @Transactional throws a checked exception (for example IOException). With default settings, what happens to the transaction?**

Spring's default rule rolls back for unchecked exceptions (RuntimeException) and Error, but not for checked ones. Use @Transactional(rollbackFor = ...) to change that.

**34. What does this method do to the database?**

```java
@Transactional
public void rename(Integer id) {
    Product p = repo.findById(id).orElseThrow();
    p.setName("New");
}
```

Entities loaded inside a transaction are managed; at flush time Hibernate compares them with their loaded snapshot (dirty checking) and writes an UPDATE for any changes. See spring-boot Q9.

**35. ProductController.update calls `updatedProduct.setId(id)` and then `productService.save(updatedProduct)`, which delegates to JpaRepository.save. What does Spring Data JPA do?**

SimpleJpaRepository.save checks whether the entity is new (for a wrapper id type and no version, id == null). New entities go through EntityManager.persist; others go through merge, which copies the state onto a managed instance. *(repo: ProductController.update; ProductService.save)*

**36. Cart uses `@GeneratedValue(strategy = GenerationType.IDENTITY)`. What is a notable consequence compared with SEQUENCE?**

With IDENTITY the id only exists after the INSERT executes, so Hibernate cannot defer inserts and transparently disables insert batching. SEQUENCE can pre-allocate ids (pooled optimizer) and keep inserts batchable. *(repo: Cart)*

**37. Given `@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true) List<OrderItem> items`, what does orphanRemoval = true add?**

CascadeType.REMOVE only propagates when the parent is removed. orphanRemoval = true additionally deletes a child row once it is removed from the parent's collection (at flush), instead of just detaching it.

**38. How does this @Query differ from one declared with nativeQuery = true?**

```java
@Query("select p from Product p where p.price > :min")
List<Product> findExpensive(@Param("min") Double min);
```

JPQL is written against the entity model (Product, p.price) and Hibernate translates it into the database's SQL dialect, which keeps it portable. A native query is passed as raw SQL using real table and column names.

**39. application.properties sets `spring.jpa.hibernate.ddl-auto=update`. Why is that risky in production?**

update makes Hibernate additively patch the schema on boot: it never drops or renames columns or migrates data, and the changes are unversioned and unreviewed. Production should use Flyway or Liquibase, with ddl-auto set to validate or none. *(repo: application.properties)*

**40. A repository method returns `Slice<Product>` instead of `Page<Product>`. What is the difference?**

A Page triggers an additional count query to know the total elements and pages, which can be expensive. A Slice only knows whether another slice follows, which is enough for infinite scroll or next/previous navigation.

# Part 2 - Expert

## JPA & Hibernate Deep Dive (Q41-Q54)

**41. `Product` declares `@ManyToOne @JoinColumn(name = "category_id") private Category category;` with no fetch attribute. A job calls `productRepository.findAll()` on 500 products that belong to 8 categories. What does Hibernate 6 actually do?**

`@ManyToOne` defaults to EAGER. `em.find`/`findById` can join it into the same SQL, but a JPQL or derived query like `findAll()` does not, so Hibernate fills each EAGER association with secondary selects; the persistence context de-duplicates repeated ids. See jpa-fetching Q1-Q3. *(repo: Product.category)*

**42. A screen lists 100 `Order` rows and touches the lazy `@OneToMany items` of each, producing 101 queries. The repository method is a derived query you may not change; you can only change the entity mapping. Which change cuts the query count the most?**

`@BatchSize` lets Hibernate initialize up to N pending lazy collections in one IN-list query, turning 100 lookups into roughly 100/20 extra queries. It is a mapping-level mitigation; JOIN FETCH or `@EntityGraph` remain the exact fix when you control the query (jpa-fetching Q4).

**43. With `spring.jpa.open-in-view=false`, a new endpoint returns an `Order` entity and Jackson fails with `LazyInitializationException` on `items`. The service is under heavy load. Which fix is best?**

Deciding the fetch plan per use case, inside the service/repository, keeps the connection short-lived and makes N+1 visible instead of hiding it. Keeping the session open across the web layer (OSIV or a controller transaction) holds a pooled connection during serialization. See jpa-fetching Q6. *(repo: application.properties open-in-view=false; CartRepository @EntityGraph)*

**44. `findByUserId` is temporarily declared WITHOUT `@EntityGraph`, `open-in-view` is false and the caller has no transaction, so `Cart.product` is an uninitialized lazy proxy. After `Cart c = cartRepository.findByUserId(1).get(0);` which line throws `LazyInitializationException`?**

Only a call that needs state beyond the identifier forces initialization, and that needs an open Session. Holding or null-checking the proxy touches no data, and by default Hibernate answers `getId()` from the identifier the proxy already holds (unless `hibernate.jpa.compliance.proxy` is enabled). *(repo: Cart.product; CartRepository.findByUserId)*

**45. `Order` has `List<OrderItem> items` and `List<Payment> payments`, both lazy `@OneToMany`. The query `select o from Order o join fetch o.items join fetch o.payments where o.id = :id` throws `MultipleBagFetchException`. An order has 20 items and 10 payments. Which fix avoids the exception AND the row explosion?**

Joining two collections in one SQL produces items x payments = 200 rows per order, and with bags Hibernate cannot tell duplicates from real rows, so it refuses. Loading one collection per query in the same persistence context keeps each result at 20 and 10 rows and attaches both to the same managed `Order`. `Set` silences the exception but keeps the Cartesian product.

**46. Suppose `OrderService.placeOrder` (`@Transactional`, default REQUIRED) calls `auditService.record(...)` on another bean whose method is `@Transactional(propagation = REQUIRES_NEW)`, and then throws a `RuntimeException`. What happens to the audit row?**

REQUIRES_NEW suspends the current transaction and starts a new physical one, which commits or rolls back independently of the outer one. The cost is a second pooled connection held at the same time, and it only takes effect when the call crosses a Spring proxy (see Q52). *(repo: OrderService.placeOrder)*

**47. PostgreSQL. Rows A and B in `oncall(doctor, on_duty)` are both on duty; the rule is that at least one must stay on duty.**

```
Tx1: SELECT count(*) FROM oncall WHERE on_duty;  -- 2
Tx2: SELECT count(*) FROM oncall WHERE on_duty;  -- 2
Tx1: UPDATE oncall SET on_duty=false WHERE doctor='A'; COMMIT;
Tx2: UPDATE oncall SET on_duty=false WHERE doctor='B'; COMMIT;
```

**Both commit under REPEATABLE READ. What is this anomaly, and what prevents it?**

Under PostgreSQL's snapshot-based REPEATABLE READ each transaction sees its own snapshot and the two UPDATEs hit different rows, so both commit and violate the invariant. Only SERIALIZABLE detects the read/write dependency and raises a serialization failure (SQLSTATE 40001), so the application must retry. See relational-db Q10.

**48. `Product` has `@Version private Long version;`. Two admins load the same product at version 5 and each saves a different stock value. What is the outcome?**

@Version adds the expected version to the UPDATE's WHERE clause; a stale writer updates zero rows, Hibernate throws an optimistic-locking failure (Spring translates it to `ObjectOptimisticLockingFailureException`), and no DB lock is held while the user is thinking. The caller must reload and retry or report a conflict.

**49. A flash sale decrements `Product.stock` for one hot product from about 500 concurrent requests. With `@Version` most updates fail and clients retry in storms. What does switching the lookup to `@Lock(LockModeType.PESSIMISTIC_WRITE)` change?**

PESSIMISTIC_WRITE takes a row lock (`SELECT ... FOR UPDATE`) that is held until commit or rollback, serializing writers on the hot row instead of letting them fail and retry. Keep the transaction short, lock rows in a consistent order to avoid deadlocks, and expect lock-wait timeouts under load. *(repo: Product.stock)*

**50. Under READ COMMITTED, inside one `@Transactional` method:**

```
Product a = productRepository.findById(1).get();          // SELECT
// another transaction commits: UPDATE products SET price = 99 WHERE id = 1
Product b = productRepository.findByName(a.getName()).get(); // JPQL
```

**What does `b.getPrice()` return?**

The persistence context guarantees one managed instance per id. A query still runs SQL, but each returned row is resolved to the instance already in the context, whose loaded state is not overwritten. Use `em.refresh(a)` or `em.clear()` to see newer data.

**51. `CartController.getCart` calls `Collectors.groupingBy(Cart::getProduct, ...)`. `Product` (Lombok `@Getter`/`@Setter` only) overrides neither `equals` nor `hashCode`. Why does the grouping work today, and when would it silently break?**

Without an override, `Object` identity is used. Within one persistence context Hibernate returns a single instance per row id, which makes identity equal id-equality; detached copies, instances from another session, or a proxy versus a real instance would not be equal. A safe entity `equals` must be proxy-aware and based on a stable key. *(repo: CartController.getCart; Product)*

**52. A teammate makes `CartService.bumpQuantity` public and adds `@Transactional(propagation = REQUIRES_NEW)` to it. `addToCart` (unannotated, same class) still calls it as `bumpQuantity(existing.get(), quantity)`. What does the annotation do for that call?**

Spring's default proxy-based AOP only intercepts calls that enter through the proxy; a call on `this` from inside the same bean runs the raw method. Move the method to another bean, inject a self-reference, or use AspectJ weaving. *(repo: CartService.bumpQuantity)*

**53. A service method is annotated `@Transactional(readOnly = true)` and by mistake modifies a loaded entity's field before returning. With Spring Boot 3.4 and Hibernate, which description is most accurate?**

In Spring's `HibernateJpaDialect` a read-only transaction sets `FlushMode.MANUAL`, makes the session default read-only and passes a read-only hint to the JDBC connection. That skips flush-time dirty checking (a real saving on query-only paths) but does not make an accidental write fail loudly.

**54. Consider the following:**

```
@Query("select o from Order o join fetch o.items")
Page<Order> findAllWithItems(Pageable pageable);
```

**Hibernate logs `HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory`. What is happening, and what is the best fix?**

A collection join multiplies rows per parent, so the database cannot limit by parent entity; Hibernate fetches everything and paginates in memory. Two-step loading (page ids or parents, then `join fetch ... where o.id in :ids`) or `@BatchSize` keeps the page bounded in SQL.

## Concurrency & Data Integrity (Q55-Q60)

**55. In `CartService.addToCart`, two concurrent requests for the same (user, product) both run `findByUserIdAndProductId` and both see no row. What actually guarantees that only one cart line survives?**

A SELECT that finds nothing takes no lock on the missing row, so any application-level check-then-act can interleave, even inside a transaction. A unique index makes the duplicate check and the insert atomic inside the database; the pre-check only avoids the exception on the common path. *(repo: Cart @Table uniqueConstraints; CartService.addToCart)*

**56. `addToCart` inserts, catches `DataIntegrityViolationException`, then re-queries the existing row. On PostgreSQL, why is it deliberately NOT wrapped in a single `@Transactional`?**

On PostgreSQL any failed statement aborts the transaction: further commands are rejected until rollback, and Spring/JPA also mark it rollback-only. Without an outer transaction each repository call commits or fails on its own, so the catch-block fallback query can still run. See jpa-fetching Q7. *(repo: CartService.addToCart)*

**57. Assume `Cart` used a SEQUENCE id and `addToCart` were called inside a caller's larger `@Transactional`:**

```
try { return cartRepository.save(cart); }
catch (DataIntegrityViolationException e) { ... }
```

**When does the unique-constraint violation surface?**

With a sequence id, `persist` only queues the INSERT and the SQL runs at flush time, so the exception escapes the try block later (with IDENTITY ids, as in this repo's `Cart`, the INSERT runs at persist, making `saveAndFlush` a safeguard). `saveAndFlush` forces the check to fire where the handler is. *(repo: CartService.addToCart)*

**58. A teammate proposes making `addToCart` `synchronized` to fix the duplicate-cart-line race. The service runs as 3 Kubernetes replicas. Which review comment is correct?**

An intrinsic lock only coordinates threads inside one JVM, so replicas (or a restarted pod) each have their own monitor; the DB unique index is the shared source of truth. (With `@Transactional` the lock would even be released before commit.) See spring-boot-expert Q15 for the same per-instance trap. *(repo: CartService.addToCart)*

**59. An admin raises a product from 10.00 to 12.00 after a customer added it to the cart. `placeOrder` totals with `c.getPrice() * c.getQuantity()` and copies `cart.getPrice()` into `OrderItem.price`. Which statement about this design is correct?**

`Product.price` is mutable, so a value read live would change what the customer is charged and would rewrite past orders. Copying it into `Cart.price` and then `OrderItem.price` makes each line a historical fact; `getCart` still exposes the live price and a `priceChanged` flag for display. *(repo: OrderService.placeOrder; Cart.price)*

**60. A client times out on `POST /orders/place`; the first request actually committed, and the mobile app retries after 5 seconds. What reliably prevents a duplicate order?**

The server cannot tell a retry from a new order unless the client labels the request. Persisting the key with a unique constraint atomically with the order makes the retry detectable across instances; serializing calls or locking only orders them, and `@Version` guards updates of existing rows, not new inserts. *(repo: OrderService.placeOrder)*

## Microservices with Spring (Q61-Q73)

**61. Checkout in a split-up system calls Inventory, Pricing and Payment one after another over synchronous HTTP, and each service has 99.9% availability. Which statement best describes the trade-off?**

Serial synchronous dependencies multiply failure probability (0.999^3 is about 0.997) and add their latencies, so only steps the user is actually waiting on should stay on the request path. See microservices Q5 and Q6.

**62. A new service on Spring Boot 3.4 (Spring MVC, blocking JDBC, no WebFlux on the classpath) needs a fluent HTTP client to call other services. Which choice is most appropriate?**

RestClient is the synchronous, fluent counterpart to WebClient introduced in Spring Framework 6.1, and Boot auto-configures a RestClient.Builder so observability instrumentation applies. WebClient needs the reactive stack and OpenFeign is a Spring Cloud project, not part of Spring Framework.

**63. Resilience4j is configured as below and the breaker has just opened. What happens on the first call after 30 seconds have passed?**

```properties
slidingWindowSize=10
minimumNumberOfCalls=10
failureRateThreshold=50
waitDurationInOpenState=30s
permittedNumberOfCallsInHalfOpenState=3
```

While OPEN, calls are rejected with CallNotPermittedException. After the wait duration the breaker goes HALF_OPEN and permits only permittedNumberOfCallsInHalfOpenState calls, whose failure rate decides between CLOSED and OPEN. See expert Q3.

**64. The gateway, Order service and Payment service each wrap their outbound call in a retry of 3 attempts, and Payment's bank API is slow and failing. What is the main risk and the best mitigation?**

Nested retries amplify load exponentially on a dependency that is already struggling (a retry storm). Keep retries at a single layer, add backoff with jitter, and keep timeouts consistent with the overall deadline. See microservices Q15 and Q16.

**65. A method returning ShippingQuote (not CompletableFuture) is annotated @Bulkhead(name = "shipping") with Resilience4j defaults plus maxConcurrentCalls=10 and maxWaitDuration=0. The carrier API stalls. What happens?**

The default bulkhead type is SEMAPHORE, which caps concurrent executions on the calling thread; a wait duration of 0 means no waiting, so extra calls are rejected. The THREADPOOL type runs work on a separate pool and requires a CompletableFuture return type.

**66. OrderService.placeOrder saves an Order (status pending) and its items in one local @Transactional. Order and Payment become separate services with separate databases, and payment fails after the order row has committed. What is the correct way to restore a consistent state?**

A local @Transactional cannot span services. In a saga every step is its own local commit and failures are reversed by compensating actions, here moving the order to the existing Order.Status.cancelled; orchestration versus choreography only decides who triggers the compensation. See expert Q7 and microservices Q19. *(repo: OrderService.placeOrder)*

**67. After the Order row commits, the code calls rabbitTemplate.convertAndSend(...) to announce it, as OrderMessageSender does. The JVM crashes between the commit and the publish, and the event is lost. Which design closes this dual-write gap?**

The outbox turns two writes to two systems into one atomic local write plus a retryable relay, so a crash can delay an event but not lose it. Delivery becomes at-least-once, which is why consumers must be idempotent (see Q70). *(repo: OrderMessageSender)*

**68. CartService copies product.getPrice() into Cart.price when a line is added. Suppose Catalog and Cart become separate services and Cart keeps its own read-only copy of product name and price, updated by ProductPriceChanged events. What does this event-carried state transfer give and cost?**

Events carry the state consumers need, so each service holds a local replica and has no runtime dependency on the owner, at the price of staleness. The repo's Cart.price snapshot is the same idea done manually inside one database. *(repo: CartService (price snapshot))*

**69. A team adds spring-cloud-starter-gateway to an existing Spring MVC service (spring-boot-starter-web) to turn it into an API gateway. It fails at startup, complaining that Spring MVC is incompatible. What is the underlying reason?**

Classic Spring Cloud Gateway is a non-blocking WebFlux/Netty proxy, which suits an edge service holding many slow connections, and it runs as a standalone application. A separate servlet-based variant (Gateway Server MVC) exists for MVC stacks.

**70. OrderMessageListener consumes from RabbitMQ. With no extra configuration, an exception thrown while handling one message causes Spring AMQP to reject and requeue it, so it is redelivered in a tight loop. The broker can also redeliver after a consumer crash. What is the right production design?**

RabbitMQ delivers at-least-once, so redelivery is normal and consumers must dedupe by ID. Capped retries followed by reject-without-requeue send poison messages to a dead-letter exchange instead of looping forever. See microservices Q14 and Q16. *(repo: OrderMessageListener)*

**71. After moving to Spring Boot 3, a service has micrometer-tracing-bridge-brave and a Zipkin reporter, yet traces break at one hop: the downstream service always starts a new trace ID. The caller creates its client with `new RestTemplate()`. Why?**

Micrometer Tracing and the Observation API hook in through the builders Boot auto-configures, so inject and use those to have the trace context propagated on outgoing calls. See expert Q4.

**72. Order's tests stub the Payment client with a hand-written stub returning {"status":"PAID"}. The Payment team renames that field to "state". Both pipelines stay green and production breaks. Which practice would have caught it before deployment?**

A consumer's own stub only encodes the consumer's assumptions. A contract is verified from both sides, so the provider's build fails when it changes a shape a consumer relies on. See microservices Q20.

**73. You are splitting this monolith (Product, Category, Discount, Review, User, Address, Cart, Order, OrderItem, Payment, Shipping, Wishlist). Payment has a @ManyToOne to Order and OrderItem has one to Product. Which split best follows bounded contexts?**

Bounded contexts follow business capabilities and data ownership, so cross-context JPA relations become ID or snapshot references (OrderItem already stores its own price) with consistency handled by events or sagas. See microservices Q11. *(repo: Payment / OrderItem entities)*

## Production Readiness (Q74-Q81)

**74. Postgres has max_connections=100. HikariCP's maximum-pool-size is 10 by default, but suppose the team raises it to 20 (spring.datasource.hikari.maximum-pool-size=20, open-in-view=false) and the app may autoscale to 8 pods. During a scale-out, new pods fail with 'too many clients' errors. What is the right fix?**

Each pod has its own pool, so total connections = pods x maximum-pool-size, and that must stay below max_connections with headroom for admin and other clients (8 x 20 = 160 > 100; the repo's default pool of 10 would give 80). A smaller pool often performs as well or better because there is less DB-side contention. Setting minimum-idle equal to maximum-pool-size changes nothing (it is already Hikari's default) and keeps more connections open. See expert Q9. *(repo: application.properties (default Hikari pool))*

**75. application.properties has management.endpoints.web.exposure.include=loggers, and SecurityConfig ends with anyRequest().authenticated() (no actuator rule). A team adds httpGet probes on /actuator/health/liveness and /actuator/health/readiness to the Deployment. What happens?**

An explicit include list replaces Boot's default web exposure, and a custom SecurityFilterChain protects /actuator/** like any other path. Expose health, permit /actuator/health/** for the kubelet, and keep downstream dependencies out of the liveness group. See expert Q2. *(repo: application.properties, SecurityConfig)*

**76. The Deployment in k8s/app has no probes and no terminationGracePeriodSeconds, and the app runs on Spring Boot 3.4 with default settings. During rolling updates users still see failed or cut-off checkout requests. Which change fixes this?**

Since Boot 3.4 graceful shutdown is already the default (server.shutdown=graceful with a 30s spring.lifecycle.timeout-per-shutdown-phase; earlier versions defaulted to immediate), so setting it changes nothing. Draining only works if Kubernetes cooperates: a readiness probe (readiness turns REFUSING_TRAFFIC during shutdown) stops routing to a terminating pod and holds traffic back from a starting one, and Kubernetes sends SIGKILL after terminationGracePeriodSeconds (default 30s), so the Spring phase timeout must fit inside it. See expert Q13. *(repo: k8s/app/ecommerce_deployment.yaml)*

**77. This repo runs Spring Boot 3.4.5 on Java 17 (Dockerfile: eclipse-temurin:17-jre-jammy) with blocking JPA. A colleague proposes spring.threads.virtual.enabled=true to handle more concurrent requests. Which statement is accurate?**

Boot supports the property since 3.2, but virtual threads are a Java 21 feature. They make blocked request threads cheap, not the resources behind them, so the connection pool stays the bottleneck for JDBC. *(repo: pom.xml (java.version 17), Dockerfile)*

**78. Suppose ProductService.findById gets @Cacheable("products") with @EnableCaching and no cache provider configured, and ProductService.save gets @CacheEvict. The app runs as 3 pods, and users see old prices for hours after a price update. What explains this?**

Without a provider Boot uses a simple ConcurrentMap cache with no expiry and no cross-pod visibility. @Cacheable has no TTL attribute, so expiry comes from the cache provider and multi-pod consistency needs a shared cache. See expert Q12. *(repo: ProductService)*

**79. Autoscaling reacts slowly because each pod needs about 40 seconds to start. Which statement about the available options is correct?**

Lazy initialization only defers work, and native images trade build time, reflection/proxy hints and peak throughput for fast startup and a small footprint. Choose based on whether cold-start or steady-state performance dominates. See expert Q14.

**80. The repo's Deployment runs replicas: 1. Suppose the app adds an in-memory rate limiter of 100 requests/min per client key. The team then scales to 4 pods behind a Service. How should the 100/min limit be kept?**

Per-pod counters allow up to N times the intended rate and drift as pods scale, so the counter must be shared and keyed per client, ideally enforced at the gateway. See expert Q15. *(repo: k8s/app/ecommerce_deployment.yaml (replicas: 1))*

**81. In OrderService.placeOrder a developer adds a Micrometer counter tagged with userId and orderId so support can trace individual orders. After a week the metrics backend runs out of memory. What is the right approach?**

Every distinct tag combination creates a new time series, so unbounded values such as IDs explode storage. Metrics answer aggregate questions while logs and traces (linked by traceId in the MDC) answer per-request ones. See Q71. *(repo: OrderService.placeOrder)*

## Advanced Security & Testing (Q82-Q85)

**82. A controller method carries @PreAuthorize("hasRole('ADMIN')"), but no configuration class has @EnableMethodSecurity (SecurityConfig only has @EnableWebSecurity and anyRequest().authenticated()). A logged-in customer calls it. What happens?**

@PreAuthorize is enforced by an interceptor registered only when @EnableMethodSecurity (Security 6, prePostEnabled true by default) is present. Without it the annotation is silently inert, so add it and test with a non-admin user. *(repo: SecurityConfig)*

**83. UserDetailsServiceImpl builds authorities with .roles(user.getRole().name()) and the Role enum is { admin, customer }. The team then protects an endpoint with hasRole("ADMIN") (a URL rule, or @PreAuthorize once @EnableMethodSecurity is added). What does a user with role admin get, and why?**

roles(...) prepends ROLE_ to the value, giving ROLE_admin, while hasRole("ADMIN") is hasAuthority("ROLE_ADMIN") and the comparison is case-sensitive. Normalize the case when building authorities (or rename the enum) and add a test that an admin passes. *(repo: UserDetailsServiceImpl, User.Role)*

**84. Tests use H2 (MODE=PostgreSQL, H2Dialect) while production runs PostgreSQL. CartService.addToCart is deliberately not @Transactional because Postgres aborts a transaction after any failed statement. Which testing approach best guards behaviour like this?**

Testcontainers runs the real engine, and @ServiceConnection derives the datasource details from the container without manual property wiring. H2's compatibility mode and Hibernate's dialect change syntax and generated SQL, not the engine's transaction, locking and type behaviour. See expert Q10. *(repo: CartService, application-test.properties)*

**85. The repo issues HS256 JWTs valid for 12,222,200,000 ms (about 141 days), and JwtService.isTokenValid checks only the subject and expiry. In a microservice setup a customer's token leaks. What is the most accurate statement?**

Stateless validation has no server-side record to delete, so early revocation needs either short lifetimes (with revocable refresh tokens) or added state such as a jti denylist. Services should also validate tokens themselves rather than trusting the network, ideally with asymmetric keys (JWKS) so verifiers cannot mint tokens. See expert Q11. *(repo: JwtService, application.properties (security.jwt.expiration-time))*
