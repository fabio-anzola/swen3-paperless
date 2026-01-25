package at.technikum.swen3.batch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import at.technikum.swen3.batch.service.model.ImportPayload;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class XmlImportParserTest {

    private final XmlImportParser parser = new XmlImportParser();

    @Test
    void parse_returnsPayloadWhenXmlValid() throws Exception {
        Path file = Files.createTempFile("import", ".xml");
        Files.writeString(file, """
                <Document>
                  <Content>Hello</Content>
                  <Date>2026-01-21</Date>
                  <Description>Sample</Description>
                </Document>
                """);

        ImportPayload payload = parser.parse(file);

        assertEquals("Hello", payload.content());
        assertEquals(LocalDate.parse("2026-01-21"), payload.date());
        assertEquals("Sample", payload.description());
    }

    @Test
    void parse_throwsWhenRequiredFieldMissing() throws Exception {
        Path file = Files.createTempFile("import-missing", ".xml");
        Files.writeString(file, """
                <Document>
                  <Content>Hello</Content>
                  <Date></Date>
                </Document>
                """);

        assertThrows(IllegalArgumentException.class, () -> parser.parse(file));
    }

    @Test
    void parse_throwsWhenDateInvalid() throws Exception {
        Path file = Files.createTempFile("import-invalid-date", ".xml");
        Files.writeString(file, """
                <Document>
                  <Content>Hello</Content>
                  <Date>21-01-2026</Date>
                  <Description>Bad date</Description>
                </Document>
                """);

        assertThrows(IllegalArgumentException.class, () -> parser.parse(file));
    }
}
