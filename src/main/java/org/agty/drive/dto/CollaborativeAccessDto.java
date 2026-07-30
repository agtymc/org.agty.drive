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
public class CollaborativeAccessDto {
    private Long id;
    private Long ownerId;
    private String ownerLogin;
    private String ownerDisplayName;
    private Long folderId;
    private String folderName;
    private String folderPathKey;
    private Long targetUserId;
    private String targetUserLogin;
    private String targetUserDisplayName;
    private String passwordHash;
    private Boolean allowWrite;
    private Boolean allowDelete;
    private Boolean isEnabled;
    private String createdAt;
    private String updatedAt;

    public boolean isPasswordProtected() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    public String getRightsTitle() {
        boolean write = Boolean.TRUE.equals(allowWrite);
        boolean delete = Boolean.TRUE.equals(allowDelete);
        if (write && delete) {
            return "Чтение, запись и удаление";
        }
        if (write) {
            return "Чтение и запись";
        }
        if (delete) {
            return "Чтение и удаление";
        }
        return "Только чтение";
    }

    public String getOwnerTitle() {
        if (ownerDisplayName != null && !ownerDisplayName.isBlank()) {
            return ownerDisplayName;
        }
        return ownerLogin;
    }

    public String getTargetUserTitle() {
        if (targetUserDisplayName != null && !targetUserDisplayName.isBlank()) {
            return targetUserDisplayName;
        }
        return targetUserLogin;
    }

    public String getCreatedAtTitle() {
        return AppTime.formatForTitle(createdAt, "dd.MM.yyyy HH:mm");
    }
}
