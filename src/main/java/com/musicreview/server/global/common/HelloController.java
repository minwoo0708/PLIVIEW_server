package com.musicreview.server.global.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
public class HelloController {

    public record HelloResponse(String message, OffsetDateTime time) {
    }

    @GetMapping("/api/hello")
    public HelloResponse hello() {
        return new HelloResponse("hello", TimeUtils.now());
    }
}
