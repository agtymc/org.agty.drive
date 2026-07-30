package org.agty.drive.dto;

import lombok.Data;
import org.agty.drive.config.AppTime;

@Data
public class CollaborativeAccessDto {
    private Long id;
    private Long ownerId;
    private String ownerLogin;
    private String ownerDisplayName;
    private Long folderId;
    private String folderName;
    private String folderPathKey;
    private Long targetUserId;
    private String targetUserLogin;
    private String targetUserDisplayName;
    private String passwordHash;
    private Boolean allowWrite;
    private Boolean allowDelete;
    private Boolean isEnabled;
    private String createdAt;
    private String updatedAt;

    public boolean isPasswordProtected() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    public String getRightsTitle() {
        boolean write = Boolean.TRUE.equals(allowWrite);
        boolean delete = Boolean.TRUE.equals(allowDelete);
        if (write && delete) {
            return "Чтение, запись и удаление";
        }
        if (write) {
            return "Чтение и запись";
        }
        if (delete) {
            return "Чтение и удаление";
        }
        return "Только чтение";
    }

    public String getOwnerTitle() {
        if (ownerDisplayName != null && !ownerDisplayName.isBlank()) {
            return ownerDisplayName;
        }
        return ownerLogin;
    }

    public String getTargetUserTitle() {
        if (targetUserDisplayName != null && !targetUserDisplayName.isBlank()) {
            return targetUserDisplayName;
        }
        return targetUserLogin;
    }

    public String getCreatedAtTitle() {
        return AppTime.formatForTitle(createdAt, "dd.MM.yyyy HH:mm");
    }
}
