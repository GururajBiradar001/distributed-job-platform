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

