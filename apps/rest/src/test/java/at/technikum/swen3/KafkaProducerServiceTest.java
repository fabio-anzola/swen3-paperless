package at.technikum.swen3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import at.technikum.swen3.kafka.KafkaProducerService;

class KafkaProducerServiceTest {

    private KafkaTemplate<String, String> kafkaTemplate;
    private KafkaProducerService kafkaProducerService;

    @BeforeEach
    void setUp() {
        kafkaTemplate = (KafkaTemplate<String, String>) mock(KafkaTemplate.class);
        kafkaProducerService = new KafkaProducerService(kafkaTemplate);
    }

    @Test
    void sendMessage_shouldSendMessageToKafka() {
        String topic = "test-topic";
        String message = "test message";

        kafkaProducerService.sendMessage(topic, message);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), messageCaptor.capture());
        
        assertEquals(topic, topicCaptor.getValue());
        assertEquals(message, messageCaptor.getValue());
    }

    @Test
    void sendMessage_shouldSendMultipleMessages() {
        String topic1 = "topic1";
        String message1 = "message1";
        String topic2 = "topic2";
        String message2 = "message2";

        kafkaProducerService.sendMessage(topic1, message1);
        kafkaProducerService.sendMessage(topic2, message2);

        verify(kafkaTemplate, times(2)).send(anyString(), anyString());
    }

    @Test
    void sendMessage_shouldHandleJsonMessage() {
        String topic = "json-topic";
        String jsonMessage = "{\"key\":\"value\",\"number\":123}";

        kafkaProducerService.sendMessage(topic, jsonMessage);

        verify(kafkaTemplate).send(topic, jsonMessage);
    }

    @Test
    void sendMessage_shouldHandleEmptyMessage() {
        String topic = "empty-topic";
        String emptyMessage = "";

        kafkaProducerService.sendMessage(topic, emptyMessage);

        verify(kafkaTemplate).send(topic, emptyMessage);
    }
}
