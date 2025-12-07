package at.technikum.swen3.service;

import at.technikum.swen3.service.dtos.document.DocumentSearchResultDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IDocumentSearchService {
    Page<DocumentSearchResultDto> search(Long userId, String query, Pageable pageable);
}
