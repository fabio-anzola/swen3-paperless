package at.technikum.swen3.endpoint;

import at.technikum.swen3.services.dtos.user.UserCreateDto;
import at.technikum.swen3.services.dtos.user.UserDto;
import at.technikum.swen3.services.mapper.UserMapper;
import at.technikum.swen3.entities.User;
import at.technikum.swen3.exception.UserCreationException;
import at.technikum.swen3.services.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.lang.invoke.MethodHandles;

@RestController
@RequestMapping(value = "/api/v1/user")
public class UserEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IUserService userService;
    private final UserMapper userMapper;

    @Autowired
    public UserEndpoint(IUserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @PostMapping("/register")
    public UserDto createUser(@RequestBody UserCreateDto user) {
        LOG.info("Creating new user: {}", user.username());

        try {
            User userDto = userMapper.userCreateDtoToUser(user);
            User createdUser = userService.registerUser(userDto);
            LOG.info("User created successfully with ID: {}", createdUser.getId());
            return userMapper.userToUserDto(createdUser);
        } catch (Exception e) {
            LOG.error("Failed to create user {}: {}", user.username(), e.getMessage());
            throw new UserCreationException("Could not create user " + user.username(), e);
        }
    }

    @DeleteMapping("/{userId}")
    public void deleteUser(@PathVariable Long userId) {
        LOG.info("Removing user with ID {}", userId);
        userService.deleteUser(userId);
        LOG.info("Removed User {} successfully", userId);
    }
}
