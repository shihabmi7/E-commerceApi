# PostgreSQL — Basic Interview Questions

## 1. Is PostgreSQL an object-relational database?
Yes. PostgreSQL is an **object-relational database (ORDBMS)**. It is a relational database (tables, rows, columns, SQL) with object-oriented features added.

**Object features PostgreSQL has:**
- **Custom data types:** you can create your own types.
- **Table inheritance:** a table can inherit columns from another table.
- **Arrays:** a column can hold an array of values.
- **Function overloading:** the same function name can work with different input types.
- **Rich types:** `JSONB`, ranges, and geometric types.

```sql
-- Custom type
CREATE TYPE address AS (street TEXT, city TEXT, zip TEXT);

CREATE TABLE customers (
    id      SERIAL PRIMARY KEY,
    name    TEXT,
    home    address,            -- custom type as a column
    phones  TEXT[]              -- array column
);

-- Table inheritance
CREATE TABLE vehicles (id SERIAL PRIMARY KEY, brand TEXT);
CREATE TABLE cars (doors INT) INHERITS (vehicles);
```

**Compared with MySQL:** MySQL is a plain relational database (RDBMS). It has a `JSON` column type, but it has no custom types, table inheritance, or arrays.

## 2. What is PostgreSQL and how is it different from MySQL?
PostgreSQL is an open-source, object-relational database known for strict standards compliance, advanced data types (JSONB, arrays, ranges), extensibility (custom types/functions), and strong support for complex queries. Compared to MySQL, it generally offers richer feature sets and better handling of complex/analytical workloads, while MySQL has historically been favored for simpler, high-throughput read-heavy web apps.

## 3. What is MVCC (Multi-Version Concurrency Control)?
PostgreSQL's mechanism for handling concurrent access: instead of locking rows for reads, each transaction sees a "snapshot" of the data as of its start, allowing readers and writers to avoid blocking each other.

## 4. What is a sequence in PostgreSQL?
A database object that generates unique numeric identifiers, commonly used to implement auto-incrementing primary keys (via `SERIAL`/`BIGSERIAL` or `GENERATED ... AS IDENTITY`).

## 5. What is the difference between `SERIAL` and `IDENTITY` columns?
`SERIAL` is a shorthand that creates a sequence and sets the column default to `nextval()`. `GENERATED AS IDENTITY` (SQL-standard, added in PG10) is the modern, safer approach with more control over whether values can be overridden.

## 6. What is JSONB and how does it differ from JSON?
Both store JSON data. `JSON` stores an exact text copy and re-parses it on read. `JSONB` stores data in a decomposed binary format — slightly slower to insert but much faster to query, and supports indexing (e.g., GIN indexes).

**JSON vs JSONB:**

| | `JSON` | `JSONB` |
|---|---|---|
| Storage | Exact text copy | Binary format |
| Insert speed | Faster (no conversion) | Slightly slower (converts to binary) |
| Query speed | Slow (re-parsed on every read) | Fast |
| Indexing | No | Yes (GIN index) |
| Whitespace | Kept as written | Removed |
| Key order | Kept as written | Not kept |
| Duplicate keys | All kept | Last one wins |
| Operators like `@>` and `?` | No | Yes |

**Example:**
```sql
CREATE TABLE products (
    id    SERIAL PRIMARY KEY,
    name  TEXT,
    attrs JSONB
);

INSERT INTO products (name, attrs)
VALUES ('T-shirt', '{"color": "red", "size": "M", "tags": ["sale", "new"]}');

SELECT attrs->>'color' FROM products;                  -- get one field as text
SELECT * FROM products WHERE attrs @> '{"color": "red"}';  -- filter by a value inside

-- Speed up the filter above with a GIN index
CREATE INDEX idx_products_attrs ON products USING GIN (attrs);
```

**Same input, different output:**
```sql
SELECT '{"b": 1,  "a": 2, "a": 3}'::json;    -- {"b": 1,  "a": 2, "a": 3}
SELECT '{"b": 1,  "a": 2, "a": 3}'::jsonb;   -- {"a": 3, "b": 1}
```

**When to use which:**
- Use `JSONB` in almost every case, because you can query and index it.
- Use `JSON` only when you must keep the exact original text (key order, spacing, duplicate keys), for example when you store raw API logs.

**Tip:** keep important columns like price and status as normal columns. Use `JSONB` for flexible data, like product attributes or settings.

## 7. What are PostgreSQL schemas?
A schema is like a folder inside one database. It groups tables and other objects, and it avoids name clashes: two schemas can each have a table with the same name. Think of it this way: database = the whole drive, schema = a folder, table = a file.

```sql
CREATE SCHEMA sales;
CREATE SCHEMA hr;

CREATE TABLE sales.orders (id SERIAL PRIMARY KEY, total NUMERIC);
CREATE TABLE hr.employees (id SERIAL PRIMARY KEY, name TEXT);

SELECT * FROM sales.orders;   -- use schema.table
SELECT * FROM orders;         -- no schema given: uses "public" (the default schema)
```

**Why use schemas:** organize tables, avoid name clashes, run multi-tenant apps (one schema per customer), and control permissions per schema.

## 8. What is a CTE (Common Table Expression)?
A named temporary result set defined with `WITH`, used to simplify complex queries and enable recursive queries.
```sql
WITH regional_sales AS (
    SELECT region, SUM(amount) AS total
    FROM orders
    GROUP BY region
)
SELECT * FROM regional_sales WHERE total > 10000;
```

## 9. What is the difference between `VACUUM` and `VACUUM FULL`?
**What is VACUUM:** PostgreSQL's cleaning job for tables. When you `UPDATE` or `DELETE` a row, the old row stays in the table as a "dead row" (because of MVCC). Dead rows pile up and make the table bloated. `VACUUM` cleans them.

| | `VACUUM` | `VACUUM FULL` |
|---|---|---|
| What it does | Marks dead-row space as reusable inside the table | Rewrites the whole table into a new compact file |
| Disk space back to the OS | No (mostly) | Yes |
| Locking | No blocking; reads and writes continue | Full table lock; no reads or writes |
| Speed | Fast | Slow on big tables |
| When to use | Regular cleanup (autovacuum does it) | Rare, in a maintenance window |

Think of a messy shelf: `VACUUM` removes old items and reuses the empty spots (same shelf size). `VACUUM FULL` repacks everything into a smaller shelf, but nobody can use the shelf meanwhile.

**Why use it:** stops table bloat, reuses space, keeps queries fast, and keeps PostgreSQL healthy (updates statistics, prevents transaction ID wraparound). PostgreSQL runs `autovacuum` automatically, so you rarely run plain `VACUUM` by hand.

```sql
VACUUM orders;
VACUUM (VERBOSE, ANALYZE) orders;
VACUUM FULL orders;   -- locks the table, use only when needed
```

## 10. What index types does PostgreSQL support?
B-Tree (default, general purpose), Hash, GIN (good for JSONB, arrays, full-text search), GiST (geometric/full-text data), BRIN (large, naturally ordered tables like time-series).

## 11. What is the difference between `TEXT` and `VARCHAR(n)` in PostgreSQL?
Functionally almost identical — both store variable-length strings. `VARCHAR(n)` enforces a length limit; `TEXT` has no limit. Unlike some databases, there's negligible performance difference between them in PostgreSQL.

## 12. What are PostgreSQL's window functions?
A window function calculates across a group of related rows but **keeps every row**. `GROUP BY` collapses rows into one; a window function does not.

Example table `employees`:

| name | dept | salary |
|---|---|---|
| Ali | IT | 5000 |
| Sara | IT | 7000 |
| John | IT | 7000 |
| Mia | HR | 4000 |
| Tom | HR | 6000 |

**1) `GROUP BY` (rows collapse, names are lost):**

```sql
SELECT dept, SUM(salary) FROM employees GROUP BY dept;
```

| dept | sum |
|---|---|
| IT | 19000 |
| HR | 10000 |

**2) Window function (all 5 rows stay, total shown next to each):**

```sql
SELECT name, dept, salary,
       SUM(salary) OVER (PARTITION BY dept) AS dept_total
FROM employees;
```

| name | dept | salary | dept_total |
|---|---|---|---|
| Ali | IT | 5000 | 19000 |
| Sara | IT | 7000 | 19000 |
| John | IT | 7000 | 19000 |
| Mia | HR | 4000 | 10000 |
| Tom | HR | 6000 | 10000 |

- `PARTITION BY dept` = do it separately for each department.
- `ORDER BY` inside `OVER` = order rows inside each department.

**3) Ranking inside each department (highest salary first):**

```sql
SELECT name, dept, salary,
  ROW_NUMBER() OVER (PARTITION BY dept ORDER BY salary DESC) AS row_num,
  RANK()       OVER (PARTITION BY dept ORDER BY salary DESC) AS rnk
FROM employees;
```

| name | dept | salary | row_num | rnk |
|---|---|---|---|---|
| Sara | IT | 7000 | 1 | 1 |
| John | IT | 7000 | 2 | 1 (tie: same RANK) |
| Ali | IT | 5000 | 3 | 3 (RANK skips 2) |
| Tom | HR | 6000 | 1 | 1 |
| Mia | HR | 4000 | 2 | 2 |

- `ROW_NUMBER` = always unique (1, 2, 3).
- `RANK` = ties share a number and the next number is skipped.

Interview use: "top earner per department" = wrap query 3 in a subquery and filter `WHERE rnk = 1`.

## 13. How does PostgreSQL handle full-text search?
Full-text search is like a "Google search" inside your database. Unlike `LIKE`, it understands word forms (searching "run" also finds "running") and skips small words like "the" and "is".

- `tsvector`: the text turned into searchable words.
- `tsquery`: what you search for.
- `@@`: checks if the text matches the search.
- **GIN index**: makes it fast on big tables (like the index at the back of a book).

```sql
SELECT * FROM articles
WHERE to_tsvector('english', body) @@ to_tsquery('english', 'fox & run');

CREATE INDEX idx_articles_fts ON articles USING GIN (to_tsvector('english', body));
```

## 14. What is the difference between `pg_dump` and physical backups?
`pg_dump` performs a logical backup (SQL statements or custom archive format) of one database, portable across versions. Physical backups (e.g., `pg_basebackup`, WAL archiving) copy the actual data files and are used for point-in-time recovery and replication.

---

## Practice: GROUP BY, HAVING, COUNT

Sample table `employees`:

| id | name | dept | salary | city |
|---|---|---|---|---|
| 1 | Ali | IT | 5000 | KL |
| 2 | Sara | IT | 7000 | Penang |
| 3 | John | IT | 7000 | KL |
| 4 | Mia | HR | 4000 | KL |
| 5 | Tom | HR | 6000 | Penang |
| 6 | Zed | Sales | 3000 | KL |
| 7 | Amy | Sales | 3500 | Johor |
| 8 | Raj | Sales | 4500 | KL |
| 9 | Eva | Sales | 3000 | Penang |

Reminders: `WHERE` filters rows before grouping. `HAVING` filters groups after grouping. `HAVING` needs the full expression (`COUNT(*) > 2`), not an alias.

**Questions**

- Q1. How many employees are there in total?
- Q2. What is the highest and the lowest salary?
- Q3. How many employees work in each department?
- Q4. Show each department with its total salary.
- Q5. Show each department with its average salary, highest first.
- Q6. Show each city with the number of employees in it.
- Q7. Show only the departments that have more than 2 employees.
- Q8. Show only the departments whose average salary is above 5000.
- Q9. Ignore employees in KL. Then show the number of employees per department.
- Q10. Ignore employees earning less than 3500. Then show only the departments with more than 1 person left.
- Q11. Which salary values appear more than once? Show the salary and how many times.
- Q12. Show each department and city together, with the number of employees for each pair.

**Answers**

```sql
-- Q1  -> 9
SELECT COUNT(*) FROM employees;

-- Q2  -> 7000, 3000
SELECT MAX(salary) AS high_sal, MIN(salary) AS low_sal FROM employees;

-- Q3  -> IT 3, HR 2, Sales 4
SELECT dept, COUNT(*) FROM employees GROUP BY dept;

-- Q4  -> IT 19000, HR 10000, Sales 14000
SELECT dept, SUM(salary) AS total_sal FROM employees GROUP BY dept;

-- Q5  -> IT 6333.33, HR 5000, Sales 3500
SELECT dept, AVG(salary) AS avg_sal
FROM employees GROUP BY dept
ORDER BY avg_sal DESC;            -- not ORDER BY salary

-- Q6  -> KL 5, Penang 3, Johor 1
SELECT city, COUNT(*) AS n FROM employees GROUP BY city;

-- Q7  -> IT 3, Sales 4
SELECT dept, COUNT(*) AS n
FROM employees GROUP BY dept
HAVING COUNT(*) > 2;              -- not HAVING n > 2

-- Q8  -> IT 6333.33 (HR is exactly 5000, so it is not above 5000)
SELECT dept, AVG(salary) AS avg_sal
FROM employees GROUP BY dept
HAVING AVG(salary) > 5000;

-- Q9  -> IT 1, HR 1, Sales 2
SELECT dept, COUNT(*) AS n
FROM employees
WHERE city <> 'KL'                -- != also works
GROUP BY dept;

-- Q10 -> IT 3, HR 2, Sales 2
SELECT dept, COUNT(*) AS n
FROM employees
WHERE salary >= 3500              -- "less than 3500 ignored" keeps 3500
GROUP BY dept
HAVING COUNT(*) > 1;

-- Q11 -> 7000 appears 2 times, 3000 appears 2 times
SELECT salary, COUNT(*) AS n
FROM employees GROUP BY salary
HAVING COUNT(*) > 1;

-- Q12 -> IT/KL 2, IT/Penang 1, HR/KL 1, HR/Penang 1,
--        Sales/KL 2, Sales/Johor 1, Sales/Penang 1
SELECT dept, city, COUNT(*) AS n
FROM employees GROUP BY dept, city;
```

Rules to remember:

- Every column in `SELECT` must be in `GROUP BY` or inside an aggregate (`COUNT`, `SUM`, `AVG`, `MAX`, `MIN`).
- `COUNT(*)` counts all rows. `COUNT(column)` skips `NULL` values.
- `ORDER BY` can use an alias. `WHERE` and `HAVING` cannot.
- Edge values: "more than 3500" is `> 3500`. "Less than 3500 ignored" is `>= 3500`.

---

## Practice: window functions

Sample table `employees` (small version):

| name | dept | salary |
|---|---|---|
| Ali | IT | 5000 |
| Sara | IT | 7000 |
| John | IT | 7000 |
| Mia | HR | 4000 |
| Tom | HR | 6000 |

**Questions**

- W1. Show every employee with the average salary of their department next to them.
- W2. Number all employees from 1 to 5, highest salary first.
- W3. Number employees inside each department, lowest salary first.
- W4. Show every employee with the total of all salaries in the company.
- W5. Show each employee with the salary of the person before them in the same department (ordered by salary).
- W6. Show only the top earner in each department.

**Answers**

```sql
-- W1  IT 6333.33, HR 5000 next to each person
SELECT *, AVG(salary) OVER (PARTITION BY dept) AS dept_avg
FROM employees;

-- W2  unique 1..5 (RANK would give ties the same number)
SELECT *, ROW_NUMBER() OVER (ORDER BY salary DESC) AS n
FROM employees;

-- W3  numbering restarts at 1 for each department
SELECT *, ROW_NUMBER() OVER (PARTITION BY dept ORDER BY salary ASC) AS n
FROM employees;

-- W4  every row shows 29000 (empty OVER () = whole table is one group)
SELECT *, SUM(salary) OVER () AS tot_salary
FROM employees;

-- W5  first person in each department gets NULL
SELECT *, LAG(salary) OVER (PARTITION BY dept ORDER BY salary) AS prev_salary
FROM employees;

-- W6  IT: Sara and John (tie at 7000), HR: Tom
SELECT name, dept, salary
FROM (
  SELECT name, dept, salary,
         RANK() OVER (PARTITION BY dept ORDER BY salary DESC) AS rnk
  FROM employees
) t
WHERE rnk = 1;                    -- WHERE cannot use a window function directly
```

Rule to remember: function + `OVER (...)` = window function. No `OVER` means a normal aggregate that collapses rows.
