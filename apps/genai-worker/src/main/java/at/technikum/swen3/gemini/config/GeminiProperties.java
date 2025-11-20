package at.technikum.swen3.gemini.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "gemini")
@Validated
public class GeminiProperties {

    /**
     * API key used to authenticate against the Gemini API.
     */
    @NotBlank
    private String apiKey;

    /**
     * Model name, e.g., gemini-1.5-flash-latest.
     */
    private String model = "gemini-1.5-flash-latest";

    /**
     * Base URL of the Gemini API.
     */
    private String endpoint = "https://generativelanguage.googleapis.com/v1beta";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
