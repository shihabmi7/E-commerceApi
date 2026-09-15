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
Row-level locking (InnoDB) locks only the specific rows being modified, allowing higher concurrency. Table-level locking (MyISAM) locks the entire table for a write, blocking other reads/writes until released.

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

## 13. How do you back up and restore a MySQL database?
Common tools: `mysqldump` (logical backup, e.g., `mysqldump -u user -p dbname > backup.sql`) and restoring with `mysql -u user -p dbname < backup.sql`. For larger databases, physical tools like Percona XtraBackup are used.
