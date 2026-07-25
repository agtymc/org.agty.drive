package org.agty.drive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminUserCreateDto {
    @NotBlank(message = "Логин обязателен.")
    private String login;
    private String email;
    private String displayName;
    @NotBlank(message = "Пароль обязателен.")
    private String password;
    @NotBlank(message = "Роль обязательна.")
    private String roleCode;
    @NotBlank(message = "Статус обязателен.")
    private String statusCode;
    @NotNull(message = "Квота обязательна.")
    private Long storageQuotaMb;
}
