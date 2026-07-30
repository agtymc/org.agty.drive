package org.agty.drive.dto;

import lombok.Data;

@Data
public class CollaborativeFolderShareDto {
    private Long folderId;
    private String folderName;
    private String folderPathKey;
    private Long ownerId;
    private String ownerLogin;
    private String ownerDisplayName;
    private String recipientLogins;
    private Integer recipientCount;
    private Boolean allowWrite;
    private Boolean allowDelete;
    private Boolean passwordProtected;
    private Long accessId;

    public String getOwnerTitle() {
        if (ownerDisplayName != null && !ownerDisplayName.isBlank()) {
            return ownerDisplayName;
        }
        return ownerLogin;
    }

    public String getRecipientCountTitle() {
        int count = recipientCount == null ? 0 : recipientCount;
        return count + " пользователей";
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
}
