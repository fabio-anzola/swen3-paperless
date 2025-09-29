package at.technikum.swen3.endpoint.exceptionhandler;

public record ErrorResponse(
    int status,
    String error,
    String message,
    String path
) {
}
