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
}