# MySQL — Basic Interview Questions

## 1. What are the storage engines in MySQL? What's the difference between InnoDB and MyISAM?
- **InnoDB** (default since 5.5): supports transactions, foreign keys, row-level locking, crash recovery.
- **MyISAM**: no transaction support, table-level locking, faster for read-heavy workloads but less safe (no crash recovery, no foreign keys).

## 2. What is the difference between CHAR and VARCHAR?
`CHAR(n)` is fixed-length, padded with spaces; faster for fixed-size data. `VARCHAR(n)` is variable-length, stores only the actual characters plus a length prefix; saves space for variable-size data.

## 3. What is AUTO_INCREMENT?
An attribute applied to a column (usually the primary key) that automatically generates a unique, incrementing integer value for each new row.

## 4. What is the difference between MySQL's `NOW()` and `CURDATE()`?
`NOW()` returns the current date and time. `CURDATE()` returns only the current date.

## 5. How do you find duplicate rows in a MySQL table?
```sql
SELECT column_name, COUNT(*)
FROM table_name
GROUP BY column_name
HAVING COUNT(*) > 1;
```

## 6. What is the difference between InnoDB row-level locking and table-level locking?
Row-level locking (InnoDB) locks only the specific rows being modified, allowing higher concurrency. Table-level locking (MyISAM, or an explicit `LOCK TABLES`) locks the entire table for a write, blocking other reads/writes until released.

| | Row-level (InnoDB) | Table-level (MyISAM / `LOCK TABLES`) |
|---|---|---|
| Concurrency | High: many users can write to different rows at once | Low: one writer at a time |
| Overhead | Higher: MySQL tracks many locks | Very low: one lock per table |
| Deadlock risk | Possible | Rare |
| Transactions | Supported | Not supported in MyISAM |

**Why it matters:** with row-level locking, two customers buying different products update different rows, so neither waits. With table-level locking, the second customer waits until the first write finishes, which gets slow when many users write at the same time.

**When to use row-level locking:** busy apps with many concurrent writes (orders, payments, stock updates). InnoDB is the default engine, so this is what you get automatically.

**When to use table-level locking:**
- Bulk imports or full-table updates, where one lock is cheaper than millions of row locks.
- Read-heavy tables that almost never change.
- Maintenance tasks where nobody else should touch the table.

```sql
-- Row-level: locks only the row with id = 1
START TRANSACTION;
SELECT * FROM products WHERE id = 1 FOR UPDATE;
UPDATE products SET stock = stock - 1 WHERE id = 1;
COMMIT;

-- Table-level: locks the whole table
LOCK TABLES products WRITE;
-- ... do the bulk work ...
UNLOCK TABLES;
```

**Gotcha:** InnoDB locks rows through indexes. If the `WHERE` column has no index, InnoDB may lock many rows, which acts almost like a table lock. Always index the columns you filter on.

## 7. What is a MySQL EXPLAIN plan used for?
It shows how MySQL executes a query — which indexes are used, join order, estimated rows scanned — helping identify performance bottlenecks.

## 8. What is replication in MySQL?
Copying data from a master (source) server to one or more replica servers, used for read scaling, backups, and high availability. Can be statement-based, row-based, or mixed replication.

## 9. What is the difference between `INT`, `BIGINT`, and `TINYINT`?
They differ in storage size and range: `TINYINT` (1 byte, -128 to 127 or 0-255 unsigned), `INT` (4 bytes), `BIGINT` (8 bytes, for very large numbers).

## 10. How does MySQL handle full-text search?
Via `FULLTEXT` indexes on `CHAR`/`VARCHAR`/`TEXT` columns, queried with `MATCH() AGAINST()`, supporting natural language and boolean search modes.

## 11. What is the difference between `COMMIT` and `ROLLBACK`?
`COMMIT` permanently saves all changes made in the current transaction. `ROLLBACK` undoes all changes made in the current transaction since the last commit.

## 12. What are MySQL's ENUM and SET types?
`ENUM` stores one value from a predefined list of allowed values. `SET` can store zero or more values from a predefined list, stored as a bitmap.

**ENUM example (one value only):**
```sql
CREATE TABLE orders (
    id     INT PRIMARY KEY AUTO_INCREMENT,
    status ENUM('PENDING', 'PAID', 'SHIPPED', 'CANCELLED') NOT NULL DEFAULT 'PENDING'
);

INSERT INTO orders (status) VALUES ('PAID');       -- OK
INSERT INTO orders (status) VALUES ('REFUNDED');   -- error in strict mode: not in the list
```

**SET example (many values allowed):**
```sql
CREATE TABLE users (
    id    INT PRIMARY KEY AUTO_INCREMENT,
    roles SET('USER', 'ADMIN', 'SELLER', 'SUPPORT')
);

INSERT INTO users (roles) VALUES ('USER');               -- one value
INSERT INTO users (roles) VALUES ('USER,SELLER');        -- two values
INSERT INTO users (roles) VALUES ('');                   -- empty set is allowed

SELECT * FROM users WHERE FIND_IN_SET('ADMIN', roles);   -- users who have ADMIN
```

**Key points:**
- `ENUM` is stored as a number internally (1 byte for up to 255 values), so it is small and fast.
- `ENUM` sorts by the order in the list, not alphabetically.
- `SET` holds up to 64 values.
- Changing the list later needs an `ALTER TABLE`, which can be slow on big tables.
- Many teams prefer a lookup table (like `order_status`) or a plain `VARCHAR` with a check, because those are easier to change.

## 13. How do you back up and restore a MySQL database?
Common tools: `mysqldump` (logical backup, e.g., `mysqldump -u user -p dbname > backup.sql`) and restoring with `mysql -u user -p dbname < backup.sql`. For larger databases, physical tools like Percona XtraBackup are used.

## 14. Should you store images in the database?
**Short answer:** usually no. Store the image file outside the database (disk or object storage like S3) and keep only the path/URL and metadata (name, size, type) in the database.

**Why not store images in the DB (as `BLOB`):**
- The database gets big fast, so backups, restores, and replication get slow.
- Image bytes fill the buffer pool (memory cache), pushing out the hot rows your queries need.
- Every image read goes through the DB connection and your app, instead of a CDN or web server.
- No easy browser caching or CDN delivery.
- Large rows slow down queries on that table, and big files can hit `max_allowed_packet`.

**When storing in the DB is fine:**
- Very small images (icons, thumbnails, under about 100 KB).
- You need the image and its row to be saved or rolled back together in one transaction.
- A small internal app where simplicity matters more than scale.

BLOB types in MySQL: `BLOB` (64 KB), `MEDIUMBLOB` (16 MB), `LONGBLOB` (4 GB).

**Recommended way: file storage + path in the DB**
```java
// Save the file to disk (or upload to S3), then store only the path
Path target = Paths.get("/data/uploads", UUID.randomUUID() + ".jpg");
Files.copy(file.getInputStream(), target);

product.setImagePath(target.toString());   // VARCHAR column in the DB
productRepository.save(product);
```

**Rule of thumb:** store images in object storage (or on disk) and keep only the URL or path in the database.
