# Java Concurrency & Multithreading — Interview Questions

24 questions on thread safety, locking, thread pools, and async patterns in Java. Grounded where possible in this repo's own code — the real race-condition bug fixed in `CartService.addToCart` (git history: `a426c6a`), and the singleton-bean scope of `JwtService`/every `@Service` in this app. Pairs with `relational-db-basics.md` (Q11 deadlock, Q1 MVCC) for the *database*-level side of concurrency, and the MCQ file's Q55-60 for the Spring/JPA-specific concurrency scenarios.

## 1. What is a race condition?
Two or more threads access shared, mutable state at the same time, and the final result depends on the unpredictable order they happen to interleave in — code written as if "check, then act" always completes as one atomic step, when it actually doesn't.

**Real bug, this repo:** `CartService.addToCart`, before the fix in commit `a426c6a`:
```java
// Thread 1 and Thread 2, running at the same time, for the same (user, product):
Optional<Cart> existing = cartRepository.findByUserIdAndProductId(userId, productId);
// BOTH threads see "empty" — neither has inserted yet
if (existing.isEmpty()) {
    cartRepository.save(new Cart(...));   // BOTH insert — two separate rows for the same product
}
```
This is the classic **check-then-act** race: the check and the act are two separate steps, and nothing stops another thread from running its own check in the gap between this thread's check and its act. See Q9 for why `synchronized` alone doesn't fix this particular bug, and `jpa-fetching-questions.md`/the MCQ file for the actual fix (a DB-level unique constraint).

## 2. Process vs. thread — what's the actual difference?
A **process** is an independent running program with its own memory space — two processes can't directly read each other's variables. A **thread** is a unit of execution *within* a process, and every thread in the same process **shares** that process's heap memory (objects, static fields) — which is exactly why race conditions (Q1) are possible: two threads can see and mutate the very same object.
```
Process (JVM instance)
 ├── Thread 1 ─┐
 ├── Thread 2 ─┼─► all share the same heap (objects, static fields)
 └── Thread 3 ─┘   each has its OWN stack (local variables, call frames)
```
Local variables are thread-safe by default (each thread's stack is private); instance/static fields are not, unless you make them so.

## 3. What is thread safety, concretely?
Code is thread-safe if it behaves correctly no matter how many threads call it concurrently, and no matter how their operations happen to interleave — without needing the caller to add any external synchronization. A method that only touches local variables is automatically thread-safe (Q2); a method that reads-then-writes a shared field is not, unless it takes explicit measures (locking, atomics, immutability) to make that read-then-write behave as one atomic unit.

## 4. `synchronized` — method vs. block, and what does it actually lock?
`synchronized` makes a block of code **mutually exclusive** — only one thread can be inside a given lock's protected region at a time; every other thread trying to enter blocks until the first one exits.
```java
public synchronized void increment() { count++; }
// equivalent to:
public void increment() {
    synchronized (this) {   // locks on "this" — the instance
        count++;
    }
}

public static synchronized void staticMethod() { ... }
// equivalent to locking on ClassName.class (the Class object) — shared across ALL instances
```
An instance method locks on `this` (so two threads calling `increment()` on the *same* object are serialized, but on two *different* objects they run in parallel, uncontended). A static method locks on the `Class` object itself — shared across every instance, a much wider lock. A `synchronized(someObject)` block lets you lock on any object explicitly — useful for locking a smaller, specific region rather than the whole method.

## 5. Why wouldn't `synchronized` have fixed the `CartService.addToCart` race condition?
Because `synchronized` only coordinates threads **inside one JVM process**. This app runs as multiple pods/replicas (see `docker-cloud-native-questions.md` Q40, and the MCQ file's own Q58 asks this exact question) — each replica is its own JVM with its own, completely separate lock. Two concurrent "add to cart" requests landing on *different* pods would each acquire their *own* uncontended lock and race exactly as before.
```
Pod 1's JVM: synchronized lock A  ─┐
                                    ├─  two DIFFERENT locks — no coordination between them at all
Pod 2's JVM: synchronized lock B  ─┘
```
This is exactly why the real fix in this repo is a **database-level unique constraint** (Q1) rather than an in-JVM lock — the database is the one thing every replica genuinely shares, so it's the only place a cross-instance guarantee can actually live. `synchronized`/`ReentrantLock` (Q8) are the right tool only when every thread that could race is guaranteed to be in the same JVM.

## 6. What does `volatile` actually guarantee?
`volatile` guarantees **visibility** — a write to a `volatile` field by one thread is immediately visible to every other thread's next read of it, instead of a thread potentially reading a stale, cached copy from its own CPU core. It does **not** guarantee atomicity.
```java
private volatile boolean shuttingDown = false;

// Thread A: shuttingDown = true;      // every other thread sees this immediately
// Thread B: while (!shuttingDown) { doWork(); }   // reliably notices the flag flip

private volatile int counter = 0;
// counter++ is STILL NOT thread-safe with volatile — it's really
// "read counter, add 1, write counter" — three steps, and two threads
// can both read the same old value before either writes back (Q1's race, just here)
```
`volatile` is the right tool for a simple flag one thread sets and others just read (like a shutdown signal). For "read-modify-write" operations like `counter++`, you need an atomic class (Q7) or a lock (Q4/Q8), not just `volatile`.

## 7. What are the atomic classes, and how do they avoid locking?
`java.util.concurrent.atomic` (`AtomicInteger`, `AtomicLong`, `AtomicReference`, ...) provide lock-free, thread-safe read-modify-write operations, built on a CPU instruction called **compare-and-swap (CAS)**.
```java
AtomicInteger counter = new AtomicInteger(0);
counter.incrementAndGet();   // atomic — no race, no explicit lock taken

// what CAS is actually doing under the hood, conceptually:
// "if the current value is still what I last read, swap it for the new value;
//  if someone else changed it in the meantime, retry the whole operation"
```
CAS is optimistic (much like `@Version` optimistic locking at the DB level, `relational-db-basics.md`) — instead of blocking other threads out (a lock), it just retries if it detects it was beaten to the write. For simple counters/flags, this is generally faster than `synchronized` under moderate contention, since no thread ever actually blocks/sleeps.

## 8. `ReentrantLock` vs. `synchronized` — when would you reach for the explicit lock?
`ReentrantLock` (`java.util.concurrent.locks`) does the same mutual-exclusion job as `synchronized`, but as an explicit object you call `.lock()`/`.unlock()` on, which unlocks a few things `synchronized` can't do:
```java
private final ReentrantLock lock = new ReentrantLock();

public void doWork() {
    if (lock.tryLock()) {          // synchronized has NO equivalent — can't "try and give up"
        try { /* critical section */ }
        finally { lock.unlock(); }  // MUST unlock manually — synchronized does this automatically
    } else {
        // couldn't get the lock immediately — do something else instead of blocking forever
    }
}
```
Concretely, reach for `ReentrantLock` when you need: `tryLock()` (attempt without blocking, or with a timeout), interruptible lock acquisition, or fairness (`new ReentrantLock(true)` — first-come-first-served rather than the JVM picking an arbitrary waiting thread). The trade-off is that you must remember to `unlock()` in a `finally` block yourself — `synchronized` releases automatically even if an exception is thrown, so for the common case it's less error-prone.

## 9. Deadlock at the thread level — the four conditions, and how it's avoided.
Two or more threads are each waiting on a lock the other one holds, so neither can ever proceed — the exact same *shape* of problem as `relational-db-basics.md` Q11's database deadlock, just between Java locks instead of database row locks. Classic four conditions (all must hold simultaneously for deadlock to occur): **mutual exclusion** (a lock can only be held by one thread), **hold and wait** (a thread holds one lock while waiting for another), **no preemption** (a lock can't be forcibly taken away), and **circular wait** (a cycle of threads each waiting on the next).
```java
// Thread 1                          // Thread 2
synchronized (lockA) {               synchronized (lockB) {
    synchronized (lockB) { ... }         synchronized (lockA) { ... }   // deadlock if both reach here
}                                     }
```
The practical fix — exactly the same one from the database side (`relational-db-basics.md` Q11): **always acquire locks in the same, consistent global order.** If both threads always locked `lockA` before `lockB`, the cycle (circular wait) becomes structurally impossible.

## 10. Livelock and thread starvation — how are they different from deadlock?
- **Deadlock** (Q9): threads are stuck, doing nothing, waiting forever.
- **Livelock**: threads are *not* stuck — they're actively running — but they keep responding to each other in a way that makes no real progress (two threads repeatedly "politely" yielding a resource to each other, forever, like two people stepping side to side trying to let each other pass in a hallway).
- **Starvation**: a thread is perpetually denied access to a resource it needs, because other threads keep getting priority ahead of it (e.g. a low-priority thread that never gets CPU time, or a non-fair lock that keeps favoring recently-arrived threads over one that's been waiting a long time).

All three are "concurrency made no progress" bugs, but the symptom differs — deadlock shows as threads permanently `BLOCKED`/`WAITING`, livelock shows as threads burning CPU with no forward progress, starvation shows as one specific thread never getting its turn while everyone else does fine.

## 11. Why is a Spring `@Service` singleton dangerous if it has a mutable field?
Every `@Service`/`@Component` bean is a **singleton** by default (`spring-boot-basic-questions.md` Q2, and the MCQ file's own Q9 asks this exact question about `JwtService`) — one shared instance, reused across every concurrent HTTP request. If that bean has a mutable instance field, *every request thread* is reading/writing the same field at once — a request-scoped-looking piece of state (something that feels like "this request's data") is actually **shared, global, racy state**.
```java
@Service
public class JwtService {
    private String secretKey;   // set once at startup via @Value — fine, effectively read-only after init

    // BAD if this existed: a mutable field set PER-REQUEST would be shared/racy across every
    // concurrent request this singleton bean is currently handling:
    // private String currentUserToken;   // request A and request B would stomp on each other's value
}
```
`JwtService.secretKey` is safe specifically because it's set once (at construction/`@Value` injection time) and never mutated afterward — effectively immutable in practice. Any field that's meant to hold *per-request* data must be a local variable inside the method (each thread gets its own stack, Q2) — never an instance field on a singleton bean.

## 12. Why use an `ExecutorService` instead of `new Thread()` per task?
Creating a native OS thread is expensive (memory for its stack, OS scheduling overhead), and nothing stops unbounded `new Thread()` calls from creating thousands of them under load, exhausting the system. An `ExecutorService` manages a **pool** of reusable threads and a work queue — submitted tasks are picked up by whichever pooled thread is free, and the pool size caps how many run truly concurrently.
```java
ExecutorService executor = Executors.newFixedThreadPool(10);
executor.submit(() -> processOrder(orderId));   // runs on one of the 10 pooled threads
executor.shutdown();   // stop accepting new tasks, let queued/running ones finish
```
This is the same underlying idea as HikariCP connection pooling (`relational-db-basics.md` Q26/Q40) applied to threads instead of DB connections — reuse a bounded, managed set of expensive resources instead of creating a fresh one per unit of work.

## 13. What are the common `ExecutorService` types, and their trade-offs?
```java
Executors.newFixedThreadPool(10);    // fixed size N threads, unbounded queue — predictable resource usage
Executors.newCachedThreadPool();     // grows as needed, reuses idle threads, shrinks after 60s idle — risk: unbounded growth under sustained load
Executors.newSingleThreadExecutor(); // exactly 1 thread — tasks run strictly sequentially, still off the caller's thread
Executors.newScheduledThreadPool(2); // supports delayed/periodic tasks (scheduleAtFixedRate, etc.)
Executors.newWorkStealingPool();     // ForkJoinPool-backed; idle threads "steal" work from busy ones' queues — good for many small, independent tasks
```
`newCachedThreadPool()` is a common production trap: with no upper bound, a burst of slow tasks can spawn an unbounded number of threads and exhaust memory — a `newFixedThreadPool` (or a manually configured `ThreadPoolExecutor` with an explicit max size and queue capacity) gives you a hard ceiling instead, the same "bound your resource usage" principle as the bulkhead pattern (`microservices-communication-questions.md` Q18).

## 14. `Future` vs. `CompletableFuture` — what does `CompletableFuture` add?
A `Future` represents a value that will be available later, but reading it (`future.get()`) **blocks** the calling thread until it's ready — and you can't chain more work onto it without blocking. `CompletableFuture` (Java 8+) lets you compose async work **without blocking** at each step.
```java
Future<PaymentDTO> future = executor.submit(() -> paymentClient.getPayment(orderId));
PaymentDTO payment = future.get();   // BLOCKS the calling thread here

CompletableFuture<PaymentDTO> cf = CompletableFuture.supplyAsync(() -> paymentClient.getPayment(orderId));
cf.thenAccept(payment -> sendReceipt(payment));   // runs when ready — the calling thread never blocked at all
```
`CompletableFuture` is what actually makes "fire off several independent calls and combine their results" practical without tying up a thread per call waiting — see Q15.

## 15. Composing `CompletableFuture`s — `thenApply`, `thenCompose`, `thenCombine`.
```java
// thenApply: transform the result (sync function, same "stage")
CompletableFuture<String> nameOnly = getUser(id).thenApply(User::getName);

// thenCompose: chain to ANOTHER async call that depends on this result (like flatMap)
CompletableFuture<PaymentDTO> payment = getOrder(orderId)
        .thenCompose(order -> getPayment(order.getPaymentId()));   // avoids CompletableFuture<CompletableFuture<...>>

// thenCombine: run two INDEPENDENT async calls in parallel, combine both results
CompletableFuture<OrderSummary> summary = getOrder(orderId)
        .thenCombine(getShipping(orderId), (order, shipping) -> new OrderSummary(order, shipping));
```
This maps directly onto the A→B→C chains in `microservices-communication-questions.md`: `thenCompose` is the async equivalent of "B's result determines what C gets called with" (a dependent, sequential chain), while `thenCombine` is "call B and C at the same time, since neither depends on the other's result" — genuinely running two downstream calls in parallel instead of one after another.

## 16. `ConcurrentHashMap` vs. `synchronizedMap` vs. plain `HashMap` — what's the difference?
Plain `HashMap` is not thread-safe at all — concurrent modification from multiple threads can corrupt its internal structure (not just "wrong values," but genuinely broken data structure, infinite loops in old JDK versions). `Collections.synchronizedMap(new HashMap<>())` wraps every single method call in one lock on the whole map — correct, but every operation (even two unrelated `get()` calls) serializes on that one lock, killing concurrency. `ConcurrentHashMap` uses fine-grained internal locking (historically per-bucket/segment) so unrelated keys can be read/written concurrently without contending with each other — far better throughput under real concurrent load, and it's the default right answer for a shared, mutable map.
```java
Map<String, Integer> counts = new ConcurrentHashMap<>();
counts.merge("productId-5", 1, Integer::sum);   // atomic read-modify-write, no external lock needed
```

## 17. `CopyOnWriteArrayList` — when is it the right (and wrong) choice?
On every **write** (add/remove), it copies the entire underlying array — so writes are expensive (O(n) copy every time), but **reads never need any lock at all** (they just read a stable, immutable snapshot array) and are never affected by concurrent writes. This makes it a good fit specifically for **read-heavy, write-rare** scenarios — e.g. a list of event listeners registered once at startup and iterated constantly afterward. It's a poor fit for anything with frequent writes (a shopping cart's line items, say) — you'd pay that full-array-copy cost on every single mutation.

## 18. What is a `BlockingQueue`, and how does it implement the producer-consumer pattern?
A queue where `take()` blocks the calling thread if the queue is empty (waiting for a producer to add something) and `put()` blocks if the queue is full (waiting for a consumer to make room) — the coordination between producers and consumers is built into the data structure itself, no manual `wait()`/`notify()` needed.
```java
BlockingQueue<Order> queue = new LinkedBlockingQueue<>(100);

// Producer thread:
queue.put(newOrder);   // blocks if queue already has 100 orders waiting

// Consumer thread:
Order order = queue.take();   // blocks until an order is available
processOrder(order);
```
This is the same conceptual shape as a message broker (Kafka/RabbitMQ, `kafka-questions.md`) — a producer and consumer decoupled by a buffer in between — just in-process and in-memory instead of across services over a network.

## 19. `CountDownLatch` vs. `CyclicBarrier` vs. `Semaphore` — what does each coordinate?
- **`CountDownLatch`**: one or more threads wait for N events to happen (counted down by other threads), then proceed — a **one-time** gate, can't be reset/reused.
```java
CountDownLatch latch = new CountDownLatch(3);
// 3 worker threads each call latch.countDown() when done
latch.await();   // main thread blocks here until all 3 have finished
```
- **`CyclicBarrier`**: N threads all wait for **each other** to reach the same point, then all proceed together — and unlike `CountDownLatch`, it's **reusable** for repeated rounds.
- **`Semaphore`**: limits how many threads can access a resource **concurrently** (a counting lock, not a strict mutual-exclusion lock — `new Semaphore(5)` lets up to 5 threads in at once, not just 1). This is the same idea as Resilience4j's semaphore-type bulkhead (`microservices-communication-questions.md` Q18), just as a raw Java primitive instead of a Spring annotation.

## 20. `ThreadLocal` — what is it, and what's the leak gotcha with thread pools?
`ThreadLocal<T>` gives each thread its own independent copy of a variable — reads/writes on one thread never see another thread's value, even though it's technically stored in one shared `ThreadLocal` object. Commonly used for per-request context that many layers need without passing it as a parameter everywhere (a trace ID, `microservices-communication-questions.md` Q10; the current authenticated user).
```java
private static final ThreadLocal<String> traceId = new ThreadLocal<>();

// at the start of request handling:
traceId.set(UUID.randomUUID().toString());
// ...deep in some other method, no parameter passing needed:
log.info("processing, trace={}", traceId.get());
```
**The leak gotcha:** in a thread *pool* (Q12), threads are reused across many different tasks/requests. If you `set()` a `ThreadLocal` value and never `remove()` it, the next unrelated task that happens to run on that same pooled thread will see the *previous* task's stale value — a subtle, hard-to-reproduce bug (and a memory leak, since the value stays referenced as long as the thread lives). Always `remove()` in a `finally` block once you're done with it.

## 21. Thread-safe lazy singleton initialization — what does double-checked locking actually fix?
A naive lazy singleton has a race: two threads can both see `instance == null` and both construct a new instance.
```java
// BROKEN under concurrency:
public static Config getInstance() {
    if (instance == null) {          // two threads can both pass this check
        instance = new Config();     // both construct — you get two "singletons"
    }
    return instance;
}
```
**Double-checked locking** fixes it by only synchronizing the (rare) first-time creation, not every single call:
```java
private static volatile Config instance;   // volatile matters — see below

public static Config getInstance() {
    if (instance == null) {                 // fast path — no lock, cheap, for every call after the first
        synchronized (Config.class) {
            if (instance == null) {          // re-check INSIDE the lock — another thread might've just built it
                instance = new Config();
            }
        }
    }
    return instance;
}
```
The `volatile` on `instance` isn't optional here — without it, another thread could observe a *partially constructed* object (a JVM reordering/visibility issue, Q22) through the outer, unsynchronized check. (In practice, the simplest fully correct alternative is often just an eagerly-initialized `static final` field, or Spring managing the singleton for you, Q11 — double-checked locking is worth understanding but rarely worth hand-rolling in real code.)

## 22. What is the Java Memory Model, and what does "happens-before" mean?
The JVM (and the underlying CPU) is allowed to **reorder** instructions and cache values in CPU-local memory for performance, as long as it doesn't change the outcome *for a single thread running alone*. The problem: those same reorderings/caching can produce surprising results when *another* thread is watching — it might observe writes out of the order they were issued in, or a stale cached value (exactly the problem `volatile`, Q6, exists to prevent for a specific field). The **Java Memory Model (JMM)** formally defines which operations establish a **happens-before** relationship — a guarantee that everything before point A in one thread is visible to anything after point B in another thread, if A happens-before B. Concrete happens-before guarantees include: unlocking a `synchronized` block happens-before a later thread locking the *same* monitor; writing a `volatile` field happens-before any later read of that same field; and a thread's actions before calling `thread.start()` happen-before anything inside that new thread. Without one of these established relationships, there's genuinely no guarantee another thread sees your writes at all — this is the deeper "why" behind Q4/Q6/Q9's rules.

## 23. Virtual threads (Java 21) — how are they different, and how does `synchronized` interact with them?
A **platform thread** maps 1:1 to a real OS thread — expensive to create, and the OS can only schedule so many. A **virtual thread** is a lightweight thread managed by the JVM itself, many of which get multiplexed onto a small pool of real **carrier** (platform) threads — you can spin up *millions* of virtual threads cheaply, since blocking one doesn't tie up an OS thread the way blocking a platform thread does (the JVM parks the virtual thread and frees the carrier thread to run something else). This is exactly what the MCQ file's own Q77 (`spring.threads.virtual.enabled`) is about: virtual threads make a **blocked request thread cheap**, but they don't make whatever's actually behind that block (a JDBC connection pool, Q12) any bigger — the connection pool stays the real bottleneck for a blocking-JPA app like this one.

**The `synchronized` gotcha:** a virtual thread that blocks inside a `synchronized` block/method historically gets **pinned** to its carrier thread instead of being unmounted — meaning that carrier thread is stuck too, defeating the "cheap blocking" benefit for exactly that section of code (JDK 24 largely fixed this for `synchronized`, but it's a real, commonly-asked gotcha for versions before that, and `ReentrantLock`, Q8, never had this problem in the first place — one more reason it's sometimes preferred in virtual-thread-heavy code).

## 24. Optimistic vs. pessimistic concurrency control — the Java-level summary.
- **Pessimistic**: assume a conflict *will* happen, so take a lock up front and block everyone else out until you're done — `synchronized`/`ReentrantLock` (Q4/Q8) at the JVM level, or `SELECT ... FOR UPDATE`/`@Lock(PESSIMISTIC_WRITE)` at the database level (MCQ file Q49). Safe, but limits concurrency — every contending thread/transaction just waits.
- **Optimistic**: assume a conflict is *rare*, proceed without locking, and only check for a conflict at the very end — the atomic classes' CAS (Q7) at the JVM level, or `@Version`-based optimistic locking at the database level (MCQ file Q48). Higher throughput when conflicts are genuinely rare, but the loser has to detect the conflict and retry (or fail) rather than never having contended at all.

The choice is the same trade-off at every level this file and `relational-db-basics.md` touch it: how often do you actually expect two things to collide? Rare collisions favor optimistic (don't pay a locking cost for something that almost never happens); frequent, high-value collisions (the flash-sale stock decrement in MCQ Q49) favor pessimistic (avoid paying for repeated failed optimistic retries under heavy contention).
