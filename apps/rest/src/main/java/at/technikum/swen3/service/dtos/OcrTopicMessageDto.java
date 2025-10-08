package at.technikum.swen3.service.dtos;

public class OcrTopicMessageDto {
  private String s3Key;

  public OcrTopicMessageDto() {}

  public OcrTopicMessageDto(String s3Key) {
    this.s3Key = s3Key;
  }

  public String getS3Key() {
    return s3Key;
  }

  public void setS3Key(String s3Key) {
    this.s3Key = s3Key;
  }

  @Override
  public String toString() {
    return "OcrTopicMessageDto{s3Key='" + s3Key + "'}";
  }
}
