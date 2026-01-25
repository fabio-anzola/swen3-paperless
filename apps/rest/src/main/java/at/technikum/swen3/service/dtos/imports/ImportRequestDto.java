package at.technikum.swen3.service.dtos.imports;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ImportRequestDto(
        @NotBlank String content,
        @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        @NotBlank @Size(max = 1024) String description) {
}
