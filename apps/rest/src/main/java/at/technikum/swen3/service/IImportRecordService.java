package at.technikum.swen3.service;

import at.technikum.swen3.service.dtos.imports.ImportRecordDto;
import at.technikum.swen3.service.dtos.imports.ImportRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IImportRecordService {
    ImportRecordDto create(ImportRequestDto requestDto);

    Page<ImportRecordDto> list(Pageable pageable);
}
