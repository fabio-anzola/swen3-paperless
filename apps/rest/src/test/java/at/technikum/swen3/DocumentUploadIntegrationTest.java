package at.technikum.swen3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import at.technikum.swen3.entity.Document;
import at.technikum.swen3.entity.User;
import at.technikum.swen3.repository.DocumentRepository;
import at.technikum.swen3.repository.UserRepository;
import at.technikum.swen3.search.elastic.PDFDocument;
import at.technikum.swen3.search.elastic.PDFDocumentRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Integration test for document upload use case.
 * Tests the complete flow:
 * 1. Upload document via REST API
 * 2. Document is stored in MinIO
 * 3. OCR worker processes the document
 * 4. GenAI worker generates summary
 * 5. Document is indexed in ElasticSearch
 * 6. Document metadata is updated with ElasticSearch ID
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class DocumentUploadIntegrationTest {

    private static final String COMPOSE_FILE_PATH = "src/test/resources/test.compose.yml";
    private static final int KAFKA_PORT = 9092;
    private static final int POSTGRES_PORT = 5432;
    private static final int MINIO_PORT = 9000;
    private static final int ELASTICSEARCH_PORT = 9200;

    @Container
    public static ComposeContainer environment = new ComposeContainer(new File(COMPOSE_FILE_PATH))
            .withExposedService("kafka", KAFKA_PORT, Wait.forListeningPort())
            .withExposedService("postgres", POSTGRES_PORT, Wait.forHealthcheck())
            .withExposedService("minio", MINIO_PORT, Wait.forHealthcheck())
            .withExposedService("elasticsearch", ELASTICSEARCH_PORT, Wait.forHealthcheck())
            .withExposedService("ocr-worker", 0)  // No exposed port for worker
            .withExposedService("genai-worker", 0)  // No exposed port for worker
            .withLocalCompose(true);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PDFDocumentRepository pdfDocumentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static KafkaMessageListenerContainer<String, String> resultTopicListenerContainer;
    private static String lastResultMessage;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // Kafka properties
        String kafkaBootstrapServers = environment.getServiceHost("kafka", KAFKA_PORT) 
            + ":" + environment.getServicePort("kafka", KAFKA_PORT);
        registry.add("spring.kafka.bootstrap-servers", () -> kafkaBootstrapServers);

        // PostgreSQL properties
        String postgresHost = environment.getServiceHost("postgres", POSTGRES_PORT);
        Integer postgresPort = environment.getServicePort("postgres", POSTGRES_PORT);
        registry.add("spring.datasource.url", 
            () -> String.format("jdbc:postgresql://%s:%d/testdb", postgresHost, postgresPort));
        registry.add("spring.datasource.username", () -> "testuser");
        registry.add("spring.datasource.password", () -> "testpass");

        // MinIO properties
        String minioHost = environment.getServiceHost("minio", MINIO_PORT);
        Integer minioPort = environment.getServicePort("minio", MINIO_PORT);
        registry.add("minio.url", () -> String.format("http://%s:%d", minioHost, minioPort));
        registry.add("minio.access-key", () -> "minioadmin");
        registry.add("minio.secret-key", () -> "minioadmin");
        registry.add("minio.bucket-name", () -> "test-documents");

        // Elasticsearch properties
        String elasticHost = environment.getServiceHost("elasticsearch", ELASTICSEARCH_PORT);
        Integer elasticPort = environment.getServicePort("elasticsearch", ELASTICSEARCH_PORT);
        registry.add("spring.elasticsearch.uris", 
            () -> String.format("http://%s:%d", elasticHost, elasticPort));
        registry.add("spring.elasticsearch.username", () -> "elastic");
        registry.add("spring.elasticsearch.password", () -> "testpass");

        // Kafka topics
        registry.add("kafka.topic.ocr", () -> "ocr");
        registry.add("kafka.topic.result", () -> "result");
    }

    @BeforeAll
    static void setupKafkaConsumer() {
        // Set up a Kafka consumer to listen to the result topic for verification
        String kafkaBootstrapServers = environment.getServiceHost("kafka", KAFKA_PORT) 
            + ":" + environment.getServicePort("kafka", KAFKA_PORT);

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-result-consumer");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        DefaultKafkaConsumerFactory<String, String> consumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties("result");
        resultTopicListenerContainer = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        
        resultTopicListenerContainer.setupMessageListener((MessageListener<String, String>) record -> {
            lastResultMessage = record.value();
        });

        resultTopicListenerContainer.start();
        // Give Kafka time to create the topic and assign partitions
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @WithMockUser(username = "testuser")
    public void testDocumentUploadEndToEnd() throws Exception {
        // Setup: Create test user
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser = userRepository.save(testUser);
        Long userId = testUser.getId();

        // Step 1: Create a sample PDF-like file for upload
        byte[] fileContent = createSamplePdfContent();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-document.pdf",
            "application/pdf",
            fileContent
        );

        String metaJson = objectMapper.writeValueAsString(Map.of("name", "Integration Test Document"));
        MockMultipartFile meta = new MockMultipartFile(
            "meta",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            metaJson.getBytes()
        );

        // Step 2: Upload document via REST API
        MvcResult uploadResult = mockMvc.perform(
                multipart("/api/v1/document")
                    .file(file)
                    .file(meta)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Integration Test Document"))
            .andExpect(jsonPath("$.s3Key").exists())
            .andReturn();

        String responseJson = uploadResult.getResponse().getContentAsString();
        JsonNode documentNode = objectMapper.readTree(responseJson);
        Long documentId = documentNode.get("id").asLong();
        String s3Key = documentNode.get("s3Key").asText();

        assertThat(documentId).isNotNull();
        assertThat(s3Key).isNotBlank();

        // Step 3: Verify document is saved in database with S3 key
        Optional<Document> documentOpt = documentRepository.findById(documentId);
        assertThat(documentOpt).isPresent();
        Document document = documentOpt.get();
        assertThat(document.getName()).isEqualTo("Integration Test Document");
        assertThat(document.getS3Key()).isEqualTo(s3Key);

        // Step 4: Wait for OCR worker to process (publishes to genai-queue)
        // and GenAI worker to process (publishes to result topic)
        await()
            .atMost(Duration.ofSeconds(60))
            .pollInterval(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                assertThat(lastResultMessage).isNotNull();
                JsonNode resultNode = objectMapper.readTree(lastResultMessage);
                assertThat(resultNode.get("s3Key").asText()).isEqualTo(s3Key);
                assertThat(resultNode.get("processedMessage").asText()).isNotBlank();
                assertThat(resultNode.get("summary").asText()).isNotBlank();
                assertThat(resultNode.get("elasticId").asText()).isNotBlank();
            });

        // Step 5: Verify document is indexed in ElasticSearch
        JsonNode resultNode = objectMapper.readTree(lastResultMessage);
        String elasticId = resultNode.get("elasticId").asText();

        await()
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                Optional<PDFDocument> pdfDocOpt = pdfDocumentRepository.findById(elasticId);
                assertThat(pdfDocOpt).isPresent();
                PDFDocument pdfDoc = pdfDocOpt.get();
                assertThat(pdfDoc.textContent()).isNotBlank();
                assertThat(pdfDoc.summary()).isNotBlank();
                assertThat(pdfDoc.fileName()).contains("test-document.pdf");
            });

        // Step 6: Verify document metadata is updated with ElasticSearch ID
        await()
            .atMost(Duration.ofSeconds(20))
            .pollInterval(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                Document updatedDoc = documentRepository.findById(documentId).orElseThrow();
                assertThat(updatedDoc.getElasticId()).isEqualTo(elasticId);
            });

        // Final verification: Document is searchable
        Optional<PDFDocument> finalPdfDoc = pdfDocumentRepository.findById(elasticId);
        assertThat(finalPdfDoc).isPresent();
        assertThat(finalPdfDoc.get().textContent()).isNotBlank();
        assertThat(finalPdfDoc.get().summary()).isNotBlank();
    }

    /**
     * Creates sample PDF-like content for testing.
     * In a real scenario, this would be actual PDF bytes.
     */
    private byte[] createSamplePdfContent() throws IOException {
        // Create a simple text file that simulates a document
        // OCR worker will extract this text
        String content = """
            Sample Document for Integration Testing
            
            This is a test document that will be processed by the OCR worker.
            The OCR worker will extract this text content.
            Then the GenAI worker will generate a summary of this content.
            Finally, the document will be indexed in ElasticSearch for full-text search.
            
            Additional content for testing purposes.
            """;

        // For a real PDF, you would need to create proper PDF bytes
        // For this test, we'll create a simple text file
        File tempFile = File.createTempFile("test-doc", ".txt");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(content.getBytes());
        }
        
        return content.getBytes();
    }
}
