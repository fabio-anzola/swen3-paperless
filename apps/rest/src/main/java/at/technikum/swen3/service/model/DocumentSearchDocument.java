package at.technikum.swen3.service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "documents")
public class DocumentSearchDocument {
    @Id
    private Long id;
    private Long ownerId;
    private String name;
    private String s3Key;
    private String summary;
    private String content;

    public DocumentSearchDocument() {
    }

    public DocumentSearchDocument(Long id, Long ownerId, String name, String s3Key, String summary, String content) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.s3Key = s3Key;
        this.summary = summary;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
