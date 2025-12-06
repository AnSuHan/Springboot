package com.tistory.glorygem.crawler.domain.repository;

import com.tistory.glorygem.crawler.domain.entity.Book;
import com.tistory.glorygem.crawler.domain.entity.BookCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {

    Optional<Book> findByUrl(String url);

    List<Book> findByBookCategory(BookCategory bookCategory);

    List<Book> findByBookCategory_CategoryName(String categoryName);

    boolean existsByUrl(String url);
}