package at.technikum.swen3.services.mapper;

import at.technikum.swen3.services.dtos.user.UserCreateDto;
import at.technikum.swen3.services.dtos.user.UserDto;
import at.technikum.swen3.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
  @Mapping(target = "id", ignore = true)
  User userCreateDtoToUser(UserCreateDto dto);

  UserDto userToUserDto(User user);
}
