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
## 🧠 Thought Process for Batching logic

* Initial Approach –

    - Using @Scheduled because the simplest way to flush data periodically is to use @Scheduled methods.
    - However, @Scheduled has fixed intervals and runs in a single-threaded context by default.
    - This is not suitable for high-throughput systems, where batching logic needs to react quickly to volume or timing constraints and run concurrently.

* Need for High-Throughput and Reactive Batching:

    - To efficiently handle spikes in traffic and avoid delays in flushing:
        - Batching should flush based on size or time threshold.
        - Events should be processed as they arrive, not just on a fixed timer.

* Switch to Continuous Loop with Executor:
    - A background thread is started using batchingExecutor (@PostConstruct).
    - It continuously polls the in-memory queue for new events (queueService.getQueue().poll()).

* Batching logic:
    - Start new batch when the first item arrives.
    - Keep accumulating until batch size exceeds maxBatchSizeBytes or
      batch age exceeds maxDelayMs.
    - When either threshold is reached, batch is flushed.

* Parallel S3 Writes with flushExecutor:
    - To avoid blocking the batching thread during I/O-heavy S3 writes:
    - Batches are handed off to flushExecutor, enabling non-blocking, concurrent writes.
    - This keeps the main loop responsive and ready for the next batch.
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

## ✅ Working Curl

```curl

curl --location 'http://3.110.28.39:8080/ingest' \
--header 'Content-Type: application/json' \
--header 'X-Customer-Tier: free' \
--data '{
"event_timestamp":"2024-01-11T01:42:50.234200+00:00",
"body":"what is the capital of India?"
}'

```
---
## 🛠️ Prerequisites

1. Java 17
2. Gradle 8.14.2
3. IntelliJ or any IDE of your choice
4. git clone git@github.com:lazyash-gpt/portal26-IngestService.git
5. open project in ide

---
## ⚙️ Configuration

Update `application.properties` file depending upon profile:

```properties
server.port=8080
allowed-tiers=free,pro,enterprise

# S3 Configuration
s3.bucket=my-s3-bucket-name
aws.region=ap-south-1

# Batching
batch.max-bytes=5242880  # 5MB
batch.max-delay-ms=5000  # 5 secs

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

## 🚰 Build & Run

### Build (with Gradle)

```bash
./gradlew clean build
```

### Run Locally

```bash
./gradlew bootRun
```
### Run on EC2
```bash
sudo systemctl start ingest 
```
---

## 📈 Metrics (Prometheus)

* `events.filtered` – Requests filtered due to invalid tier and body size.
* `events.received` – Requests accepted to further write in s3
* `s3.ingest.events` – Requests added in current batch to write into s3
* `s3.flush.success` – total successfull s3 writes
* `s3.flush.failure` - total failed s3 writes

📍 Exposed at:

* For Local : `http://localhost:8080/actuator/prometheus`
* For Prod(Ec2) : `http://3.110.28.39:8080/actuator/prometheus`

---

## ⚡ Load Testing

Use `Apache JMeter` to simulate 100+ req/sec and verify batch writes and latency.

* Below are the configs used for load testing
```json
org.apache.jmeter.testelement.TestPlan::class {
    props {
        it[name] = "posrtal26-ingestService Load Testing"
        it[guiClass] = "org.apache.jmeter.control.gui.TestPlanGui"
        it[userDefinedVariables] = org.apache.jmeter.config.Arguments().apply {
            props {
                it[name] = "User Defined Variables"
                it[guiClass] = "org.apache.jmeter.config.gui.ArgumentsPanel"
                it[testClass] = "org.apache.jmeter.config.Arguments"
            }
        }
    }

    org.apache.jmeter.threads.ThreadGroup::class {
        props {
            it[name] = "Thread Group"
            it[guiClass] = "org.apache.jmeter.threads.gui.ThreadGroupGui"
            it[numThreads] = 120
            it[rampTime] = 5
            it[duration] = 300d
            it[delay] = 3d
            it[sameUserOnNextIteration] = true
            it[useScheduler] = true
            it[onSampleError] = "continue"
            it[mainController] = org.apache.jmeter.control.LoopController().apply {
                props {
                    it[name] = "Loop Controller"
                    it[guiClass] = "org.apache.jmeter.control.gui.LoopControlPanel"
                    it[testClass] = "org.apache.jmeter.control.LoopController"
                    it[loops] = -1
                    it[continueForever] = false
                }
            }
        }

        org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy::class {
            props {
                it[name] = "HTTP Request"
                it[guiClass] = "org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui"
                it[domain] = "3.110.28.39"
                it[port] = "8080"
                it[path] = "/ingest"
                it[followRedirects] = true
                it[method] = "POST"
                it[useKeepalive] = true
                it[postBodyRaw] = true
                it[arguments] = org.apache.jmeter.config.Arguments().apply {
                    props {
                        it[arguments] = listOf(
                            org.apache.jmeter.protocol.http.util.HTTPArgument().apply {
                                props {
                                    it[alwaysEncode] = false
                                    it[value] = "{\r\n  \"event_timestamp\":\"2024-01-11T01:42:50.234200+00:00\",\r\n  \"body\":\"what is the capital of India?\"\r\n}\r\n"
                                    it[metadata] = "="
                                }
                            },
                        )
                    }
                }
            }

            org.apache.jmeter.protocol.http.control.HeaderManager::class {
                props {
                    it[headers] = listOf(
                        org.apache.jmeter.protocol.http.control.Header().apply {
                            props {
                                it[headerName] = "X-Customer-Tier"
                                it[value] = "\${tier}"
                            }
                        },
                        org.apache.jmeter.protocol.http.control.Header().apply {
                            props {
                                it[headerName] = "Content-Type"
                                it[value] = "application/json"
                            }
                        },
                    )
                    it[guiClass] = "org.apache.jmeter.protocol.http.gui.HeaderPanel"
                    it[name] = "HTTP Header Manager"
                }
            }

            org.apache.jmeter.config.CSVDataSet::class {
                props {
                    it[guiClass] = "org.apache.jmeter.testbeans.gui.TestBeanGUI"
                    it[name] = "CSV Data Set Config"
                }
                setProperty("delimiter", ",")
                setProperty("filename", "/Downloads/payloads.csv")
                setProperty("ignoreFirstLine", true)
                setProperty("quotedData", false)
                setProperty("recycle", true)
                setProperty("shareMode", "shareMode.all")
                setProperty("stopThread", false)
                setProperty("variableNames", "tier")
            }
        }

        org.apache.jmeter.reporters.ResultCollector::class {
            props {
                it[guiClass] = "org.apache.jmeter.visualizers.ViewResultsFullVisualizer"
                it[name] = "View Results Tree"
            }
        }

        org.apache.jmeter.reporters.ResultCollector::class {
            props {
                it[guiClass] = "org.apache.jmeter.visualizers.StatVisualizer"
                it[name] = "Aggregate Report"
            }
        }
    }
}

```
* payload.csv for custom header values

```csv
tier
free
pro
enterprise
free
pro
enterprise
free
pro
enterprise
free
pro
enterprise
free
pro
enterprise
gold
```
* Results

![img_1.png](img_1.png)

* Logs
```terminaloutput
c.p.i.controllers.IngestController - Accepted event from tier: free | Size: 93 bytes
Jul 12 15:21:18 ip-172-31-1-195 java[22350]: 2025-07-12 15:21:18.373 [http-nio-8080-exec-74] INFO  c.p.i.controllers.IngestController - Accepted event from tier: pro | Size: 93 bytes
Jul 12 15:21:18 ip-172-31-1-195 java[22350]: 2025-07-12 15:21:18.372 [http-nio-8080-exec-84] INFO  c.p.i.controllers.IngestController - Accepted event from tier: pro | Size: 93 bytes
Jul 12 15:21:18 ip-172-31-1-195 java[22350]: 2025-07-12 15:21:18.373 [http-nio-8080-exec-76] INFO  c.p.i.controllers.IngestController - Accepted event from tier: free | Size: 93 bytes
Jul 12 15:21:18 ip-172-31-1-195 java[22350]: 2025-07-12 15:21:18.373 [http-nio-8080-exec-96] INFO  c.p.i.controllers.IngestController - Accepted event from tier: enterprise | Size: 93 bytes
Jul 12 15:21:18 ip-172-31-1-195 java[22350]: 2025-07-12 15:21:18.373 [http-nio-8080-exec-38] INFO  c.p.i.controllers.IngestController - Accepted event from tier: free | Size: 93 bytes
Jul 12 15:21:18 ip-172-31-1-195 java[22350]: 2025-07-12 15:21:18.374 [http-nio-8080-exec-114] INFO  c.p.i.controllers.IngestController - Accepted event from tier: pro | Size: 93 bytes
Jul 12 15:21:18 ip-172-31-1-195 java[22350]: 2025-07-12 15:21:18.375 [http-nio-8080-exec-93] INFO  c.p.i.controllers.IngestController - Accepted event from tier: pro | Size: 93 bytes
Jul 12 15:21:18 ip-172-31-1-195 java[22350]: 2025-07-12 15:21:18.375 [http-nio-8080-exec-17] WARN  c.p.i.controllers.IngestController - Filtered event due to invalid/missing tier: gold
```
* events stats
```json
# HELP s3_flush_success_total  
# TYPE s3_flush_success_total counter
s3_flush_success_total 14.0
# HELP s3_ingest_events_total  
# TYPE s3_ingest_events_total counter
s3_ingest_events_total 96429.0
# HELP events_filtered_total
# TYPE events_filtered_total counter
events_filtered_total{tier="gold"} 6430.0
# HELP events_received_total
# TYPE events_received_total counter
events_received_total 96429.0
```

---

## 🔍 Issues Faced

* Major issue I faced is while creating deploy pipeline from github repo to EC2
* Some hiccups during setup of infra where I took help from internet/chatgpt

## 🤛️ Contributing

PRs welcome! Please follow conventional commit messages and write unit tests.
