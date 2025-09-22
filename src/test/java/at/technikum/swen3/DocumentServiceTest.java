package at.technikum.swen3;
import at.technikum.swen3.entity.Document;
import at.technikum.swen3.entity.User;
import at.technikum.swen3.repository.DocumentRepository;
import at.technikum.swen3.repository.UserRepository;
import at.technikum.swen3.service.DocumentService;
import at.technikum.swen3.service.dtos.document.DocumentDto;
import at.technikum.swen3.service.dtos.document.DocumentUploadDto;
import at.technikum.swen3.service.mapper.DocumentMapper;
import at.technikum.swen3.service.model.DocumentDownload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentServiceTest {

    private DocumentRepository documentRepository;
    private UserRepository userRepository;
    private DocumentMapper documentMapper;
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        userRepository = mock(UserRepository.class);
        documentMapper = mock(DocumentMapper.class);
        documentService = new DocumentService(documentRepository, userRepository, documentMapper);
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
    void download_returnsDocumentDownload_whenOwner() {
        Long userId = 1L;
        Long docId = 2L;
        Document doc = new Document();
        User user = new User();
        user.setId(userId);
        doc.setOwner(user);
        doc.setName("test.txt");
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        DocumentDownload download = documentService.download(userId, docId);

        assertNotNull(download);
        assertEquals("test.txt", download.filename());
        assertEquals("text/plain", download.contentType());
        assertNotNull(download.body());
    }

    @Test
    void upload_savesDocument_andReturnsDto() {
        Long userId = 1L;
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("file.txt");
        User user = new User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        DocumentUploadDto meta = mock(DocumentUploadDto.class);
        when(meta.name()).thenReturn(null);
        Document doc = new Document();
        doc.setOwner(user);
        doc.setName("file.txt");
        when(documentRepository.save(any(Document.class))).thenReturn(doc);
        DocumentDto dto = mock(DocumentDto.class);
        when(documentMapper.toDto(any(Document.class))).thenReturn(dto);

        DocumentDto result = documentService.upload(userId, file, meta);

        assertEquals(dto, result);
        verify(documentRepository).save(any(Document.class));
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
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        documentService.delete(userId, docId);

        verify(documentRepository).delete(doc);
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
}