package at.technikum.swen3.worker;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import at.technikum.swen3.worker.dto.OcrTopicMessageDto;
import at.technikum.swen3.worker.dto.ResultTopicMessageDto;

public class MessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void process(ConsumerRecord<String, String> record,
                               KafkaProducer<String, String> producer,
                               String outputTopic) {
        try {
            logger.info("Received message: {}", record.value());

            OcrTopicMessageDto input =
                objectMapper.readValue(record.value(), OcrTopicMessageDto.class);
            logger.info("Parsed OcrTopicMessageDto: {}", input);

            ResultTopicMessageDto output =
                new ResultTopicMessageDto(input.getS3Key().toUpperCase());

            String outputJson = objectMapper.writeValueAsString(output);

            producer.send(new ProducerRecord<>(outputTopic, record.key(), outputJson),
                (metadata, exception) -> {
                    if (exception != null) {
                        logger.error("Error sending message to {}", outputTopic, exception);
                    } else {
                        logger.info("Sent ResultTopicMessageDto to {} partition={}, offset={}",
                            metadata.topic(), metadata.partition(), metadata.offset());
                    }
                });

        } catch (Exception e) {
            logger.error("Invalid input message format. Expected JSON like {\"message\":\"...\"}. Received: {}", record.value(), e);
        }
    }
}
