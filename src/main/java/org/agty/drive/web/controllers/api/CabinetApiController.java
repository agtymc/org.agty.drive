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

package org.agty.drive.web.controllers.api;

import jakarta.servlet.http.HttpServletRequest;
import org.agty.drive.config.ApplicationInfo;
import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FileUploadDto;
import org.agty.drive.dto.FolderDto;
import org.agty.drive.dto.ItemMoveDto;
import org.agty.drive.dto.ItemRenameDto;
import org.agty.drive.dto.ShareLinkCreateDto;
import org.agty.drive.dto.ShareLinkDto;
import org.agty.drive.dto.SharedLibraryItemDto;
import org.agty.drive.security.service.DriveUserDetails;
import org.agty.drive.services.AuditLogService;
import org.agty.drive.services.FileContentStorageService;
import org.agty.drive.services.FileService;
import org.agty.drive.services.FolderDeleteService;
import org.agty.drive.services.FolderService;
import org.agty.drive.services.MimeTypePolicyService;
import org.agty.drive.services.ShareLinkService;
import org.agty.drive.web.MediaResponseSupport;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/cabinet")
public class CabinetApiController {

    private final FolderService folderService;
    private final FileService fileService;
    private final FileContentStorageService fileContentStorageService;
    private final FolderDeleteService folderDeleteService;
    private final ShareLinkService shareLinkService;
    private final AuditLogService auditLogService;
    private final MimeTypePolicyService mimeTypePolicyService;
    private final ApplicationInfo applicationInfo;

    public CabinetApiController(FolderService folderService,
                                FileService fileService,
                                FileContentStorageService fileContentStorageService,
                                FolderDeleteService folderDeleteService,
                                ShareLinkService shareLinkService,
                                AuditLogService auditLogService,
                                MimeTypePolicyService mimeTypePolicyService,
                                ApplicationInfo applicationInfo) {
        this.folderService = folderService;
        this.fileService = fileService;
        this.fileContentStorageService = fileContentStorageService;
        this.folderDeleteService = folderDeleteService;
        this.shareLinkService = shareLinkService;
        this.auditLogService = auditLogService;
        this.mimeTypePolicyService = mimeTypePolicyService;
        this.applicationInfo = applicationInfo;
    }

    @PostMapping("folders")
    public ResponseEntity<Map<String, Object>> createFolder(@RequestParam String name,
                                                            @RequestParam(required = false) Long parentId,
                                                            @RequestParam(required = false) String description,
                                                            @AuthenticationPrincipal DriveUserDetails userDetails) {
        FolderDto dto = new FolderDto();
        dto.setOwnerId(userDetails.getUser().getId());
        dto.setParentId(parentId);
        dto.setName(name);
        dto.setDescription(description);
        dto.setPathKey(folderService.buildPathKeyForCreate(userDetails.getUser().getId(), parentId, name));
        dto.setSortOrder(0);
        FolderDto saved = folderService.save(dto);
        if (saved == null) {
            return error("Не удалось создать папку.");
        }
        auditLogService.log(userDetails.getUser().getId(), "FOLDER_CREATE", "FOLDER", saved.getId(), "Создана папка " + saved.getName());
        return ok("folderId", saved.getId(), "name", saved.getName());
    }

    @PostMapping("files")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam(required = false) Long folderId,
                                                          @RequestParam MultipartFile file,
                                                          @RequestParam(required = false) String description,
                                                          @AuthenticationPrincipal DriveUserDetails userDetails) {
        FileUploadDto dto = new FileUploadDto();
        dto.setFolderId(folderId);
        dto.setFile(file);
        dto.setDescription(description);
        String validation = fileService.validateUpload(userDetails.getUser().getId(), dto);
        if (validation != null) {
            return error(validation);
        }
        FileItemDto saved = fileService.upload(userDetails.getUser().getId(), dto);
        if (saved == null) {
            return error("Не удалось загрузить файл.");
        }
        auditLogService.log(userDetails.getUser().getId(), "FILE_UPLOAD", "FILE", saved.getId(), "Загружен файл " + saved.getOriginalFilename());
        return ok("fileId", saved.getId(), "name", saved.getOriginalFilename());
    }

    @PostMapping("items/rename")
    public ResponseEntity<Map<String, Object>> renameItem(@ModelAttribute ItemRenameDto dto,
                                                          @AuthenticationPrincipal DriveUserDetails userDetails) {
        String type = dto.getResourceType() == null ? "" : dto.getResourceType().trim().toUpperCase();
        String error = switch (type) {
            case "FOLDER" -> folderService.renameByIdAndOwnerId(dto.getResourceId(), userDetails.getUser().getId(), dto.getNewName());
            case "FILE" -> fileService.renameByIdAndOwnerId(dto.getResourceId(), userDetails.getUser().getId(), dto.getNewName());
            default -> "Неизвестный тип объекта.";
        };
        if (error != null) {
            return error(error);
        }
        auditLogService.log(userDetails.getUser().getId(), "ITEM_RENAME", type, dto.getResourceId(), "Переименование в " + dto.getNewName());
        return ok("resourceId", dto.getResourceId(), "resourceType", type);
    }

    @PostMapping("items/move")
    public ResponseEntity<Map<String, Object>> moveItem(@ModelAttribute ItemMoveDto dto,
                                                        @AuthenticationPrincipal DriveUserDetails userDetails) {
        String type = dto.getResourceType() == null ? "" : dto.getResourceType().trim().toUpperCase();
        String error = switch (type) {
            case "FOLDER" -> folderService.moveByIdAndOwnerId(dto.getResourceId(), userDetails.getUser().getId(), dto.getTargetFolderId());
            case "FILE" -> fileService.moveByIdAndOwnerId(dto.getResourceId(), userDetails.getUser().getId(), dto.getTargetFolderId());
            default -> "Неизвестный тип объекта.";
        };
        if (error != null) {
            return error(error);
        }
        auditLogService.log(userDetails.getUser().getId(), "ITEM_MOVE", type, dto.getResourceId(), "Перемещение в папку " + dto.getTargetFolderId());
        return ok("resourceId", dto.getResourceId(), "resourceType", type);
    }

    @DeleteMapping("items/{resourceType}/{resourceId}")
    public ResponseEntity<Map<String, Object>> deleteItem(@PathVariable String resourceType,
                                                          @PathVariable Long resourceId,
                                                          @AuthenticationPrincipal DriveUserDetails userDetails) {
        String type = resourceType == null ? "" : resourceType.trim().toUpperCase();
        String error = switch (type) {
            case "FOLDER" -> folderDeleteService.deleteByIdAndOwnerId(resourceId, userDetails.getUser().getId());
            case "FILE" -> fileService.deleteByIdAndOwnerId(resourceId, userDetails.getUser().getId());
            default -> "Неизвестный тип объекта.";
        };
        if (error != null) {
            return error(error);
        }
        auditLogService.log(userDetails.getUser().getId(), "ITEM_DELETE", type, resourceId, "Удаление через API");
        return ok("resourceId", resourceId, "resourceType", type);
    }

    @PostMapping("shares")
    public ResponseEntity<Map<String, Object>> createShareLink(@ModelAttribute ShareLinkCreateDto dto,
                                                               @AuthenticationPrincipal DriveUserDetails userDetails,
                                                               HttpServletRequest request) {
        String error = shareLinkService.validateShareLinkCreate(userDetails.getUser().getId(), dto);
        if (error != null) {
            return error(error);
        }
        ShareLinkDto shareLink = shareLinkService.createShareLink(userDetails.getUser().getId(), dto);
        if (shareLink == null) {
            return error("Не удалось создать публичную ссылку.");
        }
        auditLogService.log(userDetails.getUser().getId(), "SHARE_CREATE", shareLink.getResourceType(), shareLink.getResourceId(), "Создана ссылка " + shareLink.getToken());
        return ok("token", shareLink.getToken(), "url", buildShareUrl(request, shareLink.getToken()));
    }

    @DeleteMapping("shares")
    public ResponseEntity<Map<String, Object>> deleteShareLink(@RequestParam String resourceType,
                                                               @RequestParam Long resourceId,
                                                               @AuthenticationPrincipal DriveUserDetails userDetails) {
        String error = shareLinkService.deleteShareLink(userDetails.getUser().getId(), resourceType, resourceId);
        if (error != null) {
            return error(error);
        }
        auditLogService.log(userDetails.getUser().getId(), "SHARE_DELETE", resourceType, resourceId, "Публичная ссылка удалена");
        return ok("resourceId", resourceId, "resourceType", resourceType);
    }

    @GetMapping("shares/latest")
    public ResponseEntity<Map<String, Object>> latestShareLink(@RequestParam String resourceType,
                                                               @RequestParam Long resourceId,
                                                               @AuthenticationPrincipal DriveUserDetails userDetails,
                                                               HttpServletRequest request) {
        ShareLinkDto shareLink = "FOLDER".equalsIgnoreCase(resourceType)
                ? shareLinkService.findLatestFolderShareLink(resourceId)
                : shareLinkService.findLatestFileShareLink(resourceId);
        if (shareLink == null) {
            return error("Публичная ссылка не найдена.");
        }
        return ok("token", shareLink.getToken(), "url", buildShareUrl(request, shareLink.getToken()));
    }

    @GetMapping("library")
    public ResponseEntity<Map<String, Object>> library(@RequestParam String scope,
                                                       @RequestParam(required = false) String q,
                                                       @RequestParam(required = false) String sort,
                                                       @RequestParam(required = false, name = "status") String sharedStatus,
                                                       @RequestParam(required = false, name = "type") String sharedType,
                                                       @RequestParam(required = false) Integer page,
                                                       @RequestParam(required = false) Integer size,
                                                       @AuthenticationPrincipal DriveUserDetails userDetails) {
        String normalizedScope = normalizeLibraryScope(scope);
        if (normalizedScope == null) {
            return error("Поддерживаются только режимы photos, videos и shared.");
        }

        int normalizedSize = normalizePageSize(size);
        int normalizedPage = Math.max(1, page == null ? 1 : page);
        String normalizedSort = normalizeLibrarySort(sort, normalizedScope);

        if ("shared".equals(normalizedScope)) {
            List<SharedLibraryItemDto> allItems = shareLinkService.findLibraryByCreator(
                    userDetails.getUser().getId(),
                    q,
                    normalizedSort,
                    sharedStatus,
                    sharedType
            );
            int totalItems = allItems.size();
            int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) normalizedSize));
            int currentPage = Math.min(normalizedPage, totalPages);
            int fromIndex = Math.min((currentPage - 1) * normalizedSize, totalItems);
            int toIndex = Math.min(totalItems, fromIndex + normalizedSize);
            List<SharedLibraryItemDto> items = allItems.subList(fromIndex, toIndex);
            return ok(
                    "scope", normalizedScope,
                    "sort", normalizedSort,
                    "filters", Map.of(
                            "status", normalizeSharedStatus(sharedStatus),
                            "type", normalizeSharedType(sharedType)
                    ),
                    "page", currentPage,
                    "size", normalizedSize,
                    "totalItems", totalItems,
                    "totalPages", totalPages,
                    "summary", shareLinkService.summarizeLibraryStatuses(
                            shareLinkService.findLibraryByCreator(userDetails.getUser().getId(), q, normalizedSort, null, null)
                    ),
                    "items", items
            );
        }

        List<FileItemDto> items = fileService.findMediaLibraryByOwnerId(
                userDetails.getUser().getId(),
                q,
                normalizedScope,
                normalizedSort
        );
        int totalItems = items.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) normalizedSize));
        int currentPage = Math.min(normalizedPage, totalPages);
        int fromIndex = Math.min((currentPage - 1) * normalizedSize, totalItems);
        int toIndex = Math.min(totalItems, fromIndex + normalizedSize);
        return ok(
                "scope", normalizedScope,
                "sort", normalizedSort,
                "filters", Map.of(),
                "page", currentPage,
                "size", normalizedSize,
                "totalItems", totalItems,
                "totalPages", totalPages,
                "items", items.subList(fromIndex, toIndex)
        );
    }

    @GetMapping("library/open")
    public ResponseEntity<Map<String, Object>> openLibrary(@RequestParam(required = false) String q,
                                                           @RequestParam(required = false) String sort,
                                                           @RequestParam(required = false, name = "status") String sharedStatus,
                                                           @RequestParam(required = false, name = "type") String sharedType,
                                                           @RequestParam(required = false) Integer page,
                                                           @RequestParam(required = false) Integer size,
                                                           @AuthenticationPrincipal DriveUserDetails userDetails) {
        return library("shared", q, sort, sharedStatus, sharedType, page, size, userDetails);
    }

    @GetMapping("library/media")
    public ResponseEntity<Map<String, Object>> mediaLibrary(@RequestParam String scope,
                                                            @RequestParam(required = false) String q,
                                                            @RequestParam(required = false) String sort,
                                                            @RequestParam(required = false) Integer page,
                                                            @RequestParam(required = false) Integer size,
                                                            @AuthenticationPrincipal DriveUserDetails userDetails) {
        return library(scope, q, sort, null, null, page, size, userDetails);
    }

    @GetMapping("library/modes")
    public ResponseEntity<Map<String, Object>> libraryModes() {
        return ok(
                "modes", List.of(
                        Map.of(
                                "code", "shared",
                                "title", "Открытый доступ",
                                "api", "/api/cabinet/library/open",
                                "filters", List.of("status", "type", "sort", "q", "page", "size")
                        ),
                        Map.of(
                                "code", "photos",
                                "title", "Фото",
                                "api", "/api/cabinet/library/media?scope=photos",
                                "filters", List.of("sort", "q", "page", "size")
                        ),
                        Map.of(
                                "code", "videos",
                                "title", "Видео",
                                "api", "/api/cabinet/library/media?scope=videos",
                                "filters", List.of("sort", "q", "page", "size")
                        ),
                        Map.of(
                                "code", "collaborative",
                                "title", "Совместный доступ",
                                "api", "/api/cabinet/library/collaborative",
                                "status", "planned"
                        )
                )
        );
    }

    @GetMapping("files/{id}/download")
    public ResponseEntity<?> downloadFile(@PathVariable Long id,
                                          @AuthenticationPrincipal DriveUserDetails userDetails) {
        FileItemDto file = fileService.findByIdAndOwnerId(id, userDetails.getUser().getId());
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        var path = fileContentStorageService.resolveExistingPath(file.getStorageName());
        if (path == null) {
            return ResponseEntity.notFound().build();
        }
        return MediaResponseSupport.buildPathResponse(
                path,
                resolveMediaType(file),
                file.getOriginalFilename(),
                false,
                null
        );
    }

    @GetMapping("files/{id}/content")
    public ResponseEntity<?> openFileContent(@PathVariable Long id,
                                             @RequestHeader(value = "Range", required = false) String rangeHeader,
                                             @AuthenticationPrincipal DriveUserDetails userDetails) {
        FileItemDto file = fileService.findByIdAndOwnerId(id, userDetails.getUser().getId());
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        var path = fileContentStorageService.resolveExistingPath(file.getStorageName());
        if (path == null) {
            return ResponseEntity.notFound().build();
        }
        return MediaResponseSupport.buildPathResponse(
                path,
                resolveMediaType(file),
                file.getOriginalFilename(),
                true,
                rangeHeader
        );
    }

    @GetMapping("files/{id}/thumbnail")
    public ResponseEntity<byte[]> openFileThumbnail(@PathVariable Long id,
                                                    @AuthenticationPrincipal DriveUserDetails userDetails) {
        FileItemDto file = fileService.findByIdAndOwnerId(id, userDetails.getUser().getId());
        if (file == null || !file.isImagePreview()) {
            return ResponseEntity.notFound().build();
        }
        byte[] content = fileService.findThumbnailBytesByFileId(id);
        if (content == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .contentLength(content.length)
                .body(content);
    }

    private ResponseEntity<Map<String, Object>> ok(Object... kv) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            body.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return ResponseEntity.ok(body);
    }

    private ResponseEntity<Map<String, Object>> error(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("error", message);
        return ResponseEntity.badRequest().body(body);
    }

    private String buildShareUrl(HttpServletRequest request, String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return applicationInfo.resolveBaseUri(request) + "/s/" + token;
    }

    private MediaType resolveMediaType(FileItemDto file) {
        return mimeTypePolicyService.resolveResponseMediaType(file == null ? null : file.getMimeType());
    }

    private String normalizeLibraryScope(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "photos", "videos", "shared" -> value.trim().toLowerCase(Locale.ROOT);
            default -> null;
        };
    }

    private String normalizeLibrarySort(String value, String scope) {
        if (value == null || value.isBlank()) {
            return "date_newest";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("shared".equals(scope)) {
            return switch (normalized) {
                case "name_asc", "name_desc", "date_newest", "date_oldest", "type_asc", "expiry_asc", "expiry_desc", "status_asc", "size_asc", "size_desc" -> normalized;
                default -> "date_newest";
            };
        }
        return switch (normalized) {
            case "name_asc", "name_desc", "date_newest", "date_oldest", "size_desc", "size_asc", "type_asc" -> normalized;
            default -> "shared".equals(scope) || "photos".equals(scope) || "videos".equals(scope) ? "date_newest" : "name_asc";
        };
    }

    private String normalizeSharedStatus(String value) {
        if (value == null || value.isBlank()) {
            return "all";
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "active", "expiring", "expired", "without_expiry" -> value.trim().toLowerCase(Locale.ROOT);
            default -> "all";
        };
    }

    private String normalizeSharedType(String value) {
        if (value == null || value.isBlank()) {
            return "all";
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "files", "folders" -> value.trim().toLowerCase(Locale.ROOT);
            default -> "all";
        };
    }

    private int normalizePageSize(Integer value) {
        if (value == null) {
            return 20;
        }
        return switch (value) {
            case 50, 100 -> value;
            default -> 20;
        };
    }

}
