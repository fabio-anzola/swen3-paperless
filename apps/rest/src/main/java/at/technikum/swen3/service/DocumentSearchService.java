package at.technikum.swen3.service;

import at.technikum.swen3.entity.Document;
import at.technikum.swen3.search.elastic.PDFDocument;
import at.technikum.swen3.service.dtos.document.DocumentDto;
import at.technikum.swen3.service.dtos.document.DocumentSearchResultDto;
import at.technikum.swen3.service.mapper.DocumentMapper;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import at.technikum.swen3.repository.DocumentRepository;

@Service
@Transactional(readOnly = true)
public class DocumentSearchService implements IDocumentSearchService {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ElasticsearchOperations elasticsearchOperations;
    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;

    public DocumentSearchService(ElasticsearchOperations elasticsearchOperations,
                                 DocumentRepository documentRepository,
                                 DocumentMapper documentMapper) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.documentRepository = documentRepository;
        this.documentMapper = documentMapper;
    }

    @Override
    public Page<DocumentSearchResultDto> search(Long userId, String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return Page.empty(pageable);
        }

        List<Document> userDocs = documentRepository.findAllByOwnerIdAndElasticIdIsNotNull(userId);
        Map<String, Document> docsByElasticId = userDocs.stream()
                .filter(d -> d.getElasticId() != null && !d.getElasticId().isBlank())
                .collect(Collectors.toMap(Document::getElasticId, Function.identity(), (a, b) -> a));

        if (docsByElasticId.isEmpty()) {
            LOG.debug("User {} has no indexed documents to search", userId);
            return Page.empty(pageable);
        }

        NativeQuery searchQuery = buildQuery(query, pageable, docsByElasticId.keySet());
        SearchHits<PDFDocument> hits = elasticsearchOperations.search(searchQuery, PDFDocument.class);

        List<DocumentSearchResultDto> results = hits.getSearchHits().stream()
                .map(hit -> mapHitToDto(hit, docsByElasticId))
                .filter(Objects::nonNull)
                .toList();

        return new PageImpl<>(results, pageable, hits.getTotalHits());
    }

    private NativeQuery buildQuery(String query, Pageable pageable, Set<String> allowedIds) {
        return new NativeQueryBuilder()
                .withQuery(q -> q.bool(b -> b
                        .must(m -> m.multiMatch(mm -> mm
                                .query(query)
                                .fields("fileName^2", "summary^1.5", "textContent")
                                .type(TextQueryType.BestFields)
                        ))
                        .filter(f -> f.ids(ids -> ids.values(new ArrayList<>(allowedIds))))
                ))
                .withPageable(pageable)
                .build();
    }

    private DocumentSearchResultDto mapHitToDto(SearchHit<PDFDocument> hit, Map<String, Document> docsByElasticId) {
        Document matched = docsByElasticId.get(hit.getId());
        if (matched == null) {
            return null;
        }
        DocumentDto dto = documentMapper.toDto(matched);
        PDFDocument esDoc = hit.getContent();
        String summary = esDoc != null ? esDoc.summary() : null;
        Float scoreValue = hit.getScore(); // may be NaN / no score
        Double score = (scoreValue == null || Float.isNaN(scoreValue)) ? null : scoreValue.doubleValue();
        return new DocumentSearchResultDto(dto.id(), dto.name(), summary, dto.ownerId(), dto.s3Key(), score);
    }
}
