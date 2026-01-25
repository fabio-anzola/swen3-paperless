package at.technikum.swen3.service.dtos.imports;

import java.time.Instant;
import java.time.LocalDate;

public record ImportRecordDto(
        Long id,
        String content,
        LocalDate date,
        String description,
        Instant createdAt) {
}
