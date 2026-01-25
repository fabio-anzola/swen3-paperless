package at.technikum.swen3.batch.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class BatchPropertiesTest {

    @Test
    void validate_acceptsValidConfiguration() {
        BatchProperties props = new BatchProperties();
        props.setFolderPath("/tmp/imports");
        props.setRunTime("02:15");
        props.setTimezone("UTC");
        props.setImportUrl("http://localhost:8080/api/v1/paperless/import");

        props.validate();

        assertEquals(Paths.get("/tmp/imports").toAbsolutePath().normalize(), props.batchFolder());
        assertEquals(LocalTime.of(2, 15), props.runAt());
        assertEquals(ZoneId.of("UTC"), props.zoneId());
        assertEquals(URI.create("http://localhost:8080/api/v1/paperless/import"), props.importUri());
    }

    @Test
    void validate_throwsWhenRunTimeMissing() {
        BatchProperties props = new BatchProperties();
        props.setFolderPath("/tmp/imports");

        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    void validate_throwsWhenRunTimeHasWrongFormat() {
        BatchProperties props = new BatchProperties();
        props.setFolderPath("/tmp/imports");
        props.setRunTime("2 PM");

        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    void validate_throwsWhenTimezoneInvalid() {
        BatchProperties props = new BatchProperties();
        props.setFolderPath("/tmp/imports");
        props.setRunTime("02:00");
        props.setTimezone("Invalid/Zone");

        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    void validate_throwsWhenImportUrlInvalid() {
        BatchProperties props = new BatchProperties();
        props.setFolderPath("/tmp/imports");
        props.setRunTime("02:00");
        props.setImportUrl("ht!tp://bad-url");

        assertThrows(IllegalStateException.class, props::validate);
    }
}
