package com.example.gateway.filter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.example.gateway.config.GatewayProps;
import com.example.gateway.ratelimit.FixedWindowRateLimiter;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.*;

import reactor.core.publisher.Mono;

@Component
public class ApiKeyRateLimitFilter implements WebFilter {

  private final GatewayProps props;
  private final FixedWindowRateLimiter limiter = new FixedWindowRateLimiter();

  private final Map<String, AtomicLong> allowed = new ConcurrentHashMap<>();
  private final Map<String, AtomicLong> blocked = new ConcurrentHashMap<>();

  public ApiKeyRateLimitFilter(GatewayProps props) { this.props = props; }

  public Map<String, AtomicLong> allowedStats() { return allowed; }
  public Map<String, AtomicLong> blockedStats() { return blocked; }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();

    if (path.startsWith("/admin")) return chain.filter(exchange);
    if (!path.startsWith("/api/")) return chain.filter(exchange);

    String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
    if (apiKey == null || apiKey.isBlank()) return reject(exchange, HttpStatus.UNAUTHORIZED, "Missing X-API-Key");

    String planName = props.getApiKeys() != null ? props.getApiKeys().get(apiKey) : null;
    if (planName == null) return reject(exchange, HttpStatus.FORBIDDEN, "Invalid API key");

    GatewayProps.Plan plan = props.getPlans().get(planName);
    int windowSeconds = plan.getWindowSeconds();
    int limit = resolveLimit(plan, path);

    var decision = limiter.tryConsume(apiKey + ":" + bucket(path), limit, windowSeconds);

    if (!decision.allowed()) {
      blocked.computeIfAbsent(apiKey, k -> new AtomicLong()).incrementAndGet();
      exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
      exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
      exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(decision.retryAfterSeconds()));
      String body = "{\"error\":\"rate_limited\",\"retryAfterSeconds\":" + decision.retryAfterSeconds() + "}";
      return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes())));
    }

    allowed.computeIfAbsent(apiKey, k -> new AtomicLong()).incrementAndGet();
    exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
    exchange.getResponse().getHeaders().set("X-RateLimit-Reset", String.valueOf(decision.windowResetEpochSeconds()));
    return chain.filter(exchange);
  }

  private int resolveLimit(GatewayProps.Plan plan, String path) {
    String upstreamPath = path.substring("/api".length());
    if (plan.getRoutes() != null) {
      for (var e : plan.getRoutes().entrySet()) {
        if (upstreamPath.startsWith(e.getKey())) return e.getValue();
      }
    }
    return plan.getDefaultLimit();
  }

  private String bucket(String path) {
    String upstreamPath = path.substring("/api".length());
    return upstreamPath.startsWith("/auth") ? "auth" : "default";
  }

  private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
    exchange.getResponse().setStatusCode(status);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    String body = "{\"error\":\"" + message.replace("\"", "") + "\"}";
    return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes())));
  }
}
