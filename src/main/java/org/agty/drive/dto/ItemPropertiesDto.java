package org.agty.drive.dto;

import lombok.Data;

@Data
public class ItemPropertiesDto {
    private String resourceType;
    private Long resourceId;
    private String expiresAt;
    private Boolean autoDelete;
}
