package at.technikum.swen3;

import at.technikum.swen3.entity.User;
import at.technikum.swen3.repository.UserRepository;
import at.technikum.swen3.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void registerUser_shouldSaveAndReturnUser() {
        User user = new User();
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.registerUser(user);

        assertEquals(user, result);
        verify(userRepository).save(user);
    }

    @Test
    void deleteUser_shouldDeleteAndReturnUser_whenUserExists() {
        Long userId = 1L;
        User user = new User();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.deleteUser(userId);

        assertEquals(user, result);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_shouldThrowException_whenUserNotFound() {
        Long userId = 2L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.deleteUser(userId));
        verify(userRepository, never()).delete(any());
    }
}