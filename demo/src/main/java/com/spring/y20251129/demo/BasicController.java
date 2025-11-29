package com.spring.y20251129.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/// 리스폰스 내용 그 자체를 반환
//@RestController
//public class HelloController {
//    @GetMapping("/")
//    public String hello() {
//        return "Hello, Codeit!";
//    }
//
//    @RequestMapping(value = "/hello", method = RequestMethod.GET)
//    public String sayHello() {
//        return "hello";
//    }
//
//    @RequestMapping(value = "/goodbye", method = RequestMethod.GET)
//    public String sayGoodbye() {
//        return "goodbye";
//    }
//}

/// templates/hello.html. templates/goodbye.html 반환
//@Controller
//public class HelloController {
//    @GetMapping("/hello")
//    public String sayHello() {
//        return "hello"; // resources/templates/hello.html 렌더링
//    }
//
//    @GetMapping("/goodbye")
//    public String sayGoodbye() {
//        return "goodbye"; // resources/templates/hello.html 렌더링
//    }
//}

/// 리스폰스 내용 그 자체를 반환
@ResponseBody
@Controller
public class BasicController {
    @GetMapping("/hello")
    public String sayHello() {
        return "{\"message\": \"Hello, Spring Boot!\"}";
    }

    @GetMapping("/goodbye")
    public String sayGoodbye() {
        return "goodbye";
    }
}
