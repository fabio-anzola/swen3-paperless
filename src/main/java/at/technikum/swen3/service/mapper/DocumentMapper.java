package at.technikum.swen3.service.mapper;

import at.technikum.swen3.entity.Document;
import at.technikum.swen3.entity.User;
import at.technikum.swen3.service.dtos.document.DocumentDto;
import at.technikum.swen3.service.dtos.document.DocumentUploadDto;
import at.technikum.swen3.service.dtos.user.UserCreateDto;
import at.technikum.swen3.service.dtos.user.UserDto;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(source = "owner.id", target = "ownerId")
    DocumentDto toDto(Document doc);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name") //only let name to be updated
    void updateEntityFromUpload(DocumentUploadDto dto, @MappingTarget Document entity);
}
