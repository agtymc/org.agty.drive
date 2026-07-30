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

package org.agty.drive.web.controllers.mvc.cabinet;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component("cabinetRoutes")
public class CabinetRoutes {

    public String sectionPath(String section) {
        return switch (normalizeSection(section)) {
            case "photos" -> "/cabinet/photos";
            case "videos" -> "/cabinet/videos";
            case "shared" -> "/cabinet/shared";
            case "collaborative" -> "/cabinet/collaborative";
            case "profile" -> "/cabinet/profile";
            default -> "/cabinet";
        };
    }

    public String controlPath() {
        return "/control";
    }

    public String pagePath(String section,
                           Long folderId,
                           String viewMode,
                           String sortMode,
                           String searchQuery,
                           String searchScope,
                           String sharedStatusFilter,
                           String sharedTypeFilter,
                           Integer page,
                           Integer pageSize) {
        return pagePath(section, folderId, viewMode, sortMode, searchQuery, searchScope, sharedStatusFilter, sharedTypeFilter, null, page, pageSize);
    }

    public String pagePath(String section,
                           Long folderId,
                           String viewMode,
                           String sortMode,
                           String searchQuery,
                           String searchScope,
                           String sharedStatusFilter,
                           String sharedTypeFilter,
                           Long collaborativeAccessId,
                           Integer page,
                           Integer pageSize) {
        String normalizedSection = normalizeSection(section);
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(sectionPath(normalizedSection));

        if ("files".equals(normalizedSection) && folderId != null) {
            builder.queryParam("folderId", folderId);
        }
        if ("collaborative".equals(normalizedSection) && collaborativeAccessId != null) {
            builder.queryParam("accessId", collaborativeAccessId);
        }
        if ("collaborative".equals(normalizedSection) && folderId != null) {
            builder.queryParam("folderId", folderId);
        }
        if (viewMode != null && !"list".equals(viewMode)) {
            builder.queryParam("view", viewMode);
        }
        if (sortMode != null) {
            String defaultSort = defaultSort(normalizedSection);
            if (!defaultSort.equals(sortMode)) {
                builder.queryParam("sort", sortMode);
            }
        }
        if (searchQuery != null && !searchQuery.isBlank()) {
            builder.queryParam("q", searchQuery);
        }
        if ("files".equals(normalizedSection) && searchScope != null && !"current".equals(searchScope)) {
            builder.queryParam("scope", searchScope);
        }
        if ("shared".equals(normalizedSection) && sharedStatusFilter != null && !"all".equals(sharedStatusFilter)) {
            builder.queryParam("shareStatus", sharedStatusFilter);
        }
        if ("shared".equals(normalizedSection) && sharedTypeFilter != null && !"all".equals(sharedTypeFilter)) {
            builder.queryParam("shareType", sharedTypeFilter);
        }
        if (page != null && page > 1) {
            builder.queryParam("page", page);
        }
        if (pageSize != null && pageSize != 20) {
            builder.queryParam("size", pageSize);
        }
        return builder.build().toUriString();
    }

    private String normalizeSection(String section) {
        if (section == null) {
            return "files";
        }
        return switch (section.trim().toLowerCase()) {
            case "photos", "videos", "shared", "collaborative", "profile" -> section.trim().toLowerCase();
            default -> "files";
        };
    }

    private String defaultSort(String section) {
        return switch (section) {
            case "photos", "videos", "shared", "collaborative" -> "date_newest";
            default -> "name_asc";
        };
    }
}
