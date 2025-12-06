# 📚 프론트 사이트 크롤러 프로젝트 가이드

## 1. 개요 및 대상

| 구분 | 내용 |
| :--- | :--- |
| **크롤링 대상** | [Books to Scrape](https://books.toscrape.com/) |
| **목표** | 카테고리별로 목록을 순회하며 책 상세 정보를 수집하고 DB에 저장 |
| **기술 스택** | **백엔드 (서버):** Spring Boot, PostgreSQL, Java |
| | **크롤링 (클라이언트):** Node.js, Playwright |

***

## 2. 프로젝트 실행 명령

### 2.1. 서버 실행 (Spring Boot)

```bash
# Spring Boot 애플리케이션 실행
./gradlew bootRun
```

### 2.2. 클라이언트 실행 (Node.js/Playwright)
| 명령                           | 설명                                                                   |
|:-----------------------------|:---------------------------------------------------------------------|
| **npm run crawl:categories** | 웹사이트에서 모든 카테고리 정보를 수집하여 DB(BookCategory 테이블)에 저장                     |
| **npm run crawl:books**      | DB의 카테고리 목록을 기반으로 웹 페이지를 순회하며 책 상세 정보를 수집하여 DB(Book 테이블)에 저장         |
| **npm run dump:categories**  | **DB에 저장된 BookCategory 테이블 데이터를 조회하여 JSON Pretty로 출력 (브라우저 사용 안 함)** |
| **npm run dump:books**       | **DB에 저장된 Book 테이블 데이터를 조회하여 JSON Pretty로 출력 (브라우저 사용 안 함)**         |

---
## 3. 초기 환경 설정 및 준비
### 3.1. 백엔드 설정 (Spring Boot / Java)
- build.gradle: 프로젝트에 필요한 의존성(PostgreSQL 드라이버, JPA 등) 추가
- DB 설정: application.yml 파일에 PostgreSQL 접속 정보(host, port, database, username, password) 설
- 엔티티/테이블 정의: domain/entity 패키지에 BookCategory, Book 등 테이블 구조에 맞는 JPA 엔티티 설
- 핵심 컴포넌트 생성: DTO, Repository, Service, Controller 계층 생성.3.2. 프론트 크롤링 환경 설정 (Node.js)Bash# Node.js 프로젝트 초기화
   - npm init -y

### 필수 모듈 설치: Playwright(웹 제어), pg(PostgreSQL 연결), uuid(식별자 생성)
npm install playwright pg uuid

### Playwright용 Chromium 브라우저 설치
npm exec playwright install chromium
3.3. 실행 환경 (nodemon) 설정Bash# nodemon 전역 설치 (변경 시 자동 재실행)
npm install -g nodemon

## 4. 실행 조언 (IP 차단 방지 및 효율성)
- 성능과 안정성을 위해 크롤러 실행 시 다음 사항을 반드시 고려해야 합니다.

### 4.1. 딜레이 및 인적 요소

* **랜덤 딜레이 (Random Delay):** 페이지 이동이나 데이터 추출 전후에는 **불규칙한 대기 시간**을 삽입해야 합니다.
    * `await randomDelay(1500, 4000);` (1.5초에서 4초 사이의 랜덤 딜레이)와 같이 설정하여 기계적인 접근 속도를 숨기세요.
* **User Agent 설정:** 크롤러 초기화 시 `context`에 실제 브라우저와 유사한 `userAgent` 문자열을 설정하여 봇임을 감춥니다.

### 4.2. 데이터 플러시 (Flush) 전략

* **페이지 단위 Flush:** 수집한 책 데이터는 **책 한 권당 DB에 저장하지 않고**, 목록 페이지(약 20권) 단위로 데이터를 모아 **트랜잭션을 통해 일괄 저장(Flush)**합니다. 이는 DB 부하를 줄이고 데이터 정합성을 높입니다.
    * **"페이지가 변경될 때마다 Flush"** 로직이 적용되어야 합니다.

### 4.3. 내비게이션 패턴 (차단 방지 핵심) [미적용]

웹사이트는 비정상적인 탐색 패턴을 봇으로 간주하고 IP를 차단합니다. **사용자의 자연스러운 패턴**을 모방해야 합니다.

| 패턴 | 설명 | IP 차단 방지 유리도 |
| :--- | :--- | :--- |
| **`a -> b -> c`** | 목록(a)에서 상세(b)로 갔다가, 바로 다른 상세(c)로 이동 | **불리** ❌<br/>(Referer 불일치, 봇 행동으로 간주) |
| **`a -> b -> a -> c`** | 목록(a)에서 상세(b)로 갔다가, **목록(a)으로 돌아가** 상세(c)로 이동 | **유리** ✅<br/>(사용자가 목록을 스캔하는 자연스러운 패턴) |

**조언:** 상세 페이지 크롤링이 끝난 후, 다음 상세 페이지로 이동하기 전에 **반드시 목록 페이지로 돌아와(Page Refresh 또는 `page.goto(a_url)`)** 다음 항목 링크를 클릭하는 방식으로 진행해야 IP 차단을 회피하는 데 유리합니다.

---
### 사용 예시
#### 1. 카테고리 크롤링
POST http://localhost:8080/api/categories/crawl

#### 2. 카테고리 목록 조회
GET http://localhost:8080/api/categories

#### 3. 특정 카테고리의 책 크롤링
POST http://localhost:8080/api/books/crawl/category/Travel

#### 4. Travel 카테고리의 책 조회
GET http://localhost:8080/api/books/category/Travel

#### 5. 전체 책 조회
GET http://localhost:8080/api/books

#### 6. 시스템 상태 확인
GET http://localhost:8080/api/books/status
