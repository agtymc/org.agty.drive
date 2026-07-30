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
import org.agty.utils.AgtyUtils;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String login;
    private String email;
    private String passwordHash;
    private String roleCode;
    private String roleTitle;
    private String statusCode;
    private String statusTitle;
    private String firstName;
    private String lastName;
    private String middleName;
    private String displayName;
    private Long createdBy;
    private String createdAt;
    private String updatedAt;
    private String lastLoginAt;
    private Long storageQuotaBytes;
    private Boolean twoFactorEmailEnabled;
    private Boolean twoFactorTotpEnabled;
    private String twoFactorTotpSecret;
    private String twoFactorTotpCreatedAt;
    private String twoFactorEmailCodeHash;
    private String twoFactorEmailCodeExpiresAt;

    public String getStorageQuotaTitle() {
        if (storageQuotaBytes == null || storageQuotaBytes <= 0) {
            return "0 Б";
        }
        return AgtyUtils.filesizeToTitle(storageQuotaBytes, "ru");
    }

    public Long getStorageQuotaMb() {
        if (storageQuotaBytes == null || storageQuotaBytes <= 0) {
            return 0L;
        }
        return storageQuotaBytes / (1024L * 1024L);
    }
}
