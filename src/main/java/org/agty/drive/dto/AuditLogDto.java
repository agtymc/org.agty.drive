package org.agty.drive.dto;

import lombok.Data;
import org.agty.drive.config.AppTime;

@Data
public class AuditLogDto {
    private Long id;
    private String createdAt;
    private Long actorUserId;
    private String actorLogin;
    private String actionCode;
    private String resourceType;
    private Long resourceId;
    private String details;

    public String getCreatedAtTitle() {
        return AppTime.formatForTitle(createdAt, "dd.MM.yyyy HH:mm:ss");
    }
}
