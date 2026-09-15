# PostgreSQL — Basic Interview Questions

## 1. What is PostgreSQL and how is it different from MySQL?
PostgreSQL is an open-source, object-relational database known for strict standards compliance, advanced data types (JSONB, arrays, ranges), extensibility (custom types/functions), and strong support for complex queries. Compared to MySQL, it generally offers richer feature sets and better handling of complex/analytical workloads, while MySQL has historically been favored for simpler, high-throughput read-heavy web apps.

## 2. What is MVCC (Multi-Version Concurrency Control)?
PostgreSQL's mechanism for handling concurrent access: instead of locking rows for reads, each transaction sees a "snapshot" of the data as of its start, allowing readers and writers to avoid blocking each other.

## 3. What is a sequence in PostgreSQL?
A database object that generates unique numeric identifiers, commonly used to implement auto-incrementing primary keys (via `SERIAL`/`BIGSERIAL` or `GENERATED ... AS IDENTITY`).

## 4. What is the difference between `SERIAL` and `IDENTITY` columns?
`SERIAL` is a shorthand that creates a sequence and sets the column default to `nextval()`. `GENERATED AS IDENTITY` (SQL-standard, added in PG10) is the modern, safer approach with more control over whether values can be overridden.

## 5. What is JSONB and how does it differ from JSON?
Both store JSON data. `JSON` stores an exact text copy and re-parses it on read. `JSONB` stores data in a decomposed binary format — slightly slower to insert but much faster to query, and supports indexing (e.g., GIN indexes).

## 6. What are PostgreSQL schemas?
Namespaces within a database that group tables, views, and other objects, allowing multiple logical groupings (or multiple applications/tenants) to coexist in a single database without name collisions.

## 7. What is a CTE (Common Table Expression)?
A named temporary result set defined with `WITH`, used to simplify complex queries and enable recursive queries.
```sql
WITH regional_sales AS (
    SELECT region, SUM(amount) AS total
    FROM orders
    GROUP BY region
)
SELECT * FROM regional_sales WHERE total > 10000;
```

## 8. What is the difference between `VACUUM` and `VACUUM FULL`?
`VACUUM` reclaims space from dead tuples (from updates/deletes under MVCC) for reuse without locking the table. `VACUUM FULL` rewrites the entire table to reclaim disk space back to the OS, but requires an exclusive lock.

## 9. What index types does PostgreSQL support?
B-Tree (default, general purpose), Hash, GIN (good for JSONB, arrays, full-text search), GiST (geometric/full-text data), BRIN (large, naturally ordered tables like time-series).

## 10. What is the difference between `TEXT` and `VARCHAR(n)` in PostgreSQL?
Functionally almost identical — both store variable-length strings. `VARCHAR(n)` enforces a length limit; `TEXT` has no limit. Unlike some databases, there's negligible performance difference between them in PostgreSQL.

## 11. What are PostgreSQL's window functions?
Functions that perform calculations across a set of rows related to the current row without collapsing them into a single output row, e.g., `ROW_NUMBER()`, `RANK()`, `SUM() OVER (PARTITION BY ...)`.

## 12. How does PostgreSQL handle full-text search?
Through `tsvector` (searchable document representation) and `tsquery` (search query), often combined with a GIN index for performance.

## 13. What is the difference between `pg_dump` and physical backups?
`pg_dump` performs a logical backup (SQL statements or custom archive format) of one database, portable across versions. Physical backups (e.g., `pg_basebackup`, WAL archiving) copy the actual data files and are used for point-in-time recovery and replication.
