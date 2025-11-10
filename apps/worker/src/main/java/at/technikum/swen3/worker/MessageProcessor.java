package at.technikum.swen3.worker;

import java.io.IOException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import at.technikum.swen3.worker.dto.OcrTopicMessageDto;
import at.technikum.swen3.worker.dto.ResultTopicMessageDto;
import at.technikum.swen3.worker.service.FileDownloadException;
import at.technikum.swen3.worker.service.OcrProcessingException;
import at.technikum.swen3.worker.service.OcrService;
import at.technikum.swen3.worker.service.S3Service;

public class MessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);

    private final ObjectMapper objectMapper;
    private final S3Service s3Service;
    private final OcrService ocrService;

    public MessageProcessor(S3Service s3Service, OcrService ocrService) {
        this(s3Service, ocrService, new ObjectMapper());
    }

    public MessageProcessor(S3Service s3Service, OcrService ocrService, ObjectMapper objectMapper) {
        this.s3Service = s3Service;
        this.ocrService = ocrService;
        this.objectMapper = objectMapper;
    }

    public void process(ConsumerRecord<String, String> record,
                        KafkaProducer<String, String> producer,
                        String outputTopic) throws OcrProcessingException, FileDownloadException {
        logger.info("Received message: {}", record.value());

        OcrTopicMessageDto input = parseMessage(record.value());
        String s3Key = input.getS3Key();
        if (s3Key == null || s3Key.isBlank()) {
            throw new OcrProcessingException("Message missing s3Key");
        }
        logger.info("Downloading file from S3 with key: {}", s3Key);

        try (var fileStream = s3Service.downloadFile(s3Key)) {
            String extractedText = ocrService.extractText(fileStream, s3Key);
            ResultTopicMessageDto output = new ResultTopicMessageDto(extractedText);

            String outputJson = serializeResult(output);

            producer.send(new ProducerRecord<>(outputTopic, record.key(), outputJson),
                (metadata, exception) -> {
                    if (exception != null) {
                        logger.error("Error sending message to {}", outputTopic, exception);
                    } else {
                        logger.info("Sent OCR result to {} partition={}, offset={} ({} chars)",
                            metadata.topic(), metadata.partition(), metadata.offset(),
                            output.getProcessedMessage() != null ? output.getProcessedMessage().length() : 0);
                    }
                });
        } catch (IOException e) {
            throw new OcrProcessingException("Failed to close file stream for key: " + s3Key, e);
        }
    }

    private OcrTopicMessageDto parseMessage(String payload) throws OcrProcessingException {
        try {
            return objectMapper.readValue(payload, OcrTopicMessageDto.class);
        } catch (JsonProcessingException e) {
            throw new OcrProcessingException("Invalid input payload: " + payload, e);
        }
    }

    private String serializeResult(ResultTopicMessageDto output) throws OcrProcessingException {
        try {
            return objectMapper.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new OcrProcessingException("Failed to serialize OCR result", e);
        }
    }
}
