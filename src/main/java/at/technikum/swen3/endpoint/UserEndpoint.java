package at.technikum.swen3.endpoint;


import at.technikum.swen3.security.JwtUtil;
import at.technikum.swen3.service.dtos.user.UserCreateDto;
import at.technikum.swen3.service.dtos.user.UserDto;
import at.technikum.swen3.service.dtos.user.UserLoginDto;
import at.technikum.swen3.service.mapper.UserMapper;
import at.technikum.swen3.entity.User;
import at.technikum.swen3.exception.UserCreationException;
import at.technikum.swen3.service.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.lang.invoke.MethodHandles;

@RestController
@RequestMapping(value = "/api/v1/user")
public class UserEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final IUserService userService;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;


    @Autowired
    public UserEndpoint(IUserService userService, UserMapper userMapper, JwtUtil jwtUtil) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
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

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserLoginDto loginDto) {
        LOG.info("Attempting login for user: {}", loginDto.username());

        User user = userService.findByUsername(loginDto.username());

        if (!user.getPassword().equals(loginDto.password())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getUsername());
        LOG.info("User {} logged in successfully", user.getUsername());
        return ResponseEntity.ok(token);
    }

    @DeleteMapping("/{userId}")
    public UserDto deleteUser(@PathVariable Long userId) {
        LOG.info("Removing user with ID {}", userId);
        User deletedUser = userService.deleteUser(userId);
        LOG.info("Removed User {} successfully", userId);
        return userMapper.userToUserDto(deletedUser);
    }

    @GetMapping("/whoami")
    public UserDto whoami(Authentication authentication) {
        String username = authentication.getName();
        LOG.info("Fetching details for user: {}", username);
        User user = userService.findByUsername(username);
        return userMapper.userToUserDto(user);
    }
}
