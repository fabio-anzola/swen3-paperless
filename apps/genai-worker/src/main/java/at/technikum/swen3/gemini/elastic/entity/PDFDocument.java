package at.technikum.swen3.gemini.elastic.entity;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "pdfdocuments")
public record PDFDocument(
    @Id String id,
    @Field(type = FieldType.Keyword) String fileName,
    @Field(type = FieldType.Text) String textContent,
    @Field(type = FieldType.Text) String summary,
    @Field(type = FieldType.Date) Instant indexedAt
) {
}
