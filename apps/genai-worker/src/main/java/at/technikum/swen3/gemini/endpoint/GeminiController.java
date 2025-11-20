package at.technikum.swen3.gemini.endpoint;

import at.technikum.swen3.gemini.dto.SummaryResponse;
import at.technikum.swen3.gemini.service.GeminiService;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/summarize")
public class GeminiController {

    private static final Logger LOG = LoggerFactory.getLogger(GeminiController.class);

    private final GeminiService geminiService;

    public GeminiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SummaryResponse summarize(@RequestPart("file") @NotNull MultipartFile file) {
        LOG.info("Received document for summarization: name={}, size={}", file.getOriginalFilename(), file.getSize());
        return geminiService.summarize(file);
    }
}
