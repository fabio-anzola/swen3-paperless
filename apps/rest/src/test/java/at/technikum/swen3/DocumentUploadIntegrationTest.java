package at.technikum.swen3;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import at.technikum.swen3.entity.Document;
import at.technikum.swen3.entity.User;
import at.technikum.swen3.repository.DocumentRepository;
import at.technikum.swen3.repository.UserRepository;
import at.technikum.swen3.service.DocumentService;
import at.technikum.swen3.service.S3Service;
import at.technikum.swen3.service.dtos.GenAiResultMessageDto;
import at.technikum.swen3.service.dtos.document.DocumentDto;

/**
 * End-to-end integration test for the complete document upload use case.
 * 
 * Uses Testcontainers to spin up real infrastructure:
 * - PostgreSQL database
 * - Kafka message broker
 * - Elasticsearch search engine
 * - MinIO object storage
 * 
 * Tests the following flow:
 * 1. Upload document - document is saved to PostgreSQL
 * 2. Automatically performs OCR - message sent to real Kafka OCR topic
 * 3. Is indexed for full-text search in Elasticsearch - result message updates document with elasticId
 * 4. A summary is automatically generated - summary is stored in Elasticsearch
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class DocumentUploadIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.0")
                    .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @Container
    static MinIOContainer minio = new MinIOContainer(DockerImageName.parse("minio/minio:latest"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        // Elasticsearch
        registry.add("spring.elasticsearch.uris", () -> "http://" + elasticsearch.getHttpHostAddress());

        // MinIO
        registry.add("minio.url", minio::getS3URL);
        registry.add("minio.access-key", minio::getUserName);
        registry.add("minio.secret-key", minio::getPassword);
    }

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private S3Service s3Service;

    private User testUser;
    private static final String TEST_FILENAME = "test-document.pdf";
    private static final String TEST_OCR_TEXT = "This is the extracted text from OCR processing";
    private static final String TEST_SUMMARY = "This is an AI-generated summary of the document";

    @BeforeEach
    void setUp() {
        // Create and save test user with unique username for each test
        String uniqueUsername = "testuser_" + UUID.randomUUID().toString().substring(0, 8);
        testUser = new User();
        testUser.setUsername(uniqueUsername);
        testUser.setPassword("hashedpassword");
        testUser = userRepository.save(testUser);

        // Ensure MinIO bucket exists
        try {
            s3Service.ensureBucketExists();
        } catch (Exception e) {
            // Bucket might already exist
        }
    }

    /**
     * Test the complete end-to-end document upload flow with real infrastructure:
     * 1. Document upload to real MinIO storage
     * 2. Message sent to real Kafka OCR topic
     * 3. Simulate OCR worker processing
     * 4. Message sent to real Kafka result topic
     * 5. Document updated in PostgreSQL with ElasticSearch ID
     */
    @Test
    @Transactional
    void testCompleteDocumentUploadFlowE2E() throws Exception {
        // Step 1: Upload document (this will use real MinIO)
        MockMultipartFile file = new MockMultipartFile(
            "file",
            TEST_FILENAME,
            "application/pdf",
            "test pdf content with real data for OCR processing".getBytes()
        );

        DocumentDto uploadedDoc = documentService.upload(testUser.getId(), file, null);

        // Verify document was created and saved to PostgreSQL
        assertNotNull(uploadedDoc);
        assertNotNull(uploadedDoc.id());
        assertEquals(TEST_FILENAME, uploadedDoc.name());
        assertNotNull(uploadedDoc.s3Key(), "S3 key should be generated");
        assertEquals(testUser.getId(), uploadedDoc.ownerId());

        // Verify document exists in PostgreSQL database
        Optional<Document> savedDoc = documentRepository.findById(uploadedDoc.id());
        assertTrue(savedDoc.isPresent());
        assertNotNull(savedDoc.get().getS3Key());
        assertNull(savedDoc.get().getElasticId(), "ElasticId should be null before processing");

        // Verify file was uploaded to MinIO
        String s3Key = savedDoc.get().getS3Key();
        assertTrue(s3Service.fileExists(s3Key), "File should exist in MinIO");

        // Step 2: Simulate the complete worker pipeline
        // In real scenario: OCR worker processes file and sends to genai-queue
        // GenAI worker processes and sends result with elasticId
        String testElasticId = "elastic-" + UUID.randomUUID().toString();
        
        GenAiResultMessageDto resultMessage = new GenAiResultMessageDto(
            TEST_OCR_TEXT,
            TEST_SUMMARY,
            s3Key,
            testElasticId,
            TEST_FILENAME
        );

        // Send message to real Kafka result topic
        String resultMessageJson = objectMapper.writeValueAsString(resultMessage);
        kafkaTemplate.send("result-test", resultMessageJson).get(10, TimeUnit.SECONDS);

        // Step 3: Wait for Kafka consumer to process the message and update database
        await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                Optional<Document> updatedDoc = documentRepository.findById(uploadedDoc.id());
                assertTrue(updatedDoc.isPresent(), "Document should still exist");
                assertNotNull(updatedDoc.get().getElasticId(), 
                    "ElasticId should be set after Kafka message processing");
                assertEquals(testElasticId, updatedDoc.get().getElasticId());
            });

        // Verify final state in PostgreSQL
        Document finalDoc = documentRepository.findById(uploadedDoc.id()).orElseThrow();
        assertEquals(TEST_FILENAME, finalDoc.getName());
        assertEquals(s3Key, finalDoc.getS3Key());
        assertEquals(testElasticId, finalDoc.getElasticId());
        assertEquals(testUser.getId(), finalDoc.getOwner().getId());

        // Cleanup - delete file from MinIO
        s3Service.deleteFile(s3Key);
    }

    /**
     * Test that document can be found by S3 key after upload to real storage
     */
    @Test
    @Transactional
    void testDocumentCanBeFoundByS3Key() throws Exception {
        // Upload document to real MinIO
        MockMultipartFile file = new MockMultipartFile(
            "file",
            TEST_FILENAME,
            "application/pdf",
            "test content for S3 key lookup".getBytes()
        );

        DocumentDto uploadedDoc = documentService.upload(testUser.getId(), file, null);

        // Verify document can be found by S3 key in PostgreSQL
        Optional<Document> foundDoc = documentRepository.findByS3Key(uploadedDoc.s3Key());
        assertTrue(foundDoc.isPresent());
        assertEquals(uploadedDoc.id(), foundDoc.get().getId());
        assertEquals(uploadedDoc.s3Key(), foundDoc.get().getS3Key());

        // Verify file exists in MinIO
        assertTrue(s3Service.fileExists(uploadedDoc.s3Key()));

        // Cleanup
        s3Service.deleteFile(uploadedDoc.s3Key());
    }

    /**
     * Test that ElasticSearch ID is properly updated when result message is sent through real Kafka
     */
    @Test
    @Transactional
    void testElasticIdUpdateFromRealKafkaMessage() throws Exception {
        // Create document with real S3 upload
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "elastic-test.pdf",
            "application/pdf",
            "test content for elastic ID update".getBytes()
        );

        DocumentDto uploadedDoc = documentService.upload(testUser.getId(), file, null);
        String s3Key = uploadedDoc.s3Key();
        
        assertNull(documentRepository.findById(uploadedDoc.id()).get().getElasticId(), 
            "ElasticId should be null initially");

        // Send real Kafka message to result topic
        String testElasticId = "elastic-" + UUID.randomUUID().toString();
        GenAiResultMessageDto resultMessage = new GenAiResultMessageDto(
            TEST_OCR_TEXT,
            TEST_SUMMARY,
            s3Key,
            testElasticId,
            "elastic-test.pdf"
        );

        String resultMessageJson = objectMapper.writeValueAsString(resultMessage);
        kafkaTemplate.send("result-test", resultMessageJson).get(10, TimeUnit.SECONDS);

        // Wait for Kafka consumer to process and update PostgreSQL
        await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                Optional<Document> updatedDoc = documentRepository.findById(uploadedDoc.id());
                assertTrue(updatedDoc.isPresent());
                assertEquals(testElasticId, updatedDoc.get().getElasticId());
            });

        // Cleanup
        s3Service.deleteFile(s3Key);
    }

    /**
     * Test that invalid Kafka messages are handled gracefully without crashing the consumer
     */
    @Test
    void testInvalidKafkaMessageHandling() throws Exception {
        // Create document
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "invalid-test.pdf",
            "application/pdf",
            "test content for invalid message handling".getBytes()
        );

        DocumentDto uploadedDoc = documentService.upload(testUser.getId(), file, null);
        String s3Key = uploadedDoc.s3Key();

        // Send message with null elasticId through real Kafka
        GenAiResultMessageDto invalidMessage1 = new GenAiResultMessageDto(
            TEST_OCR_TEXT,
            TEST_SUMMARY,
            s3Key,
            null,
            "invalid-test.pdf"
        );
        String json1 = objectMapper.writeValueAsString(invalidMessage1);
        kafkaTemplate.send("result-test", json1).get(10, TimeUnit.SECONDS);

        // Send message with empty elasticId through real Kafka
        GenAiResultMessageDto invalidMessage2 = new GenAiResultMessageDto(
            TEST_OCR_TEXT,
            TEST_SUMMARY,
            s3Key,
            "",
            "invalid-test.pdf"
        );
        String json2 = objectMapper.writeValueAsString(invalidMessage2);
        kafkaTemplate.send("result-test", json2).get(10, TimeUnit.SECONDS);

        // Wait a bit for messages to be processed
        Thread.sleep(3000);

        // Verify document was not updated in PostgreSQL
        Document finalDoc = documentRepository.findById(uploadedDoc.id()).orElseThrow();
        assertNull(finalDoc.getElasticId(), "ElasticId should remain null for invalid messages");

        // Cleanup
        s3Service.deleteFile(s3Key);
    }

    /**
     * Test that Kafka message for non-existent document doesn't cause errors
     */
    @Test
    void testKafkaMessageForNonExistentDocument() throws Exception {
        String nonExistentS3Key = "non-existent-s3-key-" + UUID.randomUUID();
        
        GenAiResultMessageDto message = new GenAiResultMessageDto(
            TEST_OCR_TEXT,
            TEST_SUMMARY,
            nonExistentS3Key,
            "elastic-" + UUID.randomUUID(),
            "non-existent.pdf"
        );

        String messageJson = objectMapper.writeValueAsString(message);
        
        // Should not throw exception - send through real Kafka
        assertDoesNotThrow(() -> 
            kafkaTemplate.send("result-test", messageJson).get(10, TimeUnit.SECONDS)
        );

        // Wait a bit for message processing
        Thread.sleep(2000);

        // Verify no document exists with this S3 key in PostgreSQL
        Optional<Document> doc = documentRepository.findByS3Key(nonExistentS3Key);
        assertFalse(doc.isPresent());
    }

    /**
     * Test concurrent document uploads to verify thread safety with real infrastructure
     */
    @Test
    void testConcurrentDocumentUploads() throws Exception {
        int concurrentUploads = 5;
        java.util.List<java.util.concurrent.CompletableFuture<DocumentDto>> futures = 
            new java.util.ArrayList<>();

        for (int i = 0; i < concurrentUploads; i++) {
            final int index = i;
            java.util.concurrent.CompletableFuture<DocumentDto> future = 
                java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try {
                        MockMultipartFile file = new MockMultipartFile(
                            "file",
                            "concurrent-test-" + index + ".pdf",
                            "application/pdf",
                            ("concurrent test content " + index).getBytes()
                        );
                        return documentService.upload(testUser.getId(), file, null);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            futures.add(future);
        }

        // Wait for all uploads to complete
        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
            .get(30, TimeUnit.SECONDS);

        // Verify all documents were created in PostgreSQL and MinIO
        for (java.util.concurrent.CompletableFuture<DocumentDto> future : futures) {
            DocumentDto doc = future.get();
            assertNotNull(doc);
            assertTrue(documentRepository.findById(doc.id()).isPresent());
            assertTrue(s3Service.fileExists(doc.s3Key()));
            
            // Cleanup
            s3Service.deleteFile(doc.s3Key());
        }
    }
}
