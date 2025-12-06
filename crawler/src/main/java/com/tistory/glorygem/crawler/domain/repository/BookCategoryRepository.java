package com.tistory.glorygem.crawler.domain.repository;

import com.tistory.glorygem.crawler.domain.entity.BookCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookCategoryRepository extends JpaRepository<BookCategory, UUID> {

    Optional<BookCategory> findByCategoryName(String categoryName);

    boolean existsByCategoryName(String categoryName);
}