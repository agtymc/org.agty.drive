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
import org.agty.drive.dto.ShareLinkDto;
import org.agty.drive.entity.AgdrvShareLink;

public final class ShareLinkConverter {

    private ShareLinkConverter() {
    }

    public static ShareLinkDto rowToDto(SqlRow row) {
        ShareLinkDto dto = new ShareLinkDto();
        dto.setId(row.getLong("id"));
        dto.setCreatedBy(row.getLong("created_by"));
        dto.setToken(row.getString("token"));
        dto.setResourceType(row.getString("resource_type"));
        dto.setResourceId(row.getLong("resource_id"));
        dto.setTitle(row.getDstring("title"));
        dto.setPasswordHash(row.getString("password_hash"));
        dto.setExpiresAt(row.getString("expires_at"));
        dto.setAllowDownload(Boolean.TRUE.equals(row.getBoolean("allow_download")));
        dto.setAllowPreview(Boolean.TRUE.equals(row.getBoolean("allow_preview")));
        dto.setIsEnabled(!Boolean.FALSE.equals(row.getBoolean("is_enabled")));
        dto.setMaxDownloads(row.getLong("max_downloads"));
        dto.setDownloadCount(row.getLong("download_count"));
        dto.setCreatedAt(row.getString("created_at"));
        dto.setUpdatedAt(row.getString("updated_at"));
        return dto;
    }

    public static AgdrvShareLink dtoToEntity(ShareLinkDto dto) {
        AgdrvShareLink entity = new AgdrvShareLink();
        entity.setId(dto.getId());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setToken(dto.getToken());
        entity.setResourceType(dto.getResourceType());
        entity.setResourceId(dto.getResourceId());
        entity.setTitle(dto.getTitle());
        entity.setPasswordHash(dto.getPasswordHash());
        entity.setExpiresAt(dto.getExpiresAt());
        entity.setAllowDownload(Boolean.TRUE.equals(dto.getAllowDownload()));
        entity.setAllowPreview(Boolean.TRUE.equals(dto.getAllowPreview()));
        entity.setIsEnabled(!Boolean.FALSE.equals(dto.getIsEnabled()));
        entity.setMaxDownloads(dto.getMaxDownloads());
        entity.setDownloadCount(dto.getDownloadCount() == null ? 0L : dto.getDownloadCount());
        entity.setUpdatedAt(AppTime.nowForDatabase());
        return entity;
    }
}
