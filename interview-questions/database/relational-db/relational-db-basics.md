# Relational Database — Basic Interview Questions

General concepts that apply across all relational databases (MySQL, PostgreSQL, Oracle, SQL Server, etc.).

## 1. What is a relational database?
A database that stores data in tables (rows and columns) where relationships between tables are defined using keys (primary key, foreign key). Data integrity is enforced through constraints, and data is queried using SQL.

**A bit more on "constraints":** these are rules attached directly to a table's columns that the database itself enforces on every write — so bad data gets rejected at insert/update time, no matter which application or query is doing the writing.

The common ones: `NOT NULL` (a value must always be provided), `UNIQUE` (no two rows can share this value), `PRIMARY KEY` (unique + not null, identifies the row), `FOREIGN KEY` (a value must actually exist in another table), `CHECK` (a custom condition, e.g. balance can't go negative), and `DEFAULT` (a value used automatically if none is given).

`FOREIGN KEY` is the one worth pausing on here, since it's what makes the "relationships between tables" part of this answer actually enforced, not just implied.

This is why constraints matter for "relational" specifically: without them, the keys linking your tables together are just conventions you *hope* every piece of code respects. With them, the database refuses any write that would break the relationship.

See Q20 for a full worked example on an `accounts` table using all of these together.

## 2. What is a primary key?
A column (or set of columns) that uniquely identifies each row in a table. It cannot be NULL and must be unique.

## 3. What is a foreign key?
A column that references the primary key of another table, used to enforce referential integrity between two tables.

## 4. What is normalization? Explain 1NF, 2NF, 3NF.
Normalization is the process of organizing data to reduce redundancy and improve integrity, done in stages — each stage (normal form) fixes a specific kind of redundancy/anomaly.

**A few terms used below:**
- **Composite key** — a primary key made up of *more than one column*, where it's the combination of those columns (not either one alone) that's unique per row. In the example below, no single column identifies an enrollment row — you need both `StudentID` and `CourseID` together (the same student appears in many rows, and the same course appears in many rows, but that exact *pair* only appears once). Written as `(StudentID, CourseID)`.
- **Partial dependency** — happens only when you *have* a composite key: a non-key column depends on just part of the composite key, not the whole thing.
- **Transitive dependency** — a non-key column depends on another non-key column, instead of depending on the key directly (A → B → C, where C should really only depend on A).
- **Anomaly** — a data-integrity bug that redundant, unnormalized data causes: an **update anomaly** (change one instructor's office, forget to update it on some rows), an **insertion anomaly** (can't add a course until a student enrolls in it), or a **deletion anomaly** (deleting the last student enrolled in a course wipes out the course/instructor info too).

**Starting point — an unnormalized `StudentCourse` table:**

| StudentID | StudentName | Courses                  | InstructorName | InstructorOffice |
|-----------|-------------|---------------------------|-----------------|-------------------|
| 1         | Alice       | Math101, Physics201       | Dr. Lee         | B-12              |
| 2         | Bob         | Math101                   | Dr. Lee         | B-12              |

Problems: the `Courses` column holds multiple values in one cell, and instructor info is duplicated per student.

### 1NF — atomic values, no repeating groups
Rule: every column must hold a single, indivisible value — no comma-separated lists, no arrays, no "repeating groups" of columns like `Course1, Course2, Course3`.

Why it matters: in the unnormalized table above, you can't easily ask "which students take Physics201?" with a simple `WHERE` clause — the value is buried inside a string alongside other values. You also can't limit or index on it properly, and there's no fixed number of courses a row can hold.

**Fix:** split the multi-valued `Courses` column into one row per (student, course) pair.

| StudentID | StudentName | CourseID | InstructorName | InstructorOffice |
|-----------|-------------|----------|-----------------|-------------------|
| 1         | Alice       | Math101  | Dr. Lee         | B-12              |
| 1         | Alice       | Physics201| Dr. Chen       | C-04              |
| 2         | Bob         | Math101  | Dr. Lee         | B-12              |

This is now in 1NF (every cell holds one atomic value) — but notice the primary key is no longer a single column. No column here is unique on its own (StudentID 1 appears twice, Math101 appears twice), so the key has to be the **composite** `(StudentID, CourseID)` — that pair, together, is what's unique per row.

### 2NF — 1NF + no partial dependency
Rule: every non-key column must depend on the **whole** composite key, not just part of it. (If your primary key is a single column already, you automatically satisfy 2NF — this rule only bites when you have a composite key.)

Here the key is `(StudentID, CourseID)`, but look at `StudentName`: it only depends on `StudentID` — it doesn't change based on which course we're looking at. That's a *partial* dependency (dependent on part of the key, not the whole thing). Same story for `InstructorName`/`InstructorOffice` depending only on `CourseID`.

The anomaly this causes: Alice's name is copied onto every row she has a course in. Misspell it on one row during an update and now the same student has two different names in the table (update anomaly).

**Fix:** split into two tables.

`Student`

| StudentID | StudentName |
|-----------|-------------|
| 1         | Alice       |
| 2         | Bob         |

`Enrollment`

| StudentID | CourseID  | InstructorName | InstructorOffice |
|-----------|-----------|-----------------|-------------------|
| 1         | Math101   | Dr. Lee         | B-12              |
| 1         | Physics201| Dr. Chen        | C-04              |
| 2         | Math101   | Dr. Lee         | B-12              |

`StudentName` now lives in exactly one row per student, keyed by the single column `StudentID` — no partial dependency possible anymore. But `Enrollment` still has a problem 3NF will catch.

### 3NF — 2NF + no transitive dependency
Rule: non-key columns must depend **only** on the primary key, directly — not on some other non-key column that happens to sit in the same table.

In `Enrollment`, ask: does `InstructorOffice` describe the *enrollment* (the student+course pairing), or does it describe the *course*? It's really a fact about the course — every student in Math101 has the same instructor and office. So `InstructorOffice` depends on `InstructorName`, and `InstructorName` depends on `CourseID` — a chain: `CourseID → InstructorName → InstructorOffice`. That chain is the transitive dependency: `InstructorOffice` depends on the key only *indirectly*, through another non-key column.

The anomaly: "Dr. Lee, B-12" gets copied onto every row where any student takes Math101. Change offices, and you must update every one of those rows — miss one and the data disagrees with itself. Delete the last student enrolled in Physics201 and you lose the only record that Dr. Chen's office is C-04 (deletion anomaly). You also can't record that a new course exists with an assigned instructor until at least one student enrolls (insertion anomaly).

**Fix:** pull course/instructor info into its own table.

`Course`

| CourseID   | InstructorName | InstructorOffice |
|------------|-----------------|-------------------|
| Math101    | Dr. Lee         | B-12              |
| Physics201 | Dr. Chen        | C-04              |

`Enrollment` (now just the relationship)

| StudentID | CourseID   |
|-----------|------------|
| 1         | Math101    |
| 1         | Physics201 |
| 2         | Math101    |

Now: `Student` holds student facts, `Course` holds course/instructor facts, `Enrollment` just links the two. No column depends on anything other than its own table's primary key — this is 3NF. Updating an instructor's office now means changing exactly one row.

## 5. What is denormalization and when would you use it?
Intentionally introducing redundancy (merging tables, duplicating data) to improve read performance, typically in reporting/analytics systems where read speed matters more than write efficiency.

## 6. What are the different types of SQL joins?
- **INNER JOIN**: rows matching in both tables.
- **LEFT JOIN**: all rows from the left table + matching rows from the right (NULLs if no match).
- **RIGHT JOIN**: all rows from the right table + matching rows from the left.
- **FULL OUTER JOIN**: all rows from both tables, matched where possible.
- **CROSS JOIN**: cartesian product of both tables.
- **SELF JOIN**: a table joined with itself.

**Real-life scenario:** using the same `customers` and `accounts` tables from the rest of this doc (`accounts.customer_id` links back to `customers.customer_id`). Not every customer has opened an account yet, so the two tables don't perfectly overlap — which is exactly what makes the join types behave differently.

```sql
-- INNER JOIN: only customers who actually have an account.
-- A customer who signed up but never opened one is simply left out.
SELECT c.customer_name, a.account_id, a.balance
FROM customers c
INNER JOIN accounts a ON a.customer_id = c.customer_id;

-- LEFT JOIN: every customer, whether they have an account or not.
-- Customers with no account show up with NULL account/balance columns
-- — handy for "find customers who haven't opened an account yet":
--   ... WHERE a.account_id IS NULL
SELECT c.customer_name, a.account_id, a.balance
FROM customers c
LEFT JOIN accounts a ON a.customer_id = c.customer_id;

-- RIGHT JOIN: every account, plus the owning customer's info.
-- Equivalent to swapping the table order in a LEFT JOIN — rarely used in
-- practice since it reads less naturally, but produces the same result as:
--   accounts a LEFT JOIN customers c ON c.customer_id = a.customer_id
SELECT c.customer_name, a.account_id, a.balance
FROM customers c
RIGHT JOIN accounts a ON a.customer_id = c.customer_id;

-- FULL OUTER JOIN: every customer AND every account, matched where possible.
-- Surfaces customers with no account (a.account_id IS NULL) in the same
-- query as any account somehow missing a valid customer (c.customer_id IS NULL)
-- — a quick way to audit both sides of the relationship at once.
SELECT c.customer_name, a.account_id, a.balance
FROM customers c
FULL OUTER JOIN accounts a ON a.customer_id = c.customer_id;

-- CROSS JOIN: every combination of two small tables — e.g. building a matrix
-- of every account type offered at every branch, before deciding which
-- combinations to actually launch.
SELECT b.branch_name, t.account_type
FROM branches b
CROSS JOIN account_types t;

-- SELF JOIN: join accounts to itself to find customers holding more than
-- one account (a1 and a2 are the same table, aliased twice).
SELECT a1.customer_id, a1.account_id AS account_1, a2.account_id AS account_2
FROM accounts a1
JOIN accounts a2 ON a1.customer_id = a2.customer_id
               AND a1.account_id < a2.account_id;  -- avoids matching a row with itself or listing each pair twice
```

## 7. What is the difference between WHERE and HAVING?
`WHERE` filters rows before grouping/aggregation. `HAVING` filters groups after `GROUP BY` has been applied.

**Real-life scenario:** using the `transactions` table, find customers whose total spending this month exceeds $5,000.

```sql
-- WHERE: filters individual transaction rows before they're grouped —
-- e.g. only look at this month's transactions in the first place.
SELECT customer_id, SUM(amount) AS total_spent
FROM transactions
WHERE transaction_date >= '2026-07-01'
GROUP BY customer_id
-- HAVING: filters the grouped result — applied *after* SUM(amount) is
-- calculated per customer, since "total_spent > 5000" doesn't exist as a
-- column on any single row, only after aggregation.
HAVING SUM(amount) > 5000;
```

Trying to write `WHERE SUM(amount) > 5000` instead would fail — at the point `WHERE` runs, the rows haven't been grouped yet, so there's no per-customer total to compare against.

## 8. What is an index and why does it matter?
A data structure (commonly a B-Tree) that speeds up row lookups at the cost of extra storage and slower writes (inserts/updates need to update the index too).

**Real-life scenario:** customer support looks up accounts by `account_number` dozens of times a day.

```sql
-- Without an index, this scans every row in accounts to find a match.
SELECT * FROM accounts WHERE account_number = 'ACC-004521';

-- Add an index on the column that's actually searched on:
CREATE INDEX idx_account_number ON accounts (account_number);

-- Same query now uses the index: the engine looks account_number up in a
-- B-Tree instead of scanning the whole table, turning a lookup that might
-- scan millions of rows into one that reads just a handful.
SELECT * FROM accounts WHERE account_number = 'ACC-004521';
```

The trade-off: every `INSERT`/`UPDATE` that touches `account_number` now also has to update this index, so it's not free — which is why you index columns that are searched/joined on often, not every column.

## 9. What is a transaction? What are the ACID properties?
A transaction is a unit of work executed as a single logical operation.
- **Atomicity**: all or nothing.
- **Consistency**: brings the DB from one valid state to another.
- **Isolation**: concurrent transactions don't interfere with each other.
- **Durability**: once committed, changes survive crashes.

**Real-life scenario: transferring money between two bank accounts.**

Moving $100 from Account A to Account B is really two updates that must happen together:

```sql
BEGIN TRANSACTION;

UPDATE accounts
SET balance = balance - 100
WHERE account_id = 'A';

UPDATE accounts
SET balance = balance + 100
WHERE account_id = 'B';

COMMIT;
```

Here's what each ACID property guarantees about this exact transaction:

- **Atomicity** — both `UPDATE`s succeed, or neither does. If the app crashes right after debiting A but before crediting B, the database rolls the whole transaction back on restart — you never end up with money deducted from A and not added to B. Without atomicity, a crash mid-transfer could make $100 vanish.

- **Consistency** — the database enforces its rules (e.g., a `CHECK (balance >= 0)` constraint) across the whole transaction. If debiting A would push its balance negative and that violates a constraint, the entire transaction is rejected — you can't end up with a state that breaks the bank's invariants ("no account may be negative").

- **Isolation** — if another transaction is reading Account A's balance at the same time (say, to display it in a mobile app), it won't see the intermediate state where A has been debited but B hasn't been credited yet. It sees either the balance before the transfer or after, never the half-finished version. This is what an isolation level like `READ COMMITTED` or `SERIALIZABLE` controls (see Q10).

- **Durability** — once `COMMIT` returns successfully, the transfer is permanent. Even if the database server loses power one second later, the updated balances survive (typically because the change was already written to a transaction log on disk before the commit was acknowledged).

## 10. What are the transaction isolation levels?
Read Uncommitted, Read Committed, Repeatable Read, Serializable — each trades off consistency guarantees against concurrency/performance. Higher isolation prevents more anomalies (dirty reads, non-repeatable reads, phantom reads) but reduces concurrency.

**Real-life scenario:** Account A has a balance of $500. Transaction 1 debits $100 from A but hasn't committed yet. At the same moment, Transaction 2 reads A's balance to show it in a mobile banking app.

```sql
-- Transaction 1
BEGIN TRANSACTION;
UPDATE accounts SET balance = balance - 100 WHERE account_id = 'A'; -- now $400, not committed

-- Transaction 2 (running concurrently)
SELECT balance FROM accounts WHERE account_id = 'A';
```

- Under `READ UNCOMMITTED`, Transaction 2 sees $400 — a **dirty read**. If Transaction 1 then rolls back, the app just showed the customer money that never actually left their account.
- Under `READ COMMITTED` (the common default, e.g. in PostgreSQL/SQL Server), Transaction 2 sees the last committed value, $500, until Transaction 1 actually commits.
- Under `SERIALIZABLE`, the database behaves as if Transaction 1 and Transaction 2 ran one after another, fully preventing any overlap-related anomaly, at the cost of more blocking/retries under load.

## 11. What is a deadlock? How can it be avoided?
Two or more transactions waiting on locks held by each other, none able to proceed. Avoided by acquiring locks in a consistent order, keeping transactions short, and using timeouts/deadlock detection.

**Real-life scenario:** two transfers happen at the same time in opposite directions.

```sql
-- Transaction 1: transfer $50 from A to B
BEGIN TRANSACTION;
UPDATE accounts SET balance = balance - 50 WHERE account_id = 'A'; -- locks row A
-- ...about to lock row B next...
UPDATE accounts SET balance = balance + 50 WHERE account_id = 'B'; -- waits, B is locked by Transaction 2

-- Transaction 2: transfer $30 from B to A (running at the same time)
BEGIN TRANSACTION;
UPDATE accounts SET balance = balance - 30 WHERE account_id = 'B'; -- locks row B
-- ...about to lock row A next...
UPDATE accounts SET balance = balance + 30 WHERE account_id = 'A'; -- waits, A is locked by Transaction 1
```

Transaction 1 holds A and waits for B; Transaction 2 holds B and waits for A — neither can finish. The database detects this and kills one transaction (rolling it back with a deadlock error) so the other can proceed. This is exactly why the "acquire locks in a consistent order" fix matters in practice: if every transfer always locked the lower account ID first, both transactions would queue for A first instead of deadlocking on each other.

## 12. What's the difference between a clustered and non-clustered index?
A **clustered index** determines the physical storage order of table data (only one per table). A **non-clustered index** is a separate structure with pointers back to the actual rows (a table can have many).

**Real-life scenario:** an `accounts` table with millions of rows.

```sql
-- Clustered index (often automatic on the primary key):
-- rows are physically stored on disk sorted by account_id
CREATE TABLE accounts (
    account_id   BIGINT PRIMARY KEY,   -- clustered index goes here
    customer_name VARCHAR(100),
    balance      DECIMAL(12,2)
);

-- Non-clustered index: a separate lookup structure
CREATE INDEX idx_customer_name ON accounts (customer_name);
```

`SELECT * FROM accounts WHERE account_id = 12345` is fast because the table itself is stored in `account_id` order — the engine jumps straight to the right physical location. `SELECT * FROM accounts WHERE customer_name = 'Alice'` uses the separate `idx_customer_name` index, which stores `customer_name` values sorted along with a pointer back to the actual row — one extra hop, but still far faster than scanning every row.

**Note — this behaves differently per database, and it's a common gotcha:**
- **MySQL (InnoDB)** and **SQL Server**: you don't create the clustered index yourself — declaring `PRIMARY KEY` creates it automatically (InnoDB falls back to the first `UNIQUE NOT NULL` column, or a hidden internal row ID, if there's no primary key). SQL Server also lets you override this with `PRIMARY KEY NONCLUSTERED` and put the clustered index on a different column instead.
- **PostgreSQL doesn't really have this concept.** Tables are stored as an unordered heap regardless of the primary key. Postgres does have a `CLUSTER` command (`CLUSTER accounts USING accounts_pkey;`) that physically reorders the table to match an index — but only **once**, at the moment you run it. New inserts/updates aren't kept in that order automatically, so the physical ordering drifts again over time unless you periodically re-run `CLUSTER`, which most teams don't bother doing. So in Postgres, `account_id BIGINT PRIMARY KEY` still gives you a fast, sorted **index** lookup — it just doesn't mean the table's physical storage is kept in that order the way InnoDB guarantees.

## 13. What is a stored procedure? What is a trigger?
A **stored procedure** is precompiled SQL logic stored in the database, callable by name. A **trigger** is code that automatically executes in response to an event (INSERT/UPDATE/DELETE) on a table.

**Real-life scenario:** wrap the transfer logic in a stored procedure, and automatically log every balance change with a trigger.

```sql
-- Stored procedure: callers just do CALL transfer_funds('A', 'B', 100)
-- instead of writing the transaction by hand every time.
CREATE PROCEDURE transfer_funds(IN from_acct VARCHAR(10), IN to_acct VARCHAR(10), IN amount DECIMAL(12,2))
BEGIN
    START TRANSACTION;
    UPDATE accounts SET balance = balance - amount WHERE account_id = from_acct;
    UPDATE accounts SET balance = balance + amount WHERE account_id = to_acct;
    COMMIT;
END;
```

**Calling it from application code — it's not a file, it's a database object you call over the same connection you already use for queries.** In a Spring Boot app (see `src/com/shihab/springboot`), Spring Data JPA can call it directly from a repository method with `@Procedure`, the same way `EmployeeRepository` declares derived query methods:

```java
public interface AccountRepository extends JpaRepository<Account, Long> {

    @Procedure(procedureName = "transfer_funds")
    void transferFunds(@Param("from_acct") String fromAccount,
                        @Param("to_acct") String toAccount,
                        @Param("amount") BigDecimal amount);
}
```

Calling `accountRepository.transferFunds("A", "B", new BigDecimal("100"))` from a service method then runs the exact same `CALL transfer_funds('A', 'B', 100)` under the hood — no raw JDBC or native query boilerplate needed.

```sql
-- balance_audit also records *who* made the change, not just what changed —
-- important for fraud investigation and dispute resolution, not just
-- reconstructing values.
CREATE TABLE balance_audit (
    audit_id     BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id   VARCHAR(10),
    old_balance  DECIMAL(12,2),
    new_balance  DECIMAL(12,2),
    changed_by   VARCHAR(50),   -- who/what made the change
    changed_at   TIMESTAMP
);

-- Trigger: fires automatically, no one has to remember to call it
CREATE TRIGGER log_balance_change
AFTER UPDATE ON accounts
FOR EACH ROW
INSERT INTO balance_audit (account_id, old_balance, new_balance, changed_by, changed_at)
VALUES (OLD.account_id, OLD.balance, NEW.balance, CURRENT_USER(), NOW());
```

Every time any code updates a row in `accounts` — whether via the stored procedure, an app, or a manual query — the trigger fires and writes an audit row, so the bank always has a change history without every caller having to remember to log it.

**Note on `changed_by`:** `CURRENT_USER()` only gives you the *database* login (e.g. `app_service`), which is often the same shared connection for every request from your application — not the actual bank employee or customer who triggered it. To capture the real end-user, the application typically sets a session variable before running the update (e.g. `SET @app_user_id = 'employee_42'` in MySQL, or `SET LOCAL app.user_id = 'employee_42'` in PostgreSQL) and the trigger reads that instead of `CURRENT_USER()`. Worth mentioning in an interview if asked "how do you know *who* made a change" — the DB-level username usually isn't enough on its own.

**PostgreSQL note — the trigger above is simplified/MySQL-style.** PostgreSQL doesn't let you write the trigger body inline like that; it requires a separate trigger *function* that the trigger calls, and reads the custom session variable (see the earlier `app.user_id` question) via `current_setting()` instead of `CURRENT_USER()`. The full, real flow:

```sql
-- 1. Trigger function: the actual logic PostgreSQL requires triggers to call.
CREATE OR REPLACE FUNCTION log_balance_change_fn() RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO balance_audit (account_id, old_balance, new_balance, changed_by, changed_at)
    VALUES (
        OLD.account_id,
        OLD.balance,
        NEW.balance,
        current_setting('app.user_id', true), -- true = return NULL instead of erroring if unset
        NOW()
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 2. The trigger itself just wires the event to that function.
CREATE TRIGGER log_balance_change
AFTER UPDATE ON accounts
FOR EACH ROW
EXECUTE FUNCTION log_balance_change_fn();
```

```sql
-- 3. The application must set app.user_id in the *same transaction*,
-- before the UPDATE, so it's visible when the trigger fires.
BEGIN;
SET LOCAL app.user_id = 'employee_42';
UPDATE accounts SET balance = balance - 100 WHERE account_id = 'A';  -- trigger fires here
COMMIT;                                                              -- SET LOCAL resets automatically
```

In a Spring Boot service (see `src/com/shihab/springboot`), step 3 means issuing that `SET LOCAL` as a native query at the start of the same `@Transactional` method that performs the update, so JPA keeps both statements on one connection:

```java
@Transactional
public EmployeeDTO updateEmployee(Long id, EmployeeDTO dto) {
    entityManager.createNativeQuery("SET LOCAL app.user_id = :userId")
                 .setParameter("userId", getCurrentUserId())
                 .executeUpdate();
    // ...rest of the update — same transaction, same DB connection
}
```

If `SET LOCAL` and the `UPDATE` ran on different pooled connections, `current_setting('app.user_id', true)` would come back `NULL` — the setting doesn't travel with the request, only with the connection/transaction it was set on.

**Note on where trigger code actually lives:** a trigger is a two-sided thing.
- **Inside the database**, `CREATE TRIGGER` registers it as a database object in the engine's system catalog (`information_schema.triggers` in MySQL, `pg_trigger`/`\dy` in PostgreSQL) — it's not a file, and it fires automatically forever until someone runs `DROP TRIGGER`.
- **In your codebase**, the *source* of that `CREATE TRIGGER` statement should still be tracked in version control — otherwise nobody can see it in code review or know it exists without inspecting the live database. In practice this means a versioned schema-migration file (e.g. Flyway's `V12__add_balance_audit_trigger.sql`, Liquibase, Rails/Django migrations), not something typed once into a DB client and forgotten. For a Spring Boot app with Flyway, that'd be `resources/db/migration/`.

This "invisible unless version-controlled" nature is itself one of the real downsides teams cite when choosing application-level logic over triggers.

## 14. What is the difference between DELETE, TRUNCATE, and DROP?
- **DELETE**: removes rows (can filter with WHERE), logged, can be rolled back, triggers fire.
- **TRUNCATE**: removes all rows quickly, minimal logging, resets identity, generally can't be rolled back (DB-dependent).
- **DROP**: removes the entire table structure and data.

**Real-life scenario**, three different cleanup tasks in the same bank system:

```sql
-- DELETE: remove specific closed accounts with a zero balance.
-- Filtered, logged, triggers (like log_balance_change) still fire, can be rolled back.
DELETE FROM accounts WHERE status = 'CLOSED' AND balance = 0;

-- TRUNCATE: wipe a daily staging table after its data has been loaded into
-- the real tables. No filtering needed, no per-row logging, very fast.
TRUNCATE TABLE daily_transactions_staging;

-- DROP: a temporary table created for a one-off migration is no longer needed.
-- Removes the table definition itself, not just its rows.
DROP TABLE migration_temp_2025;
```

Using `DELETE` on the staging table would work too, just far slower on millions of rows. Using `TRUNCATE` on `accounts` instead of `DELETE ... WHERE ...` would be a mistake — it ignores the `WHERE` filter and wipes every account.

## 15. What is a view?
A virtual table defined by a stored SQL query. It doesn't store data itself (unless materialized) but simplifies complex queries and can restrict access to specific columns/rows.

**Real-life scenario:** the fraud team should be able to see high-value accounts, but not every column (e.g., not the customer's SSN stored elsewhere in the customer table).

```sql
CREATE VIEW high_value_accounts AS
SELECT account_id, customer_name, balance, status
FROM accounts
WHERE balance > 100000;
```

The fraud team just runs `SELECT * FROM high_value_accounts;` — a simple query — instead of repeating the `WHERE balance > 100000` filter (and remembering which columns are safe to expose) every time. Grant them access to the view only, not the underlying `accounts` table, and they can never see columns you didn't include.

## 16. How would you optimize a slow SQL query?
Check the execution plan (`EXPLAIN`), ensure proper indexes exist on filtered/joined columns, avoid `SELECT *`, avoid functions on indexed columns in WHERE clauses, reduce joins where possible, and consider query rewriting or denormalization.

**Real-life scenario:** the "show my last 30 days of transactions" screen is slow.

```sql
-- Slow: no index on customer_id or transaction_date,
-- so the engine scans the entire transactions table.
SELECT * FROM transactions
WHERE customer_id = 42
  AND transaction_date > '2026-06-21';
```

```sql
-- Check what the engine is actually doing:
EXPLAIN SELECT * FROM transactions
WHERE customer_id = 42
  AND transaction_date > '2026-06-21';
-- reports "type: ALL" (full table scan) — confirms no useful index is used.
```

```sql
-- Fix: add a composite index matching how the query filters,
-- and select only the columns the screen actually needs.
CREATE INDEX idx_customer_date ON transactions (customer_id, transaction_date);

SELECT transaction_id, amount, transaction_date
FROM transactions
WHERE customer_id = 42
  AND transaction_date > '2026-06-21';
```

Re-running `EXPLAIN` now shows the engine using `idx_customer_date` to jump straight to customer 42's recent rows instead of scanning the whole table.

## 17. What is the N+1 query problem?
A performance issue common in ORMs where fetching a list of N parent records triggers N additional queries to fetch related child records, instead of one batched query (e.g., a JOIN or `IN` clause).

**Real-life scenario:** a screen lists 50 customers along with each customer's most recent transaction.

```sql
-- The "1": fetch the customers
SELECT customer_id, customer_name FROM customers LIMIT 50;

-- The "N": a naive ORM then runs this once per customer, in a loop —
-- 50 separate round-trips to the database.
SELECT * FROM transactions WHERE customer_id = 1 ORDER BY transaction_date DESC LIMIT 1;
SELECT * FROM transactions WHERE customer_id = 2 ORDER BY transaction_date DESC LIMIT 1;
-- ...48 more of these...
```

```sql
-- Fix: fetch everything needed in one or two queries instead of 51.
SELECT t.*
FROM transactions t
JOIN (
    SELECT customer_id, MAX(transaction_date) AS max_date
    FROM transactions
    WHERE customer_id IN (1, 2, 3 /* ...all 50 ids... */)
    GROUP BY customer_id
) latest ON latest.customer_id = t.customer_id AND latest.max_date = t.transaction_date;
```

The rewritten version pulls the same data in one query, regardless of whether the page shows 50 customers or 5,000 — this is exactly what triggers in interviews as "how do you spot and fix N+1 problems in a Spring Data JPA app" (see the Spring Boot example in `src/com/shihab/springboot` — `EmployeeRepository.findByDepartment` returning employees is the kind of call that becomes N+1 if you then lazily fetch something per-employee in a loop).

## 18. What is a composite key?
A primary key made up of two or more columns whose combination is unique, used when no single column uniquely identifies a row.

**Real-life scenario:** a joint bank account can have multiple account holders, and each person can be a holder on multiple accounts — a many-to-many relationship, tracked in a link table.

```sql
CREATE TABLE account_holders (
    account_id  BIGINT,
    customer_id BIGINT,
    role        VARCHAR(20), -- e.g. 'PRIMARY', 'JOINT'
    PRIMARY KEY (account_id, customer_id)
);
```

Neither `account_id` alone nor `customer_id` alone is unique here (one account has several holders; one customer holds several accounts) — only the *pair* `(account_id, customer_id)` is guaranteed unique, which is exactly what a composite primary key expresses: "this customer is a holder on this account, and that combination can't be recorded twice."

## 19. What's the difference between UNION and UNION ALL?
`UNION` combines result sets and removes duplicates (slower, involves a sort/distinct step). `UNION ALL` combines result sets keeping duplicates (faster).

**Real-life scenario:** build a mailing list from two overlapping groups — customers with a savings account and customers enrolled in the loyalty program (some customers are in both).

```sql
-- UNION: dedupes, so a customer in both groups appears once.
SELECT customer_id, email FROM savings_account_holders
UNION
SELECT customer_id, email FROM loyalty_program_members;

-- UNION ALL: keeps duplicates, so that same customer appears twice.
SELECT customer_id, email FROM savings_account_holders
UNION ALL
SELECT customer_id, email FROM loyalty_program_members;
```

For the mailing list you want `UNION` — nobody wants two copies of the same promotional email. But if you were instead counting "total number of (customer, program) memberships" for a report, `UNION ALL` is correct and faster, since you actually want every row counted, duplicates included.

## 20. What is a constraint? Name common types.
Rules enforced on table columns: `NOT NULL`, `UNIQUE`, `PRIMARY KEY`, `FOREIGN KEY`, `CHECK`, `DEFAULT`.

**Real-life scenario:** the `accounts` table encodes the bank's business rules directly in the schema, so bad data can never be inserted no matter which application writes to it.

```sql
CREATE TABLE accounts (
    account_id    BIGINT PRIMARY KEY,                       -- PRIMARY KEY: uniquely identifies each account
    account_number VARCHAR(20) NOT NULL UNIQUE,              -- NOT NULL + UNIQUE: every account needs a number, and no two accounts share one
    customer_id   BIGINT NOT NULL REFERENCES customers(customer_id), -- FOREIGN KEY: can't create an account for a customer that doesn't exist
    balance       DECIMAL(12,2) NOT NULL CHECK (balance >= 0), -- CHECK: balance can never go negative
    status        VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'       -- DEFAULT: new accounts are ACTIVE unless told otherwise
);
```

Try `INSERT INTO accounts (account_id, account_number, customer_id, balance) VALUES (1, 'ACC-001', 999, -50)` where customer `999` doesn't exist and the balance is negative — the database rejects it outright on both the foreign key and the check constraint, before it ever becomes an application bug.
