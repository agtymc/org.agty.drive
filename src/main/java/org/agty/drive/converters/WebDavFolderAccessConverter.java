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
import org.agty.drive.dto.WebDavFolderAccessDto;
import org.agty.drive.entity.AgdrvFolderWebDavAccess;

public final class WebDavFolderAccessConverter {

    private WebDavFolderAccessConverter() {
    }

    public static WebDavFolderAccessDto rowToDto(SqlRow row) {
        WebDavFolderAccessDto dto = new WebDavFolderAccessDto();
        dto.setId(row.getLong("id"));
        dto.setOwnerId(row.getLong("owner_id"));
        dto.setFolderId(row.getLong("folder_id"));
        dto.setFolderName(row.getDstring("folder_name"));
        dto.setFolderPathKey(row.getDstring("folder_path_key"));
        dto.setAccessToken(row.getString("access_token"));
        dto.setLoginName(row.getString("login_name"));
        dto.setPasswordHash(row.getString("password_hash"));
        dto.setAllowWrite(row.getBoolean("allow_write"));
        dto.setIsEnabled(row.getBoolean("is_enabled"));
        dto.setCreatedAt(row.getString("created_at"));
        dto.setUpdatedAt(row.getString("updated_at"));
        return dto;
    }

    public static AgdrvFolderWebDavAccess dtoToEntity(WebDavFolderAccessDto dto) {
        AgdrvFolderWebDavAccess entity = new AgdrvFolderWebDavAccess();
        entity.setId(dto.getId());
        entity.setOwnerId(dto.getOwnerId());
        entity.setFolderId(dto.getFolderId());
        entity.setAccessToken(dto.getAccessToken());
        entity.setLoginName(dto.getLoginName());
        entity.setPasswordHash(dto.getPasswordHash());
        entity.setAllowWrite(Boolean.TRUE.equals(dto.getAllowWrite()));
        entity.setIsEnabled(!Boolean.FALSE.equals(dto.getIsEnabled()));
        entity.setUpdatedAt(AppTime.nowForDatabase());
        return entity;
    }
}
