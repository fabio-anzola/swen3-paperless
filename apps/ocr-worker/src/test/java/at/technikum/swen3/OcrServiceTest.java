package at.technikum.swen3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import at.technikum.swen3.worker.service.OcrProcessingException;
import at.technikum.swen3.worker.service.OcrService;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

class OcrServiceTest {

    @BeforeAll
    static void beforeAll() {
        System.setProperty("java.awt.headless", "true");
    }

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

    @Test
    void extractText_image_success_trimsAndSetsTessdataAndLanguage() throws Exception {
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.fillRect(0, 0, 20, 20);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] imgBytes = baos.toByteArray();

        try (MockedConstruction<Tesseract> mocked = Mockito.mockConstruction(Tesseract.class,
                (mock, context) -> {
                    try {
                        Mockito.when(mock.doOCR(Mockito.any(BufferedImage.class))).thenReturn("  extracted  ");
                    } catch (TesseractException e) {
                    }
                })) {

            OcrService service = new OcrService("my/tess/path", "eng");
            String result = service.extractText(new ByteArrayInputStream(imgBytes), "image.png");

            assertEquals("extracted", result);

            Tesseract constructed = mocked.constructed().get(0);
            Mockito.verify(constructed).setDatapath("my/tess/path");
            Mockito.verify(constructed).setLanguage("eng");
            Mockito.verify(constructed).doOCR(Mockito.any(BufferedImage.class));
        }
    }

    @Test
    void extractText_image_tesseractThrows_isWrapped() throws Exception {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] imgBytes = baos.toByteArray();

        try (MockedConstruction<Tesseract> mocked = Mockito.mockConstruction(Tesseract.class,
                (mock, context) -> {
                    Mockito.when(mock.doOCR(Mockito.any(BufferedImage.class)))
                            .thenThrow(new TesseractException("tess fail"));
                })) {

            OcrService service = new OcrService(null, null);
            OcrProcessingException ex = assertThrows(OcrProcessingException.class,
                    () -> service.extractText(new ByteArrayInputStream(imgBytes), "img.png"));

            assertTrue(ex.getMessage().contains("Tesseract failed for key: img.png"));
            assertInstanceOf(TesseractException.class, ex.getCause());
            assertEquals("tess fail", ex.getCause().getMessage());
        }
    }

    @Test
    void extractText_pdf_multiplePages_concatenatesPages() throws Exception {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());
        doc.addPage(new PDPage());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        doc.close();
        byte[] pdfBytes = baos.toByteArray();

        AtomicInteger constructedIndex = new AtomicInteger(0);

        try (MockedConstruction<Tesseract> mocked = Mockito.mockConstruction(Tesseract.class,
                (mock, context) -> {
                    int idx = constructedIndex.getAndIncrement();
                    try {
                        if (idx == 0) {
                            Mockito.when(mock.doOCR(Mockito.any(java.awt.image.BufferedImage.class)))
                                    .thenReturn("page1-text");
                        } else if (idx == 1) {
                            Mockito.when(mock.doOCR(Mockito.any(java.awt.image.BufferedImage.class)))
                                    .thenReturn("page2-text");
                        } else {
                            Mockito.when(mock.doOCR(Mockito.any(java.awt.image.BufferedImage.class)))
                                    .thenReturn("");
                        }
                    } catch (TesseractException e) {
                    }
                })) {

            OcrService service = new OcrService(null, null);
            String result = service.extractText(new ByteArrayInputStream(pdfBytes), "doc.pdf");

            String expected = "page1-text" + System.lineSeparator() + System.lineSeparator() + "page2-text";
            assertEquals(expected, result);
        }
    }


    @Test
    void extractText_pdf_noText_returnsEmpty() throws Exception {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        doc.close();
        byte[] pdfBytes = baos.toByteArray();

        try (MockedConstruction<Tesseract> mocked = Mockito.mockConstruction(Tesseract.class,
                (mock, context) -> {
                    try {
                        Mockito.when(mock.doOCR(Mockito.any(BufferedImage.class))).thenReturn("   ");
                    } catch (TesseractException e) {
                    }
                })) {

            OcrService service = new OcrService(null, null);
            String result = service.extractText(new ByteArrayInputStream(pdfBytes), "empty.pdf");

            assertEquals("", result);
        }
    }

    @Test
    void extractText_pdf_loadFails_throwsOcrProcessingException() throws Exception {
        byte[] notPdf = "not a pdf".getBytes();
        OcrService service = new OcrService(null, null);

        OcrProcessingException ex = assertThrows(OcrProcessingException.class,
                () -> service.extractText(new ByteArrayInputStream(notPdf), "weird.pdf"));

        assertTrue(ex.getMessage().contains("Failed to render PDF for key: weird.pdf"));
    }
}
