package at.technikum.swen3.service.dtos.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserLoginDto(
        @NotNull @NotBlank @Size(min = 3, max = 50)
        String username,

        @NotNull @NotBlank @Size(min = 3)
        String password
) {}