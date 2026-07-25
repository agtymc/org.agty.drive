package org.agty.drive.dto;

import lombok.Data;
import org.agty.drive.config.AppTime;

@Data
public class ShareLinkDto {
    private Long id;
    private Long createdBy;
    private String token;
    private String resourceType;
    private Long resourceId;
    private String title;
    private String passwordHash;
    private String expiresAt;
    private Boolean allowDownload;
    private Boolean allowPreview;
    private Boolean isEnabled;
    private Long maxDownloads;
    private Long downloadCount;
    private String createdAt;
    private String updatedAt;

    public String getExpiresAtTitle() {
        if (expiresAt == null || expiresAt.isBlank()) {
            return "Без срока";
        }
        return AppTime.formatForTitle(expiresAt, "dd.MM.yyyy HH:mm");
    }
}
