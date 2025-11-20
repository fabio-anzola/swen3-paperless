package at.technikum.swen3.gemini.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import at.technikum.swen3.gemini.config.GeminiProperties;
import at.technikum.swen3.gemini.dto.GeminiCandidate;
import at.technikum.swen3.gemini.dto.GeminiContent;
import at.technikum.swen3.gemini.dto.GeminiGenerationConfig;
import at.technikum.swen3.gemini.dto.GeminiPart;
import at.technikum.swen3.gemini.dto.GeminiRequest;
import at.technikum.swen3.gemini.dto.GeminiResponse;
import at.technikum.swen3.gemini.dto.SummaryResponse;

@Service
public class GeminiService {

    private static final String SUMMARY_PROMPT = "Provide a concise summary of the following text extracted from a document.";
    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final GeminiProperties properties;
    private final RestClient restClient;

    public GeminiService(GeminiProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder
                .baseUrl(properties.getEndpoint())
                .build();
    }

    public SummaryResponse summarize(String text) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("Text to summarize must not be blank");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("Gemini API key is not configured");
        }

        try {
            GeminiRequest request = buildRequest(text);
            GeminiResponse response = invokeGemini(request);
            String summary = extractSummary(response);

            return new SummaryResponse(summary);
        } catch (RestClientException e) {
            throw new IllegalStateException("Gemini request failed: " + e.getMessage(), e);
        }
    }

    private GeminiRequest buildRequest(String text) {
        GeminiPart instruction = new GeminiPart(SUMMARY_PROMPT, null);
        GeminiPart body = new GeminiPart(text, null);

        GeminiContent content = new GeminiContent(List.of(instruction, body));
        GeminiGenerationConfig config = new GeminiGenerationConfig(256, 0.4);

        return new GeminiRequest(List.of(content), config);
    }

    private GeminiResponse invokeGemini(GeminiRequest request) {
        return restClient.post()
                .uri("/models/{model}:generateContent?key={apiKey}", properties.getModel(), properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GeminiResponse.class);
    }

    private String extractSummary(GeminiResponse response) {
        if (response == null) {
            throw new IllegalStateException("Gemini response was null");
        }

        if (response.promptFeedback() != null && StringUtils.hasText(response.promptFeedback().blockReason())) {
            throw new IllegalStateException("Gemini blocked the prompt: " + response.promptFeedback().blockReason());
        }

        List<GeminiCandidate> candidates = Optional.ofNullable(response.candidates())
                .filter(list -> !list.isEmpty())
                .orElseThrow(() -> new IllegalStateException("Gemini returned no summary text"));

        GeminiCandidate firstCandidate = candidates.getFirst();
        if (StringUtils.hasText(firstCandidate.finishReason())
                && !"STOP".equalsIgnoreCase(firstCandidate.finishReason())) {
            throw new IllegalStateException("Gemini did not finish: " + firstCandidate.finishReason());
        }

        Optional<String> summary = Optional.ofNullable(firstCandidate.content())
                .map(GeminiContent::parts)
                .filter(parts -> !parts.isEmpty())
                .flatMap(parts -> parts.stream()
                        .map(GeminiPart::text)
                        .filter(StringUtils::hasText)
                        .findFirst());

        if (summary.isEmpty()) {
            log.warn("Gemini response had no text: {}", response);
            throw new IllegalStateException("Gemini returned no summary text");
        }

        return summary.get();
    }
}
