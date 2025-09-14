package at.technikum.swen3.repository;

import at.technikum.swen3.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  User save(User user);

  void deleteById(Long id);

  Optional<User> findById(Long id);
}
