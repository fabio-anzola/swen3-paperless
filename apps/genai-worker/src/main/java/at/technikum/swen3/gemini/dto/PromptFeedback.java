package at.technikum.swen3.gemini.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PromptFeedback(String blockReason) { }
