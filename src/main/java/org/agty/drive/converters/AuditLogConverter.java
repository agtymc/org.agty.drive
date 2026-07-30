/*
 * Copyright 2026 Vladimir V
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
