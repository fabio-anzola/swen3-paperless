package at.technikum.swen3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import at.technikum.swen3.worker.service.OcrProcessingException;
import at.technikum.swen3.worker.service.OcrService;
import org.junit.jupiter.api.Test;

class OcrServiceTest {

    @Test
    void extractText_whenEmptyInput_throwsOcrProcessingException() {
        OcrService service = new OcrService(null, null);
        InputStream empty = new ByteArrayInputStream(new byte[0]);

        OcrProcessingException ex = assertThrows(OcrProcessingException.class,
            () -> service.extractText(empty, "empty-key"));

        assertTrue(ex.getMessage().contains("Object is empty for key: empty-key"));
    }

    @Test
    void extractText_whenStreamReadFails_throwsOcrProcessingExceptionWithCause() {
        OcrService service = new OcrService(null, null);

        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("simulated read failure");
            }
        };

        OcrProcessingException ex = assertThrows(OcrProcessingException.class,
            () -> service.extractText(failing, "fail-key"));

        assertTrue(ex.getMessage().contains("Failed to read object for key: fail-key"));
        assertInstanceOf(IOException.class, ex.getCause());
        assertEquals("simulated read failure", ex.getCause().getMessage());
    }

    @Test
    void extractText_whenUnsupportedType_throwsOcrProcessingException() {
        OcrService service = new OcrService(null, null);
        byte[] randomBytes = "this is not an image or pdf".getBytes();
        InputStream in = new ByteArrayInputStream(randomBytes);

        OcrProcessingException ex = assertThrows(OcrProcessingException.class,
            () -> service.extractText(in, "file.txt"));

        assertTrue(ex.getMessage().contains("Unsupported or unreadable object type for key: file.txt"));
    }
}