# SWEN3 Paperless Application

A Spring Boot application for document management with PostgreSQL database and pgAdmin interface.

## Architecture

![System Architecture](artifacts/architecture.svg)

The system follows a microservices architecture with the following components:

- **Next.js Web Frontend** (Port 3000): User interface for document management
- **Spring Boot REST API** (Port 4000): Backend API handling business logic
- **Kafka Message Queue** (Port 9092): Event streaming backbone
- **OCR Worker**: Scalable workers for document text extraction; consumes the `ocr` topic and publishes OCR text to `genai-queue`
- **GenAI Worker**: Scalable workers that summarize OCR text with Gemini; consume `genai-queue` and publish enriched payloads to `result`
- **PostgreSQL Database** (Port 5455): Data persistence
- **pgAdmin** (Port 5050): Database administration interface
- **File Storage**: Document and file storage system

## Prerequisites

- Docker and Docker Compose installed on your system
- Git for cloning the repository

## Quick Start

### Build and Start the Application

- Create a `.env.secrets` file in the projects root directory to define secret
  environment keys.

- Run one of the below commands:

```bash
# Build and start all services (database, API, workers, UI)
docker compose --env-file .env.secrets up --build

# Scale stateless workers (example: 1 OCR worker, 3 GenAI workers)
docker compose --env-file .env.secrets up --build --scale ocr-worker=1 --scale genai-worker=3
```

### Access the Services

- **Spring Boot API**: http://localhost:8080
- **pgAdmin (Database Management)**: http://localhost:5050
  - Email: `admin@admin.com`
  - Password: `admin`
- **PostgreSQL Database**: localhost:5455 (from host machine)

### Worker / Kafka Flow

1. REST API uploads user files to MinIO and emits `{ "s3Key": "<key>" }` on topic `ocr`.
2. OCR workers download the file, extract text, and publish `{ "processedMessage": "<ocr-text>" }` to `genai-queue`.
3. GenAI workers consume `genai-queue`, call the Gemini API to summarize, and publish `{ "processedMessage": "<ocr-text>", "summary": "<genai-summary>" }` to the `result` topic.

### GenAI Worker

- Headless Kafka worker (no HTTP API). Set `GEMINI_API_KEY` before starting `docker compose` so it can call the Gemini API. Optional overrides: `GEMINI_MODEL` (default `gemini-1.5-flash-latest`) and `GEMINI_ENDPOINT` (default Google v1beta endpoint).
- Scale horizontally with `--scale genai-worker=<n>`; Kafka will spread `genai-queue` partitions across the replicas.

## Database Configuration

The application uses PostgreSQL with the following configuration:

- **Database Name**: `paperlessdb`
- **Username**: `paperless_user`
- **Password**: `paperless_pw`
- **Port**: 5455 (host) -> 5432 (container)

## Testing and Code Coverage

### Unit Tests

The project uses JUnit for testing and JaCoCo for code coverage analysis.

#### REST API Module

```bash
# Navigate to the REST API directory
cd apps/rest

# Run unit tests only
mvnw test

# Run unit tests with code coverage report
mvnw clean test

# Generate coverage report without running tests again
mvnw jacoco:report
```

#### OCR Worker Module

```bash
# Navigate to the OCR worker directory
cd apps/ocr-worker

# Run tests
mvn test

# Run tests with code coverage report
mvn clean test

# Generate coverage report without running tests again
mvn jacoco:report
```

#### GenAI Worker Module

```bash
# Navigate to the GenAI worker directory
cd apps/genai-worker

# Run tests
mvn test
```

### Integration Tests

The REST API module includes comprehensive integration tests using **Testcontainers** that verify the document upload workflow components:

1. Upload document → Store in PostgreSQL → Upload to MinIO
2. Send OCR message to Kafka
3. Repository operations for document management

**Note:** Integration tests use a simplified approach that disables Kafka listeners and manually simulates consumer behavior to avoid test hangs and timeouts.

#### Running Integration Tests

```bash
# Navigate to the REST API directory
cd apps/rest

# Run only integration tests (recommended)
mvn failsafe:integration-test -Djacoco.skip=true

# Run both unit tests and integration tests
mvn verify -Djacoco.skip=true

# Skip integration tests (run only unit tests)
mvn test

# Run a specific integration test
mvn failsafe:integration-test -Dit.test=DocumentUploadIntegrationTest -Djacoco.skip=true
```

**Note:** `-Djacoco.skip=true` skips code coverage checks which would otherwise fail due to coverage requirements.

**Requirements for Integration Tests:**

- Docker must be running (Testcontainers automatically starts required containers)
- First run may be slow due to downloading Docker images

**What gets tested:**

- `DocumentUploadIntegrationTest` (4 tests) - REST API, database persistence, S3 uploads, and Kafka producer
- `KafkaConsumerIntegrationTest` (3 tests) - Kafka message sending and repository queries
- `DocumentSearchIntegrationTest` (3 tests) - Document creation, updates, and elasticId management

**Total: 10 integration tests** running against real PostgreSQL, Kafka, and MinIO containers via Testcontainers.

See [Integration Tests README](apps/rest/src/test/java/at/technikum/swen3/integration/README.md) for detailed documentation.

### Viewing Code Coverage Reports

After running tests, JaCoCo generates HTML reports that you can view in your browser:

- **REST API Coverage Report**: `apps/rest/target/site/jacoco/index.html`
- **OCR Worker Coverage Report**: `apps/ocr-worker/target/site/jacoco/index.html`

### Code Coverage Thresholds

REST API and OCR worker builds are configured with a minimum code coverage threshold of 50% at the package level. Builds will fail if coverage falls below this threshold. To customize coverage thresholds, edit the JaCoCo plugin configuration in the respective `pom.xml` files.

### Running Coverage Check

To check if your code coverage meets the minimum threshold, you must first run tests to generate the coverage data:

```bash
# REST API - Run tests first, then check coverage
cd apps/rest
mvnw clean test
mvnw jacoco:check

# OCR worker - Run tests first, then check coverage
cd apps/ocr-worker
mvn clean test
mvn jacoco:check
```

**Note**: The `jacoco:check` goal requires the coverage data file (`jacoco.exec`) which is generated during test execution. If you run `jacoco:check` without running tests first, it will skip the check.
