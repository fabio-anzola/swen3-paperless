package at.technikum.swen3.batch.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;

@Validated
@ConfigurationProperties(prefix = "batch")
public class BatchProperties {

    private static final DateTimeFormatter RUN_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @NotBlank
    private String folderPath;

    @NotBlank
    private String runTime;

    private String timezone = "UTC";

    @NotBlank
    private String importUrl = "http://localhost:8080/api/v1/paperless/import";

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public String getRunTime() {
        return runTime;
    }

    public void setRunTime(String runTime) {
        this.runTime = runTime;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getImportUrl() {
        return importUrl;
    }

    public void setImportUrl(String importUrl) {
        this.importUrl = importUrl;
    }

    @PostConstruct
    public void validate() {
        if (!StringUtils.hasText(folderPath)) {
            throw new IllegalStateException("batch.folder-path (BATCH_FOLDER_PATH) is required");
        }
        if (!StringUtils.hasText(runTime)) {
            throw new IllegalStateException("batch.run-time (BATCH_RUN_TIME) is required");
        }
        try {
            runAt();
        } catch (DateTimeParseException ex) {
            throw new IllegalStateException("batch.run-time must use HH:mm (24h) format", ex);
        }

        try {
            ZoneId.of(timezone);
        } catch (Exception ex) {
            throw new IllegalStateException("batch.timezone is invalid: " + timezone, ex);
        }

        try {
            URI.create(importUrl);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("batch.import-url is invalid: " + importUrl, ex);
        }
    }

    public Path batchFolder() {
        return Paths.get(folderPath).toAbsolutePath().normalize();
    }

    public LocalTime runAt() {
        return LocalTime.parse(runTime, RUN_TIME_FORMATTER);
    }

    public ZoneId zoneId() {
        return ZoneId.of(timezone);
    }

    public URI importUri() {
        return URI.create(importUrl);
    }
}
