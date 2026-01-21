package at.technikum.swen3.endpoint;

import at.technikum.swen3.service.IImportRecordService;
import at.technikum.swen3.service.dtos.imports.ImportRecordDto;
import at.technikum.swen3.service.dtos.imports.ImportRequestDto;
import jakarta.validation.Valid;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/paperless/import")
public class ImportEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IImportRecordService importRecordService;

    public ImportEndpoint(IImportRecordService importRecordService) {
        this.importRecordService = importRecordService;
    }

    @PostMapping
    public ResponseEntity<ImportRecordDto> importRecord(@Valid @RequestBody ImportRequestDto requestDto) {
        LOG.info("Received import request for date={}", requestDto.date());
        ImportRecordDto saved = importRecordService.create(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
