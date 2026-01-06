package at.technikum.swen3.service.dtos.share;

import java.time.Instant;

public record DocumentShareDto(Long id,
                               Long documentId,
                               String token,
                               Instant startsAt,
                               Instant expiresAt,
                               boolean active,
                               boolean passwordProtected,
                               Instant createdAt) {
}
