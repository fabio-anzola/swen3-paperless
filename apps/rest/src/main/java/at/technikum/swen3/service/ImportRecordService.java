package at.technikum.swen3.service;

import at.technikum.swen3.entity.ImportedRecord;
import at.technikum.swen3.repository.ImportedRecordRepository;
import at.technikum.swen3.service.dtos.imports.ImportRecordDto;
import at.technikum.swen3.service.dtos.imports.ImportRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ImportRecordService implements IImportRecordService {

    private static final Logger LOG = LoggerFactory.getLogger(ImportRecordService.class);

    private final ImportedRecordRepository importedRecordRepository;

    public ImportRecordService(ImportedRecordRepository importedRecordRepository) {
        this.importedRecordRepository = importedRecordRepository;
    }

    @Override
    public ImportRecordDto create(ImportRequestDto requestDto) {
        if (requestDto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        ImportedRecord record = new ImportedRecord();
        record.setContent(requestDto.content());
        record.setDate(requestDto.date());
        record.setDescription(requestDto.description());

        ImportedRecord saved = importedRecordRepository.save(record);
        LOG.info("Stored imported record id={}, date={}", saved.getId(), saved.getDate());
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ImportRecordDto> list(Pageable pageable) {
        return importedRecordRepository.findAll(pageable).map(this::toDto);
    }

    private ImportRecordDto toDto(ImportedRecord record) {
        return new ImportRecordDto(record.getId(), record.getContent(), record.getDate(), record.getDescription(), record.getCreatedAt());
    }
}
