package at.technikum.swen3.worker.config;

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

    public WorkerConfig() {
        this.bootstrapServers = getEnv("BOOTSTRAP_SERVERS", "localhost:9092");
        this.groupId = getEnv("GROUP_ID", "demo-consumer-group");
        this.inputTopic = getEnv("TOPIC", "ocr");
        this.outputTopic = getEnv("OUTPUT_TOPIC", "result");
        this.autoOffsetReset = getEnv("AUTO_OFFSET_RESET", "earliest");
        this.enableAutoCommit = getEnv("ENABLE_AUTO_COMMIT", "false");
        this.pollMs = Long.parseLong(getEnv("POLL_MS", "1000"));
        this.minioUrl = getEnv("MINIO_URL", "http://localhost:9000");
        this.minioAccessKey = getEnv("MINIO_ACCESS_KEY", "minioadmin");
        this.minioSecretKey = getEnv("MINIO_SECRET_KEY", "minioadmin");
        this.minioBucketName = getEnv("MINIO_BUCKET_NAME", "uploaded-documents");
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}
