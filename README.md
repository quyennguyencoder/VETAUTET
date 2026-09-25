# TicketPro — Flash Sale Ticket System

Project thực hành xây dựng hệ thống đặt vé tàu/sự kiện flash sale chịu tải cao theo kiến trúc **DDD (Domain-Driven Design)**. Bài toán chính là bán vé sự kiện/vé tàu dịp Tết — stock giới hạn, lượng truy cập đồng thời khổng lồ (High Concurrency), đảm bảo không bao giờ bán vượt vé (No Overselling) và server không bị quá tải.

---

## Architecture Diagrams

### 1. System Architecture
![System Architecture](https://res.cloudinary.com/shopdev/image/upload/v1781839037/sa_dmxsou.png)

---

### 2. DDD Module Layers

![DDD Module Layers](https://res.cloudinary.com/shopdev/image/upload/v1781839099/Screenshot_2026-06-19_at_10.18.13_smtu6l.png)

> `vetautet-domain` không phụ thuộc layer nào khác — infrastructure implement interface của domain, tuân thủ Dependency Inversion.

---

### 3. Order Flow (MQ Path)

![Order Flow MQ Path](https://res.cloudinary.com/shopdev/image/upload/v1781839149/Screenshot_2026-06-19_at_10.19.02_zttnnc.png)

---

## Module structure

```
vetautet
├── vetautet-start          # Spring Boot entry point, cấu hình hệ thống (application.yml)
├── vetautet-controller     # REST controllers, DTOs, Webhook (VNPay IPN), Security
├── vetautet-application    # Business Use Cases, Cron jobs, App services, Distributed Locks
├── vetautet-domain         # Entities, Domain services, Repository interfaces
├── vetautet-infrastructure # JPA, Redis, Kafka, VNPay Gateway, Redisson, OAuth2
└── vetautet.fe.com         # Frontend — React + Vite + TailwindCSS
```

---

## Tech Stack

| | |
|---|---|
| **Backend Core** | Java 21 (Virtual Threads), Spring Boot 3.3.5, Spring Security |
| **Architecture** | Domain-Driven Design (DDD), SAGA, Outbox Pattern |
| **Database** | MySQL 8 (HikariCP pool size 100) |
| **Cache & Lock** | Redis (Lettuce), Caffeine (Local L1), Redisson (Distributed Lock) |
| **Message Broker** | Kafka 3.7 (KRaft mode — không cần Zookeeper) |
| **Resilience** | Resilience4j (Circuit Breaker + Rate Limiter) |
| **3rd Party API** | VNPay Gateway (Payment), Google OAuth2 (SSO Login) |
| **Monitoring** | Prometheus, Grafana, ELK Stack |
| **Load Testing** | k6, JMeter |
| **Frontend** | ReactJS, Vite, TailwindCSS |

---

## How to run?

**Yêu cầu:** Java 21, Maven 3.8+, Docker Desktop, Node 18+

### 1. Khởi động infrastructure

```bash
cd environment

# MySQL, Redis, Prometheus, Grafana, ELK
docker compose -f docker-compose-dev.yml up -d

# Kafka + Kafka UI
docker compose -f docker-compose-kafka.yml up -d
```

| Service | URL / Port |
|---|---|
| MySQL | `localhost:3316` — db `vetautet`, pass `root1234` |
| Redis | `localhost:6319` |
| Kafka | `localhost:9094` |
| Kafka UI | http://localhost:8989 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 — admin/admin |
| Kibana | http://localhost:5601 |

### 2. Build & chạy backend

```bash
mvn clean package -DskipTests
java -jar vetautet-start/target/vetautet-start-1.0-SNAPSHOT.jar
```

App chạy tại `http://localhost:1122`. Check health: `curl localhost:1122/actuator/health`

### 3. Frontend

```bash
cd vetautet.fe.com && npm install && npm run dev
# → http://localhost:5173
```

---

## Section SAGA | Outbox | Order Queue | Idempotency

4 pattern giải quyết các failure case của luồng đặt vé async.

### Luồng tổng quan

```
POST /order/mq
    │  generate token = "TOKEN_TICKET_USER_{userId}_{ticketId}"
    │  pre-check stock Redis (DECRBY)
    │
    ▼
[DB Transaction]
    INSERT order_queue   (token, status=PENDING)
    INSERT outbox_event  (token, payload, status=PENDING)
    COMMIT
    │
    │  → trả token về cho client ngay, không chờ Kafka
    │
    ▼
[OutboxPublisherJob — cron 1s]
    SELECT outbox_event WHERE status=PENDING LIMIT 500
    → send Kafka → await broker ACK
    → UPDATE status=PUBLISHED
    │
    ▼
[KafkaOrderConsumer — concurrency=10]
    INSERT IGNORE idempotency_key(token)   ← nếu affected=0 thì skip
    UPDATE ticket_detail SET stock=stock-qty WHERE stock>=qty
    INSERT ticker_order
    UPDATE order_queue SET status=1 (hoặc 2 nếu hết vé)
    COMMIT
    │
    ▼
GET /order/queue/{token}  → client polling để lấy kết quả
```

---

### Outbox Pattern

Vấn đề cần giải quyết: ghi DB xong, app crash trước khi gửi Kafka → message mất, đơn không được xử lý.

Cách fix: thay vì gửi Kafka trực tiếp trong request, ghi message vào bảng `outbox_event` trong cùng transaction với business data. Một cron job chạy riêng sẽ đọc và gửi Kafka sau.

```
OutboxPublisherJob (fixedDelay=1s)
├── publishRowByRow()  ← đang dùng
│     gửi từng row, nhận ACK, update PUBLISHED ngay
│     failure window nhỏ — nếu crash chỉ 1 row bị retry
│
└── publishBatch()     ← option thay thế nếu cần throughput cao
      gửi async hàng loạt, bulk UPDATE một lần
      cần consumer idempotent chặt hơn vì failure window lớn hơn
```

File: `vetautet-application/.../cronjob/OutboxPublisherJob.java`

```sql
outbox_event (id, aggregate_id, payload, status, created_at, published_at)
-- PENDING → PUBLISHED
```

---

### Idempotency Key

Kafka có thể retry hoặc duplicate message — consumer phải xử lý được trường hợp cùng 1 message đến 2 lần mà không tạo ra 2 đơn hàng.

Cách xử lý: mỗi message có `token` duy nhất, consumer `INSERT IGNORE` token đó vào `idempotency_key` trong cùng transaction với toàn bộ business logic:

```java
boolean isNew = idempotencyKeyRepository.tryInsert(token, expiredAt);
if (!isNew) {
    log.info("[IDEMPOTENCY] Duplicate skip token={}", token);
    return;
}
// xử lý đơn bình thường...
```

Tại sao để trong cùng transaction: nếu xử lý fail và rollback, row idempotency cũng rollback → Kafka retry sẽ insert được lại → xử lý lại đúng. Nếu không có cùng transaction thì trường hợp này sẽ bị bỏ qua nhầm.

```sql
idempotency_key (token, expired_at)
-- UNIQUE KEY (token), INSERT IGNORE để atomic check+insert
```

---

### Order Queue

Vì đặt vé async, client cần biết kết quả mà không cần hold connection.

`order_queue` lưu trạng thái theo `token`, client polling sau khi nhận token:

```
status 0 = đang xử lý
status 1 = thành công (kèm order_number)
status 2 = thất bại (kèm message lý do)
```

```
GET /order/queue/{token}
→ { status: 1, orderNumber: "MQ-123-1718000000000" }
```

---

### SAGA — compensating transaction

Luồng MQ pre-deduct stock Redis trước khi gửi Kafka (để reject sớm khi hết vé). Nhưng nếu consumer xử lý xong thì DB không đủ stock (race condition giữa nhiều consumer), cần hoàn lại Redis:

```java
boolean stockDecreased = tickerOrderDomainService.decreaseStockLevel1(ticketId, quantity);
if (!stockDecreased) {
    stockOrderCacheService.increaseStockCache(ticketId, quantity); // hoàn Redis
    orderQueueRepository.updateStatus(token, 2, null, "Hết vé");
    return;
}
```

---

## API

```
# Ticket
GET    /ticket/{id}
POST   /ticket
PUT    /ticket/{id}
POST   /ticket/detail

# Order
POST   /order/cas          sync, dùng CAS trên DB
POST   /order/mq           async qua Kafka, dùng cho flash sale thật
GET    /order/queue/{token}
GET    /order/list

# Payment
POST   /payment/vnpay
GET    /payment/vnpay/callback

# Booking
POST   /booking
GET    /booking/{id}
```

---

## Benchmark

### k6

```bash
# chạy mặc định — CAS, 2000 requests, stock 1000
k6 run benchmark/k6/flash-sale.js

# so sánh với MQ
k6 run benchmark/k6/flash-sale.js -e ENDPOINT=/order/mq

# tuỳ chỉnh
k6 run benchmark/k6/flash-sale.js \
  -e TICKET_ID=23 \
  -e STOCK=1000 \
  -e TOTAL_USERS=2000 \
  -e VUS=500
```

Output:
```
╔══════════════════════════════════════════════════════════╗
║            FLASH SALE BENCHMARK — RESULT                 ║
╠══════════════════════════════════════════════════════════╣
║  ✅ Đặt thành công  : 1000                               ║
║  🚫 Hết vé          : 1000                               ║
║  Throughput (RPS)   : 1234.5                             ║
║  Latency p95 (ms)   : 420                                ║
║  Oversell Check     : ✅ OK (1000 ≤ 1000)                ║
╚══════════════════════════════════════════════════════════╝
```

### JMeter

```
benchmark/jmeter/POST Order CAS.jmx
benchmark/jmeter/HTTP Request level 0.jmx
```

---

## Monitoring

| | |
|---|---|
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| Kibana | http://localhost:5601 |
| Kafka UI | http://localhost:8989 |
| Actuator | http://localhost:1122/actuator |

`http.server.requests` — track p50/p95/p99, SLO buckets 100ms/500ms/1s/2s/5s
`hikaricp.connections.acquire` — DB pool pressure
Circuit Breaker state — `/actuator/health`

---

## Config nhanh (`application.yml`)

```yaml
server:
  port: 1122
  tomcat:
    accept-count: 2000
    max-connections: 10000

spring:
  threads:
    virtual:
      enabled: true   # Java 21

  datasource:
    hikari:
      maximum-pool-size: 100

  kafka:
    bootstrap-servers: localhost:9094
    consumer:
      group-id: order-consumer-group

resilience4j:
  circuitbreaker:
    instances:
      checkRandom:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 5s
  ratelimiter:
    instances:
      backendA:
        limitForPeriod: 2
        limitRefreshPeriod: 10s
```
