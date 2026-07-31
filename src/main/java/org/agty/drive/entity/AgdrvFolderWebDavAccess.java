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

package org.agty.drive.entity;

import lombok.Data;
import org.agty.agtysql.model.annotations.Column;
import org.agty.agtysql.model.annotations.Entity;
import org.agty.agtysql.model.annotations.Id;
import org.agty.agtysql.model.annotations.Table;

@Data
@Entity
@Table(name = "agdrv_folder_webdav_access", schema = "public")
public class AgdrvFolderWebDavAccess {
    @Id
    private Long id;

    @Column(name = "created_at", skipIfNull = true)
    private String createdAt;

    @Column(name = "updated_at", skipIfNull = true)
    private String updatedAt;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "folder_id")
    private Long folderId;

    @Column(name = "access_token")
    private String accessToken;

    @Column(name = "login_name")
    private String loginName;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "allow_write")
    private Boolean allowWrite;

    @Column(name = "is_enabled")
    private Boolean isEnabled;
}
