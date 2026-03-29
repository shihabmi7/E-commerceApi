# One-to-One Relationship in Spring Boot: Complete Guide

---

## Table of Contents
1. [Overview](#1-overview)
2. [Unidirectional One-to-One](#2-unidirectional-one-to-one)
    - [Entity Classes](#21-entity-classes)
    - [Repository Layer](#22-repository-layer)
    - [Example Queries](#23-example-queries)
3. [Bidirectional One-to-One](#3-bidirectional-one-to-one)
    - [Entity Classes](#31-entity-classes)
    - [Example Queries](#32-example-queries)
4. [Key Notes](#4-key-notes)
5. [Database Schema](#5-database-schema)
6. [Comparison: Unidirectional vs. Bidirectional](#6-comparison-unidirectional-vs-bidirectional)

---

## 1. Overview
A one-to-one relationship in Spring Boot using JPA/Hibernate links each record in one table to exactly one record in another. This guide provides full implementations for both **unidirectional** and **bidirectional** relationships, including all entity classes, repositories, and queries.

---

## 2. Unidirectional One-to-One

### 2.1 Entity Classes

#### User.java
```java
package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private Address address;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }
}
```

#### Address.java
```java
package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "addresses")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String street;
    private String city;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
}
```

---

### 2.2 Repository Layer

#### UserRepository.java
```java
package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {}
```

#### AddressRepository.java
```java
package com.example.demo.repository;

import com.example.demo.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {}
```

---

### 2.3 Example Queries

#### Save User with Address
```java
User user = new User();
user.setName("John Doe");

Address address = new Address();
address.setStreet("123 Main St");
address.setCity("New York");

user.setAddress(address);
userRepository.save(user);
```

#### Fetch User with Address
```java
User user = userRepository.findById(1L).orElse(null);
if (user != null) {
    System.out.println("User: " + user.getName());
    System.out.println("Address: " + user.getAddress().getStreet() + ", " + user.getAddress().getCity());
}
```

---

## 3. Bidirectional One-to-One

### 3.1 Entity Classes

#### User.java
```java
package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private Address address;

    // Getters and Setters (same as above)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }
}
```

#### Address.java
```java
package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "addresses")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String street;
    private String city;

    @OneToOne(mappedBy = "address")
    private User user;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
```

---

### 3.2 Example Queries

#### Save User with Address (Bidirectional)
```java
User user = new User();
user.setName("Jane Smith");

Address address = new Address();
address.setStreet("456 Oak Ave");
address.setCity("Los Angeles");

user.setAddress(address);
address.setUser(user);

userRepository.save(user);
```

#### Fetch Address with User
```java
Address address = addressRepository.findById(1L).orElse(null);
if (address != null) {
    System.out.println("Address: " + address.getStreet() + ", " + address.getCity());
    System.out.println("User: " + address.getUser().getName());
}
```

---

## 4. Key Notes
- **CascadeType.ALL**: Automatically persists, updates, and deletes related entities.
- **@JoinColumn**: Defines the foreign key in the `users` table.
- **mappedBy**: Indicates the inverse side of the relationship in bidirectional mapping.
- **Bidirectional**: Requires setting both sides (`user.setAddress(address)` and `address.setUser(user)`).

---

## 5. Database Schema
- The `users` table contains an `address_id` column as a foreign key to the `addresses` table.

---

## 6. Comparison: Unidirectional vs. Bidirectional

| Feature               | Unidirectional                          | Bidirectional                          |
|-----------------------|-----------------------------------------|-----------------------------------------|
| Navigation            | User → Address                          | User ↔ Address                          |
| Foreign Key           | In `users` table                        | In `users` table                        |
| `mappedBy` Required    | No                                      | Yes                                     |
| Use Case              | Simple, one-way access                  | Complex, two-way access                 |
| Code Complexity       | Lower                                   | Higher                                  |
| Performance           | Slightly better (no inverse mapping)    | Slightly worse (inverse mapping)        |
| Example Use Case      | User profile with an address            | Address book with user details          |
