package org.agty.drive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ItemMoveDto {
    @NotBlank(message = "Тип объекта обязателен.")
    private String resourceType;

    @NotNull(message = "Объект для перемещения не выбран.")
    private Long resourceId;

    private Long targetFolderId;
}
