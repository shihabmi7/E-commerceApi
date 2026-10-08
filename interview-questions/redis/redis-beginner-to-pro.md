# Redis — Beginner to Pro (Interview Notes + E-Commerce Integration Plan)

This repo does **not** use Redis yet — current stack is Spring Boot 3.4 + PostgreSQL (JPA) + RabbitMQ + JWT (see `pom.xml`, `application.properties`). Part 1–3 are interview Q&A ordered from beginner to pro; **Part 4 is a concrete, step-by-step plan for introducing Redis into this e-commerce API**, pointing at the real classes it would touch (`ProductService`, `CartService`, `OrderService`, `AuthController`, `JwtFilter`).

Pairs with `spring-boot/kafka-questions.md` (Redis Streams vs Kafka, Q27) and `database/relational-db` (why the cache is never the source of truth).

---

## Part 1 — Beginner

## 1. What is Redis, in short?
**RE**mote **DI**ctionary **S**erver: an **in-memory key–value data store**. All data lives in RAM, so reads/writes take microseconds instead of the milliseconds a disk-backed DB needs. Values aren't just strings — they're typed data structures (lists, hashes, sets, sorted sets, streams…), and Redis gives you atomic commands that operate on them server-side.

Common roles: **cache**, **session store**, **rate limiter**, **distributed lock**, **leaderboard**, **pub/sub / lightweight queue**.

## 2. Why is Redis so fast?
1. **In memory** — no disk seek on the read path.
2. **Single-threaded command execution** — no locks, no context switches between commands; each command runs start-to-finish atomically. (Redis 6+ uses extra threads for network I/O only; command execution is still one thread.)
3. **I/O multiplexing** (epoll/kqueue) — one thread serves thousands of connections.
4. **Simple, optimized data structures** — e.g. small hashes/lists stored as compact `listpack`s.

Typical numbers: ~100k+ ops/sec per instance, sub-millisecond latency.

## 3. Redis vs a relational DB (Postgres) — when to use which?
| | PostgreSQL | Redis |
|---|---|---|
| Storage | Disk (with memory cache) | RAM (optional disk persistence) |
| Data model | Tables, joins, SQL | Keys → typed values |
| Durability | Strong (WAL, ACID) | Configurable, weaker by default |
| Queries | Arbitrary (WHERE, JOIN, GROUP BY) | Lookup by key; no ad-hoc queries |
| Size limit | Disk size | RAM size |
| Role in our app | **Source of truth** (orders, users, payments) | **Speed layer** (cache, sessions, counters, locks) |

Rule of thumb: **if losing it would lose money, it lives in Postgres.** Redis holds data you can rebuild or can afford to lose.

## 4. Core data types and what you'd use each for
| Type | Example commands | E-commerce use |
|---|---|---|
| **String** | `SET`, `GET`, `INCR`, `SETEX` | Cached product JSON, counters, rate-limit buckets, locks |
| **Hash** | `HSET`, `HGET`, `HINCRBY`, `HGETALL` | Cart: `cart:{userId}` → `{productId: qty}` |
| **List** | `LPUSH`, `RPOP`, `LRANGE`, `LTRIM` | "Recently viewed products" (capped list) |
| **Set** | `SADD`, `SISMEMBER`, `SINTER` | Wishlist product IDs, "users who viewed this" |
| **Sorted Set (ZSet)** | `ZADD`, `ZINCRBY`, `ZREVRANGE` | Best-sellers leaderboard, trending products |
| **Stream** | `XADD`, `XREADGROUP`, `XACK` | Event log (order events) with consumer groups |
| **Bitmap / HyperLogLog** | `SETBIT`, `PFADD`, `PFCOUNT` | Daily active users, unique product-page visitors (approx.) |
| **Geo** | `GEOADD`, `GEOSEARCH` | Nearest warehouse / pickup point |

## 5. Basic commands you should know cold
```bash
SET product:42 '{"id":42,"name":"Mouse","price":19.99}' EX 600   # set with 10-min TTL
GET product:42
TTL product:42          # seconds left (-1 = no expiry, -2 = key doesn't exist)
EXPIRE product:42 60
DEL product:42
EXISTS product:42
INCR page:views:42      # atomic counter
KEYS product:*          # NEVER in production — blocks the server (see Q20)
SCAN 0 MATCH product:* COUNT 100   # production-safe iteration
```

## 6. What is TTL and why does every cache key need one?
TTL = time-to-live; after it elapses Redis deletes the key. Without TTLs, a cache (a) grows until memory runs out, and (b) keeps serving stale data forever if an invalidation is ever missed. A TTL is the **safety net** that bounds how stale data can get even when your invalidation code has a bug.

## 7. Key naming conventions
Use colon-separated namespaces: `app:entity:id[:field]` → `ecom:product:42`, `ecom:cart:7`, `ecom:ratelimit:login:203.0.113.5`. Keeps keys readable, lets you `SCAN` by prefix, and avoids collisions when several services share a Redis.

## 8. Is Redis single-threaded? Does that mean it can't scale?
Command execution is single-threaded — which is exactly why each command is atomic. It scales by (1) being so fast one core is usually enough, (2) **read replicas**, and (3) **Redis Cluster** sharding across many nodes (Q24). The consequence for you: **one slow command (e.g. `KEYS *`, `HGETALL` on a 1M-field hash, a long Lua script) blocks every other client.**

---

## Part 2 — Intermediate

## 9. Caching patterns: cache-aside vs read-through vs write-through vs write-behind
| Pattern | Read | Write | Notes |
|---|---|---|---|
| **Cache-aside (lazy loading)** | App checks cache → on miss, reads DB and fills cache | App writes DB, then **deletes** cache key | Most common; what Spring `@Cacheable`/`@CacheEvict` does. **We'll use this.** |
| Read-through | Cache library loads from DB on miss | — | Same as cache-aside but the loader lives in the cache layer |
| Write-through | — | Write cache and DB synchronously | Cache always warm; slower writes |
| Write-behind (write-back) | — | Write cache, flush to DB asynchronously | Fast writes; risk of data loss — avoid for orders/payments |

## 10. On update, why *delete* the cache key instead of *updating* it?
Two concurrent writers can interleave: A writes DB (price=10), B writes DB (price=12), B updates cache (12), A updates cache (10) → cache says 10, DB says 12, until TTL. Deleting avoids that write-ordering race — the next reader repopulates from the DB. Always **write DB first, then delete cache** (deleting first lets a reader re-cache the old value before the DB commit).

There's still a tiny window (reader loaded old value before the commit, writes it back after the delete). Mitigations: short TTLs, **delayed double delete** (delete, then delete again ~500ms later), or evict **after transaction commit** (`@TransactionalEventListener(phase = AFTER_COMMIT)`).

## 11. Cache penetration, breakdown (stampede), and avalanche
| Problem | What happens | Fix |
|---|---|---|
| **Penetration** | Requests for IDs that don't exist (e.g. `/products/-1`, bots) always miss cache and hit DB | Cache the "null" result with short TTL; Bloom filter of valid IDs; validate input |
| **Breakdown / stampede** | A single **hot key** expires → thousands of concurrent requests all miss and hammer the DB | Mutex/lock so only one request rebuilds (`@Cacheable(sync = true)`); logical expiry + background refresh |
| **Avalanche** | **Many keys expire at the same moment** (all set with TTL=600 at deploy) or Redis goes down | Add random jitter to TTLs; replicas/HA; circuit breaker + fallback to DB |

These three come up in almost every Redis interview — know them by name.

## 12. Eviction policies — what happens when memory is full?
Set with `maxmemory` + `maxmemory-policy`:
- `noeviction` — writes fail with an error (default; right for a **primary store/queue**, wrong for a cache)
- `allkeys-lru` — evict least-recently-used key from all keys (**good default for a pure cache**)
- `volatile-lru` — LRU among keys *with a TTL* only
- `allkeys-lfu` / `volatile-lfu` — least-frequently-used (better when a few keys are persistently hot)
- `allkeys-random`, `volatile-random`, `volatile-ttl`

Redis's LRU/LFU are **approximated** by sampling (`maxmemory-samples`), not exact.

## 13. How does Redis expire keys?
Two mechanisms combined:
1. **Lazy (passive)** — on access, if the key is expired, delete it and return nil.
2. **Active** — ~10×/sec, sample random keys with TTLs and delete expired ones; repeat if >25% of the sample was expired.

So an expired key may briefly still occupy memory, but you'll never *read* it.

## 14. Persistence: RDB vs AOF
| | RDB (snapshot) | AOF (append-only file) |
|---|---|---|
| How | Forks, dumps whole dataset to `dump.rdb` periodically | Logs every write command |
| Data loss on crash | Everything since last snapshot (minutes) | ~1 second with `appendfsync everysec` |
| Restart speed | Fast | Slower (replay), mitigated by AOF rewrite |
| File size | Compact | Larger |

Production usually runs **both** (Redis 7 hybrid: AOF with an RDB preamble). A pure cache can run with persistence off.

## 15. Transactions: MULTI / EXEC / WATCH
```bash
WATCH stock:42
val = GET stock:42          # client reads
MULTI
DECRBY stock:42 1
EXEC                        # returns nil if stock:42 changed since WATCH → retry
```
- `MULTI`/`EXEC` queues commands and runs them **back-to-back without interleaving** — but there's **no rollback**: if one command fails at runtime, the others still apply.
- `WATCH` = optimistic locking (CAS). Abort if the watched key changed.
- For conditional logic, a **Lua script** is usually simpler and truly atomic (Q16).

## 16. Lua scripts — why and when?
`EVAL` runs a script **atomically** on the server: no other command runs in between. Perfect for check-then-act logic that would otherwise race:
```lua
-- KEYS[1] = stock:{productId}, ARGV[1] = qty
local stock = tonumber(redis.call('GET', KEYS[1]) or '-1')
if stock < tonumber(ARGV[1]) then return -1 end
return redis.call('DECRBY', KEYS[1], ARGV[1])
```
Keep scripts short — they block the server while running. Redis 7 adds **Functions** (`FUNCTION LOAD`) as the managed successor to ad-hoc `EVAL`.

## 17. Pipelining vs transactions
**Pipelining** batches many commands in one network round-trip (big latency win, e.g. warming 1,000 product keys), but gives **no atomicity**. `MULTI/EXEC` gives isolation. They can be combined.

## 18. Pub/Sub vs Streams vs RabbitMQ (which we already have)
| | Redis Pub/Sub | Redis Streams | RabbitMQ (in this repo) |
|---|---|---|---|
| Delivery | Fire-and-forget; offline subscribers miss messages | Persisted log, consumer groups, `XACK`, replay | Durable queues, acks, DLQ, routing |
| Use | Cache-invalidation broadcast, live notifications | Lightweight event log | Business events (order placed → email, etc.) |

For this project: **keep RabbitMQ for business events**; Redis Pub/Sub is fine for broadcasting "evict product 42" to multiple app instances holding a local (L1) cache.

## 19. How do you implement rate limiting with Redis?
**Fixed window** (simplest):
```bash
INCR ratelimit:login:{ip}:{minute}
EXPIRE ratelimit:login:{ip}:{minute} 60    # only on first INCR (== 1)
# reject if value > 5
```
**Sliding window log** — ZSet of request timestamps: `ZREMRANGEBYSCORE` old entries, `ZCARD`, `ZADD` now — in a Lua script for atomicity. **Token bucket** — store `tokens` + `last_refill` in a hash, refill in Lua. Libraries: Bucket4j (with Redis backend), Resilience4j is in-memory only (per instance).

## 20. Why is `KEYS *` dangerous? What instead?
`KEYS` is O(N) over the whole keyspace and runs on the single command thread — on 10M keys it freezes Redis for seconds and every request times out. Use `SCAN` (cursor-based, incremental). Similarly avoid `FLUSHALL`, `HGETALL`/`SMEMBERS`/`LRANGE 0 -1` on huge collections (use `HSCAN`/`SSCAN`), and `DEL` on huge keys (use `UNLINK` — frees memory in a background thread).

---

## Part 3 — Pro / Senior

## 21. Distributed locks: `SET NX PX` and its pitfalls
```bash
SET lock:order:checkout:7 <uuid> NX PX 10000   # acquire only if absent, auto-expire in 10s
```
Release **only if you still own it** (Lua, compare-and-delete):
```lua
if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end
return 0
```
Pitfalls:
- **No TTL** → a crashed holder deadlocks everyone. **TTL too short** → lock expires mid-work (GC pause) and two holders run at once. Fix: watchdog renewal (Redisson does this automatically).
- **Failover**: primary grants lock, dies before replicating, replica promoted without the lock → two holders. **Redlock** (majority across N independent masters) reduces this; Martin Kleppmann's critique: without **fencing tokens** checked by the resource, no lease-based lock is safe for correctness.
- Takeaway for interviews: **Redis locks are for efficiency (avoid duplicate work), not correctness.** For correctness (e.g. never oversell), back it with a DB constraint / optimistic locking — just like `CartService.addToCart` already relies on the `unique(user_id, product_id)` constraint.

## 22. High availability: Replication vs Sentinel vs Cluster
- **Replication** — primary → replicas, **asynchronous**. Replicas serve reads. Acknowledged writes can be lost on failover (`WAIT` reduces but doesn't eliminate this).
- **Sentinel** — monitors primary, runs quorum-based automatic failover, tells clients the new primary. HA for a **single shard**.
- **Cluster** — **sharding + HA**: 16,384 hash slots spread across multiple primaries, each with replicas. Use when data or throughput exceeds one node.

Managed options: AWS ElastiCache / MemoryDB, Azure Cache for Redis, GCP Memorystore, Redis Cloud.

## 23. Redis Cluster hash slots and hash tags
`slot = CRC16(key) mod 16384`. Multi-key commands (`MGET`, `MULTI`, Lua with several keys) only work if **all keys are in the same slot**, otherwise `CROSSSLOT` error. Force co-location with **hash tags** — only the part in `{}` is hashed:
```
cart:{7}        stock-reservation:{7}     → same slot, can be used together in one Lua script
```
Beware: a hash tag that's too coarse (e.g. `{ecom}`) puts everything on one node → hot shard.

## 24. Hot keys and big keys
- **Big key**: a single value that's huge (a 50 MB string, a hash with 5M fields). Causes slow commands, uneven memory across shards, slow migration/deletion. Find with `redis-cli --bigkeys` / `MEMORY USAGE`. Fix: split (`cart:7` not `carts`), paginate, `UNLINK`.
- **Hot key**: one key receiving a huge share of traffic (flash-sale product, homepage). One shard maxes out. Fix: **local L1 cache** (Caffeine) in front of Redis, replicate the key (`product:42:{1..N}` read randomly), read from replicas.

## 25. Two-level cache (L1 Caffeine + L2 Redis)
Request → **Caffeine (in-JVM, ~ns)** → **Redis (network, ~0.5 ms)** → **Postgres (~ms)**. L1 absorbs hot keys; L2 is shared across instances. Invalidation: on write, delete Redis key and **publish** an eviction message (Redis Pub/Sub) so every instance clears its L1. Keep L1 TTL short (seconds) as a safety net.

## 26. Cache consistency — can the cache ever be strongly consistent with the DB?
Not cheaply. Options, from simplest to strongest:
1. TTL only (bounded staleness)
2. Cache-aside + delete-after-commit (+ delayed double delete)
3. **CDC**: Debezium reads Postgres WAL → publishes changes → consumer evicts/updates Redis. Decouples invalidation from app code and catches writes made by other services/SQL scripts.
4. Don't cache it (orders, payments, balances — read from DB).

Interview answer: "We accept eventual consistency with bounded staleness for catalog data, and never cache money."

## 27. Redis Streams vs Kafka
Streams: consumer groups, acks, pending-entries list, `XCLAIM` for stuck messages, in-memory (capped with `MAXLEN`). Great for modest volumes and when Redis is already there. Kafka: disk-based, huge retention, partitions for massive throughput, ecosystem (Connect, Streams). Retention is the usual deciding factor.

## 28. Memory optimization
- Set `maxmemory` explicitly; watch `used_memory`, `mem_fragmentation_ratio`, `evicted_keys`.
- Prefer **hashes of small objects** over many tiny string keys (listpack encoding is very compact).
- Serialize compactly (JSON with short field names, or Kryo/Protobuf/MessagePack).
- Always TTL cache keys; use `UNLINK` for big deletes; `activedefrag yes` for fragmentation.

## 29. Security
- Never expose 6379 to the internet (bots scan it constantly; unauthenticated Redis has been used to write SSH keys and crontabs).
- **ACLs** (Redis 6+): per-user passwords and command/key restrictions (`-@dangerous`, `~ecom:*`).
- **TLS** in transit; rename/disable `FLUSHALL`, `CONFIG`, `DEBUG`.
- Don't cache PII/secrets unless necessary; if you do, TTL it.

## 30. Monitoring & debugging
`INFO` (memory, stats, replication), `SLOWLOG GET`, `LATENCY DOCTOR`, `MONITOR` (debug only — expensive), `CLIENT LIST`, `redis-cli --bigkeys --hotkeys`. Key metrics: **hit ratio** (`keyspace_hits / (hits + misses)`), latency p99, evictions, memory, connected clients, replication lag. Spring Boot Actuator exposes a Redis health indicator and Micrometer cache metrics (`cache.gets{result=hit|miss}`).

## 31. Licensing note (2024–2025)
Redis moved from BSD to RSAL/SSPL in 2024 (7.4+), which triggered the Linux Foundation fork **Valkey** (BSD, API-compatible, adopted by AWS/Google). Redis 8 (2025) added **AGPLv3** as an option again. For our purposes the client code is identical — Spring Data Redis works against Redis or Valkey.

## 32. Classic system-design prompts where Redis is the answer
- "Design a URL shortener" → `INCR` for IDs, cache hot redirects.
- "Design a leaderboard" → ZSet.
- "Flash sale / prevent overselling" → Lua atomic decrement of pre-loaded stock + async order creation via queue + DB as final authority.
- "Rate-limit an API" → Q19.
- "Online presence / who's online" → keys with short TTL heartbeats, or bitmaps.
- "Session store for horizontally scaled app" → Spring Session Redis.

---

## Part 4 — Introducing Redis into This E-Commerce API

### 4.0 Current state — where it hurts today
| Area | Today | Problem Redis fixes |
|---|---|---|
| `GET /api/v1/products`, `/products/{id}`, categories | Every request → `productRepository.findAll()` / `findById` → Postgres | Catalog is read-heavy, rarely changes → perfect cache target |
| `POST /auth/login` | No attempt limiting | Brute-force / credential stuffing |
| Logout | JWT is stateless; `JwtFilter` accepts any unexpired token (`expiration-time` is ~141 days!) | No way to revoke a stolen token |
| `OrderService.placeOrder` | Double-click / client retry → two orders | Idempotency key |
| Stock | `placeOrder` never checks or decrements `Product.stock` | Flash-sale overselling (Redis as fast gate, DB as authority) |
| Best-sellers / recently viewed | Not implemented | ZSet / capped List are one-liners |

Adopt in phases — each phase is independently shippable and **the app must keep working if Redis is down** (cache is an optimization, not a dependency).

### 4.1 Phase 0 — Infrastructure

**`docker-compose.yml`** — add a service:
```yaml
  redis:
    image: redis:7.4-alpine
    container_name: redis
    command: ["redis-server", "--appendonly", "yes", "--maxmemory", "256mb", "--maxmemory-policy", "allkeys-lru"]
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      retries: 5

volumes:
  redis-data:
```
and in `springboot-app`: add `redis` to `depends_on` plus `SPRING_DATA_REDIS_HOST=redis`.

**`pom.xml`**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>  <!-- Lettuce client by default -->
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

**`application.properties`**:
```properties
spring.data.redis.host=${SPRING_DATA_REDIS_HOST:localhost}
spring.data.redis.port=${SPRING_DATA_REDIS_PORT:6379}
spring.data.redis.password=${SPRING_DATA_REDIS_PASSWORD:}
spring.data.redis.timeout=500ms
spring.cache.type=redis
spring.cache.redis.key-prefix=ecom:
spring.cache.redis.time-to-live=10m
spring.cache.redis.cache-null-values=false
management.endpoints.web.exposure.include=loggers,health,metrics,caches
```
For `k8s/`: a `redis` Deployment/Service (or better, a managed Redis) and the same env vars in the app's ConfigMap/Secret.

### 4.2 Phase 1 — Cache the catalog (biggest, safest win)

**New `config/RedisConfig.java`** — JSON serialization (not JDK serialization: readable in `redis-cli`, no `Serializable` requirement, survives class changes better) and per-cache TTLs:
```java
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        var serializer = new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("ecom:")
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(cf)
                .cacheDefaults(base)
                .withCacheConfiguration("products",   base.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("product",    base.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("categories", base.entryTtl(Duration.ofHours(6)))
                .build();
    }

    // If Redis is down, log and fall through to the DB instead of failing the request.
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override public void handleCacheGetError(RuntimeException e, Cache c, Object k) { log.warn("cache get failed {}::{}", c.getName(), k, e); }
            @Override public void handleCachePutError(RuntimeException e, Cache c, Object k, Object v) { log.warn("cache put failed", e); }
            @Override public void handleCacheEvictError(RuntimeException e, Cache c, Object k) { log.warn("cache evict failed", e); }
        };
    }
}
```

**`service/ProductService.java`** — annotate the existing methods:
```java
@Cacheable(cacheNames = "products", key = "'all'", sync = true)   // sync = stampede protection (Q11)
public List<Product> findAll() { return productRepository.findAll(); }

@Cacheable(cacheNames = "product", key = "#id", sync = true)
public Optional<Product> findById(Integer id) { return productRepository.findById(id); }

@Caching(evict = {
    @CacheEvict(cacheNames = "product",  key = "#result.id"),
    @CacheEvict(cacheNames = "products", key = "'all'")
})
public Product save(Product product) { return productRepository.save(product); }

@Caching(evict = {
    @CacheEvict(cacheNames = "product",  key = "#id"),
    @CacheEvict(cacheNames = "products", key = "'all'")
})
public void deleteById(Integer id) { productRepository.deleteById(id); }
```
Same pattern for `CategoryService` (`categories` cache).

Gotchas specific to this codebase:
- **Cache DTOs, not JPA entities.** `Product` has `@ManyToOne Category`; caching entities risks `LazyInitializationException` (we have `spring.jpa.open-in-view=false`) and serializes whatever graph is loaded. Better: move the annotations to a method returning `ProductResponse` (via the existing `mapper/` package).
- `Optional` return with `@Cacheable`: Spring unwraps it and caches the value; an empty Optional isn't cached (because `disableCachingNullValues`) — add short-TTL null caching later if bots probe missing IDs (Q11 penetration).
- `findAll()` cached as one big list is fine for a small catalog; once there's pagination, cache per page (`key = "#pageable.pageNumber + ':' + #pageable.pageSize"`) and evict by prefix or version the key.
- **Self-invocation**: `@Cacheable` works through a proxy — calling `this.findById()` from inside `ProductService` bypasses the cache.
- Evict after commit if `save` ever becomes part of a larger `@Transactional` flow (Q10).

Expected effect: product reads go from a Postgres round-trip to a sub-ms Redis GET; DB load for catalog browsing drops by the hit ratio (typically 90%+).

### 4.3 Phase 2 — Login rate limiting (security win)

In `AuthController` (or a `OncePerRequestFilter` on `/auth/login`):
```java
@Service
@RequiredArgsConstructor
public class LoginRateLimiter {
    private final StringRedisTemplate redis;
    private static final int MAX_ATTEMPTS = 5;

    public boolean isAllowed(String ip, String username) {
        String key = "ecom:ratelimit:login:" + ip + ":" + username;
        Long attempts = redis.opsForValue().increment(key);
        if (attempts != null && attempts == 1) {
            redis.expire(key, Duration.ofMinutes(15));
        }
        return attempts == null || attempts <= MAX_ATTEMPTS;   // fail open if Redis is down
    }

    public void reset(String ip, String username) {             // call on successful login
        redis.delete("ecom:ratelimit:login:" + ip + ":" + username);
    }
}
```
Return `429 Too Many Requests` via `GlobalExceptionHandler`. (INCR + EXPIRE is two commands — if the app dies between them the key never expires; fine for a demo, use a Lua script or `SET key 0 EX 900 NX` then `INCR` for production.)

### 4.4 Phase 3 — JWT revocation / logout

Tokens live ~141 days (`security.jwt.expiration-time=12222200000`), so logout must be enforceable server-side:
1. Add a `jti` (UUID) claim in `JwtService.generateToken`.
2. `POST /auth/logout` → `SET ecom:jwt:denylist:{jti} 1 EXAT <token exp>` — the key auto-expires exactly when the token would have, so the denylist never grows unbounded.
3. In `JwtFilter`, after signature validation: `if (redis.hasKey("ecom:jwt:denylist:" + jti)) → 401`.

(Better long-term: shorten access token to 15 min and store **refresh tokens** in Redis, `ecom:refresh:{userId}:{tokenId}` with TTL — revoke by deleting.) Decide fail-open vs fail-closed here deliberately — for auth, fail-closed is safer.

### 4.5 Phase 4 — Idempotent order placement

`OrderService.placeOrder` is `@Transactional` but a client retry/double-click creates two orders. Require an `Idempotency-Key` header on `POST /api/v1/orders/place`:
```java
String key = "ecom:idem:order:" + userId + ":" + idemKey;
Boolean first = redis.opsForValue().setIfAbsent(key, "IN_PROGRESS", Duration.ofHours(24));
if (Boolean.FALSE.equals(first)) {
    String existing = redis.opsForValue().get(key);
    if ("IN_PROGRESS".equals(existing)) throw new ConflictException("Order is being processed");
    return orderService.findById(Integer.valueOf(existing)).orElseThrow();   // replay original result
}
try {
    Order order = orderService.placeOrder(req);
    redis.opsForValue().set(key, order.getId().toString(), Duration.ofHours(24));
    return order;
} catch (RuntimeException e) {
    redis.delete(key);           // allow the client to retry after a real failure
    throw e;
}
```
For hard guarantees, also persist the key in an `orders.idempotency_key` column with a unique constraint — Redis is the fast path, the DB constraint is the backstop (same philosophy as `CartService`).

### 4.6 Phase 5 — Stock reservation for flash sales

Today `placeOrder` doesn't touch `Product.stock` at all. When it does, the DB-only version is `UPDATE products SET stock = stock - :q WHERE id = :id AND stock >= :q` (check rows affected) — that's correct and should stay the authority. Under a flash sale (10k users, 100 units), Redis becomes a **gate** so 9,900 requests are rejected in memory without touching Postgres:
1. Preload: `SET ecom:stock:{42} 100` when the sale starts.
2. On checkout: run the Lua script from Q16 (`-1` → "sold out", return immediately).
3. On success: create the order in Postgres (conditional UPDATE above). If the DB step fails, `INCRBY` the stock back (compensation — see the saga notes).
4. Periodically reconcile Redis stock with DB stock.

### 4.7 Phase 6 — Nice-to-haves
- **Best-sellers**: on order placed (existing RabbitMQ consumer is a good hook), `ZINCRBY ecom:bestsellers:2026-09 <qty> <productId>`; `GET /products/bestsellers` → `ZREVRANGE ... 0 9`.
- **Recently viewed**: on `GET /products/{id}` for a logged-in user, `LPUSH ecom:recent:{userId} 42` + `LTRIM 0 19` + `EXPIRE 30d`.
- **Cart in Redis** (hash `ecom:cart:{userId}` → `productId: qty`): fast and good for guest carts, but our cart snapshots price and relies on a DB unique constraint — keep Postgres as the source of truth unless cart traffic becomes a bottleneck.
- **Spring Session Redis**: only needed if we move away from stateless JWT to server-side sessions.
- **Distributed scheduled jobs**: ShedLock with Redis provider so a `@Scheduled` job (e.g. cancel unpaid orders) runs on one instance only when scaled out in `k8s/`.

### 4.8 What we will NOT put in Redis
Orders, payments, user accounts, addresses — anything where loss or staleness costs money or trust. Redis holds **copies** (cache) and **ephemeral state** (counters, locks, denylist, idempotency keys).

### 4.9 Testing
- **Testcontainers**: `@Container static GenericContainer<?> redis = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);` + `@DynamicPropertySource` for `spring.data.redis.host/port`.
- Test: second `findById` call doesn't hit the repository (`verify(repo, times(1))` with a `@SpyBean`); `save` evicts; app still serves products when the Redis container is stopped (error handler works).
- `@DataRedisTest` slice for templates/repositories.

### 4.10 Rollout checklist
- [ ] Redis in docker-compose + k8s, `maxmemory` + `allkeys-lru` set
- [ ] Starter deps + properties, `RedisConfig` with JSON serializer and `CacheErrorHandler`
- [ ] Phase 1 caching on products/categories (DTOs), with TTL jitter if many keys are warmed at once
- [ ] Actuator health/metrics; dashboard for hit ratio, latency, memory, evictions
- [ ] Phase 2 rate limit → Phase 3 JWT denylist → Phase 4 idempotency → Phase 5 stock gate
- [ ] Load test before/after (e.g. k6 against `GET /api/v1/products`) to show the gain

---

## Quick-fire interview answers
- **Is Redis a database?** Yes — an in-memory data store that can persist, but usually used as cache/ephemeral store alongside a primary DB.
- **Why single-threaded is fine?** CPU is rarely the bottleneck; memory and network are. No locks = simple + atomic.
- **Cache-aside write order?** Update DB, then delete cache.
- **Stampede fix?** Lock/single-flight (`sync = true`), logical expiry, TTL jitter.
- **Lock correctness?** Redis locks = efficiency; correctness needs fencing / DB constraints.
- **Scale beyond one node?** Replicas for reads, Cluster for sharding.
- **Data loss window?** AOF `everysec` ≈ 1s; async replication can lose acknowledged writes on failover.
- **Most dangerous commands?** `KEYS`, `FLUSHALL`, big `DEL`, long Lua scripts, `MONITOR` in prod.
