package at.technikum.swen3;

import at.technikum.swen3.entity.Document;
import at.technikum.swen3.entity.DocumentShare;
import at.technikum.swen3.entity.DocumentShareAccessLog;
import at.technikum.swen3.entity.User;
import at.technikum.swen3.repository.DocumentRepository;
import at.technikum.swen3.repository.DocumentShareAccessLogRepository;
import at.technikum.swen3.repository.DocumentShareRepository;
import at.technikum.swen3.service.DocumentShareService;
import at.technikum.swen3.service.S3Service;
import at.technikum.swen3.service.dtos.share.DocumentShareAccessLogDto;
import at.technikum.swen3.service.dtos.share.DocumentShareCreateDto;
import at.technikum.swen3.service.dtos.share.DocumentShareDto;
import at.technikum.swen3.service.model.DocumentDownload;
import io.minio.StatObjectResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DocumentShareServiceTest {

    private DocumentRepository documentRepository;
    private DocumentShareRepository documentShareRepository;
    private DocumentShareAccessLogRepository accessLogRepository;
    private S3Service s3Service;
    private DocumentShareService documentShareService;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        documentShareRepository = mock(DocumentShareRepository.class);
        accessLogRepository = mock(DocumentShareAccessLogRepository.class);
        s3Service = mock(S3Service.class);
        documentShareService = new DocumentShareService(documentRepository, documentShareRepository, accessLogRepository, s3Service);
        passwordEncoder = new BCryptPasswordEncoder();
    }

    private Document createMockDocument(Long id, Long ownerId, String s3Key) {
        Document doc = new Document();
        doc.setId(id);
        doc.setName("test.pdf");
        doc.setS3Key(s3Key);
        User owner = new User();
        owner.setId(ownerId);
        doc.setOwner(owner);
        return doc;
    }

    private DocumentShare createMockShare(Long id, Document document, String token) {
        DocumentShare share = new DocumentShare();
        share.setId(id);
        share.setDocument(document);
        share.setToken(token);
        share.setActive(true);
        share.setCreatedAt(Instant.now());
        return share;
    }

    @Test
    void createShare_withoutDto_shouldCreatePublicShare() {
        Long ownerId = 1L;
        Long documentId = 5L;
        Document doc = createMockDocument(documentId, ownerId, "s3-key");
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(doc));
        when(documentShareRepository.save(any(DocumentShare.class))).thenAnswer(invocation -> {
            DocumentShare share = invocation.getArgument(0);
            share.setId(10L);
            return share;
        });

        DocumentShareDto result = documentShareService.createShare(ownerId, documentId, null);

        assertNotNull(result);
        assertEquals(10L, result.id());
        assertEquals(documentId, result.documentId());
        assertNotNull(result.token());
        assertNotNull(result.startsAt());
        assertNull(result.expiresAt());
        assertTrue(result.active());
        assertFalse(result.passwordProtected());
        verify(documentShareRepository).save(any(DocumentShare.class));
    }

    @Test
    void createShare_withPasswordAndExpiry_shouldCreateProtectedShare() {
        Long ownerId = 1L;
        Long documentId = 5L;
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(3600);
        DocumentShareCreateDto dto = new DocumentShareCreateDto("secret123", now, expiresAt);
        
        Document doc = createMockDocument(documentId, ownerId, "s3-key");
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(doc));
        when(documentShareRepository.save(any(DocumentShare.class))).thenAnswer(invocation -> {
            DocumentShare share = invocation.getArgument(0);
            share.setId(10L);
            return share;
        });

        DocumentShareDto result = documentShareService.createShare(ownerId, documentId, dto);

        assertNotNull(result);
        assertTrue(result.passwordProtected());
        assertEquals(expiresAt, result.expiresAt());
        verify(documentShareRepository).save(argThat(share -> 
            share.getPasswordHash() != null && !share.getPasswordHash().isBlank()
        ));
    }

    @Test
    void createShare_withBlankPassword_shouldNotSetPassword() {
        Long ownerId = 1L;
        Long documentId = 5L;
        DocumentShareCreateDto dto = new DocumentShareCreateDto("   ", null, null);
        
        Document doc = createMockDocument(documentId, ownerId, "s3-key");
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(doc));
        when(documentShareRepository.save(any(DocumentShare.class))).thenAnswer(invocation -> {
            DocumentShare share = invocation.getArgument(0);
            share.setId(10L);
            return share;
        });

        DocumentShareDto result = documentShareService.createShare(ownerId, documentId, dto);

        assertFalse(result.passwordProtected());
        verify(documentShareRepository).save(argThat(share -> share.getPasswordHash() == null));
    }

    @Test
    void createShare_withExpiryBeforeStart_shouldThrowBadRequest() {
        Long ownerId = 1L;
        Long documentId = 5L;
        Instant now = Instant.now();
        Instant past = now.minusSeconds(3600);
        DocumentShareCreateDto dto = new DocumentShareCreateDto(null, now, past);
        
        Document doc = createMockDocument(documentId, ownerId, "s3-key");
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(doc));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> documentShareService.createShare(ownerId, documentId, dto));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("expiresAt must be after startsAt"));
        verify(documentShareRepository, never()).save(any());
    }

    @Test
    void createShare_documentNotFound_shouldThrowNotFound() {
        Long ownerId = 1L;
        Long documentId = 5L;
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> documentShareService.createShare(ownerId, documentId, null));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(documentShareRepository, never()).save(any());
    }

    @Test
    void createShare_notOwner_shouldThrowForbidden() {
        Long ownerId = 1L;
        Long documentId = 5L;
        Document doc = createMockDocument(documentId, 99L, "s3-key");
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(doc));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> documentShareService.createShare(ownerId, documentId, null));
        
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(documentShareRepository, never()).save(any());
    }

    @Test
    void listShares_shouldReturnAllSharesForDocument() {
        Long ownerId = 1L;
        Long documentId = 5L;
        Document doc = createMockDocument(documentId, ownerId, "s3-key");
        
        DocumentShare share1 = createMockShare(1L, doc, "token1");
        DocumentShare share2 = createMockShare(2L, doc, "token2");
        share2.setPasswordHash("hash");
        
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(doc));
        when(documentShareRepository.findAllByDocumentId(documentId))
                .thenReturn(List.of(share1, share2));

        List<DocumentShareDto> result = documentShareService.listShares(ownerId, documentId);

        assertEquals(2, result.size());
        assertEquals("token1", result.get(0).token());
        assertFalse(result.get(0).passwordProtected());
        assertEquals("token2", result.get(1).token());
        assertTrue(result.get(1).passwordProtected());
        verify(documentShareRepository).findAllByDocumentId(documentId);
    }

    @Test
    void listShares_emptyList_shouldReturnEmptyList() {
        Long ownerId = 1L;
        Long documentId = 5L;
        Document doc = createMockDocument(documentId, ownerId, "s3-key");
        
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(doc));
        when(documentShareRepository.findAllByDocumentId(documentId)).thenReturn(List.of());

        List<DocumentShareDto> result = documentShareService.listShares(ownerId, documentId);

        assertTrue(result.isEmpty());
    }

    @Test
    void listLogs_shouldReturnAccessLogs() {
        Long ownerId = 1L;
        Long documentId = 5L;
        Document doc = createMockDocument(documentId, ownerId, "s3-key");
        DocumentShare share = createMockShare(1L, doc, "token");
        
        DocumentShareAccessLog log1 = new DocumentShareAccessLog();
        log1.setShare(share);
        log1.setAccessedAt(Instant.now());
        log1.setSuccess(true);
        log1.setRemoteAddress("192.168.1.1");
        log1.setUserAgent("Mozilla/5.0");
        
        DocumentShareAccessLog log2 = new DocumentShareAccessLog();
        log2.setShare(share);
        log2.setAccessedAt(Instant.now().minusSeconds(60));
        log2.setSuccess(false);
        log2.setReason("invalid_password");
        log2.setRemoteAddress("192.168.1.2");
        
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(doc));
        when(accessLogRepository.findAllByShareDocumentIdOrderByAccessedAtDesc(documentId))
                .thenReturn(List.of(log1, log2));

        List<DocumentShareAccessLogDto> result = documentShareService.listLogs(ownerId, documentId);

        assertEquals(2, result.size());
        assertTrue(result.get(0).success());
        assertFalse(result.get(1).success());
        assertEquals("invalid_password", result.get(1).reason());
    }

    @Test
    void deactivateShare_shouldSetActiveToFalse() {
        Long ownerId = 1L;
        Long documentId = 5L;
        Long shareId = 10L;
        
        Document doc = createMockDocument(documentId, ownerId, "s3-key");
        DocumentShare share = createMockShare(shareId, doc, "token");
        
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(doc));
        when(documentShareRepository.findById(shareId)).thenReturn(Optional.of(share));
        when(documentShareRepository.save(any(DocumentShare.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentShareDto result = documentShareService.deactivateShare(ownerId, documentId, shareId);

        assertFalse(result.active());
        verify(documentShareRepository).save(argThat(s -> !s.isActive()));
    }

    @Test
    void deactivateShare_shareNotFound_shouldThrowNotFound() {
        Long ownerId = 1L;
        Long documentId = 5L;
        Long shareId = 10L;
        
        when(documentShareRepository.findById(shareId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> documentShareService.deactivateShare(ownerId, documentId, shareId));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Share not found"));
    }

    @Test
    void deactivateShare_wrongDocument_shouldThrowNotFound() {
        Long ownerId = 1L;
        Long documentId = 5L;
        Long shareId = 10L;
        
        Document wrongDoc = createMockDocument(99L, ownerId, "s3-key");
        DocumentShare share = createMockShare(shareId, wrongDoc, "token");
        
        when(documentShareRepository.findById(shareId)).thenReturn(Optional.of(share));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> documentShareService.deactivateShare(ownerId, documentId, shareId));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Share not found for this document"));
    }

    @Test
    void downloadShared_validToken_shouldReturnDocument() throws Exception {
        String token = "valid-token";
        Document doc = createMockDocument(1L, 1L, "s3-key");
        doc.setName("document.pdf");
        DocumentShare share = createMockShare(1L, doc, token);
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        
        byte[] content = "PDF content".getBytes();
        StatObjectResponse metadata = mock(StatObjectResponse.class);
        when(metadata.contentType()).thenReturn("application/pdf");
        when(metadata.size()).thenReturn((long) content.length);
        
        when(documentShareRepository.findByToken(token)).thenReturn(Optional.of(share));
        when(s3Service.getObjectMetadata("s3-key")).thenReturn(metadata);
        when(s3Service.downloadFile("s3-key")).thenReturn(new ByteArrayInputStream(content));

        DocumentDownload result = documentShareService.downloadShared(token, null, request);

        assertNotNull(result);
        assertEquals("document.pdf", result.filename());
        assertEquals("application/pdf", result.contentType());
        assertEquals(content.length, result.contentLength());
        verify(accessLogRepository).save(argThat(log -> log.isSuccess() && log.getReason() == null));
    }

    @Test
    void downloadShared_withPassword_shouldValidatePassword() throws Exception {
        String token = "valid-token";
        String password = "secret123";
        Document doc = createMockDocument(1L, 1L, "s3-key");
        DocumentShare share = createMockShare(1L, doc, token);
        share.setPasswordHash(passwordEncoder.encode(password));
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        
        byte[] content = "content".getBytes();
        StatObjectResponse metadata = mock(StatObjectResponse.class);
        when(metadata.contentType()).thenReturn("text/plain");
        when(metadata.size()).thenReturn((long) content.length);
        
        when(documentShareRepository.findByToken(token)).thenReturn(Optional.of(share));
        when(s3Service.getObjectMetadata(anyString())).thenReturn(metadata);
        when(s3Service.downloadFile(anyString())).thenReturn(new ByteArrayInputStream(content));

        DocumentDownload result = documentShareService.downloadShared(token, password, request);

        assertNotNull(result);
        verify(accessLogRepository).save(argThat(log -> log.isSuccess()));
    }

    @Test
    void downloadShared_invalidPassword_shouldThrowForbidden() {
        String token = "valid-token";
        Document doc = createMockDocument(1L, 1L, "s3-key");
        DocumentShare share = createMockShare(1L, doc, token);
        share.setPasswordHash(passwordEncoder.encode("secret123"));
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        
        when(documentShareRepository.findByToken(token)).thenReturn(Optional.of(share));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> documentShareService.downloadShared(token, "wrongpassword", request));
        
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Invalid password"));
        verify(accessLogRepository).save(argThat(log -> 
            !log.isSuccess() && "invalid_password".equals(log.getReason())
        ));
    }

    @Test
    void downloadShared_noPasswordProvided_shouldThrowForbidden() {
        String token = "valid-token";
        Document doc = createMockDocument(1L, 1L, "s3-key");
        DocumentShare share = createMockShare(1L, doc, token);
        share.setPasswordHash(passwordEncoder.encode("secret123"));
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        
        when(documentShareRepository.findByToken(token)).thenReturn(Optional.of(share));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> documentShareService.downloadShared(token, null, request));
        
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(accessLogRepository).save(argThat(log -> !log.isSuccess()));
    }

    @Test
    void downloadShared_inactiveShare_shouldThrowGone() {
        String token = "valid-token";
        Document doc = createMockDocument(1L, 1L, "s3-key");
        DocumentShare share = createMockShare(1L, doc, token);
        share.setActive(false);
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        
        when(documentShareRepository.findByToken(token)).thenReturn(Optional.of(share));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> documentShareService.downloadShared(token, null, request));
        
        assertEquals(HttpStatus.GONE, exception.getStatusCode());
        assertTrue(exception.getReason().contains("no longer active"));
        verify(accessLogRepository).save(argThat(log -> 
            !log.isSuccess() && "inactive".equals(log.getReason())
        ));
    }

    @Test
    void downloadShared_notStartedYet_shouldThrowForbidden() {
        String token = "valid-token";
        Document doc = createMockDocument(1L, 1L, "s3-key");
        DocumentShare share = createMockShare(1L, doc, token);
        share.setStartsAt(Instant.now().plusSeconds(3600));
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        
        when(documentShareRepository.findByToken(token)).thenReturn(Optional.of(share));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> documentShareService.downloadShared(token, null, request));
        
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("not active yet"));
        verify(accessLogRepository).save(argThat(log -> 
            !log.isSuccess() && "not_started".equals(log.getReason())
        ));
    }

    @Test
    void downloadShared_expired_shouldThrowGone() {
        String token = "valid-token";
        Document doc = createMockDocument(1L, 1L, "s3-key");
        DocumentShare share = createMockShare(1L, doc, token);
        share.setExpiresAt(Instant.now().minusSeconds(3600));
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        
        when(documentShareRepository.findByToken(token)).thenReturn(Optional.of(share));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> documentShareService.downloadShared(token, null, request));
        
        assertEquals(HttpStatus.GONE, exception.getStatusCode());
        assertTrue(exception.getReason().contains("expired"));
        verify(accessLogRepository).save(argThat(log -> 
            !log.isSuccess() && "expired".equals(log.getReason())
        ));
    }

    @Test
    void downloadShared_tokenNotFound_shouldThrowNotFound() {
        String token = "invalid-token";
        when(documentShareRepository.findByToken(token)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> documentShareService.downloadShared(token, null, null));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(accessLogRepository, never()).save(any());
    }

    @Test
    void downloadShared_s3Failure_shouldThrowInternalServerError() {
        String token = "valid-token";
        Document doc = createMockDocument(1L, 1L, "s3-key");
        DocumentShare share = createMockShare(1L, doc, token);
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        
        when(documentShareRepository.findByToken(token)).thenReturn(Optional.of(share));
        when(s3Service.getObjectMetadata(anyString())).thenThrow(new RuntimeException("S3 error"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> documentShareService.downloadShared(token, null, request));
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        verify(accessLogRepository).save(argThat(log -> 
            !log.isSuccess() && "download_failed".equals(log.getReason())
        ));
    }

    @Test
    void downloadShared_withXForwardedFor_shouldUseForwardedAddress() throws Exception {
        String token = "valid-token";
        Document doc = createMockDocument(1L, 1L, "s3-key");
        DocumentShare share = createMockShare(1L, doc, token);
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 198.51.100.1");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        
        byte[] content = "content".getBytes();
        StatObjectResponse metadata = mock(StatObjectResponse.class);
        when(metadata.contentType()).thenReturn("text/plain");
        when(metadata.size()).thenReturn((long) content.length);
        
        when(documentShareRepository.findByToken(token)).thenReturn(Optional.of(share));
        when(s3Service.getObjectMetadata(anyString())).thenReturn(metadata);
        when(s3Service.downloadFile(anyString())).thenReturn(new ByteArrayInputStream(content));

        documentShareService.downloadShared(token, null, request);

        verify(accessLogRepository).save(argThat(log -> 
            "203.0.113.1".equals(log.getRemoteAddress())
        ));
    }

    @Test
    void downloadShared_nullRequest_shouldHandleGracefully() throws Exception {
        String token = "valid-token";
        Document doc = createMockDocument(1L, 1L, "s3-key");
        DocumentShare share = createMockShare(1L, doc, token);
        
        byte[] content = "content".getBytes();
        StatObjectResponse metadata = mock(StatObjectResponse.class);
        when(metadata.contentType()).thenReturn("text/plain");
        when(metadata.size()).thenReturn((long) content.length);
        
        when(documentShareRepository.findByToken(token)).thenReturn(Optional.of(share));
        when(s3Service.getObjectMetadata(anyString())).thenReturn(metadata);
        when(s3Service.downloadFile(anyString())).thenReturn(new ByteArrayInputStream(content));

        DocumentDownload result = documentShareService.downloadShared(token, null, null);

        assertNotNull(result);
        verify(accessLogRepository).save(argThat(log -> 
            log.getRemoteAddress() == null && log.getUserAgent() == null
        ));
    }

    @Test
    void downloadShared_longUserAgent_shouldTrimTo512Chars() throws Exception {
        String token = "valid-token";
        Document doc = createMockDocument(1L, 1L, "s3-key");
        DocumentShare share = createMockShare(1L, doc, token);
        
        String longUserAgent = "X".repeat(600);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getHeader("User-Agent")).thenReturn(longUserAgent);
        
        byte[] content = "content".getBytes();
        StatObjectResponse metadata = mock(StatObjectResponse.class);
        when(metadata.contentType()).thenReturn("text/plain");
        when(metadata.size()).thenReturn((long) content.length);
        
        when(documentShareRepository.findByToken(token)).thenReturn(Optional.of(share));
        when(s3Service.getObjectMetadata(anyString())).thenReturn(metadata);
        when(s3Service.downloadFile(anyString())).thenReturn(new ByteArrayInputStream(content));

        documentShareService.downloadShared(token, null, request);

        verify(accessLogRepository).save(argThat(log -> 
            log.getUserAgent() != null && log.getUserAgent().length() == 512
        ));
    }
}
