package at.technikum.swen3.gemini.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiPart(
        @JsonProperty("text") String text,
        @JsonProperty("inline_data") GeminiInlineData inlineData
) { }

public record GeminiInlineData(
        @JsonProperty("mime_type") String mimeType,
        String data
) { }

public record GeminiContent(List<GeminiPart> parts) { }

public record GeminiGenerationConfig(
        @JsonProperty("maxOutputTokens") Integer maxOutputTokens,
        Double temperature
) { }

public record GeminiRequest(
        List<GeminiContent> contents,
        @JsonProperty("generationConfig") GeminiGenerationConfig generationConfig
) { }
