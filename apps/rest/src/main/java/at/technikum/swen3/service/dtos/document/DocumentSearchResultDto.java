package at.technikum.swen3.service.dtos.document;

public record DocumentSearchResultDto(
        Long id,
        String name,
        String summary,
        Long ownerId,
        String s3Key,
        Double score) {
}
