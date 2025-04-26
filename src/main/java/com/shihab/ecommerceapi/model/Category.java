package com.shihab.ecommerceapi.model;

import lombok.*;

import jakarta.persistence.*;


@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String description;

    // Getters and setters...
}