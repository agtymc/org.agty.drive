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
public class SharedLibraryItemDto {
    private ShareLinkDto shareLink;
    private String resourceName;
    private String resourceTypeTitle;
    private String resourceMimeType;
    private Long resourceSizeBytes;
    private boolean previewAvailable;
    private String previewType;

    public SharedLibraryItemDto(ShareLinkDto shareLink, String resourceName, String resourceTypeTitle) {
        this.shareLink = shareLink;
        this.resourceName = resourceName;
        this.resourceTypeTitle = resourceTypeTitle;
    }

    public boolean isFolder() {
        return shareLink != null && "FOLDER".equalsIgnoreCase(shareLink.getResourceType());
    }

    public boolean isFile() {
        return !isFolder();
    }

    public String getStatusCode() {
        return shareLink == null ? "active" : shareLink.getExpiryStatusCode();
    }

    public String getStatusTitle() {
        return shareLink == null ? "Активна" : shareLink.getExpiryStatusTitle();
    }
}
