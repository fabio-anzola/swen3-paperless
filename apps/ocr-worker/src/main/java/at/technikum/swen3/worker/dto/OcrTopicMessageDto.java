package at.technikum.swen3.worker.dto;

public class OcrTopicMessageDto {
  private String s3Key;
  private String fileName;

  public OcrTopicMessageDto() {}

  public OcrTopicMessageDto(String s3Key, String fileName) {
    this.s3Key = s3Key;
    this.fileName = fileName;
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
    return "OcrTopicMessageDto{s3Key='" + s3Key + "', fileName='" + fileName + "'}";
  }
}
