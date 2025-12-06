package com.tistory.glorygem.crawler.controller;

import com.tistory.glorygem.crawler.domain.dto.BookDTO;
import com.tistory.glorygem.crawler.service.BookService;
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
@RequestMapping("/api/books")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BookController {

    private final BookService bookService;

    /**
     * 특정 카테고리의 책 크롤링 및 저장
     */
    @PostMapping("/crawl/category/{categoryName}")
    public ResponseEntity<?> crawlBooksByCategory(@PathVariable String categoryName) {
        try {
            var books = bookService.crawlAndSaveBooksByCategory(categoryName);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "책 크롤링 완료");
            response.put("category", categoryName);
            response.put("count", books.size());

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("책 크롤링 실패: {}", categoryName, e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("책 크롤링에 실패했습니다: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("존재하지 않는 카테고리: {}", categoryName);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 모든 카테고리의 책 크롤링 및 저장 (전체 크롤링)
     */
    @PostMapping("/crawl/all")
    public ResponseEntity<?> crawlAllBooks() {
        try {
            log.info("전체 책 크롤링 시작");
            var books = bookService.crawlAndSaveAllBooks();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "전체 크롤링 완료");
            response.put("totalCount", books.size());

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("전체 크롤링 실패", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("크롤링에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 저장된 모든 책 조회
     */
    @GetMapping
    public ResponseEntity<?> getAllBooks() {
        try {
            List<BookDTO> books = bookService.getAllBooks();

            Map<String, Object> response = new HashMap<>();
            response.put("count", books.size());
            response.put("books", books);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("책 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("책 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 카테고리별 책 조회
     */
    @GetMapping("/category/{categoryName}")
    public ResponseEntity<?> getBooksByCategory(@PathVariable String categoryName) {
        try {
            List<BookDTO> books = bookService.getBooksByCategory(categoryName);

            Map<String, Object> response = new HashMap<>();
            response.put("category", categoryName);
            response.put("count", books.size());
            response.put("books", books);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("카테고리별 책 조회 실패: {}", categoryName, e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("책 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 책 개수 조회 (전체)
     */
    @GetMapping("/count")
    public ResponseEntity<?> getBookCount() {
        try {
            int count = bookService.getAllBooks().size();

            Map<String, Object> response = new HashMap<>();
            response.put("totalCount", count);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("책 개수 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("개수 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 카테고리별 책 개수 조회
     */
    @GetMapping("/count/category/{categoryName}")
    public ResponseEntity<?> getBookCountByCategory(@PathVariable String categoryName) {
        try {
            int count = bookService.getBooksByCategory(categoryName).size();

            Map<String, Object> response = new HashMap<>();
            response.put("category", categoryName);
            response.put("count", count);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("카테고리별 책 개수 조회 실패: {}", categoryName, e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("개수 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 크롤링 상태 확인 (통합 정보)
     */
    @GetMapping("/status")
    public ResponseEntity<?> getCrawlingStatus() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "running");
            response.put("totalBooks", bookService.getAllBooks().size());
            response.put("message", "크롤링 시스템이 정상 작동 중입니다");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("상태 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("상태 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}