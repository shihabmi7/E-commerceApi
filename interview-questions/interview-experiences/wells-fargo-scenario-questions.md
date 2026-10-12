# Wells Fargo Client Interview (Mphasis) — Java Backend, 8+ Years

Scenario-based questions with short, real answers. Topics: Kafka, distributed transactions, Redis, microservices, Spring AI, Kubernetes, Java, coding.

## Kafka and Distributed Transactions

## 1. DB transaction commits, but publishing to Kafka fails. How do you handle it?
This is the **dual-write problem**. The DB and Kafka cannot share one transaction, so one can succeed while the other fails.

**Best fix: Transactional Outbox pattern.**
1. In the **same DB transaction**, save the business data **and** an `outbox` row (the event).
2. A separate relay publishes outbox rows to Kafka (a polling job, or CDC with Debezium).
3. Mark the row as sent only after Kafka acks.

```java
@Transactional
public void placeOrder(Order o) {
    orderRepo.save(o);
    outboxRepo.save(new OutboxEvent("OrderCreated", o.getId(), toJson(o)));
}

@Scheduled(fixedDelay = 1000)
public void relay() {
    for (OutboxEvent e : outboxRepo.findTop100ByStatusOrderById("NEW")) {
        kafka.send("orders", e.getAggregateId(), e.getPayload()).get(); // wait for ack
        e.setStatus("SENT");
        outboxRepo.save(e);
    }
}
```

**Key point:** the outbox is a **normal table in the same database** (e.g. `outbox_event`). The order row and the event row are saved in **one DB transaction**, so both succeed or both roll back. If Kafka is down, the rows stay `NEW` and the relay retries later. Nothing is lost.

Result: **at-least-once** delivery, so consumers must be **idempotent**.

**Weaker fallback:** commit the order, try Kafka, retry a few times, then save the event to a `failed_events` table for a job to resend. It is weaker because that is a **second, separate write**. If the app crashes after the DB commit and before that insert, the event is lost. The outbox avoids this because the event is saved in the same transaction as the order.

## 2. How do you ensure zero message loss in Kafka?
Protect all three places: producer, broker, consumer.

- **Producer:** `acks=all`, `enable.idempotence=true`, `retries` high, `delivery.timeout.ms` set.
- **Broker/topic:** `replication.factor=3`, `min.insync.replicas=2`, `unclean.leader.election.enable=false`.
- **Consumer:** `enable.auto.commit=false`. Commit the offset **after** processing succeeds. Make processing idempotent.

Add a **DLQ** for messages that keep failing. Note: this gives at-least-once. True "exactly-once" needs Kafka transactions or idempotent consumers.

## 3. What is exponential backoff? How do you implement retries?
Wait longer after each failure: 1s, 2s, 4s, 8s... Add **jitter** (random delay) so many clients do not retry at the same moment.

Formula: `delay = min(base * 2^attempt, maxDelay) + random jitter`

```java
// Spring Kafka
DefaultErrorHandler handler = new DefaultErrorHandler(
    new DeadLetterPublishingRecoverer(kafkaTemplate),
    new ExponentialBackOffWithMaxRetries(5) {{
        setInitialInterval(1000); setMultiplier(2.0); setMaxInterval(30000);
    }});
handler.addNotRetryableExceptions(IllegalArgumentException.class); // don't retry bad data

// Resilience4j / Spring Retry
@Retryable(maxAttempts = 4, backoff = @Backoff(delay = 1000, multiplier = 2, random = true))
```

Retry only **transient** errors (timeout, 503). After max retries, send to a DLQ.

## 4. 20 consumers, 8 partitions. How many process messages?
**8.** One partition is read by only one consumer in a group. The other **12 stay idle** (standby, they take over if one dies).

To scale more, increase partitions (cannot reduce later, and key ordering changes). Consumers in **different groups** each get all messages.

## Redis and Caching

## 5. How are you using Redis in your application?
Typical answer (say what you really used):
- **Cache** for product/user data (Spring `@Cacheable`) with TTL.
- **Session store** (Spring Session).
- **Rate limiting** (`INCR` + `EXPIRE`).
- **Distributed lock** (`SET key val NX PX`) and **idempotency keys**.
- Also: leaderboards (sorted sets), pub/sub, counters.

## 6. Why cache-aside?
App controls the cache: **read** cache, on miss read DB then put in cache. **Write** to DB, then evict cache.

Why: simple; only requested data gets cached; if Redis is down the app still works from the DB; works with any cache. Downside: first request is slow, and data can be briefly stale. Use TTL plus eviction on update to limit staleness.

```java
Product get(Long id) {
    Product p = redis.get("p:" + id);
    if (p == null) {
        p = db.findById(id);
        redis.set("p:" + id, p, Duration.ofMinutes(10));
    }
    return p;
}
```

## 7. 1,000 requests hit the same key right after expiry. Prevent a cache stampede?
Only **one** request should rebuild the cache.

1. **Per-key lock / single-flight:** the first request takes a lock (`SET lock:key NX PX 5000`) and loads from DB. Others wait briefly, or return stale data, then re-read cache.
2. **Spring:** `@Cacheable(sync = true)` locks per key inside one JVM. Use Redis lock for multiple instances.
3. **TTL jitter:** TTL = 10 min + random 0–60s, so keys do not expire together.
4. **Stale-while-revalidate / early refresh:** refresh in the background before expiry.
5. **Hot keys:** never expire them, refresh by a scheduler.
6. Cache **null** results briefly (stops penetration), and use a Bloom filter for invalid keys.

## Production Scenarios and Microservices

## 8. A critical production issue you handled?
Use this structure (STAR): **Situation, Impact, Diagnosis, Fix, Prevention.**

Example: *Orders API latency jumped from 200ms to 10s.* Checked dashboards: DB connection pool exhausted. Thread dump + slow query log showed a missing index and an N+1 query after a release. **Fix:** rolled back, added the index, used `JOIN FETCH`. **Prevention:** query-count test in CI, pool metrics and alerts, load test before release, post-mortem.

Tip: pick a real story. Show that you mitigate first (rollback), then find root cause, then prevent.

## 9. Payment commits, Order Service fails. How to keep consistency?
No distributed ACID, so use a **Saga** with **compensation**.

- **Choreography:** services react to events (`PaymentCompleted` → Order Service). If order fails, it publishes `OrderFailed` → Payment Service **refunds**.
- **Orchestration:** a coordinator calls steps in order and runs compensations on failure. Easier to track.

Also needed: **outbox** for reliable events, **idempotent** handlers and refunds, **retries + DLQ**, and a saga state (`PENDING → CONFIRMED / CANCELLED`). Result is **eventual consistency**. Avoid 2PC: it is slow and blocks.

## 10. When choose synchronous REST over async Kafka?
**REST (sync):** the caller needs the answer **now** to continue (price check, stock check, auth, "is card valid"). Simple, easy to debug, request/response.

**Kafka (async):** fire-and-forget work, many consumers, spikes (buffering), need decoupling and replay (email, analytics, order events).

Trade-off: sync = temporal coupling, cascading failures (needs timeouts, circuit breaker). Async = more complexity, eventual consistency. Often mix: sync for queries, async for state changes.

## 11. Circuit breaker in Spring Boot?
Use **Resilience4j**. States: **CLOSED** (normal) → **OPEN** (fail fast after too many failures) → **HALF_OPEN** (try a few calls, then close or reopen).

```yaml
resilience4j.circuitbreaker.instances.paymentService:
  slidingWindowSize: 10
  failureRateThreshold: 50
  waitDurationInOpenState: 10s
  permittedNumberOfCallsInHalfOpenState: 3
```

```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "fallback")
@TimeLimiter(name = "paymentService")
public Payment pay(Request r) { return paymentClient.pay(r); }

Payment fallback(Request r, Throwable t) { return Payment.pending(r); }
```

Combine with **timeout, retry, bulkhead**. The fallback must be meaningful (cached data, queue for later), not just an error.

## Spring AI and RAG

## 12. AI chatbot architecture?
**RAG chatbot** with Spring AI:

1. **Ingestion:** load docs → split into chunks → create **embeddings** → store in a **vector DB** (pgvector, Redis, Pinecone).
2. **Query:** user question → embed → **similarity search** → top-K chunks.
3. **Prompt:** system prompt + retrieved context + question (+ chat memory).
4. **LLM** (via `ChatClient`) generates the answer, returned with sources.
5. **Extras:** chat memory, auth, rate limit, logging, caching, guardrails.

```java
chatClient.prompt()
    .advisors(new QuestionAnswerAdvisor(vectorStore))
    .user(question)
    .call().content();
```

## 13. Prevent hallucinations with RAG?
- **Ground the answer:** prompt says "Answer **only** from the context. If not found, say you don't know."
- **Good retrieval:** proper chunk size, good embeddings, **hybrid search** (keyword + vector), **re-ranking**, similarity **threshold** (no context → no answer).
- **Citations:** return source links so users can verify.
- **Low temperature** (0–0.2).
- **Validate output:** structured output, a check step that compares the answer to the context.
- **Keep data fresh**, and **evaluate** with test questions. Human fallback for low confidence.

## Payment Processing and Kubernetes

## 14. 10 duplicate payment requests at the same time?
Use **idempotency keys**.

1. Client sends `Idempotency-Key: <uuid>` header.
2. Server does an **atomic insert** of the key (DB **unique constraint** or Redis `SET NX`).
3. Winner processes the payment and stores the response. Others get the **same stored response** (or `409`/wait).
4. Store key with status (`IN_PROGRESS`, `DONE`) and a TTL.

```sql
CREATE TABLE idempotency (key VARCHAR(64) PRIMARY KEY, status VARCHAR(20), response JSON);
-- INSERT fails for 9 of the 10 requests -> duplicate
```

Also: DB-level unique business keys, optimistic locking (`@Version`), and send the same key to the external payment gateway. The atomic step is what stops the race, not an `if exists` check.

## 15. Pod crashes or worker node goes down?
**Pod crash:** kubelet **restarts** the container (per `restartPolicy`). Repeated crashes → `CrashLoopBackOff` (growing delay). The Deployment/ReplicaSet keeps the desired replica count.

**Node down:** the node becomes `NotReady`; after the timeout (~5 min default tolerations) pods are **evicted and rescheduled** on healthy nodes. Traffic is safe only if you have **multiple replicas**, **readiness probes** (Service removes bad pods), **liveness probes**, **PodDisruptionBudget**, **anti-affinity / topology spread** across nodes, and **resource requests/limits**. StatefulSets and PVs need extra care (volume re-attach).

## 16. Sudden 10x traffic. How to scale?
- **Autoscale pods:** **HPA** on CPU or custom metrics (RPS, Kafka lag). **Cluster Autoscaler** adds nodes. KEDA for event-based scaling.
- **Absorb the spike:** CDN, **Redis cache**, **rate limiting** / throttling, **queue** (Kafka) to buffer writes.
- **Protect services:** circuit breaker, timeouts, bulkheads, load shedding, degrade non-critical features.
- **Database:** read replicas, connection pool limits, indexes, avoid N+1.
- **Prepare:** keep min replicas, fast startup, load test, monitoring and alerts.

HPA is not instant (minutes), so caching, queues and rate limiting handle the first seconds.

## Core Java and Coding

## 17. Java Streams and when they add overhead?
A Stream is a **lazy pipeline** over data: source → intermediate ops (`filter`, `map`) → terminal op (`collect`, `sum`). Declarative, no changing the source.

**Overhead appears when:**
- The data is **small** or the loop is simple and hot (lambda, object allocation, pipeline setup cost).
- **Boxing** with `Stream<Integer>`: use `IntStream`, `mapToInt`.
- **`parallelStream()`** on small data, I/O-bound or blocking tasks, shared common pool, or poorly splittable sources (`LinkedList`). Can be slower and unsafe with shared state.
- **Stateful ops** (`sorted`, `distinct`) or repeated stream creation.
- Hard to debug, and side effects inside the pipeline.

Rule: use streams for readability; use plain loops in performance-critical code. Measure with JMH.

## 18. Longest substring without repeating characters (`ABCABCBB` → 3, `"ABC"`)
**Sliding window + HashMap** (last seen index). Move `left` forward only when a repeat is inside the window.

```java
public int lengthOfLongestSubstring(String s) {
    Map<Character, Integer> last = new HashMap<>();
    int left = 0, best = 0;
    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        if (last.containsKey(c) && last.get(c) >= left) {
            left = last.get(c) + 1;      // jump past the previous duplicate
        }
        last.put(c, right);
        best = Math.max(best, right - left + 1);
    }
    return best;
}
```

Walk-through for `ABCABCBB`: window grows to `ABC` (3). At the second `A`, `left` moves to 1 → `BCA` (3). And so on. Max = **3**.

**Time: O(n)** (one pass). **Space: O(min(n, k))**, where k is the character set size (use `int[128]` for ASCII to be faster).

---

## Key Takeaway
They test **why** you chose an approach, how it **fails**, and how it **scales**: failure recovery, consistency (outbox, saga, idempotency), Kafka reliability, cache concurrency, resilience, and practical Java.
