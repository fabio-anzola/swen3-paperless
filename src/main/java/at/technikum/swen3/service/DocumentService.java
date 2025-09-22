package at.technikum.swen3.service;

import at.technikum.swen3.entity.Document;
import at.technikum.swen3.entity.User;
import at.technikum.swen3.repository.DocumentRepository;
import at.technikum.swen3.repository.UserRepository;
import at.technikum.swen3.service.dtos.document.DocumentDto;
import at.technikum.swen3.service.dtos.document.DocumentUploadDto;
import at.technikum.swen3.service.mapper.DocumentMapper;
import at.technikum.swen3.service.model.DocumentDownload;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class DocumentService implements IDocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentMapper documentMapper;

    public DocumentService(DocumentRepository documentRepository, UserRepository userRepository, DocumentMapper documentMapper) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.documentMapper = documentMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentDto> listMine(Long userId, Pageable pageable) {
        return documentRepository.findAllByOwnerId(userId, pageable).map(documentMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDto getMeta(Long userId, Long id) {
        Document d = documentRepository.findById(id).orElseThrow(this::notFound);
        enforceOwner(userId, d);
        return documentMapper.toDto(d);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDownload download(Long userId, Long id) {
        Document d = documentRepository.findById(id).orElseThrow(this::notFound);
        enforceOwner(userId, d);

        // TODO: replace with actual S3 download later
        Resource dummy = new ByteArrayResource("Not yet implemented".getBytes());
        return new DocumentDownload(dummy, "text/plain", d.getName(), (long) "Not yet implemented".length());
    }

    @Override
    public DocumentDto upload(Long userId, MultipartFile file, DocumentUploadDto meta) {
        if (file == null || file.isEmpty()) throw badRequest("File is required");

        User owner = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Document d = new Document();
        d.setOwner(owner);
        d.setName(meta != null && meta.name() != null ? meta.name() : file.getOriginalFilename());

        // TODO: upload file to S3 and set generated key
        // TODO: load key from properties
        d.setS3Key("TODO-S3KEY");

        d = documentRepository.save(d);
        return documentMapper.toDto(d);
    }

    @Override
    public DocumentDto updateMeta(Long userId, Long id, DocumentUploadDto meta) {
        Document d = documentRepository.findById(id).orElseThrow(this::notFound);
        enforceOwner(userId, d);
        if (meta != null) documentMapper.updateEntityFromUpload(meta, d);
        return documentMapper.toDto(d);
    }

    @Override
    public void delete(Long userId, Long id) {
        Document d = documentRepository.findById(id).orElseThrow(this::notFound);
        enforceOwner(userId, d);

        // TODO: delete object from S3

        documentRepository.delete(d);
    }

    private void enforceOwner(Long userId, Document d) {
        if (d.getOwner() == null || !d.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your document");
        }
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
    }

    private ResponseStatusException badRequest(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }
}