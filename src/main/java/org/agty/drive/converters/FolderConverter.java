package org.agty.drive.converters;

import org.agty.agtysql.interfaces.SqlRow;
import org.agty.drive.config.AppTime;
import org.agty.drive.dto.FolderDto;
import org.agty.drive.entity.AgdrvFolder;

public final class FolderConverter {

    private FolderConverter() {
    }

    public static FolderDto rowToDto(SqlRow row) {
        FolderDto dto = new FolderDto();
        dto.setId(row.getLong("id"));
        dto.setOwnerId(row.getLong("owner_id"));
        dto.setParentId(row.getLong("parent_id"));
        dto.setName(row.getDstring("name"));
        dto.setPathKey(row.getString("path_key"));
        dto.setDescription(row.getDstring("description"));
        dto.setExpiresAt(row.getString("expires_at"));
        dto.setSortOrder(row.getInt("sort_order"));
        dto.setCreatedAt(row.getString("created_at"));
        dto.setUpdatedAt(row.getString("updated_at"));
        dto.setDeletedAt(row.getString("deleted_at"));
        return dto;
    }

    public static AgdrvFolder dtoToEntity(FolderDto dto) {
        AgdrvFolder entity = new AgdrvFolder();
        entity.setId(dto.getId());
        entity.setOwnerId(dto.getOwnerId());
        entity.setParentId(dto.getParentId());
        entity.setName(dto.getName());
        entity.setPathKey(dto.getPathKey());
        entity.setDescription(dto.getDescription());
        entity.setExpiresAt(dto.getExpiresAt());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setDeletedAt(dto.getDeletedAt());
        entity.setUpdatedAt(AppTime.nowForDatabase());
        return entity;
    }
}
