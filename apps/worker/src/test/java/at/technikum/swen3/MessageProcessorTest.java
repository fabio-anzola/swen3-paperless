package at.technikum.swen3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import at.technikum.swen3.worker.MessageProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import at.technikum.swen3.worker.service.FileDownloadException;
import at.technikum.swen3.worker.service.OcrProcessingException;
import at.technikum.swen3.worker.service.OcrService;
import at.technikum.swen3.worker.service.S3Service;

class MessageProcessorTest {

    @Test
    void happyPath_sendsSerializedResult() throws Exception {
        S3Service s3 = mock(S3Service.class);
        OcrService ocr = mock(OcrService.class);
        KafkaProducer<String, String> producer = mock(KafkaProducer.class);

        String payload = "{\"s3Key\":\"some-key\"}";
        InputStream fileStream = new ByteArrayInputStream("file".getBytes());
        when(s3.downloadFile("some-key")).thenReturn(fileStream);
        when(ocr.extractText(any(InputStream.class), any())).thenReturn("extracted-text");

        ObjectMapper mapper = new ObjectMapper();
        MessageProcessor processor = new MessageProcessor(s3, ocr, mapper);

        doAnswer(invocation -> {
            Callback cb = invocation.getArgument(1);
            cb.onCompletion(mock(RecordMetadata.class), null);
            return null;
        }).when(producer).send(any(ProducerRecord.class), any(Callback.class));

        ConsumerRecord<String, String> record = new ConsumerRecord<>("in", 0, 0L, "key1", payload);
        processor.process(record, producer, "out-topic");

        ArgumentCaptor<ProducerRecord<String, String>> cap = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer).send(cap.capture(), any(Callback.class));
        ProducerRecord<String, String> sent = cap.getValue();
        assertEquals("out-topic", sent.topic());
        String sentValue = sent.value();
        org.junit.jupiter.api.Assertions.assertTrue(sentValue.contains("extracted-text"));
    }

    @Test
    void invalidJson_throwsOcrProcessingException() {
        S3Service s3 = mock(S3Service.class);
        OcrService ocr = mock(OcrService.class);
        ObjectMapper mapper = new ObjectMapper();
        MessageProcessor processor = new MessageProcessor(s3, ocr, mapper);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("in", 0, 0L, "k", "not-json");

        assertThrows(OcrProcessingException.class, () ->
            processor.process(record, mock(KafkaProducer.class), "out")
        );
    }

    @Test
    void missingS3Key_throwsOcrProcessingException() throws Exception {
        S3Service s3 = mock(S3Service.class);
        OcrService ocr = mock(OcrService.class);
        ObjectMapper mapper = new ObjectMapper();
        MessageProcessor processor = new MessageProcessor(s3, ocr, mapper);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("in", 0, 0L, "k", "{\"s3Key\":\"\"}");

        assertThrows(OcrProcessingException.class, () ->
            processor.process(record, mock(KafkaProducer.class), "out")
        );
    }

    @Test
    void s3Download_failure_bubblesFileDownloadException() throws Exception {
        S3Service s3 = mock(S3Service.class);
        OcrService ocr = mock(OcrService.class);
        when(s3.downloadFile("bad-key")).thenThrow(new FileDownloadException("fail"));
        MessageProcessor processor = new MessageProcessor(s3, ocr, new ObjectMapper());

        ConsumerRecord<String, String> record = new ConsumerRecord<>("in", 0, 0L, "k", "{\"s3Key\":\"bad-key\"}");

        assertThrows(FileDownloadException.class, () ->
            processor.process(record, mock(KafkaProducer.class), "out")
        );
    }

    @Test
    void ocrFailure_throwsOcrProcessingException() throws Exception {
        S3Service s3 = mock(S3Service.class);
        OcrService ocr = mock(OcrService.class);
        InputStream fileStream = new ByteArrayInputStream("file".getBytes());
        when(s3.downloadFile("some")).thenReturn(fileStream);
        when(ocr.extractText(any(InputStream.class), any())).thenThrow(new OcrProcessingException("ocr fail"));

        MessageProcessor processor = new MessageProcessor(s3, ocr, new ObjectMapper());
        ConsumerRecord<String, String> record = new ConsumerRecord<>("in", 0, 0L, "k", "{\"s3Key\":\"some\"}");

        assertThrows(OcrProcessingException.class, () ->
            processor.process(record, mock(KafkaProducer.class), "out")
        );
    }

    @Test
    void serializationFailure_throwsOcrProcessingException() throws Exception {
        S3Service s3 = mock(S3Service.class);
        OcrService ocr = mock(OcrService.class);
        InputStream fileStream = new ByteArrayInputStream("file".getBytes());
        when(s3.downloadFile("some")).thenReturn(fileStream);
        when(ocr.extractText(any(InputStream.class), any())).thenReturn("extracted");

        ObjectMapper real = new ObjectMapper();
        ObjectMapper spyMapper = spy(real);
        doThrow(new JsonProcessingException("boom"){}).when(spyMapper).writeValueAsString(any());

        MessageProcessor processor = new MessageProcessor(s3, ocr, spyMapper);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("in", 0, 0L, "k", "{\"s3Key\":\"some\"}");

        assertThrows(OcrProcessingException.class, () ->
            processor.process(record, mock(KafkaProducer.class), "out")
        );
    }

    @Test
    void closingStreamThrows_isWrappedAsOcrProcessingException() throws Exception {
        S3Service s3 = mock(S3Service.class);
        OcrService ocr = mock(OcrService.class);

        InputStream badClose = new FilterInputStream(new ByteArrayInputStream("file".getBytes())) {
            @Override
            public void close() throws IOException {
                throw new IOException("close failed");
            }
        };

        when(s3.downloadFile("k")).thenReturn(badClose);
        when(ocr.extractText(any(InputStream.class), any())).thenReturn("extracted");

        MessageProcessor processor = new MessageProcessor(s3, ocr, new ObjectMapper());
        ConsumerRecord<String, String> record = new ConsumerRecord<>("in", 0, 0L, "k1", "{\"s3Key\":\"k\"}");

        assertThrows(OcrProcessingException.class, () ->
            processor.process(record, mock(KafkaProducer.class), "out")
        );
    }
}