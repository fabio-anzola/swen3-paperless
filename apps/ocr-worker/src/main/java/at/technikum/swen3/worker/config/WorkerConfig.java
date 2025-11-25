package at.technikum.swen3.worker.config;

import java.nio.file.Files;
import java.nio.file.Path;

public class WorkerConfig {
    public final String bootstrapServers;
    public final String groupId;
    public final String inputTopic;
    public final String outputTopic;
    public final String autoOffsetReset;
    public final String enableAutoCommit;
    public final long pollMs;
    public final String minioUrl;
    public final String minioAccessKey;
    public final String minioSecretKey;
    public final String minioBucketName;
    public final String tessdataPath;
    public final String tesseractLanguage;

    public WorkerConfig() {
        this.bootstrapServers = getEnv("BOOTSTRAP_SERVERS", "localhost:9092");
        this.groupId = getEnv("GROUP_ID", "demo-consumer-group");
        this.inputTopic = getEnv("TOPIC", "ocr");
        this.outputTopic = getEnv("OUTPUT_TOPIC", "genai-queue");
        this.autoOffsetReset = getEnv("AUTO_OFFSET_RESET", "earliest");
        this.enableAutoCommit = getEnv("ENABLE_AUTO_COMMIT", "false");
        this.pollMs = Long.parseLong(getEnv("POLL_MS", "1000"));
        this.minioUrl = getEnv("MINIO_URL", "http://localhost:9000");
        this.minioAccessKey = getEnv("MINIO_ACCESS_KEY", "minioadmin");
        this.minioSecretKey = getEnv("MINIO_SECRET_KEY", "minioadmin");
        this.minioBucketName = getEnv("MINIO_BUCKET_NAME", "uploaded-documents");
        this.tessdataPath = getEnv("TESSDATA_PATH", resolveDefaultTessdataPath());
        this.tesseractLanguage = getEnv("TESSERACT_LANGUAGE", "eng");
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }

    private static String resolveDefaultTessdataPath() {
        String[] candidates = {
            "/usr/share/tesseract-ocr/5/tessdata",
            "/usr/share/tesseract-ocr/4.00/tessdata",
            "/usr/share/tesseract-ocr/tessdata"
        };
        for (String candidate : candidates) {
            try {
                if (Files.isDirectory(Path.of(candidate))) {
                    return candidate;
                }
            } catch (SecurityException ignored) {
                // fall through to next candidate
            }
        }
        return candidates[0];
    }
}
