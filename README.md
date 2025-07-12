## 📦 portal26-IngestService

A high-throughput backend service that receives JSON events over HTTP, batches them in-memory, and uploads them to S3 in near real-time.

---

## 🚀 Features

* Accepts `POST /ingest` requests with event payloads
* Reads and filters requests by `X-Customer-Tier` header (`free`, `pro`, `enterprise`)
* Batches events up to **5MB or 5 seconds** max delay
* Uploads batches to **Amazon S3**
* Built-in metrics (requests/sec, S3 writes/sec, failures, etc.)
* Prometheus-compatible monitoring via `/actuator/prometheus`

---

## 📨 Request Format

### Endpoint

```
POST /ingest
```

### Headers

```
X-Customer-Tier: [free | pro | enterprise]
Content-Type: application/json
```

### Body

```json
{
  "event_timestamp": "2024-01-11T01:42:50.234200+00:00",
  "body": "what is the capital of India?"
}
```

---

## ✅ Response

```json
{
  "status": "success"
}
```

---

## ⚙️ Configuration

Create an `application.properties` file:

```properties
server.port=8080
allowed-tiers=free,pro,enterprise

# S3 Configuration
s3.bucket=my-s3-bucket-name
aws.region=ap-south-1

# Batching
batch.max-bytes=5242880  # 5MB
batch.max-delay-ms=5000

# Metrics
management.endpoints.web.exposure.include=prometheus
management.endpoint.prometheus.enabled=true
management.metrics.export.prometheus.enabled=true
```

---

## ☁️ AWS Setup

1. Create an S3 bucket:

    * Region: `ap-south-1`
    * Bucket name: `my-s3-bucket-name`

2. Set up AWS credentials:

    * Locally: via `~/.aws/credentials` or environment variables
    * On EC2: attach a role with `s3:PutObject` permission

---

## 📈 Metrics (Prometheus)

* `events.filtered` – Requests filtered due to invalid tier and body size.
* `events.received` – Requests accepted to further write in s3
* `s3.ingest.eventst` – Requests added in current batch to write into s3
* `s3.flush.success` – total successfull s3 writes
* `s3.flush.failure` - total failed s3 writes

📍 Exposed at:

```
http://localhost:8080/actuator/prometheus
```

---

## 🚰 Build & Run

### Build (with Gradle)

```bash
./gradlew clean build
```

### Run Locally

```bash
./gradlew bootRun
```

### Run via Docker (optional)

```Dockerfile
# Dockerfile
FROM openjdk:17-jdk
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
docker build -t event-ingestor .
docker run -p 8080:8080 -e AWS_ACCESS_KEY_ID=... -e AWS_SECRET_ACCESS_KEY=... event-ingestor
```

---

## 🥺 Load Testing

Use `k6`, `wrk`, or `Apache JMeter` to simulate 100+ req/sec and verify batch writes and latency.

---

## 🦾 License

MIT License. See `LICENSE` file.

---

## 🤛️ Contributing

PRs welcome! Please follow conventional commit messages and write unit tests.
