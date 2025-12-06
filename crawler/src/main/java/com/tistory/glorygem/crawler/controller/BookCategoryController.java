package com.tistory.glorygem.crawler.controller;

import com.tistory.glorygem.crawler.domain.entity.BookCategory;
import com.tistory.glorygem.crawler.service.BookCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BookCategoryController {

    private final BookCategoryService bookCategoryService;

    /**
     * 카테고리 크롤링 및 저장
     */
    @PostMapping("/crawl")
    public ResponseEntity<?> crawlCategories() {
        try {
            List<BookCategory> categories = bookCategoryService.crawlAndSaveCategories();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "카테고리 크롤링 완료");
            response.put("count", categories.size());
            response.put("categories", categories);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("카테고리 크롤링 실패", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("카테고리 크롤링에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 저장된 모든 카테고리 조회
     */
    @GetMapping
    public ResponseEntity<?> getAllCategories() {
        try {
            List<BookCategory> categories = bookCategoryService.getAllCategories();

            Map<String, Object> response = new HashMap<>();
            response.put("count", categories.size());
            response.put("categories", categories);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("카테고리 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("카테고리 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 특정 카테고리 조회 (카테고리명으로)
     */
    @GetMapping("/{categoryName}")
    public ResponseEntity<?> getCategoryByName(@PathVariable String categoryName) {
        try {
            BookCategory category = bookCategoryService.getCategoryByName(categoryName);

            Map<String, Object> response = new HashMap<>();
            response.put("category", category);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("카테고리를 찾을 수 없음: {}", categoryName);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("카테고리 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("카테고리 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 카테고리 존재 여부 확인
     */
    @GetMapping("/exists/{categoryName}")
    public ResponseEntity<?> checkCategoryExists(@PathVariable String categoryName) {
        try {
            boolean exists = bookCategoryService.existsCategory(categoryName);

            Map<String, Object> response = new HashMap<>();
            response.put("categoryName", categoryName);
            response.put("exists", exists);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("카테고리 존재 여부 확인 실패", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("확인에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 카테고리 개수 조회
     */
    @GetMapping("/count")
    public ResponseEntity<?> getCategoryCount() {
        try {
            int count = bookCategoryService.getAllCategories().size();

            Map<String, Object> response = new HashMap<>();
            response.put("count", count);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("카테고리 개수 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("개수 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}