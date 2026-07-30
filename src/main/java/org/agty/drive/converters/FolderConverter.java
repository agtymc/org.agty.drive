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
