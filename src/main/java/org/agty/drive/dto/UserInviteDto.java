package org.agty.drive.dto;

import lombok.Data;
import org.agty.drive.config.AppTime;

@Data
public class UserInviteDto {
    private Long id;
    private String createdAt;
    private String updatedAt;
    private Long createdBy;
    private String token;
    private String login;
    private String email;
    private String displayName;
    private String roleCode;
    private String roleTitle;
    private String statusCode;
    private String statusTitle;
    private Long storageQuotaBytes;
    private String expiresAt;
    private Boolean isEnabled;
    private String usedAt;
    private Long invitedUserId;

    public String getInviteUrl() {
        return token == null || token.isBlank() ? null : "/invite/" + token;
    }

    public Long getStorageQuotaMb() {
        if (storageQuotaBytes == null || storageQuotaBytes <= 0) {
            return 0L;
        }
        return storageQuotaBytes / (1024L * 1024L);
    }

    public String getCreatedAtTitle() {
        return AppTime.formatForTitle(createdAt, "dd.MM.yyyy HH:mm");
    }

    public String getExpiresAtTitle() {
        if (expiresAt == null || expiresAt.isBlank()) {
            return "Без срока";
        }
        return AppTime.formatForTitle(expiresAt, "dd.MM.yyyy HH:mm");
    }

    public String getUsedAtTitle() {
        return AppTime.formatForTitle(usedAt, "dd.MM.yyyy HH:mm");
    }

    public boolean isAvailable() {
        return Boolean.TRUE.equals(isEnabled) && usedAt == null;
    }
}
