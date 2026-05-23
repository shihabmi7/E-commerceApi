# MySQL Install (No Password) & Complete Uninstall Guide (macOS - Homebrew)

## 🧹 Complete Uninstall (Clean Removal)

### 1. Stop MySQL

```bash
brew services stop mysql
```

### 2. Uninstall MySQL

```bash
brew uninstall --force mysql
```

### 3. Remove All Data (⚠️ deletes everything)

```bash
rm -rf /opt/homebrew/var/mysql
rm -rf /opt/homebrew/etc/my.cnf
```

### 4. Cleanup

```bash
brew cleanup
```

### 5. Verify Removal

```bash
mysql --version
```

Expected:

```
command not found
```

---

## 🚀 Install MySQL (Without Password Setup)

### 1. Install via Homebrew

```bash
brew install mysql
```

### 2. Start MySQL

```bash
brew services start mysql
```

### 3. Login Without Password

```bash
mysql -u root
```

👉 By default (Homebrew), root login may work without password via socket.

---

## 🔧 Optional: Ensure No Password (Reset Mode)

If login fails, use safe mode:

### 1. Stop MySQL

```bash
brew services stop mysql
```

### 2. Start Safe Mode

```bash
mysqld_safe --skip-grant-tables --skip-networking &
```

### 3. Login

```bash
mysql -u root
```

### 4. Remove Password

```sql
FLUSH PRIVILEGES;
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '';
```

### 5. Restart Normally

```bash
killall mysqld
brew services start mysql
```

---

## 🧪 Test

```bash
mysql -u root
```

---

## ⚠️ Notes

* No-password setup is **NOT secure** (use only for local development)
* For production, always set a strong password
* Default socket path:

```bash
mysql_config --socket
```

---

## 👍 Recommended (Better Practice)

Instead of root without password:

```sql
CREATE USER 'dev'@'localhost' IDENTIFIED BY '';
GRANT ALL PRIVILEGES ON *.* TO 'dev'@'localhost';
FLUSH PRIVILEGES;
```

---

## 🎯 Done!

You now have:

* Clean uninstall steps
* Fresh install
* No-password login setup

---
