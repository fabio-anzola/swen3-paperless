package at.technikum.swen3.worker.dto;

public class OcrTopicMessageDto {
  private String message;

  public OcrTopicMessageDto() {}

  public OcrTopicMessageDto(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return "OcrTopicMessageDto{message='" + message + "'}";
  }
}
