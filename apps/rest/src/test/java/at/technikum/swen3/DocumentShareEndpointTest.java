package at.technikum.swen3;

import at.technikum.swen3.endpoint.DocumentShareEndpoint;
import at.technikum.swen3.entity.User;
import at.technikum.swen3.service.IDocumentShareService;
import at.technikum.swen3.service.IUserService;
import at.technikum.swen3.service.dtos.share.DocumentShareAccessLogDto;
import at.technikum.swen3.service.dtos.share.DocumentShareCreateDto;
import at.technikum.swen3.service.dtos.share.DocumentShareDto;
import at.technikum.swen3.service.model.DocumentDownload;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentShareEndpointTest {

    private DocumentShareEndpoint documentShareEndpoint;

    @Mock
    private IDocumentShareService documentShareService;

    @Mock
    private IUserService userService;

    @Mock
    private Authentication authentication;

    @Mock
    private HttpServletRequest httpServletRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        documentShareEndpoint = new DocumentShareEndpoint(documentShareService, userService);
    }

    private Long mockUserAndAuthentication(String username) {
        Long userId = 1L;
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);
        when(authentication.getName()).thenReturn(username);
        when(userService.findByUsername(username)).thenReturn(mockUser);
        return userId;
    }

    @Test
    void createShare_shouldReturnShareDto() {
        String username = "testUser";
        Long userId = mockUserAndAuthentication(username);
        Long documentId = 5L;
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(3600);
        DocumentShareCreateDto createDto = new DocumentShareCreateDto("password123", now, expiresAt);
        DocumentShareDto expectedDto = new DocumentShareDto(
                1L,
                documentId,
                "token123",
                now,
                expiresAt,
                true,
                true,
                now
        );

        when(documentShareService.createShare(userId, documentId, createDto)).thenReturn(expectedDto);

        DocumentShareDto result = documentShareEndpoint.createShare(authentication, documentId, createDto);

        assertEquals(expectedDto, result);
        verify(userService).findByUsername(username);
        verify(documentShareService).createShare(userId, documentId, createDto);
    }

    @Test
    void createShare_withNullDto_shouldReturnShareDto() {
        String username = "testUser";
        Long userId = mockUserAndAuthentication(username);
        Long documentId = 5L;
        Instant now = Instant.now();
        DocumentShareDto expectedDto = new DocumentShareDto(
                1L,
                documentId,
                "token123",
                now,
                null,
                true,
                false,
                now
        );

        when(documentShareService.createShare(userId, documentId, null)).thenReturn(expectedDto);

        DocumentShareDto result = documentShareEndpoint.createShare(authentication, documentId, null);

        assertEquals(expectedDto, result);
        verify(userService).findByUsername(username);
        verify(documentShareService).createShare(userId, documentId, null);
    }

    @Test
    void listShares_shouldReturnListOfShares() {
        String username = "testUser";
        Long userId = mockUserAndAuthentication(username);
        Long documentId = 5L;
        Instant now = Instant.now();
        List<DocumentShareDto> expectedShares = List.of(
                new DocumentShareDto(1L, documentId, "token1", now, null, true, false, now),
                new DocumentShareDto(2L, documentId, "token2", now, now.plusSeconds(7200), true, true, now),
                new DocumentShareDto(3L, documentId, "token3", now, now.plusSeconds(3600), false, false, now)
        );

        when(documentShareService.listShares(userId, documentId)).thenReturn(expectedShares);

        List<DocumentShareDto> result = documentShareEndpoint.listShares(authentication, documentId);

        assertEquals(expectedShares, result);
        assertEquals(3, result.size());
        verify(userService).findByUsername(username);
        verify(documentShareService).listShares(userId, documentId);
    }

    @Test
    void listShares_shouldReturnEmptyList_whenNoShares() {
        String username = "testUser";
        Long userId = mockUserAndAuthentication(username);
        Long documentId = 5L;
        List<DocumentShareDto> expectedShares = List.of();

        when(documentShareService.listShares(userId, documentId)).thenReturn(expectedShares);

        List<DocumentShareDto> result = documentShareEndpoint.listShares(authentication, documentId);

        assertTrue(result.isEmpty());
        verify(userService).findByUsername(username);
        verify(documentShareService).listShares(userId, documentId);
    }

    @Test
    void listShareLogs_shouldReturnListOfAccessLogs() {
        String username = "testUser";
        Long userId = mockUserAndAuthentication(username);
        Long documentId = 5L;
        Instant now = Instant.now();
        List<DocumentShareAccessLogDto> expectedLogs = List.of(
                new DocumentShareAccessLogDto(1L, now, true, "192.168.1.1", "Mozilla/5.0", null),
                new DocumentShareAccessLogDto(1L, now.minusSeconds(60), false, "192.168.1.2", "Chrome/91.0", "Invalid password"),
                new DocumentShareAccessLogDto(2L, now.minusSeconds(120), true, "10.0.0.1", "Safari/14.0", null)
        );

        when(documentShareService.listLogs(userId, documentId)).thenReturn(expectedLogs);

        List<DocumentShareAccessLogDto> result = documentShareEndpoint.listShareLogs(authentication, documentId);

        assertEquals(expectedLogs, result);
        assertEquals(3, result.size());
        verify(userService).findByUsername(username);
        verify(documentShareService).listLogs(userId, documentId);
    }

    @Test
    void listShareLogs_shouldReturnEmptyList_whenNoLogs() {
        String username = "testUser";
        Long userId = mockUserAndAuthentication(username);
        Long documentId = 5L;
        List<DocumentShareAccessLogDto> expectedLogs = List.of();

        when(documentShareService.listLogs(userId, documentId)).thenReturn(expectedLogs);

        List<DocumentShareAccessLogDto> result = documentShareEndpoint.listShareLogs(authentication, documentId);

        assertTrue(result.isEmpty());
        verify(userService).findByUsername(username);
        verify(documentShareService).listLogs(userId, documentId);
    }

    @Test
    void deactivateShare_shouldReturnDeactivatedShare() {
        String username = "testUser";
        Long userId = mockUserAndAuthentication(username);
        Long documentId = 5L;
        Long shareId = 10L;
        Instant now = Instant.now();
        DocumentShareDto expectedDto = new DocumentShareDto(
                shareId,
                documentId,
                "token123",
                now,
                now.plusSeconds(3600),
                false,
                true,
                now
        );

        when(documentShareService.deactivateShare(userId, documentId, shareId)).thenReturn(expectedDto);

        DocumentShareDto result = documentShareEndpoint.deactivateShare(authentication, documentId, shareId);

        assertEquals(expectedDto, result);
        assertFalse(result.active());
        verify(userService).findByUsername(username);
        verify(documentShareService).deactivateShare(userId, documentId, shareId);
    }

    @Test
    void downloadShared_shouldReturnResourceWithHeaders() {
        String token = "valid-token";
        String password = "password123";
        byte[] content = "Test file content".getBytes();
        Resource resource = new ByteArrayResource(content);
        DocumentDownload download = new DocumentDownload(
                resource,
                "application/pdf",
                "document.pdf",
                (long) content.length
        );

        when(documentShareService.downloadShared(token, password, httpServletRequest)).thenReturn(download);

        ResponseEntity<Resource> response = documentShareEndpoint.downloadShared(token, password, httpServletRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(resource, response.getBody());

        HttpHeaders headers = response.getHeaders();
        assertNotNull(headers.getContentDisposition());
        assertEquals("document.pdf", headers.getContentDisposition().getFilename());
        assertEquals(content.length, headers.getContentLength());
        assertEquals(MediaType.parseMediaType("application/pdf"), headers.getContentType());

        verify(documentShareService).downloadShared(token, password, httpServletRequest);
    }

    @Test
    void downloadShared_withoutPassword_shouldReturnResource() {
        String token = "valid-token";
        byte[] content = "Public file content".getBytes();
        Resource resource = new ByteArrayResource(content);
        DocumentDownload download = new DocumentDownload(
                resource,
                "text/plain",
                "public.txt",
                (long) content.length
        );

        when(documentShareService.downloadShared(token, null, httpServletRequest)).thenReturn(download);

        ResponseEntity<Resource> response = documentShareEndpoint.downloadShared(token, null, httpServletRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(resource, response.getBody());
        assertEquals("public.txt", response.getHeaders().getContentDisposition().getFilename());
        assertEquals(MediaType.parseMediaType("text/plain"), response.getHeaders().getContentType());

        verify(documentShareService).downloadShared(token, null, httpServletRequest);
    }

    @Test
    void downloadShared_withoutContentLength_shouldReturnResourceWithoutLengthHeader() {
        String token = "valid-token";
        byte[] content = "Test content".getBytes();
        Resource resource = new ByteArrayResource(content);
        DocumentDownload download = new DocumentDownload(
                resource,
                "text/plain",
                "test.txt",
                null
        );

        when(documentShareService.downloadShared(token, null, httpServletRequest)).thenReturn(download);

        ResponseEntity<Resource> response = documentShareEndpoint.downloadShared(token, null, httpServletRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(-1, response.getHeaders().getContentLength());

        verify(documentShareService).downloadShared(token, null, httpServletRequest);
    }

    @Test
    void downloadShared_withComplexFilename_shouldReturnResource() {
        String token = "valid-token";
        byte[] content = "File content".getBytes();
        Resource resource = new ByteArrayResource(content);
        String complexFilename = "my document (2024).pdf";
        DocumentDownload download = new DocumentDownload(
                resource,
                "application/pdf",
                complexFilename,
                (long) content.length
        );

        when(documentShareService.downloadShared(token, null, httpServletRequest)).thenReturn(download);

        ResponseEntity<Resource> response = documentShareEndpoint.downloadShared(token, null, httpServletRequest);

        assertNotNull(response);
        assertEquals(complexFilename, response.getHeaders().getContentDisposition().getFilename());

        verify(documentShareService).downloadShared(token, null, httpServletRequest);
    }

    @Test
    void downloadShared_withVariousContentTypes_shouldSetCorrectMediaType() {
        String token = "valid-token";
        String[] contentTypes = {
                "image/jpeg",
                "application/json",
                "video/mp4",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        };

        for (String contentType : contentTypes) {
            Resource resource = new ByteArrayResource("content".getBytes());
            DocumentDownload download = new DocumentDownload(resource, contentType, "file", 7L);
            when(documentShareService.downloadShared(token, null, httpServletRequest)).thenReturn(download);

            ResponseEntity<Resource> response = documentShareEndpoint.downloadShared(token, null, httpServletRequest);

            assertEquals(MediaType.parseMediaType(contentType), response.getHeaders().getContentType());
        }

        verify(documentShareService, times(contentTypes.length)).downloadShared(token, null, httpServletRequest);
    }
}
