# Preparation Guide: Java / PHP Backend Developer

## 1. What the role wants

- Maintain and improve existing systems (read old code, fix bugs safely, refactor in small steps).
- Read business requirement documents and build automation solutions.
- Work in Java and PHP.
- Support backend services and APIs for mobile and service-oriented projects.
- Work in an Agile team (sprints, stand-ups, code review).

## 2. Skills checklist

| Skill | Priority | What to know |
|---|---|---|
| Java | High | Collections, streams, concurrency, JVM basics, exceptions |
| Spring Boot | High | REST, JPA/Hibernate, validation, security, profiles, testing |
| Spring Cloud | Medium | Config server, discovery, gateway, Feign, circuit breaker |
| PHP | High | OOP, namespaces, Composer, PSR, PHP 8 features |
| Laravel | High | Eloquent, routes, middleware, queues, migrations, validation |
| Yii2 | Medium | Active Record, controllers, modules, behaviors, RBAC |
| MySQL / PostgreSQL | High | Joins, indexes, transactions, query tuning |
| Redis | Medium | Caching, TTL, sessions, locks, rate limiting |
| Kafka | Medium | Topics, partitions, consumer groups, offsets, retries |
| Python / Shell | Low-Medium | Small automation scripts, cron, log checks |

## 3. Study plan (2-3 weeks)

**Week 1: PHP and Laravel**
- Learn PHP 8 basics and Composer.
- Build a small Laravel REST API (login, CRUD, validation, one queue job).
- Compare with the same API in Spring Boot.

**Week 2: Yii2, Redis, Kafka**
- Build a short Yii2 app (one model, one controller, one REST endpoint).
- Add Redis caching to your API.
- Run Kafka locally with Docker. Write one producer and one consumer.

**Week 3: Database, Spring Cloud, scripting, mock interviews**
- Practice SQL and indexes on MySQL and PostgreSQL.
- Try Spring Cloud Gateway and a config server in a two-service demo.
- Write 3-4 shell scripts (log check, backup, cron job).
- Practice answering out loud.

## 4. Project idea to show in the interview

Extend this ecommerce project:
- Kafka event when an order is created.
- Redis cache for product lists.
- A small Laravel service that uses the same data (reporting or notifications).
- Docker Compose with MySQL, PostgreSQL, Redis and Kafka.

## 5. How to answer well

- Be honest about gaps: "I am strong in Spring Boot. I have been building Laravel projects to match, and I learn new frameworks quickly."
- Use real examples: bugs fixed, systems changed, speed improvements.
- Requirements: read the document, list unclear points, confirm with the business side, build, test, demo.
- Legacy code: add tests or logging first, make small changes, avoid big rewrites.

## 6. Questions to ask them

- Which systems are Java and which are PHP?
- How old is the code? Are there automated tests?
- How do you deploy (CI/CD, Docker, servers)?
- What does a normal sprint look like?

---

# Spring Interview Answers (Easy Way)

## 1. How does dependency injection work in Spring?

Normally a class creates the objects it needs. With dependency injection (DI), Spring creates the objects (called beans), keeps them in a container, and passes them into your class.

```java
@Service
public class OrderService {
    private final PaymentService paymentService;

    // Spring gives the object here (constructor injection)
    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

Why it is good: code is easier to test and change. Tip: use constructor injection (fields can be `final`).

## 2. `@Component` vs `@Service` vs `@Repository`

All three tell Spring: "make this class a bean."

| Annotation | Use for | Extra |
|---|---|---|
| `@Component` | Any general class | Base annotation |
| `@Service` | Business logic | Label only |
| `@Repository` | Database access | Converts DB errors to `DataAccessException` |

## 3. How does `@Transactional` work? What is the self-invocation trap?

`@Transactional` runs a method as one unit. If it works, Spring commits. If an exception happens, Spring rolls back.

Spring does this with a **proxy**: a hidden wrapper starts the transaction, calls your method, then commits or rolls back.

**The trap:** if a method in the same class calls another `@Transactional` method, the proxy is skipped and the annotation does nothing.

```java
@Service
public class OrderService {

    public void process() {
        saveOrder();   // Problem: skips the proxy, no transaction
    }

    @Transactional
    public void saveOrder() { ... }
}
```

Fixes:
- Put `@Transactional` on the method that is called from outside.
- Move the second method to another class (best fix).

Also remember:
- Rollback is only for unchecked exceptions by default. Use `rollbackFor = Exception.class` for checked ones.
- It works only on public methods.

## 4. What is the N+1 query problem? How do you fix it?

You load a list (1 query), then each item loads its children with a separate query. 100 orders = 1 + 100 = 101 queries.

```java
List<Order> orders = orderRepository.findAll();
for (Order o : orders) {
    o.getItems().size();   // each call runs a new query
}
```

Fixes:

1. JOIN FETCH
   ```java
   @Query("SELECT o FROM Order o JOIN FETCH o.items")
   List<Order> findAllWithItems();
   ```
2. `@EntityGraph`
   ```java
   @EntityGraph(attributePaths = "items")
   List<Order> findAll();
   ```
3. Batch fetching: `@BatchSize(size = 50)` or `hibernate.default_batch_fetch_size`.
4. DTO projections when you need only a few fields.

How to find it: check the SQL logs first (see the next section), then fix, then check again.

## 5. How do you handle errors globally (`@ControllerAdvice`)?

One class catches errors for the whole app, so controllers stay clean.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(400, "Invalid input"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        return ResponseEntity.status(500).body(new ErrorResponse(500, "Something went wrong"));
    }
}
```

Good because: clean controllers, same JSON format for every error, no stack traces shown to users. `@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`.

## 6. Main Spring Cloud parts and why use them

Spring Cloud helps when you have many small services (microservices).

| Part | What it does |
|---|---|
| Config Server | All settings in one place |
| Service Discovery (Eureka) | Services find each other by name, not IP |
| API Gateway | One entry point: routing, login check, rate limit |
| OpenFeign | Call another service like a Java interface |
| Load Balancer | Spreads requests across copies of a service |
| Circuit Breaker (Resilience4j) | Stops calling a service that is down |
| Distributed Tracing (Micrometer/Zipkin) | Follows one request across services |

Why: in microservices, services move, fail and change often. Spring Cloud handles settings, discovery, safe calls and monitoring for you.

## How to check SQL logs in Spring Boot (for N+1)

In `application.properties`:

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Cleaner option that also shows values:

```properties
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE
```

(Hibernate 5 uses `org.hibernate.type.descriptor.sql.BasicBinder=TRACE` for the second line.)

Call your endpoint and look for the same query repeating:

```
select * from orders
select * from order_items where order_id = 1
select * from order_items where order_id = 2
...
```

After the fix there should be one query with a join.

Count queries exactly:

```properties
spring.jpa.properties.hibernate.generate_statistics=true
logging.level.org.hibernate.stat=DEBUG
```

Other options: p6spy, datasource-proxy, MySQL general log, PostgreSQL `log_statement = 'all'`.

Interview answer: "I turn on SQL logging, call the endpoint, and look for a repeating query. If I see 1 + N queries, it is N+1. I fix it with JOIN FETCH or @EntityGraph, then check the logs again." Use `show-sql` only in development.

---

# Top 10 Laravel Interview Questions (Easy Answers)

## 1. What is Laravel, and why use it?

A PHP framework using MVC. It gives routing, database tools, login, queues and caching out of the box. Think of it as the PHP version of Spring Boot's idea.

## 2. What is the service container?

Laravel's DI container (like Spring's bean container). Laravel creates objects and injects them.

```php
class OrderController extends Controller
{
    public function __construct(private PaymentService $payment) {}
}
```

Bind an interface to a class in a service provider:

```php
$this->app->bind(PaymentInterface::class, StripePayment::class);
```

## 3. What is Eloquent ORM?

Maps each table to a PHP model class. It uses the Active Record pattern: the model can save, update and delete itself.

```php
$user = User::find(1);
$orders = Order::where('status', 'paid')->get();
```

## 4. Relationship types in Eloquent

- `hasOne`: a user has one profile.
- `hasMany`: an order has many items.
- `belongsTo`: an item belongs to an order.
- `belongsToMany`: users and roles (pivot table).
- `hasManyThrough`: a country has many posts through users.

## 5. N+1 problem in Laravel

```php
// Bad: N+1
$orders = Order::all();
foreach ($orders as $o) { echo $o->items->count(); }

// Good: eager loading
$orders = Order::with('items')->get();
```

In development, `Model::preventLazyLoading()` catches these early.

## 6. What is middleware?

Code that runs before or after a request reaches the controller: login check, logging, rate limit, CORS. Like filters or interceptors in Spring.

```php
Route::get('/orders', [OrderController::class, 'index'])->middleware('auth');
```

## 7. Migrations and seeders

- Migrations: version control for the database, written in PHP.
- Seeders: fill the database with test or starting data.

```bash
php artisan make:migration create_orders_table
php artisan migrate
php artisan db:seed
```

## 8. How do queues and jobs work?

Queues run slow tasks in the background (email, reports).

1. Dispatch a job: `SendInvoiceEmail::dispatch($order);`
2. It goes to a queue (Redis, database, SQS).
3. A worker runs it: `php artisan queue:work`.

Failed jobs can retry, with a set number of tries and delay.

## 9. Validation and security

Validation with a Form Request:

```php
public function rules(): array
{
    return [
        'email' => 'required|email|unique:users',
        'amount' => 'required|numeric|min:1',
    ];
}
```

Built-in security:
- SQL injection: Eloquent and the query builder use prepared statements.
- XSS: Blade `{{ }}` escapes output.
- CSRF: token checked on forms (`@csrf`).
- Passwords: `Hash::make()`.
- Mass assignment: protect with `$fillable`.

## 10. Route types, API Resources and Sanctum

- `Route::get`: one route for one URL.
- `Route::resource('orders', OrderController::class)`: all CRUD routes in one line.
- `Route::apiResource(...)`: same, without the HTML form routes. Use for APIs.
- API Resources (`JsonResource`): control how a model becomes JSON.
- Sanctum: simple token login for mobile apps and single-page apps.

## Bonus questions

- What is `artisan`? The Laravel command-line tool.
- `find()` vs `first()` vs `get()`? One by ID, one record, many records.
- What are Facades? A short static-style way to reach container services, like `Cache::get()`.
- What are Service Providers? Where services are registered and set up at app start.

Tip: compare with Spring Boot when you answer. Example: "The service container is like Spring's IoC container, and middleware is like a filter."

---

# Still to prepare

- Yii2 questions
- MySQL / PostgreSQL (indexes, transactions, EXPLAIN)
- Kafka and Redis questions
- Linux shell / Python scripting
- Agile / teamwork questions
