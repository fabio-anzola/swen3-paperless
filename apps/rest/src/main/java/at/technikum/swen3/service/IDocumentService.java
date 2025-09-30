package at.technikum.swen3.service;

import at.technikum.swen3.service.dtos.document.DocumentDto;
import at.technikum.swen3.service.dtos.document.DocumentUploadDto;
import at.technikum.swen3.service.model.DocumentDownload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface IDocumentService {
    Page<DocumentDto> listMine(Long userId, Pageable pageable);

    DocumentDto getMeta(Long userId, Long id);

    DocumentDownload download(Long userId, Long id);

    DocumentDto upload(Long userId, MultipartFile file, DocumentUploadDto meta);

    DocumentDto updateMeta(Long userId, Long id, DocumentUploadDto meta);

    void delete(Long userId, Long id);
}