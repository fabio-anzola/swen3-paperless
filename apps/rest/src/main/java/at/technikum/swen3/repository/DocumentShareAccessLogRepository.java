package at.technikum.swen3.repository;

import at.technikum.swen3.entity.DocumentShareAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentShareAccessLogRepository extends JpaRepository<DocumentShareAccessLog, Long> {
    List<DocumentShareAccessLog> findAllByShareDocumentIdOrderByAccessedAtDesc(Long documentId);
}
