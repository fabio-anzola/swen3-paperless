package at.technikum.swen3.endpoint;

import at.technikum.swen3.service.IDocumentShareService;
import at.technikum.swen3.service.IUserService;
import at.technikum.swen3.service.dtos.share.DocumentShareAccessLogDto;
import at.technikum.swen3.service.dtos.share.DocumentShareCreateDto;
import at.technikum.swen3.service.dtos.share.DocumentShareDto;
import at.technikum.swen3.service.model.DocumentDownload;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.lang.invoke.MethodHandles;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class DocumentShareEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IDocumentShareService documentShareService;
    private final IUserService userService;

    @Autowired
    public DocumentShareEndpoint(IDocumentShareService documentShareService, IUserService userService) {
        this.documentShareService = documentShareService;
        this.userService = userService;
    }

    @PostMapping("/document/{id}/shares")
    public DocumentShareDto createShare(Authentication authentication,
                                        @PathVariable Long id,
                                        @RequestBody(required = false) DocumentShareCreateDto dto) {
        Long userId = userService.findByUsername(authentication.getName()).getId();
        LOG.info("Creating share for documentId={} by userId={}", id, userId);
        return documentShareService.createShare(userId, id, dto);
    }

    @GetMapping("/document/{id}/shares")
    public List<DocumentShareDto> listShares(Authentication authentication, @PathVariable Long id) {
        Long userId = userService.findByUsername(authentication.getName()).getId();
        LOG.info("Listing shares for documentId={} by userId={}", id, userId);
        return documentShareService.listShares(userId, id);
    }

    @GetMapping("/document/{id}/shares/logs")
    public List<DocumentShareAccessLogDto> listShareLogs(Authentication authentication, @PathVariable Long id) {
        Long userId = userService.findByUsername(authentication.getName()).getId();
        LOG.info("Listing share access logs for documentId={} by userId={}", id, userId);
        return documentShareService.listLogs(userId, id);
    }

    @DeleteMapping("/document/{id}/shares/{shareId}")
    public DocumentShareDto deactivateShare(Authentication authentication,
                                            @PathVariable Long id,
                                            @PathVariable Long shareId) {
        Long userId = userService.findByUsername(authentication.getName()).getId();
        LOG.info("Deactivating shareId={} for documentId={} by userId={}", shareId, id, userId);
        return documentShareService.deactivateShare(userId, id, shareId);
    }

    @GetMapping("/public/share/{token}")
    public ResponseEntity<Resource> downloadShared(@PathVariable String token,
                                                   @RequestParam(required = false) String password,
                                                   HttpServletRequest request) {
        LOG.info("Accessing shared document token={}", token);
        DocumentDownload dl = documentShareService.downloadShared(token, password, request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(dl.filename()).build());
        if (dl.contentLength() != null) headers.setContentLength(dl.contentLength());
        headers.setContentType(MediaType.parseMediaType(dl.contentType()));
        return ResponseEntity.ok().headers(headers).body(dl.body());
    }
}
