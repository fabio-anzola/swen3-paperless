package at.technikum.swen3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import at.technikum.swen3.entity.Document;
import at.technikum.swen3.entity.User;
import at.technikum.swen3.kafka.KafkaProducerService;
import at.technikum.swen3.repository.DocumentRepository;
import at.technikum.swen3.repository.UserRepository;
import at.technikum.swen3.service.DocumentSearchService;
import at.technikum.swen3.service.DocumentService;
import at.technikum.swen3.service.S3Service;
import at.technikum.swen3.service.dtos.document.DocumentDto;
import at.technikum.swen3.service.dtos.document.DocumentUploadDto;
import at.technikum.swen3.service.mapper.DocumentMapper;
import at.technikum.swen3.service.model.DocumentDownload;

class DocumentServiceTest {

    private DocumentRepository documentRepository;
    private UserRepository userRepository;
    private DocumentMapper documentMapper;
    private KafkaProducerService kafkaProducerService;
    private ObjectMapper objectMapper;
    private S3Service s3Service;
    private DocumentSearchService documentSearchService;
    private DocumentService documentService;

    @BeforeEach
    void setUp() throws Exception {
        documentRepository = mock(DocumentRepository.class);
        userRepository = mock(UserRepository.class);
        documentMapper = mock(DocumentMapper.class);
        kafkaProducerService = mock(KafkaProducerService.class);
        objectMapper = mock(ObjectMapper.class);
        s3Service = mock(S3Service.class);
        documentSearchService = mock(DocumentSearchService.class);
        documentService = new DocumentService(documentRepository, userRepository, documentMapper, kafkaProducerService, objectMapper, s3Service, documentSearchService);

        Field topicField = DocumentService.class.getDeclaredField("ocrTopic");
        topicField.setAccessible(true);
        topicField.set(documentService, "documents");
    }

    @Test
    void listMine_returnsPageOfDocumentDto() {
        Long userId = 1L;
        Pageable pageable = Pageable.unpaged();
        Document doc = new Document();
        doc.setId(1L);
        Page<Document> page = new PageImpl<>(Collections.singletonList(doc));
        DocumentDto dto = mock(DocumentDto.class);

        when(documentRepository.findAllByOwnerId(userId, pageable)).thenReturn(page);
        when(documentMapper.toDto(doc)).thenReturn(dto);

        Page<DocumentDto> result = documentService.listMine(userId, pageable);

        assertEquals(1, result.getTotalElements());
        verify(documentRepository).findAllByOwnerId(userId, pageable);
        verify(documentMapper).toDto(doc);
    }

    @Test
    void getMeta_returnsDocumentDto_whenOwner() {
        Long userId = 1L;
        Long docId = 2L;
        Document doc = new Document();
        User user = new User();
        user.setId(userId);
        doc.setOwner(user);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        DocumentDto dto = mock(DocumentDto.class);
        when(documentMapper.toDto(doc)).thenReturn(dto);

        DocumentDto result = documentService.getMeta(userId, docId);

        assertEquals(dto, result);
    }

    @Test
    void getMeta_throwsForbidden_whenNotOwner() {
        Long userId = 1L;
        Long docId = 2L;
        Document doc = new Document();
        User user = new User();
        user.setId(99L);
        doc.setOwner(user);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        assertThrows(ResponseStatusException.class, () -> documentService.getMeta(userId, docId));
    }

    @Test
    void download_returnsDocumentDownload_whenOwner() throws Exception {
        Long userId = 1L;
        Long docId = 2L;
        Document doc = new Document();
        User user = new User();
        user.setId(userId);
        doc.setOwner(user);
        doc.setName("test.txt");
        doc.setS3Key("s3-key-123");
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        // Mock S3Service
        io.minio.StatObjectResponse metadata = mock(io.minio.StatObjectResponse.class);
        when(metadata.contentType()).thenReturn("text/plain");
        when(metadata.size()).thenReturn(100L);
        when(s3Service.getObjectMetadata("s3-key-123")).thenReturn(metadata);
        when(s3Service.downloadFile("s3-key-123")).thenReturn(new java.io.ByteArrayInputStream("test content".getBytes()));

        DocumentDownload download = documentService.download(userId, docId);

        assertNotNull(download);
        assertEquals("test.txt", download.filename());
        assertEquals("text/plain", download.contentType());
        assertNotNull(download.body());
    }

    @Test
    void upload_savesDocument_andReturnsDto() throws Exception {
        Long userId = 1L;
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("file.txt");
        User user = new User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        DocumentUploadDto meta = mock(DocumentUploadDto.class);
        when(meta.name()).thenReturn(null);

        // Mock S3Service
        when(s3Service.uploadFile(file)).thenReturn("s3-key-123");

        Document doc = new Document();
        doc.setOwner(user);
        doc.setName("file.txt");
        doc.setS3Key("s3-key-123");
        when(documentRepository.save(any(Document.class))).thenReturn(doc);
        DocumentDto dto = mock(DocumentDto.class);
        when(documentMapper.toDto(any(Document.class))).thenReturn(dto);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"s3Key\":\"s3-key-123\"}");

        DocumentDto result = documentService.upload(userId, file, meta);

        assertEquals(dto, result);
        verify(documentRepository).save(any(Document.class));
        verify(kafkaProducerService).sendMessage(anyString(), anyString());
        verify(s3Service).uploadFile(file);
    }

    @Test
    void upload_throwsBadRequest_whenFileIsNull() {
        assertThrows(ResponseStatusException.class, () -> documentService.upload(1L, null, null));
    }

    @Test
    void updateMeta_updatesAndReturnsDto() {
        Long userId = 1L;
        Long docId = 2L;
        Document doc = new Document();
        User user = new User();
        user.setId(userId);
        doc.setOwner(user);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        DocumentUploadDto meta = mock(DocumentUploadDto.class);
        DocumentDto dto = mock(DocumentDto.class);
        when(documentMapper.toDto(doc)).thenReturn(dto);

        DocumentDto result = documentService.updateMeta(userId, docId, meta);

        assertEquals(dto, result);
        verify(documentMapper).updateEntityFromUpload(meta, doc);
    }

    @Test
    void delete_deletesDocument_whenOwner() {
        Long userId = 1L;
        Long docId = 2L;
        Document doc = new Document();
        User user = new User();
        user.setId(userId);
        doc.setOwner(user);
        doc.setS3Key("s3-key-789");
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        documentService.delete(userId, docId);

        verify(documentRepository).delete(doc);
        verify(s3Service).deleteFile("s3-key-789");
    }

    @Test
    void delete_throwsForbidden_whenNotOwner() {
        Long userId = 1L;
        Long docId = 2L;
        Document doc = new Document();
        User user = new User();
        user.setId(99L);
        doc.setOwner(user);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        assertThrows(ResponseStatusException.class, () -> documentService.delete(userId, docId));
    }

    @Test
    void getMeta_throwsNotFound_whenDocumentDoesNotExist() {
        Long userId = 1L;
        Long docId = 999L;
        when(documentRepository.findById(docId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> documentService.getMeta(userId, docId));
    }

    @Test
    void download_throwsNotFound_whenDocumentDoesNotExist() {
        Long userId = 1L;
        Long docId = 999L;
        when(documentRepository.findById(docId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> documentService.download(userId, docId));
    }

    @Test
    void download_throwsForbidden_whenNotOwner() {
        Long userId = 1L;
        Long docId = 2L;
        Document doc = new Document();
        User user = new User();
        user.setId(99L);
        doc.setOwner(user);
        doc.setS3Key("s3-key-test");
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        assertThrows(ResponseStatusException.class, () -> documentService.download(userId, docId));
    }

    @Test
    void upload_throwsBadRequest_whenFileIsEmpty() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> documentService.upload(1L, file, null));
    }

    @Test
    void upload_usesCustomName_whenProvidedInMeta() throws Exception {
        Long userId = 1L;
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("original.txt");
        User user = new User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        DocumentUploadDto meta = mock(DocumentUploadDto.class);
        when(meta.name()).thenReturn("custom-name.txt");

        // Mock S3Service
        when(s3Service.uploadFile(file)).thenReturn("s3-key-456");

        Document doc = new Document();
        doc.setOwner(user);
        doc.setName("custom-name.txt");
        doc.setS3Key("s3-key-456");
        when(documentRepository.save(any(Document.class))).thenReturn(doc);
        DocumentDto dto = mock(DocumentDto.class);
        when(documentMapper.toDto(any(Document.class))).thenReturn(dto);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"s3Key\":\"s3-key-456\"}");

        DocumentDto result = documentService.upload(userId, file, meta);

        assertEquals(dto, result);
        verify(documentRepository).save(any(Document.class));
        verify(s3Service).uploadFile(file);
    }

    @Test
    void upload_throwsUnauthorized_whenUserNotFound() {
        Long userId = 999L;
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> documentService.upload(userId, file, null));
    }

    @Test
    void updateMeta_throwsNotFound_whenDocumentDoesNotExist() {
        Long userId = 1L;
        Long docId = 999L;
        when(documentRepository.findById(docId)).thenReturn(Optional.empty());
        DocumentUploadDto meta = mock(DocumentUploadDto.class);

        assertThrows(ResponseStatusException.class, () -> documentService.updateMeta(userId, docId, meta));
    }

    @Test
    void updateMeta_throwsForbidden_whenNotOwner() {
        Long userId = 1L;
        Long docId = 2L;
        Document doc = new Document();
        User user = new User();
        user.setId(99L);
        doc.setOwner(user);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        DocumentUploadDto meta = mock(DocumentUploadDto.class);

        assertThrows(ResponseStatusException.class, () -> documentService.updateMeta(userId, docId, meta));
    }

    @Test
    void delete_throwsNotFound_whenDocumentDoesNotExist() {
        Long userId = 1L;
        Long docId = 999L;
        when(documentRepository.findById(docId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> documentService.delete(userId, docId));
    }

    @Test
    void download_returnsCorrectContentType_forPdfFile() throws Exception {
        Long userId = 1L;
        Long docId = 2L;
        Document doc = new Document();
        User user = new User();
        user.setId(userId);
        doc.setOwner(user);
        doc.setName("document.pdf");
        doc.setS3Key("s3-key-pdf");
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        // Mock S3Service
        io.minio.StatObjectResponse metadata = mock(io.minio.StatObjectResponse.class);
        when(metadata.contentType()).thenReturn("application/pdf");
        when(metadata.size()).thenReturn(200L);
        when(s3Service.getObjectMetadata("s3-key-pdf")).thenReturn(metadata);
        when(s3Service.downloadFile("s3-key-pdf")).thenReturn(new java.io.ByteArrayInputStream("pdf content".getBytes()));

        DocumentDownload download = documentService.download(userId, docId);

        assertNotNull(download);
        assertEquals("document.pdf", download.filename());
        assertEquals("application/pdf", download.contentType());
    }

    @Test
    void updateMeta_handlesNullMeta() {
        Long userId = 1L;
        Long docId = 2L;
        Document doc = new Document();
        User user = new User();
        user.setId(userId);
        doc.setOwner(user);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        DocumentDto dto = mock(DocumentDto.class);
        when(documentMapper.toDto(doc)).thenReturn(dto);

        DocumentDto result = documentService.updateMeta(userId, docId, null);

        assertEquals(dto, result);
        verify(documentMapper, never()).updateEntityFromUpload(any(), any());
    }
}
