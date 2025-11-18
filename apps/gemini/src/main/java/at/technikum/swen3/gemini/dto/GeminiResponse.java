package at.technikum.swen3.gemini.dto;

import java.util.List;

public record GeminiCandidate(GeminiContent content) { }

public record GeminiResponse(List<GeminiCandidate> candidates) { }
