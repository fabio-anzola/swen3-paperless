package at.technikum.swen3.batch.service;

import at.technikum.swen3.batch.service.model.ImportPayload;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

@Component
public class XmlImportParser {

    private static final Logger LOG = LoggerFactory.getLogger(XmlImportParser.class);

    public ImportPayload parse(Path file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(file.toFile());
        document.getDocumentElement().normalize();

        String content = getText(document, "Content");
        String dateText = getText(document, "Date");
        String description = getText(document, "Description");

        if (isBlank(content) || isBlank(dateText) || isBlank(description)) {
            throw new IllegalArgumentException("XML file is missing required fields");
        }

        try {
            LocalDate date = LocalDate.parse(dateText.trim());
            return new ImportPayload(content.trim(), date, description.trim());
        } catch (DateTimeParseException ex) {
            LOG.warn("Invalid date '{}' in file {}", dateText, file.getFileName());
            throw new IllegalArgumentException("Invalid date format in XML", ex);
        }
    }

    private String getText(Document document, String tagName) {
        Node node = document.getElementsByTagName(tagName).item(0);
        return node != null && node.getTextContent() != null ? node.getTextContent() : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
