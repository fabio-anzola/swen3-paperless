package at.technikum.swen3.integration;

import at.technikum.swen3.entity.User;
import at.technikum.swen3.repository.DocumentRepository;
import at.technikum.swen3.repository.UserRepository;
import at.technikum.swen3.service.S3Service;
import at.technikum.swen3.service.dtos.document.DocumentDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the document upload workflow.
 * Tests the complete flow:
 * 1. Upload document via REST API
 * 2. Verify document is stored in database
 * 3. Verify document is uploaded to MinIO (S3)
 * 4. Verify OCR message is sent to Kafka
 */
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class DocumentUploadIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private S3Service s3Service;

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
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser = userRepository.save(testUser);
    }

    @Test
    void testDocumentUploadWorkflow() throws Exception {
        // Prepare test file
        String filename = "test-document.txt";
        String fileContent = "This is a test document for OCR processing.";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                filename,
                MediaType.TEXT_PLAIN_VALUE,
                fileContent.getBytes()
        );

        // Step 1: Upload document via REST API
        MvcResult result = mockMvc.perform(multipart("/api/v1/document")
                        .file(file)
                        .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        DocumentDto documentDto = objectMapper.readValue(responseBody, DocumentDto.class);

        assertNotNull(documentDto);
        assertNotNull(documentDto.id());
        assertEquals(filename, documentDto.name());
        assertNotNull(documentDto.s3Key());

        // Step 2: Verify document is stored in database
        var savedDocument = documentRepository.findById(documentDto.id());
        assertTrue(savedDocument.isPresent());
        assertEquals(filename, savedDocument.get().getName());
        assertEquals(documentDto.s3Key(), savedDocument.get().getS3Key());
        assertEquals(testUser.getId(), savedDocument.get().getOwner().getId());

        // Step 3: Verify document is uploaded to MinIO (S3)
        byte[] downloadedContent = s3Service.downloadFile(documentDto.s3Key()).readAllBytes();
        assertNotNull(downloadedContent);
        assertEquals(fileContent, new String(downloadedContent));
        
        // Note: Kafka message verification is skipped in this test
        // The Kafka producer is tested separately in unit tests
    }

    @Test
    void testDocumentUploadWithCustomName() throws Exception {
        // Prepare test file
        String originalFilename = "original.pdf";
        String customName = "custom-document-name.pdf";
        byte[] fileContent = "PDF content here".getBytes();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                originalFilename,
                MediaType.APPLICATION_PDF_VALUE,
                fileContent
        );

        MockMultipartFile meta = new MockMultipartFile(
                "meta",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                String.format("{\"name\":\"%s\"}", customName).getBytes()
        );

        // Upload document with custom name
        MvcResult result = mockMvc.perform(multipart("/api/v1/document")
                        .file(file)
                        .file(meta)
                        .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        DocumentDto documentDto = objectMapper.readValue(responseBody, DocumentDto.class);

        // Verify custom name is used
        assertEquals(customName, documentDto.name());

        // Verify in database
        var savedDocument = documentRepository.findById(documentDto.id());
        assertTrue(savedDocument.isPresent());
        assertEquals(customName, savedDocument.get().getName());
    }

    @Test
    void testDocumentUploadWithEmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[0]
        );

        // Upload should fail with empty file
        mockMvc.perform(multipart("/api/v1/document")
                        .file(emptyFile)
                        .with(user(testUser.getUsername()).roles("USER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUnauthorizedUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "content".getBytes()
        );

        // Upload without authentication should fail
        mockMvc.perform(multipart("/api/v1/document")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }
}
