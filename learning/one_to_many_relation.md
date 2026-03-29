# One-to-Many Relationship in Spring Boot: University and Student Example

---

## Table of Contents
1. [Overview](#1-overview)
2. [Unidirectional One-to-Many](#2-unidirectional-one-to-many)
    - [Entity Classes](#21-entity-classes)
    - [Repository Layer](#22-repository-layer)
    - [Example Queries](#23-example-queries)
3. [Bidirectional One-to-Many](#3-bidirectional-one-to-many)
    - [Entity Classes](#31-entity-classes)
    - [Example Queries](#32-example-queries)
4. [Key Notes](#4-key-notes)
5. [Database Schema](#5-database-schema)
6. [Comparison: Unidirectional vs. Bidirectional](#6-comparison-unidirectional-vs-bidirectional)

---

## 1. Overview
A one-to-many relationship in Spring Boot using JPA/Hibernate links one record in a table to multiple records in another table. This guide uses a **University and Student** example to demonstrate both **unidirectional** and **bidirectional** implementations.

---

## 2. Unidirectional One-to-Many

### 2.1 Entity Classes

#### University.java
```java
package com.example.demo.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "universities")
public class University {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "university_id")
    private List<Student> students = new ArrayList<>();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Student> getStudents() { return students; }
    public void setStudents(List<Student> students) { this.students = students; }
}
```

#### Student.java
```java
package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

---

### 2.2 Repository Layer

#### UniversityRepository.java
```java
package com.example.demo.repository;

import com.example.demo.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityRepository extends JpaRepository<University, Long> {}
```

#### StudentRepository.java
```java
package com.example.demo.repository;

import com.example.demo.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {}
```

---

### 2.3 Example Queries

#### Save University with Students
```java
University university = new University();
university.setName("Harvard University");

Student student1 = new Student();
student1.setName("Alice");
student1.setEmail("alice@example.com");

Student student2 = new Student();
student2.setName("Bob");
student2.setEmail("bob@example.com");

university.getStudents().add(student1);
university.getStudents().add(student2);

universityRepository.save(university);
```

#### Fetch University with Students
```java
University university = universityRepository.findById(1L).orElse(null);
if (university != null) {
    System.out.println("University: " + university.getName());
    university.getStudents().forEach(student ->
        System.out.println("Student: " + student.getName() + ", Email: " + student.getEmail())
    );
}
```

---

## 3. Bidirectional One-to-Many

### 3.1 Entity Classes

#### University.java
```java
package com.example.demo.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "universities")
public class University {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "university", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Student> students = new ArrayList<>();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Student> getStudents() { return students; }
    public void setStudents(List<Student> students) { this.students = students; }
}
```

#### Student.java
```java
package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    @ManyToOne
    @JoinColumn(name = "university_id")
    private University university;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public University getUniversity() { return university; }
    public void setUniversity(University university) { this.university = university; }
}
```

---

### 3.2 Example Queries

#### Save University with Students (Bidirectional)
```java
University university = new University();
university.setName("Stanford University");

Student student1 = new Student();
student1.setName("Charlie");
student1.setEmail("charlie@example.com");
student1.setUniversity(university);

Student student2 = new Student();
student2.setName("Diana");
student2.setEmail("diana@example.com");
student2.setUniversity(university);

university.getStudents().add(student1);
university.getStudents().add(student2);

universityRepository.save(university);
```

#### Fetch Student with University
```java
Student student = studentRepository.findById(1L).orElse(null);
if (student != null) {
    System.out.println("Student: " + student.getName());
    System.out.println("University: " + student.getUniversity().getName());
}
```

---

## 4. Key Notes
- **CascadeType.ALL**: Automatically persists, updates, and deletes related entities.
- **@JoinColumn**: Defines the foreign key in the `students` table.
- **mappedBy**: Indicates the inverse side of the relationship in bidirectional mapping.
- **Bidirectional**: Requires setting both sides (`student.setUniversity(university)` and `university.getStudents().add(student)`).

---

## 5. Database Schema
- The `students` table contains a `university_id` column as a foreign key to the `universities` table.

---

## 6. Comparison: Unidirectional vs. Bidirectional

| Feature               | Unidirectional                          | Bidirectional                          |
|-----------------------|-----------------------------------------|-----------------------------------------|
| Navigation            | University → Students                   | University ↔ Students                   |
| Foreign Key           | In `students` table                     | In `students` table                     |
| `mappedBy` Required    | No                                      | Yes                                     |
| Use Case              | Simple, one-way access                  | Complex, two-way access                 |
| Code Complexity       | Lower                                   | Higher                                  |
| Performance           | Slightly better (no inverse mapping)    | Slightly worse (inverse mapping)        |
| Example Use Case      | University managing students             | Student portal with university details  |
