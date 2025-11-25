package at.technikum.swen3.gemini.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiPart(
        @JsonProperty("text") String text,
        @JsonProperty("inline_data") GeminiInlineData inlineData
) { }
