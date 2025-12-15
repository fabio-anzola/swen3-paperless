package at.technikum.swen3.service;

import at.technikum.swen3.service.dtos.share.DocumentShareAccessLogDto;
import at.technikum.swen3.service.dtos.share.DocumentShareCreateDto;
import at.technikum.swen3.service.dtos.share.DocumentShareDto;
import at.technikum.swen3.service.model.DocumentDownload;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface IDocumentShareService {
    DocumentShareDto createShare(Long ownerId, Long documentId, DocumentShareCreateDto dto);

    List<DocumentShareDto> listShares(Long ownerId, Long documentId);

    List<DocumentShareAccessLogDto> listLogs(Long ownerId, Long documentId);

    DocumentShareDto deactivateShare(Long ownerId, Long documentId, Long shareId);

    DocumentDownload downloadShared(String token, String password, HttpServletRequest request);
}
