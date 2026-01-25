package at.technikum.swen3.batch.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import at.technikum.swen3.batch.config.BatchProperties;
import at.technikum.swen3.batch.service.model.ImportPayload;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class BatchImportServiceTest {

    private BatchProperties batchProperties;
    @Mock
    private XmlImportParser xmlImportParser;
    @Mock
    private ImportRestClient importRestClient;

    private BatchImportService batchImportService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        batchProperties = new BatchProperties();
        batchProperties.setRunTime("02:00");
        batchProperties.setTimezone("UTC");
        batchProperties.setImportUrl("http://localhost:8080/api/v1/paperless/import");
    }

    @Test
    void runImportBatch_processesXmlFilesOnly() throws Exception {
        Path tempDir = Files.createTempDirectory("batch-test");
        batchProperties.setFolderPath(tempDir.toString());
        batchImportService = new BatchImportService(batchProperties, xmlImportParser, importRestClient);

        Path xmlFile = Files.createFile(tempDir.resolve("one.xml"));
        Files.createFile(tempDir.resolve("ignore.txt"));
        Path xmlFileTwo = Files.createFile(tempDir.resolve("two.xml"));

        ImportPayload payload = new ImportPayload("a", LocalDate.parse("2026-01-21"), "d1");

        when(xmlImportParser.parse(xmlFile)).thenReturn(payload);
        when(xmlImportParser.parse(xmlFileTwo)).thenThrow(new IllegalArgumentException("bad xml"));

        batchImportService.runImportBatch();

        verify(xmlImportParser, times(1)).parse(xmlFile);
        verify(xmlImportParser, times(1)).parse(xmlFileTwo);
        verify(importRestClient, times(1)).send(payload);
    }

    @Test
    void runImportBatch_skipsWhenDirectoryMissing() {
        batchProperties.setFolderPath("Z:/missing-folder");
        batchImportService = new BatchImportService(batchProperties, xmlImportParser, importRestClient);

        batchImportService.runImportBatch();

        verifyNoInteractions(xmlImportParser);
        verify(importRestClient, never()).send(any());
    }
}
