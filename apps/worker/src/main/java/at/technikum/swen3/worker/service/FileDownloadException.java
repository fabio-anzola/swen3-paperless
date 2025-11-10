package at.technikum.swen3.worker.service;

public class FileDownloadException extends Exception {
    public FileDownloadException(String message) {
        super(message);
    }

    public FileDownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
