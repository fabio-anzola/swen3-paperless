package at.technikum.swen3.gemini.elastic.repository;

import at.technikum.swen3.gemini.elastic.entity.PDFDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PDFDocumentRepository extends ElasticsearchRepository<PDFDocument, String> {
}
