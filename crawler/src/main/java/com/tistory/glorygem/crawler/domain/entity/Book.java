package com.tistory.glorygem.crawler.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Book")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid_book", columnDefinition = "UUID")
    private UUID uuidBook;

    @Column(name = "title", nullable = false)
    private String title;  // "It's Only the Himalayas"

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;  // £45.17

    @Column(name = "upc")
    private String upc;  // "a22124811bfa8350"

    @Column(name = "product_type")
    private String productType;  // "Books"

    @Column(name = "availability")
    private String availability;  // "In stock (19 available)"

    @Column(name = "number_of_reviews")
    private Integer numberOfReviews;  // 0

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;  // 긴 상품 설명

    @Column(name = "url", length = 500)
    private String url;  // "https://books.toscrape.com/catalogue/its-only-the-himalayas_981/index.html"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid_bookCategory")
    private BookCategory bookCategory;
}
