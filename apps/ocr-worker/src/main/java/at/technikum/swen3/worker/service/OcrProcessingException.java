package at.technikum.swen3.worker.service;

public class OcrProcessingException extends Exception {
    public OcrProcessingException(String message) {
        super(message);
    }

    public OcrProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
