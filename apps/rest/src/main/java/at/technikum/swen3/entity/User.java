package at.technikum.swen3.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @NotNull
  @NotBlank
  @Size(min = 3, max = 50)
  @Column(nullable = false, unique = true)
  private String username;

  @NotNull
  @NotBlank
  @Size(min = 3)
  @Column(nullable = false)
  private String password;
}
