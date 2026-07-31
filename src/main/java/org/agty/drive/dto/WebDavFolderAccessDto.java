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

@Data
public class WebDavFolderAccessDto {
    private Long id;
    private Long ownerId;
    private Long folderId;
    private String folderName;
    private String folderPathKey;
    private String accessToken;
    private String loginName;
    private String passwordHash;
    private Boolean allowWrite;
    private Boolean isEnabled;
    private String createdAt;
    private String updatedAt;
}
