package com.example.upstream;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class DemoController {

  @GetMapping("/health")
  public Map<String, Object> health() {
    return Map.of("ok", true, "service", "upstream", "ts", Instant.now().toString());
  }

  @PostMapping("/auth/login")
  public Map<String, Object> login(@RequestBody(required = false) Map<String, Object> body) {
    return Map.of("ok", true, "msg", "fake login ok", "echo", body);
  }

  @GetMapping("/orders/{id}")
  public Map<String, Object> order(@PathVariable String id) {
    return Map.of("orderId", id, "status", "CREATED", "ts", Instant.now().toString());
  }

  @GetMapping("/slow")
  public ResponseEntity<String> slow() throws InterruptedException {
    Thread.sleep(300);
    return ResponseEntity.ok("slow response ok");
  }
}
