package at.technikum.swen3.repository;

import at.technikum.swen3.entity.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository {
  User save(User user);

  void deleteById(Long id);

  User findById(Long id);
}
