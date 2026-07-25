package org.agty.drive.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InviteAcceptDto {
    @NotBlank(message = "Пароль обязателен.")
    private String password;
    @NotBlank(message = "Подтверждение пароля обязательно.")
    private String confirmPassword;
}
