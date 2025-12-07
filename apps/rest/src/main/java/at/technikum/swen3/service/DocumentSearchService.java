package at.technikum.swen3.service;

import at.technikum.swen3.service.model.DocumentSearchDocument;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import jakarta.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

@Service
public class DocumentSearchService {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ElasticsearchOperations operations;

    public DocumentSearchService(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    @PostConstruct
    public void ensureIndexOnStartup() {
        ensureIndexExists();
    }

    public void index(DocumentSearchDocument document) {
        try {
            ensureIndexExists();
            operations.save(document);
        } catch (Exception e) {
            LOG.warn("Failed to index document {} in Elasticsearch: {}", document.getId(), e.getMessage());
        }
    }

    public void delete(Long id) {
        try {
            operations.delete(String.valueOf(id), DocumentSearchDocument.class);
        } catch (Exception e) {
            LOG.warn("Failed to delete document {} from Elasticsearch: {}", id, e.getMessage());
        }
    }

    public SearchResult search(Long ownerId, String query, Pageable pageable) {
        ensureIndexExists();
        String trimmedQuery = query == null ? "" : query.trim();

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {
                    b.filter(f -> f.term(t -> t.field("ownerId").value(ownerId)));
                    if (!trimmedQuery.isEmpty()) {
                        b.must(m -> m.multiMatch(mm -> mm
                                .fields("name", "summary", "content")
                                .query(trimmedQuery)
                                .operator(Operator.And)));
                    }
                    return b;
                }))
                .withPageable(pageable)
                .build();

        SearchHits<DocumentSearchDocument> hits = operations.search(searchQuery, DocumentSearchDocument.class);

        List<Long> ids = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(DocumentSearchDocument::getId)
                .collect(Collectors.toList());

        long totalHits = hits.getTotalHits();
        return new SearchResult(ids, totalHits);
    }

    public record SearchResult(List<Long> ids, long totalHits) {
    }

    private void ensureIndexExists() {
        try {
            IndexOperations indexOps = operations.indexOps(DocumentSearchDocument.class);
            if (!indexOps.exists()) {
                indexOps.create();
                indexOps.putMapping(indexOps.createMapping());
                LOG.info("Created Elasticsearch index {}", indexOps.getIndexCoordinates().getIndexName());
            }
        } catch (Exception e) {
            LOG.warn("Could not ensure Elasticsearch index exists: {}", e.getMessage());
        }
    }
}
