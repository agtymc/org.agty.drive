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

package org.agty.drive.dto;

import lombok.Data;
import org.agty.drive.config.AppTime;

@Data
public class UserInviteDto {
    private Long id;
    private String createdAt;
    private String updatedAt;
    private Long createdBy;
    private String token;
    private String login;
    private String email;
    private String displayName;
    private String roleCode;
    private String roleTitle;
    private String statusCode;
    private String statusTitle;
    private Long storageQuotaBytes;
    private String expiresAt;
    private Boolean isEnabled;
    private String usedAt;
    private Long invitedUserId;

    public String getInviteUrl() {
        return token == null || token.isBlank() ? null : "/invite/" + token;
    }

    public Long getStorageQuotaMb() {
        if (storageQuotaBytes == null || storageQuotaBytes <= 0) {
            return 0L;
        }
        return storageQuotaBytes / (1024L * 1024L);
    }

    public String getCreatedAtTitle() {
        return AppTime.formatForTitle(createdAt, "dd.MM.yyyy HH:mm");
    }

    public String getExpiresAtTitle() {
        if (expiresAt == null || expiresAt.isBlank()) {
            return "Без срока";
        }
        return AppTime.formatForTitle(expiresAt, "dd.MM.yyyy HH:mm");
    }

    public String getUsedAtTitle() {
        return AppTime.formatForTitle(usedAt, "dd.MM.yyyy HH:mm");
    }

    public boolean isAvailable() {
        return Boolean.TRUE.equals(isEnabled) && usedAt == null;
    }
}
