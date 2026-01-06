package at.technikum.swen3.worker.dto;

public class ResultTopicMessageDto {
  private String processedMessage;
  private String s3Key;
  private String fileName;

  public ResultTopicMessageDto() {}

  public ResultTopicMessageDto(String processedMessage, String s3Key, String fileName) {
    this.processedMessage = processedMessage;
    this.s3Key = s3Key;
    this.fileName = fileName;
  }

  public String getProcessedMessage() {
    return processedMessage;
  }

  public void setProcessedMessage(String processedMessage) {
    this.processedMessage = processedMessage;
  }

  public String getS3Key() {
    return s3Key;
  }

  public void setS3Key(String s3Key) {
    this.s3Key = s3Key;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  @Override
  public String toString() {
    return "ResultTopicMessageDto{processedMessage='" + processedMessage + "', s3Key='" + s3Key + "', fileName='" + fileName + "'}";
  }
}
