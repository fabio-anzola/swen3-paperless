package at.technikum.swen3.worker;

import java.time.Duration;
import java.util.Collections;

import at.technikum.swen3.worker.config.KafkaConfig;
import at.technikum.swen3.worker.config.WorkerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkerApplication {
  private static final Logger log = LoggerFactory.getLogger(WorkerApplication.class);

  @SuppressWarnings("InfiniteLoopStatement")
  public static void main(String[] args) {
    log.info("Starting Kafka Worker Application");

    WorkerConfig config = new WorkerConfig();

    try (KafkaConsumer<String, String> consumer = KafkaConfig.createConsumer(
        config.bootstrapServers, config.groupId, config.autoOffsetReset, config.enableAutoCommit);
         KafkaProducer<String, String> producer = KafkaConfig.createProducer(config.bootstrapServers)) {

      consumer.subscribe(Collections.singletonList(config.inputTopic));
      log.info("Subscribed to topic: {}", config.inputTopic);

      Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

      while (true) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(config.pollMs));

        for (ConsumerRecord<String, String> record : records) {
          try {
            MessageProcessor.process(record, producer, config.outputTopic);
            consumer.commitSync();
          } catch (Exception e) {
            log.error("Failed to process record, not committing", e);
          }
        }
      }

    } catch (WakeupException e) {
      log.info("Consumer wakeup - shutting down gracefully");
    } catch (Exception e) {
      log.error("Error in worker application", e);
    }
  }
}