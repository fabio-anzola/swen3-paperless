package at.technikum.swen3.integration;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    protected static final PostgreSQLContainer<?> postgres;
    protected static final KafkaContainer kafka;
    protected static final GenericContainer<?> minio;

    static {
        postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:14-alpine"))
                .withDatabaseName("testdb")
                .withUsername("test_user")
                .withPassword("test_pw");

        kafka = new KafkaContainer(
                DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
                        .asCompatibleSubstituteFor("confluentinc/cp-kafka"));

        minio = new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
                .withCommand("server", "/data")
                .withExposedPorts(9000)
                .withEnv("MINIO_ROOT_USER", "testadmin")
                .withEnv("MINIO_ROOT_PASSWORD", "testadmin");
    }

    @BeforeAll
    static void startContainers() {
        if (!postgres.isRunning()) {
            postgres.start();
        }
        if (!kafka.isRunning()) {
            kafka.start();
        }
        if (!minio.isRunning()) {
            minio.start();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.listener.auto-startup", () -> "false");

        registry.add("minio.url", () -> "http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
        registry.add("minio.access-key", () -> "testadmin");
        registry.add("minio.secret-key", () -> "testadmin");
        registry.add("minio.bucket-name", () -> "test-documents");

        registry.add("spring.elasticsearch.uris", () -> "http://localhost:9200");
        registry.add("spring.data.elasticsearch.repositories.enabled", () -> "false");
    }
}
