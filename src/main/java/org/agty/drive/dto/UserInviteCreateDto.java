package org.agty.drive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserInviteCreateDto {
    @NotBlank(message = "Логин обязателен.")
    private String login;
    private String email;
    private String displayName;
    @NotBlank(message = "Роль обязательна.")
    private String roleCode;
    @NotBlank(message = "Статус обязателен.")
    private String statusCode;
    @NotNull(message = "Квота обязательна.")
    private Long storageQuotaMb;
    private Integer expiresInHours;
}
