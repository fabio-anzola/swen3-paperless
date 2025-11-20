package at.technikum.swen3.gemini.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiCandidate(
        GeminiContent content,
        @JsonProperty("finishReason") String finishReason
) { }
