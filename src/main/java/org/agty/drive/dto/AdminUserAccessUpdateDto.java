package org.agty.drive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminUserAccessUpdateDto {
    @NotNull(message = "Пользователь не выбран.")
    private Long userId;
    @NotBlank(message = "Роль обязательна.")
    private String roleCode;
    @NotBlank(message = "Статус обязателен.")
    private String statusCode;
    @NotNull(message = "Квота обязательна.")
    private Long storageQuotaMb;
}
