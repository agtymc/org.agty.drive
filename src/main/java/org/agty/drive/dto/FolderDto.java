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

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.agty.drive.config.AppTime;

@Data
public class FolderDto {
    private Long id;
    private Long ownerId;
    private Long parentId;

    @NotBlank(message = "Название папки обязательно")
    private String name;

    private String pathKey;
    private String description;
    private Integer sortOrder;
    private String expiresAt;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

    public String getCreatedAtTitle() {
        return convertDateTime(createdAt);
    }

    public String getUpdatedAtTitle() {
        return convertDateTime(updatedAt);
    }

    public String getExpiresAtTitle() {
        if (expiresAt == null || expiresAt.isBlank()) {
            return "Без срока";
        }
        return convertDateTime(expiresAt);
    }

    public String getExpiresAtInputValue() {
        return AppTime.formatForDateTimeInput(expiresAt);
    }

    private String convertDateTime(String value) {
        return AppTime.formatForTitle(value, "dd.MM.yyyy HH:mm");
    }
}
