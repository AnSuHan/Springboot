package com.tistory.glorygem.crawler.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {
    private String title;
    private BigDecimal price;
    private String upc;
    private String productType;
    private String availability;
    private Integer numberOfReviews;
    private String description;
    private String url;
    private String categoryName;
}