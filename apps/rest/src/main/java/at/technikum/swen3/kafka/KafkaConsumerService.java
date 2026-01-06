package at.technikum.swen3.kafka;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import at.technikum.swen3.repository.DocumentRepository;
import at.technikum.swen3.service.dtos.GenAiResultMessageDto;

@Service
public class KafkaConsumerService {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ObjectMapper objectMapper;
    private final DocumentRepository documentRepository;

    public KafkaConsumerService(ObjectMapper objectMapper, DocumentRepository documentRepository) {
        this.objectMapper = objectMapper;
        this.documentRepository = documentRepository;
    }

    @KafkaListener(topics = "${kafka.topic.result}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeMessage(String message) {
        LOG.info("Message received from topic 'result': {}", message);
        try {
            GenAiResultMessageDto dto = objectMapper.readValue(message, GenAiResultMessageDto.class);
            if (dto == null || dto.elasticId() == null || dto.elasticId().isBlank()
                || dto.s3Key() == null || dto.s3Key().isBlank()) {
                LOG.warn("Skipping message missing s3Key or elasticId");
                return;
            }

            documentRepository.findByS3Key(dto.s3Key()).ifPresentOrElse(doc -> {
                doc.setElasticId(dto.elasticId());
                documentRepository.save(doc);
                LOG.info("Stored elasticsearch id for document with s3Key={}", dto.s3Key());
            }, () -> LOG.warn("No document found for s3Key={}, cannot store elasticsearch id", dto.s3Key()));
        } catch (Exception e) {
            LOG.error("Failed to process message from 'result': {}", message, e);
        }
    }
}
