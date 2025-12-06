package com.tistory.glorygem.crawler.service;

import com.tistory.glorygem.crawler.domain.entity.Book;
import com.tistory.glorygem.crawler.domain.entity.BookCategory;
import com.tistory.glorygem.crawler.domain.repository.BookRepository;
import com.tistory.glorygem.crawler.domain.dto.BookDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private static final String BASE_URL = "https://books.toscrape.com";
    private static final int TIMEOUT = 10000;

    private final BookRepository bookRepository;
    private final BookCategoryService bookCategoryService;

    /**
     * 특정 카테고리의 모든 책을 크롤링하여 저장합니다
     */
    @Transactional
    public List<Book> crawlAndSaveBooksByCategory(String categoryName) throws IOException {
        log.info("카테고리 '{}' 책 크롤링 시작", categoryName);

        BookCategory bookCategory = bookCategoryService.getCategoryByName(categoryName);
        List<Book> books = new ArrayList<>();

        // 카테고리 URL 생성 (실제로는 카테고리 엔티티에 URL을 저장하거나 매핑 로직 필요)
        String categoryUrl = getCategoryUrl(categoryName);
        String currentUrl = categoryUrl;

        while (currentUrl != null) {
            Document doc = Jsoup.connect(currentUrl)
                    .timeout(TIMEOUT)
                    .userAgent("Mozilla/5.0")
                    .get();

            Elements bookElements = doc.select("article.product_pod");

            for (Element bookElement : bookElements) {
                String bookUrl = extractBookUrl(bookElement);

                // 중복 체크
                if (!bookRepository.existsByUrl(bookUrl)) {
                    Book book = crawlBookDetail(bookUrl, bookCategory);
                    Book savedBook = bookRepository.save(book);
                    books.add(savedBook);
                    log.debug("새 책 저장: {}", book.getTitle());
                } else {
                    log.debug("이미 존재하는 책: {}", bookUrl);
                }
            }

            currentUrl = getNextPageUrl(doc, currentUrl);
        }

        log.info("카테고리 '{}'에서 총 {} 권의 책 저장 완료", categoryName, books.size());
        return books;
    }

    /**
     * 모든 카테고리의 모든 책을 크롤링하여 저장합니다
     */
    @Transactional
    public List<Book> crawlAndSaveAllBooks() throws IOException {
        log.info("전체 책 크롤링 시작");

        // 먼저 카테고리를 크롤링하여 저장
        List<BookCategory> categories = bookCategoryService.crawlAndSaveCategories();
        List<Book> allBooks = new ArrayList<>();

        for (BookCategory category : categories) {
            try {
                List<Book> books = crawlAndSaveBooksByCategory(category.getCategoryName());
                allBooks.addAll(books);

                // 서버 부담 방지
                Thread.sleep(1000);
            } catch (IOException e) {
                log.error("카테고리 '{}' 크롤링 실패: {}", category.getCategoryName(), e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("크롤링 중단됨");
                break;
            }
        }

        log.info("전체 크롤링 완료: 총 {} 권의 책", allBooks.size());
        return allBooks;
    }

    /**
     * 책 상세 정보를 크롤링합니다
     */
    private Book crawlBookDetail(String bookUrl, BookCategory bookCategory) throws IOException {
        Document doc = Jsoup.connect(bookUrl)
                .timeout(TIMEOUT)
                .userAgent("Mozilla/5.0")
                .get();

        Book book = new Book();
        book.setUrl(bookUrl);
        book.setBookCategory(bookCategory);

        // 제목
        Element titleElement = doc.selectFirst("div.product_main h1");
        if (titleElement != null) {
            book.setTitle(titleElement.text());
        }

        // 가격
        Element priceElement = doc.selectFirst("p.price_color");
        if (priceElement != null) {
            String priceText = priceElement.text().replaceAll("[^0-9.]", "");
            try {
                book.setPrice(new BigDecimal(priceText));
            } catch (NumberFormatException e) {
                log.warn("가격 파싱 실패: {}", priceText);
            }
        }

        // 상품 정보 테이블
        Elements infoRows = doc.select("table.table tr");
        for (Element row : infoRows) {
            Element th = row.selectFirst("th");
            Element td = row.selectFirst("td");

            if (th != null && td != null) {
                String key = th.text();
                String value = td.text();

                switch (key) {
                    case "UPC":
                        book.setUpc(value);
                        break;
                    case "Product Type":
                        book.setProductType(value);
                        break;
                    case "Availability":
                        book.setAvailability(value);
                        break;
                    case "Number of reviews":
                        try {
                            book.setNumberOfReviews(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            log.warn("리뷰 수 파싱 실패: {}", value);
                        }
                        break;
                }
            }
        }

        // 설명
        Element descElement = doc.selectFirst("article.product_page > p");
        if (descElement != null) {
            book.setDescription(descElement.text());
        }

        return book;
    }

    /**
     * 책 목록 조회 (전체)
     */
    @Transactional(readOnly = true)
    public List<BookDTO> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리별 책 목록 조회
     */
    @Transactional(readOnly = true)
    public List<BookDTO> getBooksByCategory(String categoryName) {
        return bookRepository.findByBookCategory_CategoryName(categoryName).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Entity를 DTO로 변환
     */
    private BookDTO convertToDTO(Book book) {
        return BookDTO.builder()
                .title(book.getTitle())
                .price(book.getPrice())
                .upc(book.getUpc())
                .productType(book.getProductType())
                .availability(book.getAvailability())
                .numberOfReviews(book.getNumberOfReviews())
                .description(book.getDescription())
                .url(book.getUrl())
                .categoryName(book.getBookCategory() != null ?
                        book.getBookCategory().getCategoryName() : null)
                .build();
    }

    /**
     * 책 URL 추출
     */
    private String extractBookUrl(Element bookElement) {
        Element linkElement = bookElement.selectFirst("h3 a");
        if (linkElement != null) {
            String href = linkElement.attr("href");
            return BASE_URL + "/catalogue/" + href.replace("../../../", "");
        }
        return null;
    }

    /**
     * 다음 페이지 URL 반환
     */
    private String getNextPageUrl(Document doc, String currentUrl) {
        Element nextButton = doc.selectFirst("li.next a");
        if (nextButton != null) {
            String nextHref = nextButton.attr("href");
            int lastSlashIndex = currentUrl.lastIndexOf('/');
            String baseUrl = currentUrl.substring(0, lastSlashIndex + 1);
            return baseUrl + nextHref;
        }
        return null;
    }

    /**
     * 카테고리 이름으로 URL 생성 (간단한 매핑)
     * 실제로는 더 정교한 매핑이 필요할 수 있습니다
     */
    private String getCategoryUrl(String categoryName) {
        String urlName = categoryName.toLowerCase().replace(" ", "-");
        return BASE_URL + "/catalogue/category/books/" + urlName + "_2/index.html";
    }
}