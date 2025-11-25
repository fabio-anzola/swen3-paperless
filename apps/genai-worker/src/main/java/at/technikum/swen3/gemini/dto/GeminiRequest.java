package at.technikum.swen3.gemini.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GeminiRequest(
        List<GeminiContent> contents,
        @JsonProperty("generationConfig") GeminiGenerationConfig generationConfig
) { }

