package at.technikum.swen3.gemini.dto;

public record OcrResultMessage(String processedMessage, String s3Key, String fileName) { }
