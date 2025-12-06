# 프론트 사이트 크롤러
## 크롤링 대상 페이지
https://books.toscrape.com/

## 실행 명령
### 서버
./gradlew bootRun
### 클라이언트

---
## 작업 순서
- build.gradle 설정
  - 의존성 추가
- 디비 설정
  - application.yml에 디비 설정
  - domain/entity/ 테이블 구조 설정
- DTO, repository, service, controller 생성
- 프론트 프로젝트 생성 및 설정
  - react 프로젝트
  - npx create-react-app crawler-frontend
    cd crawler-frontend
    npm install axios react-router-dom @types/uuid
    npm start
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
