package at.technikum.swen3.search.elastic;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PDFDocumentRepository extends ElasticsearchRepository<PDFDocument, String> {
}
