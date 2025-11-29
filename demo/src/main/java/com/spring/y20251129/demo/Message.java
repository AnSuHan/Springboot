package com.spring.y20251129.demo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Message {
    private String content = "안녕!";

    public Message(String content) {
        this.content = content;
        System.out.println("Message 객체 생성!");
    }

    public Message() {
        System.out.println("Message 객체 생성!");
    }

    public int getContentLength() {
        return this.content.length();
    }

    public void setContent(String content) {
        if (content != null && content.contains("바보")) {
            return;
        }
        this.content = content;
    }

    public String getContent() {
        return this.content;
    }

    @JsonIgnore  // JSON 직렬화에서 제외
    @GetMapping("/message/object")
    public Message getMessageObject() {
        return new Message();
    }

    @GetMapping("/message/object/param")
    public Message getMessageObjectParam(
        @RequestParam String color
    ) {
        return new Message(color);
    }
}
