# PostgreSQL Docker Commands

## List Docker Containers
```bash
docker ps
```

## Enter PostgreSQL Container
```bash
docker exec -it <container_name_or_id> bash
```

## Access PostgreSQL Shell
```bash
psql -U postgres
```

## Show All Databases
```sql
\l
```

## Enter a Database
```sql
\c mydatabase
```

## Show All Tables
```sql
\dt
```

## Show Schema of a Table
```sql
\d+ mytable
```

## Toggle Expanded View
```sql
\x
```

## Switch Databases
```sql
\c otherdb
```

## Clear Screen
```sql
\! clear
```

## Exit PostgreSQL Shell
```sql
\q
```

## Exit Container Shell
```bash
exit
```

## View PostgreSQL Logs (Inside Container)
```bash
cat /var/log/postgresql/postgresql-*.log
```

---
**Summary Table**

| Action                          | Command                                      |
|---------------------------------|----------------------------------------------|
| List Docker containers          | `docker ps`                                  |
| Enter PostgreSQL container       | `docker exec -it <container_name_or_id> bash` |
| Access PostgreSQL shell          | `psql -U postgres`                            |
| List all databases              | `\l`                                         |
| Connect to a database           | `\c mydatabase`                              |
| List tables                     | `\dt`                                        |
| Show table schema               | `\d+ mytable`                                |
| Toggle expanded view            | `\x`                                         |
| Switch databases                | `\c otherdb`                                 |
| Clear screen                    | `\! clear`                                   |
| Exit PostgreSQL shell           | `\q`                                         |
| Exit container shell            | `exit`                                       |
| View PostgreSQL logs             | `cat /var/log/postgresql/postgresql-*.log`   |
