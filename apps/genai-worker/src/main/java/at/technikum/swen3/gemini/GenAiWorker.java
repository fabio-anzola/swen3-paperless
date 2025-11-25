package at.technikum.swen3.gemini;

import at.technikum.swen3.gemini.config.KafkaFactory;
import at.technikum.swen3.gemini.config.WorkerProperties;
import at.technikum.swen3.gemini.dto.GenAiResultMessage;
import at.technikum.swen3.gemini.dto.OcrResultMessage;
import at.technikum.swen3.gemini.service.GeminiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Collections;
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

    public GenAiWorker(WorkerProperties properties, GeminiService geminiService, ObjectMapper objectMapper) {
        this.properties = properties;
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
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

        GenAiResultMessage output = new GenAiResultMessage(input.processedMessage(), summary);
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

    private OcrResultMessage parseInput(String payload) {
        try {
            OcrResultMessage message = objectMapper.readValue(payload, OcrResultMessage.class);
            if (message == null || message.processedMessage() == null || message.processedMessage().isBlank()) {
                throw new IllegalArgumentException("Payload missing processedMessage");
            }
            return message;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid input payload for GenAI worker: " + payload, e);
        }
    }

    private String serialize(GenAiResultMessage output) {
        try {
            return objectMapper.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize GenAI result", e);
        }
    }
}
