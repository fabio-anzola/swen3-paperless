package at.technikum.swen3.service.dtos.share;

import java.time.Instant;

public record DocumentShareAccessLogDto(Long shareId,
                                        Instant accessedAt,
                                        boolean success,
                                        String remoteAddress,
                                        String userAgent,
                                        String reason) {
}
