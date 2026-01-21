package at.technikum.swen3.batch.service;

import at.technikum.swen3.batch.config.BatchProperties;
import at.technikum.swen3.batch.service.model.ImportPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class ImportRestClient {

    private static final Logger LOG = LoggerFactory.getLogger(ImportRestClient.class);

    private final RestTemplate restTemplate;
    private final BatchProperties batchProperties;

    public ImportRestClient(RestTemplate restTemplate, BatchProperties batchProperties) {
        this.restTemplate = restTemplate;
        this.batchProperties = batchProperties;
    }

    public void send(ImportPayload payload) {
        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(batchProperties.importUri(), payload, Void.class);
            LOG.info("Posted import for date={} status={}", payload.date(), response.getStatusCode());
        } catch (RestClientException ex) {
            LOG.error("Failed to post import payload to REST API at {}: {}", batchProperties.getImportUrl(), ex.getMessage());
            throw ex;
        }
    }
}
