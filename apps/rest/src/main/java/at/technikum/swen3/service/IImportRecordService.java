package at.technikum.swen3.service;

import at.technikum.swen3.service.dtos.imports.ImportRecordDto;
import at.technikum.swen3.service.dtos.imports.ImportRequestDto;

public interface IImportRecordService {
    ImportRecordDto create(ImportRequestDto requestDto);
}
