package at.technikum.swen3.worker;

import java.time.Duration;
import java.util.Collections;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.technikum.swen3.worker.config.KafkaConfig;
import at.technikum.swen3.worker.config.WorkerConfig;
import at.technikum.swen3.worker.service.OcrProcessingException;
import at.technikum.swen3.worker.service.OcrService;
import at.technikum.swen3.worker.service.S3Service;

public class WorkerApplication {
  private static final Logger log = LoggerFactory.getLogger(WorkerApplication.class);

  @SuppressWarnings("InfiniteLoopStatement")
  public static void main(String[] args) {
    log.info("Starting Kafka Worker Application");

    WorkerConfig config = new WorkerConfig();
    S3Service s3Service = new S3Service(config.minioUrl, config.minioAccessKey,
        config.minioSecretKey, config.minioBucketName);
    OcrService ocrService = new OcrService(config.tessdataPath, config.tesseractLanguage);
    MessageProcessor messageProcessor = new MessageProcessor(s3Service, ocrService);
    log.info("Initialized OCR with tessdataPath={} language={} bucket={}",
        config.tessdataPath, config.tesseractLanguage, config.minioBucketName);

    try (KafkaConsumer<String, String> consumer = KafkaConfig.createConsumer(
        config.bootstrapServers, config.groupId, config.autoOffsetReset, config.enableAutoCommit);
         KafkaProducer<String, String> producer = KafkaConfig.createProducer(config.bootstrapServers)) {

      consumer.subscribe(Collections.singletonList(config.inputTopic));
      log.info("Subscribed to topic: {}", config.inputTopic);

      Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

      while (true) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(config.pollMs));
        boolean allOk = true;
        for (ConsumerRecord<String, String> record : records) {
          try {
            messageProcessor.process(record, producer, config.outputTopic);
          } catch (OcrProcessingException e) {
            allOk = false;
            log.error("Failed to process record due to OCR error, not committing", e);
          } catch (RuntimeException e) {
            allOk = false;
            log.error("Failed to process record, not committing", e);
          }
        }
        if (allOk) {
          consumer.commitSync();
        }
      }

    } catch (WakeupException e) {
      log.info("Consumer wakeup - shutting down gracefully");
    } catch (Exception e) {
      log.error("Error in worker application", e);
    }
  }
}
