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
import org.agty.drive.dto.UserDto;
import org.agty.drive.entity.AgdrvUser;

public final class UserConverter {

    private UserConverter() {
    }

    public static UserDto rowToDto(SqlRow row) {
        UserDto dto = new UserDto();
        dto.setId(row.getLong("id"));
        dto.setLogin(row.getString("login"));
        dto.setEmail(row.getString("email"));
        dto.setPasswordHash(row.getString("password_hash"));
        dto.setRoleCode(row.getString("role_code"));
        dto.setRoleTitle(row.getDstring("role_title"));
        dto.setStatusCode(row.getString("status_code"));
        dto.setStatusTitle(row.getDstring("status_title"));
        dto.setFirstName(row.getDstring("first_name"));
        dto.setLastName(row.getDstring("last_name"));
        dto.setMiddleName(row.getDstring("middle_name"));
        dto.setDisplayName(row.getDstring("display_name"));
        dto.setCreatedBy(row.getLong("created_by"));
        dto.setCreatedAt(row.getString("created_at"));
        dto.setUpdatedAt(row.getString("updated_at"));
        dto.setLastLoginAt(row.getString("last_login_at"));
        dto.setStorageQuotaBytes(row.getLong("storage_quota_bytes"));
        dto.setTwoFactorEmailEnabled(Boolean.TRUE.equals(row.getBoolean("two_factor_email_enabled")));
        dto.setTwoFactorTotpEnabled(Boolean.TRUE.equals(row.getBoolean("two_factor_totp_enabled")));
        dto.setTwoFactorTotpSecret(row.getString("two_factor_totp_secret"));
        dto.setTwoFactorTotpCreatedAt(row.getString("two_factor_totp_created_at"));
        dto.setTwoFactorEmailCodeHash(row.getString("two_factor_email_code_hash"));
        dto.setTwoFactorEmailCodeExpiresAt(row.getString("two_factor_email_code_expires_at"));
        return dto;
    }

    public static AgdrvUser dtoToEntity(UserDto dto) {
        AgdrvUser entity = new AgdrvUser();
        entity.setId(dto.getId());
        entity.setLogin(dto.getLogin());
        entity.setEmail(dto.getEmail());
        entity.setPasswordHash(dto.getPasswordHash());
        entity.setRoleCode(dto.getRoleCode());
        entity.setStatusCode(dto.getStatusCode());
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setMiddleName(dto.getMiddleName());
        entity.setDisplayName(dto.getDisplayName());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setLastLoginAt(dto.getLastLoginAt());
        entity.setStorageQuotaBytes(dto.getStorageQuotaBytes());
        entity.setTwoFactorEmailEnabled(Boolean.TRUE.equals(dto.getTwoFactorEmailEnabled()));
        entity.setTwoFactorTotpEnabled(Boolean.TRUE.equals(dto.getTwoFactorTotpEnabled()));
        entity.setTwoFactorTotpSecret(dto.getTwoFactorTotpSecret());
        entity.setTwoFactorTotpCreatedAt(dto.getTwoFactorTotpCreatedAt());
        entity.setTwoFactorEmailCodeHash(dto.getTwoFactorEmailCodeHash());
        entity.setTwoFactorEmailCodeExpiresAt(dto.getTwoFactorEmailCodeExpiresAt());
        entity.setUpdatedAt(AppTime.nowForDatabase());
        return entity;
    }
}
