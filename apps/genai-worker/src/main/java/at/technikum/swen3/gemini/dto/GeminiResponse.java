package at.technikum.swen3.gemini.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiResponse(
        List<GeminiCandidate> candidates,
        @JsonProperty("promptFeedback") PromptFeedback promptFeedback
) { }

