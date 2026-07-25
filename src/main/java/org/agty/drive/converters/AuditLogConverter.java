package org.agty.drive.converters;

import org.agty.agtysql.interfaces.SqlRow;
import org.agty.drive.dto.AuditLogDto;
import org.agty.drive.entity.AgdrvAuditLog;

public final class AuditLogConverter {

    private AuditLogConverter() {
    }

    public static AuditLogDto rowToDto(SqlRow row) {
        AuditLogDto dto = new AuditLogDto();
        dto.setId(row.getLong("id"));
        dto.setCreatedAt(row.getString("created_at"));
        dto.setActorUserId(row.getLong("actor_user_id"));
        dto.setActorLogin(row.getString("actor_login"));
        dto.setActionCode(row.getString("action_code"));
        dto.setResourceType(row.getString("resource_type"));
        dto.setResourceId(row.getLong("resource_id"));
        dto.setDetails(row.getDstring("details"));
        return dto;
    }

    public static AgdrvAuditLog dtoToEntity(AuditLogDto dto) {
        AgdrvAuditLog entity = new AgdrvAuditLog();
        entity.setId(dto.getId());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setActorUserId(dto.getActorUserId());
        entity.setActionCode(dto.getActionCode());
        entity.setResourceType(dto.getResourceType());
        entity.setResourceId(dto.getResourceId());
        entity.setDetails(dto.getDetails());
        return entity;
    }
}
