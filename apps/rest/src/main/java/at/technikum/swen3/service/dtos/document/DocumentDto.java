package at.technikum.swen3.service.dtos.document;

import jakarta.validation.constraints.Size;

public record DocumentDto(Long id,
                          @Size(max = 255) String name,
                          @Size(max = 255) String s3Key,
                          Long ownerId) {
}