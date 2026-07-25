package org.agty.drive.web.controllers.mvc.cabinet;

import jakarta.validation.Valid;
import org.agty.drive.dto.ChangePasswordDto;
import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FileUploadDto;
import org.agty.drive.dto.FolderDto;
import org.agty.drive.dto.ItemMoveDto;
import org.agty.drive.dto.ItemRenameDto;
import org.agty.drive.dto.ProfileSecuritySettingsDto;
import org.agty.drive.dto.ShareLinkCreateDto;
import org.agty.drive.dto.ShareLinkDto;
import org.agty.drive.security.service.DriveUserDetails;
import org.agty.drive.services.FileContentStorageService;
import org.agty.drive.services.FileService;
import org.agty.drive.services.AuditLogService;
import org.agty.drive.services.FolderArchiveService;
import org.agty.drive.services.FolderDeleteService;
import org.agty.drive.services.FolderService;
import org.agty.drive.services.ShareLinkService;
import org.agty.drive.services.UserService;
import org.agty.drive.web.MediaResponseSupport;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/cabinet")
public class CabinetMvcController {

    private final FolderService folderService;
    private final FileService fileService;
    private final FileContentStorageService fileContentStorageService;
    private final FolderArchiveService folderArchiveService;
    private final FolderDeleteService folderDeleteService;
    private final ShareLinkService shareLinkService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public CabinetMvcController(FolderService folderService,
                                FileService fileService,
                                FileContentStorageService fileContentStorageService,
                                FolderArchiveService folderArchiveService,
                                FolderDeleteService folderDeleteService,
                                ShareLinkService shareLinkService,
                                UserService userService,
                                AuditLogService auditLogService) {
        this.folderService = folderService;
        this.fileService = fileService;
        this.fileContentStorageService = fileContentStorageService;
        this.folderArchiveService = folderArchiveService;
        this.folderDeleteService = folderDeleteService;
        this.shareLinkService = shareLinkService;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @ModelAttribute("folderDto")
    public FolderDto folderForm() {
        return new FolderDto();
    }

    @ModelAttribute("fileUploadDto")
    public FileUploadDto fileUploadForm() {
        return new FileUploadDto();
    }

    @ModelAttribute("changePasswordDto")
    public ChangePasswordDto changePasswordForm() {
        return new ChangePasswordDto();
    }

    @ModelAttribute("itemRenameDto")
    public ItemRenameDto itemRenameForm() {
        return new ItemRenameDto();
    }

    @ModelAttribute("itemMoveDto")
    public ItemMoveDto itemMoveForm() {
        return new ItemMoveDto();
    }

    @ModelAttribute("profileSecuritySettingsDto")
    public ProfileSecuritySettingsDto profileSecuritySettingsForm() {
        return new ProfileSecuritySettingsDto();
    }

    @ModelAttribute("shareLinkCreateDto")
    public ShareLinkCreateDto shareLinkForm() {
        ShareLinkCreateDto dto = new ShareLinkCreateDto();
        dto.setResourceType("FILE");
        dto.setAllowDownload(true);
        dto.setAllowPreview(true);
        dto.setExpiresInHours(24);
        dto.setExpiresUnlimited(false);
        return dto;
    }

    @GetMapping
    public String index(@RequestParam(name = "folderId", required = false) Long folderId,
                        @RequestParam(name = "view", required = false) String viewMode,
                        @RequestParam(name = "sort", required = false) String sortMode,
                        @RequestParam(name = "q", required = false) String searchQuery,
                        @RequestParam(name = "scope", required = false) String searchScope,
                        @RequestParam(name = "page", required = false) Integer page,
                        @RequestParam(name = "size", required = false) Integer pageSize,
                        @AuthenticationPrincipal DriveUserDetails userDetails,
                        Model model) {
        fillCabinetModel(model, userDetails, folderId, normalizeViewMode(viewMode), normalizeSortMode(sortMode), searchQuery, searchScope, page, pageSize);
        return "cabinet/index";
    }

    @GetMapping("profile")
    public String profile(@AuthenticationPrincipal DriveUserDetails userDetails, Model model) {
        fillProfileModel(model, userDetails);
        return "cabinet/profile";
    }

    @PostMapping("folders/add")
    public String addFolder(
            @Valid @ModelAttribute("folderDto") FolderDto folderDto,
            BindingResult bindingResult,
            @RequestParam(name = "view", required = false) String viewMode,
            @RequestParam(name = "sort", required = false) String sortMode,
            @RequestParam(name = "q", required = false) String searchQuery,
            @RequestParam(name = "scope", required = false) String searchScope,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer pageSize,
            @AuthenticationPrincipal DriveUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Long ownerId = userDetails.getUser().getId();
        folderDto.setOwnerId(ownerId);
        Long currentFolderId = folderDto.getParentId();
        folderDto.setPathKey(buildPathKey(ownerId, currentFolderId, folderDto.getName()));
        folderDto.setSortOrder(0);

        if (bindingResult.hasErrors()) {
            fillCabinetModel(model, userDetails, currentFolderId, normalizeViewMode(viewMode), normalizeSortMode(sortMode), searchQuery, searchScope, page, pageSize);
            return "cabinet/index";
        }

        FolderDto saved = folderService.save(folderDto);
        if (saved == null) {
            fillCabinetModel(model, userDetails, currentFolderId, normalizeViewMode(viewMode), normalizeSortMode(sortMode), searchQuery, searchScope, page, pageSize);
            if (folderService.existsByOwnerIdAndParentIdAndName(ownerId, currentFolderId, folderDto.getName())) {
                model.addAttribute("folderError", "Папка с таким названием уже существует.");
            } else {
                model.addAttribute("folderError", "Не удалось создать папку. Подробности смотрите в логе приложения.");
            }
            return "cabinet/index";
        }

        auditLogService.log(ownerId, "FOLDER_CREATE", "FOLDER", saved.getId(), "Создана папка " + saved.getName());
        redirectAttributes.addFlashAttribute("folderSuccess", "Папка создана.");
        return redirectCabinet(currentFolderId, viewMode, sortMode, searchQuery, searchScope, page, pageSize);
    }

    @PostMapping("files/upload")
    public String uploadFile(@ModelAttribute("fileUploadDto") FileUploadDto fileUploadDto,
                             @RequestParam(name = "view", required = false) String viewMode,
                             @RequestParam(name = "sort", required = false) String sortMode,
                             @RequestParam(name = "q", required = false) String searchQuery,
                             @RequestParam(name = "scope", required = false) String searchScope,
                             @RequestParam(name = "page", required = false) Integer page,
                             @RequestParam(name = "size", required = false) Integer pageSize,
                             @AuthenticationPrincipal DriveUserDetails userDetails,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        String uploadError = fileService.validateUpload(userDetails.getUser().getId(), fileUploadDto);
        if (uploadError != null) {
            fillCabinetModel(model, userDetails, fileUploadDto.getFolderId(), normalizeViewMode(viewMode), normalizeSortMode(sortMode), searchQuery, searchScope, page, pageSize);
            model.addAttribute("fileError", uploadError);
            return "cabinet/index";
        }

        FileItemDto saved = fileService.upload(userDetails.getUser().getId(), fileUploadDto);
        if (saved == null) {
            fillCabinetModel(model, userDetails, fileUploadDto.getFolderId(), normalizeViewMode(viewMode), normalizeSortMode(sortMode), searchQuery, searchScope, page, pageSize);
            model.addAttribute("fileError", "Не удалось загрузить файл. Проверьте папку и содержимое файла.");
            return "cabinet/index";
        }

        auditLogService.log(userDetails.getUser().getId(), "FILE_UPLOAD", "FILE", saved.getId(), "Загружен файл " + saved.getOriginalFilename());
        redirectAttributes.addFlashAttribute("fileSuccess", "Файл загружен: " + saved.getOriginalFilename());
        return redirectCabinet(saved.getFolderId(), viewMode, sortMode, searchQuery, searchScope, page, pageSize);
    }

    @PostMapping("password/change")
    public String changePassword(@ModelAttribute("changePasswordDto") ChangePasswordDto changePasswordDto,
                                 @AuthenticationPrincipal DriveUserDetails userDetails,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        String error = userService.changePassword(
                userDetails.getUser().getId(),
                changePasswordDto.getCurrentPassword(),
                changePasswordDto.getNewPassword(),
                changePasswordDto.getConfirmPassword()
        );

        if (error != null) {
            model.addAttribute("passwordError", error);
            fillProfileModel(model, userDetails);
            return "cabinet/profile";
        }

        redirectAttributes.addFlashAttribute("passwordSuccess", "Пароль обновлен.");
        return "redirect:/cabinet/profile";
    }

    @PostMapping("profile/security")
    public String updateProfileSecurity(@ModelAttribute("profileSecuritySettingsDto") ProfileSecuritySettingsDto profileSecuritySettingsDto,
                                        @AuthenticationPrincipal DriveUserDetails userDetails,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        String error = userService.updateProfileSecuritySettings(userDetails.getUser().getId(), profileSecuritySettingsDto);
        if (error != null) {
            fillProfileModel(model, userDetails);
            model.addAttribute("profileSecurityError", error);
            model.addAttribute("profileSecuritySettingsDto", profileSecuritySettingsDto);
            return "cabinet/profile";
        }

        redirectAttributes.addFlashAttribute("profileSecuritySuccess", "Настройки профиля и 2FA обновлены.");
        return "redirect:/cabinet/profile";
    }

    @GetMapping("files/{id}/download")
    public ResponseEntity<?> downloadFile(@PathVariable("id") Long id,
                                          @AuthenticationPrincipal DriveUserDetails userDetails) {
        FileItemDto fileItemDto = fileService.findByIdAndOwnerId(id, userDetails.getUser().getId());
        if (fileItemDto == null) {
            return ResponseEntity.notFound().build();
        }

        var path = fileContentStorageService.resolveExistingPath(fileItemDto.getStorageName());
        if (path == null) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (fileItemDto.getMimeType() != null && !fileItemDto.getMimeType().isBlank()) {
            mediaType = MediaType.parseMediaType(fileItemDto.getMimeType());
        }

        return MediaResponseSupport.buildPathResponse(
                path,
                mediaType,
                fileItemDto.getOriginalFilename(),
                false,
                null
        );
    }

    @GetMapping("folders/{id}/download")
    public ResponseEntity<byte[]> downloadFolder(@PathVariable("id") Long id,
                                                 @AuthenticationPrincipal DriveUserDetails userDetails) {
        FolderDto folderDto = folderService.findByIdAndOwnerId(id, userDetails.getUser().getId());
        if (folderDto == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] content = folderArchiveService.buildFolderArchive(folderDto);
        if (content == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(content.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(folderDto.getName() + ".zip", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(content);
    }

    @GetMapping("files/{id}/content")
    public ResponseEntity<?> openFileContent(@PathVariable("id") Long id,
                                             @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader,
                                             @AuthenticationPrincipal DriveUserDetails userDetails) {
        FileItemDto fileItemDto = fileService.findByIdAndOwnerId(id, userDetails.getUser().getId());
        if (fileItemDto == null) {
            return ResponseEntity.notFound().build();
        }

        var path = fileContentStorageService.resolveExistingPath(fileItemDto.getStorageName());
        if (path == null) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (fileItemDto.getMimeType() != null && !fileItemDto.getMimeType().isBlank()) {
            mediaType = MediaType.parseMediaType(fileItemDto.getMimeType());
        }

        return MediaResponseSupport.buildPathResponse(
                path,
                mediaType,
                fileItemDto.getOriginalFilename(),
                true,
                rangeHeader
        );
    }

    @GetMapping("files/{id}/thumbnail")
    public ResponseEntity<byte[]> openFileThumbnail(@PathVariable("id") Long id,
                                                    @AuthenticationPrincipal DriveUserDetails userDetails) {
        FileItemDto fileItemDto = fileService.findByIdAndOwnerId(id, userDetails.getUser().getId());
        if (fileItemDto == null || !fileItemDto.isImagePreview()) {
            return ResponseEntity.notFound().build();
        }

        byte[] content = fileService.findThumbnailBytesByFileId(id);
        if (content == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .contentLength(content.length)
                .body(content);
    }

    @PostMapping("files/{id}/delete")
    public String deleteFile(@PathVariable("id") Long id,
                             @RequestParam(name = "currentFolderId", required = false) Long currentFolderId,
                             @RequestParam(name = "view", required = false) String viewMode,
                             @RequestParam(name = "sort", required = false) String sortMode,
                             @RequestParam(name = "q", required = false) String searchQuery,
                             @RequestParam(name = "scope", required = false) String searchScope,
                             @RequestParam(name = "page", required = false) Integer page,
                             @RequestParam(name = "size", required = false) Integer pageSize,
                             @AuthenticationPrincipal DriveUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        FileItemDto fileItemDto = fileService.findByIdAndOwnerId(id, userDetails.getUser().getId());
        String error = fileService.deleteByIdAndOwnerId(id, userDetails.getUser().getId());
        if (error != null) {
            redirectAttributes.addFlashAttribute("fileError", error);
        } else {
            if (fileItemDto != null) {
                auditLogService.log(userDetails.getUser().getId(), "FILE_DELETE", "FILE", id, "Удален файл " + fileItemDto.getOriginalFilename());
            }
            redirectAttributes.addFlashAttribute("fileSuccess", "Файл удален.");
        }
        return redirectCabinet(currentFolderId, viewMode, sortMode, searchQuery, searchScope, page, pageSize);
    }

    @PostMapping("folders/{id}/delete")
    public String deleteFolder(@PathVariable("id") Long id,
                               @RequestParam(name = "currentFolderId", required = false) Long currentFolderId,
                               @RequestParam(name = "view", required = false) String viewMode,
                               @RequestParam(name = "sort", required = false) String sortMode,
                               @RequestParam(name = "q", required = false) String searchQuery,
                               @RequestParam(name = "scope", required = false) String searchScope,
                               @RequestParam(name = "page", required = false) Integer page,
                               @RequestParam(name = "size", required = false) Integer pageSize,
                               @AuthenticationPrincipal DriveUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        FolderDto folderDto = folderService.findByIdAndOwnerId(id, userDetails.getUser().getId());
        String error = folderDeleteService.deleteByIdAndOwnerId(id, userDetails.getUser().getId());
        if (error != null) {
            redirectAttributes.addFlashAttribute("folderError", error);
        } else {
            if (folderDto != null) {
                auditLogService.log(userDetails.getUser().getId(), "FOLDER_DELETE", "FOLDER", id, "Удалена папка " + folderDto.getName());
            }
            redirectAttributes.addFlashAttribute("folderSuccess", "Папка удалена.");
        }
        return redirectCabinet(currentFolderId, viewMode, sortMode, searchQuery, searchScope, page, pageSize);
    }

    @PostMapping("items/delete")
    public String deleteItem(@RequestParam(name = "resourceType", required = false) String resourceType,
                             @RequestParam(name = "resourceId", required = false) Long resourceId,
                             @RequestParam(name = "currentFolderId", required = false) Long currentFolderId,
                             @RequestParam(name = "view", required = false) String viewMode,
                             @RequestParam(name = "sort", required = false) String sortMode,
                             @RequestParam(name = "q", required = false) String searchQuery,
                             @RequestParam(name = "scope", required = false) String searchScope,
                             @RequestParam(name = "page", required = false) Integer page,
                             @RequestParam(name = "size", required = false) Integer pageSize,
                             @AuthenticationPrincipal DriveUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        if (resourceType == null || resourceType.isBlank() || resourceId == null) {
            redirectAttributes.addFlashAttribute("fileError", "Не выбран объект для удаления.");
            return redirectCabinet(currentFolderId, viewMode, sortMode, searchQuery, searchScope, page, pageSize);
        }

        String normalizedType = resourceType.trim().toUpperCase();
        String error;
        if ("FOLDER".equals(normalizedType)) {
            FolderDto folderDto = folderService.findByIdAndOwnerId(resourceId, userDetails.getUser().getId());
            error = folderDeleteService.deleteByIdAndOwnerId(resourceId, userDetails.getUser().getId());
            if (error != null) {
                redirectAttributes.addFlashAttribute("folderError", error);
            } else {
                if (folderDto != null) {
                    auditLogService.log(userDetails.getUser().getId(), "FOLDER_DELETE", "FOLDER", resourceId, "Удалена папка " + folderDto.getName());
                }
                redirectAttributes.addFlashAttribute("folderSuccess", "Папка удалена.");
            }
        } else if ("FILE".equals(normalizedType)) {
            FileItemDto fileItemDto = fileService.findByIdAndOwnerId(resourceId, userDetails.getUser().getId());
            error = fileService.deleteByIdAndOwnerId(resourceId, userDetails.getUser().getId());
            if (error != null) {
                redirectAttributes.addFlashAttribute("fileError", error);
            } else {
                if (fileItemDto != null) {
                    auditLogService.log(userDetails.getUser().getId(), "FILE_DELETE", "FILE", resourceId, "Удален файл " + fileItemDto.getOriginalFilename());
                }
                redirectAttributes.addFlashAttribute("fileSuccess", "Файл удален.");
            }
        } else {
            redirectAttributes.addFlashAttribute("fileError", "Неизвестный тип объекта.");
        }

        return redirectCabinet(currentFolderId, viewMode, sortMode, searchQuery, searchScope, page, pageSize);
    }

    @PostMapping("items/rename")
    public String renameItem(@Valid @ModelAttribute("itemRenameDto") ItemRenameDto itemRenameDto,
                             BindingResult bindingResult,
                             @RequestParam(name = "currentFolderId", required = false) Long currentFolderId,
                             @RequestParam(name = "view", required = false) String viewMode,
                             @RequestParam(name = "sort", required = false) String sortMode,
                             @RequestParam(name = "q", required = false) String searchQuery,
                             @RequestParam(name = "scope", required = false) String searchScope,
                             @RequestParam(name = "page", required = false) Integer page,
                             @RequestParam(name = "size", required = false) Integer pageSize,
                             @AuthenticationPrincipal DriveUserDetails userDetails,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            fillCabinetModel(model, userDetails, currentFolderId, normalizeViewMode(viewMode), normalizeSortMode(sortMode), searchQuery, searchScope, page, pageSize);
            prepareItemModalState(model, "item-rename-modal", itemRenameDto.getResourceType(), itemRenameDto.getResourceId());
            return "cabinet/index";
        }

        String resourceType = itemRenameDto.getResourceType() == null ? "" : itemRenameDto.getResourceType().trim().toUpperCase();
        String error;
        String successMessage;
        if ("FOLDER".equals(resourceType)) {
            error = folderService.renameByIdAndOwnerId(itemRenameDto.getResourceId(), userDetails.getUser().getId(), itemRenameDto.getNewName());
            successMessage = "Папка переименована.";
            if (error != null) {
                redirectAttributes.addFlashAttribute("folderError", error);
            }
        } else if ("FILE".equals(resourceType)) {
            error = fileService.renameByIdAndOwnerId(itemRenameDto.getResourceId(), userDetails.getUser().getId(), itemRenameDto.getNewName());
            successMessage = "Файл переименован.";
            if (error != null) {
                redirectAttributes.addFlashAttribute("fileError", error);
            }
        } else {
            error = "Неизвестный тип объекта.";
            successMessage = null;
        }

        if (error != null) {
            fillCabinetModel(model, userDetails, currentFolderId, normalizeViewMode(viewMode), normalizeSortMode(sortMode), searchQuery, searchScope, page, pageSize);
            model.addAttribute("renameError", error);
            prepareItemModalState(model, "item-rename-modal", itemRenameDto.getResourceType(), itemRenameDto.getResourceId());
            return "cabinet/index";
        }

        auditLogService.log(userDetails.getUser().getId(), "ITEM_RENAME", resourceType, itemRenameDto.getResourceId(),
                "Переименование в " + itemRenameDto.getNewName());
        if ("FOLDER".equals(resourceType)) {
            redirectAttributes.addFlashAttribute("folderSuccess", successMessage);
        } else {
            redirectAttributes.addFlashAttribute("fileSuccess", successMessage);
        }
        return redirectCabinet(currentFolderId, viewMode, sortMode, searchQuery, searchScope, page, pageSize);
    }

    @PostMapping("items/move")
    public String moveItem(@Valid @ModelAttribute("itemMoveDto") ItemMoveDto itemMoveDto,
                           BindingResult bindingResult,
                           @RequestParam(name = "currentFolderId", required = false) Long currentFolderId,
                           @RequestParam(name = "view", required = false) String viewMode,
                           @RequestParam(name = "sort", required = false) String sortMode,
                           @RequestParam(name = "q", required = false) String searchQuery,
                           @RequestParam(name = "scope", required = false) String searchScope,
                           @RequestParam(name = "page", required = false) Integer page,
                           @RequestParam(name = "size", required = false) Integer pageSize,
                           @AuthenticationPrincipal DriveUserDetails userDetails,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            fillCabinetModel(model, userDetails, currentFolderId, normalizeViewMode(viewMode), normalizeSortMode(sortMode), searchQuery, searchScope, page, pageSize);
            prepareItemModalState(model, "item-move-modal", itemMoveDto.getResourceType(), itemMoveDto.getResourceId());
            return "cabinet/index";
        }

        String resourceType = itemMoveDto.getResourceType() == null ? "" : itemMoveDto.getResourceType().trim().toUpperCase();
        String error;
        String successMessage;
        if ("FOLDER".equals(resourceType)) {
            error = folderService.moveByIdAndOwnerId(itemMoveDto.getResourceId(), userDetails.getUser().getId(), itemMoveDto.getTargetFolderId());
            successMessage = "Папка перемещена.";
        } else if ("FILE".equals(resourceType)) {
            error = fileService.moveByIdAndOwnerId(itemMoveDto.getResourceId(), userDetails.getUser().getId(), itemMoveDto.getTargetFolderId());
            successMessage = "Файл перемещен.";
        } else {
            error = "Неизвестный тип объекта.";
            successMessage = null;
        }

        if (error != null) {
            fillCabinetModel(model, userDetails, currentFolderId, normalizeViewMode(viewMode), normalizeSortMode(sortMode), searchQuery, searchScope, page, pageSize);
            model.addAttribute("moveError", error);
            prepareItemModalState(model, "item-move-modal", itemMoveDto.getResourceType(), itemMoveDto.getResourceId());
            return "cabinet/index";
        }

        auditLogService.log(userDetails.getUser().getId(), "ITEM_MOVE", resourceType, itemMoveDto.getResourceId(),
                "Перемещен в папку " + itemMoveDto.getTargetFolderId());
        if ("FOLDER".equals(resourceType)) {
            redirectAttributes.addFlashAttribute("folderSuccess", successMessage);
        } else {
            redirectAttributes.addFlashAttribute("fileSuccess", successMessage);
        }
        return redirectCabinet(currentFolderId, viewMode, sortMode, searchQuery, searchScope, page, pageSize);
    }

    @PostMapping("items/bulk/delete")
    public String bulkDelete(@RequestParam(name = "currentFolderId", required = false) Long currentFolderId,
                             @RequestParam(name = "view", required = false) String viewMode,
                             @RequestParam(name = "sort", required = false) String sortMode,
                             @RequestParam(name = "q", required = false) String searchQuery,
                             @RequestParam(name = "scope", required = false) String searchScope,
                             @RequestParam(name = "page", required = false) Integer page,
                             @RequestParam(name = "size", required = false) Integer pageSize,
                             @RequestParam(name = "selectedFileIds", required = false) String selectedFileIds,
                             @RequestParam(name = "selectedFolderIds", required = false) String selectedFolderIds,
                             @AuthenticationPrincipal DriveUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        List<Long> fileIds = parseIds(selectedFileIds);
        List<Long> folderIds = parseIds(selectedFolderIds);
        if (fileIds.isEmpty() && folderIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("fileError", "Не выбраны объекты для удаления.");
            return redirectCabinet(currentFolderId, viewMode, sortMode, searchQuery, searchScope, page, pageSize);
        }

        int deletedFiles = 0;
        int deletedFolders = 0;
        int failedItems = 0;
        for (Long fileId : fileIds) {
            FileItemDto file = fileService.findByIdAndOwnerId(fileId, userDetails.getUser().getId());
            if (file == null) {
                failedItems++;
                continue;
            }
            String error = fileService.deleteByIdAndOwnerId(fileId, userDetails.getUser().getId());
            if (error == null) {
                deletedFiles++;
                auditLogService.log(userDetails.getUser().getId(), "FILE_DELETE", "FILE", fileId, "Массовое удаление файла " + file.getOriginalFilename());
            } else {
                failedItems++;
            }
        }
        folderIds.sort(Comparator.comparingLong(Long::longValue));
        for (Long folderId : folderIds) {
            FolderDto folder = folderService.findByIdAndOwnerId(folderId, userDetails.getUser().getId());
            if (folder == null) {
                failedItems++;
                continue;
            }
            String error = folderDeleteService.deleteByIdAndOwnerId(folderId, userDetails.getUser().getId());
            if (error == null) {
                deletedFolders++;
                auditLogService.log(userDetails.getUser().getId(), "FOLDER_DELETE", "FOLDER", folderId, "Массовое удаление папки " + folder.getName());
            } else {
                failedItems++;
            }
        }
        redirectAttributes.addFlashAttribute("fileSuccess", "Удалено файлов: " + deletedFiles + ", папок: " + deletedFolders + ".");
        if (failedItems > 0) {
            redirectAttributes.addFlashAttribute("fileError", "Не удалось обработать объектов: " + failedItems + ".");
        }
        return redirectCabinet(currentFolderId, viewMode, sortMode, searchQuery, searchScope, page, pageSize);
    }

    @PostMapping("items/bulk/move")
    public String bulkMove(@RequestParam(name = "currentFolderId", required = false) Long currentFolderId,
                           @RequestParam(name = "view", required = false) String viewMode,
                           @RequestParam(name = "sort", required = false) String sortMode,
                           @RequestParam(name = "q", required = false) String searchQuery,
                           @RequestParam(name = "scope", required = false) String searchScope,
                           @RequestParam(name = "page", required = false) Integer page,
                           @RequestParam(name = "size", required = false) Integer pageSize,
                           @RequestParam(name = "targetFolderId", required = false) Long targetFolderId,
                           @RequestParam(name = "selectedFileIds", required = false) String selectedFileIds,
                           @RequestParam(name = "selectedFolderIds", required = false) String selectedFolderIds,
                           @AuthenticationPrincipal DriveUserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        List<Long> fileIds = parseIds(selectedFileIds);
        List<Long> folderIds = parseIds(selectedFolderIds);
        if (fileIds.isEmpty() && folderIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("fileError", "Не выбраны объекты для перемещения.");
            return redirectCabinet(currentFolderId, viewMode, sortMode, searchQuery, searchScope, page, pageSize);
        }

        int movedFiles = 0;
        int movedFolders = 0;
        int failedItems = 0;
        for (Long fileId : fileIds) {
            FileItemDto file = fileService.findByIdAndOwnerId(fileId, userDetails.getUser().getId());
            if (file == null) {
                failedItems++;
                continue;
            }
            String error = fileService.moveByIdAndOwnerId(fileId, userDetails.getUser().getId(), targetFolderId);
            if (error == null) {
                movedFiles++;
                auditLogService.log(userDetails.getUser().getId(), "ITEM_MOVE", "FILE", fileId, "Массовое перемещение файла " + file.getOriginalFilename());
            } else {
                failedItems++;
            }
        }
        for (Long folderId : folderIds) {
            FolderDto folder = folderService.findByIdAndOwnerId(folderId, userDetails.getUser().getId());
            if (folder == null) {
                failedItems++;
                continue;
            }
            String error = folderService.moveByIdAndOwnerId(folderId, userDetails.getUser().getId(), targetFolderId);
            if (error == null) {
                movedFolders++;
                auditLogService.log(userDetails.getUser().getId(), "ITEM_MOVE", "FOLDER", folderId, "Массовое перемещение папки " + folder.getName());
            } else {
                failedItems++;
            }
        }
        redirectAttributes.addFlashAttribute("fileSuccess", "Перемещено файлов: " + movedFiles + ", папок: " + movedFolders + ".");
        if (failedItems > 0) {
            redirectAttributes.addFlashAttribute("fileError", "Не удалось обработать объектов: " + failedItems + ".");
        }
        return redirectCabinet(currentFolderId, viewMode, sortMode, searchQuery, searchScope, page, pageSize);
    }

    @PostMapping("shares")
    public String createShare(@ModelAttribute("shareLinkCreateDto") ShareLinkCreateDto shareLinkCreateDto,
                              @RequestParam(name = "currentFolderId", required = false) Long currentFolderId,
                              @RequestParam(name = "resourceType", required = false) String resourceType,
                              @RequestParam(name = "resourceId", required = false) Long resourceId,
                              @AuthenticationPrincipal DriveUserDetails userDetails,
                              HttpServletRequest request,
                              Model model,
                              @RequestParam(name = "view", required = false) String viewMode,
                              @RequestParam(name = "sort", required = false) String sortMode,
                              @RequestParam(name = "q", required = false) String searchQuery,
                              @RequestParam(name = "scope", required = false) String searchScope,
                              @RequestParam(name = "page", required = false) Integer page,
                              @RequestParam(name = "size", required = false) Integer pageSize,
                              RedirectAttributes redirectAttributes) {
        if ((shareLinkCreateDto.getResourceType() == null || shareLinkCreateDto.getResourceType().isBlank()) && resourceType != null) {
            shareLinkCreateDto.setResourceType(resourceType);
        }
        if (shareLinkCreateDto.getResourceId() == null && resourceId != null) {
            shareLinkCreateDto.setResourceId(resourceId);
        }

        String validationError = shareLinkService.validateShareLinkCreate(userDetails.getUser().getId(), shareLinkCreateDto);
        if (validationError != null) {
            fillCabinetModel(model, userDetails, currentFolderId, normalizeViewMode(viewMode), normalizeSortMode(sortMode), searchQuery, searchScope, page, pageSize);
            model.addAttribute("shareError", validationError);
            return "cabinet/index";
        }

        ShareLinkDto shareLink = shareLinkService.createShareLink(userDetails.getUser().getId(), shareLinkCreateDto);
        if (shareLink == null) {
            fillCabinetModel(model, userDetails, currentFolderId, normalizeViewMode(viewMode), normalizeSortMode(sortMode), searchQuery, searchScope, page, pageSize);
            model.addAttribute("shareError", "Не удалось создать публичную ссылку. Проверьте тип ресурса и данные формы.");
            return "cabinet/index";
        }

        String shareUrl = buildShareUrl(request, shareLink.getToken());
        auditLogService.log(userDetails.getUser().getId(), "SHARE_CREATE", shareLink.getResourceType(), shareLink.getResourceId(),
                "Создана публичная ссылка " + shareLink.getToken());
        redirectAttributes.addFlashAttribute("shareSuccess", "Публичная ссылка создана.");
        redirectAttributes.addFlashAttribute("shareUrl", shareUrl);
        redirectAttributes.addFlashAttribute("shareOpenResourceId", shareLink.getResourceId());
        redirectAttributes.addFlashAttribute("shareOpenResourceType", shareLink.getResourceType());
        return redirectCabinet(currentFolderId, viewMode, sortMode, searchQuery, searchScope, page, pageSize);
    }

    @PostMapping("shares/delete")
    public String deleteShare(@RequestParam(name = "currentFolderId", required = false) Long currentFolderId,
                              @RequestParam(name = "resourceType", required = false) String resourceType,
                              @RequestParam(name = "resourceId", required = false) Long resourceId,
                              @RequestParam(name = "view", required = false) String viewMode,
                              @RequestParam(name = "sort", required = false) String sortMode,
                              @RequestParam(name = "q", required = false) String searchQuery,
                              @RequestParam(name = "scope", required = false) String searchScope,
                              @RequestParam(name = "page", required = false) Integer page,
                              @RequestParam(name = "size", required = false) Integer pageSize,
                              @AuthenticationPrincipal DriveUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        String error = shareLinkService.deleteShareLink(userDetails.getUser().getId(), resourceType, resourceId);
        if (error != null) {
            redirectAttributes.addFlashAttribute("shareError", error);
        } else {
            auditLogService.log(userDetails.getUser().getId(), "SHARE_DELETE", resourceType, resourceId, "Удалена публичная ссылка");
            redirectAttributes.addFlashAttribute("shareSuccess", "Публичная ссылка удалена.");
        }
        return redirectCabinet(currentFolderId, viewMode, sortMode, searchQuery, searchScope, page, pageSize);
    }

    private void fillCabinetModel(Model model, DriveUserDetails userDetails, Long currentFolderId, String viewMode, String sortMode,
                                  String searchQuery, String searchScope, Integer page, Integer pageSize) {
        CabinetViewState viewState = buildViewState(viewMode, sortMode, searchQuery, searchScope, page, pageSize);
        Long ownerId = userDetails.getUser().getId();
        FolderDto currentFolder = currentFolderId == null ? null : folderService.findByIdAndOwnerId(currentFolderId, ownerId);
        long filesSize = fileService.sumSizeByOwnerId(ownerId);
        var currentUser = userService.findById(ownerId);
        long storageQuotaBytes = currentUser.getStorageQuotaBytes() == null ? 0L : currentUser.getStorageQuotaBytes();
        int storageUsagePercent = storageQuotaBytes <= 0
                ? 0
                : (int) Math.min(100L, Math.round((filesSize * 100.0d) / storageQuotaBytes));
        Long scopeFolderId = currentFolder == null ? null : currentFolder.getId();
        String scopeFolderPath = currentFolder == null ? null : currentFolder.getPathKey();
        long folderCount = folderService.countSearchByOwnerId(ownerId, viewState.searchQuery(), scopeFolderId, scopeFolderPath, viewState.searchScope());
        long fileCount = fileService.countSearchByOwnerId(ownerId, viewState.searchQuery(), scopeFolderId, scopeFolderPath, viewState.searchScope());
        CabinetPage cabinetPage = paginateCabinetEntries(
                folderCount,
                fileCount,
                viewState.page(),
                viewState.pageSize()
        );
        List<FolderDto> folders = folderService.searchByOwnerId(
                ownerId,
                viewState.searchQuery(),
                scopeFolderId,
                scopeFolderPath,
                viewState.searchScope(),
                viewState.sortMode(),
                cabinetPage.folderOffset(),
                cabinetPage.folderLimit()
        );
        List<FileItemDto> files = fileService.searchByOwnerId(
                ownerId,
                viewState.searchQuery(),
                scopeFolderId,
                scopeFolderPath,
                viewState.searchScope(),
                viewState.sortMode(),
                cabinetPage.fileOffset(),
                cabinetPage.fileLimit()
        );
        model.addAttribute("title", "AGTY/DRIVE");
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("folders", folders);
        model.addAttribute("foldersCount", folderService.countByOwnerId(ownerId));
        model.addAttribute("files", files);
        model.addAttribute("folderShareLinksByFolderId", shareLinkService.findLatestFolderShareLinks(folders));
        model.addAttribute("shareLinksByFileId", shareLinkService.findLatestFileShareLinks(files));
        model.addAttribute("filesSizeTitle", org.agty.utils.AgtyUtils.filesizeToTitle(filesSize, "ru"));
        model.addAttribute("storageQuotaTitle", currentUser.getStorageQuotaTitle());
        model.addAttribute("storageUsagePercent", storageUsagePercent);
        model.addAttribute("currentFolder", currentFolder);
        model.addAttribute("currentFolderId", currentFolderId);
        model.addAttribute("breadcrumbs", buildBreadcrumbs(ownerId, currentFolder));
        model.addAttribute("moveFolderOptions", folderService.buildMoveOptions(ownerId));
        model.addAttribute("uploadFolderOptions", folderService.buildMoveOptions(ownerId));
        model.addAttribute("viewMode", viewState.viewMode());
        model.addAttribute("sortMode", viewState.sortMode());
        model.addAttribute("searchQuery", viewState.searchQuery());
        model.addAttribute("searchScope", viewState.searchScope());
        model.addAttribute("currentPage", cabinetPage.currentPage());
        model.addAttribute("pageSize", cabinetPage.pageSize());
        model.addAttribute("totalPages", cabinetPage.totalPages());
        model.addAttribute("totalItems", cabinetPage.totalItems());
        model.addAttribute("pageSizeOptions", List.of(20, 50, 100));
        model.addAttribute("searchActive", !viewState.searchQuery().isBlank());
    }

    private List<FolderDto> buildBreadcrumbs(Long ownerId, FolderDto currentFolder) {
        LinkedList<FolderDto> breadcrumbs = new LinkedList<>();
        FolderDto pointer = currentFolder;
        while (pointer != null) {
            breadcrumbs.addFirst(pointer);
            pointer = pointer.getParentId() == null ? null : folderService.findByIdAndOwnerId(pointer.getParentId(), ownerId);
        }
        return breadcrumbs;
    }

    private String buildPathKey(Long ownerId, Long parentId, String folderName) {
        String slug = folderName == null ? "" : folderName.trim().toLowerCase().replace(' ', '-');
        if (parentId == null) {
            return "/" + slug;
        }

        FolderDto parentFolder = folderService.findByIdAndOwnerId(parentId, ownerId);
        if (parentFolder == null || parentFolder.getPathKey() == null || parentFolder.getPathKey().isBlank()) {
            return "/" + slug;
        }

        return parentFolder.getPathKey() + "/" + slug;
    }

    private void fillProfileModel(Model model, DriveUserDetails userDetails) {
        Long ownerId = userDetails.getUser().getId();
        model.addAttribute("title", "AGTY/DRIVE Profile");
        model.addAttribute("currentUser", userService.findById(ownerId));
        model.addAttribute("profileSecuritySettingsDto", userService.getProfileSecuritySettings(ownerId));
    }

    private void prepareItemModalState(Model model, String modalName, String resourceType, Long resourceId) {
        model.addAttribute("itemOpenModal", modalName);
        model.addAttribute("itemOpenResourceType", resourceType);
        model.addAttribute("itemOpenResourceId", resourceId);
    }

    private String normalizeViewMode(String value) {
        return "grid".equalsIgnoreCase(value) ? "grid" : "list";
    }

    private String normalizeSortMode(String value) {
        if (value == null) {
            return "name_asc";
        }
        return switch (value.trim().toLowerCase()) {
            case "name_desc", "date_newest", "date_oldest", "size_desc", "size_asc", "type_asc" -> value.trim().toLowerCase();
            default -> "name_asc";
        };
    }

    private String redirectCabinet(Long folderId, String viewMode, String sortMode, String searchQuery, String searchScope, Integer page, Integer pageSize) {
        return redirectCabinet(folderId, buildViewState(viewMode, sortMode, searchQuery, searchScope, page, pageSize));
    }

    private String redirectCabinet(Long folderId, CabinetViewState viewState) {
        StringBuilder builder = new StringBuilder("redirect:/cabinet");
        List<String> params = new ArrayList<>();
        if (folderId != null) {
            params.add("folderId=" + folderId);
        }
        if (!"list".equals(viewState.viewMode())) {
            params.add("view=" + viewState.viewMode());
        }
        if (!"name_asc".equals(viewState.sortMode())) {
            params.add("sort=" + viewState.sortMode());
        }
        if (!viewState.searchQuery().isBlank()) {
            params.add("q=" + viewState.searchQuery());
        }
        if (!"current".equals(viewState.searchScope())) {
            params.add("scope=" + viewState.searchScope());
        }
        if (viewState.page() > 1) {
            params.add("page=" + viewState.page());
        }
        if (viewState.pageSize() != 20) {
            params.add("size=" + viewState.pageSize());
        }
        if (!params.isEmpty()) {
            builder.append("?").append(String.join("&", params));
        }
        return builder.toString();
    }

    private String normalizeSearchQuery(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        return normalized.isBlank() ? "" : normalized;
    }

    private int normalizePage(Integer value) {
        return value == null || value < 1 ? 1 : value;
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

    private String normalizeSearchScope(String value) {
        if (value == null) {
            return "current";
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "tree", "all" -> value.trim().toLowerCase(Locale.ROOT);
            default -> "current";
        };
    }

    private List<Long> parseIds(String value) {
        List<Long> result = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return result;
        }
        for (String item : value.split(",")) {
            String normalized = item == null ? "" : item.trim();
            if (normalized.isBlank()) {
                continue;
            }
            try {
                result.add(Long.parseLong(normalized));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private String buildShareUrl(HttpServletRequest request, String token) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.getScheme()).append("://").append(request.getServerName());
        boolean standardPort = ("http".equalsIgnoreCase(request.getScheme()) && request.getServerPort() == 80)
                || ("https".equalsIgnoreCase(request.getScheme()) && request.getServerPort() == 443);
        if (!standardPort) {
            builder.append(":").append(request.getServerPort());
        }
        if (request.getContextPath() != null && !request.getContextPath().isBlank()) {
            builder.append(request.getContextPath());
        }
        builder.append("/s/").append(token);
        return builder.toString();
    }

    private CabinetViewState buildViewState(String viewMode,
                                            String sortMode,
                                            String searchQuery,
                                            String searchScope,
                                            Integer page,
                                            Integer pageSize) {
        return new CabinetViewState(
                normalizeViewMode(viewMode),
                normalizeSortMode(sortMode),
                normalizeSearchQuery(searchQuery),
                normalizeSearchScope(searchScope),
                normalizePage(page),
                normalizePageSize(pageSize)
        );
    }

    private CabinetPage paginateCabinetEntries(long folderCount, long fileCount, int page, int pageSize) {
        int totalItems = Math.toIntExact(Math.max(0L, folderCount + fileCount));
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));
        int currentPage = Math.min(page, totalPages);
        int offset = Math.max(0, (currentPage - 1) * pageSize);

        int folderOffset = (int) Math.min(offset, folderCount);
        int folderLimit = (int) Math.min(Math.max(0L, folderCount - folderOffset), pageSize);
        int remaining = Math.max(0, pageSize - folderLimit);
        int fileOffset = (int) Math.max(0L, offset - folderCount);
        int fileLimit = remaining;

        return new CabinetPage(
                folderOffset,
                folderLimit,
                fileOffset,
                fileLimit,
                currentPage,
                totalPages,
                pageSize,
                totalItems
        );
    }

    private record CabinetViewState(String viewMode,
                                    String sortMode,
                                    String searchQuery,
                                    String searchScope,
                                    int page,
                                    int pageSize) {
    }

    private record CabinetPage(int folderOffset,
                               int folderLimit,
                               int fileOffset,
                               int fileLimit,
                               int currentPage,
                               int totalPages,
                               int pageSize,
                               int totalItems) {
    }
}
