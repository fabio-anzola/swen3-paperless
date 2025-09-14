package at.technikum.swen3.service;

import at.technikum.swen3.entity.User;

public interface UserService {
  User registerUser(User user);
  User deleteUser(Long id);
}
