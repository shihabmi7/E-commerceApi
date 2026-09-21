# Spring Boot Interview Questions - MCQ Self-Test (Basic + Expert)

85 multiple-choice questions with a separate answer key at the bottom. Attempt the questions first; the correct letters are deliberately spread across A-D (no pattern to guess).

**Based on:** Spring Boot 3.4 / Spring Framework 6.2 / Spring Security 6 / Hibernate 6 / Jakarta EE 10. Questions tagged `repo:` in the answer key point at real code in this e-commerce project.

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
- [Answer Key](#answer-key)

---

# Part 1 - Basic

## Core Spring Boot (Q1-Q12)

**1. SecurityConfig takes a JwtFilter in its constructor, yet no code in the project ever calls new JwtFilter(...). What makes this work?**

- A) The JVM instantiates every class annotated with @Component at class-loading time, and Spring later looks the instance up by its type when needed
- B) JwtFilter is a @Component, so component scanning found it; the ApplicationContext created it and injected it into the constructor
- C) The servlet container creates JwtFilter because it extends OncePerRequestFilter, then hands the instance to Spring Security
- D) Spring instantiates every class that sits in the same package as the @Configuration class referencing it, whether annotated or not

**2. UserDetailsServiceImpl declares `private final UserRepository userRepository;` and sets it in its constructor. What is a concrete benefit over putting @Autowired on a private field?**

- A) Constructor injection avoids reflection entirely, so beans are created noticeably faster at startup and use less memory per bean
- B) Constructor injection is the only style that allows two beans to depend on each other
- C) The dependency can be final and required, and a test can call new UserDetailsServiceImpl(mockRepo) without Spring
- D) Spring can swap the injected bean at runtime when constructor injection is used, which field injection does not allow

**3. ECommerceApiApplication carries a single @SpringBootApplication annotation. Which annotations does it combine?**

- A) @Configuration, @EnableWebMvc, and @EnableTransactionManagement
- B) @Component, @EnableAutoConfiguration, and @EnableConfigurationProperties
- C) @SpringBootConfiguration, @EnableAutoConfiguration, and @ComponentScan
- D) @Configuration, @EnableAutoConfiguration, and @EnableJpaRepositories

**4. Boot's UserDetailsServiceAutoConfiguration (default in-memory user with a generated password) is guarded by @ConditionalOnMissingBean for types such as AuthenticationProvider and UserDetailsService. This project defines both beans. What happens?**

- A) The auto-configuration backs off, so no default user or generated password is created
- B) Both sets of beans are registered, and the generated password still works for logging in
- C) Startup fails with NoUniqueBeanDefinitionException because two UserDetailsService beans exist
- D) The auto-configuration replaces the project's beans, since auto-configuration is processed last

**5. The project's pom.xml declares spring-boot-starter-web, and the built jar runs with plain `java -jar` on a machine with no Tomcat installed. Why?**

- A) The spring-boot-maven-plugin copies a Tomcat installation into every jar, regardless of which dependencies are declared
- B) spring-boot-starter-web contains the auto-configuration and Tomcat code itself, compiled into one module
- C) Without a servlet container on the host, Spring Boot falls back to the JDK's built-in HTTP server that ships with the JVM
- D) The starter pulls in embedded Tomcat as a dependency, and Boot auto-configures it to run inside the app's own JVM

**6. application.properties sets `security.jwt.secret-key=${JWT_SECRET_KEY}` with no default. application-test.properties sets the same key to a literal value. You start with the test profile active and JWT_SECRET_KEY unset. What happens?**

- A) Startup fails, because application.properties is always resolved first and its placeholder has no value to fall back on
- B) Startup fails, because a profile-specific file can only add new keys and never overrides ones from the base file
- C) It starts normally: the profile-specific value overrides the base one, so ${JWT_SECRET_KEY} is never resolved
- D) It starts, but JwtService receives the literal text ${JWT_SECRET_KEY} as its signing secret at runtime

**7. The jar's application.properties has server.port=8080. The container sets the environment variable SERVER_PORT=9090, and the app is launched with `java -jar app.jar --server.port=7070`. Which port is used?**

- A) 8080, because the properties file packaged with the code takes priority over any external source
- B) 9090, because operating-system environment variables outrank every other configuration source
- C) None: startup fails, because Boot refuses to reconcile three conflicting values for one key
- D) 7070: command-line arguments outrank environment variables, which outrank the packaged file

**8. JwtService reads security.jwt.secret-key and security.jwt.expiration-time with separate @Value fields. What is a real advantage of @ConfigurationProperties(prefix = "security.jwt") for this?**

- A) Values are re-read from the properties file at runtime, so edits take effect without restarting the app
- B) It supports SpEL expressions inside property values, a feature that @Value lacks entirely
- C) It binds the whole group into one typed object, with relaxed binding and optional @Validated checks
- D) It is the only mechanism that can read environment variables such as JWT_SECRET_KEY into a bean field

**9. JwtService is annotated only with @Service and no @Scope. What is its scope, and what follows from that?**

- A) Prototype: each injection point receives a new instance, so fields are private to each caller
- B) Request: Spring creates a new instance per HTTP request, so instance fields can safely hold per-request data
- C) Thread: Spring binds one instance to each Tomcat worker thread, so its fields are thread-confined
- D) Singleton: one shared instance per ApplicationContext, so it must be stateless or thread-safe

**10. JwtService declares `@Value("${security.jwt.secret-key}") private String secretKey;` on a field. Why would reading secretKey inside a constructor of JwtService give null?**

- A) @Value fields are resolved lazily, the first time a method reads the field, rather than at bean creation
- B) Property sources are not loaded until every bean in the context has finished construction
- C) @Value is only processed after the context has fully refreshed and ContextRefreshedEvent is published
- D) The constructor runs first, then @Value fields are populated, then init callbacks like @PostConstruct run

**11. BCryptPasswordEncoder is a Spring Security class the project cannot annotate. How does ApplicationConfiguration make it available for injection?**

- A) By placing @Component on a static factory method that returns the instance
- B) By marking a field @Autowired, which registers the object it points to as a bean
- C) By declaring a @Bean method in a @Configuration class that returns new BCryptPasswordEncoder()
- D) By adding the library's package to @ComponentScan, since scanning registers every class it finds, annotated or not

**12. Two beans depend on each other through constructors:**

```java
@Service class OrderService { OrderService(PaymentService p) {} }
@Service class PaymentService { PaymentService(OrderService o) {} }
```

**What happens at startup on Spring Boot 3.4 with default settings?**

- A) Spring injects an early reference to one bean, and the application starts normally after a debug log message
- B) Boot logs a warning and passes null into the second bean's constructor so that startup can continue
- C) Startup fails with BeanCurrentlyInCreationException, because neither bean can be constructed first
- D) The application starts, and a StackOverflowError is thrown on the first call that travels between the two beans

## Security & Testing Basics (Q13-Q18)

**13. SecurityConfig calls `.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)`. What does this do?**

- A) It places JwtFilter ahead of UsernamePasswordAuthenticationFilter so a bearer token can authenticate the request early
- B) It replaces UsernamePasswordAuthenticationFilter, which is removed from the chain so it can no longer process form logins
- C) It runs JwtFilter after the DispatcherServlet has already invoked the controller, so it can inspect and modify the response
- D) It registers JwtFilter in the servlet container outside Spring Security, so it always runs before the DelegatingFilterProxy

**14. A caller sends a valid JWT for an existing account to an endpoint that requires a role the account does not have. Under standard Spring Security behavior, what is the outcome?**

- A) 401 Unauthorized, because a missing role means the caller is not authenticated for that endpoint
- B) 404 Not Found, because Spring Security hides resources the caller may not access
- C) 403 Forbidden: the caller is authenticated, but authorization (what they may do) fails
- D) 200 OK, because a valid JWT is enough and role checks only apply to session-based login

**15. JwtService signs tokens with HS256 using the configured secret key. Which statement about the resulting JWT is correct?**

- A) Three Base64URL parts (header.payload.signature); the payload is readable by anyone, the signature detects tampering
- B) Its payload is encrypted with the secret key, so only the server holding that key can ever read the claims
- C) It is an opaque random string that the server looks up in an in-memory session table on every request
- D) It has two parts, claims plus a checksum, and the server must keep a record of each issued checksum to validate it

**16. ApplicationConfiguration exposes a BCryptPasswordEncoder, and calling encode("secret") twice returns two different strings. How should a login check verify a password?**

- A) Encode the submitted password again and compare the two resulting strings with equals() to check for a match
- B) Decrypt the stored hash with the application's secret key and compare it to the submitted password
- C) Call passwordEncoder.matches(rawPassword, storedHash), since the random salt is embedded in the hash
- D) Read the salt from a separate database column and pass it to encode() together with the raw password

**17. CartControllerTest uses @WebMvcTest(controllers = CartController.class) and declares `@MockBean CartService cartService`. Why is that mock needed?**

- A) @WebMvcTest loads only the MVC slice, so @Service beans are absent and the dependency must be mocked
- B) @WebMvcTest starts the full context like @SpringBootTest, and the mock stands in for the real database access
- C) @WebMvcTest loads all @Service beans but automatically replaces their repositories with an in-memory H2 database
- D) Spring cannot create a proxy for the controller unless every one of its collaborators is a Mockito mock

**18. CartServiceTest uses @Mock with MockitoExtension, while CartControllerTest uses @MockBean. What is the difference?**

- A) @Mock works only inside a @SpringBootTest context, while @MockBean also works in plain JUnit tests without Spring
- B) They are equivalent, and @MockBean is simply the newer name for @Mock when used outside of a Spring application
- C) @Mock can mock only interfaces, while @MockBean can also mock concrete classes such as CartService or a repository
- D) @Mock is a plain Mockito mock; @MockBean also registers the mock in the ApplicationContext, replacing any bean of that type

## MVC & REST (Q19-Q30)

**19. What does the @RestController annotation add compared with a plain @Controller?**

- A) It is @Controller plus @RequestMapping, which forces every handler method to return JSON
- B) It is @Component plus @EnableWebMvc, which registers JSON message converters for the class
- C) It is a @Controller that bypasses the DispatcherServlet and writes straight to the servlet response
- D) It is @Controller plus @ResponseBody, so return values become the response body, not view names

**20. CartController.getCart declares `@RequestParam Integer userId`. A client calls GET /api/v1/cart with no query string. With default settings, what happens?**

- A) MissingServletRequestParameterException is thrown, which becomes a 400 Bad Request
- B) userId is bound as null and the handler still runs, querying the carts of a null user id
- C) The request returns 404 Not Found because the URL no longer matches the mapping
- D) The request returns 415 Unsupported Media Type because no parameter source was supplied

**21. ProductController.create builds a URI as shown and returns it via ResponseEntity.created(location). For a POST to /api/v1/products that saves id 7, what does the client receive?**

```java
ServletUriComponentsBuilder.fromCurrentRequest()
    .path("/{id}")
    .buildAndExpand(saved.getId())
    .toUri();
```

- A) 200 OK, with the new id in the body only and no Location header, so clients must build the URL
- B) 201 Created, with a Location header ending in /api/v1/products/7 for the new product
- C) 201 Created, with a Location header equal to the collection URL /api/v1/products
- D) 204 No Content, with a Location header pointing at /api/v1/products/7

**22. ProductController.delete returns `ResponseEntity.noContent().build()`. What does a client receive after a successful DELETE /api/v1/products/7?**

- A) 200 OK with a CustomResponse body whose data field is null
- B) 204 No Content, with an empty response body
- C) 200 OK with an empty body, because ResponseEntity<Void> maps to 200 by default
- D) 202 Accepted, indicating the deletion will complete asynchronously

**23. ProductController.create takes `@Valid @RequestBody Product product`, and Product.name is @NotBlank. A client POSTs `{"name": ""}`. What happens by default?**

- A) The method runs and the errors are silently ignored unless a BindingResult parameter is declared
- B) The method runs and Hibernate rejects the row at flush, which surfaces as a 500 Internal Server Error
- C) Spring returns 415 Unsupported Media Type because the JSON does not satisfy the entity
- D) Spring throws MethodArgumentNotValidException before the method runs, giving a 400 Bad Request

**24. What is the difference between @Valid and @Validated?**

- A) @Valid is Spring-specific and supports validation groups, while @Validated is the standard jakarta.validation annotation
- B) @Valid is the standard jakarta.validation annotation; @Validated is Spring's variant that adds validation groups
- C) @Validated turns on Bean Validation for controllers, while @Valid only cascades validation into nested objects
- D) They are interchangeable aliases, and validation groups are selected with @Valid(groups = ...)

**25. ProductController.getById calls `productService.findById(id).orElseThrow(() -> new EntityNotFoundException(...))`. How does the client end up with a 404 response?**

- A) A @RestControllerAdvice has an @ExceptionHandler for it that returns a 404 ResponseEntity with a JSON body
- B) Spring MVC maps any RuntimeException thrown from a controller to 404 by default
- C) The servlet container converts the uncaught exception into a 404 through its default error page
- D) The controller must catch the exception itself, because @ExceptionHandler methods only work inside the same controller

**26. In Spring Framework 6 and Spring Boot 3, what is ProblemDetail?**

- A) A Bean Validation interface implemented by each constraint violation, exposing its property path, message and invalid value
- B) A Spring Security class describing authentication failures, produced only by security filters
- C) A class for the RFC 9457 error format (type, title, status, detail, instance), served as application/problem+json
- D) An annotation placed on exception classes to map them to a status code and a reason phrase

**27. An endpoint produces JSON only (no XML converter is on the classpath) and a client sends `Accept: application/xml`. With default Spring MVC behaviour and no custom exception handlers, what status is returned?**

- A) 415 Unsupported Media Type, because the requested type is unsupported
- B) 400 Bad Request, because the header value is malformed for this endpoint
- C) 406 Not Acceptable, because no representation matches the Accept header
- D) 200 OK with JSON, because the Accept header is only a hint that is ignored

**28. A controller method is declared `getAll(Pageable pageable)`. For `GET /products?page=0&size=10&sort=name,desc`, where do the Pageable values come from?**

- A) From X-Page and X-Size request headers, because query parameters are reserved for filters
- B) From query parameters, where page is one-based so page=1 is the first page by default
- C) From a JSON request body, so the method needs @RequestBody Pageable to bind them
- D) From the page (zero-based), size and sort query parameters, via Spring Data web support

**29. All CRUD controllers in this repo are mounted under /api/v1/. What is the main trade-off of this URI-path versioning strategy?**

- A) Clients cannot test it in a browser because the version must be sent in a custom request header
- B) It requires Accept-header parsing on every request, so shared caches cannot store the responses
- C) It cannot be done with @RequestMapping, so a servlet filter must rewrite the URLs
- D) The version is explicit and easy to route, but a new version means new URLs for clients

**30. A client times out and retries the same request. Comparing PUT /api/v1/products/42 (full body) with POST /api/v1/products, which statement is correct?**

- A) Both are safe to retry because HTTP requires servers to deduplicate repeated requests
- B) PUT is idempotent, so repeats leave the same state; POST is not, so a retry may duplicate
- C) POST is idempotent and PUT is not, because PUT overwrites data that the first call already changed
- D) Neither is idempotent, because only safe methods such as GET can be idempotent

## Spring Data JPA Basics (Q31-Q40)

**31. Spring Data JPA sees this method on CartRepository. What does it do with it?**

```java
Optional<Cart> findByUserIdAndProductId(
    Integer userId, Integer productId);
```

- A) It fails at startup unless a @Query is added, because Spring Data cannot infer SQL from names
- B) It runs two separate queries, one per property, and returns the first non-empty result
- C) It parses the name into a query with user.id = ?1 AND product.id = ?2 and returns an Optional
- D) It matches the columns user_id and product_id literally, so property names must equal column names

**32. CartRepository extends JpaRepository<Cart, Integer> and overrides `List<Cart> findAll()`. What does JpaRepository offer beyond CrudRepository?**

- A) JPA-specific operations such as flush() and batch deletes, plus List-returning finders and paging/sorting
- B) Support for non-relational stores, since CrudRepository is tied to JPA and JpaRepository is store-agnostic
- C) Automatic read-only transactions on every method, which CrudRepository does not provide
- D) Derived query methods such as findByUserId, which CrudRepository does not support

**33. A method annotated with @Transactional throws a checked exception (for example IOException). With default settings, what happens to the transaction?**

- A) It rolls back, because any Exception thrown out of the method triggers a rollback
- B) It rolls back only if the exception is a SQLException or a PersistenceException
- C) It rolls back only when the propagation is REQUIRES_NEW
- D) It commits, because default rollback covers only unchecked exceptions and Errors

**34. What does this method do to the database?**

```java
@Transactional
public void rename(Integer id) {
    Product p = repo.findById(id).orElseThrow();
    p.setName("New");
}
```

- A) Nothing, because no save() call is made, so the change stays in memory only
- B) Hibernate detects the change and issues an UPDATE at flush or commit
- C) An UPDATE is issued immediately inside setName(), before the method returns
- D) It throws an exception, because a loaded entity is read-only until save() is called

**35. ProductController.update calls `updatedProduct.setId(id)` and then `productService.save(updatedProduct)`, which delegates to JpaRepository.save. What does Spring Data JPA do?**

- A) It calls merge, because a non-null id means the entity is not new (null would mean persist)
- B) It calls persist, because save() always persists and simply re-attaches a detached entity to the session
- C) It calls merge, because save() always merges and never uses persist
- D) It runs a single INSERT ... ON CONFLICT UPDATE statement as an upsert

**36. Cart uses `@GeneratedValue(strategy = GenerationType.IDENTITY)`. What is a notable consequence compared with SEQUENCE?**

- A) The database assigns the id on INSERT, so Hibernate must insert immediately, which prevents insert batching
- B) IDENTITY reads ids from a separate hibernate_sequence table, while SEQUENCE relies on AUTO_INCREMENT columns
- C) SEQUENCE is unsupported on PostgreSQL, so IDENTITY is the only strategy that works there
- D) IDENTITY assigns ids in memory before the INSERT, so batching works, while SEQUENCE needs a round trip per row

**37. Given `@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true) List<OrderItem> items`, what does orphanRemoval = true add?**

- A) It deletes the parent Order whenever its last OrderItem is removed from the list
- B) It is identical to CascadeType.REMOVE, so it has no effect when CascadeType.ALL is present
- C) A child removed from the items list is treated as an orphan and deleted
- D) A removed child has its order_id foreign key set to null and its row is kept

**38. How does this @Query differ from one declared with nativeQuery = true?**

```java
@Query("select p from Product p where p.price > :min")
List<Product> findExpensive(@Param("min") Double min);
```

- A) JPQL uses entity and field names and is translated to SQL; native uses table and column names
- B) JPQL runs in the JVM over already-loaded entities, whereas native queries are sent to the database
- C) JPQL cannot use named parameters such as :min, so only native queries can bind them by name
- D) JPQL is limited to SELECT, so any bulk UPDATE or DELETE has to be a native query

**39. application.properties sets `spring.jpa.hibernate.ddl-auto=update`. Why is that risky in production?**

- A) It drops and recreates every table on each application startup, which wipes the existing data
- B) It only adds missing tables and columns, migrates no data, and applies unreviewed changes
- C) It only validates the schema against the entities and aborts startup on any mismatch it finds
- D) It creates the schema from the entities and drops it again when the application shuts down

**40. A repository method returns `Slice<Product>` instead of `Page<Product>`. What is the difference?**

- A) A Slice loads the whole result set and pages it in memory, while a Page uses LIMIT and OFFSET
- B) A Slice ignores the Sort in the Pageable, while a Page applies it to the query
- C) A Slice is only allowed on native queries, while a Page works with derived queries and JPQL
- D) A Slice skips the count query, so it only knows whether a next slice exists

# Part 2 - Expert

## JPA & Hibernate Deep Dive (Q41-Q54)

**41. `Product` declares `@ManyToOne @JoinColumn(name = "category_id") private Category category;` with no fetch attribute. A job calls `productRepository.findAll()` on 500 products that belong to 8 categories. What does Hibernate 6 actually do?**

- A) One SELECT with an outer join to category, because EAGER means Hibernate adds the join to every query that loads a Product, JPQL included
- B) One SELECT for products only; `category` stays an uninitialized proxy until some code calls a getter on it, as with any lazy association
- C) One SELECT for products plus 500 follow-up SELECTs, one per product row, because EAGER is resolved row by row and never de-duplicated
- D) One SELECT for products, then a secondary SELECT per distinct category not yet loaded (about 8); JPQL skips the join `em.find` would use

**42. A screen lists 100 `Order` rows and touches the lazy `@OneToMany items` of each, producing 101 queries. The repository method is a derived query you may not change; you can only change the entity mapping. Which change cuts the query count the most?**

- A) Change `items` to `FetchType.EAGER`, so Hibernate loads the items together with the orders in every query
- B) Mark the service method `@Transactional(readOnly = true)`, so the session reuses one prepared statement for all collections
- C) Add `@BatchSize(size = 20)` on `items`, so lazy collections load via `IN (...)` lists, about 1 + 5 queries
- D) Set `hibernate.jdbc.batch_size=20`, so the lazy loads are grouped and sent to the database as a single JDBC batch

**43. With `spring.jpa.open-in-view=false`, a new endpoint returns an `Order` entity and Jackson fails with `LazyInitializationException` on `items`. The service is under heavy load. Which fix is best?**

- A) Set `spring.jpa.open-in-view=true`; the session then stays open through serialization and lazy loading works safely everywhere
- B) Change `items` to `FetchType.EAGER`, so every load of an `Order`, in every code path, always brings the items along
- C) Annotate the controller method with `@Transactional` so the session stays open while the response is being written
- D) Fetch what the response needs in the service (JOIN FETCH, `@EntityGraph`, or a DTO projection) before returning

**44. `findByUserId` is temporarily declared WITHOUT `@EntityGraph`, `open-in-view` is false and the caller has no transaction, so `Cart.product` is an uninitialized lazy proxy. After `Cart c = cartRepository.findByUserId(1).get(0);` which line throws `LazyInitializationException`?**

- A) `Product p = c.getProduct();`
- B) `String n = c.getProduct().getName();`
- C) `Integer id = c.getProduct().getId();`
- D) `boolean has = c.getProduct() != null;`

**45. `Order` has `List<OrderItem> items` and `List<Payment> payments`, both lazy `@OneToMany`. The query `select o from Order o join fetch o.items join fetch o.payments where o.id = :id` throws `MultipleBagFetchException`. An order has 20 items and 10 payments. Which fix avoids the exception AND the row explosion?**

- A) Change both collections from `List` to `Set`; the query then runs as one join and returns one row per order
- B) Add `distinct` to the query; Hibernate 6 then de-duplicates the bag rows before it runs the multiple-bag check
- C) Fetch one collection per query: `join fetch o.items`, then `join fetch o.payments` for the same ids, in one session
- D) Keep both join fetches and add `@OrderBy` on each list, so Hibernate treats them as ordered lists rather than bags

**46. Suppose `OrderService.placeOrder` (`@Transactional`, default REQUIRED) calls `auditService.record(...)` on another bean whose method is `@Transactional(propagation = REQUIRES_NEW)`, and then throws a `RuntimeException`. What happens to the audit row?**

- A) It is rolled back too: REQUIRES_NEW joins the caller's transaction whenever one is already active on the thread
- B) It stays committed: the outer transaction is suspended and the audit runs on its own transaction and connection
- C) It is rolled back too: the inner commit is deferred and only applied when the outer transaction commits
- D) Spring throws `IllegalTransactionStateException`, because two transactions cannot be active in one thread at once

**47. PostgreSQL. Rows A and B in `oncall(doctor, on_duty)` are both on duty; the rule is that at least one must stay on duty.**

```
Tx1: SELECT count(*) FROM oncall WHERE on_duty;  -- 2
Tx2: SELECT count(*) FROM oncall WHERE on_duty;  -- 2
Tx1: UPDATE oncall SET on_duty=false WHERE doctor='A'; COMMIT;
Tx2: UPDATE oncall SET on_duty=false WHERE doctor='B'; COMMIT;
```

**Both commit under REPEATABLE READ. What is this anomaly, and what prevents it?**

- A) Write skew: the writes hit different rows so no conflict is seen; SERIALIZABLE would abort one transaction (retry needed)
- B) Lost update: the second UPDATE overwrites the first; PostgreSQL's REPEATABLE READ prevents it by aborting the second writer, so this trace cannot commit
- C) Dirty read: Tx2 saw Tx1's uncommitted change; READ COMMITTED prevents it, so moving down from REPEATABLE READ would fix the trace
- D) Non-repeatable read: REPEATABLE READ is the level that prevents it, and the session is already on it, so the rule cannot be violated

**48. `Product` has `@Version private Long version;`. Two admins load the same product at version 5 and each saves a different stock value. What is the outcome?**

- A) The second save blocks on a row lock until the first one commits, then overwrites it, because the version column is only advisory metadata
- B) Both saves succeed; the version column is incremented twice and the later write silently wins, so no update is ever lost
- C) Both saves fail, because Hibernate sees the same version in two transactions and rejects concurrent writers by default
- D) The first `UPDATE ... WHERE id=? AND version=5` bumps it to 6; the second matches 0 rows and throws an optimistic-locking exception

**49. A flash sale decrements `Product.stock` for one hot product from about 500 concurrent requests. With `@Version` most updates fail and clients retry in storms. What does switching the lookup to `@Lock(LockModeType.PESSIMISTIC_WRITE)` change?**

- A) It locks the whole `products` table for the duration, so throughput drops but no transaction ever fails or waits
- B) It emits `SELECT ... FOR UPDATE` on the row, so other writers queue until the holder's transaction ends instead of retrying
- C) It adds a version predicate to the UPDATE and retries automatically, so callers never see a locking exception
- D) It locks the row only until the query returns, after which other transactions may update it while the first still holds the entity

**50. Under READ COMMITTED, inside one `@Transactional` method:**

```
Product a = productRepository.findById(1).get();          // SELECT
// another transaction commits: UPDATE products SET price = 99 WHERE id = 1
Product b = productRepository.findByName(a.getName()).get(); // JPQL
```

**What does `b.getPrice()` return?**

- A) The new price 99, because a JPQL query always overwrites the state of managed entities with fresh row data from the database
- B) The old price: the query hits the database, but Hibernate returns the already-managed instance and ignores the fresh values
- C) The old price, because JPQL results are served from the first-level cache, so no SQL is sent for the second lookup
- D) The new price 99, because the entity is not `readOnly`, so Hibernate re-reads any managed entity that is not dirty

**51. `CartController.getCart` calls `Collectors.groupingBy(Cart::getProduct, ...)`. `Product` (Lombok `@Getter`/`@Setter` only) overrides neither `equals` nor `hashCode`. Why does the grouping work today, and when would it silently break?**

- A) Lombok's `@Getter` generates `equals`/`hashCode` from all fields, so products already compare by value; it only breaks when a field is null
- B) Hibernate's proxy classes override `equals`/`hashCode` using the identifier, so grouping is by id whatever the session or instance
- C) One query in one persistence context yields one instance per product id, so identity equals id-equality; other sessions' copies would not group
- D) `groupingBy` compares keys by `toString()`, so products group by what they print; it breaks only if `toString` is overridden

**52. A teammate makes `CartService.bumpQuantity` public and adds `@Transactional(propagation = REQUIRES_NEW)` to it. `addToCart` (unannotated, same class) still calls it as `bumpQuantity(existing.get(), quantity)`. What does the annotation do for that call?**

- A) It opens a new transaction, because the method is now public and Spring weaves the advice into the class at compile time
- B) Nothing: the call goes through `this` and bypasses the Spring proxy, so only the repository's own per-call transaction applies
- C) It opens a new transaction whenever `addToCart` is itself invoked through the proxy from a controller, since that outer call covers the inner one
- D) The application fails at startup with `BeanCreationException`, because a `@Transactional` method may not be called from its own class

**53. A service method is annotated `@Transactional(readOnly = true)` and by mistake modifies a loaded entity's field before returning. With Spring Boot 3.4 and Hibernate, which description is most accurate?**

- A) The Spring transaction interceptor throws as soon as the method changes any managed entity, since read-only transactions forbid writes
- B) The change is persisted normally: `readOnly` is documentation only and has no effect at runtime with Hibernate sessions
- C) No transaction is started at all, so no connection is acquired up front and any lazy loading inside the method fails
- D) Spring sets Hibernate's flush mode to MANUAL and marks the session read-only, so no flush runs at commit and the change is silently lost

**54. Consider the following:**

```
@Query("select o from Order o join fetch o.items")
Page<Order> findAllWithItems(Pageable pageable);
```

**Hibernate logs `HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory`. What is happening, and what is the best fix?**

- A) The LIMIT is applied to the joined rows in SQL, so a page can hold fewer orders than requested and the fix is to add `distinct` to the query
- B) Hibernate ignores the `Pageable` and returns every order in the result set; the warning only says the count query was skipped
- C) Hibernate paginates correctly through a subselect; the warning is cosmetic and can simply be suppressed in the logging config
- D) No LIMIT is sent: all rows load and are paged in Java memory (OOM risk); page the orders first, then fetch their items in a second query

## Concurrency & Data Integrity (Q55-Q60)

**55. In `CartService.addToCart`, two concurrent requests for the same (user, product) both run `findByUserIdAndProductId` and both see no row. What actually guarantees that only one cart line survives?**

- A) The existence check itself, because it runs before the insert in the same method and Spring serializes calls to each bean
- B) The `unique(user_id, product_id)` constraint: the database rejects the second insert atomically, however stale the earlier check
- C) READ COMMITTED isolation on the lookup, which makes the second request wait until the first one has committed its row
- D) `@Transactional` on `addToCart`, because select and insert then form one atomic unit that other transactions cannot interleave with

**56. `addToCart` inserts, catches `DataIntegrityViolationException`, then re-queries the existing row. On PostgreSQL, why is it deliberately NOT wrapped in a single `@Transactional`?**

- A) Postgres aborts the whole transaction after the violation, so the catch-block re-query fails ("current transaction is aborted")
- B) Postgres enforces unique constraints only under autocommit, so inside a transaction the duplicate insert would silently succeed
- C) Spring forbids catching `DataIntegrityViolationException` inside a transactional method, because the proxy rethrows it first
- D) The transaction interceptor would roll back and re-run the method automatically, hiding the race from the application code

**57. Assume `Cart` used a SEQUENCE id and `addToCart` were called inside a caller's larger `@Transactional`:**

```
try { return cartRepository.save(cart); }
catch (DataIntegrityViolationException e) { ... }
```

**When does the unique-constraint violation surface?**

- A) Always inside `save()`, because `save()` executes the INSERT immediately, so the catch block always sees the exception
- B) At the next flush (auto-flush, explicit flush or commit), so the catch may never run; `saveAndFlush` forces the INSERT inside the try
- C) Never at all: Hibernate checks unique constraints inside the persistence context and rejects duplicates before sending any SQL statement
- D) Only at commit, never earlier, so `saveAndFlush` would not change when the exception appears in the code

**58. A teammate proposes making `addToCart` `synchronized` to fix the duplicate-cart-line race. The service runs as 3 Kubernetes replicas. Which review comment is correct?**

- A) The monitor exists per JVM, so requests in different pods still interleave; only the DB constraint guards all replicas
- B) It is fine if `addToCart` is also `@Transactional`, since the JVM lock then lasts until the database transaction commits
- C) Spring's CGLIB proxies ignore `synchronized`, so the method never takes the lock, even within a single pod
- D) It works across pods, because every replica runs the same class and the monitor is tied to the class definition itself

**59. An admin raises a product from 10.00 to 12.00 after a customer added it to the cart. `placeOrder` totals with `c.getPrice() * c.getQuantity()` and copies `cart.getPrice()` into `OrderItem.price`. Which statement about this design is correct?**

- A) It is a bug: the total should read `product.getPrice()` at checkout so the shop never sells below the current price
- B) `OrderItem.price` is redundant: joining to `Product.price` at read time gives identical amounts and keeps the schema normalized
- C) The snapshot exists only to avoid a lazy-loading failure on `Product` after the session closes; it has no business meaning
- D) The customer pays 10.00 as shown; `OrderItem` keeps the price paid, so later catalogue changes cannot rewrite totals or history

**60. A client times out on `POST /orders/place`; the first request actually committed, and the mobile app retries after 5 seconds. What reliably prevents a duplicate order?**

- A) `@Transactional(isolation = SERIALIZABLE)` on `placeOrder`, so calls for the same user run one after another and the second becomes a no-op
- B) A client-sent idempotency key under a unique constraint, saved in the same transaction as the order; a retry returns the original order
- C) `@Version` on `Order`, because the retried insert would then fail its version check and be rejected
- D) A `synchronized` block around `placeOrder`, so the retry waits for the first request to finish before running

## Microservices with Spring (Q61-Q73)

**61. Checkout in a split-up system calls Inventory, Pricing and Payment one after another over synchronous HTTP, and each service has 99.9% availability. Which statement best describes the trade-off?**

- A) Checkout stays at about 99.9% because the services fail independently and a Resilience4j fallback hides any callee failure from the user.
- B) Availability drops to roughly the product (about 99.7%) and latencies add up, so steps the response does not need, like the confirmation email, belong on async events.
- C) Synchronous calls are always the more reliable choice, because the caller gets an immediate error while a published event can silently vanish.
- D) Async messaging removes the contract between services, so a producer can change an event's shape or field names without ever breaking a consumer downstream, which is why events need no versioning.

**62. A new service on Spring Boot 3.4 (Spring MVC, blocking JDBC, no WebFlux on the classpath) needs a fluent HTTP client to call other services. Which choice is most appropriate?**

- A) WebClient, because it is the only Spring HTTP client with a fluent API, and calling .block() on its result is the intended pattern for MVC apps that avoid RestTemplate.
- B) RestTemplate, because RestClient exists only for reactive applications and RestTemplate is the client Spring recommends for MVC.
- C) OpenFeign, because it ships inside Spring Framework 6.1 as the official replacement for RestTemplate.
- D) RestClient (Spring Framework 6.1): a synchronous fluent client built from Boot's auto-configured RestClient.Builder, with no WebFlux dependency needed.

**63. Resilience4j is configured as below and the breaker has just opened. What happens on the first call after 30 seconds have passed?**

```properties
slidingWindowSize=10
minimumNumberOfCalls=10
failureRateThreshold=50
waitDurationInOpenState=30s
permittedNumberOfCallsInHalfOpenState=3
```

- A) It closes automatically and resumes normal traffic; a full window of 10 new calls must be recorded before it can open again.
- B) It stays open until an operator or a health check resets it manually, because the wait duration only controls how long fallbacks are returned to callers.
- C) It becomes HALF_OPEN and admits only 3 trial calls; if their failure rate is below 50% it closes, otherwise it goes back to OPEN for another 30s.
- D) It becomes HALF_OPEN and lets all traffic through while it measures a fresh window of 10 calls, then decides whether to close.

**64. The gateway, Order service and Payment service each wrap their outbound call in a retry of 3 attempts, and Payment's bank API is slow and failing. What is the main risk and the best mitigation?**

- A) Attempts multiply across layers (up to 3 x 3 x 3 = 27 bank calls per request); retry at one layer only, with exponential backoff, jitter and timeouts inside the overall deadline.
- B) Retries and circuit breakers cannot be combined in Resilience4j, so the real risk is a breaker that never opens; disable the breaker wherever @Retry is applied to a method.
- C) Retries add load only in proportion to failures, so the only real risk is duplicate writes; backoff is merely cosmetic and simply raising the per-attempt timeout is the real fix here.
- D) The risk is that every retry reopens the TCP connection, so the fix is a larger connection pool; no backoff is needed.

**65. A method returning ShippingQuote (not CompletableFuture) is annotated @Bulkhead(name = "shipping") with Resilience4j defaults plus maxConcurrentCalls=10 and maxWaitDuration=0. The carrier API stalls. What happens?**

- A) Calls move to a dedicated thread pool of 10 with an unbounded queue, so Tomcat request threads are released immediately and callers never wait on the stalled carrier.
- B) It is the semaphore bulkhead: at most 10 calls run at once on the callers' threads and the 11th fails immediately with BulkheadFullException.
- C) It behaves like a circuit breaker: after 10 failed calls it opens and rejects everything for a configured wait duration.
- D) Excess callers block until a slot frees up, because maxWaitDuration=0 means wait indefinitely.

**66. OrderService.placeOrder saves an Order (status pending) and its items in one local @Transactional. Order and Payment become separate services with separate databases, and payment fails after the order row has committed. What is the correct way to restore a consistent state?**

- A) Keep @Transactional on placeOrder and call Payment with propagation = REQUIRES_NEW, so a payment failure rolls back the order in the other database.
- B) Wrap both services in a single @Transactional across the REST call, which Spring Boot coordinates as a distributed two-phase commit automatically.
- C) Run a saga: Payment emits a failure event and Order applies a compensating local transaction, e.g. status cancelled, a semantic undo.
- D) Retry the payment call inside placeOrder indefinitely until it succeeds, so the committed order never has to be undone.

**67. After the Order row commits, the code calls rabbitTemplate.convertAndSend(...) to announce it, as OrderMessageSender does. The JVM crashes between the commit and the publish, and the event is lost. Which design closes this dual-write gap?**

- A) Write the event to an outbox table in the same local transaction as the order; a separate relay (poller or CDC such as Debezium) publishes it, at-least-once.
- B) Call the publish inside the @Transactional method before returning, because RabbitTemplate automatically joins the JDBC transaction and rolls back together with it.
- C) Publish from a @TransactionalEventListener(phase = AFTER_COMMIT), which guarantees delivery because it runs only after a successful commit.
- D) Enable publisher confirms on the RabbitTemplate; a broker acknowledgement guarantees the event is never lost even if the app dies before publishing.

**68. CartService copies product.getPrice() into Cart.price when a line is added. Suppose Catalog and Cart become separate services and Cart keeps its own read-only copy of product name and price, updated by ProductPriceChanged events. What does this event-carried state transfer give and cost?**

- A) Cart serves reads and checkout without calling Catalog synchronously, but its copy can lag, so the design must accept eventual consistency and stale prices.
- B) Cart and Catalog stay strongly consistent, because the broker applies each event atomically with Catalog's own database commit.
- C) It only works if Cart can read Catalog's products table, since events carry just a notification with the product ID.
- D) It removes the need to think about ordering and duplicates, since each event carries the full state and therefore cannot be applied twice or out of order.

**69. A team adds spring-cloud-starter-gateway to an existing Spring MVC service (spring-boot-starter-web) to turn it into an API gateway. It fails at startup, complaining that Spring MVC is incompatible. What is the underlying reason?**

- A) The gateway needs service discovery, so startup fails until a Eureka client is added to the classpath.
- B) Gateway routes are implemented as servlet filters that clash with Spring Security's filter chain; setting spring.main.web-application-type=servlet resolves the conflict.
- C) The classic starter is deprecated on Boot 3.x in favour of MVC controllers with RestClient, so it refuses to start.
- D) The classic Spring Cloud Gateway is built on WebFlux and Reactor Netty, so it must run as its own reactive application, not alongside spring-boot-starter-web.

**70. OrderMessageListener consumes from RabbitMQ. With no extra configuration, an exception thrown while handling one message causes Spring AMQP to reject and requeue it, so it is redelivered in a tight loop. The broker can also redeliver after a consumer crash. What is the right production design?**

- A) Assume at-least-once delivery: dedupe on a message ID so the handler is idempotent, cap retries, then reject without requeue so a dead-letter exchange collects poison messages.
- B) Durable queues plus publisher confirms give exactly-once processing, so no dedupe is needed, and failed messages are simply dropped by the container after three failed attempts, so no dead-letter setup is required.
- C) Switch the acknowledge mode to none so the broker never redelivers, which removes both duplicates and the need for a dead-letter queue.
- D) Make the listener @Transactional against the database; a rollback also cancels the broker delivery, so the message is never redelivered.

**71. After moving to Spring Boot 3, a service has micrometer-tracing-bridge-brave and a Zipkin reporter, yet traces break at one hop: the downstream service always starts a new trace ID. The caller creates its client with `new RestTemplate()`. Why?**

- A) Boot 3 removed Sleuth, so propagation only works if spring-cloud-starter-sleuth is kept on the classpath in compatibility mode.
- B) Boot instruments its auto-configured builders (RestTemplateBuilder, RestClient.Builder, WebClient.Builder); a hand-created RestTemplate sends no propagation headers.
- C) Trace IDs live in a ThreadLocal and cross HTTP calls automatically with any client, so the downstream controller is probably just missing a @NewSpan annotation.
- D) Zipkin regenerates the trace ID on export whenever the sampling probability is below 1.0, so IDs always differ between services.

**72. Order's tests stub the Payment client with a hand-written stub returning {"status":"PAID"}. The Payment team renames that field to "state". Both pipelines stay green and production breaks. Which practice would have caught it before deployment?**

- A) Consumer-driven contract tests (Pact or Spring Cloud Contract): Payment's build verifies its real responses against Order's published expectations and fails on the rename.
- B) Raising line coverage of Order's unit tests, since a well-covered stub will eventually reflect Payment's real behaviour.
- C) Publishing Payment's OpenAPI document, because a generated spec automatically fails every consumer's build whenever the provider changes it, without any further tooling.
- D) A full end-to-end suite across all services in shared staging on every commit; it is the only way to verify compatibility and it scales cheaply as teams and services grow.

**73. You are splitting this monolith (Product, Category, Discount, Review, User, Address, Cart, Order, OrderItem, Payment, Shipping, Wishlist). Payment has a @ManyToOne to Order and OrderItem has one to Product. Which split best follows bounded contexts?**

- A) Split by business capability: Catalog (Product, Category, Discount, Review), Ordering (Cart, Order, OrderItem), Payment, Shipping, each with private tables and links held as IDs or snapshots.
- B) Split by technical layer: one service for controllers, one for business logic and one for persistence, all sharing the existing database for simplicity and to keep every join.
- C) Create one service per JPA entity for maximum independence, keeping every foreign key so the database still guarantees integrity.
- D) Keep one shared database and split only the controllers into separate deployables, because the foreign keys between Order, Payment and Product are needed to keep the data consistent.

## Production Readiness (Q74-Q81)

**74. Postgres has max_connections=100. HikariCP's maximum-pool-size is 10 by default, but suppose the team raises it to 20 (spring.datasource.hikari.maximum-pool-size=20, open-in-view=false) and the app may autoscale to 8 pods. During a scale-out, new pods fail with 'too many clients' errors. What is the right fix?**

- A) Raise maximum-pool-size to 50 so each pod queues less; Hikari hands idle connections back automatically when the database is under pressure.
- B) Nothing needs fixing: HikariCP shares one logical pool across all pods of a Deployment, so the limit applies per application, not per instance.
- C) Budget pods x pool size against max_connections (8 x 20 = 160 > 100): shrink the per-pod pool, or add a server-side pooler such as PgBouncer.
- D) Set minimum-idle equal to maximum-pool-size, so connections are pre-opened and the database can never run out.

**75. application.properties has management.endpoints.web.exposure.include=loggers, and SecurityConfig ends with anyRequest().authenticated() (no actuator rule). A team adds httpGet probes on /actuator/health/liveness and /actuator/health/readiness to the Deployment. What happens?**

- A) The probes fail: the kubelet sends no JWT so the chain answers 401, and health is not in the exposure list; expose health and permit only those paths.
- B) The probes pass: health is exposed over HTTP by default and in-cluster kubelet requests bypass Spring Security.
- C) The probes fail only with 404 because health is not exposed; once exposed, Spring Security lets health endpoints through by default.
- D) The probes pass but always report UP, because the probe health groups exist only if management.endpoint.health.probes.enabled=true is set manually in properties.

**76. The Deployment in k8s/app has no probes and no terminationGracePeriodSeconds, and the app runs on Spring Boot 3.4 with default settings. During rolling updates users still see failed or cut-off checkout requests. Which change fixes this?**

- A) Set server.shutdown=graceful and change nothing else; Kubernetes automatically waits for the JVM to finish draining in-flight requests, however long that takes, before stopping the pod.
- B) Add readiness (and liveness) probes on the actuator health groups, and keep spring.lifecycle.timeout-per-shutdown-phase (say 20s) below terminationGracePeriodSeconds (default 30s); raise both if requests can run longer.
- C) Set spring.lifecycle.timeout-per-shutdown-phase=5m; Spring tells the kubelet its deadline, so the pod is given five minutes to finish.
- D) Add a @PreDestroy method that sleeps; @PreDestroy hooks run before the embedded web server stops, so they keep in-flight requests alive.

**77. This repo runs Spring Boot 3.4.5 on Java 17 (Dockerfile: eclipse-temurin:17-jre-jammy) with blocking JPA. A colleague proposes spring.threads.virtual.enabled=true to handle more concurrent requests. Which statement is accurate?**

- A) It needs a Java 21+ runtime; then Tomcat request handling uses virtual threads, but concurrent DB work is still capped by the Hikari pool.
- B) It works on Java 17 once --enable-preview is passed, because virtual threads were introduced as a preview feature in Java 17 and Boot detects them.
- C) Virtual threads make connection pooling unnecessary, because each virtual thread can open its own cheap database connection when it needs one.
- D) It only affects WebFlux applications; Spring MVC on Tomcat always keeps using platform threads no matter how the property is set.

**78. Suppose ProductService.findById gets @Cacheable("products") with @EnableCaching and no cache provider configured, and ProductService.save gets @CacheEvict. The app runs as 3 pods, and users see old prices for hours after a price update. What explains this?**

- A) @CacheEvict broadcasts an invalidation to every pod through Spring's cache abstraction, so the stale prices must be coming from the browser or a CDN in front.
- B) Boot falls back to a simple ConcurrentMap cache: it is local to each JVM, so eviction reaches only one pod, and it has no TTL; use Redis or a TTL-capable cache.
- C) @Cacheable entries expire after 60 seconds by default, so the pods are probably not calling updateProduct through the Spring proxy.
- D) Add @Cacheable(value = "products", ttl = 300) to make entries expire, which also keeps the pods in sync.

**79. Autoscaling reacts slowly because each pod needs about 40 seconds to start. Which statement about the available options is correct?**

- A) spring.main.lazy-initialization=true is a free win: it cuts startup time with no downside, since beans that are never used cost nothing and any wiring problem still surfaces at startup exactly as before.
- B) A GraalVM native image keeps JIT-optimized peak throughput and works unchanged with reflection-heavy libraries, so only build time is affected.
- C) Excluding auto-configuration classes has no effect on startup, because Boot evaluates those conditions lazily on the first request.
- D) Lazy initialization shifts cost to first requests and hides wiring errors until runtime; a native image starts fast with less memory but needs longer builds, hints, and usually peaks lower.

**80. The repo's Deployment runs replicas: 1. Suppose the app adds an in-memory rate limiter of 100 requests/min per client key. The team then scales to 4 pods behind a Service. How should the 100/min limit be kept?**

- A) Enable sticky sessions on the load balancer so each key always hits one pod; the in-memory counters then stay accurate through rescheduling and autoscaling events.
- B) Move the counter to shared state, e.g. a Redis token bucket (Spring Cloud Gateway's RequestRateLimiter or Bucket4j) keyed by API key, returning 429 when exceeded.
- C) Divide the limit by the pod count (25 per pod) in configuration; this stays exact when the balancer is uneven or replicas change.
- D) Raise server.tomcat.threads.max, since limiting concurrent threads is equivalent to limiting requests per key per minute.

**81. In OrderService.placeOrder a developer adds a Micrometer counter tagged with userId and orderId so support can trace individual orders. After a week the metrics backend runs out of memory. What is the right approach?**

- A) Increase retention on the metrics backend, since Micrometer aggregates tags into one time series and cost grows only with the number of orders placed.
- B) Keep the tags but sample only 10% of increments, because time-series cost depends on sample rate rather than on distinct tag values.
- C) Keep tags low-cardinality (outcome, payment method) and put userId and orderId in structured log fields and trace attributes, correlated by traceId.
- D) Put the identifier in the metric name (orders.placed.<orderId>) instead of a tag, which avoids the tag lookup cost.

## Advanced Security & Testing (Q82-Q85)

**82. A controller method carries @PreAuthorize("hasRole('ADMIN')"), but no configuration class has @EnableMethodSecurity (SecurityConfig only has @EnableWebSecurity and anyRequest().authenticated()). A logged-in customer calls it. What happens?**

- A) 403 Forbidden, because Spring Boot's security auto-configuration turns on method security whenever spring-boot-starter-security is present.
- B) An exception at startup, because an unprocessed @PreAuthorize is treated as a configuration error.
- C) The call succeeds: nothing registers the method-security interceptor, so the annotation is ignored and only the URL rules apply.
- D) 403 Forbidden, because @EnableWebSecurity already processes @PreAuthorize and @EnableMethodSecurity only adds @Secured support.

**83. UserDetailsServiceImpl builds authorities with .roles(user.getRole().name()) and the Role enum is { admin, customer }. The team then protects an endpoint with hasRole("ADMIN") (a URL rule, or @PreAuthorize once @EnableMethodSecurity is added). What does a user with role admin get, and why?**

- A) 403 Forbidden: the granted authority is ROLE_admin, but hasRole("ADMIN") looks for ROLE_ADMIN and matching is case-sensitive.
- B) Access granted: hasRole upper-cases both sides before comparing, so the authority ROLE_admin matches the expression ROLE_ADMIN without any change.
- C) 403 Forbidden: roles() adds no prefix, so the authority is plain admin and the check must be written as hasAuthority("admin") instead.
- D) 401 Unauthorized: the role mismatch makes the JwtFilter reject the token before authorization ever runs, so no 403 is produced.

**84. Tests use H2 (MODE=PostgreSQL, H2Dialect) while production runs PostgreSQL. CartService.addToCart is deliberately not @Transactional because Postgres aborts a transaction after any failed statement. Which testing approach best guards behaviour like this?**

- A) Keep H2: MODE=PostgreSQL makes it behave identically to Postgres, including transaction-abort semantics, so the tests are equivalent.
- B) Use Testcontainers, but still wire the URL through @DynamicPropertySource, because @ServiceConnection only supports MongoDB and Redis containers, not JDBC databases.
- C) Run integration tests against a real PostgreSQLContainer wired via @ServiceConnection (Boot 3.1+); H2's compatibility mode imitates syntax, not engine behaviour.
- D) Keep H2 but switch the dialect to PostgreSQLDialect; the dialect controls the engine's transaction semantics as well as the SQL generated.

**85. The repo issues HS256 JWTs valid for 12,222,200,000 ms (about 141 days), and JwtService.isTokenValid checks only the subject and expiry. In a microservice setup a customer's token leaks. What is the most accurate statement?**

- A) Rotating the user's password immediately invalidates that user's existing tokens on every service, because the signature covers the password hash as well as the claims.
- B) Any service can revoke the token individually by removing it from its own filter, since the JWT is held in that service's SecurityContext.
- C) Validating at the gateway is enough for revocation: once the gateway drops the token, no internal service accepts it, even if replayed directly to a service.
- D) Local validation checks only signature and exp, so the token works until it expires; use short-lived access tokens plus refresh tokens, and a jti denylist for instant revocation.

---

# Answer Key

## Core Spring Boot (Q1-Q12)

**1. B** - This is inversion of control: the container, not your code, creates and wires objects. JwtFilter is a @Component in a scanned package, so it becomes a singleton bean that Spring passes to any constructor asking for it (see spring-boot-basic Q2). *(repo: SecurityConfig / JwtFilter)*

**2. C** - With constructor injection the object cannot exist without its dependencies, the field can be final, and plain Mockito tests can build it directly. Field injection hides the dependency and forces reflection or a Spring context in tests (see spring-boot-basic Q4). *(repo: UserDetailsServiceImpl)*

**3. C** - @SpringBootApplication is meta-annotated with @SpringBootConfiguration (a specialization of @Configuration), @EnableAutoConfiguration, and @ComponentScan. The scan starts at the annotated class's package, com.shihab.ecommerceapi, and covers its sub-packages. *(repo: ECommerceApiApplication)*

**4. A** - Auto-configuration classes are processed after user configuration, so @ConditionalOnMissingBean can see your beans and skips its own definition. This back-off is how Boot lets you override its defaults simply by declaring a bean. *(repo: ApplicationConfiguration)*

**5. D** - A starter is a dependency descriptor with no code of its own; spring-boot-starter-web transitively brings in spring-boot-starter-tomcat and Spring MVC. Boot's web server auto-configuration then starts embedded Tomcat within the application process (see spring-boot-basic Q13). *(repo: pom.xml)*

**6. C** - Profile-specific files always override the non-specific ones for the same key. Without that override, the unresolvable placeholder makes the app fail fast at startup instead of running with a missing secret (see spring-boot-basic Q14). *(repo: application-test.properties)*

**7. D** - Boot's externalized configuration lets later sources override earlier ones: packaged application.properties is overridden by OS environment variables, which are overridden by command-line arguments. This is what lets one jar run unchanged in any environment.

**8. C** - @ConfigurationProperties gives type-safe binding of a prefix-grouped set of properties, full relaxed binding (e.g. env-var style names), and Bean Validation via @Validated. @Value is fine for a single value but supports SpEL instead and only limited relaxed binding. *(repo: JwtService)*

**9. D** - Singleton is the default bean scope: the container creates one instance and reuses it everywhere. Any mutable field in such a bean is shared across all concurrent requests, so per-request state does not belong there (see spring-boot-basic Q2). *(repo: JwtService)*

**10. D** - Bean creation is instantiate, then populate properties and injected fields, then initialization callbacks (@PostConstruct, InitializingBean). Constructor parameters are available immediately, which is another reason to prefer constructor injection. *(repo: JwtService)*

**11. C** - Stereotype annotations like @Component only apply to classes whose source you own. For third-party classes, a @Bean method inside a @Configuration class is the way to register an instance (see spring-boot-basic Q2). *(repo: ApplicationConfiguration.passwordEncoder())*

**12. C** - A constructor cycle cannot be satisfied because each object needs the other to exist first, and this fails even if spring.main.allow-circular-references is enabled. Since Boot 2.6, setter and field cycles are also rejected by default; the fix is to redesign the dependency (or, as a last resort, use @Lazy).

## Security & Testing Basics (Q13-Q18)

**13. A** - Spring Security runs an ordered list of filters inside a SecurityFilterChain, and addFilterBefore positions a custom filter relative to a known one. The JWT filter must run before AuthorizationFilter so the request is already authenticated when access rules such as anyRequest().authenticated() are evaluated. *(repo: SecurityConfig)*

**14. C** - Authentication establishes who the caller is; authorization decides what that identity may do. An authenticated caller who fails an access check gets 403 from the access-denied handler, whereas the entry point (HttpStatusEntryPoint returning 401 here) is used for callers who are not authenticated. *(repo: SecurityConfig)*

**15. A** - A signed JWT (JWS) is encoded, not encrypted: the claims can be decoded by anyone, so never put secrets in them. The server validates the signature and the expiry claim itself, which is why no server-side session is needed (stateless auth). *(repo: JwtService)*

**16. C** - BCrypt is a one-way, salted, adaptive hash: each encode() generates a fresh random salt and stores it inside the resulting string. matches() extracts that salt, re-hashes the raw password, and compares the results, so hashes are never decrypted or compared directly. *(repo: ApplicationConfiguration.passwordEncoder())*

**17. A** - A test slice builds only the beans relevant to one layer: @WebMvcTest covers controllers, advice, converters, and filters, but not @Service or @Repository beans. @DataJpaTest slices the JPA layer, and @SpringBootTest loads the full context (see spring-boot-basic Q16). *(repo: CartControllerTest)*

**18. D** - @Mock has no Spring involvement, so it suits fast unit tests with @InjectMocks. @MockBean puts the mock into the Spring test context so that MockMvc and other beans use it. Since Boot 3.4 it is deprecated in favour of Spring Framework 6.2's @MockitoBean, which does the same job but is not identical (for example, it is not supported on @Configuration classes). *(repo: CartServiceTest / CartControllerTest)*

## MVC & REST (Q19-Q30)

**19. D** - @RestController is a meta-annotation combining @Controller and @ResponseBody. Every handler's return value is serialized by an HttpMessageConverter (Jackson for JSON) instead of being treated as a view name. *(repo: ProductController)*

**20. A** - @RequestParam is required by default, so a missing parameter raises MissingServletRequestParameterException before the method body runs; the repo's GlobalExceptionHandler maps it to 400 as well. Use required = false or a defaultValue to make it optional. *(repo: CartController.getCart)*

**21. B** - The builder starts from the current request URL (/api/v1/products), appends /{id} and expands it with the saved id. ResponseEntity.created(location) sets status 201 and the Location header, telling the client where the new resource lives. *(repo: ProductController.create)*

**22. B** - noContent() sets status 204, which by definition carries no response body. The ResponseEntity<Void> return type matches that: there is nothing to serialize. *(repo: ProductController.delete)*

**23. D** - @Valid on a @RequestBody makes Spring run Bean Validation right after deserialization; on failure it throws MethodArgumentNotValidException and the handler never executes. In this repo the exception is caught by GlobalExceptionHandler and returned as a 400 ValidationErrorResponse. *(repo: ProductController.create; GlobalExceptionHandler.handleValidationException)*

**24. B** - @Valid comes from the Jakarta Bean Validation spec and has no groups attribute. @Validated is Spring's own annotation, which adds group selection (and, at class level, method-level validation).

**25. A** - @RestControllerAdvice is @ControllerAdvice plus @ResponseBody, so its @ExceptionHandler methods apply to all controllers and return serialized bodies. GlobalExceptionHandler maps EntityNotFoundException to a 404 ApiErrorResponse. *(repo: GlobalExceptionHandler.handleEntityNotFoundException)*

**26. C** - ProblemDetail is Spring's container for the RFC 9457 (formerly RFC 7807) standard error body, rendered with the application/problem+json media type. Setting spring.mvc.problemdetails.enabled=true makes Boot use it for built-in MVC exceptions.

**27. C** - Content negotiation matches the Accept header against the media types the handler can produce; with no match Spring throws HttpMediaTypeNotAcceptableException, which maps to 406. 415 is for an unsupported request Content-Type instead.

**28. D** - Spring Data's PageableHandlerMethodArgumentResolver, auto-configured by Boot when Spring Data web support is present, builds the Pageable from the page (zero-based), size and sort=property,direction query parameters.

**29. D** - Path versioning makes the version visible in every URL, which is simple to route, cache and try from a browser. The cost is that the resource URLs change per version. See rest-api Q8 for header and media-type alternatives. *(repo: All controllers under /api/v1/ (e.g. ProductController))*

**30. B** - Idempotent means repeating the call leaves the same server state; PUT replaces the resource at a known URI, so repeats converge. POST typically creates a new resource each time. Idempotent is not the same as safe (see rest-api Q2). *(repo: ProductController.update / create)*

## Spring Data JPA Basics (Q31-Q40)

**31. C** - Spring Data parses the method name at startup: Cart has no userId property, so UserId is resolved as the nested path user.id (same for product.id), joined with AND. The Optional return type yields empty when no row matches. *(repo: CartRepository.findByUserIdAndProductId)*

**32. A** - JpaRepository (which extends ListCrudRepository and ListPagingAndSortingRepository) adds flush(), saveAllAndFlush(), deleteAllInBatch() and similar, and its finders return List rather than Iterable. Derived queries work with any repository interface. *(repo: CartRepository)*

**33. D** - Spring's default rule rolls back for unchecked exceptions (RuntimeException) and Error, but not for checked ones. Use @Transactional(rollbackFor = ...) to change that.

**34. B** - Entities loaded inside a transaction are managed; at flush time Hibernate compares them with their loaded snapshot (dirty checking) and writes an UPDATE for any changes. See spring-boot Q9.

**35. A** - SimpleJpaRepository.save checks whether the entity is new (for a wrapper id type and no version, id == null). New entities go through EntityManager.persist; others go through merge, which copies the state onto a managed instance. *(repo: ProductController.update; ProductService.save)*

**36. A** - With IDENTITY the id only exists after the INSERT executes, so Hibernate cannot defer inserts and transparently disables insert batching. SEQUENCE can pre-allocate ids (pooled optimizer) and keep inserts batchable. *(repo: Cart)*

**37. C** - CascadeType.REMOVE only propagates when the parent is removed. orphanRemoval = true additionally deletes a child row once it is removed from the parent's collection (at flush), instead of just detaching it.

**38. A** - JPQL is written against the entity model (Product, p.price) and Hibernate translates it into the database's SQL dialect, which keeps it portable. A native query is passed as raw SQL using real table and column names.

**39. B** - update makes Hibernate additively patch the schema on boot: it never drops or renames columns or migrates data, and the changes are unversioned and unreviewed. Production should use Flyway or Liquibase, with ddl-auto set to validate or none. *(repo: application.properties)*

**40. D** - A Page triggers an additional count query to know the total elements and pages, which can be expensive. A Slice only knows whether another slice follows, which is enough for infinite scroll or next/previous navigation.

## JPA & Hibernate Deep Dive (Q41-Q54)

**41. D** - `@ManyToOne` defaults to EAGER. `em.find`/`findById` can join it into the same SQL, but a JPQL or derived query like `findAll()` does not, so Hibernate fills each EAGER association with secondary selects; the persistence context de-duplicates repeated ids. See jpa-fetching Q1-Q3. *(repo: Product.category)*

**42. C** - `@BatchSize` lets Hibernate initialize up to N pending lazy collections in one IN-list query, turning 100 lookups into roughly 100/20 extra queries. It is a mapping-level mitigation; JOIN FETCH or `@EntityGraph` remain the exact fix when you control the query (jpa-fetching Q4).

**43. D** - Deciding the fetch plan per use case, inside the service/repository, keeps the connection short-lived and makes N+1 visible instead of hiding it. Keeping the session open across the web layer (OSIV or a controller transaction) holds a pooled connection during serialization. See jpa-fetching Q6. *(repo: application.properties open-in-view=false; CartRepository @EntityGraph)*

**44. B** - Only a call that needs state beyond the identifier forces initialization, and that needs an open Session. Holding or null-checking the proxy touches no data, and by default Hibernate answers `getId()` from the identifier the proxy already holds (unless `hibernate.jpa.compliance.proxy` is enabled). *(repo: Cart.product; CartRepository.findByUserId)*

**45. C** - Joining two collections in one SQL produces items x payments = 200 rows per order, and with bags Hibernate cannot tell duplicates from real rows, so it refuses. Loading one collection per query in the same persistence context keeps each result at 20 and 10 rows and attaches both to the same managed `Order`. `Set` silences the exception but keeps the Cartesian product.

**46. B** - REQUIRES_NEW suspends the current transaction and starts a new physical one, which commits or rolls back independently of the outer one. The cost is a second pooled connection held at the same time, and it only takes effect when the call crosses a Spring proxy (see Q52). *(repo: OrderService.placeOrder)*

**47. A** - Under PostgreSQL's snapshot-based REPEATABLE READ each transaction sees its own snapshot and the two UPDATEs hit different rows, so both commit and violate the invariant. Only SERIALIZABLE detects the read/write dependency and raises a serialization failure (SQLSTATE 40001), so the application must retry. See relational-db Q10.

**48. D** - @Version adds the expected version to the UPDATE's WHERE clause; a stale writer updates zero rows, Hibernate throws an optimistic-locking failure (Spring translates it to `ObjectOptimisticLockingFailureException`), and no DB lock is held while the user is thinking. The caller must reload and retry or report a conflict.

**49. B** - PESSIMISTIC_WRITE takes a row lock (`SELECT ... FOR UPDATE`) that is held until commit or rollback, serializing writers on the hot row instead of letting them fail and retry. Keep the transaction short, lock rows in a consistent order to avoid deadlocks, and expect lock-wait timeouts under load. *(repo: Product.stock)*

**50. B** - The persistence context guarantees one managed instance per id. A query still runs SQL, but each returned row is resolved to the instance already in the context, whose loaded state is not overwritten. Use `em.refresh(a)` or `em.clear()` to see newer data.

**51. C** - Without an override, `Object` identity is used. Within one persistence context Hibernate returns a single instance per row id, which makes identity equal id-equality; detached copies, instances from another session, or a proxy versus a real instance would not be equal. A safe entity `equals` must be proxy-aware and based on a stable key. *(repo: CartController.getCart; Product)*

**52. B** - Spring's default proxy-based AOP only intercepts calls that enter through the proxy; a call on `this` from inside the same bean runs the raw method. Move the method to another bean, inject a self-reference, or use AspectJ weaving. *(repo: CartService.bumpQuantity)*

**53. D** - In Spring's `HibernateJpaDialect` a read-only transaction sets `FlushMode.MANUAL`, makes the session default read-only and passes a read-only hint to the JDBC connection. That skips flush-time dirty checking (a real saving on query-only paths) but does not make an accidental write fail loudly.

**54. D** - A collection join multiplies rows per parent, so the database cannot limit by parent entity; Hibernate fetches everything and paginates in memory. Two-step loading (page ids or parents, then `join fetch ... where o.id in :ids`) or `@BatchSize` keeps the page bounded in SQL.

## Concurrency & Data Integrity (Q55-Q60)

**55. B** - A SELECT that finds nothing takes no lock on the missing row, so any application-level check-then-act can interleave, even inside a transaction. A unique index makes the duplicate check and the insert atomic inside the database; the pre-check only avoids the exception on the common path. *(repo: Cart @Table uniqueConstraints; CartService.addToCart)*

**56. A** - On PostgreSQL any failed statement aborts the transaction: further commands are rejected until rollback, and Spring/JPA also mark it rollback-only. Without an outer transaction each repository call commits or fails on its own, so the catch-block fallback query can still run. See jpa-fetching Q7. *(repo: CartService.addToCart)*

**57. B** - With a sequence id, `persist` only queues the INSERT and the SQL runs at flush time, so the exception escapes the try block later (with IDENTITY ids, as in this repo's `Cart`, the INSERT runs at persist, making `saveAndFlush` a safeguard). `saveAndFlush` forces the check to fire where the handler is. *(repo: CartService.addToCart)*

**58. A** - An intrinsic lock only coordinates threads inside one JVM, so replicas (or a restarted pod) each have their own monitor; the DB unique index is the shared source of truth. (With `@Transactional` the lock would even be released before commit.) See spring-boot-expert Q15 for the same per-instance trap. *(repo: CartService.addToCart)*

**59. D** - `Product.price` is mutable, so a value read live would change what the customer is charged and would rewrite past orders. Copying it into `Cart.price` and then `OrderItem.price` makes each line a historical fact; `getCart` still exposes the live price and a `priceChanged` flag for display. *(repo: OrderService.placeOrder; Cart.price)*

**60. B** - The server cannot tell a retry from a new order unless the client labels the request. Persisting the key with a unique constraint atomically with the order makes the retry detectable across instances; serializing calls or locking only orders them, and `@Version` guards updates of existing rows, not new inserts. *(repo: OrderService.placeOrder)*

## Microservices with Spring (Q61-Q73)

**61. B** - Serial synchronous dependencies multiply failure probability (0.999^3 is about 0.997) and add their latencies, so only steps the user is actually waiting on should stay on the request path. See microservices Q5 and Q6.

**62. D** - RestClient is the synchronous, fluent counterpart to WebClient introduced in Spring Framework 6.1, and Boot auto-configures a RestClient.Builder so observability instrumentation applies. WebClient needs the reactive stack and OpenFeign is a Spring Cloud project, not part of Spring Framework.

**63. C** - While OPEN, calls are rejected with CallNotPermittedException. After the wait duration the breaker goes HALF_OPEN and permits only permittedNumberOfCallsInHalfOpenState calls, whose failure rate decides between CLOSED and OPEN. See expert Q3.

**64. A** - Nested retries amplify load exponentially on a dependency that is already struggling (a retry storm). Keep retries at a single layer, add backoff with jitter, and keep timeouts consistent with the overall deadline. See microservices Q15 and Q16.

**65. B** - The default bulkhead type is SEMAPHORE, which caps concurrent executions on the calling thread; a wait duration of 0 means no waiting, so extra calls are rejected. The THREADPOOL type runs work on a separate pool and requires a CompletableFuture return type.

**66. C** - A local @Transactional cannot span services. In a saga every step is its own local commit and failures are reversed by compensating actions, here moving the order to the existing Order.Status.cancelled; orchestration versus choreography only decides who triggers the compensation. See expert Q7 and microservices Q19. *(repo: OrderService.placeOrder)*

**67. A** - The outbox turns two writes to two systems into one atomic local write plus a retryable relay, so a crash can delay an event but not lose it. Delivery becomes at-least-once, which is why consumers must be idempotent (see Q70). *(repo: OrderMessageSender)*

**68. A** - Events carry the state consumers need, so each service holds a local replica and has no runtime dependency on the owner, at the price of staleness. The repo's Cart.price snapshot is the same idea done manually inside one database. *(repo: CartService (price snapshot))*

**69. D** - Classic Spring Cloud Gateway is a non-blocking WebFlux/Netty proxy, which suits an edge service holding many slow connections, and it runs as a standalone application. A separate servlet-based variant (Gateway Server MVC) exists for MVC stacks.

**70. A** - RabbitMQ delivers at-least-once, so redelivery is normal and consumers must dedupe by ID. Capped retries followed by reject-without-requeue send poison messages to a dead-letter exchange instead of looping forever. See microservices Q14 and Q16. *(repo: OrderMessageListener)*

**71. B** - Micrometer Tracing and the Observation API hook in through the builders Boot auto-configures, so inject and use those to have the trace context propagated on outgoing calls. See expert Q4.

**72. A** - A consumer's own stub only encodes the consumer's assumptions. A contract is verified from both sides, so the provider's build fails when it changes a shape a consumer relies on. See microservices Q20.

**73. A** - Bounded contexts follow business capabilities and data ownership, so cross-context JPA relations become ID or snapshot references (OrderItem already stores its own price) with consistency handled by events or sagas. See microservices Q11. *(repo: Payment / OrderItem entities)*

## Production Readiness (Q74-Q81)

**74. C** - Each pod has its own pool, so total connections = pods x maximum-pool-size, and that must stay below max_connections with headroom for admin and other clients (8 x 20 = 160 > 100; the repo's default pool of 10 would give 80). A smaller pool often performs as well or better because there is less DB-side contention. Setting minimum-idle equal to maximum-pool-size changes nothing (it is already Hikari's default) and keeps more connections open. See expert Q9. *(repo: application.properties (default Hikari pool))*

**75. A** - An explicit include list replaces Boot's default web exposure, and a custom SecurityFilterChain protects /actuator/** like any other path. Expose health, permit /actuator/health/** for the kubelet, and keep downstream dependencies out of the liveness group. See expert Q2. *(repo: application.properties, SecurityConfig)*

**76. B** - Since Boot 3.4 graceful shutdown is already the default (server.shutdown=graceful with a 30s spring.lifecycle.timeout-per-shutdown-phase; earlier versions defaulted to immediate), so setting it changes nothing. Draining only works if Kubernetes cooperates: a readiness probe (readiness turns REFUSING_TRAFFIC during shutdown) stops routing to a terminating pod and holds traffic back from a starting one, and Kubernetes sends SIGKILL after terminationGracePeriodSeconds (default 30s), so the Spring phase timeout must fit inside it. See expert Q13. *(repo: k8s/app/ecommerce_deployment.yaml)*

**77. A** - Boot supports the property since 3.2, but virtual threads are a Java 21 feature. They make blocked request threads cheap, not the resources behind them, so the connection pool stays the bottleneck for JDBC. *(repo: pom.xml (java.version 17), Dockerfile)*

**78. B** - Without a provider Boot uses a simple ConcurrentMap cache with no expiry and no cross-pod visibility. @Cacheable has no TTL attribute, so expiry comes from the cache provider and multi-pod consistency needs a shared cache. See expert Q12. *(repo: ProductService)*

**79. D** - Lazy initialization only defers work, and native images trade build time, reflection/proxy hints and peak throughput for fast startup and a small footprint. Choose based on whether cold-start or steady-state performance dominates. See expert Q14.

**80. B** - Per-pod counters allow up to N times the intended rate and drift as pods scale, so the counter must be shared and keyed per client, ideally enforced at the gateway. See expert Q15. *(repo: k8s/app/ecommerce_deployment.yaml (replicas: 1))*

**81. C** - Every distinct tag combination creates a new time series, so unbounded values such as IDs explode storage. Metrics answer aggregate questions while logs and traces (linked by traceId in the MDC) answer per-request ones. See Q71. *(repo: OrderService.placeOrder)*

## Advanced Security & Testing (Q82-Q85)

**82. C** - @PreAuthorize is enforced by an interceptor registered only when @EnableMethodSecurity (Security 6, prePostEnabled true by default) is present. Without it the annotation is silently inert, so add it and test with a non-admin user. *(repo: SecurityConfig)*

**83. A** - roles(...) prepends ROLE_ to the value, giving ROLE_admin, while hasRole("ADMIN") is hasAuthority("ROLE_ADMIN") and the comparison is case-sensitive. Normalize the case when building authorities (or rename the enum) and add a test that an admin passes. *(repo: UserDetailsServiceImpl, User.Role)*

**84. C** - Testcontainers runs the real engine, and @ServiceConnection derives the datasource details from the container without manual property wiring. H2's compatibility mode and Hibernate's dialect change syntax and generated SQL, not the engine's transaction, locking and type behaviour. See expert Q10. *(repo: CartService, application-test.properties)*

**85. D** - Stateless validation has no server-side record to delete, so early revocation needs either short lifetimes (with revocable refresh tokens) or added state such as a jti denylist. Services should also validate tokens themselves rather than trusting the network, ideally with asymmetric keys (JWKS) so verifiers cannot mint tokens. See expert Q11. *(repo: JwtService, application.properties (security.jwt.expiration-time))*

---

## Answers at a glance

- Q1-10: 1-B  2-C  3-C  4-A  5-D  6-C  7-D  8-C  9-D  10-D
- Q11-20: 11-C  12-C  13-A  14-C  15-A  16-C  17-A  18-D  19-D  20-A
- Q21-30: 21-B  22-B  23-D  24-B  25-A  26-C  27-C  28-D  29-D  30-B
- Q31-40: 31-C  32-A  33-D  34-B  35-A  36-A  37-C  38-A  39-B  40-D
- Q41-50: 41-D  42-C  43-D  44-B  45-C  46-B  47-A  48-D  49-B  50-B
- Q51-60: 51-C  52-B  53-D  54-D  55-B  56-A  57-B  58-A  59-D  60-B
- Q61-70: 61-B  62-D  63-C  64-A  65-B  66-C  67-A  68-A  69-D  70-A
- Q71-80: 71-B  72-A  73-A  74-C  75-A  76-B  77-A  78-B  79-D  80-B
- Q81-85: 81-C  82-C  83-A  84-C  85-D
