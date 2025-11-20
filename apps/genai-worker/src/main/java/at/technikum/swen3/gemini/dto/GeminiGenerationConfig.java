package at.technikum.swen3.gemini.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GeminiGenerationConfig(
        @JsonProperty("maxOutputTokens") Integer maxOutputTokens,
        Double temperature
) { }
