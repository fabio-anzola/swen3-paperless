package at.technikum.swen3.exception;

public class UserCreationException extends RuntimeException {

  public UserCreationException() {
  }

  public UserCreationException(String message) {
    super(message);
  }

  public UserCreationException(String message, Throwable cause) {
    super(message, cause);
  }

  public UserCreationException(Exception e) {super(e); }
}