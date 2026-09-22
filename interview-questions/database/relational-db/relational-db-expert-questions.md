# Relational Database — Expert-Level Interview Questions

Builds on `relational-db-basics.md`. Past table design and single-query tuning, into how the engine actually works under the hood, and production-scale concerns (replication, distributed transactions, consistency). Postgres-flavored, since that's this repo's database (`application.properties`).

## 1. What is MVCC, and how does it let readers and writers avoid blocking each other?
**Multi-Version Concurrency Control**: instead of locking a row for reads, the database keeps multiple *versions* of a row and gives each transaction a consistent snapshot as of when it started. A reader never blocks a writer, and a writer never blocks a reader — only two writers touching the same row actually contend.

**Real-life scenario:** a `SELECT` on `Product` while an `UPDATE` to the same row is mid-flight.
```sql
-- Transaction 1 (long-running report query)
BEGIN;
SELECT * FROM products WHERE id = 5;  -- sees the row as of this transaction's snapshot

-- Transaction 2 (concurrent update)
UPDATE products SET price = 12.99 WHERE id = 5;  -- creates a NEW row version, doesn't block T1's read
COMMIT;
```
In Postgres, `UPDATE` doesn't modify the row in place — it writes a brand-new row version (old row marked with an `xmax`, new one gets an `xmin`) and each transaction only sees the version valid for its own snapshot (isolation level dependent, see basics Q10). The old version becomes **dead** once no transaction can still see it — which is exactly what Q2 (`VACUUM`) has to clean up. This is also why Postgres never needs a dedicated "read lock" the way some older engines do.

## 2. What is VACUUM in PostgreSQL, and why does it matter?
Because MVCC (Q1) never deletes an old row version in place, `UPDATE`/`DELETE` leave behind **dead tuples** — old versions nothing can see anymore, but still physically occupying disk space. `VACUUM` reclaims that space for reuse (it does *not* shrink the file back to the OS by default — that needs `VACUUM FULL`, which takes an exclusive lock).

**Real-life scenario:** `Cart` rows get updated constantly (`bumpQuantity` in `CartService.addToCart`) — every bump leaves a dead tuple behind.
```sql
VACUUM ANALYZE cart;   -- reclaims dead tuple space + refreshes planner statistics
```
Autovacuum runs this automatically in the background by default, but a table with very high write/update churn (like a busy `cart` table) can still bloat faster than autovacuum keeps up, especially under heavy load or long-running transactions (a transaction open for hours prevents Postgres from considering rows dead, since *that* transaction might still need the old version). Left unchecked, table and index bloat slows every query on that table and eventually forces manual intervention. This is the same underlying mechanism behind Q47 in the MCQ file (the on-call doctors `SERIALIZABLE` anomaly) — MVCC snapshots are what let two concurrent reads both see "2 on duty" even while updates are in flight.

## 3. How do you read an `EXPLAIN ANALYZE` output?
`EXPLAIN` shows the planner's chosen query plan and its *estimated* cost; `EXPLAIN ANALYZE` actually **runs** the query and adds *real* timing/row counts, which is what you compare estimated vs. actual to spot a bad plan.
```sql
EXPLAIN ANALYZE
SELECT * FROM transactions WHERE customer_id = 42 AND transaction_date > '2026-06-21';
```
```
Bitmap Heap Scan on transactions  (cost=12.50..145.32 rows=40 width=64) (actual time=0.45..2.10 rows=38 loops=1)
  Recheck Cond: (customer_id = 42)
  ->  Bitmap Index Scan on idx_customer_date  (cost=0.00..12.49 rows=40 width=0) (actual time=0.30..0.30 rows=38 loops=1)
        Index Cond: (customer_id = 42)
  Filter: (transaction_date > '2026-06-21')
Planning Time: 0.15 ms
Execution Time: 2.35 ms
```
What to look for:
- **Scan type**: `Seq Scan` (reads the whole table — fine for a small table, a red flag on a big one), `Index Scan` (jumps via the index, fetches rows one at a time), `Bitmap Heap Scan` (index finds matching rows in one pass, then fetches the actual pages in physical order — cheaper than `Index Scan` when many rows match).
- **`rows=X` estimated vs. `rows=Y` actual**: a huge gap means the planner's statistics are stale (`ANALYZE` the table) or the query needs rewriting — a bad row estimate often means every plan decision downstream (join order, join algorithm) is also wrong.
- **`cost=start..total`**: arbitrary planner units, not milliseconds — only useful for comparing plans against each other, not as an absolute number.
- **Nested loops with a high `loops=N`** on an inner scan are a common N+1-at-the-SQL-level smell — the same shape as the ORM-level N+1 problem in basics Q17, just visible directly in the plan.

## 4. What is a covering index / index-only scan?
An index that includes **every column the query needs** (not just the `WHERE`/`JOIN` columns), so Postgres can answer the query straight from the index without ever touching the actual table (heap) rows.
```sql
-- Query only ever needs these 3 columns
SELECT customer_id, transaction_date, amount
FROM transactions
WHERE customer_id = 42;

-- Covering index: customer_id for the WHERE, INCLUDE adds the rest without making them sort keys
CREATE INDEX idx_covering ON transactions (customer_id) INCLUDE (transaction_date, amount);
```
Without `INCLUDE`, Postgres finds the matching index entries, then still has to jump to the heap to fetch `transaction_date`/`amount` (a "heap fetch" per row) — extra random I/O. With every needed column inside the index, `EXPLAIN` shows `Index Only Scan`, skipping the heap entirely (subject to Postgres's visibility map being up to date, which is another thing `VACUUM`, Q2, maintains). The trade-off: a wider index costs more disk space and slightly more write overhead, so it's worth it only for genuinely hot, narrow queries.

## 5. What is write-ahead logging (WAL), and how does it relate to durability and replication?
Before any change is applied to the actual data files, Postgres first writes a record of that change to the **WAL** (a sequential, append-only log — the actual mechanism behind the "commit log" concept from `kafka-questions.md` Q2, just for a relational DB instead of Kafka). Once the WAL record is safely on disk, the transaction can be acknowledged as committed — this is what gives Durability (the "D" in ACID, basics Q9) even if the server crashes right after.
```
Client: COMMIT
   │
   ▼
Postgres: write change to WAL (fsync to disk)  ← durability guaranteed HERE
   │
   ▼
Postgres: acknowledges COMMIT to client
   │
   ▼ (can happen later, async)
Postgres: applies the actual change to the data files
```
If the server crashes between "WAL written" and "data file updated," Postgres replays the WAL on restart to redo the work — nothing is lost. This same WAL stream is also **how replication works** (Q6): a replica just receives and replays the same WAL records the primary generated, applying the exact same sequence of changes.

## 6. Streaming replication vs. logical replication — what's the difference?
Both copy data from a primary to a replica, but at different levels.
- **Streaming (physical) replication**: the replica receives the raw WAL byte stream (Q5) and replays it exactly — the replica becomes a byte-for-byte copy of the primary. Fast, simple, but all-or-nothing: you replicate the *entire* database/cluster, and the replica must be the same Postgres major version.
- **Logical replication**: decodes the WAL into logical changes (`INSERT`/`UPDATE`/`DELETE` on specific tables) and replays those as SQL-level operations. Slower, but lets you replicate a subset of tables, replicate into a *different* schema/version, or even into a different database entirely (e.g. feeding a data warehouse for OLAP, basics Q27).
```sql
-- Logical replication: publish specific tables on the primary
CREATE PUBLICATION order_events FOR TABLE orders, order_items;

-- Subscribe from another Postgres instance
CREATE SUBSCRIPTION order_events_sub
    CONNECTION 'host=primary dbname=ecommerce'
    PUBLICATION order_events;
```
Streaming replication is the default choice for read replicas/failover (Q7); logical replication is the choice when you need selective, cross-version, or cross-system data flow.

## 7. What is replication lag, and what consistency problems does it cause?
A replica applies WAL records **after** the primary already committed them — there's always some delay, even if tiny. During that window, a query against the replica can return **stale** data.

**Real-life scenario:** `OrderService.placeOrder` writes to the primary, then the confirmation page immediately reads the order back — but the read is routed to a replica that hasn't caught up yet.
```
T0: Primary commits new Order row
T0+50ms: Replica still hasn't applied it (lag)
T0+10ms: Confirmation page reads from the replica → "order not found" or shows the pre-order state
```
This is the classic **read-your-writes** consistency problem. Common fixes: route the *immediate* post-write read back to the primary (or a replica confirmed to be caught up), accept eventual consistency for things that can tolerate it (e.g. an analytics dashboard), or use a session-level guarantee (some setups stick a user's reads to the primary for N seconds after they write). This is a narrower, DB-specific version of the same trade-off CAP theorem (Q10) describes at the whole-system level.

## 8. Two-phase commit (2PC) vs. the Saga pattern — when would you actually reach for 2PC?
Both coordinate a transaction across multiple databases/services, but with very different guarantees and costs.
- **2PC**: a coordinator asks every participant to **prepare** (lock resources, confirm it *can* commit) and only tells everyone to actually **commit** once all participants say yes — genuinely atomic across systems, but every participant holds locks for the whole round-trip, and if the coordinator crashes mid-protocol, participants can be left blocked holding locks indefinitely ("in-doubt" transactions).
- **Saga** (microservices notes Q19): each step commits locally and immediately; a failure triggers **compensating** actions to undo prior steps. No cross-system locks held, but there's a window where the system is genuinely in an intermediate state (Order created, Payment not yet confirmed) that other reads can observe.
```
2PC:   Prepare(A) → Prepare(B) → Prepare(C) → [all yes] → Commit(A) → Commit(B) → Commit(C)
                                              → [any no]  → Abort(A) → Abort(B) → Abort(C)

Saga:  Commit(A) → Commit(B) → Commit(C) succeeds, OR
       Commit(A) → Commit(B) → Fail(C) → Compensate(B) → Compensate(A)
```
In practice: 2PC is rare in microservices architectures precisely because of that locking/availability cost (a slow or dead participant blocks everyone) — it shows up more within a *single* database engine's internal distributed transaction handling, or specific XA-transaction integrations. Most microservice systems (this project's `OrderService.placeOrder` → Payment example, MCQ Q66) use sagas instead, trading strict cross-system atomicity for availability and accepting a brief inconsistent window that compensations resolve.

## 9. `plan_cache_mode` and parameter sniffing — how can a cached query plan go wrong?
For a **prepared statement** run repeatedly with different parameter values, Postgres can either re-plan every execution (using the actual parameter values — "custom plan") or cache one **generic plan** after enough executions and reuse it regardless of the parameter values passed in.

**Real-life scenario:** a query filtering `transactions` by `status` where 99% of rows are `'COMPLETED'` and 1% are `'DISPUTED'`.
```sql
PREPARE find_by_status (text) AS
SELECT * FROM transactions WHERE status = $1;

EXECUTE find_by_status('DISPUTED');    -- best plan: Index Scan (few matching rows)
EXECUTE find_by_status('COMPLETED');   -- best plan: Seq Scan (most of the table matches anyway)
```
If Postgres settles on a generic plan optimized for the *average* case, a query for the rare value (`'DISPUTED'`) can end up using a `Seq Scan` when an `Index Scan` would've been far cheaper, or vice versa — the plan doesn't adapt per call. `plan_cache_mode` (`auto` (default) / `force_custom_plan` / `force_generic_plan`) controls this trade-off: `force_custom_plan` re-plans every time (safer for skewed data, more planning overhead per call); `force_generic_plan` never re-plans (cheaper planning, risk of a bad plan for outlier parameter values). This is the Postgres-specific flavor of the more general "parameter sniffing" problem that also shows up in SQL Server/other engines with cached execution plans.

## 10. How does the CAP theorem relate to choosing Postgres vs. a NoSQL store for a given feature?
CAP says a **distributed** system can only fully guarantee two of **Consistency** (every read sees the latest write), **Availability** (every request gets a response), and **Partition tolerance** (the system keeps working despite network partitions between nodes) — and since network partitions can always happen, the real choice in practice is **CP vs. AP** when a partition occurs.

- **A single-primary Postgres setup** (this repo's setup) is effectively **CP**: all writes go through one primary, so it's strongly consistent, but if the primary is unreachable, writes stop (you sacrifice availability rather than risk inconsistency).
- **A multi-master or eventually-consistent store** (many NoSQL databases, some multi-region setups) leans **AP**: it stays available and accepts writes even during a partition, but different nodes can temporarily disagree, resolved later (eventual consistency) — the same lag/staleness idea as Q7, just as an explicit design choice rather than a side effect.

**Where this matters practically in an e-commerce system:** `Order`/`Payment` data wants **CP** — you'd rather reject a checkout than silently lose consistency about whether payment happened. A `ProductView` counter or a recommendation cache can tolerate **AP** — showing a slightly stale "1,204 views" is harmless, and staying available matters more than perfect accuracy. This is exactly why a real system is rarely "one database for everything" — the CAP trade-off is made **per piece of data**, not once for the whole architecture.

## 11. What is partition pruning, and how does it interact with query performance?
When a table is partitioned (basics Q28), the planner can skip scanning partitions that **cannot possibly** contain matching rows, based on the `WHERE` clause and the partition key — this is pruning.
```sql
CREATE TABLE transactions (
    transaction_id BIGINT,
    transaction_date DATE,
    amount DECIMAL(12,2)
) PARTITION BY RANGE (transaction_date);

CREATE TABLE transactions_2025 PARTITION OF transactions FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE transactions_2026 PARTITION OF transactions FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');

EXPLAIN SELECT * FROM transactions WHERE transaction_date > '2026-06-01';
-- Plan only touches transactions_2026 — transactions_2025 is pruned entirely, never scanned
```
Pruning only works when the `WHERE` clause directly constrains the partition key with a comparable literal/parameter — wrapping the column in a function (`WHERE EXTRACT(YEAR FROM transaction_date) = 2026`, the same "function on an indexed column" mistake as basics Q16) defeats pruning just like it defeats a regular index, forcing every partition to be scanned. This is the mechanism that makes partitioning (rather than just indexing) worth it for genuinely huge time-series-style tables: entire partitions — potentially billions of rows — are skipped without even opening them.

## 12. Application-level pooling (HikariCP) vs. an external pooler (PgBouncer) — when do you need both?
HikariCP (basics Q26, this repo's default) pools connections **per application instance** — each pod holds its own pool of up to `maximum-pool-size` connections, all the way to the database. **PgBouncer** sits as a separate proxy *in front of* Postgres and pools connections **across every client**, including every app instance.

**Real-life scenario:** this repo's own numbers from basics Q26 — `max_connections=100` on Postgres, HikariCP default of 10 per pod, scaled to 8 pods (`8 x 10 = 80`, uncomfortably close to the limit).
```
Without PgBouncer:
  8 pods x 10 HikariCP connections each = 80 real Postgres connections, always open

With PgBouncer (transaction pooling mode) in front of Postgres:
  8 pods x 10 HikariCP connections each = 80 connections... to PgBouncer, not Postgres
  PgBouncer multiplexes those onto a much smaller real pool to Postgres (e.g. 20),
  handing out a real connection only for the duration of one transaction
```
This matters because each real Postgres connection is a full OS process with real memory overhead — Postgres doesn't handle thousands of idle connections gracefully the way some other engines do. PgBouncer (or a managed equivalent) lets you scale to many more application instances/threads than `max_connections` would otherwise allow, by sharing a much smaller pool of *actual* database connections underneath. The trade-off: PgBouncer's transaction-pooling mode breaks session-level features that assume a connection is "yours" for the whole session (e.g. `SET` variables, `LISTEN`/`NOTIFY`, prepared statements across transactions) — worth checking against before adopting it blindly.
