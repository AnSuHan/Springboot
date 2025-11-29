package com.spring.y20251129.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {
    @GetMapping("/message")
    public String getMessage() {
        Message message = new Message("안녕, 코드잇!");
        System.out.println(message.getContent());

        Message message1 = new Message("만나서 반가워!");
        System.out.println(message1.getContent());
        System.out.println(message1.getContentLength());

        Message message2 = new Message();
        message2.setContent("잘가!");
        System.out.println(message2.getContent());

        return "message";
    }
}
