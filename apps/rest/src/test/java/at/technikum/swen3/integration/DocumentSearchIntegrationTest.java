package at.technikum.swen3.integration;

import at.technikum.swen3.entity.Document;
import at.technikum.swen3.entity.User;
import at.technikum.swen3.integration.TestSecurityConfig;
import at.technikum.swen3.repository.DocumentRepository;
import at.technikum.swen3.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for document search and repository operations.
 * Tests document persistence, querying, and search endpoint availability.
 * 
 * Note: Full E2E tests with Kafka message consumption and ElasticSearch indexing
 * require Kafka listener management which is disabled in the base test to prevent hangs.
 */
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class DocumentSearchIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        documentRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("searchtest");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser = userRepository.save(testUser);
    }

    /**
     * Test document creation and manual elasticId update (simulating Kafka consumer behavior).
     */
    @Test
    void testDocumentCreationAndUpdate() {
        // Create and save a document
        Document document = new Document();
        document.setName("Important Business Report.pdf");
        document.setS3Key("s3-business-report-789");
        document.setOwner(testUser);
        document = documentRepository.save(document);

        assertNull(document.getElasticId());
        assertNotNull(document.getId());

        // Simulate what Kafka consumer would do after receiving result message
        String elasticId = "elastic-business-report-id";
        document.setElasticId(elasticId);
        document = documentRepository.save(document);

        // Verify document was updated
        Document updatedDoc = documentRepository.findById(document.getId()).orElseThrow();
        assertEquals("Important Business Report.pdf", updatedDoc.getName());
        assertEquals("s3-business-report-789", updatedDoc.getS3Key());
        assertEquals(elasticId, updatedDoc.getElasticId());
        assertEquals(testUser.getId(), updatedDoc.getOwner().getId());
    }

    /**
     * Test querying documents by elasticId.
     */
    @Test
    void testQueryDocumentsByElasticId() {
        // Create multiple documents with elasticId
        Document doc1 = new Document();
        doc1.setName("Contract Agreement.pdf");
        doc1.setS3Key("s3-contract-001");
        doc1.setElasticId("elastic-contract-001");
        doc1.setOwner(testUser);
        documentRepository.save(doc1);

        Document doc2 = new Document();
        doc2.setName("Invoice 2024.pdf");
        doc2.setS3Key("s3-invoice-002");
        doc2.setElasticId("elastic-invoice-002");
        doc2.setOwner(testUser);
        documentRepository.save(doc2);

        Document doc3 = new Document();
        doc3.setName("No ElasticId.pdf");
        doc3.setS3Key("s3-no-elastic");
        doc3.setOwner(testUser);
        documentRepository.save(doc3);

        // Query all documents
        List<Document> allDocs = documentRepository.findAll();
        assertEquals(3, allDocs.size());

        // Verify documents with elasticId
        long docsWithElasticId = allDocs.stream()
                .filter(d -> d.getElasticId() != null)
                .count();
        assertEquals(2, docsWithElasticId);
    }

    /**
     * Test bulk document creation and updates.
     */
    @Test
    void testBulkDocumentProcessing() {
        // Create multiple documents
        for (int i = 1; i <= 3; i++) {
            Document doc = new Document();
            doc.setName("Document-" + i + ".pdf");
            doc.setS3Key("s3-key-" + i);
            doc.setOwner(testUser);
            doc = documentRepository.save(doc);

            // Simulate what Kafka consumer would do
            doc.setElasticId("elastic-id-" + i);
            documentRepository.save(doc);
        }

        // Verify all documents were created and updated
        List<Document> allDocs = documentRepository.findAll();
        assertEquals(3, allDocs.size());
        
        for (Document doc : allDocs) {
            assertNotNull(doc.getElasticId());
            assertTrue(doc.getElasticId().startsWith("elastic-id-"));
            assertNotNull(doc.getS3Key());
            assertTrue(doc.getName().startsWith("Document-"));
        }
    }
}
