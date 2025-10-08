package at.technikum.swen3.exception;

public class ControllerException extends RuntimeException {
    public ControllerException(String message) {
        super(message);
    }
}