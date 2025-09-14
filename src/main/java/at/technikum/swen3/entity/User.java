package at.technikum.swen3.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Size(min = 2, max = 100)
  @Column(nullable = false, unique = true, length = 100)
  private String username;

  @NotNull
  @Size(min = 5, max = 100)
  @Column(nullable = false, length = 100)
  private String password;
}
