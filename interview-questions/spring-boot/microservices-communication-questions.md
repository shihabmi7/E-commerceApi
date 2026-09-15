# Microservices Communication (Spring Boot) — Beginner to Mid-Level

Focused specifically on how services call each other, and how failures surface across a chain of calls. Pairs with `spring-boot-basic-questions.md` (single-service) and `spring-boot-expert-questions.md` (production-scale depth).

## 1. What is a microservice, in short?
A small, independently deployable service owning one piece of business capability (e.g. "orders," "payments," "employees") with its own database, built and released separately from every other service. Contrast with a monolith, where everything ships as one deployable unit.

## 2. Why split into microservices instead of one big app?
Independent deployment (release the payment service without redeploying everything), independent scaling (scale the busy service, not the whole app), technology freedom (each service can pick its own DB/language), and fault isolation (one service crashing doesn't necessarily take the whole system down — though as Q6 shows, it can still cause problems downstream).

## 3. Synchronous communication — calling another service and waiting for the answer
The caller sends a request and blocks until it gets a response — same mental model as calling any REST API.
```java
@FeignClient(name = "payment-service")
public interface PaymentClient {
    @GetMapping("/api/payments/{orderId}")
    PaymentDTO getPayment(@PathVariable Long orderId);
}

// in OrderService:
PaymentDTO payment = paymentClient.getPayment(orderId);   // blocks here until payment-service responds
```
Use when the caller genuinely needs the result to proceed (e.g. "check payment status before confirming the order").

## 4. Asynchronous communication — publish an event, don't wait
The caller publishes a message and moves on immediately; some other service consumes it whenever it's ready.
```java
// Order service — fires the event and returns right away
kafkaTemplate.send("order-created", new OrderCreatedEvent(orderId, customerId));

// Notification service — consumes it independently, on its own time
@KafkaListener(topics = "order-created")
public void onOrderCreated(OrderCreatedEvent event) {
    emailService.sendConfirmation(event.getCustomerId());
}
```
Use when the caller doesn't need an immediate answer, or when you want services decoupled (the order service doesn't need to know or care that a notification service exists).

## 5. Sync or async — how do you decide?
Ask: does the caller need the result *right now* to continue? If yes → synchronous (REST/Feign). If it's a "let others know, whenever" — a notification, an audit log, a side effect that doesn't block the main flow → asynchronous (messaging). Most real systems use both: synchronous for the request/response the user is waiting on, asynchronous for everything that can happen after the response is already sent.

## 6. Scenario: A calls C through B (A → B → C). If B's call to C fails, how does A find out?
This is really two different designs, and the answer depends entirely on whether **B → C** is synchronous or asynchronous.

**If A → B and B → C are both synchronous (typical case):**
```java
// Service B
@GetMapping("/api/b-endpoint")
public ResponseEntity<ResultDTO> handle() {
    try {
        ResultDTO result = serviceCClient.call();   // B calling C
        return ResponseEntity.ok(result);
    } catch (FeignException ex) {
        // C failed (down, timeout, 500, etc.) — B decides what A sees
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ResultDTO.failed("C unavailable"));
    }
}
```
A never talks to C directly — A only ever sees **B's response**. So the honest answer is: *A is notified because B's response to A reflects what happened when B called C* — either:
- B **propagates the failure** — returns an error status (502/503) or an error body to A, and A's own call to B fails/returns an error it then handles.
- B **applies a fallback** (Q7, circuit breaker) — catches C's failure, returns a degraded-but-successful response to A. In this case **A does *not* know C failed at all** — from A's point of view, the call to B just succeeded with slightly different data. This is a real design decision, not a bug: sometimes hiding the failure and degrading gracefully is exactly what you want (e.g. show cached/default data instead of erroring the whole page).

**If B → C is asynchronous** (B fires an event to C and doesn't wait), B has likely *already returned a response to A* before C even starts processing — often a `202 Accepted`, meaning "request received, still working on it." If C then fails *after* B has already responded to A, A can't find out through the original response at all — some other mechanism is needed:
- **Callback/webhook** — C (or B, once it hears back from C) calls a URL A exposed, notifying it of the outcome.
- **A polls a status endpoint** — A periodically checks `GET /api/orders/{id}/status` until it shows success or failure.
- **A subscribes to an event** — A listens on a "result" topic (e.g. `order-failed`) the same way C listens on "order-created" (Q4), correlating by a request/order ID.
- **Push channel** — WebSocket/Server-Sent Events pushing the outcome to a still-connected client.

**The one-line answer interviewers are fishing for:** in a synchronous chain, failure naturally propagates back up through each call's response/exception — A finds out because B's answer to A changes. In an asynchronous chain, nothing propagates automatically — you have to explicitly design a notification path (callback, polling, or a subscribed event) back to A, because A already got its response before the failure even happened.

## 7. What's a circuit breaker's role in this chain?
Wrapping B's call to C in `@CircuitBreaker` (see the expert doc, Q3) means repeated C failures make B stop calling C for a while and immediately return a fallback — protecting B from piling up slow/timed-out calls, and protecting A from waiting on a call that's very likely to fail anyway. This is *why* the fallback-vs-propagate choice in Q6 exists: the circuit breaker is the mechanism that decides "return degraded data" instead of "wait and then fail."

## 8. Why doesn't A just hardcode B's URL, and B just hardcode C's?
Because instances scale up/down and move around (especially in Kubernetes) — a hardcoded IP/port breaks the moment a pod restarts elsewhere. Services register with a discovery mechanism (Eureka, or Kubernetes' built-in DNS-based service discovery) under a logical name (`payment-service`), and the caller resolves that name to a live instance at call time, with load balancing across replicas handled automatically.

## 9. Where does an API Gateway fit into A → B → C?
Typically, external clients don't call A directly at all — they go through a **Gateway** (Spring Cloud Gateway, Kong), which routes to A. The Gateway is a good place to centralize cross-cutting concerns that would otherwise need repeating in every service: authentication, rate limiting (see the expert doc, Q15), and request logging. A → B → C (internal, service-to-service) calls usually bypass the gateway and talk directly (via service discovery, Q8) — the gateway is the front door, not every internal hop.

## 10. How do you debug a failure somewhere in an A → B → C chain?
This is exactly what **distributed tracing** (expert doc, Q4) is for: a single trace ID is generated for the original request and passed along on every hop (A → B → C), so you can pull up one timeline showing all three services' spans for that one request and see exactly where it broke and how long each hop took, instead of manually cross-referencing separate log files from three different services by timestamp and guessing.

## 11. Why can't microservices just share one database?
Sharing a DB across services quietly turns them back into a monolith — any service can read/write another's tables, so you can't change B's schema without checking every other service that might depend on it, and you lose independent deployability (the whole point from Q2). The standard rule is **database-per-service**: B owns its tables exclusively, and if A needs B's data, it asks B (via Q3/Q4), never queries B's database directly.

## 12. What's the difference between REST and gRPC for service-to-service calls?
REST/JSON over HTTP is what A/B/C use in this doc's examples — simple, human-readable, widely supported. **gRPC** uses Protocol Buffers (a compact binary format) over HTTP/2, and defines the contract in a `.proto` file that generates client/server code in multiple languages:
```protobuf
service PaymentService {
    rpc GetPayment (PaymentRequest) returns (PaymentResponse);
}
```
gRPC is faster (smaller payloads, HTTP/2 multiplexing) and gives you a strict, generated contract — but it's less human-debuggable than JSON and less natural for public-facing APIs. Common pattern: gRPC for internal service-to-service calls (B → C), REST for the external-facing edge (client → A).

## 13. What's the difference between Kafka and RabbitMQ, at a basic level?
Both are message brokers, but built around different models. **RabbitMQ** is a traditional message queue — a message is pushed to a queue and consumed (typically once) by a worker, good for task distribution. **Kafka** is a distributed log — messages are appended to a topic and *retained*, so multiple independent consumers can each read the same stream at their own pace (and even replay old messages), which is why Kafka is the more common choice for event-driven architectures with several services all reacting to the same event (like `order-created` in Q4).

## 14. In the async version of Q6 (B → C fails), what happens to the message that failed?
If C's consumer throws while processing a message, most brokers will retry it. But after enough failed retries, you don't want it retried forever — it gets routed to a **Dead Letter Queue (DLQ)**, a separate topic/queue holding messages that repeatedly failed processing, so they don't block the rest of the stream and someone can inspect/reprocess them later instead of losing them silently.
```java
@KafkaListener(topics = "order-created")
@RetryableTopic(attempts = "3", dltStrategy = DltStrategy.FAIL_ON_ERROR)
public void onOrderCreated(OrderCreatedEvent event) { ... }
// after 3 failed attempts, the message lands on "order-created-dlt" instead of vanishing
```

## 15. What is the retry pattern, and how does it interact with timeouts?
Automatically re-attempt a failed call a bounded number of times before giving up — useful for transient failures (a brief network blip, a downstream service mid-restart).
```java
@Retry(name = "paymentService", fallbackMethod = "fallback")
public PaymentDTO getPayment(Long orderId) { return paymentClient.getPayment(orderId); }
```
Retries only make sense paired with a sensible **timeout** — without one, a single slow call to C can hang B far longer than expected, and B retrying a request that's still technically in flight risks duplicate processing (see Q16 on idempotency). Retries are usually combined with **exponential backoff** (wait longer between each attempt) so a struggling service isn't immediately hit with a second wave of retries on top of the first failure.

## 16. Why does idempotency matter when B retries a failed call to C?
If B calls C, the request actually succeeds on C's end, but the *response* to B is lost (timeout, network blip) — B doesn't know it succeeded, and retries. If "create a payment" isn't idempotent, that retry creates a **second** payment. Making an endpoint idempotent (e.g. requiring an `Idempotency-Key` header, or designing "create payment for order X" so calling it twice for the same order X is a no-op the second time) means B's retry is always safe, regardless of whether the original call actually got through.

## 17. Why is having no timeout on a synchronous call dangerous?
If B calls C with no timeout configured and C hangs (not down, just very slow), B's thread sits there waiting indefinitely. Under load, more and more of B's request-handling threads end up stuck waiting on the same slow C — eventually B runs out of threads entirely and can't serve *any* request, including ones that have nothing to do with C. This is **thread pool exhaustion**, and it's how one slow downstream service can take down an otherwise-healthy upstream service. Fix: always set an explicit timeout on inter-service calls, so a stuck call fails fast instead of holding a thread forever.

## 18. What's the bulkhead pattern, and how does it relate to Q17?
Named after ship bulkheads (compartments that stop one flooded section from sinking the whole ship): isolate the resources (usually thread pool/connection limit) used for calling one downstream service from the resources used for everything else.
```java
@Bulkhead(name = "paymentService", type = Bulkhead.Type.THREADPOOL)
public PaymentDTO getPayment(Long orderId) { ... }
```
If C is slow and its calls exhaust *their own* dedicated thread pool, B's other functionality (calls to other services, or requests that don't touch C at all) keeps working normally — the damage from Q17 stays contained to just the C-related calls instead of spreading to all of B.

## 19. Orchestration vs. choreography — the short version
Two ways to coordinate multiple services for one business process. **Orchestration**: a central coordinator explicitly calls each service in sequence and decides what happens next (closer to how A calling B calling C works — someone is directing traffic). **Choreography**: no coordinator — each service reacts to events from the previous one and publishes its own event, and the overall flow emerges from everyone just doing their part (closer to the async event-chain style from Q4/Q14). Orchestration is easier to reason about/debug (one place shows the whole flow); choreography scales better and avoids a single coordinator becoming a bottleneck or single point of failure, at the cost of the overall flow being harder to see in one place.

## 20. What is contract testing, and why does it matter between A, B, and C?
A way to verify that B's API still matches what A *expects* it to look like, without spinning up all three services together for every test. A defines a "contract" (example request/response pairs) against B's API; that contract is checked both from A's side (does A handle this shape correctly) and B's side (does B's real API actually still produce this shape) — commonly done with a tool like **Pact**. This catches "B changed a field name and broke A" *before* it reaches a shared staging environment, which matters more as the number of services (and teams owning them) grows — nobody wants to manually re-test every A→B→C combination after every deploy.
