package at.technikum.swen3.service.dtos.document;

public record DocumentDto(Long id, String name, String s3Key, Long ownerId) {
}