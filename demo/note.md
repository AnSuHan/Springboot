# 설정
## Spring Initializr
Spring Initializr는 Spring Boot 프로젝트를 쉽게 생성할 수 있는 웹 서비스예요. 복잡한 설정 없이도 프로젝트의 기본 구조와 필요한 의존성을 선택해서 프로젝트를 만들 수 있어요.

접속 주소: https://start.spring.io
프로젝트 언어, 빌드 도구, 필요한 라이브러리를 선택
"Generate" 버튼으로 프로젝트 다운로드
주요 설정 예시는 다음과 같아요.

| 설정 항목        | 선택 값                  |
| ------------ | --------------------- |
| Project      | Gradle - Kotlin       |
| Language     | Java                  |
| Dependencies | Spring Web, Thymeleaf |

# 명령
## 실행 명령
- ./gradlew bootRun

# 코드
## 어노테이션
### http
아래 두 어노테이션은 동일한 동작을 수행
- @RequestMapping(value = "/goodbye", method = RequestMethod.GET)
- @GetMapping("/goodbye")

### 합성 어노테이션
@RequestMapping에 value와 method를 매번 쓰지 않아도 되도록, 자주 사용되는 설정을 미리 조합해서 만든 어노테이션

| HTTP 메소드 | 합성 어노테이션       |
| -------- | -------------- |
| GET      | @GetMapping    |
| POST     | @PostMapping   |
| PUT      | @PutMapping    |
| PATCH    | @PatchMapping  |
| DELETE   | @DeleteMapping |

### 사용
@ResponseBody + @Controller == @RestController