package at.technikum.swen3.worker;

import java.io.InputStream;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import at.technikum.swen3.worker.dto.OcrTopicMessageDto;
import at.technikum.swen3.worker.dto.ResultTopicMessageDto;
import at.technikum.swen3.worker.service.S3Service;

public class MessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void process(ConsumerRecord<String, String> record,
                               KafkaProducer<String, String> producer,
                               String outputTopic,
                               S3Service s3Service) {
        try {
            logger.info("Received message: {}", record.value());

            OcrTopicMessageDto input =
                objectMapper.readValue(record.value(), OcrTopicMessageDto.class);
            logger.info("Parsed OcrTopicMessageDto: {}", input);

            String s3Key = input.getS3Key();
            logger.info("Downloading file from S3 with key: {}", s3Key);
            
            try (InputStream fileStream = s3Service.downloadFile(s3Key)) {
                byte[] fileContent = fileStream.readAllBytes();
                logger.info("Downloaded file from S3, size: {} bytes", fileContent.length);
                
                /* TO-DO: Process the file (simulate OCR processing)
                   for now simulate processing by converting the s3Key to uppercase */
                ResultTopicMessageDto output =
                    new ResultTopicMessageDto(s3Key.toUpperCase());

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
            }

        } catch (Exception e) {
            logger.error("Error processing message. Received: {}", record.value(), e);
            throw new RuntimeException("Failed to process message", e);
        }
    }
}
