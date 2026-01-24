# Integration Tests

This directory contains integration tests for the SWEN3 Paperless application using Testcontainers.

## Overview

The integration tests verify the document upload workflow components:

1. **Upload document** via REST API
2. **Store in PostgreSQL** database
3. **Upload to MinIO** (S3-compatible storage)
4. **Send OCR message** to Kafka
5. **Repository operations** for document management

**Note:** Tests use a simplified approach that disables Kafka listeners and manually simulates consumer behavior to avoid test hangs and timeouts.

## Test Structure

### Base Test Class

- `BaseIntegrationTest.java` - Sets up Testcontainers for all integration tests
  - PostgreSQL 14-alpine container
  - Kafka (Confluent Platform 7.5.0) container
  - MinIO container
  - Singleton pattern (containers reused across all tests)
  - Kafka listeners disabled globally to prevent test hangs

### Integration Test Classes

1. **DocumentUploadIntegrationTest** (4 tests) - Tests document upload workflow
   - Upload document via REST API
   - Verify document stored in database
   - Verify document uploaded to MinIO
   - Test custom filenames, invalid files, and authentication

2. **KafkaConsumerIntegrationTest** (3 tests) - Tests Kafka producer and repository
   - Send messages to Kafka topics
   - Query documents by S3 key
   - Parse and validate message formats

3. **DocumentSearchIntegrationTest** (3 tests) - Tests document persistence and queries
   - Create and update documents with elasticId
   - Query documents by elasticId
   - Bulk document creation and updates

## Running Integration Tests

### Using Maven

Run only integration tests (recommended):

```bash
cd apps/rest
mvn failsafe:integration-test -Djacoco.skip=true
```

Run both unit tests and integration tests:

```bash
mvn verify -Djacoco.skip=true
```

Run a specific integration test:

```bash
mvn failsafe:integration-test -Dit.test=DocumentUploadIntegrationTest -Djacoco.skip=true
```

**Note:** `-Djacoco.skip=true` skips code coverage checks which would otherwise fail due to coverage requirements.

## Requirements

- **Docker** must be running (Testcontainers requires Docker)
- **Java 21** or higher
- **Maven 3.8+**

## How It Works

### Testcontainers

The integration tests use Testcontainers to automatically:

1. Start required containers (PostgreSQL, Kafka, MinIO)
2. Configure Spring Boot to connect to test containers
3. Run tests against real infrastructure
4. Reuse containers across tests (singleton pattern)
5. Clean up containers after all tests complete

### Test Approach

To avoid test hangs and timeouts:

- Kafka listeners are disabled globally (`spring.kafka.listener.auto-startup=false`)
- Tests manually simulate what Kafka consumers would do
- No async waiting or Awaitility usage
- Tests verify individual components (REST, DB, S3, Kafka producer) rather than full async workflows

### Maven Configuration

- **maven-surefire-plugin**: Runs unit tests (excludes `*IntegrationTest.java`)
- **maven-failsafe-plugin**: Runs integration tests (includes `*IntegrationTest.java`)

Integration tests run in the `integration-test` phase, after unit tests.

## Test Data

Each test:

- Creates a fresh test user
- Cleans up database before running
- Uses isolated test data
- Tears down automatically

## Environment Variables

No environment variables are required. Testcontainers handles all configuration dynamically.

## Troubleshooting

### Docker Issues

If tests fail to start containers:

```bash
# Check Docker is running
docker ps

# Check Docker resources
docker system df

# Clean up old containers
docker system prune -a
```

### Port Conflicts

Testcontainers uses random ports to avoid conflicts. If you see port binding errors:

- Check no other containers are using random ports
- Restart Docker

### Slow Tests

First run may be slow due to:

- Downloading Docker images
- Starting containers
- Database initialization

Subsequent runs are faster as images are cached.

## Adding New Tests

To add a new integration test:

1. Extend `BaseIntegrationTest`
2. Name the class with `*IntegrationTest` suffix
3. Use `@AutoConfigureMockMvc` for REST API testing
4. Import `@Import(TestSecurityConfig.class)` for security
5. Clean up data in `@BeforeEach`
6. Avoid async operations - manually simulate instead

Example:

```java
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class MyNewIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @BeforeEach
    void setUp() {
        // Clean up data
        documentRepository.deleteAll();
    }

    @Test
    void testSomething() {
        // Test implementation - no await() needed
        // Manually perform operations instead of waiting
    }
}
```

## CI/CD Integration

Integration tests can be run in CI/CD pipelines that support Docker:

```yaml
# GitHub Actions example
- name: Run Integration Tests
  run: mvn verify
```

## Best Practices

1. **Isolate test data** - Each test should be independent
2. **Clean up** - Always clean data in @BeforeEach using `deleteAll()`
3. **Meaningful names** - Test names should describe what they test
4. **Avoid async dependencies** - Manually simulate async operations instead of waiting
5. **Test components** - Test individual components rather than full end-to-end workflows
6. **Use TestSecurityConfig** - Import security configuration for authenticated endpoints

## Performance

Typical execution times (10 tests total):

- Container startup (first run): 30-40 seconds
- Container startup (cached): 15-20 seconds
- DocumentSearchIntegrationTest (3 tests): ~20 seconds
- DocumentUploadIntegrationTest (4 tests): ~2 seconds
- KafkaConsumerIntegrationTest (3 tests): ~0.6 seconds
- **Full suite: ~25-30 seconds** (with cached containers)

## Further Reading

- [Testcontainers Documentation](https://www.testcontainers.org/)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [Maven Failsafe Plugin](https://maven.apache.org/surefire/maven-failsafe-plugin/)
