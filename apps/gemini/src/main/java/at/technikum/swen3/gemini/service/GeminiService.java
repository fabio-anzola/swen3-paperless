package at.technikum.swen3.gemini.service;

import at.technikum.swen3.gemini.config.GeminiProperties;
import at.technikum.swen3.gemini.dto.GeminiCandidate;
import at.technikum.swen3.gemini.dto.GeminiContent;
import at.technikum.swen3.gemini.dto.GeminiGenerationConfig;
import at.technikum.swen3.gemini.dto.GeminiInlineData;
import at.technikum.swen3.gemini.dto.GeminiPart;
import at.technikum.swen3.gemini.dto.GeminiRequest;
import at.technikum.swen3.gemini.dto.GeminiResponse;
import at.technikum.swen3.gemini.dto.SummaryResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GeminiService {

    private static final String SUMMARY_PROMPT = """
            Provide a concise summary of the attached document. Focus on the main ideas and keep it easy to skim.
            Limit the response to a few sentences and avoid adding extra commentary.""";

    private final GeminiProperties properties;
    private final RestClient restClient;

    public GeminiService(GeminiProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder
                .baseUrl(properties.getEndpoint())
                .build();
    }

    public SummaryResponse summarize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gemini API key is not configured");
        }

        try {
            byte[] data = file.getBytes();
            String encoded = Base64.getEncoder().encodeToString(data);
            String mimeType = resolveMimeType(file);

            GeminiRequest request = buildRequest(encoded, mimeType);
            GeminiResponse response = invokeGemini(request);
            String summary = extractSummary(response);

            return new SummaryResponse(summary);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded file", e);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini request failed: " + e.getMessage(), e);
        }
    }

    private GeminiRequest buildRequest(String encodedData, String mimeType) {
        GeminiInlineData inlineData = new GeminiInlineData(mimeType, encodedData);
        GeminiPart instruction = new GeminiPart(SUMMARY_PROMPT, null);
        GeminiPart attachment = new GeminiPart(null, inlineData);

        GeminiContent content = new GeminiContent(List.of(instruction, attachment));
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
        return Optional.ofNullable(response)
                .map(GeminiResponse::candidates)
                .filter(candidates -> !candidates.isEmpty())
                .map(candidates -> candidates.getFirst().content())
                .map(GeminiContent::parts)
                .filter(parts -> !parts.isEmpty())
                .flatMap(parts -> parts.stream()
                        .map(GeminiPart::text)
                        .filter(StringUtils::hasText)
                        .findFirst())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini returned no summary"));
    }

    private String resolveMimeType(MultipartFile file) {
        if (StringUtils.hasText(file.getContentType())) {
            return file.getContentType();
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
