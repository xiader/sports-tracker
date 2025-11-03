# Sports Tracker Microservice

A Java-based microservice for tracking live sports events with REST API for event status management, periodic polling, and Kafka integration.

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-4.0.1-black.svg)](https://kafka.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Quick Start](#-quick-start)
- [API Documentation](#-api-documentation)
- [Kafka Setup](#-kafka-setup)
- [Testing](#-testing)
- [Monitoring](#-monitoring)
- [Troubleshooting](#-troubleshooting)

---

## ✨ Features

### Core Requirements Implemented

- ✅ **Event Status Management** - REST API to mark events as `live` or `not_live`
- ✅ **Dynamic Polling** - Automatic scheduled polling for each live event (every 10 seconds)
- ✅ **External API Integration** - Fetches real-time event data from external sources
- ✅ **Kafka Publishing** - Publishes event updates to Kafka topics
- ✅ **Retry Logic** - Exponential backoff for both API calls and Kafka publishing
- ✅ **Error Handling** - Comprehensive error handling with detailed logging
- ✅ **Mock API** - Built-in mock API for testing without external dependencies
- ✅ **Thread-Safe** - In-memory state management with `ConcurrentHashMap`
- ✅ **Health Checks** - Spring Actuator endpoints for monitoring

### Technical Highlights

- **Apache Kafka 4.0.1** in KRaft mode (no Zookeeper required)
- **Dual Kafka Listeners** - Separate endpoints for host and Docker network connections
- **TaskScheduler** - Dynamic task creation/cancellation per event
- **WebFlux** - Non-blocking HTTP client for external API calls
- **Spring Retry** - Declarative retry mechanism with exponential backoff

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    REST API Layer                            │
│  POST /events/status    │  GET /events/{id}/status          │
│  GET /events/live       │  GET /events/stats                │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│            EventStatusController                             │
│  • Receives status updates (live/not_live)                  │
│  • Triggers start/stop of polling tasks                     │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│           SportsTrackerService                              │
│  • Manages scheduled tasks (TaskScheduler)                  │
│  • One task per live event (every 10 seconds)              │
│  • Calls External API → Transforms → Publishes to Kafka    │
└──────┬──────────────────────────────────────┬───────────────┘
       │                                       │
┌──────▼────────────┐              ┌──────────▼──────────────┐
│ ExternalApiService│              │ KafkaProducerService    │
│ • WebClient       │              │ • KafkaTemplate         │
│ • Retry (3x)      │              │ • Retry (3x)            │
└──────┬────────────┘              └─────────────────────────┘
       │                                       │
┌──────▼────────────┐              ┌──────────▼──────────────┐
│   External API    │              │     Kafka Topic         │
│   (Mock/Real)     │              │   "sports-events"       │
└───────────────────┘              └─────────────────────────┘

             In-Memory State: EventStateManager
             • ConcurrentHashMap<eventId, LiveEvent>
             • Thread-safe operations
```

---

## 🚀 Quick Start

### Prerequisites

- **Java 25** (or compatible JDK)
- **Docker** + **Docker Compose**
- **Git** (optional)

### Step 1: Clone the Repository

```bash
git clone <repository-url>
cd sports-tracker
```

### Step 2: Start Kafka

```bash
docker-compose up -d
```

Verify containers are running:
```bash
docker-compose ps
```

Expected output:
```
NAME       IMAGE                             STATUS
kafka      apache/kafka:4.0.1               healthy
kafka-ui   provectuslabs/kafka-ui:latest    running
```

**Kafka UI:** http://localhost:8090

### Step 3: Build the Application

```bash
./gradlew clean build
```

### Step 4: Run the Application

```bash
./gradlew bootRun
```

Or using the JAR:
```bash
java -jar build/libs/sports-tracker-0.0.1-SNAPSHOT.jar
```

### Step 5: Test the API

#### Mark an event as "live":
```bash
curl -X POST http://localhost:8080/events/status \
  -H "Content-Type: application/json" \
  -d '{"eventId": "match-001", "status": "live"}'
```

**What happens:**
1. Event is marked as "live" in memory
2. Scheduled task is automatically created
3. External API is polled every 10 seconds
4. Data is transformed and published to Kafka

#### Check event status:
```bash
curl http://localhost:8080/events/match-001/status
```

#### View messages in Kafka UI:
1. Open http://localhost:8090
2. Navigate to: **Topics** → **sports-events** → **Messages**

---

### Customization

**Change polling interval:**
```properties
app.polling.interval=5000  # 5 seconds
```

**Use real external API:**
```properties
app.external-api.url=https://your-api.com/events
```

**Change Kafka topic:**
```properties
app.kafka.topic=your-topic-name
```

---

## 📚 API Documentation

### POST /events/status
Updates event status (live/not_live)

**Request:**
```json
{
  "eventId": "match-001",
  "status": "live"
}
```

**Response (200 OK):**
```json
{
  "eventId": "match-001",
  "status": "LIVE",
  "message": "Event status updated successfully",
  "timestamp": "2025-11-02T18:00:00Z"
}
```

**Status Codes:**
- `200 OK` - Success
- `400 Bad Request` - Invalid input
- `500 Internal Server Error` - Server error

---

### GET /events/{eventId}/status
Retrieves status of a specific event

**Response (200 OK):**
```json
{
  "eventId": "match-001",
  "isLive": true,
  "lastUpdated": "2025-11-02T18:00:00Z",
  "lastPolled": "2025-11-02T18:00:10Z",
  "hasScheduledTask": true
}
```

---

### GET /events/live
Lists all live events

**Response (200 OK):**
```json
{
  "liveEvents": ["match-001", "match-002"],
  "count": 2
}
```

---

### GET /events/stats
Returns polling statistics

**Response (200 OK):**
```json
{
  "totalEvents": 5,
  "liveEvents": 2,
  "pollingIntervalMs": 10000,
  "timestamp": "2025-11-02T18:00:00Z"
}
```

---

### GET /mock/events/data
Mock API endpoint for testing

**Query Parameters:**
- `eventId` (required) - Event identifier

**Response (200 OK):**
```json
{
  "eventId": "match-001",
  "currentScore": "2:1"
}
```

---

## 🔧 Kafka Setup

### Port Configuration

| Port | Purpose | Used By |
|------|---------|---------|
| **9092** | PLAINTEXT_HOST listener | Spring Boot (host machine) |
| **29092** | PLAINTEXT listener | Kafka UI (Docker containers) |
| **9093** | CONTROLLER listener | Internal KRaft controller |
| **8090** | Kafka UI web interface | Browser |

### Connection Architecture

```
┌─────────────────┐
│  Spring Boot    │ ──→ localhost:9092 ──→ PLAINTEXT_HOST ──→ Advertises: localhost:9092 ✅
│  (Host)         │
└─────────────────┘

┌─────────────────┐
│   Kafka UI      │ ──→ kafka:29092 ──→ PLAINTEXT ──→ Advertises: kafka:29092 ✅
│  (Docker)       │
└─────────────────┘
```

### Why Two Listeners?

**Problem:** Kafka advertises itself using `ADVERTISED_LISTENERS`. If set to `localhost:9092`:
- ✅ Spring Boot (host) can connect
- ❌ Kafka UI (Docker) cannot connect (localhost inside container ≠ Kafka)

**Solution:** Two separate listeners:
1. `PLAINTEXT_HOST` (port 9092) - advertises `localhost:9092` for host
2. `PLAINTEXT` (port 29092) - advertises `kafka:29092` for Docker

### Docker Compose Commands

```bash
# Start Kafka
docker-compose up -d

# Stop Kafka
docker-compose down

# Stop and remove data
docker-compose down -v

# View logs
docker logs kafka --tail 50
docker logs kafka-ui --tail 50

# Check status
docker-compose ps
```

### Manual Kafka Commands

```bash
# Enter Kafka container
docker exec -it kafka bash

# List topics
/opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092

# Describe topic
/opt/kafka/bin/kafka-topics.sh --describe --topic sports-events --bootstrap-server localhost:9092

# Read messages
/opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic sports-events \
  --from-beginning
```

---

## 🧪 Testing

### Run All Tests
```bash
./gradlew test
```

### Test Scenarios

#### Scenario 1: Track Multiple Events

```bash
# Add first event
curl -X POST http://localhost:8080/events/status \
  -H "Content-Type: application/json" \
  -d '{"eventId": "match-001", "status": "live"}'

# Add second event
curl -X POST http://localhost:8080/events/status \
  -H "Content-Type: application/json" \
  -d '{"eventId": "match-002", "status": "live"}'

# Verify both are live
curl http://localhost:8080/events/live

# Check Kafka - should receive messages from both events
```

#### Scenario 2: Start/Stop Event

```bash
# Start event
curl -X POST http://localhost:8080/events/status \
  -H "Content-Type: application/json" \
  -d '{"eventId": "match-123", "status": "live"}'

# Stop event
curl -X POST http://localhost:8080/events/status \
  -H "Content-Type: application/json" \
  -d '{"eventId": "match-123", "status": "not_live"}'

# Restart event
curl -X POST http://localhost:8080/events/status \
  -H "Content-Type: application/json" \
  -d '{"eventId": "match-123", "status": "live"}'
```

---

## 📊 Monitoring

### Actuator Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Info
curl http://localhost:8080/actuator/info
```

### Logging

All operations are logged:
- REST API calls
- Scheduled task execution
- External API calls (with retries)
- Kafka publishing (with retries)
- Errors and warnings

**Change log level:**
```properties
logging.level.com.spgroup=DEBUG
```

---

## 🔄 Retry Mechanisms

### External API Retry
- **Attempts:** 3
- **Backoff:** Exponential (1s, 2s, 4s)
- **Timeout:** 5 seconds per request

### Kafka Publishing Retry
- **Attempts:** 3
- **Backoff:** Exponential (2s, 4s, 8s)
- **Timeout:** 10 seconds

---

## 🐛 Troubleshooting

### Application Won't Start

```bash
# Check if Kafka is running
docker-compose ps

# Check application logs
./gradlew bootRun --info

# Check if port 8080 is available
# Linux/Mac:
lsof -i :8080
# Windows:
netstat -ano | findstr :8080
```

### Messages Not Reaching Kafka

```bash
# 1. Verify Kafka is running
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092

# 2. Check if topic exists
# Should see: sports-events

# 3. Check application logs
# Should see: "Successfully sent message to Kafka"

# 4. Check Kafka UI
# http://localhost:8090 → Topics → sports-events → Messages
```


## 🤖 AI Assistance

This project was developed with assistance from Claude and ChatGPT for:
- Code generation and boilerplate
- Test writing
- Documentation creation
- Best practices review

---

## 📄 License

MIT License - Free to use

---


**Ready to track sports events! 🚀**

For quick start, see [Quick Start](#-quick-start) section.