# sports-tracker

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