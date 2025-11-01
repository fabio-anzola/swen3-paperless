package at.technikum.swen3.worker.service;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class OcrService {
    private static final Logger logger = LoggerFactory.getLogger(OcrService.class);

    private final String tessdataPath;
    private final String language;

    public OcrService(String tessdataPath, String language) {
        this.tessdataPath = tessdataPath;
        this.language = language;
    }

    public String extractText(InputStream inputStream, String objectKey) throws OcrProcessingException {
        try {
            byte[] data = toByteArray(inputStream);
            if (data.length == 0) {
                throw new OcrProcessingException("Object is empty for key: " + objectKey);
            }

            BufferedImage image = tryReadImage(data);
            if (image != null) {
                return runOcrOnImage(image, objectKey);
            }

            if (looksLikePdf(data, objectKey)) {
                return runOcrOnPdf(data, objectKey);
            }

            throw new OcrProcessingException("Unsupported or unreadable object type for key: " + objectKey);
        } catch (IOException e) {
            throw new OcrProcessingException("Failed to read object for key: " + objectKey, e);
        }
    }

    private BufferedImage tryReadImage(byte[] data) throws IOException {
        try (ByteArrayInputStream imageStream = new ByteArrayInputStream(data)) {
            return ImageIO.read(new BufferedInputStream(imageStream));
        }
    }

    private String runOcrOnImage(BufferedImage image, String objectKey) throws OcrProcessingException {
        try {
            String text = buildTesseract().doOCR(image);
            logger.info("Extracted {} characters of text from {}", text != null ? text.length() : 0, objectKey);
            return text != null ? text.trim() : "";
        } catch (TesseractException e) {
            throw new OcrProcessingException("Tesseract failed for key: " + objectKey, e);
        }
    }

    private String runOcrOnPdf(byte[] data, String objectKey) throws OcrProcessingException {
        try (PDDocument document = PDDocument.load(data)) {
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder result = new StringBuilder();
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage pageImage = renderer.renderImageWithDPI(page, 300, ImageType.RGB);
                String pageText = runOcrOnImage(pageImage, objectKey + "#page=" + (page + 1));
                if (!pageText.isBlank()) {
                    if (result.length() > 0) {
                        result.append(System.lineSeparator()).append(System.lineSeparator());
                    }
                    result.append(pageText);
                }
            }
            String text = result.toString().trim();
            if (text.isEmpty()) {
                logger.warn("No text extracted from PDF {}", objectKey);
            }
            return text;
        } catch (IOException e) {
            throw new OcrProcessingException("Failed to render PDF for key: " + objectKey, e);
        }
    }

    private boolean looksLikePdf(byte[] data, String objectKey) {
        if (data.length >= 4 && data[0] == '%' && data[1] == 'P' && data[2] == 'D' && data[3] == 'F') {
            return true;
        }
        String lowerKey = objectKey != null ? objectKey.toLowerCase(Locale.ROOT) : "";
        return lowerKey.endsWith(".pdf");
    }

    private byte[] toByteArray(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            inputStream.transferTo(buffer);
            return buffer.toByteArray();
        }
    }

    private Tesseract buildTesseract() {
        Tesseract tesseract = new Tesseract();
        if (tessdataPath != null && !tessdataPath.isBlank()) {
            tesseract.setDatapath(tessdataPath);
        }
        if (language != null && !language.isBlank()) {
            tesseract.setLanguage(language);
        }
        return tesseract;
    }
}
