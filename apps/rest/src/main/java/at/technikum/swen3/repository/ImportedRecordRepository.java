package at.technikum.swen3.repository;

import at.technikum.swen3.entity.ImportedRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportedRecordRepository extends JpaRepository<ImportedRecord, Long> {
}
