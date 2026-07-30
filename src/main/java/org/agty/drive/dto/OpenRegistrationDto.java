package org.agty.drive.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OpenRegistrationDto {
    @NotBlank(message = "Логин обязателен.")
    private String login;
    private String email;
    private String displayName;
    @NotBlank(message = "Пароль обязателен.")
    private String password;
    @NotBlank(message = "Подтверждение пароля обязательно.")
    private String confirmPassword;
}
