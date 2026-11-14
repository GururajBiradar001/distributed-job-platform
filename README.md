\# Distributed Job Orchestration Platform



A distributed job processing system built with Java, Spring Boot, Apache Kafka, PostgreSQL, and Docker.



The system accepts jobs through a REST API, publishes them through Kafka, and distributes execution across worker services.



\## Key Features



\- Event-driven job processing using Kafka

\- Priority-based job dispatching

\- Retry and backoff

\- Idempotency support

\- Distributed job claiming

\- Worker lease and heartbeat

\- Dead-letter handling

\- PostgreSQL persistence

\- Dockerized microservices

\- Spring Boot Actuator metrics



\## Tech Stack



Java 24 · Spring Boot · Apache Kafka · PostgreSQL · Docker · Maven



\## Run Locally



```bash

docker compose up --build



## Deployment

Each service is independently buildable from the repository root. The job and worker Dockerfiles use the repository root as their Docker build context, so they can be deployed as separate services.

Required runtime variables for `job-service`:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `KAFKA_BOOTSTRAP_SERVERS`

Required runtime variables for `worker-service`:

- `KAFKA_BOOTSTRAP_SERVERS`
- `KAFKA_CONSUMER_GROUP` (optional)
- `JOB_SERVICE_URL` (optional; defaults to `http://job-service:8081`)
