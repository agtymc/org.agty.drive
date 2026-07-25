package org.agty.drive.web.controllers.api;

import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FileUploadDto;
import org.agty.drive.dto.FolderDto;
import org.agty.drive.dto.ItemMoveDto;
import org.agty.drive.dto.ItemRenameDto;
import org.agty.drive.dto.ShareLinkCreateDto;
import org.agty.drive.dto.ShareLinkDto;
import org.agty.drive.security.service.DriveUserDetails;
import org.agty.drive.services.AuditLogService;
import org.agty.drive.services.FileService;
import org.agty.drive.services.FolderDeleteService;
import org.agty.drive.services.FolderService;
import org.agty.drive.services.ShareLinkService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cabinet")
public class CabinetApiController {

    private final FolderService folderService;
    private final FileService fileService;
    private final FolderDeleteService folderDeleteService;
    private final ShareLinkService shareLinkService;
    private final AuditLogService auditLogService;

    public CabinetApiController(FolderService folderService,
                                FileService fileService,
                                FolderDeleteService folderDeleteService,
                                ShareLinkService shareLinkService,
                                AuditLogService auditLogService) {
        this.folderService = folderService;
        this.fileService = fileService;
        this.folderDeleteService = folderDeleteService;
        this.shareLinkService = shareLinkService;
        this.auditLogService = auditLogService;
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
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam Long folderId,
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
    public ResponseEntity<Map<String, Object>> renameItem(ItemRenameDto dto,
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
    public ResponseEntity<Map<String, Object>> moveItem(ItemMoveDto dto,
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
    public ResponseEntity<Map<String, Object>> createShareLink(ShareLinkCreateDto dto,
                                                               @AuthenticationPrincipal DriveUserDetails userDetails) {
        String error = shareLinkService.validateShareLinkCreate(userDetails.getUser().getId(), dto);
        if (error != null) {
            return error(error);
        }
        ShareLinkDto shareLink = shareLinkService.createShareLink(userDetails.getUser().getId(), dto);
        if (shareLink == null) {
            return error("Не удалось создать публичную ссылку.");
        }
        auditLogService.log(userDetails.getUser().getId(), "SHARE_CREATE", shareLink.getResourceType(), shareLink.getResourceId(), "Создана ссылка " + shareLink.getToken());
        return ok("token", shareLink.getToken(), "url", "/s/" + shareLink.getToken());
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
                                                               @AuthenticationPrincipal DriveUserDetails userDetails) {
        ShareLinkDto shareLink = "FOLDER".equalsIgnoreCase(resourceType)
                ? shareLinkService.findLatestFolderShareLink(resourceId)
                : shareLinkService.findLatestFileShareLink(resourceId);
        if (shareLink == null) {
            return error("Публичная ссылка не найдена.");
        }
        return ok("token", shareLink.getToken(), "url", "/s/" + shareLink.getToken());
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
}
