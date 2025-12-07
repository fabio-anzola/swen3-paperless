package at.technikum.swen3.gemini;

import at.technikum.swen3.gemini.config.KafkaFactory;
import at.technikum.swen3.gemini.config.WorkerProperties;
import at.technikum.swen3.gemini.dto.GenAiResultMessage;
import at.technikum.swen3.gemini.dto.OcrResultMessage;
import at.technikum.swen3.gemini.elastic.entity.PDFDocument;
import at.technikum.swen3.gemini.elastic.repository.PDFDocumentRepository;
import at.technikum.swen3.gemini.service.GeminiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class GenAiWorker implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GenAiWorker.class);

    private final WorkerProperties properties;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;
    private final PDFDocumentRepository pdfDocumentRepository;

    public GenAiWorker(WorkerProperties properties, GeminiService geminiService,
                       ObjectMapper objectMapper, PDFDocumentRepository pdfDocumentRepository) {
        this.properties = properties;
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
        this.pdfDocumentRepository = pdfDocumentRepository;
    }

    @Override
    @SuppressWarnings("InfiniteLoopStatement")
    public void run(String... args) {
        log.info("Starting GenAI worker; consuming from '{}' and producing to '{}'",
            properties.getInputTopic(), properties.getOutputTopic());

        try (KafkaConsumer<String, String> consumer = KafkaFactory.createConsumer(properties);
             KafkaProducer<String, String> producer = KafkaFactory.createProducer(properties.getBootstrapServers())) {

            consumer.subscribe(Collections.singletonList(properties.getInputTopic()));
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(properties.getPollMs()));
                if (records.isEmpty()) {
                    continue;
                }

                boolean allOk = true;
                for (ConsumerRecord<String, String> record : records) {
                    try {
                        processRecord(record, producer);
                    } catch (Exception e) {
                        allOk = false;
                        log.error("Failed to process record at offset {}: {}", record.offset(), e.getMessage(), e);
                    }
                }

                if (allOk) {
                    consumer.commitSync();
                }
            }
        } catch (WakeupException e) {
            log.info("Shutdown requested, stopping GenAI worker loop");
        } catch (Exception e) {
            log.error("Unexpected error in GenAI worker", e);
            throw e;
        }
    }

    private void processRecord(ConsumerRecord<String, String> record, KafkaProducer<String, String> producer) {
        OcrResultMessage input = parseInput(record.value());
        String summary = geminiService.summarize(input.processedMessage()).summary();

        String fileName = resolveFileName(input.fileName(), input.s3Key());
        String elasticId = indexDocument(fileName, input.processedMessage(), summary);

        GenAiResultMessage output = new GenAiResultMessage(input.processedMessage(), summary, input.s3Key(), elasticId, fileName);
        String outputJson = serialize(output);

        producer.send(new ProducerRecord<>(properties.getOutputTopic(), record.key(), outputJson),
            (metadata, exception) -> {
                if (exception != null) {
                    log.error("Error sending GenAI result to {}: {}", properties.getOutputTopic(), exception.getMessage(), exception);
                } else {
                    log.info("Published GenAI result to {} partition {} offset {}", metadata.topic(), metadata.partition(), metadata.offset());
                }
            });
    }

    private String indexDocument(String fileName, String textContent, String summary) {
        String documentId = UUID.randomUUID().toString();
        PDFDocument document = new PDFDocument(documentId, fileName, textContent, summary, Instant.now());
        pdfDocumentRepository.save(document);
        log.info("Indexed document {} into Elasticsearch ({} chars)", documentId, textContent.length());
        return documentId;
    }

    private OcrResultMessage parseInput(String payload) {
        try {
            OcrResultMessage message = objectMapper.readValue(payload, OcrResultMessage.class);
            if (message == null || message.processedMessage() == null || message.processedMessage().isBlank()) {
                throw new IllegalArgumentException("Payload missing processedMessage");
            }
            if (message.s3Key() == null || message.s3Key().isBlank()) {
                throw new IllegalArgumentException("Payload missing s3Key");
            }
            return message;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid input payload for GenAI worker: " + payload, e);
        }
    }

    private String resolveFileName(String providedName, String s3Key) {
        if (providedName != null && !providedName.isBlank()) {
            return providedName;
        }
        if (s3Key == null || s3Key.isBlank()) {
            return "unknown";
        }
        int lastSlash = s3Key.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < s3Key.length() - 1) {
            return s3Key.substring(lastSlash + 1);
        }
        return s3Key;
    }

    private String serialize(GenAiResultMessage output) {
        try {
            return objectMapper.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize GenAI result", e);
        }
    }
}
