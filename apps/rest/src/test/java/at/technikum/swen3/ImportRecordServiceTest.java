package at.technikum.swen3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import at.technikum.swen3.entity.ImportedRecord;
import at.technikum.swen3.repository.ImportedRecordRepository;
import at.technikum.swen3.service.ImportRecordService;
import at.technikum.swen3.service.dtos.imports.ImportRecordDto;
import at.technikum.swen3.service.dtos.imports.ImportRequestDto;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ImportRecordServiceTest {

    private ImportedRecordRepository importedRecordRepository;
    private ImportRecordService importRecordService;

    @BeforeEach
    void setUp() {
        importedRecordRepository = org.mockito.Mockito.mock(ImportedRecordRepository.class);
        importRecordService = new ImportRecordService(importedRecordRepository);
    }

    @Test
    void create_persistsRecordAndReturnsDto() {
        ImportRequestDto requestDto = new ImportRequestDto("content", LocalDate.parse("2026-01-21"), "desc");

        ImportedRecord saved = new ImportedRecord();
        saved.setId(5L);
        saved.setContent("content");
        saved.setDate(LocalDate.parse("2026-01-21"));
        saved.setDescription("desc");
        saved.setCreatedAt(Instant.parse("2026-01-22T00:00:00Z"));

        when(importedRecordRepository.save(any(ImportedRecord.class))).thenReturn(saved);

        ImportRecordDto result = importRecordService.create(requestDto);

        assertNotNull(result);
        assertEquals(saved.getId(), result.id());
        assertEquals(saved.getContent(), result.content());
        assertEquals(saved.getDate(), result.date());
        assertEquals(saved.getDescription(), result.description());
        assertEquals(saved.getCreatedAt(), result.createdAt());
        verify(importedRecordRepository).save(any(ImportedRecord.class));
    }

    @Test
    void create_throwsBadRequest_whenPayloadNull() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> importRecordService.create(null));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
