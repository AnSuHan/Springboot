package com.tistory.glorygem.crawler.service;

import com.tistory.glorygem.crawler.domain.entity.BookCategory;
import com.tistory.glorygem.crawler.domain.repository.BookCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookCategoryService {

    private static final String BASE_URL = "https://books.toscrape.com";
    private static final int TIMEOUT = 10000;

    private final BookCategoryRepository bookCategoryRepository;

    /**
     * 모든 카테고리 정보를 크롤링하여 DB에 저장합니다
     */
    @Transactional
    public List<BookCategory> crawlAndSaveCategories() throws IOException {
        log.info("카테고리 크롤링 시작");
        List<BookCategory> categories = new ArrayList<>();

        Document doc = Jsoup.connect(BASE_URL)
                .timeout(TIMEOUT)
                .userAgent("Mozilla/5.0")
                .get();

        Elements categoryElements = doc.select("div.side_categories ul.nav-list li ul li a");

        for (Element element : categoryElements) {
            String categoryName = element.text().trim();

            // 중복 체크
            if (!bookCategoryRepository.existsByCategoryName(categoryName)) {
                BookCategory category = new BookCategory();
                category.setCategoryName(categoryName);

                BookCategory savedCategory = bookCategoryRepository.save(category);
                categories.add(savedCategory);
                log.info("새 카테고리 저장: {}", categoryName);
            } else {
                BookCategory existingCategory = bookCategoryRepository
                        .findByCategoryName(categoryName)
                        .orElseThrow();
                categories.add(existingCategory);
                log.info("기존 카테고리 발견: {}", categoryName);
            }
        }

        log.info("총 {} 개의 카테고리 처리 완료", categories.size());
        return categories;
    }

    /**
     * 모든 카테고리 조회
     */
    @Transactional(readOnly = true)
    public List<BookCategory> getAllCategories() {
        return bookCategoryRepository.findAll();
    }

    /**
     * 카테고리명으로 조회
     */
    @Transactional(readOnly = true)
    public BookCategory getCategoryByName(String categoryName) {
        return bookCategoryRepository.findByCategoryName(categoryName)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다: " + categoryName));
    }

    /**
     * 카테고리가 존재하는지 확인
     */
    @Transactional(readOnly = true)
    public boolean existsCategory(String categoryName) {
        return bookCategoryRepository.existsByCategoryName(categoryName);
    }
}