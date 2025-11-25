package at.technikum.swen3.gemini.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GeminiInlineData(
        @JsonProperty("mime_type") String mimeType,
        String data
) { }
