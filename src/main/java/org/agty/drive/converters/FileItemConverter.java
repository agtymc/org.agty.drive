package org.agty.drive.converters;

import org.agty.agtysql.interfaces.SqlRow;
import org.agty.drive.config.AppTime;
import org.agty.drive.dto.FileItemDto;
import org.agty.drive.entity.AgdrvFile;

public final class FileItemConverter {

    private FileItemConverter() {
    }

    public static FileItemDto rowToDto(SqlRow row) {
        FileItemDto dto = new FileItemDto();
        dto.setId(row.getLong("id"));
        dto.setOwnerId(row.getLong("owner_id"));
        dto.setFolderId(row.getLong("folder_id"));
        dto.setFolderName(row.getDstring("folder_name"));
        dto.setOriginalFilename(row.getDstring("original_filename"));
        dto.setStorageName(row.getString("storage_name"));
        dto.setMimeType(row.getString("mime_type"));
        dto.setExtension(row.getString("extension"));
        dto.setSizeBytes(row.getLong("size_bytes"));
        dto.setChecksumSha256(row.getString("checksum_sha256"));
        dto.setDescription(row.getDstring("description"));
        dto.setExpiresAt(row.getString("expires_at"));
        dto.setPreviewStatus(row.getString("preview_status"));
        dto.setIsImage(row.getBoolean("is_image"));
        dto.setIsVideo(row.getBoolean("is_video"));
        dto.setCreatedAt(row.getString("created_at"));
        dto.setUpdatedAt(row.getString("updated_at"));
        dto.setDeletedAt(row.getString("deleted_at"));
        return dto;
    }

    public static AgdrvFile dtoToEntity(FileItemDto dto) {
        AgdrvFile entity = new AgdrvFile();
        entity.setId(dto.getId());
        entity.setOwnerId(dto.getOwnerId());
        entity.setFolderId(dto.getFolderId());
        entity.setOriginalFilename(dto.getOriginalFilename());
        entity.setStorageName(dto.getStorageName());
        entity.setMimeType(dto.getMimeType());
        entity.setExtension(dto.getExtension());
        entity.setSizeBytes(dto.getSizeBytes() == null ? 0L : dto.getSizeBytes());
        entity.setChecksumSha256(dto.getChecksumSha256());
        entity.setDescription(dto.getDescription());
        entity.setExpiresAt(dto.getExpiresAt());
        entity.setPreviewStatus(dto.getPreviewStatus() == null ? "NONE" : dto.getPreviewStatus());
        entity.setIsImage(Boolean.TRUE.equals(dto.getIsImage()));
        entity.setIsVideo(Boolean.TRUE.equals(dto.getIsVideo()));
        entity.setDeletedAt(dto.getDeletedAt());
        entity.setUpdatedAt(AppTime.nowForDatabase());
        return entity;
    }
}
