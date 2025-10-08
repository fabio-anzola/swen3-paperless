package at.technikum.swen3.service.dtos.document;

import jakarta.validation.constraints.Size;

public record DocumentUploadDto(
        @Size(max = 255) String name) {
}