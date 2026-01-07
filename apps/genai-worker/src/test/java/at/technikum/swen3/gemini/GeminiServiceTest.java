package at.technikum.swen3.gemini;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import at.technikum.swen3.gemini.config.GeminiProperties;
import at.technikum.swen3.gemini.dto.GeminiCandidate;
import at.technikum.swen3.gemini.dto.GeminiContent;
import at.technikum.swen3.gemini.dto.GeminiPart;
import at.technikum.swen3.gemini.dto.GeminiRequest;
import at.technikum.swen3.gemini.dto.GeminiResponse;
import at.technikum.swen3.gemini.dto.PromptFeedback;
import at.technikum.swen3.gemini.dto.SummaryResponse;
import at.technikum.swen3.gemini.service.GeminiService;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    private GeminiProperties geminiProperties;

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        when(geminiProperties.getEndpoint()).thenReturn("https://test-endpoint.com");
        lenient().when(geminiProperties.getApiKey()).thenReturn("test-api-key");
        lenient().when(geminiProperties.getModel()).thenReturn("gemini-1.5-flash-latest");
        lenient().when(geminiProperties.getMaxOutputTokens()).thenReturn(256);
        lenient().when(geminiProperties.getTemperature()).thenReturn(0.4);

        when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);
    }

    private void createGeminiService() {
        geminiService = new GeminiService(geminiProperties, restClientBuilder);
    }

    @Test
    void constructor_shouldBuildRestClientWithCorrectBaseUrl() {
        createGeminiService();
        verify(restClientBuilder, atLeastOnce()).baseUrl("https://test-endpoint.com");
        verify(restClientBuilder, atLeastOnce()).build();
    }

    @Test
    void summarize_shouldReturnSummary_whenValidTextProvided() {
        createGeminiService();
        String inputText = "This is a long document that needs to be summarized.";
        String expectedSummary = "A concise summary of the document.";

        GeminiPart summaryPart = new GeminiPart(expectedSummary, null);
        GeminiContent responseContent = new GeminiContent(List.of(summaryPart));
        GeminiCandidate candidate = new GeminiCandidate(responseContent, "STOP");
        GeminiResponse geminiResponse = new GeminiResponse(List.of(candidate), null);

        setupMockRestClientChain(geminiResponse);

        SummaryResponse result = geminiService.summarize(inputText);

        assertThat(result).isNotNull();
        assertThat(result.summary()).isEqualTo(expectedSummary);

        verify(restClient).post();
        verify(requestBodyUriSpec).uri(
            "/models/{model}:generateContent?key={apiKey}",
            "gemini-1.5-flash-latest",
            "test-api-key"
        );
        verify(requestBodySpec).contentType(MediaType.APPLICATION_JSON);
        verify(responseSpec).body(GeminiResponse.class);
    }

    @Test
    void summarize_shouldBuildCorrectRequest() {
        createGeminiService();
        String inputText = "Test document content.";
        String expectedSummary = "Summary";

        GeminiPart summaryPart = new GeminiPart(expectedSummary, null);
        GeminiContent responseContent = new GeminiContent(List.of(summaryPart));
        GeminiCandidate candidate = new GeminiCandidate(responseContent, "STOP");
        GeminiResponse geminiResponse = new GeminiResponse(List.of(candidate), null);

        setupMockRestClientChain(geminiResponse);

        ArgumentCaptor<GeminiRequest> requestCaptor = ArgumentCaptor.forClass(GeminiRequest.class);

        geminiService.summarize(inputText);

        verify(requestBodySpec).body(requestCaptor.capture());

        GeminiRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest).isNotNull();
        assertThat(capturedRequest.contents()).hasSize(1);

        GeminiContent content = capturedRequest.contents().get(0);
        assertThat(content.parts()).hasSize(2);
        assertThat(content.parts().get(0).text()).contains("concise summary");
        assertThat(content.parts().get(1).text()).isEqualTo(inputText);

        assertThat(capturedRequest.generationConfig()).isNotNull();
        assertThat(capturedRequest.generationConfig().maxOutputTokens()).isEqualTo(256);
        assertThat(capturedRequest.generationConfig().temperature()).isEqualTo(0.4);
    }

    @Test
    void summarize_shouldThrowException_whenTextIsNull() {
        createGeminiService();
        assertThatThrownBy(() -> geminiService.summarize(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Text to summarize must not be blank");

        verifyNoInteractions(restClient);
    }

    @Test
    void summarize_shouldThrowException_whenTextIsEmpty() {
        createGeminiService();
        assertThatThrownBy(() -> geminiService.summarize(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Text to summarize must not be blank");

        verifyNoInteractions(restClient);
    }

    @Test
    void summarize_shouldThrowException_whenTextIsBlank() {
        createGeminiService();
        assertThatThrownBy(() -> geminiService.summarize("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Text to summarize must not be blank");

        verifyNoInteractions(restClient);
    }

    @Test
    void summarize_shouldThrowException_whenApiKeyIsNull() {
        when(geminiProperties.getApiKey()).thenReturn(null);
        createGeminiService();

        assertThatThrownBy(() -> geminiService.summarize("Test text"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Gemini API key is not configured");

        verifyNoInteractions(restClient);
    }

    @Test
    void summarize_shouldThrowException_whenApiKeyIsEmpty() {
        when(geminiProperties.getApiKey()).thenReturn("");
        createGeminiService();

        assertThatThrownBy(() -> geminiService.summarize("Test text"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Gemini API key is not configured");

        verifyNoInteractions(restClient);
    }

    @Test
    void summarize_shouldThrowException_whenApiKeyIsBlank() {
        when(geminiProperties.getApiKey()).thenReturn("   ");
        createGeminiService();

        assertThatThrownBy(() -> geminiService.summarize("Test text"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Gemini API key is not configured");

        verifyNoInteractions(restClient);
    }

    @Test
    void summarize_shouldThrowException_whenRestClientFails() {
        createGeminiService();
        String inputText = "Test text";
        setupMockRestClientChain(null);
        when(responseSpec.body(GeminiResponse.class))
            .thenThrow(new RestClientException("Network error"));

        assertThatThrownBy(() -> geminiService.summarize(inputText))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Gemini request failed")
            .hasCauseInstanceOf(RestClientException.class);
    }

    @Test
    void summarize_shouldThrowException_whenResponseIsNull() {
        createGeminiService();
        String inputText = "Test text";
        setupMockRestClientChain(null);

        assertThatThrownBy(() -> geminiService.summarize(inputText))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Gemini response was null");
    }

    @Test
    void summarize_shouldThrowException_whenPromptIsBlocked() {
        createGeminiService();
        String inputText = "Inappropriate content";
        PromptFeedback feedback = new PromptFeedback("SAFETY");
        GeminiResponse geminiResponse = new GeminiResponse(null, feedback);

        setupMockRestClientChain(geminiResponse);

        assertThatThrownBy(() -> geminiService.summarize(inputText))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Gemini blocked the prompt: SAFETY");
    }

    @Test
    void summarize_shouldThrowException_whenCandidatesListIsNull() {
        createGeminiService();
        String inputText = "Test text";
        GeminiResponse geminiResponse = new GeminiResponse(null, null);

        setupMockRestClientChain(geminiResponse);

        assertThatThrownBy(() -> geminiService.summarize(inputText))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Gemini returned no summary text");
    }

    @Test
    void summarize_shouldThrowException_whenCandidatesListIsEmpty() {
        createGeminiService();
        String inputText = "Test text";
        GeminiResponse geminiResponse = new GeminiResponse(Collections.emptyList(), null);

        setupMockRestClientChain(geminiResponse);

        assertThatThrownBy(() -> geminiService.summarize(inputText))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Gemini returned no summary text");
    }

    @Test
    void summarize_shouldLogWarningAndContinue_whenMaxTokensReached() {
        createGeminiService();
        String inputText = "Test text";
        String expectedSummary = "Partial summary that was truncated";
        GeminiPart summaryPart = new GeminiPart(expectedSummary, null);
        GeminiContent responseContent = new GeminiContent(List.of(summaryPart));
        GeminiCandidate candidate = new GeminiCandidate(responseContent, "MAX_TOKENS");
        GeminiResponse geminiResponse = new GeminiResponse(List.of(candidate), null);

        setupMockRestClientChain(geminiResponse);

        SummaryResponse result = geminiService.summarize(inputText);

        assertThat(result.summary()).isEqualTo(expectedSummary);
    }

    @Test
    void summarize_shouldReturnFallbackMessage_whenMaxTokensReachedAndNoText() {
        createGeminiService();
        String inputText = "Test text";
        GeminiPart summaryPart = new GeminiPart("", null);
        GeminiContent responseContent = new GeminiContent(List.of(summaryPart));
        GeminiCandidate candidate = new GeminiCandidate(responseContent, "MAX_TOKENS");
        GeminiResponse geminiResponse = new GeminiResponse(List.of(candidate), null);

        setupMockRestClientChain(geminiResponse);

        SummaryResponse result = geminiService.summarize(inputText);

        assertThat(result.summary()).isEqualTo("Summary truncated; Gemini returned no text (MAX_TOKENS)");
    }

    @Test
    void summarize_shouldThrowException_whenFinishReasonIsOther() {
        createGeminiService();
        String inputText = "Test text";
        GeminiPart summaryPart = new GeminiPart("Partial summary", null);
        GeminiContent responseContent = new GeminiContent(List.of(summaryPart));
        GeminiCandidate candidate = new GeminiCandidate(responseContent, "SAFETY");
        GeminiResponse geminiResponse = new GeminiResponse(List.of(candidate), null);

        setupMockRestClientChain(geminiResponse);

        assertThatThrownBy(() -> geminiService.summarize(inputText))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Gemini did not finish: SAFETY");
    }

    @Test
    void summarize_shouldSucceed_whenFinishReasonIsStopInLowerCase() {
        createGeminiService();
        String inputText = "Test text";
        String expectedSummary = "Summary";
        GeminiPart summaryPart = new GeminiPart(expectedSummary, null);
        GeminiContent responseContent = new GeminiContent(List.of(summaryPart));
        GeminiCandidate candidate = new GeminiCandidate(responseContent, "stop");
        GeminiResponse geminiResponse = new GeminiResponse(List.of(candidate), null);

        setupMockRestClientChain(geminiResponse);

        SummaryResponse result = geminiService.summarize(inputText);

        assertThat(result.summary()).isEqualTo(expectedSummary);
    }

    @Test
    void summarize_shouldSucceed_whenFinishReasonIsNull() {
        createGeminiService();
        String inputText = "Test text";
        String expectedSummary = "Summary";
        GeminiPart summaryPart = new GeminiPart(expectedSummary, null);
        GeminiContent responseContent = new GeminiContent(List.of(summaryPart));
        GeminiCandidate candidate = new GeminiCandidate(responseContent, null);
        GeminiResponse geminiResponse = new GeminiResponse(List.of(candidate), null);

        setupMockRestClientChain(geminiResponse);

        SummaryResponse result = geminiService.summarize(inputText);

        assertThat(result.summary()).isEqualTo(expectedSummary);
    }

    @Test
    void summarize_shouldSucceed_whenFinishReasonIsEmpty() {
        createGeminiService();
        String inputText = "Test text";
        String expectedSummary = "Summary";
        GeminiPart summaryPart = new GeminiPart(expectedSummary, null);
        GeminiContent responseContent = new GeminiContent(List.of(summaryPart));
        GeminiCandidate candidate = new GeminiCandidate(responseContent, "");
        GeminiResponse geminiResponse = new GeminiResponse(List.of(candidate), null);

        setupMockRestClientChain(geminiResponse);

        SummaryResponse result = geminiService.summarize(inputText);

        assertThat(result.summary()).isEqualTo(expectedSummary);
    }

    @Test
    void summarize_shouldThrowException_whenContentIsNull() {
        createGeminiService();
        String inputText = "Test text";
        GeminiCandidate candidate = new GeminiCandidate(null, "STOP");
        GeminiResponse geminiResponse = new GeminiResponse(List.of(candidate), null);

        setupMockRestClientChain(geminiResponse);

        assertThatThrownBy(() -> geminiService.summarize(inputText))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Gemini returned no summary text");
    }

    @Test
    void summarize_shouldThrowException_whenPartsListIsNull() {
        createGeminiService();
        String inputText = "Test text";
        GeminiContent responseContent = new GeminiContent(null);
        GeminiCandidate candidate = new GeminiCandidate(responseContent, "STOP");
        GeminiResponse geminiResponse = new GeminiResponse(List.of(candidate), null);

        setupMockRestClientChain(geminiResponse);

        assertThatThrownBy(() -> geminiService.summarize(inputText))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Gemini returned no summary text");
    }

    @Test
    void summarize_shouldThrowException_whenPartsListIsEmpty() {
        createGeminiService();
        String inputText = "Test text";
        GeminiContent responseContent = new GeminiContent(Collections.emptyList());
        GeminiCandidate candidate = new GeminiCandidate(responseContent, "STOP");
        GeminiResponse geminiResponse = new GeminiResponse(List.of(candidate), null);

        setupMockRestClientChain(geminiResponse);

        assertThatThrownBy(() -> geminiService.summarize(inputText))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Gemini returned no summary text");
    }

    @Test
    void summarize_shouldThrowException_whenPartTextIsNull() {
        createGeminiService();
        String inputText = "Test text";
        GeminiPart summaryPart = new GeminiPart(null, null);
        GeminiContent responseContent = new GeminiContent(List.of(summaryPart));
        GeminiCandidate candidate = new GeminiCandidate(responseContent, "STOP");
        GeminiResponse geminiResponse = new GeminiResponse(List.of(candidate), null);

        setupMockRestClientChain(geminiResponse);

        assertThatThrownBy(() -> geminiService.summarize(inputText))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Gemini returned no summary text");
    }

    @Test
    void summarize_shouldThrowException_whenPartTextIsEmpty() {
        createGeminiService();
        String inputText = "Test text";
        GeminiPart summaryPart = new GeminiPart("", null);
        GeminiContent responseContent = new GeminiContent(List.of(summaryPart));
        GeminiCandidate candidate = new GeminiCandidate(responseContent, "STOP");
        GeminiResponse geminiResponse = new GeminiResponse(List.of(candidate), null);

        setupMockRestClientChain(geminiResponse);

        assertThatThrownBy(() -> geminiService.summarize(inputText))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Gemini returned no summary text");
    }

    @Test
    void summarize_shouldThrowException_whenPartTextIsBlank() {
        createGeminiService();
        String inputText = "Test text";
        GeminiPart summaryPart = new GeminiPart("   ", null);
        GeminiContent responseContent = new GeminiContent(List.of(summaryPart));
        GeminiCandidate candidate = new GeminiCandidate(responseContent, "STOP");
        GeminiResponse geminiResponse = new GeminiResponse(List.of(candidate), null);

        setupMockRestClientChain(geminiResponse);

        assertThatThrownBy(() -> geminiService.summarize(inputText))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Gemini returned no summary text");
    }

    @Test
    void summarize_shouldReturnFirstValidText_whenMultiplePartsExist() {
        createGeminiService();
        String inputText = "Test text";
        String expectedSummary = "First valid summary";

        GeminiPart emptyPart = new GeminiPart("", null);
        GeminiPart validPart1 = new GeminiPart(expectedSummary, null);
        GeminiPart validPart2 = new GeminiPart("Second summary", null);

        GeminiContent responseContent = new GeminiContent(List.of(emptyPart, validPart1, validPart2));
        GeminiCandidate candidate = new GeminiCandidate(responseContent, "STOP");
        GeminiResponse geminiResponse = new GeminiResponse(List.of(candidate), null);

        setupMockRestClientChain(geminiResponse);

        SummaryResponse result = geminiService.summarize(inputText);

        assertThat(result.summary()).isEqualTo(expectedSummary);
    }

    @Test
    void summarize_shouldUseFirstCandidate_whenMultipleCandidatesExist() {
        createGeminiService();
        String inputText = "Test text";
        String expectedSummary = "Summary from first candidate";

        GeminiPart part1 = new GeminiPart(expectedSummary, null);
        GeminiContent content1 = new GeminiContent(List.of(part1));
        GeminiCandidate candidate1 = new GeminiCandidate(content1, "STOP");

        GeminiPart part2 = new GeminiPart("Summary from second candidate", null);
        GeminiContent content2 = new GeminiContent(List.of(part2));
        GeminiCandidate candidate2 = new GeminiCandidate(content2, "STOP");

        GeminiResponse geminiResponse = new GeminiResponse(List.of(candidate1, candidate2), null);

        setupMockRestClientChain(geminiResponse);

        SummaryResponse result = geminiService.summarize(inputText);

        assertThat(result.summary()).isEqualTo(expectedSummary);
    }

    private void setupMockRestClientChain(GeminiResponse response) {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString(), any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(GeminiRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(GeminiResponse.class)).thenReturn(response);
    }
}

