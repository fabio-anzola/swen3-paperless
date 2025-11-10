package at.technikum.swen3.service;

import at.technikum.swen3.entity.User;
import at.technikum.swen3.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements IUserService {
  private final UserRepository userRepository;

  @Autowired
  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public User registerUser(User user) {
   return userRepository.save(user);
  }

  @Override
  @Transactional
  public User deleteUser(Long id) {
    User existing = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User " + id + " not found"));
    userRepository.delete(existing);
    return existing;
  }

  @Override
  public User findByUsername(String username) {
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new EntityNotFoundException("User with username " + username + " not found"));
  }

  @Override
  public boolean existsByUsername(String username) {
    return userRepository.findByUsername(username).isPresent();
  }

}
