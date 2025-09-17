package at.technikum.swen3.services;

import at.technikum.swen3.entities.User;

public interface IUserService {
  User registerUser(User user);
  User deleteUser(Long id);
}
