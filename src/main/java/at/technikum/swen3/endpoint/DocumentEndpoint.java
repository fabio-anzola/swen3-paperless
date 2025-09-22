package at.technikum.swen3.endpoint;

import at.technikum.swen3.service.IDocumentService;
import at.technikum.swen3.service.IUserService;
import at.technikum.swen3.service.dtos.document.DocumentDto;
import at.technikum.swen3.service.dtos.document.DocumentUploadDto;
import at.technikum.swen3.service.model.DocumentDownload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

@RestController
@RequestMapping(value = "/api/v1/document")
public class DocumentEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final IDocumentService documentService;
    private final IUserService userService;

    public DocumentEndpoint(IDocumentService documentService, IUserService userService) {
        this.documentService = documentService;
        this.userService = userService;
    }

    @GetMapping
    public Page<DocumentDto> listMine(Authentication authentication, Pageable pageable) {
        Long userId = userService.findByUsername(authentication.getName()).getId();
        return documentService.listMine(userId, pageable);
    }

    @GetMapping("/{id}")
    public DocumentDto getMeta(Authentication authentication, @PathVariable Long id) {
        Long userId = userService.findByUsername(authentication.getName()).getId();
        return documentService.getMeta(userId, id);
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> download(Authentication authentication, @PathVariable Long id) {
        Long userId = userService.findByUsername(authentication.getName()).getId();
        DocumentDownload dl = documentService.download(userId, id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(dl.filename()).build());
        if (dl.contentLength() != null) headers.setContentLength(dl.contentLength());
        headers.setContentType(MediaType.parseMediaType(dl.contentType()));

        return ResponseEntity.ok().headers(headers).body(dl.body());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentDto upload(Authentication authentication,
                              @RequestPart("file") MultipartFile file,
                              @RequestPart(value = "meta", required = false) String metaJson) throws IOException {
        Long userId = userService.findByUsername(authentication.getName()).getId();
        DocumentUploadDto meta = (metaJson != null && !metaJson.isBlank())
                ? new ObjectMapper().readValue(metaJson, DocumentUploadDto.class)
                : null;
        return documentService.upload(userId, file, meta);
    }

    @PutMapping("/{id}")
    public DocumentDto updateMeta(Authentication authentication,
                                  @PathVariable Long id,
                                  @RequestBody DocumentUploadDto meta) {
        Long userId = userService.findByUsername(authentication.getName()).getId();
        return documentService.updateMeta(userId, id, meta);
    }

    @DeleteMapping("/{id}")
    public void delete(Authentication authentication, @PathVariable Long id) {
        Long userId = userService.findByUsername(authentication.getName()).getId();
        documentService.delete(userId, id);
    }
}