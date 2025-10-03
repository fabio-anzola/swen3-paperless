package at.technikum.swen3.worker.config;

public class WorkerConfig {
    public final String bootstrapServers;
    public final String groupId;
    public final String inputTopic;
    public final String outputTopic;
    public final String autoOffsetReset;
    public final String enableAutoCommit;
    public final long pollMs;

    public WorkerConfig() {
        this.bootstrapServers = getEnv("BOOTSTRAP_SERVERS", "localhost:9092");
        this.groupId = getEnv("GROUP_ID", "demo-consumer-group");
        this.inputTopic = getEnv("TOPIC", "ocr");
        this.outputTopic = getEnv("OUTPUT_TOPIC", "result");
        this.autoOffsetReset = getEnv("AUTO_OFFSET_RESET", "earliest");
        this.enableAutoCommit = getEnv("ENABLE_AUTO_COMMIT", "false");
        this.pollMs = Long.parseLong(getEnv("POLL_MS", "1000"));
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}
