package at.technikum.swen3.repository;

import at.technikum.swen3.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    Page<Document> findAllByOwnerId(Long ownerId, Pageable pageable);

    java.util.Optional<Document> findByS3Key(String s3Key);
}
