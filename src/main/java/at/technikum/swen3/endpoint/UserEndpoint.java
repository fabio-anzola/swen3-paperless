package at.technikum.swen3.endpoint;

import at.technikum.swen3.dto.user.UserCreateDto;
import at.technikum.swen3.dto.user.UserDto;
import at.technikum.swen3.endpoint.mapper.UserMapper;
import at.technikum.swen3.exception.UserCreationException;
import at.technikum.swen3.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;

@RestController
@RequestMapping(value = "/api/v1/user")
public class UserEndpoint {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final UserService userService;
  private final UserMapper userMapper;

  @Autowired
  public UserEndpoint(UserService userService, UserMapper userMapper) {
    this.userService = userService;
    this.userMapper = userMapper;
  }

  @PostMapping("/register")
  public UserDto createUser(@RequestBody UserCreateDto user) {
    LOG.info("Creating new user: {}", user.username());

    try {
      var createdUser = userService.registerUser(userMapper.userCreateDtoToUser(user));
      LOG.info("User created successfully with ID: {}", createdUser.getId());
      return userMapper.userToUserDto(createdUser);
    } catch (Exception e) {
      LOG.error("Failed to create user {}: {}", user.username(), e.getMessage());
      throw new UserCreationException("Could not create user " + user.username(), e);
    }
  }
}
