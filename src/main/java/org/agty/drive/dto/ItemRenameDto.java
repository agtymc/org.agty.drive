package org.agty.drive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ItemRenameDto {
    @NotBlank(message = "Тип объекта обязателен.")
    private String resourceType;

    @NotNull(message = "Объект для переименования не выбран.")
    private Long resourceId;

    @NotBlank(message = "Введите новое название.")
    private String newName;
}
