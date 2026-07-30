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
import org.agty.drive.config.AppTime;
import org.agty.drive.dto.UserInviteDto;
import org.agty.drive.entity.AgdrvUserInvite;

public final class UserInviteConverter {

    private UserInviteConverter() {
    }

    public static UserInviteDto rowToDto(SqlRow row) {
        UserInviteDto dto = new UserInviteDto();
        dto.setId(row.getLong("id"));
        dto.setCreatedAt(row.getString("created_at"));
        dto.setUpdatedAt(row.getString("updated_at"));
        dto.setCreatedBy(row.getLong("created_by"));
        dto.setToken(row.getString("token"));
        dto.setLogin(row.getString("login"));
        dto.setEmail(row.getString("email"));
        dto.setDisplayName(row.getDstring("display_name"));
        dto.setRoleCode(row.getString("role_code"));
        dto.setRoleTitle(row.getDstring("role_title"));
        dto.setStatusCode(row.getString("status_code"));
        dto.setStatusTitle(row.getDstring("status_title"));
        dto.setStorageQuotaBytes(row.getLong("storage_quota_bytes"));
        dto.setExpiresAt(row.getString("expires_at"));
        dto.setIsEnabled(row.getBoolean("is_enabled"));
        dto.setUsedAt(row.getString("used_at"));
        dto.setInvitedUserId(row.getLong("invited_user_id"));
        return dto;
    }

    public static AgdrvUserInvite dtoToEntity(UserInviteDto dto) {
        AgdrvUserInvite entity = new AgdrvUserInvite();
        entity.setId(dto.getId());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setToken(dto.getToken());
        entity.setLogin(dto.getLogin());
        entity.setEmail(dto.getEmail());
        entity.setDisplayName(dto.getDisplayName());
        entity.setRoleCode(dto.getRoleCode());
        entity.setStatusCode(dto.getStatusCode());
        entity.setStorageQuotaBytes(dto.getStorageQuotaBytes());
        entity.setExpiresAt(dto.getExpiresAt());
        entity.setIsEnabled(!Boolean.FALSE.equals(dto.getIsEnabled()));
        entity.setUsedAt(dto.getUsedAt());
        entity.setInvitedUserId(dto.getInvitedUserId());
        entity.setUpdatedAt(AppTime.nowForDatabase());
        return entity;
    }
}
