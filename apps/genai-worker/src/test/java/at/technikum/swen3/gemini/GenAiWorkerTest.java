package at.technikum.swen3.gemini;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import at.technikum.swen3.gemini.config.KafkaFactory;
import at.technikum.swen3.gemini.config.WorkerProperties;
import at.technikum.swen3.gemini.dto.GenAiResultMessage;
import at.technikum.swen3.gemini.dto.OcrResultMessage;
import at.technikum.swen3.gemini.dto.SummaryResponse;
import at.technikum.swen3.gemini.service.GeminiService;

@ExtendWith(MockitoExtension.class)
class GenAiWorkerTest {

    @Mock
    private WorkerProperties workerProperties;

    @Mock
    private GeminiService geminiService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private KafkaConsumer<String, String> kafkaConsumer;

    @Mock
    private KafkaProducer<String, String> kafkaProducer;

    private GenAiWorker genAiWorker;

    @BeforeEach
    void setUp() {
        genAiWorker = new GenAiWorker(workerProperties, geminiService, objectMapper);
    }

    @Test
    void run_shouldProcessRecordsSuccessfully() throws Exception {
        when(workerProperties.getInputTopic()).thenReturn("input-topic");
        when(workerProperties.getOutputTopic()).thenReturn("output-topic");
        when(workerProperties.getBootstrapServers()).thenReturn("localhost:9092");
        when(workerProperties.getPollMs()).thenReturn(1000L);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("input-topic", 0, 0, "key",
            "{\"processedMessage\":\"Test content\"}");
        ConsumerRecords<String, String> records = new ConsumerRecords<>(
            Map.of(new TopicPartition("input-topic", 0), Collections.singletonList(record)));

        OcrResultMessage ocrMessage = new OcrResultMessage("Test content");
        SummaryResponse summaryResponse = new SummaryResponse("Test summary");
        GenAiResultMessage genAiMessage = new GenAiResultMessage("Test content", "Test summary");

        try (MockedStatic<KafkaFactory> kafkaFactoryMock = mockStatic(KafkaFactory.class)) {
            kafkaFactoryMock.when(() -> KafkaFactory.createConsumer(workerProperties)).thenReturn(kafkaConsumer);
            kafkaFactoryMock.when(() -> KafkaFactory.createProducer("localhost:9092")).thenReturn(kafkaProducer);

            when(kafkaConsumer.poll(Duration.ofMillis(1000L)))
                .thenReturn(records)
                .thenThrow(new WakeupException());

            when(objectMapper.readValue("{\"processedMessage\":\"Test content\"}", OcrResultMessage.class))
                .thenReturn(ocrMessage);
            when(geminiService.summarize("Test content")).thenReturn(summaryResponse);
            when(objectMapper.writeValueAsString(genAiMessage)).thenReturn("{\"processedMessage\":\"Test content\",\"summary\":\"Test summary\"}");

            genAiWorker.run();

            verify(kafkaConsumer).subscribe(Collections.singletonList("input-topic"));
            verify(kafkaConsumer).commitSync();
            verify(kafkaProducer).send(any(ProducerRecord.class), any(Callback.class));
            verify(kafkaConsumer).close();
            verify(kafkaProducer).close();
        }
    }

    @Test
    void run_shouldHandleEmptyRecords() {
        when(workerProperties.getInputTopic()).thenReturn("input-topic");
        when(workerProperties.getOutputTopic()).thenReturn("output-topic");
        when(workerProperties.getBootstrapServers()).thenReturn("localhost:9092");
        when(workerProperties.getPollMs()).thenReturn(1000L);

        ConsumerRecords<String, String> emptyRecords = new ConsumerRecords<>(Collections.emptyMap());

        try (MockedStatic<KafkaFactory> kafkaFactoryMock = mockStatic(KafkaFactory.class)) {
            kafkaFactoryMock.when(() -> KafkaFactory.createConsumer(workerProperties)).thenReturn(kafkaConsumer);
            kafkaFactoryMock.when(() -> KafkaFactory.createProducer("localhost:9092")).thenReturn(kafkaProducer);

            when(kafkaConsumer.poll(Duration.ofMillis(1000L)))
                .thenReturn(emptyRecords)
                .thenThrow(new WakeupException());

            genAiWorker.run();

            verify(kafkaConsumer).subscribe(Collections.singletonList("input-topic"));
            verify(kafkaConsumer, never()).commitSync();
            verify(kafkaProducer, never()).send(any(ProducerRecord.class), any(Callback.class));
        }
    }

    @Test
    void run_shouldHandleRecordProcessingFailure() throws Exception {
        when(workerProperties.getInputTopic()).thenReturn("input-topic");
        when(workerProperties.getOutputTopic()).thenReturn("output-topic");
        when(workerProperties.getBootstrapServers()).thenReturn("localhost:9092");
        when(workerProperties.getPollMs()).thenReturn(1000L);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("input-topic", 0, 0, "key", "invalid-json");
        ConsumerRecords<String, String> records = new ConsumerRecords<>(
            Map.of(new TopicPartition("input-topic", 0), Collections.singletonList(record)));

        try (MockedStatic<KafkaFactory> kafkaFactoryMock = mockStatic(KafkaFactory.class)) {
            kafkaFactoryMock.when(() -> KafkaFactory.createConsumer(workerProperties)).thenReturn(kafkaConsumer);
            kafkaFactoryMock.when(() -> KafkaFactory.createProducer("localhost:9092")).thenReturn(kafkaProducer);

            when(kafkaConsumer.poll(Duration.ofMillis(1000L)))
                .thenReturn(records)
                .thenThrow(new WakeupException());

            when(objectMapper.readValue("invalid-json", OcrResultMessage.class))
                .thenThrow(new JsonProcessingException("Invalid JSON") {});

            genAiWorker.run();

            verify(kafkaConsumer).subscribe(Collections.singletonList("input-topic"));
            verify(kafkaConsumer, never()).commitSync();
            verify(kafkaProducer, never()).send(any(ProducerRecord.class), any(Callback.class));
        }
    }

    @Test
    void run_shouldHandleUnexpectedException() {
        when(workerProperties.getInputTopic()).thenReturn("input-topic");
        when(workerProperties.getBootstrapServers()).thenReturn("localhost:9092");

        try (MockedStatic<KafkaFactory> kafkaFactoryMock = mockStatic(KafkaFactory.class)) {
            kafkaFactoryMock.when(() -> KafkaFactory.createConsumer(workerProperties)).thenReturn(kafkaConsumer);
            kafkaFactoryMock.when(() -> KafkaFactory.createProducer("localhost:9092")).thenReturn(kafkaProducer);

            doThrow(new RuntimeException("Unexpected error")).when(kafkaConsumer).subscribe(Collections.singletonList("input-topic"));

            assertThatThrownBy(() -> genAiWorker.run())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unexpected error");
        }
    }

    @Test
    void run_shouldProcessMultipleRecords() throws Exception {
        when(workerProperties.getInputTopic()).thenReturn("input-topic");
        when(workerProperties.getOutputTopic()).thenReturn("output-topic");
        when(workerProperties.getBootstrapServers()).thenReturn("localhost:9092");
        when(workerProperties.getPollMs()).thenReturn(1000L);

        ConsumerRecord<String, String> record1 = new ConsumerRecord<>("input-topic", 0, 0, "key1",
            "{\"processedMessage\":\"Content 1\"}");
        ConsumerRecord<String, String> record2 = new ConsumerRecord<>("input-topic", 0, 1, "key2",
            "{\"processedMessage\":\"Content 2\"}");

        ConsumerRecords<String, String> records = new ConsumerRecords<>(
            Map.of(new TopicPartition("input-topic", 0),
                   java.util.Arrays.asList(record1, record2)));

        try (MockedStatic<KafkaFactory> kafkaFactoryMock = mockStatic(KafkaFactory.class)) {
            kafkaFactoryMock.when(() -> KafkaFactory.createConsumer(workerProperties)).thenReturn(kafkaConsumer);
            kafkaFactoryMock.when(() -> KafkaFactory.createProducer("localhost:9092")).thenReturn(kafkaProducer);

            when(kafkaConsumer.poll(Duration.ofMillis(1000L)))
                .thenReturn(records)
                .thenThrow(new WakeupException());

            when(objectMapper.readValue(eq("{\"processedMessage\":\"Content 1\"}"), eq(OcrResultMessage.class)))
                .thenReturn(new OcrResultMessage("Content 1"));
            when(objectMapper.readValue(eq("{\"processedMessage\":\"Content 2\"}"), eq(OcrResultMessage.class)))
                .thenReturn(new OcrResultMessage("Content 2"));

            when(geminiService.summarize("Content 1")).thenReturn(new SummaryResponse("Summary 1"));
            when(geminiService.summarize("Content 2")).thenReturn(new SummaryResponse("Summary 2"));

            when(objectMapper.writeValueAsString(any(GenAiResultMessage.class)))
                .thenReturn("{\"result\":\"test\"}");

            genAiWorker.run();

            verify(kafkaConsumer).commitSync();
            verify(kafkaProducer, times(2)).send(any(ProducerRecord.class), any(Callback.class));
        }
    }

    @Test
    void run_shouldHandlePartialFailure() throws Exception {
        when(workerProperties.getInputTopic()).thenReturn("input-topic");
        when(workerProperties.getOutputTopic()).thenReturn("output-topic");
        when(workerProperties.getBootstrapServers()).thenReturn("localhost:9092");
        when(workerProperties.getPollMs()).thenReturn(1000L);

        ConsumerRecord<String, String> validRecord = new ConsumerRecord<>("input-topic", 0, 0, "key1",
            "{\"processedMessage\":\"Valid content\"}");
        ConsumerRecord<String, String> invalidRecord = new ConsumerRecord<>("input-topic", 0, 1, "key2",
            "invalid-json");

        ConsumerRecords<String, String> records = new ConsumerRecords<>(
            Map.of(new TopicPartition("input-topic", 0),
                   java.util.Arrays.asList(validRecord, invalidRecord)));

        try (MockedStatic<KafkaFactory> kafkaFactoryMock = mockStatic(KafkaFactory.class)) {
            kafkaFactoryMock.when(() -> KafkaFactory.createConsumer(workerProperties)).thenReturn(kafkaConsumer);
            kafkaFactoryMock.when(() -> KafkaFactory.createProducer("localhost:9092")).thenReturn(kafkaProducer);

            when(kafkaConsumer.poll(Duration.ofMillis(1000L)))
                .thenReturn(records)
                .thenThrow(new WakeupException());

            when(objectMapper.readValue(eq("{\"processedMessage\":\"Valid content\"}"), eq(OcrResultMessage.class)))
                .thenReturn(new OcrResultMessage("Valid content"));
            when(objectMapper.readValue(eq("invalid-json"), eq(OcrResultMessage.class)))
                .thenThrow(new JsonProcessingException("Invalid JSON") {});

            when(geminiService.summarize("Valid content")).thenReturn(new SummaryResponse("Summary"));
            when(objectMapper.writeValueAsString(any(GenAiResultMessage.class)))
                .thenReturn("{\"result\":\"test\"}");

            genAiWorker.run();

            verify(kafkaConsumer, never()).commitSync();
            verify(kafkaProducer, times(1)).send(any(ProducerRecord.class), any(Callback.class));
        }
    }

    @Test
    void processRecord_shouldProcessValidRecord_andSendToOutputTopic() throws Exception {
        String recordKey = "test-key";
        String inputJson = "{\"processedMessage\":\"Test OCR content\"}";
        String expectedSummary = "Test summary";
        String outputJson = "{\"processedMessage\":\"Test OCR content\",\"summary\":\"Test summary\"}";

        OcrResultMessage ocrMessage = new OcrResultMessage("Test OCR content");
        SummaryResponse summaryResponse = new SummaryResponse(expectedSummary);
        GenAiResultMessage genAiMessage = new GenAiResultMessage("Test OCR content", expectedSummary);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("input-topic", 0, 0, recordKey, inputJson);

        when(workerProperties.getOutputTopic()).thenReturn("test-output-topic");
        when(objectMapper.readValue(inputJson, OcrResultMessage.class)).thenReturn(ocrMessage);
        when(geminiService.summarize("Test OCR content")).thenReturn(summaryResponse);
        when(objectMapper.writeValueAsString(genAiMessage)).thenReturn(outputJson);

        invokeProcessRecord(record, kafkaProducer);

        verify(geminiService).summarize("Test OCR content");

        ArgumentCaptor<ProducerRecord<String, String>> producerRecordCaptor =
            ArgumentCaptor.forClass(ProducerRecord.class);
        ArgumentCaptor<Callback> callbackCaptor = ArgumentCaptor.forClass(Callback.class);

        verify(kafkaProducer).send(producerRecordCaptor.capture(), callbackCaptor.capture());

        ProducerRecord<String, String> sentRecord = producerRecordCaptor.getValue();
        assertThat(sentRecord.topic()).isEqualTo("test-output-topic");
        assertThat(sentRecord.key()).isEqualTo(recordKey);
        assertThat(sentRecord.value()).isEqualTo(outputJson);
    }

    @Test
    void processRecord_shouldHandleSuccessfulCallback() throws Exception {
        String inputJson = "{\"processedMessage\":\"Test content\"}";
        OcrResultMessage ocrMessage = new OcrResultMessage("Test content");
        SummaryResponse summaryResponse = new SummaryResponse("Summary");
        GenAiResultMessage genAiMessage = new GenAiResultMessage("Test content", "Summary");

        ConsumerRecord<String, String> record = new ConsumerRecord<>("input-topic", 0, 0, "key", inputJson);
        RecordMetadata metadata = mock(RecordMetadata.class);
        when(metadata.topic()).thenReturn("output-topic");
        when(metadata.partition()).thenReturn(1);
        when(metadata.offset()).thenReturn(100L);

        when(workerProperties.getOutputTopic()).thenReturn("test-output-topic");
        when(objectMapper.readValue(inputJson, OcrResultMessage.class)).thenReturn(ocrMessage);
        when(geminiService.summarize("Test content")).thenReturn(summaryResponse);
        when(objectMapper.writeValueAsString(genAiMessage)).thenReturn("{}");

        invokeProcessRecord(record, kafkaProducer);

        ArgumentCaptor<Callback> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(kafkaProducer).send(any(), callbackCaptor.capture());

        Callback callback = callbackCaptor.getValue();
        callback.onCompletion(metadata, null);
    }

    @Test
    void processRecord_shouldHandleFailedCallback() throws Exception {
        String inputJson = "{\"processedMessage\":\"Test content\"}";
        OcrResultMessage ocrMessage = new OcrResultMessage("Test content");
        SummaryResponse summaryResponse = new SummaryResponse("Summary");
        GenAiResultMessage genAiMessage = new GenAiResultMessage("Test content", "Summary");

        ConsumerRecord<String, String> record = new ConsumerRecord<>("input-topic", 0, 0, "key", inputJson);
        Exception sendException = new RuntimeException("Send failed");

        when(workerProperties.getOutputTopic()).thenReturn("test-output-topic");
        when(objectMapper.readValue(inputJson, OcrResultMessage.class)).thenReturn(ocrMessage);
        when(geminiService.summarize("Test content")).thenReturn(summaryResponse);
        when(objectMapper.writeValueAsString(genAiMessage)).thenReturn("{}");

        invokeProcessRecord(record, kafkaProducer);

        ArgumentCaptor<Callback> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(kafkaProducer).send(any(), callbackCaptor.capture());

        Callback callback = callbackCaptor.getValue();
        callback.onCompletion(null, sendException);
    }

    @Test
    void parseInput_shouldParseValidJson() throws Exception {
        String validJson = "{\"processedMessage\":\"Valid content\"}";
        OcrResultMessage expectedMessage = new OcrResultMessage("Valid content");

        when(objectMapper.readValue(validJson, OcrResultMessage.class)).thenReturn(expectedMessage);

        OcrResultMessage result = invokeParseInput(validJson);

        assertThat(result).isEqualTo(expectedMessage);
        verify(objectMapper).readValue(validJson, OcrResultMessage.class);
    }

    @Test
    void parseInput_shouldThrowException_whenJsonIsInvalid() throws Exception {
        String invalidJson = "invalid json";
        JsonProcessingException jsonException = mock(JsonProcessingException.class);

        when(objectMapper.readValue(invalidJson, OcrResultMessage.class)).thenThrow(jsonException);

        assertThatThrownBy(() -> invokeParseInput(invalidJson))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid input payload for GenAI worker: " + invalidJson)
            .hasCause(jsonException);
    }

    @Test
    void parseInput_shouldThrowException_whenMessageIsNull() throws Exception {
        String jsonWithNullMessage = "{\"processedMessage\":null}";

        when(objectMapper.readValue(jsonWithNullMessage, OcrResultMessage.class))
            .thenReturn(new OcrResultMessage(null));

        assertThatThrownBy(() -> invokeParseInput(jsonWithNullMessage))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Payload missing processedMessage");
    }

    @Test
    void parseInput_shouldThrowException_whenMessageIsBlank() throws Exception {
        String jsonWithBlankMessage = "{\"processedMessage\":\"   \"}";

        when(objectMapper.readValue(jsonWithBlankMessage, OcrResultMessage.class))
            .thenReturn(new OcrResultMessage("   "));

        assertThatThrownBy(() -> invokeParseInput(jsonWithBlankMessage))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Payload missing processedMessage");
    }

    @Test
    void parseInput_shouldThrowException_whenEntireMessageIsNull() throws Exception {
        String jsonReturningNull = "{\"processedMessage\":\"content\"}";

        when(objectMapper.readValue(jsonReturningNull, OcrResultMessage.class)).thenReturn(null);

        assertThatThrownBy(() -> invokeParseInput(jsonReturningNull))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Payload missing processedMessage");
    }

    @Test
    void serialize_shouldSerializeValidObject() throws Exception {
        GenAiResultMessage message = new GenAiResultMessage("content", "summary");
        String expectedJson = "{\"processedMessage\":\"content\",\"summary\":\"summary\"}";

        when(objectMapper.writeValueAsString(message)).thenReturn(expectedJson);

        String result = invokeSerialize(message);

        assertThat(result).isEqualTo(expectedJson);
        verify(objectMapper).writeValueAsString(message);
    }

    @Test
    void serialize_shouldThrowException_whenSerializationFails() throws Exception {
        GenAiResultMessage message = new GenAiResultMessage("content", "summary");
        JsonProcessingException jsonException = mock(JsonProcessingException.class);

        when(objectMapper.writeValueAsString(message)).thenThrow(jsonException);

        assertThatThrownBy(() -> invokeSerialize(message))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failed to serialize GenAI result")
            .hasCause(jsonException);
    }

    private void invokeProcessRecord(ConsumerRecord<String, String> record, KafkaProducer<String, String> producer)
            throws Exception {
        Method method = GenAiWorker.class.getDeclaredMethod("processRecord", ConsumerRecord.class, KafkaProducer.class);
        method.setAccessible(true);
        try {
            method.invoke(genAiWorker, record, producer);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    private OcrResultMessage invokeParseInput(String payload) throws Exception {
        Method method = GenAiWorker.class.getDeclaredMethod("parseInput", String.class);
        method.setAccessible(true);
        try {
            return (OcrResultMessage) method.invoke(genAiWorker, payload);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    private String invokeSerialize(GenAiResultMessage output) throws Exception {
        Method method = GenAiWorker.class.getDeclaredMethod("serialize", GenAiResultMessage.class);
        method.setAccessible(true);
        try {
            return (String) method.invoke(genAiWorker, output);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }
}

