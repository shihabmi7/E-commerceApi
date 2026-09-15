# REST API — Basic Interview Questions

Paired with the runnable example at `src/com/shihab/springboot` (the Employee
Management REST API) where useful.

## 1. What is REST, and what makes an API "RESTful"?
An architectural style for building APIs over HTTP, built around **resources** (nouns, e.g. `/employees`) manipulated with standard HTTP methods, rather than custom action-based endpoints (`/getEmployee`, `/deleteEmployee`). Key constraints: statelessness, a uniform interface (standard HTTP verbs/status codes), client-server separation, and resources identified by URIs.

`EmployeeController` follows this: `/api/employees` is the resource, and `GET`/`POST`/`PUT`/`DELETE` on that same URL express the different actions — not `/api/getEmployees`, `/api/createEmployee`, etc.

## 2. What are the main HTTP methods, and which are "safe" or "idempotent"?
- `GET` — read, safe (no side effects), idempotent.
- `POST` — create, not safe, **not** idempotent (calling it twice creates two resources).
- `PUT` — full update/replace, not safe, idempotent (calling it 5 times with the same body leaves the same end state).
- `PATCH` — partial update, not safe, not guaranteed idempotent.
- `DELETE` — remove, not safe, idempotent (deleting an already-deleted resource still ends with it gone).

`EmployeeController` maps these directly: `getAllEmployees`/`getEmployeeById` (`GET`), `createEmployee` (`POST`), `updateEmployee` (`PUT`), `deleteEmployee` (`DELETE`).

**Idempotent ≠ safe.** Idempotent means repeating the call doesn't change the result further; safe means it doesn't change anything at all. `PUT`/`DELETE` are idempotent but not safe — they do modify data, just predictably.

## 3. What's the difference between PUT and PATCH?
`PUT` replaces the entire resource — every field you don't send is expected to be cleared/defaulted. `PATCH` updates only the fields provided.

`EmployeeController.updateEmployee` is written as a `PUT`: it takes a full `EmployeeDTO` and overwrites every field on the entity. A `PATCH` version would instead accept a partial DTO (all fields optional/nullable) and only apply the ones present — often implemented with a `Map<String, Object>` body or a library like JSON Merge Patch, since Java DTOs don't naturally distinguish "field omitted" from "field set to null."

## 4. What do the common HTTP status codes mean?
- `200 OK` — success, has a body (e.g. `GET`, successful `PUT`).
- `201 Created` — resource created (`POST`), typically with a `Location` header pointing to it.
- `204 No Content` — success, no body (e.g. `DELETE`).
- `400 Bad Request` — client sent invalid data (validation failure).
- `401 Unauthorized` — not authenticated.
- `403 Forbidden` — authenticated, but not allowed to do this.
- `404 Not Found` — resource doesn't exist.
- `409 Conflict` — request conflicts with current state (e.g. duplicate unique field).
- `500 Internal Server Error` — unhandled server-side failure.

This project's `GlobalExceptionHandler` maps exactly three of these: `ResourceNotFoundException` → 404, `MethodArgumentNotValidException` → 400, anything else → 500. `EmployeeController.createEmployee` also returns 201 explicitly via `ResponseEntity.created(...)`, and `deleteEmployee` returns 204 via `ResponseEntity.status(HttpStatus.NO_CONTENT)`.

## 5. What does "statelessness" mean in REST?
Each request must contain everything the server needs to process it — the server doesn't store any client session state between requests. A request to `GET /api/employees/7` must be fully self-contained (auth token, params, etc.); the server can't rely on remembering something from a previous request by the same client.

Why it matters: statelessness is what lets you scale horizontally — any server instance behind a load balancer can handle any request, since none of them are holding onto session-specific state. It's also why REST APIs commonly use tokens (JWT, API keys) sent on every request instead of server-side sessions.

## 6. How do you handle pagination in a REST API?
Common approaches:
- **Offset-based**: `GET /api/employees?page=2&size=20` — simple, but can skip/duplicate rows if data changes between requests.
- **Cursor-based**: `GET /api/employees?after=employee_123&size=20` — more consistent under concurrent writes, common in large-scale APIs.

`EmployeeController.getAllEmployees` currently returns the full list — for a real-world version handling many rows, you'd change the return type to a `Page<EmployeeDTO>` and have `EmployeeRepository` (already a `JpaRepository`) accept a `Pageable` parameter, since Spring Data JPA supports offset-based pagination out of the box:
```java
Page<Employee> findByDepartment(Department department, Pageable pageable);
```

## 7. What is content negotiation?
The client and server agreeing on the response format via the `Accept` header (e.g. `Accept: application/json`), and the request body format via `Content-Type`. Spring's `@RestController` handles this automatically through Jackson — `EmployeeController` returns Java objects (`EmployeeDTO`), and Spring serializes them to JSON because that's the default (and the `Accept` header the client sends). Supporting XML too would just mean adding the JAXB dependency; no controller code changes.

## 8. What is API versioning, and what are the common strategies?
Changing an API's contract without breaking existing clients. Common approaches:
- **URI versioning**: `/api/v1/employees`, `/api/v2/employees` — simplest, most visible, but duplicates routes over time.
- **Header versioning**: a custom header like `X-API-Version: 2` — keeps URLs stable, less discoverable.
- **Content-type versioning**: `Accept: application/vnd.company.v2+json` — "purest" REST approach, more complex to set up.

`EmployeeController` uses `/api/employees` with no version — fine for an internal/demo API, but a public API with external consumers would need one of the above before the first breaking change ships.

## 9. What is CORS, and when do you need it?
Cross-Origin Resource Sharing — a browser security mechanism that blocks a web page on one origin (e.g. `https://myapp.com`) from calling an API on a different origin (e.g. `https://api.myapp.com`) unless the server explicitly allows it via response headers (`Access-Control-Allow-Origin`, etc.).

It only matters for browser-based clients — a mobile app or `curl` calling `EmployeeController` directly isn't affected by CORS at all, since CORS is enforced by the browser, not the server. To allow a frontend to call this API from a different origin, you'd add `@CrossOrigin(origins = "https://myapp.com")` on the controller, or configure it globally via a `WebMvcConfigurer` bean.

## 10. What's the difference between authentication and authorization?
**Authentication** — verifying *who* you are (logging in, presenting a valid token). **Authorization** — verifying *what you're allowed to do* once identified (can this user delete this employee, or only view it?).

None of this project's endpoints currently enforce either — `EmployeeController` is fully open. Adding Spring Security would typically mean: authentication via a login endpoint issuing a JWT, then each request carrying `Authorization: Bearer <token>`, and authorization enforced with something like `@PreAuthorize("hasRole('ADMIN')")` on `deleteEmployee` specifically, while `getAllEmployees` stays open to any authenticated user.

## 11. What is idempotency, and why does it matter for retries?
An idempotent operation produces the same end result no matter how many times it's repeated. This matters specifically for network retries: if a client calls `DELETE /api/employees/7`, doesn't get a response (timeout), and retries, idempotency guarantees the second call is harmless — the employee is still just deleted, not "double deleted" or erroring unexpectedly.

`POST` is the odd one out — retrying a timed-out `POST /api/employees` risks creating a duplicate employee, since `POST` isn't idempotent. This is why some APIs support an `Idempotency-Key` header on `POST` requests: the client generates a unique key per logical request, and the server recognizes a retried key and returns the original result instead of creating a second resource.

## 12. What are some REST API design best practices?
- Use plural nouns for resources: `/employees`, not `/employee` or `/getEmployees`.
- Nest resources to express relationships: `/employees/7/transactions`, not a flat `/employeeTransactions?employeeId=7`.
- Use HTTP status codes correctly (Q4) instead of always returning 200 with an error message in the body.
- Version before you need to (Q8), not after the first breaking change ships to real clients.
- Keep responses consistent — this project's `ApiError` (used by every error path) is exactly this principle: one predictable error shape, not a different one per endpoint.
- Don't leak persistence details in the API contract — the `EmployeeDTO`/`Employee` entity split (see the Spring Boot doc, Q7) exists for this reason.

## 13. What's the difference between REST and SOAP?
REST is an architectural style over HTTP, using its existing verbs/status codes, typically exchanging JSON. SOAP is a stricter messaging protocol with its own envelope format (XML only), typically requiring a WSDL contract and built-in standards for security/transactions (WS-Security, WS-AtomicTransaction). REST is generally lighter-weight and more common for public/web APIs; SOAP still shows up in enterprise/legacy systems (banking, telecom) where strict contracts and built-in transactional guarantees matter more than simplicity.

## 14. How would you handle rate limiting on a REST API?
Restrict how many requests a client can make in a given time window (e.g. 100 requests/minute per API key), typically returning `429 Too Many Requests` once exceeded, often with a `Retry-After` header. Common implementations: a token bucket or sliding window counter, stored in something fast like Redis so it works correctly across multiple server instances (a per-instance in-memory counter would under-count once you have more than one server behind a load balancer — ties back to statelessness/horizontal scaling from Q5).

## 15. How do you document a REST API?
Most commonly via the OpenAPI/Swagger specification — either written by hand (`openapi.yaml`) or generated from code annotations. In a Spring Boot project, the `springdoc-openapi` starter dependency scans `@RestController` classes like `EmployeeController` and generates an interactive Swagger UI automatically from the existing `@GetMapping`/`@PostMapping` annotations and DTO fields — no separate documentation to maintain by hand, though you can add `@Operation`/`@Schema` annotations to enrich the generated descriptions.
