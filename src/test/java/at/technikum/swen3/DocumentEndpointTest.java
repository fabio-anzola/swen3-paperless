package at.technikum.swen3;

import at.technikum.swen3.endpoint.DocumentEndpoint;
import at.technikum.swen3.entity.User;
import at.technikum.swen3.service.IDocumentService;
import at.technikum.swen3.service.IUserService;
import at.technikum.swen3.service.dtos.document.DocumentDto;
import at.technikum.swen3.service.dtos.document.DocumentUploadDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DocumentEndpointTest {

    private DocumentEndpoint documentEndpoint;

    @Mock
    private IDocumentService documentService;

    @Mock
    private IUserService userService;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        documentEndpoint = new DocumentEndpoint(documentService, userService, null);
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
}
