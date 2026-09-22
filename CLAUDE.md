# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands
- **Build the project**: `mvn clean package -DskipTests`
- **Run backend**: `java -jar vetautet-start/target/vetautet-start-1.0-SNAPSHOT.jar`
- **Run all tests**: `mvn test`
- **Run a single test**: `mvn test -Dtest=TestClassName#testMethodName`
- **Start infrastructure**: 
  - Main DB/Cache/Metrics: `docker compose -f environment/docker-compose-dev.yml up -d`
  - Kafka: `docker compose -f environment/docker-compose-kafka.yml up -d`
- **Run Load Tests**: `k6 run benchmark/k6/flash-sale.js`

## Architecture & Structure
This is a Java 21 Spring Boot 3.3.5 project implementing Domain-Driven Design (DDD) to handle high-concurrency ticket flash sales.

### Module Layers
- `vetautet-domain`: Core entities, domain services, and repository interfaces. Independent of other layers.
- `vetautet-application`: Orchestrates use cases, app services, and cron jobs.
- `vetautet-infrastructure`: Implementations for JPA (MySQL), Redis (Lettuce/Redisson), and Kafka.
- `vetautet-controller`: REST API layer (HTTP DTOs, controllers).
- `vetautet-start`: Spring Boot application entry point and properties.

### Key Business Flows (Order placement)
The system solves overselling and bottlenecks using two main approaches:
1. **Synchronous Flow (CAS)**: Located in `OrderAppServiceImpl`. Request goes through a Redis Lua script atomic gate. It falls back to a DB atomic update (`UPDATE ... WHERE stock >= qty`). Implements dynamic Just-In-Time (JIT) table sharding (`order_yyyyMM`) for storing orders. Order timeouts are pushed to a Redis ZSET.
2. **Asynchronous Flow (MQ)**: Located in `OrderMQAppServiceImpl`. Used for extreme traffic. Implements a fast-fail Redis pre-deduction, then synchronously writes to `OrderQueue` and `OutboxEvent` (PENDING) in MySQL. An `OutboxPublisherJob` periodically sends events to Kafka (`order-place-topic`). The `KafkaOrderConsumer` processes the creation of orders asynchronously.

### Critical Patterns Used
- **Transactional Outbox**: Eliminates the dual-write problem between MySQL and Kafka by storing the Kafka payload in `outbox_event` during the local business transaction.
- **Idempotency Key**: The `KafkaOrderConsumer` ensures exactly-once processing by doing an `INSERT IGNORE` into the `idempotency_key` table within the same `@Transactional` block as the order creation.
- **SAGA / Compensating Transactions**: If the async consumer fails to deduct DB stock (a rare DB/Redis inconsistency), it issues a compensating transaction to increment the Redis cache back and marks the `OrderQueue` as FAILED.
- **Dynamic Sharding**: Uses Double-Check Locking with Redis distributed locks to automatically create monthly order tables at runtime.