package at.technikum.swen3.exception;

public class UserCreationException extends RuntimeException {
  public UserCreationException(String message, Throwable cause) {
    super(message, cause);
  }
}