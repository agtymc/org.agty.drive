package org.agty.drive.dto;

import lombok.Data;

@Data
public class AppSettingDto {
    private Long id;
    private String createdAt;
    private String updatedAt;
    private String settingKey;
    private String settingValue;
    private Long updatedBy;
}
