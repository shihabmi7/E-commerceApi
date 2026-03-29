# Many-to-Many Relationship in Spring Boot: Course and Student Example

---

## Table of Contents
1. [Overview](#1-overview)
2. [Unidirectional Many-to-Many](#2-unidirectional-many-to-many)
    - [Entity Classes](#21-entity-classes)
    - [Repository Layer](#22-repository-layer)
    - [Example Queries](#23-example-queries)
3. [Bidirectional Many-to-Many](#3-bidirectional-many-to-many)
    - [Entity Classes](#31-entity-classes)
    - [Example Queries](#32-example-queries)
4. [Key Notes](#4-key-notes)
5. [Database Schema](#5-database-schema)
6. [Comparison: Unidirectional vs. Bidirectional](#6-comparison-unidirectional-vs-bidirectional)

---

## 1. Overview
A many-to-many relationship in Spring Boot using JPA/Hibernate links multiple records in one table to multiple records in another table. This guide uses a **Course and Student** example to demonstrate both **unidirectional** and **bidirectional** implementations.

---

## 2. Unidirectional Many-to-Many

### 2.1 Entity Classes

#### Student.java
```java
package com.example.demo.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> courses = new HashSet<>();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Set<Course> getCourses() { return courses; }
    public void setCourses(Set<Course> courses) { this.courses = courses; }
}
```

#### Course.java
```java
package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String code;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
```

---

### 2.2 Repository Layer

#### StudentRepository.java
```java
package com.example.demo.repository;

import com.example.demo.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {}
```

#### CourseRepository.java
```java
package com.example.demo.repository;

import com.example.demo.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {}
```

---

### 2.3 Example Queries

#### Save Student with Courses
```java
Student student = new Student();
student.setName("Alice");
student.setEmail("alice@example.com");

Course course1 = new Course();
course1.setTitle("Mathematics");
course1.setCode("MATH101");

Course course2 = new Course();
course2.setTitle("Physics");
course2.setCode("PHYS101");

student.getCourses().add(course1);
student.getCourses().add(course2);

studentRepository.save(student);
```

#### Fetch Student with Courses
```java
Student student = studentRepository.findById(1L).orElse(null);
if (student != null) {
    System.out.println("Student: " + student.getName());
    student.getCourses().forEach(course ->
        System.out.println("Course: " + course.getTitle() + ", Code: " + course.getCode())
    );
}
```

---

## 3. Bidirectional Many-to-Many

### 3.1 Entity Classes

#### Student.java
```java
package com.example.demo.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> courses = new HashSet<>();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Set<Course> getCourses() { return courses; }
    public void setCourses(Set<Course> courses) { this.courses = courses; }
}
```

#### Course.java
```java
package com.example.demo.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String code;

    @ManyToMany(mappedBy = "courses")
    private Set<Student> students = new HashSet<>();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Set<Student> getStudents() { return students; }
    public void setStudents(Set<Student> students) { this.students = students; }
}
```

---

### 3.2 Example Queries

#### Save Student with Courses (Bidirectional)
```java
Student student = new Student();
student.setName("Bob");
student.setEmail("bob@example.com");

Course course1 = new Course();
course1.setTitle("Chemistry");
course1.setCode("CHEM101");

Course course2 = new Course();
course2.setTitle("Biology");
course2.setCode("BIO101");

student.getCourses().add(course1);
student.getCourses().add(course2);

course1.getStudents().add(student);
course2.getStudents().add(student);

studentRepository.save(student);
```

#### Fetch Course with Students
```java
Course course = courseRepository.findById(1L).orElse(null);
if (course != null) {
    System.out.println("Course: " + course.getTitle());
    course.getStudents().forEach(student ->
        System.out.println("Student: " + student.getName() + ", Email: " + student.getEmail())
    );
}
```

---

## 4. Key Notes
- **`@JoinTable`**: Defines the join table for the many-to-many relationship.
- **`joinColumns`**: Specifies the foreign key column for the current entity (`Student`).
- **`inverseJoinColumns`**: Specifies the foreign key column for the associated entity (`Course`).
- **Bidirectional**: Requires setting both sides of the relationship (`student.getCourses().add(course)` and `course.getStudents().add(student)`).

---

## 5. Database Schema
- A **join table** named `student_course` is created with two columns:
    - `student_id`: Foreign key to the `students` table.
    - `course_id`: Foreign key to the `courses` table.

---

## 6. Comparison: Unidirectional vs. Bidirectional

| Feature               | Unidirectional                          | Bidirectional                          |
|-----------------------|-----------------------------------------|-----------------------------------------|
| Navigation            | Student → Courses                      | Student ↔ Courses                      |
| Join Table             | Yes                                     | Yes                                     |
| `mappedBy` Required    | No                                      | Yes                                     |
| Use Case              | Simple, one-way access                  | Complex, two-way access                 |
| Code Complexity       | Lower                                   | Higher                                  |
| Performance           | Slightly better (no inverse mapping)    | Slightly worse (inverse mapping)        |
| Example Use Case      | Student enrolling in courses            | Course roster with student details      |
