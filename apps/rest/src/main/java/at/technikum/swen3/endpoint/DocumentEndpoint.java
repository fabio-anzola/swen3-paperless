package at.technikum.swen3.endpoint;

import at.technikum.swen3.exception.DocumentNotFoundException;
import at.technikum.swen3.exception.UserNotFoundException;
import at.technikum.swen3.service.IDocumentService;
import at.technikum.swen3.service.IUserService;
import at.technikum.swen3.service.dtos.document.DocumentDto;
import at.technikum.swen3.service.dtos.document.DocumentUploadDto;
import at.technikum.swen3.service.model.DocumentDownload;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

@RestController
@RequestMapping(value = "/api/v1/document")
public class DocumentEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IDocumentService documentService;
    private final IUserService userService;
    private final ObjectMapper objectMapper;

    @Autowired
    public DocumentEndpoint(IDocumentService documentService, IUserService userService, ObjectMapper objectMapper) {
        this.documentService = documentService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public Page<DocumentDto> listMine(Authentication authentication, Pageable pageable) {
        Long userId = userService.findByUsername(authentication.getName()).getId();
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized: user not found"
            );
        }
        LOG.info("Listing documents for userId={}", userId);
        return documentService.listMine(userId, pageable);
    }

    @GetMapping("/search")
    public Page<DocumentDto> searchMine(Authentication authentication,
                                        @RequestParam(value = "query", required = false) String query,
                                        @RequestParam(value = "q", required = false) String q,
                                        Pageable pageable) {
        Long userId = userService.findByUsername(authentication.getName()).getId();
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized: user not found"
            );
        }
        String effectiveQuery = (query != null && !query.isBlank()) ? query : q;
        LOG.info("Searching documents for userId={}, query={}", userId, effectiveQuery);
        return documentService.searchMine(userId, effectiveQuery, pageable);
    }

    @GetMapping("/{id}")
    public DocumentDto getMeta(Authentication authentication, @PathVariable Long id) {
        Long userId = userService.findByUsername(authentication.getName()).getId();
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized: user not found"
            );
        }
        LOG.info("Fetching metadata for documentId={} by userId={}", id, userId);
        return documentService.getMeta(userId, id);
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> download(Authentication authentication, @PathVariable Long id) {
        Long userId = userService.findByUsername(authentication.getName()).getId();
        LOG.info("Downloading content for documentId={} by userId={}", id, userId);
        DocumentDownload dl = documentService.download(userId, id);
        if (dl == null) {
            throw new DocumentNotFoundException("Document not found");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(dl.filename()).build());
        if (dl.contentLength() != null) headers.setContentLength(dl.contentLength());
        headers.setContentType(MediaType.parseMediaType(dl.contentType()));

        return ResponseEntity.ok().headers(headers).body(dl.body());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentDto upload(Authentication authentication, @RequestPart("file") MultipartFile file, @RequestPart(value = "meta", required = false) String metaJson) throws IOException {
        Long userId = userService.findByUsername(authentication.getName()).getId();
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized: user not found"
            );
        }
        LOG.info("Uploading document for userId={}, filename={}", userId, file.getOriginalFilename());
        DocumentUploadDto meta = (metaJson != null && !metaJson.isBlank()) ? objectMapper.readValue(metaJson, DocumentUploadDto.class) : null;
        return documentService.upload(userId, file, meta);
    }

    @PutMapping("/{id}")
    public DocumentDto updateMeta(Authentication authentication, @PathVariable Long id, @Valid @RequestBody DocumentUploadDto meta) {
        Long userId = userService.findByUsername(authentication.getName()).getId();
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized: user not found"
            );
        }
        LOG.info("Updating metadata for documentId={} by userId={}", id, userId);
        return documentService.updateMeta(userId, id, meta);
    }

    @DeleteMapping("/{id}")
    public void delete(Authentication authentication, @PathVariable Long id) {
        Long userId = userService.findByUsername(authentication.getName()).getId();
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized: user not found"
            );
        }
        if (id == null) {
            throw new DocumentNotFoundException("Document not found");
        }
        LOG.info("Deleting documentId={} by userId={}", id, userId);
        documentService.delete(userId, id);
    }
}
