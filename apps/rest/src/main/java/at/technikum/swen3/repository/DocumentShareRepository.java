package at.technikum.swen3.repository;

import at.technikum.swen3.entity.DocumentShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentShareRepository extends JpaRepository<DocumentShare, Long> {
    Optional<DocumentShare> findByToken(String token);

    List<DocumentShare> findAllByDocumentId(Long documentId);
}
