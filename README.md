# Rate Limiter + API Gateway (Java, Spring Boot)

A lightweight **API Gateway** that sits in front of a service, validates callers via an **API key**, enforces **rate limits** (BASIC vs PRO tiers + per-route limits), and **proxies** requests to an upstream API. Includes a tiny upstream service for local demos.

---

## What this demonstrates
- Backend systems thinking: **gateway pattern**, edge enforcement, and service protection
- HTTP fundamentals: headers/status codes (**200**, **429**), request forwarding, routing
- Concurrency-safe limiting logic (thread-safe counters)
- Simple observability via an admin **stats** endpoint

---

## Architecture

```
Client  --->  Gateway (port 8080)  --->  Upstream Service (port 8081)
              - checks X-API-Key
              - rate limits per key + route
              - forwards /api/** to upstream
              - exposes /admin/stats
```

---

## Features

### Reverse proxy
- Requests to `GET/POST /api/**` are forwarded to the upstream service.
- Response is returned to the client (status/body/headers).

### API key authentication
- Clients must send the header `X-API-Key`.
- API keys map to plans:
  - `basic-key -> BASIC`
  - `pro-key -> PRO`

### Rate limiting (Fixed Window)
- Per-key rate limits with response `429 Too Many Requests` when exceeded.
- Headers:
  - `X-RateLimit-Remaining` (on allowed requests)
  - `X-RateLimit-Reset` (epoch seconds)
  - `Retry-After` (on 429)

### Per-route limits
- Example: `/auth/*` can be stricter than other endpoints.

### Admin stats
- `GET /admin/stats` returns allowed/blocked counters per API key (since gateway start).

---

## Tech stack
- Java 17  
- Spring Boot 3 (Gateway uses WebFlux)  
- Maven  
- Local-only setup (no paid services required)

---

## Project structure

```
rate-limit-gateway/
  upstream-service/   # Fake API to proxy to
  gateway/            # API Gateway + rate limiting + stats
```

---

## Prerequisites
- Java **17+**
- Maven **3.9+**

Verify:
```bash
java -version
mvn -v
```

---

## Run locally

### Terminal A — start upstream service (:8081)
```bash
cd upstream-service
mvn spring-boot:run
```

### Terminal B — start gateway (:8080)
```bash
cd gateway
mvn spring-boot:run
```

---

## Demo

### 0) Quick sanity check
```bash
curl -i "http://localhost:8080/api/health" -H "X-API-Key: basic-key"
```

---

## Demo (macOS / Linux)

### 1) Trigger rate limiting (200 → 429)
BASIC defaults to `20 requests / 60 seconds`. This burst should exceed it:

```bash
for i in {1..40}; do
  code=$(curl -s -o /dev/null -w "%{http_code}"     "http://localhost:8080/api/health" -H "X-API-Key: basic-key")
  echo "$i -> $code"
done
```

Expected:
- Requests 1–20: `200`
- Requests 21+: `429`

### 2) View stats
```bash
curl -s "http://localhost:8080/admin/stats"
```

### 3) Show PRO tier has higher limit
PRO defaults to `200 requests / 60 seconds`:

```bash
for i in {1..120}; do
  code=$(curl -s -o /dev/null -w "%{http_code}"     "http://localhost:8080/api/health" -H "X-API-Key: pro-key")
  echo "$i -> $code"
done
```

Then view stats again:
```bash
curl -s "http://localhost:8080/admin/stats"
```

---

## Demo (Windows PowerShell)

> PowerShell often aliases `curl` to `Invoke-WebRequest`.
> Use `curl.exe` for consistent behavior.

### 1) Trigger rate limiting (200 → 429)
```powershell
1..40 | ForEach-Object {
  $code = (curl.exe -s -o NUL -w "%{http_code}" "http://localhost:8080/api/health" -H "X-API-Key: basic-key")
  "$_ -> $code"
}
```

### 2) View stats
```powershell
curl.exe "http://localhost:8080/admin/stats"
```

### 3) Show PRO tier has higher limit
```powershell
1..120 | ForEach-Object {
  $code = (curl.exe -s -o NUL -w "%{http_code}" "http://localhost:8080/api/health" -H "X-API-Key: pro-key")
  "$_ -> $code"
}
```

---

## Configuration

Gateway config:
- `gateway/src/main/resources/application.yml`

Example:
```yaml
gateway:
  upstreamBaseUrl: "http://localhost:8081"
  apiKeys:
    basic-key: BASIC
    pro-key: PRO
  plans:
    BASIC:
      windowSeconds: 60
      defaultLimit: 20
      routes:
        "/auth": 5
    PRO:
      windowSeconds: 60
      defaultLimit: 200
      routes:
        "/auth": 30
```

---

## Notes
- On macOS you may see a Netty DNS native library warning in gateway logs. For localhost demos it does not affect behavior.

---

