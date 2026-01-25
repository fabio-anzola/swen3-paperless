package at.technikum.swen3.batch.service.model;

import java.time.LocalDate;

public record ImportPayload(String content, LocalDate date, String description) {
}
