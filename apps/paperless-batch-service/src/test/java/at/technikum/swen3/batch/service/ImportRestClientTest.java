package at.technikum.swen3.batch.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import at.technikum.swen3.batch.config.BatchProperties;
import at.technikum.swen3.batch.service.model.ImportPayload;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

class ImportRestClientTest {

    @Mock
    private RestTemplate restTemplate;

    private BatchProperties batchProperties;
    private ImportRestClient importRestClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        batchProperties = new BatchProperties();
        batchProperties.setFolderPath("/tmp/imports");
        batchProperties.setRunTime("02:00");
        batchProperties.setImportUrl("http://localhost:8080/api/v1/paperless/import");
        batchProperties.setTimezone("UTC");
        batchProperties.validate();
        importRestClient = new ImportRestClient(restTemplate, batchProperties);
    }

    @Test
    void send_postsPayloadToConfiguredUrl() {
        ImportPayload payload = new ImportPayload("content", LocalDate.parse("2026-01-21"), "desc");
        when(restTemplate.postForEntity(eq(batchProperties.importUri()), any(), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        importRestClient.send(payload);

        verify(restTemplate).postForEntity(eq(batchProperties.importUri()), eq(payload), eq(Void.class));
    }
}
