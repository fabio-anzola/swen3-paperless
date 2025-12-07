package at.technikum.swen3.service.dtos;

public record GenAiResultMessageDto(String processedMessage, String summary, String s3Key, String elasticId, String fileName) { }
