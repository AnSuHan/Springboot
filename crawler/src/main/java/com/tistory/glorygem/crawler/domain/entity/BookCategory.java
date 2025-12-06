package com.tistory.glorygem.crawler.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "BookCategory")
public class BookCategory {

    // Getters/Setters
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid_bookCategory", columnDefinition = "UUID")
    private UUID uuidBookCategory = UUID.randomUUID();

    @Column(name = "category_name", nullable = false)
    private String categoryName;
}
