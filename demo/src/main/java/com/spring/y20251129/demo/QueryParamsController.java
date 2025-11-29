package com.spring.y20251129.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@RestController
public class QueryParamsController {
    /// name이라는 쿼리 파라미터가 존재하지 않으면 오류가 발생
    @GetMapping("/welcome")
    public String sayWelcome(@RequestParam String name) {
        return "{\"message\": \"Welcome, " + name + "!\"}";
    }

    @GetMapping("/wave")
    public String tripleWave(@RequestParam Integer count) {
        int tripleCount = count * 3;
        return "{\"message\": \"손을 " + tripleCount + "번 흔들었습니다.\"}";
    }

    /// Optional 파라미터, 여러 개 파라미터
    @GetMapping("/books")
    public String getBooks(
        @RequestParam(required = false, defaultValue = "1") Integer page,
        @RequestParam(required = false, defaultValue = "1") Integer line
    ) {
        return "{\"message\": \"" + page + "번째 페이지, " + line + "번째 줄입니다.\"}";
    }

    /// 경로 변수
    @GetMapping("/hello/{name}")
    public String welcomeMessage(@PathVariable String name) {
        return "{\"message\": \"안녕, " + name + "!\"}";
    }

    // 경로명 == 파라미터명
    @GetMapping("/cube/{number}")
    public String getCube(@PathVariable Integer number) {
        int cube = number * number * number;
        return "{\"message\": \"" + number + "의 세제곱은 " + cube + "입니다.\"}";
    }

    // 경로명 != 파라미터명
    @GetMapping("/cubeDiff/{side}")
    public String getCubeDiff(@PathVariable(name = "side") Integer number) {
        int cube = number * number * number;
        return "{\"message\": \"" + number + "의 세제곱은 " + cube + "입니다.\"}";
    }

    // 파라미터 여러 개
    @GetMapping("/cubeMulti/{side}/{count}")
    public String getCube(@PathVariable(name = "side") Integer number, @PathVariable Integer count) {
        Integer cube = number * number * number;
        int total = cube * count;
        return "{\"message\": \"" + number + "의 세제곱 " + cube + "을 " + count + "번 더하면 " + total + "입니다.\"}";
    }

    @GetMapping("/articles/{articleId}/comments/{commentId}")
    public String getComment(@PathVariable Integer articleId, @PathVariable Integer commentId) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("id", articleId);
        node.put("articleId", commentId);
        node.put("content", "멋져요");
        return mapper.writeValueAsString(node);
    }
}

