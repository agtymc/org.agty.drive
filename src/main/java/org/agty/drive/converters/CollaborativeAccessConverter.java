package org.agty.drive.converters;

import org.agty.agtysql.interfaces.SqlRow;
import org.agty.drive.config.AppTime;
import org.agty.drive.dto.CollaborativeAccessDto;
import org.agty.drive.entity.AgdrvFolderCollaborativeAccess;

public final class CollaborativeAccessConverter {

    private CollaborativeAccessConverter() {
    }

    public static CollaborativeAccessDto rowToDto(SqlRow row) {
        CollaborativeAccessDto dto = new CollaborativeAccessDto();
        dto.setId(row.getLong("id"));
        dto.setOwnerId(row.getLong("owner_id"));
        dto.setFolderId(row.getLong("folder_id"));
        dto.setTargetUserId(row.getLong("target_user_id"));
        dto.setPasswordHash(row.getString("password_hash"));
        dto.setAllowWrite(Boolean.TRUE.equals(row.getBoolean("allow_write")));
        dto.setAllowDelete(Boolean.TRUE.equals(row.getBoolean("allow_delete")));
        dto.setIsEnabled(!Boolean.FALSE.equals(row.getBoolean("is_enabled")));
        dto.setCreatedAt(row.getString("created_at"));
        dto.setUpdatedAt(row.getString("updated_at"));
        dto.setOwnerLogin(row.getString("owner_login"));
        dto.setOwnerDisplayName(row.getDstring("owner_display_name"));
        dto.setFolderName(row.getString("folder_name"));
        dto.setFolderPathKey(row.getString("folder_path_key"));
        dto.setTargetUserLogin(row.getString("target_user_login"));
        dto.setTargetUserDisplayName(row.getDstring("target_user_display_name"));
        return dto;
    }

    public static AgdrvFolderCollaborativeAccess dtoToEntity(CollaborativeAccessDto dto) {
        AgdrvFolderCollaborativeAccess entity = new AgdrvFolderCollaborativeAccess();
        entity.setId(dto.getId());
        entity.setOwnerId(dto.getOwnerId());
        entity.setFolderId(dto.getFolderId());
        entity.setTargetUserId(dto.getTargetUserId());
        entity.setPasswordHash(dto.getPasswordHash());
        entity.setAllowWrite(Boolean.TRUE.equals(dto.getAllowWrite()));
        entity.setAllowDelete(Boolean.TRUE.equals(dto.getAllowDelete()));
        entity.setIsEnabled(!Boolean.FALSE.equals(dto.getIsEnabled()));
        entity.setUpdatedAt(AppTime.nowForDatabase());
        return entity;
    }
}
