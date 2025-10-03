package at.technikum.swen3.worker.dto;

public class ResultTopicMessageDto {
  private String processedMessage;

  public ResultTopicMessageDto() {}

  public ResultTopicMessageDto(String processedMessage) {
    this.processedMessage = processedMessage;
  }

  public String getProcessedMessage() {
    return processedMessage;
  }

  public void setProcessedMessage(String processedMessage) {
    this.processedMessage = processedMessage;
  }

  @Override
  public String toString() {
    return "ResultTopicMessageDto{processedMessage='" + processedMessage + "'}";
  }
}
