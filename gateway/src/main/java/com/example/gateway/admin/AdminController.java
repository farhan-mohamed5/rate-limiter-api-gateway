package com.example.gateway.admin;

import java.util.Map;
import java.util.stream.Collectors;

import com.example.gateway.filter.ApiKeyRateLimitFilter;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {
  private final ApiKeyRateLimitFilter filter;

  public AdminController(ApiKeyRateLimitFilter filter) { this.filter = filter; }

  @GetMapping("/admin/stats")
  public Map<String, Object> stats() {
    var allowed = filter.allowedStats().entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    var blocked = filter.blockedStats().entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    return Map.of("allowed", allowed, "blocked", blocked);
  }
}
