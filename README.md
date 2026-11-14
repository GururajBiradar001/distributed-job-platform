# Distributed Job Orchestration Platform

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-3.x-231F20?style=for-the-badge&logo=apachekafka&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?style=for-the-badge&logo=docker&logoColor=white)

An enterprise-grade, event-driven distributed task orchestration engine designed to reliably schedule, dispatch, and execute asynchronous background workloads across independent worker clusters.

---

## 🏛️ System Architecture

```
  [Client]
     │
     │ HTTP POST /jobs (Idempotency-Key)
     ▼
┌────────────────────────────────────────────────────────┐
│                      Job Service                       │
│  - REST API & Idempotency Key deduplication            │
│  - PostgreSQL: Jobs, OutboxEvents, InboxEvents         │
│  - PriorityJobDispatcher (pg_advisory_xact_lock)       │
│  - OutboxPublisher (@Scheduled polling)                │
└────────────┬─────────────────────────────▲─────────────┘
             │ (job-events)                │ (job-results)
             ▼                             │
      [Kafka Broker]                 [Kafka Broker]
             │                             │
             ▼                             │
┌──────────────────────────────────────────┴─────────────┐
│                     Worker Service                     │
│  - JobEventConsumer (claims job via HTTP handshake)    │
│  - LeaseHeartbeat (background lease renewal)           │
│  - JobExecutorRegistry (Strategy: ReportGeneration...) │
│  - JobResultPublisher (publishes completion)           │
└────────────────────────────────────────────────────────┘
```

---

## ⚡ Key Distributed Systems Patterns

* **Transactional Outbox Pattern**: Decouples local database state from Kafka publishing, eliminating the distributed dual-write problem.
* **Transactional Inbox (Idempotent Consumer)**: Ingests completed job events and de-duplicates them via an atomic `inbox_events` table (`ON CONFLICT DO NOTHING`).
* **PostgreSQL Advisory Locks (`pg_advisory_xact_lock`)**: Coordinates multiple instances of `job-service` to serialize priority dispatching without requiring heavy consensus engines like ZooKeeper or Raft.
* **Optimistic Distributed Job Claiming**: Prevents multiple workers from executing the same job concurrently by atomically checking status before transitioning from `QUEUED` to `RUNNING`.
* **Worker Leases & Heartbeats**: Workers renew leases periodically (`PUT /jobs/{id}/heartbeat`). If a worker dies, hangs, or experiences network partition, the lease expires and the orchestrator automatically re-queues the job.
* **Exponential Backoff & Dead-Letter Queue (DLQ)**: Failed tasks undergo exponential backoff with randomized jitter. Once `maxAttempts` are exhausted, events are routed to Kafka's `job-dead-letter` topic.
* **Full Observability**: Exposes Micrometer & Prometheus metrics (`/actuator/prometheus`) tracking `jobs_created`, `jobs_completed`, `jobs_failed`, and `jobs_dead`.

---

## 🚀 Quickstart: Run Locally in 1 Command

Prerequisites: **Docker** & **Docker Compose**.

```bash
git clone https://github.com/GururajBiradar001/distributed-job-platform.git
cd distributed-job-platform

# Build and start PostgreSQL, Kafka, Job Service, and Worker Service
docker compose up --build
```

Verify services:
* **Job Service Health**: `http://localhost:8081/actuator/health`
* **Worker Service Health**: `http://localhost:8082/actuator/health`

---

## 📡 API Reference

### 1. Submit a Job
```bash
curl -X POST http://localhost:8081/jobs \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: demo-job-001" \
  -d '{
    "type": "REPORT_GENERATION",
    "payload": "q1_earnings_report",
    "priority": 10,
    "maxAttempts": 3
  }'
```

**Response (201 Created):**
```json
{
  "id": "d1a31ca6-7958-4f30-9544-2ac85f9606dc",
  "type": "REPORT_GENERATION",
  "payload": "q1_earnings_report",
  "status": "QUEUED",
  "priority": 10,
  "attempts": 0,
  "maxAttempts": 3,
  "createdAt": "2026-11-14T05:36:11.049Z",
  "updatedAt": "2026-11-14T05:36:11.049Z"
}
```

### 2. Query Job Status
```bash
curl http://localhost:8081/jobs/d1a31ca6-7958-4f30-9544-2ac85f9606dc
```

**Response:**
```json
{
  "id": "d1a31ca6-7958-4f30-9544-2ac85f9606dc",
  "type": "REPORT_GENERATION",
  "payload": "q1_earnings_report",
  "status": "COMPLETED",
  "priority": 10,
  "attempts": 0,
  "maxAttempts": 3
}
```

---

## ☁️ 100% Free Cloud Deployment (Render + Upstash)

This repository includes a `render.yaml` blueprint configured for **Render's Free Tier** ($0/month):

1. **Create Free Kafka**:
   * Create a free cluster at [console.upstash.com](https://console.upstash.com) (no credit card needed).
   * Create topics: `job-events`, `job-results`, `job-dead-letter`.
2. **Deploy on Render**:
   * In [Render](https://render.com), click **New** -> **Blueprint**.
   * Connect this GitHub repo.
   * Render automatically provisions a free PostgreSQL database, `job-service`, and `worker-service`.
   * Add your Upstash bootstrap server and credentials into the environment variables.

---

## 🛠️ Tech Stack

* **Language**: Java 21 (LTS)
* **Framework**: Spring Boot 3.4.2 (Spring Web, Spring Data JPA, Spring Kafka)
* **Message Broker**: Apache Kafka (KRaft mode)
* **Database**: PostgreSQL 16
* **Observability**: Micrometer, Prometheus, Spring Boot Actuator
* **Containers**: Docker & Docker Compose
