package at.technikum.swen3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import at.technikum.swen3.endpoint.DocumentEndpoint;
import at.technikum.swen3.entity.User;
import at.technikum.swen3.service.IDocumentService;
import at.technikum.swen3.service.IUserService;
import at.technikum.swen3.service.dtos.document.DocumentDto;
import at.technikum.swen3.service.dtos.document.DocumentUploadDto;
import at.technikum.swen3.service.model.DocumentDownload;

class DocumentEndpointTest {

    private DocumentEndpoint documentEndpoint;

    @Mock
    private IDocumentService documentService;

    @Mock
    private IUserService userService;

    @Mock
    private Authentication authentication;

    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        documentEndpoint = new DocumentEndpoint(documentService, userService, objectMapper);
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
    void listMine_shouldReturnDocuments() {
        String username = "testUser";
        Long userId = mockUserAndAuthentication(username);
        Pageable pageable = Pageable.unpaged();
        List<DocumentDto> documents = List.of(new DocumentDto(1L, "Doc1", "Description1", userId));
        Page<DocumentDto> documentPage = new PageImpl<>(documents);

        when(documentService.listMine(userId, pageable)).thenReturn(documentPage);

        Page<DocumentDto> result = documentEndpoint.listMine(authentication, pageable);

        assertEquals(documentPage, result);
        verify(userService).findByUsername(username);
        verify(documentService).listMine(userId, pageable);
    }

    @Test
    void getMeta_shouldReturnDocumentMeta() {
        String username = "testUser";
        Long userId = mockUserAndAuthentication(username);
        Long documentId = 2L;
        DocumentDto documentDto = new DocumentDto(2L, "Doc2", "Description2", userId);

        when(documentService.getMeta(userId, documentId)).thenReturn(documentDto);

        DocumentDto result = documentEndpoint.getMeta(authentication, documentId);

        assertEquals(documentDto, result);
        verify(userService).findByUsername(username);
        verify(documentService).getMeta(userId, documentId);
    }

@Test
void updateMeta_shouldReturnUpdatedDocument() {
    String username = "testUser";
    Long userId = mockUserAndAuthentication(username);
    Long documentId = 2L;
    DocumentUploadDto meta = new DocumentUploadDto("{\"key\":\"value\"}");
    DocumentDto documentDto = new DocumentDto(4L, "Doc4", "Updated Description", userId);

    when(documentService.updateMeta(userId, documentId, meta)).thenReturn(documentDto);

    DocumentDto result = documentEndpoint.updateMeta(authentication, documentId, meta);

    assertEquals(documentDto, result);
    verify(userService).findByUsername(username);
    verify(documentService).updateMeta(userId, documentId, meta);
}

@Test
void delete_shouldCallServiceDelete() {
    String username = "testUser";
    Long userId = mockUserAndAuthentication(username);
    Long documentId = 2L;

    documentEndpoint.delete(authentication, documentId);

    verify(userService).findByUsername(username);
    verify(documentService).delete(userId, documentId);
}

    @Test
    void listMine_shouldThrowUnauthorized_whenUserNotFound() {
        String username = "testUser";
        when(authentication.getName()).thenReturn(username);
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(null);
        when(userService.findByUsername(username)).thenReturn(mockUser);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> documentEndpoint.listMine(authentication, Pageable.unpaged()));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void getMeta_shouldThrowUnauthorized_whenUserNotFound() {
        String username = "testUser";
        when(authentication.getName()).thenReturn(username);
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(null);
        when(userService.findByUsername(username)).thenReturn(mockUser);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> documentEndpoint.getMeta(authentication, 1L));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void download_shouldReturnDocument() {
        String username = "testUser";
        Long userId = mockUserAndAuthentication(username);
        Long documentId = 1L;
        Resource resource = new ByteArrayResource("test content".getBytes());
        DocumentDownload download = new DocumentDownload(resource, "text/plain", "test.txt", 12L);

        when(documentService.download(userId, documentId)).thenReturn(download);

        ResponseEntity<Resource> response = documentEndpoint.download(authentication, documentId);

        assertNotNull(response);
        assertEquals(resource, response.getBody());
        verify(documentService).download(userId, documentId);
    }

    @Test
    void download_shouldThrowNotFound_whenDocumentNotFound() {
        String username = "testUser";
        Long userId = mockUserAndAuthentication(username);
        Long documentId = 999L;

        when(documentService.download(userId, documentId)).thenReturn(null);

        assertThrows(Exception.class, () -> documentEndpoint.download(authentication, documentId));
    }

    @Test
    void upload_shouldUploadDocument() throws IOException {
        String username = "testUser";
        Long userId = mockUserAndAuthentication(username);
        MultipartFile file = mock(MultipartFile.class);
        String metaJson = "{\"name\":\"custom.txt\"}";
        DocumentUploadDto meta = new DocumentUploadDto("custom.txt");
        DocumentDto expectedDto = new DocumentDto(1L, "custom.txt", "s3-key", userId);

        when(objectMapper.readValue(metaJson, DocumentUploadDto.class)).thenReturn(meta);
        when(documentService.upload(userId, file, meta)).thenReturn(expectedDto);

        DocumentDto result = documentEndpoint.upload(authentication, file, metaJson);

        assertEquals(expectedDto, result);
        verify(documentService).upload(userId, file, meta);
    }

    @Test
    void upload_shouldUploadDocument_withNullMeta() throws IOException {
        String username = "testUser";
        Long userId = mockUserAndAuthentication(username);
        MultipartFile file = mock(MultipartFile.class);
        DocumentDto expectedDto = new DocumentDto(1L, "file.txt", "s3-key", userId);

        when(documentService.upload(userId, file, null)).thenReturn(expectedDto);

        DocumentDto result = documentEndpoint.upload(authentication, file, null);

        assertEquals(expectedDto, result);
        verify(documentService).upload(userId, file, null);
    }

    @Test
    void upload_shouldThrowUnauthorized_whenUserNotFound() {
        String username = "testUser";
        when(authentication.getName()).thenReturn(username);
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(null);
        when(userService.findByUsername(username)).thenReturn(mockUser);
        MultipartFile file = mock(MultipartFile.class);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> documentEndpoint.upload(authentication, file, null));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void updateMeta_shouldThrowUnauthorized_whenUserNotFound() {
        String username = "testUser";
        when(authentication.getName()).thenReturn(username);
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(null);
        when(userService.findByUsername(username)).thenReturn(mockUser);
        DocumentUploadDto meta = new DocumentUploadDto("updated.txt");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> documentEndpoint.updateMeta(authentication, 1L, meta));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void delete_shouldThrowUnauthorized_whenUserNotFound() {
        String username = "testUser";
        when(authentication.getName()).thenReturn(username);
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(null);
        when(userService.findByUsername(username)).thenReturn(mockUser);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> documentEndpoint.delete(authentication, 1L));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
}
