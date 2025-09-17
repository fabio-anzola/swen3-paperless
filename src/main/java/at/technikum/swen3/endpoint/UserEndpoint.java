package at.technikum.swen3.endpoint;

import at.technikum.swen3.dto.user.*;
import at.technikum.swen3.entity.User;
import at.technikum.swen3.endpoint.mapper.UserMapper;
import at.technikum.swen3.exception.UserCreationException;
import at.technikum.swen3.repository.UserRepository;
import at.technikum.swen3.security.JwtUtil;
import at.technikum.swen3.service.IUserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.invoke.MethodHandles;

@RestController
@RequestMapping(value = "/api/v1/user")
public class UserEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final IUserService userService;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Autowired
    public UserEndpoint(IUserService userService, UserMapper userMapper,
                        UserRepository userRepository, JwtUtil jwtUtil) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public UserDto createUser(@RequestBody UserCreateDto user) {
        LOG.info("Creating new user: {}", user.username());

        try {
            User userEntity = userMapper.userCreateDtoToUser(user);
            var createdUser = userService.registerUser(userEntity);
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

        User user = userRepository.findByUsername(loginDto.username())
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!user.getPassword().equals(loginDto.password())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getUsername());
        LOG.info("User {} logged in successfully", user.getUsername());
        return ResponseEntity.ok(token);
    }
}
