package at.technikum.swen3.worker;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import at.technikum.swen3.worker.config.KafkaConfig;
import at.technikum.swen3.worker.config.WorkerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WorkerApplication {
    private static final Logger logger = LoggerFactory.getLogger(WorkerApplication.class);
    private static final AtomicBoolean running = new AtomicBoolean(true);

    public static void main(String[] args) {
        logger.info("Starting Kafka Worker Application");

        WorkerConfig config = new WorkerConfig();
        logger.info("Configuration: bootstrapServers={}, groupId={}, inputTopic={}, outputTopic={}",
                config.bootstrapServers, config.groupId, config.inputTopic, config.outputTopic);

        KafkaConsumer<String, String> consumer = KafkaConfig.createConsumer(
                config.bootstrapServers, config.groupId, config.autoOffsetReset, config.enableAutoCommit);

      consumer.subscribe(Collections.singletonList(config.inputTopic));
        logger.info("Subscribed to topic: {}", config.inputTopic);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down worker...");
            running.set(false);
        }));

      try (consumer; KafkaProducer<String, String> producer = KafkaConfig.createProducer(config.bootstrapServers)) {
        while (running.get()) {
          ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(config.pollMs));

          for (ConsumerRecord<String, String> record : records) {
            MessageProcessor.process(record, producer, config.outputTopic);
          }

          if (!records.isEmpty()) {
            consumer.commitSync();
            logger.info("Committed offsets for {} records", records.count());
          }
        }
      } catch (Exception e) {
        logger.error("Error in worker application", e);
      } finally {
        logger.info("Closing consumer and producer...");
      }
    }
}
