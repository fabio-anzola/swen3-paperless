package at.technikum.swen3;

import at.technikum.swen3.service.dtos.user.UserCreateDto;
import at.technikum.swen3.service.dtos.user.UserDto;
import at.technikum.swen3.endpoint.UserEndpoint;
import at.technikum.swen3.service.mapper.UserMapper;
import at.technikum.swen3.entity.User;
import at.technikum.swen3.exception.UserCreationException;
import at.technikum.swen3.service.IUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEndpointTest {

    @Mock
    private IUserService userService;

    @Mock
    private UserMapper userMapper;

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

        when(userMapper.userCreateDtoToUser(userCreateDto)).thenReturn(user);
        when(userService.registerUser(user)).thenReturn(createdUser);
        when(userMapper.userToUserDto(createdUser)).thenReturn(userDto);

        UserDto result = userEndpoint.createUser(userCreateDto);

        assertEquals(userDto, result);
        verify(userMapper).userCreateDtoToUser(userCreateDto);
        verify(userService).registerUser(user);
        verify(userMapper).userToUserDto(createdUser);
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
}