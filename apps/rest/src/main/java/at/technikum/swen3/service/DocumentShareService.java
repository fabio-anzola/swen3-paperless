package at.technikum.swen3.service;

import at.technikum.swen3.entity.Document;
import at.technikum.swen3.entity.DocumentShare;
import at.technikum.swen3.entity.DocumentShareAccessLog;
import at.technikum.swen3.repository.DocumentRepository;
import at.technikum.swen3.repository.DocumentShareAccessLogRepository;
import at.technikum.swen3.repository.DocumentShareRepository;
import at.technikum.swen3.service.dtos.share.DocumentShareAccessLogDto;
import at.technikum.swen3.service.dtos.share.DocumentShareCreateDto;
import at.technikum.swen3.service.dtos.share.DocumentShareDto;
import at.technikum.swen3.service.model.DocumentDownload;
import io.minio.StatObjectResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DocumentShareService implements IDocumentShareService {

    private final DocumentRepository documentRepository;
    private final DocumentShareRepository documentShareRepository;
    private final DocumentShareAccessLogRepository accessLogRepository;
    private final S3Service s3Service;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DocumentShareService(DocumentRepository documentRepository,
                                DocumentShareRepository documentShareRepository,
                                DocumentShareAccessLogRepository accessLogRepository,
                                S3Service s3Service) {
        this.documentRepository = documentRepository;
        this.documentShareRepository = documentShareRepository;
        this.accessLogRepository = accessLogRepository;
        this.s3Service = s3Service;
    }

    @Override
    public DocumentShareDto createShare(Long ownerId, Long documentId, DocumentShareCreateDto dto) {
        Document document = findOwnedDocument(ownerId, documentId);

        Instant startsAt = dto != null && dto.startsAt() != null ? dto.startsAt() : Instant.now();
        Instant expiresAt = dto != null ? dto.expiresAt() : null;
        if (expiresAt != null && startsAt != null && expiresAt.isBefore(startsAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expiresAt must be after startsAt");
        }

        DocumentShare share = new DocumentShare();
        share.setDocument(document);
        share.setToken(UUID.randomUUID().toString());
        share.setStartsAt(startsAt);
        share.setExpiresAt(expiresAt);
        if (dto != null && dto.password() != null && !dto.password().isBlank()) {
            share.setPasswordHash(passwordEncoder.encode(dto.password()));
        }
        DocumentShare saved = documentShareRepository.save(share);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentShareDto> listShares(Long ownerId, Long documentId) {
        findOwnedDocument(ownerId, documentId);
        return documentShareRepository.findAllByDocumentId(documentId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentShareAccessLogDto> listLogs(Long ownerId, Long documentId) {
        findOwnedDocument(ownerId, documentId);
        return accessLogRepository.findAllByShareDocumentIdOrderByAccessedAtDesc(documentId)
                .stream()
                .map(this::toLogDto)
                .toList();
    }

    @Override
    public DocumentShareDto deactivateShare(Long ownerId, Long documentId, Long shareId) {
        DocumentShare share = documentShareRepository.findById(shareId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Share not found"));
        if (share.getDocument() == null || !share.getDocument().getId().equals(documentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Share not found for this document");
        }
        findOwnedDocument(ownerId, documentId);
        share.setActive(false);
        DocumentShare saved = documentShareRepository.save(share);
        return toDto(saved);
    }

    @Override
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public DocumentDownload downloadShared(String token, String password, HttpServletRequest request) {
        DocumentShare share = documentShareRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Share not found"));

        Instant now = Instant.now();
        String remoteAddr = extractRemoteAddress(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        if (!share.isActive()) {
            logAccess(share, false, "inactive", remoteAddr, userAgent);
            throw new ResponseStatusException(HttpStatus.GONE, "Share no longer active");
        }
        if (share.getStartsAt() != null && now.isBefore(share.getStartsAt())) {
            logAccess(share, false, "not_started", remoteAddr, userAgent);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Share not active yet");
        }
        if (share.getExpiresAt() != null && now.isAfter(share.getExpiresAt())) {
            logAccess(share, false, "expired", remoteAddr, userAgent);
            throw new ResponseStatusException(HttpStatus.GONE, "Share expired");
        }
        if (share.getPasswordHash() != null && !share.getPasswordHash().isBlank()) {
            if (password == null || password.isBlank() || !passwordEncoder.matches(password, share.getPasswordHash())) {
                logAccess(share, false, "invalid_password", remoteAddr, userAgent);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid password");
            }
        }

        try {
            Document doc = share.getDocument();
            StatObjectResponse metadata = s3Service.getObjectMetadata(doc.getS3Key());
            Resource resource = new ByteArrayResource(s3Service.downloadFile(doc.getS3Key()).readAllBytes());
            logAccess(share, true, null, remoteAddr, userAgent);
            return new DocumentDownload(resource, metadata.contentType(), doc.getName(), metadata.size());
        } catch (Exception e) {
            logAccess(share, false, "download_failed", remoteAddr, userAgent);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to download shared document", e);
        }
    }

    private Document findOwnedDocument(Long ownerId, Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        if (document.getOwner() == null || !document.getOwner().getId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your document");
        }
        return document;
    }

    private DocumentShareDto toDto(DocumentShare share) {
        return new DocumentShareDto(
                share.getId(),
                share.getDocument().getId(),
                share.getToken(),
                share.getStartsAt(),
                share.getExpiresAt(),
                share.isActive(),
                share.getPasswordHash() != null && !share.getPasswordHash().isBlank(),
                share.getCreatedAt()
        );
    }

    private DocumentShareAccessLogDto toLogDto(DocumentShareAccessLog log) {
        return new DocumentShareAccessLogDto(
                log.getShare().getId(),
                log.getAccessedAt(),
                log.isSuccess(),
                log.getRemoteAddress(),
                log.getUserAgent(),
                log.getReason()
        );
    }

    private void logAccess(DocumentShare share, boolean success, String reason, String remoteAddress, String userAgent) {
        DocumentShareAccessLog log = new DocumentShareAccessLog();
        log.setShare(share);
        log.setAccessedAt(Instant.now());
        log.setSuccess(success);
        log.setReason(reason);
        log.setRemoteAddress(trim(remoteAddress, 255));
        log.setUserAgent(trim(userAgent, 512));
        accessLogRepository.save(log);
    }

    private String trim(String value, int maxLen) {
        if (value == null) return null;
        if (value.length() <= maxLen) return value;
        return value.substring(0, maxLen);
    }

    private String extractRemoteAddress(HttpServletRequest request) {
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
