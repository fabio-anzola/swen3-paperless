package at.technikum.swen3.service.mapper;

import at.technikum.swen3.service.dtos.user.UserCreateDto;
import at.technikum.swen3.service.dtos.user.UserDto;
import at.technikum.swen3.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
  @Mapping(target = "id", ignore = true)
  User userCreateDtoToUser(UserCreateDto dto);

  UserDto userToUserDto(User user);
}
