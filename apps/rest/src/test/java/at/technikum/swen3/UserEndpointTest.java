package at.technikum.swen3;

import at.technikum.swen3.service.dtos.user.UserCreateDto;
import at.technikum.swen3.service.dtos.user.UserDto;
import at.technikum.swen3.service.dtos.user.UserLoginDto;
import at.technikum.swen3.endpoint.UserEndpoint;
import at.technikum.swen3.service.mapper.UserMapper;
import at.technikum.swen3.entity.User;
import at.technikum.swen3.exception.UserCreationException;
import at.technikum.swen3.exception.UserNotFoundException;
import at.technikum.swen3.exception.ServiceException;
import at.technikum.swen3.service.IUserService;
import at.technikum.swen3.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEndpointTest {

    @Mock
    private IUserService userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserEndpoint userEndpoint;

    @Test
    void createUser_shouldReturnUserDto_whenUserCreatedSuccessfully() {
        UserCreateDto userCreateDto = mock(UserCreateDto.class);
        when(userCreateDto.username()).thenReturn("testuser");
        User user = new User();
        User createdUser = new User();
        createdUser.setId(1L);
        UserDto userDto = mock(UserDto.class);

        when(userService.existsByUsername("testuser")).thenReturn(false);
        when(userMapper.userCreateDtoToUser(userCreateDto)).thenReturn(user);
        when(userService.registerUser(user)).thenReturn(createdUser);
        when(userMapper.userToUserDto(createdUser)).thenReturn(userDto);

        UserDto result = userEndpoint.createUser(userCreateDto);

        assertEquals(userDto, result);
        verify(userService).existsByUsername("testuser");
        verify(userMapper).userCreateDtoToUser(userCreateDto);
        verify(userService).registerUser(user);
        verify(userMapper).userToUserDto(createdUser);
    }

    @Test
    void createUser_shouldThrowUserCreationException_whenUserAlreadyExists() {
        UserCreateDto userCreateDto = mock(UserCreateDto.class);
        when(userCreateDto.username()).thenReturn("exists");
        when(userService.existsByUsername("exists")).thenReturn(true);

        UserCreationException exception = assertThrows(
                UserCreationException.class,
                () -> userEndpoint.createUser(userCreateDto)
        );

        assertTrue(exception.getMessage().contains("user already exists"));
        verify(userService).existsByUsername("exists");
        verify(userMapper, never()).userCreateDtoToUser(any());
    }

    @Test
    void createUser_shouldThrowUserCreationException_whenExceptionOccurs() {
        UserCreateDto userCreateDto = mock(UserCreateDto.class);
        when(userCreateDto.username()).thenReturn("failuser");
        when(userMapper.userCreateDtoToUser(userCreateDto)).thenThrow(new RuntimeException("Mapping failed"));

        UserCreationException exception = assertThrows(
                UserCreationException.class,
                () -> userEndpoint.createUser(userCreateDto)
        );

        assertTrue(exception.getMessage().contains("Could not create user"));
        verify(userMapper).userCreateDtoToUser(userCreateDto);
        verify(userService, never()).registerUser(any());
    }

    @Test
    void login_shouldReturnToken_whenCredentialsValid() {
        UserLoginDto loginDto = mock(UserLoginDto.class);
        when(loginDto.username()).thenReturn("loginUser");
        when(loginDto.password()).thenReturn("secret");

        User user = new User();
        user.setUsername("loginUser");
        user.setPassword("secret");

        when(userService.findByUsername("loginUser")).thenReturn(user);
        when(jwtUtil.generateToken("loginUser")).thenReturn("jwt-token");

        var response = userEndpoint.login(loginDto);

        assertEquals("jwt-token", response.getBody());
        verify(userService).findByUsername("loginUser");
        verify(jwtUtil).generateToken("loginUser");
    }

    @Test
    void login_shouldThrowServiceException_whenInvalidCredentials() {
        UserLoginDto loginDto = mock(UserLoginDto.class);
        when(loginDto.username()).thenReturn("loginUser");
        when(loginDto.password()).thenReturn("wrong");

        User user = new User();
        user.setUsername("loginUser");
        user.setPassword("real");

        when(userService.findByUsername("loginUser")).thenReturn(user);

        ServiceException ex = assertThrows(ServiceException.class, () -> userEndpoint.login(loginDto));
        assertTrue(ex.getMessage().contains("Invalid credentials"));
        verify(userService).findByUsername("loginUser");
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void deleteUser_shouldReturnUserDto_whenDeletedSuccessfully() {
        Long userId = 1L;
        User deletedUser = new User();
        deletedUser.setId(userId);
        UserDto dto = mock(UserDto.class);

        when(userService.deleteUser(userId)).thenReturn(deletedUser);
        when(userMapper.userToUserDto(deletedUser)).thenReturn(dto);

        UserDto result = userEndpoint.deleteUser(userId);

        assertEquals(dto, result);
        verify(userService).deleteUser(userId);
        verify(userMapper).userToUserDto(deletedUser);
    }

    @Test
    void deleteUser_shouldThrowUserNotFoundException_whenNotFound() {
        Long userId = 2L;
        when(userService.deleteUser(userId)).thenReturn(null);

        UserNotFoundException ex = assertThrows(UserNotFoundException.class, () -> userEndpoint.deleteUser(userId));
        assertTrue(ex.getMessage().contains(String.valueOf(userId)));
        verify(userService).deleteUser(userId);
        verify(userMapper, never()).userToUserDto(any());
    }

    @Test
    void whoami_shouldReturnUserDto_whenUserExists() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("whoamiUser");

        User user = new User();
        user.setUsername("whoamiUser");
        UserDto dto = mock(UserDto.class);

        when(userService.findByUsername("whoamiUser")).thenReturn(user);
        when(userMapper.userToUserDto(user)).thenReturn(dto);

        UserDto result = userEndpoint.whoami(auth);

        assertEquals(dto, result);
        verify(userService).findByUsername("whoamiUser");
        verify(userMapper).userToUserDto(user);
    }

    @Test
    void whoami_shouldThrowUserNotFoundException_whenUserMissing() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("missingUser");

        when(userService.findByUsername("missingUser")).thenReturn(null);

        UserNotFoundException ex = assertThrows(UserNotFoundException.class, () -> userEndpoint.whoami(auth));
        assertTrue(ex.getMessage().contains("missingUser"));
        verify(userService).findByUsername("missingUser");
    }
}
