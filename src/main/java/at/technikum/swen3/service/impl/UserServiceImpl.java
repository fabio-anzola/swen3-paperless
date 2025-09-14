package at.technikum.swen3.service.impl;

import at.technikum.swen3.entity.User;
import at.technikum.swen3.repository.UserRepository;
import at.technikum.swen3.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;

  @Autowired
  public UserServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public User registerUser(User user) {
   return userRepository.save(user);
  }

  @Override
  public User deleteUser(Long id) {
    var deleteUser = userRepository.findById(id);
    userRepository.deleteById(id);
    return deleteUser;
  }
}
