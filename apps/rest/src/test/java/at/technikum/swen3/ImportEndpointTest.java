package at.technikum.swen3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import at.technikum.swen3.endpoint.ImportEndpoint;
import at.technikum.swen3.service.IImportRecordService;
import at.technikum.swen3.service.dtos.imports.ImportRecordDto;
import at.technikum.swen3.service.dtos.imports.ImportRequestDto;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ImportEndpointTest {

    @Mock
    private IImportRecordService importRecordService;

    private ImportEndpoint importEndpoint;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        importEndpoint = new ImportEndpoint(importRecordService);
    }

    @Test
    void importRecord_returnsCreatedAndPayload() {
        ImportRequestDto requestDto = new ImportRequestDto("content", LocalDate.parse("2026-01-21"), "desc");
        ImportRecordDto responseDto = new ImportRecordDto(1L, "content", LocalDate.parse("2026-01-21"), "desc", Instant.parse("2026-01-22T00:00:00Z"));

        when(importRecordService.create(any(ImportRequestDto.class))).thenReturn(responseDto);

        ResponseEntity<ImportRecordDto> response = importEndpoint.importRecord(requestDto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(importRecordService).create(requestDto);
    }
}
