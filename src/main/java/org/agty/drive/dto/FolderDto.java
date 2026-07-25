package org.agty.drive.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.agty.drive.config.AppTime;

@Data
public class FolderDto {
    private Long id;
    private Long ownerId;
    private Long parentId;

    @NotBlank(message = "Название папки обязательно")
    private String name;

    private String pathKey;
    private String description;
    private Integer sortOrder;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

    public String getCreatedAtTitle() {
        return convertDateTime(createdAt);
    }

    public String getUpdatedAtTitle() {
        return convertDateTime(updatedAt);
    }

    private String convertDateTime(String value) {
        return AppTime.formatForTitle(value, "dd.MM.yyyy HH:mm");
    }
}
