package at.technikum.swen3.kafka;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @KafkaListener(topics = "result", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeMessage(String message) {
        LOG.info("Message received from topic 'result': {}", message);
        // Add your business logic here to process the message
    }
}
