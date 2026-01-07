package at.technikum.swen3;

import at.technikum.swen3.entity.Document;
import at.technikum.swen3.entity.User;
import at.technikum.swen3.repository.DocumentRepository;
import at.technikum.swen3.search.elastic.PDFDocument;
import at.technikum.swen3.service.DocumentSearchService;
import at.technikum.swen3.service.dtos.document.DocumentDto;
import at.technikum.swen3.service.dtos.document.DocumentSearchResultDto;
import at.technikum.swen3.service.mapper.DocumentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DocumentSearchServiceTest {

    private DocumentSearchService documentSearchService;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentMapper documentMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        documentSearchService = new DocumentSearchService(
                elasticsearchOperations,
                documentRepository,
                documentMapper
        );
    }

    private Document createDocument(Long id, String elasticId, String name, String s3Key, Long ownerId) {
        Document doc = new Document();
        doc.setId(id);
        doc.setElasticId(elasticId);
        doc.setName(name);
        doc.setS3Key(s3Key);
        User owner = new User();
        owner.setId(ownerId);
        doc.setOwner(owner);
        return doc;
    }

    @SuppressWarnings("unchecked")
    private SearchHit<PDFDocument> createSearchHit(String id, String fileName, String summary, float score) {
        PDFDocument pdfDoc = new PDFDocument(id, fileName, "content", summary, Instant.now());
        SearchHit<PDFDocument> hit = mock(SearchHit.class);
        when(hit.getId()).thenReturn(id);
        when(hit.getScore()).thenReturn(score);
        when(hit.getContent()).thenReturn(pdfDoc);
        return hit;
    }

    @SuppressWarnings("unchecked")
    private SearchHits<PDFDocument> createSearchHits(long totalHits, List<SearchHit<PDFDocument>> hits) {
        SearchHits<PDFDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getTotalHits()).thenReturn(totalHits);
        when(searchHits.getSearchHits()).thenReturn(hits);
        return searchHits;
    }

    @Test
    void search_withNullQuery_returnsEmptyPage() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Page<DocumentSearchResultDto> result = documentSearchService.search(userId, null, pageable);

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(documentRepository, never()).findAllByOwnerIdAndElasticIdIsNotNull(any());
        verify(elasticsearchOperations, never()).search(any(NativeQuery.class), eq(PDFDocument.class));
    }

    @Test
    void search_withBlankQuery_returnsEmptyPage() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Page<DocumentSearchResultDto> result = documentSearchService.search(userId, "   ", pageable);

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(documentRepository, never()).findAllByOwnerIdAndElasticIdIsNotNull(any());
        verify(elasticsearchOperations, never()).search(any(NativeQuery.class), eq(PDFDocument.class));
    }

    @Test
    void search_withEmptyQuery_returnsEmptyPage() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Page<DocumentSearchResultDto> result = documentSearchService.search(userId, "", pageable);

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(documentRepository, never()).findAllByOwnerIdAndElasticIdIsNotNull(any());
        verify(elasticsearchOperations, never()).search(any(NativeQuery.class), eq(PDFDocument.class));
    }

    @Test
    void search_withNoUserDocuments_returnsEmptyPage() {
        Long userId = 1L;
        String query = "test";
        Pageable pageable = PageRequest.of(0, 10);

        when(documentRepository.findAllByOwnerIdAndElasticIdIsNotNull(userId))
                .thenReturn(Collections.emptyList());

        Page<DocumentSearchResultDto> result = documentSearchService.search(userId, query, pageable);

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(documentRepository).findAllByOwnerIdAndElasticIdIsNotNull(userId);
        verify(elasticsearchOperations, never()).search(any(NativeQuery.class), eq(PDFDocument.class));
    }

    @Test
    void search_withDocumentsHavingNullElasticId_returnsEmptyPage() {
        Long userId = 1L;
        String query = "test";
        Pageable pageable = PageRequest.of(0, 10);

        Document doc1 = createDocument(1L, null, "doc1.pdf", "s3-key-1", userId);
        Document doc2 = createDocument(2L, "", "doc2.pdf", "s3-key-2", userId);

        when(documentRepository.findAllByOwnerIdAndElasticIdIsNotNull(userId))
                .thenReturn(List.of(doc1, doc2));

        Page<DocumentSearchResultDto> result = documentSearchService.search(userId, query, pageable);

        assertTrue(result.isEmpty());
        verify(documentRepository).findAllByOwnerIdAndElasticIdIsNotNull(userId);
        verify(elasticsearchOperations, never()).search(any(NativeQuery.class), eq(PDFDocument.class));
    }

    @Test
    void search_withValidQuery_returnsResults() {
        Long userId = 1L;
        String query = "invoice";
        Pageable pageable = PageRequest.of(0, 10);

        Document doc1 = createDocument(1L, "elastic-1", "invoice.pdf", "s3-key-1", userId);
        Document doc2 = createDocument(2L, "elastic-2", "report.pdf", "s3-key-2", userId);

        when(documentRepository.findAllByOwnerIdAndElasticIdIsNotNull(userId))
                .thenReturn(List.of(doc1, doc2));

        SearchHit<PDFDocument> hit1 = createSearchHit("elastic-1", "invoice.pdf", "Invoice summary", 0.95f);
        SearchHit<PDFDocument> hit2 = createSearchHit("elastic-2", "report.pdf", "Report summary", 0.75f);

        SearchHits<PDFDocument> searchHits = createSearchHits(2L, List.of(hit1, hit2));

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(PDFDocument.class)))
                .thenReturn(searchHits);

        DocumentDto dto1 = new DocumentDto(1L, "invoice.pdf", "Description 1", userId);
        DocumentDto dto2 = new DocumentDto(2L, "report.pdf", "Description 2", userId);

        when(documentMapper.toDto(doc1)).thenReturn(dto1);
        when(documentMapper.toDto(doc2)).thenReturn(dto2);

        Page<DocumentSearchResultDto> result = documentSearchService.search(userId, query, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        DocumentSearchResultDto result1 = result.getContent().get(0);
        assertEquals(1L, result1.id());
        assertEquals("invoice.pdf", result1.name());
        assertEquals("Invoice summary", result1.summary());
        assertEquals(userId, result1.ownerId());
        assertEquals(0.95, result1.score(), 0.001);

        DocumentSearchResultDto result2 = result.getContent().get(1);
        assertEquals(2L, result2.id());
        assertEquals("report.pdf", result2.name());
        assertEquals("Report summary", result2.summary());
        assertEquals(0.75, result2.score(), 0.001);

        verify(documentRepository).findAllByOwnerIdAndElasticIdIsNotNull(userId);
        verify(elasticsearchOperations).search(any(NativeQuery.class), eq(PDFDocument.class));
        verify(documentMapper).toDto(doc1);
        verify(documentMapper).toDto(doc2);
    }

    @Test
    void search_withNoElasticsearchResults_returnsEmptyPage() {
        Long userId = 1L;
        String query = "nonexistent";
        Pageable pageable = PageRequest.of(0, 10);

        Document doc = createDocument(1L, "elastic-1", "doc.pdf", "s3-key-1", userId);

        when(documentRepository.findAllByOwnerIdAndElasticIdIsNotNull(userId))
                .thenReturn(List.of(doc));

        SearchHits<PDFDocument> searchHits = createSearchHits(0L, Collections.emptyList());

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(PDFDocument.class)))
                .thenReturn(searchHits);

        Page<DocumentSearchResultDto> result = documentSearchService.search(userId, query, pageable);

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(documentRepository).findAllByOwnerIdAndElasticIdIsNotNull(userId);
        verify(elasticsearchOperations).search(any(NativeQuery.class), eq(PDFDocument.class));
    }

    @Test
    void search_withNaNScore_setsScoreToNull() {
        Long userId = 1L;
        String query = "test";
        Pageable pageable = PageRequest.of(0, 10);

        Document doc = createDocument(1L, "elastic-1", "doc.pdf", "s3-key-1", userId);

        when(documentRepository.findAllByOwnerIdAndElasticIdIsNotNull(userId))
                .thenReturn(List.of(doc));

        SearchHit<PDFDocument> hit = createSearchHit("elastic-1", "doc.pdf", "summary", Float.NaN);

        SearchHits<PDFDocument> searchHits = createSearchHits(1L, List.of(hit));

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(PDFDocument.class)))
                .thenReturn(searchHits);

        DocumentDto dto = new DocumentDto(1L, "doc.pdf", "Description", userId);
        when(documentMapper.toDto(doc)).thenReturn(dto);

        Page<DocumentSearchResultDto> result = documentSearchService.search(userId, query, pageable);

        assertEquals(1, result.getTotalElements());
        assertNull(result.getContent().get(0).score());
    }

    @Test
    void search_withNullSummary_includesSummaryAsNull() {
        Long userId = 1L;
        String query = "test";
        Pageable pageable = PageRequest.of(0, 10);

        Document doc = createDocument(1L, "elastic-1", "doc.pdf", "s3-key-1", userId);

        when(documentRepository.findAllByOwnerIdAndElasticIdIsNotNull(userId))
                .thenReturn(List.of(doc));

        SearchHit<PDFDocument> hit = createSearchHit("elastic-1", "doc.pdf", null, 0.8f);

        SearchHits<PDFDocument> searchHits = createSearchHits(1L, List.of(hit));

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(PDFDocument.class)))
                .thenReturn(searchHits);

        DocumentDto dto = new DocumentDto(1L, "doc.pdf", "Description", userId);
        when(documentMapper.toDto(doc)).thenReturn(dto);

        Page<DocumentSearchResultDto> result = documentSearchService.search(userId, query, pageable);

        assertEquals(1, result.getTotalElements());
        assertNull(result.getContent().get(0).summary());
    }

    @Test
    void search_withNullPDFDocument_includesSummaryAsNull() {
        Long userId = 1L;
        String query = "test";
        Pageable pageable = PageRequest.of(0, 10);

        Document doc = createDocument(1L, "elastic-1", "doc.pdf", "s3-key-1", userId);

        when(documentRepository.findAllByOwnerIdAndElasticIdIsNotNull(userId))
                .thenReturn(List.of(doc));

        SearchHit<PDFDocument> hit = mock(SearchHit.class);
        when(hit.getId()).thenReturn("elastic-1");
        when(hit.getScore()).thenReturn(0.8f);
        when(hit.getContent()).thenReturn(null);

        SearchHits<PDFDocument> searchHits = createSearchHits(1L, List.of(hit));

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(PDFDocument.class)))
                .thenReturn(searchHits);

        DocumentDto dto = new DocumentDto(1L, "doc.pdf", "Description", userId);
        when(documentMapper.toDto(doc)).thenReturn(dto);

        Page<DocumentSearchResultDto> result = documentSearchService.search(userId, query, pageable);

        assertEquals(1, result.getTotalElements());
        assertNull(result.getContent().get(0).summary());
    }

    @Test
    void search_filtersOutUnmatchedElasticIds() {
        Long userId = 1L;
        String query = "test";
        Pageable pageable = PageRequest.of(0, 10);

        Document doc1 = createDocument(1L, "elastic-1", "doc1.pdf", "s3-key-1", userId);

        when(documentRepository.findAllByOwnerIdAndElasticIdIsNotNull(userId))
                .thenReturn(List.of(doc1));

        SearchHit<PDFDocument> hit = createSearchHit("elastic-999", "other.pdf", "summary", 0.9f);

        SearchHits<PDFDocument> searchHits = createSearchHits(1L, List.of(hit));

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(PDFDocument.class)))
                .thenReturn(searchHits);

        Page<DocumentSearchResultDto> result = documentSearchService.search(userId, query, pageable);

        assertTrue(result.isEmpty());
        assertEquals(0, result.getContent().size());
        verify(documentMapper, never()).toDto(any());
    }

    @Test
    void search_handlesDuplicateElasticIds() {
        Long userId = 1L;
        String query = "test";
        Pageable pageable = PageRequest.of(0, 10);

        Document doc1 = createDocument(1L, "elastic-1", "doc1.pdf", "s3-key-1", userId);
        Document doc2 = createDocument(2L, "elastic-1", "doc2.pdf", "s3-key-2", userId);

        when(documentRepository.findAllByOwnerIdAndElasticIdIsNotNull(userId))
                .thenReturn(List.of(doc1, doc2));

        SearchHit<PDFDocument> hit = createSearchHit("elastic-1", "doc.pdf", "summary", 0.9f);

        SearchHits<PDFDocument> searchHits = createSearchHits(1L, List.of(hit));

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(PDFDocument.class)))
                .thenReturn(searchHits);

        DocumentDto dto = new DocumentDto(1L, "doc1.pdf", "Description", userId);
        when(documentMapper.toDto(doc1)).thenReturn(dto);

        Page<DocumentSearchResultDto> result = documentSearchService.search(userId, query, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).id());
        assertEquals("doc1.pdf", result.getContent().get(0).name());
        verify(documentMapper).toDto(doc1);
        verify(documentMapper, never()).toDto(doc2);
    }

    @Test
    void search_usesCorrectPageable() {
        Long userId = 1L;
        String query = "test";
        Pageable pageable = PageRequest.of(2, 5);

        Document doc = createDocument(1L, "elastic-1", "doc.pdf", "s3-key-1", userId);

        when(documentRepository.findAllByOwnerIdAndElasticIdIsNotNull(userId))
                .thenReturn(List.of(doc));

        SearchHits<PDFDocument> searchHits = createSearchHits(0L, Collections.emptyList());

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(PDFDocument.class)))
                .thenReturn(searchHits);

        Page<DocumentSearchResultDto> result = documentSearchService.search(userId, query, pageable);

        assertEquals(2, result.getNumber());
        assertEquals(5, result.getSize());

        ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(elasticsearchOperations).search(queryCaptor.capture(), eq(PDFDocument.class));

        NativeQuery capturedQuery = queryCaptor.getValue();
        assertEquals(pageable, capturedQuery.getPageable());
    }

    @Test
    void search_withMultipleResults_maintainsOrder() {
        Long userId = 1L;
        String query = "document";
        Pageable pageable = PageRequest.of(0, 10);

        Document doc1 = createDocument(1L, "elastic-1", "doc1.pdf", "s3-key-1", userId);
        Document doc2 = createDocument(2L, "elastic-2", "doc2.pdf", "s3-key-2", userId);
        Document doc3 = createDocument(3L, "elastic-3", "doc3.pdf", "s3-key-3", userId);

        when(documentRepository.findAllByOwnerIdAndElasticIdIsNotNull(userId))
                .thenReturn(List.of(doc1, doc2, doc3));

        SearchHit<PDFDocument> hit1 = createSearchHit("elastic-1", "doc1.pdf", "summary1", 0.9f);
        SearchHit<PDFDocument> hit2 = createSearchHit("elastic-2", "doc2.pdf", "summary2", 0.8f);
        SearchHit<PDFDocument> hit3 = createSearchHit("elastic-3", "doc3.pdf", "summary3", 0.7f);

        SearchHits<PDFDocument> searchHits = createSearchHits(3L, List.of(hit1, hit2, hit3));

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(PDFDocument.class)))
                .thenReturn(searchHits);

        when(documentMapper.toDto(doc1)).thenReturn(new DocumentDto(1L, "doc1.pdf", "d1", userId));
        when(documentMapper.toDto(doc2)).thenReturn(new DocumentDto(2L, "doc2.pdf", "d2", userId));
        when(documentMapper.toDto(doc3)).thenReturn(new DocumentDto(3L, "doc3.pdf", "d3", userId));

        Page<DocumentSearchResultDto> result = documentSearchService.search(userId, query, pageable);

        assertEquals(3, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).id());
        assertEquals(2L, result.getContent().get(1).id());
        assertEquals(3L, result.getContent().get(2).id());
    }
}
