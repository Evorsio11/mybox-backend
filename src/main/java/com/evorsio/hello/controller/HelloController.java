package com.evorsio.hello.controller;

import java.time.Duration;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evorsio.hello.service.CacheService;

@RestController
public class HelloController {
    private final CacheService cacheService;

    public HelloController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("/hello")
    public String hello(){
        return "Hello!";
    }

    @PostMapping("/cache/{key}")
    public ResponseEntity<String> setCache(
            @PathVariable String key,
            @RequestBody String value,
            @RequestParam(name = "ttlSeconds", required = false) Long ttlSeconds
    ) {
        long safeTtl = ttlSeconds == null ? 0L : Math.max(ttlSeconds, 0L);
        cacheService.setValue(key, value, safeTtl == 0L ? Duration.ZERO : Duration.ofSeconds(safeTtl));
        return ResponseEntity.ok("stored");
    }

    @GetMapping("/cache/{key}")
    public ResponseEntity<String> getCache(@PathVariable String key) {
        return cacheService.getValue(key)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
