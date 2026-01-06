package at.technikum.swen3.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import at.technikum.swen3.entity.Document;
import at.technikum.swen3.entity.User;
import at.technikum.swen3.kafka.KafkaProducerService;
import at.technikum.swen3.repository.DocumentRepository;
import at.technikum.swen3.repository.UserRepository;
import at.technikum.swen3.service.dtos.OcrTopicMessageDto;
import at.technikum.swen3.service.dtos.document.DocumentDto;
import at.technikum.swen3.service.dtos.document.DocumentUploadDto;
import at.technikum.swen3.service.mapper.DocumentMapper;
import at.technikum.swen3.service.model.DocumentDownload;
import io.minio.StatObjectResponse;

@Service
@Transactional
public class DocumentService implements IDocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentMapper documentMapper;
    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper;
    private final S3Service s3Service;
    @Value("${kafka.topic.ocr}")
    private String ocrTopic;


    public DocumentService(DocumentRepository documentRepository, UserRepository userRepository, DocumentMapper documentMapper, KafkaProducerService kafkaProducerService, ObjectMapper objectMapper, S3Service s3Service) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.documentMapper = documentMapper;
        this.kafkaProducerService = kafkaProducerService;
        this.objectMapper = objectMapper;
        this.s3Service = s3Service;
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

        try {
            StatObjectResponse metadata = s3Service.getObjectMetadata(d.getS3Key());
            Resource resource = new ByteArrayResource(s3Service.downloadFile(d.getS3Key()).readAllBytes());
            return new DocumentDownload(resource, metadata.contentType(), d.getName(), metadata.size());
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }

    @Override
    public DocumentDto upload(Long userId, MultipartFile file, DocumentUploadDto meta) {
        if (file == null || file.isEmpty()) throw badRequest("File is required");

        User owner = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Document d = new Document();
        d.setOwner(owner);
        d.setName(meta != null && meta.name() != null ? meta.name() : file.getOriginalFilename());

        String s3Key = s3Service.uploadFile(file);
        d.setS3Key(s3Key);

        d = documentRepository.save(d);

        try {
            kafkaProducerService.sendMessage(ocrTopic, objectMapper.writeValueAsString(new OcrTopicMessageDto(d.getS3Key(), d.getName())));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

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

        s3Service.deleteFile(d.getS3Key());

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
