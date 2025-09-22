package at.technikum.swen3;

import at.technikum.swen3.entity.Document;
import at.technikum.swen3.entity.User;
import at.technikum.swen3.service.dtos.document.DocumentDto;
import at.technikum.swen3.service.dtos.document.DocumentUploadDto;
import at.technikum.swen3.service.mapper.DocumentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class DocumentMapperTest {

    private DocumentMapper documentMapper;

    @BeforeEach
    void setUp() {
        documentMapper = Mappers.getMapper(DocumentMapper.class);
    }

    @Test
    void toDto_shouldMapEntityToDto() {
        // Arrange
        User owner = new User();
        owner.setId(1L);
        Document document = new Document();
        document.setId(100L);
        document.setName("Test Document");
        document.setOwner(owner);

        // Act
        DocumentDto documentDto = documentMapper.toDto(document);

        // Assert
        assertNotNull(documentDto);
        assertEquals(1L, documentDto.ownerId());
        assertEquals("Test Document", documentDto.name());
    }

    @Test
    void updateEntityFromUpload_shouldUpdateEntityFields() {
        // Arrange
        DocumentUploadDto uploadDto = new DocumentUploadDto("Updated Name");
        Document document = new Document();
        document.setName("Old Name");

        // Act
        documentMapper.updateEntityFromUpload(uploadDto, document);

        // Assert
        assertEquals("Updated Name", document.getName());
    }
}
