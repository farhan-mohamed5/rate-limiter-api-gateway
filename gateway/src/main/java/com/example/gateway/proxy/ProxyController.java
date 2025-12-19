package com.example.gateway.proxy;

import com.example.gateway.config.GatewayProps;

import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@RestController
public class ProxyController {

  private final WebClient client;
  private final GatewayProps props;

  public ProxyController(GatewayProps props) {
    this.props = props;
    this.client = WebClient.builder().build();
  }

  @RequestMapping(value = "/api/**")
  public Mono<ResponseEntity<byte[]>> proxy(ServerHttpRequest request) {
    String fullPath = request.getURI().getRawPath();
    String upstreamPath = fullPath.substring("/api".length());
    String query = request.getURI().getRawQuery();

    String target = props.getUpstreamBaseUrl() + upstreamPath + (query != null ? "?" + query : "");

    HttpMethod method = request.getMethod();
    HttpHeaders headers = new HttpHeaders();
    request.getHeaders().forEach((k, v) -> {
      if (!k.equalsIgnoreCase(HttpHeaders.HOST)) headers.put(k, v);
    });

    return client.method(method)
        .uri(target)
        .headers(h -> h.addAll(headers))
        .body(BodyInserters.fromDataBuffers(request.getBody()))
        .exchangeToMono(resp ->
            resp.bodyToMono(byte[].class)
                .defaultIfEmpty(new byte[0])
                .map(body -> {
                  HttpHeaders out = new HttpHeaders();
                  resp.headers().asHttpHeaders().forEach(out::put);
                  return new ResponseEntity<>(body, out, resp.statusCode());
                })
        );
  }
}
