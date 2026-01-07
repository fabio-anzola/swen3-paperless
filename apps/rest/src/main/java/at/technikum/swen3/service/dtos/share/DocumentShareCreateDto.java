package at.technikum.swen3.service.dtos.share;

import java.time.Instant;

public record DocumentShareCreateDto(String password,
                                     Instant startsAt,
                                     Instant expiresAt) {
}
