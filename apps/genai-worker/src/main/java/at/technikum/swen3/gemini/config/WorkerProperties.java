package at.technikum.swen3.gemini.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "worker")
public class WorkerProperties {

    @NotBlank
    private String bootstrapServers = "localhost:9092";

    @NotBlank
    private String groupId = "genai-workers";

    @NotBlank
    private String inputTopic = "genai-queue";

    @NotBlank
    private String outputTopic = "result";

    @NotBlank
    private String autoOffsetReset = "earliest";

    private boolean enableAutoCommit = false;

    @Min(100)
    private long pollMs = 1000;

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getInputTopic() {
        return inputTopic;
    }

    public void setInputTopic(String inputTopic) {
        this.inputTopic = inputTopic;
    }

    public String getOutputTopic() {
        return outputTopic;
    }

    public void setOutputTopic(String outputTopic) {
        this.outputTopic = outputTopic;
    }

    public String getAutoOffsetReset() {
        return autoOffsetReset;
    }

    public void setAutoOffsetReset(String autoOffsetReset) {
        this.autoOffsetReset = autoOffsetReset;
    }

    public boolean isEnableAutoCommit() {
        return enableAutoCommit;
    }

    public void setEnableAutoCommit(boolean enableAutoCommit) {
        this.enableAutoCommit = enableAutoCommit;
    }

    public long getPollMs() {
        return pollMs;
    }

    public void setPollMs(long pollMs) {
        this.pollMs = pollMs;
    }
}
