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

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.agty.drive.dto.ChangePasswordDto;
import org.agty.drive.dto.CabinetPageStateDto;
import org.agty.drive.dto.CollaborativeAccessCreateDto;
import org.agty.drive.dto.CollaborativeAccessDto;
import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FileUploadDto;
import org.agty.drive.dto.FolderDto;
import org.agty.drive.dto.ItemMoveDto;
import org.agty.drive.dto.ItemPropertiesDto;
import org.agty.drive.dto.ItemRenameDto;
import org.agty.drive.dto.ProfileSecuritySettingsDto;
import org.agty.drive.dto.ShareLinkCreateDto;
import org.agty.drive.dto.ShareLinkDto;
import org.agty.drive.dto.SharedLibraryItemDto;
import org.agty.drive.security.service.DriveUserDetails;
import org.agty.drive.services.FileContentStorageService;
import org.agty.drive.services.FileService;
import org.agty.drive.services.FolderArchiveService;
import org.agty.drive.services.CollaborativeAccessService;
import org.agty.drive.services.FolderDeleteService;
import org.agty.drive.services.FolderService;
import org.agty.drive.services.MimeTypePolicyService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cabinet")
public class CabinetMvcController {

    private static final Logger log = LoggerFactory.getLogger(CabinetMvcController.class);

    private final FolderService folderService;
    private final FileService fileService;
    private final FileContentStorageService fileContentStorageService;
    private final FolderArchiveService folderArchiveService;
    private final FolderDeleteService folderDeleteService;
    private final CollaborativeAccessService collaborativeAccessService;
    private final ShareLinkService shareLinkService;
    private final UserService userService;
    private final MimeTypePolicyService mimeTypePolicyService;
    private final CabinetMvcSupport cabinetMvcSupport;

    public CabinetMvcController(FolderService folderService,
                                FileService fileService,
                                FileContentStorageService fileContentStorageService,
                                FolderArchiveService folderArchiveService,
                                FolderDeleteService folderDeleteService,
                                CollaborativeAccessService collaborativeAccessService,
                                ShareLinkService shareLinkService,
                                UserService userService,
                                MimeTypePolicyService mimeTypePolicyService,
                                CabinetMvcSupport cabinetMvcSupport) {
        this.folderService = folderService;
        this.fileService = fileService;
        this.fileContentStorageService = fileContentStorageService;
        this.folderArchiveService = folderArchiveService;
        this.folderDeleteService = folderDeleteService;
        this.collaborativeAccessService = collaborativeAccessService;
        this.shareLinkService = shareLinkService;
        this.userService = userService;
        this.mimeTypePolicyService = mimeTypePolicyService;
        this.cabinetMvcSupport = cabinetMvcSupport;
    }

    @PostMapping("folders/add")
    public String addFolder(
            @Valid @ModelAttribute("folderDto") FolderDto folderDto,
            BindingResult bindingResult,
            @ModelAttribute("cabinetState") CabinetPageStateDto cabinetState,
            @AuthenticationPrincipal DriveUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Long ownerId = userDetails.getUser().getId();
        folderDto.setOwnerId(ownerId);
        Long currentFolderId = folderDto.getParentId();
        cabinetState.setCurrentFolderId(currentFolderId);
        CabinetPageStateDto normalizedState = cabinetMvcSupport.normalizeState(cabinetState);
        folderDto.setPathKey(cabinetMvcSupport.buildPathKey(ownerId, currentFolderId, folderDto.getName()));
        folderDto.setSortOrder(0);
        String expirationError = folderService.validateExpirationInput(folderDto.getExpiresAt());

        if (bindingResult.hasErrors() || expirationError != null) {
            cabinetMvcSupport.fillCabinetModel(model, userDetails, normalizedState);
            if (expirationError != null) {
                model.addAttribute("folderError", expirationError);
            }
            return "cabinet/index";
        }

        folderService.normalizeExpiration(folderDto);

        FolderDto saved = folderService.save(folderDto);
        if (saved == null) {
            cabinetMvcSupport.fillCabinetModel(model, userDetails, normalizedState);
            if (folderService.existsByOwnerIdAndParentIdAndName(ownerId, currentFolderId, folderDto.getName())) {
                model.addAttribute("folderError", "Папка с таким названием уже существует.");
            } else {
                model.addAttribute("folderError", "Не удалось создать папку. Подробности смотрите в логе приложения.");
            }
            return "cabinet/index";
        }

        cabinetMvcSupport.log(ownerId, "FOLDER_CREATE", "FOLDER", saved.getId(), "Создана папка " + saved.getName());
        redirectAttributes.addFlashAttribute("folderSuccess", "Папка создана.");
        return cabinetMvcSupport.redirectCabinet(normalizedState);
    }

    @PostMapping("files/upload")
    public Object uploadFile(@ModelAttribute("fileUploadDto") FileUploadDto fileUploadDto,
                             @ModelAttribute("cabinetState") CabinetPageStateDto cabinetState,
                             @AuthenticationPrincipal DriveUserDetails userDetails,
                             Model model,
                             RedirectAttributes redirectAttributes,
                             HttpServletRequest request) {
        cabinetState.setCurrentFolderId(fileUploadDto.getFolderId());
        CabinetPageStateDto normalizedState = cabinetMvcSupport.normalizeState(cabinetState);
        String uploadError = fileService.validateUpload(userDetails.getUser().getId(), fileUploadDto);
        if (uploadError != null) {
            if (isAjaxUploadRequest(request)) {
                return ResponseEntity.badRequest().body(Map.of("error", uploadError));
            }
            cabinetMvcSupport.fillCabinetModel(model, userDetails, normalizedState);
            model.addAttribute("fileError", uploadError);
            return "cabinet/index";
        }

        FileItemDto saved;
        try {
            saved = fileService.upload(userDetails.getUser().getId(), fileUploadDto);
        } catch (RuntimeException exception) {
            return handleUploadException(exception, request, model, redirectAttributes, userDetails, normalizedState, false);
        }
        if (saved == null) {
            if (isAjaxUploadRequest(request)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Не удалось загрузить файл."));
            }
            cabinetMvcSupport.fillCabinetModel(model, userDetails, normalizedState);
            model.addAttribute("fileError", "Не удалось загрузить файл.");
            return "cabinet/index";
        }

        cabinetMvcSupport.log(userDetails.getUser().getId(), "FILE_UPLOAD", "FILE", saved.getId(), "Загружен файл " + saved.getOriginalFilename());
        String successMessage = "Файл загружен: " + saved.getOriginalFilename();
        normalizedState.setCurrentFolderId(saved.getFolderId());
        normalizedState.setPage(1);
        if (isAjaxUploadRequest(request)) {
            return ResponseEntity.ok(Map.of(
                    "redirectUrl", cabinetMvcSupport.redirectCabinet(normalizedState).replace("redirect:", ""),
                    "message", successMessage
            ));
        }
        redirectAttributes.addFlashAttribute("fileSuccess", successMessage);
        return cabinetMvcSupport.redirectCabinet(normalizedState);
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
            cabinetMvcSupport.fillProfileModel(model, userDetails);
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
            cabinetMvcSupport.fillProfileModel(model, userDetails);
            model.addAttribute("profileSecurityError", error);
            model.addAttribute("profileSecuritySettingsDto", profileSecuritySettingsDto);
            return "cabinet/profile";
        }

        redirectAttributes.addFlashAttribute("profileSecuritySuccess", "Настройки профиля и 2FA обновлены.");
        return "redirect:/cabinet/profile";
    }

    @PostMapping("collaborative/access")
    public String saveCollaborativeAccess(@ModelAttribute("collaborativeAccessCreateDto") CollaborativeAccessCreateDto dto,
                                          @RequestParam(value = "allowWrite", defaultValue = "false") boolean allowWrite,
                                          @RequestParam(value = "allowDelete", defaultValue = "false") boolean allowDelete,
                                          @ModelAttribute("cabinetState") CabinetPageStateDto cabinetState,
                                          @AuthenticationPrincipal DriveUserDetails userDetails,
                                          RedirectAttributes redirectAttributes) {
        dto.setAllowWrite(allowWrite);
        dto.setAllowDelete(allowDelete);
        CabinetPageStateDto normalizedState = cabinetMvcSupport.normalizeState(cabinetState);
        String error = collaborativeAccessService.saveFolderAccess(userDetails.getUser().getId(), dto);
        if (error != null) {
            redirectAttributes.addFlashAttribute("collaborativeError", error);
        } else if (dto.getLogins() == null || dto.getLogins().isBlank()) {
            redirectAttributes.addFlashAttribute("collaborativeSuccess", "Совместный доступ закрыт.");
        } else {
            redirectAttributes.addFlashAttribute("collaborativeSuccess", "Совместный доступ обновлен.");
        }
        return cabinetMvcSupport.redirectCabinet(normalizedState);
    }

    @PostMapping("collaborative/unlock")
    public String unlockCollaborative(@ModelAttribute("cabinetState") CabinetPageStateDto cabinetState,
                                      @RequestParam("password") String password,
                                      @AuthenticationPrincipal DriveUserDetails userDetails,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        CabinetPageStateDto normalizedState = cabinetMvcSupport.normalizeState(cabinetState);
        CollaborativeAccessDto access = collaborativeAccessService.resolveReceivedAccess(
                userDetails.getUser().getId(),
                normalizedState.getCollaborativeAccessId()
        );
        if (!collaborativeAccessService.unlock(session, access, password)) {
            redirectAttributes.addFlashAttribute("collaborativeError", "Неверный пароль.");
        }
        return cabinetMvcSupport.redirectCabinet(normalizedState);
    }

    @PostMapping("collaborative/folders/add")
    public String addCollaborativeFolder(@Valid @ModelAttribute("folderDto") FolderDto folderDto,
                                         BindingResult bindingResult,
                                         @ModelAttribute("cabinetState") CabinetPageStateDto cabinetState,
                                         @AuthenticationPrincipal DriveUserDetails userDetails,
                                         Model model,
                                         RedirectAttributes redirectAttributes) {
        CabinetPageStateDto normalizedState = cabinetMvcSupport.normalizeState(cabinetState);
        CollaborativeAccessDto access = collaborativeAccessService.resolveReceivedAccess(
                userDetails.getUser().getId(),
                normalizedState.getCollaborativeAccessId()
        );
        if (!collaborativeAccessService.canWrite(access)) {
            redirectAttributes.addFlashAttribute("collaborativeError", "Нет прав на запись в эту директорию.");
            return cabinetMvcSupport.redirectCabinet(normalizedState);
        }
        if (!collaborativeAccessService.canReadFolder(access, folderDto.getParentId())) {
            redirectAttributes.addFlashAttribute("collaborativeError", "Нельзя создать папку вне открытой директории.");
            return cabinetMvcSupport.redirectCabinet(normalizedState);
        }

        folderDto.setOwnerId(access.getOwnerId());
        folderDto.setPathKey(cabinetMvcSupport.buildPathKey(access.getOwnerId(), folderDto.getParentId(), folderDto.getName()));
        folderDto.setSortOrder(0);
        String expirationError = folderService.validateExpirationInput(folderDto.getExpiresAt());
        if (bindingResult.hasErrors() || expirationError != null) {
            cabinetMvcSupport.fillCollaborativeModel(model, userDetails, normalizedState.getCollaborativeAccessId(), normalizedState.getCurrentFolderId(), normalizedState.getView(), normalizedState.getSort(), normalizedState.getPage(), normalizedState.getSize());
            model.addAttribute("collaborativeError", expirationError == null ? "Проверьте название новой папки." : expirationError);
            return "cabinet/collaborative";
        }
        folderService.normalizeExpiration(folderDto);

        FolderDto saved = folderService.save(folderDto);
        if (saved == null) {
            redirectAttributes.addFlashAttribute("collaborativeError", "Не удалось создать папку.");
            return cabinetMvcSupport.redirectCabinet(normalizedState);
        }

        redirectAttributes.addFlashAttribute("collaborativeSuccess", "Папка создана.");
        return cabinetMvcSupport.redirectCabinet(normalizedState);
    }

    @PostMapping("items/properties")
    public String updateItemProperties(@ModelAttribute("itemPropertiesDto") ItemPropertiesDto itemPropertiesDto,
                                       @ModelAttribute("cabinetState") CabinetPageStateDto cabinetState,
                                       @AuthenticationPrincipal DriveUserDetails userDetails,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        CabinetPageStateDto normalizedState = cabinetMvcSupport.normalizeState(cabinetState);
        String resourceType = itemPropertiesDto.getResourceType() == null ? "" : itemPropertiesDto.getResourceType().trim().toUpperCase();
        boolean autoDelete = Boolean.TRUE.equals(itemPropertiesDto.getAutoDelete());
        boolean expiresUnlimited = !autoDelete;

        String error;
        String successMessage;
        if ("FOLDER".equals(resourceType)) {
            error = folderService.updateExpirationByIdAndOwnerId(
                    itemPropertiesDto.getResourceId(),
                    userDetails.getUser().getId(),
                    itemPropertiesDto.getExpiresAt(),
                    expiresUnlimited
            );
            successMessage = "Свойства папки обновлены.";
        } else if ("FILE".equals(resourceType)) {
            error = fileService.updateExpirationByIdAndOwnerId(
                    itemPropertiesDto.getResourceId(),
                    userDetails.getUser().getId(),
                    itemPropertiesDto.getExpiresAt(),
                    expiresUnlimited
            );
            successMessage = "Свойства файла обновлены.";
        } else {
            error = "Неизвестный тип объекта.";
            successMessage = null;
        }

        if (error != null) {
            cabinetMvcSupport.fillCabinetModel(model, userDetails, normalizedState);
            model.addAttribute("propertiesError", error);
            cabinetMvcSupport.prepareItemModalState(model, "item-properties-modal", itemPropertiesDto.getResourceType(), itemPropertiesDto.getResourceId());
            return "cabinet/index";
        }

        redirectAttributes.addFlashAttribute("fileSuccess", successMessage);
        return cabinetMvcSupport.redirectCabinet(normalizedState);
    }

    @PostMapping("collaborative/files/upload")
    public Object uploadCollaborativeFile(@ModelAttribute("fileUploadDto") FileUploadDto fileUploadDto,
                                          @ModelAttribute("cabinetState") CabinetPageStateDto cabinetState,
                                          @AuthenticationPrincipal DriveUserDetails userDetails,
                                          RedirectAttributes redirectAttributes,
                                          Model model,
                                          HttpServletRequest request) {
        CabinetPageStateDto normalizedState = cabinetMvcSupport.normalizeState(cabinetState);
        CollaborativeAccessDto access = collaborativeAccessService.resolveReceivedAccess(
                userDetails.getUser().getId(),
                normalizedState.getCollaborativeAccessId()
        );
        if (!collaborativeAccessService.canWrite(access)) {
            if (isAjaxUploadRequest(request)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Нет прав на запись в эту директорию."));
            }
            redirectAttributes.addFlashAttribute("collaborativeError", "Нет прав на запись в эту директорию.");
            return cabinetMvcSupport.redirectCabinet(normalizedState);
        }
        if (!collaborativeAccessService.canReadFolder(access, fileUploadDto.getFolderId())) {
            if (isAjaxUploadRequest(request)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Нельзя загружать файлы вне открытой директории."));
            }
            redirectAttributes.addFlashAttribute("collaborativeError", "Нельзя загружать файлы вне открытой директории.");
            return cabinetMvcSupport.redirectCabinet(normalizedState);
        }

        String uploadError = fileService.validateUpload(access.getOwnerId(), fileUploadDto);
        if (uploadError != null) {
            if (isAjaxUploadRequest(request)) {
                return ResponseEntity.badRequest().body(Map.of("error", uploadError));
            }
            cabinetMvcSupport.fillCollaborativeModel(model, userDetails, normalizedState.getCollaborativeAccessId(), normalizedState.getCurrentFolderId(), normalizedState.getView(), normalizedState.getSort(), normalizedState.getPage(), normalizedState.getSize());
            model.addAttribute("collaborativeError", uploadError);
            return "cabinet/collaborative";
        }

        FileItemDto saved;
        try {
            saved = fileService.upload(access.getOwnerId(), fileUploadDto);
        } catch (RuntimeException exception) {
            return handleUploadException(exception, request, model, redirectAttributes, userDetails, normalizedState, true);
        }
        if (saved == null) {
            if (isAjaxUploadRequest(request)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Не удалось загрузить файл."));
            }
            redirectAttributes.addFlashAttribute("collaborativeError", "Не удалось загрузить файл.");
            return cabinetMvcSupport.redirectCabinet(normalizedState);
        }

        String successMessage = "Файл загружен: " + saved.getOriginalFilename();
        normalizedState.setCurrentFolderId(saved.getFolderId());
        normalizedState.setPage(1);
        if (isAjaxUploadRequest(request)) {
            return ResponseEntity.ok(Map.of(
                    "redirectUrl", cabinetMvcSupport.redirectCabinet(normalizedState).replace("redirect:", ""),
                    "message", successMessage
            ));
        }
        redirectAttributes.addFlashAttribute("collaborativeSuccess", successMessage);
        return cabinetMvcSupport.redirectCabinet(normalizedState);
    }

    private Object handleUploadException(RuntimeException exception,
                                         HttpServletRequest request,
                                         Model model,
                                         RedirectAttributes redirectAttributes,
                                         DriveUserDetails userDetails,
                                         CabinetPageStateDto normalizedState,
                                         boolean collaborative) {
        String message = resolveUploadExceptionMessage(exception);
        log.error("Upload failed: collaborative={}, folderId={}, userId={}",
                collaborative,
                normalizedState == null ? null : normalizedState.getCurrentFolderId(),
                userDetails == null ? null : userDetails.getUser().getId(),
                exception);

        if (isAjaxUploadRequest(request)) {
            return ResponseEntity.badRequest().body(Map.of("error", message));
        }

        if (collaborative) {
            cabinetMvcSupport.fillCollaborativeModel(
                    model,
                    userDetails,
                    normalizedState.getCollaborativeAccessId(),
                    normalizedState.getCurrentFolderId(),
                    normalizedState.getView(),
                    normalizedState.getSort(),
                    normalizedState.getPage(),
                    normalizedState.getSize()
            );
            model.addAttribute("collaborativeError", message);
            return "cabinet/collaborative";
        }

        cabinetMvcSupport.fillCabinetModel(model, userDetails, normalizedState);
        model.addAttribute("fileError", message);
        return "cabinet/index";
    }

    private String resolveUploadExceptionMessage(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                String sqlState = sqlException.getSQLState();
                String sqlMessage = sqlException.getMessage();
                if ("23505".equals(sqlState)) {
                    if (sqlMessage != null && sqlMessage.contains("agdrv_files_folder_name_uq")) {
                        return "В этой папке уже есть файл с таким именем.";
                    }
                    return "Найден конфликт уникальности при загрузке файла.";
                }
                if ("23503".equals(sqlState)) {
                    return "Выбранная папка больше недоступна.";
                }
            }
            current = current.getCause();
        }

        String message = exception == null ? null : exception.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return "Не удалось загрузить файл. Подробности смотрите в логе приложения.";
    }

    private boolean isAjaxUploadRequest(HttpServletRequest request) {
        return request != null && "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }

    @PostMapping("collaborative/items/delete")
    public String deleteCollaborativeItem(@RequestParam("resourceType") String resourceType,
                                          @RequestParam("resourceId") Long resourceId,
                                          @ModelAttribute("cabinetState") CabinetPageStateDto cabinetState,
                                          @AuthenticationPrincipal DriveUserDetails userDetails,
                                          RedirectAttributes redirectAttributes) {
        CabinetPageStateDto normalizedState = cabinetMvcSupport.normalizeState(cabinetState);
        CollaborativeAccessDto access = collaborativeAccessService.resolveReceivedAccess(
                userDetails.getUser().getId(),
                normalizedState.getCollaborativeAccessId()
        );
        if (!collaborativeAccessService.canDelete(access)) {
            redirectAttributes.addFlashAttribute("collaborativeError", "Нет прав на удаление в этой директории.");
            return cabinetMvcSupport.redirectCabinet(normalizedState);
        }

        String normalizedType = resourceType == null ? "" : resourceType.trim().toUpperCase();
        String error;
        if ("FOLDER".equals(normalizedType)) {
            if (!collaborativeAccessService.canReadFolder(access, resourceId)) {
                redirectAttributes.addFlashAttribute("collaborativeError", "Папка недоступна.");
                return cabinetMvcSupport.redirectCabinet(normalizedState);
            }
            error = folderDeleteService.deleteByIdAndOwnerId(resourceId, access.getOwnerId());
        } else if ("FILE".equals(normalizedType)) {
            if (!collaborativeAccessService.canReadFile(access, resourceId)) {
                redirectAttributes.addFlashAttribute("collaborativeError", "Файл недоступен.");
                return cabinetMvcSupport.redirectCabinet(normalizedState);
            }
            error = fileService.deleteByIdAndOwnerId(resourceId, access.getOwnerId());
        } else {
            error = "Неизвестный тип объекта.";
        }

        if (error != null) {
            redirectAttributes.addFlashAttribute("collaborativeError", error);
        } else {
            redirectAttributes.addFlashAttribute("collaborativeSuccess", "Объект удален.");
        }
        return cabinetMvcSupport.redirectCabinet(normalizedState);
    }

    @org.springframework.web.bind.annotation.GetMapping("files/{id}/download")
    @ResponseBody
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

        return MediaResponseSupport.buildPathResponse(
                path,
                mimeTypePolicyService.resolveResponseMediaType(fileItemDto.getMimeType()),
                fileItemDto.getOriginalFilename(),
                false,
                null
        );
    }

    @org.springframework.web.bind.annotation.GetMapping("folders/{id}/download")
    @ResponseBody
    public ResponseEntity<?> downloadFolder(@PathVariable("id") Long id,
                                            @AuthenticationPrincipal DriveUserDetails userDetails) {
        FolderDto folderDto = folderService.findByIdAndOwnerId(id, userDetails.getUser().getId());
        if (folderDto == null) {
            return ResponseEntity.notFound().build();
        }

        var archivePath = folderArchiveService.buildFolderArchiveTempFile(folderDto);
        if (archivePath == null) {
            return ResponseEntity.notFound().build();
        }

        return MediaResponseSupport.buildEphemeralPathResponse(
                archivePath,
                MediaType.APPLICATION_OCTET_STREAM,
                folderDto.getName() + ".zip",
                false,
                null
        );
    }

    @org.springframework.web.bind.annotation.GetMapping("collaborative/files/{id}/download")
    @ResponseBody
    public ResponseEntity<?> downloadCollaborativeFile(@PathVariable("id") Long id,
                                                       @RequestParam("accessId") Long accessId,
                                                       @AuthenticationPrincipal DriveUserDetails userDetails) {
        CollaborativeAccessDto access = collaborativeAccessService.resolveReceivedAccess(userDetails.getUser().getId(), accessId);
        if (!collaborativeAccessService.canReadFile(access, id)) {
            return ResponseEntity.notFound().build();
        }
        FileItemDto fileItemDto = fileService.findById(id);
        if (fileItemDto == null) {
            return ResponseEntity.notFound().build();
        }
        var path = fileContentStorageService.resolveExistingPath(fileItemDto.getStorageName());
        if (path == null) {
            return ResponseEntity.notFound().build();
        }
        return MediaResponseSupport.buildPathResponse(
                path,
                mimeTypePolicyService.resolveResponseMediaType(fileItemDto.getMimeType()),
                fileItemDto.getOriginalFilename(),
                false,
                null
        );
    }

    @org.springframework.web.bind.annotation.GetMapping("collaborative/files/{id}/content")
    @ResponseBody
    public ResponseEntity<?> openCollaborativeFileContent(@PathVariable("id") Long id,
                                                          @RequestParam("accessId") Long accessId,
                                                          @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader,
                                                          @AuthenticationPrincipal DriveUserDetails userDetails) {
        CollaborativeAccessDto access = collaborativeAccessService.resolveReceivedAccess(userDetails.getUser().getId(), accessId);
        if (!collaborativeAccessService.canReadFile(access, id)) {
            return ResponseEntity.notFound().build();
        }
        FileItemDto fileItemDto = fileService.findById(id);
        if (fileItemDto == null) {
            return ResponseEntity.notFound().build();
        }
        var path = fileContentStorageService.resolveExistingPath(fileItemDto.getStorageName());
        if (path == null) {
            return ResponseEntity.notFound().build();
        }
        return MediaResponseSupport.buildPathResponse(
                path,
                mimeTypePolicyService.resolveResponseMediaType(fileItemDto.getMimeType()),
                fileItemDto.getOriginalFilename(),
                true,
                rangeHeader
        );
    }

    @org.springframework.web.bind.annotation.GetMapping("collaborative/files/{id}/preview-text")
    @ResponseBody
    public ResponseEntity<String> openCollaborativeFileTextPreview(@PathVariable("id") Long id,
                                                                   @RequestParam("accessId") Long accessId,
                                                                   @AuthenticationPrincipal DriveUserDetails userDetails) {
        CollaborativeAccessDto access = collaborativeAccessService.resolveReceivedAccess(userDetails.getUser().getId(), accessId);
        if (!collaborativeAccessService.canReadFile(access, id)) {
            return ResponseEntity.notFound().build();
        }
        FileItemDto fileItemDto = fileService.findById(id);
        if (fileItemDto == null || !fileItemDto.isTextPreviewAllowed()) {
            return ResponseEntity.notFound().build();
        }
        byte[] content = fileService.findContentBytesByFileId(id);
        if (content == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(new String(content, StandardCharsets.UTF_8));
    }

    @org.springframework.web.bind.annotation.GetMapping("collaborative/files/{id}/preview-audio")
    @ResponseBody
    public ResponseEntity<byte[]> openCollaborativeFileAudioPreview(@PathVariable("id") Long id,
                                                                    @RequestParam("accessId") Long accessId,
                                                                    @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader,
                                                                    @AuthenticationPrincipal DriveUserDetails userDetails) {
        CollaborativeAccessDto access = collaborativeAccessService.resolveReceivedAccess(userDetails.getUser().getId(), accessId);
        if (!collaborativeAccessService.canReadFile(access, id)) {
            return ResponseEntity.notFound().build();
        }
        FileItemDto fileItemDto = fileService.findById(id);
        if (fileItemDto == null || !fileItemDto.isAudioPreview()) {
            return ResponseEntity.notFound().build();
        }
        byte[] content = fileService.findContentBytesByFileId(id);
        return MediaResponseSupport.buildResponse(
                content,
                mimeTypePolicyService.resolveResponseMediaType(fileItemDto.getMimeType()),
                fileItemDto.getOriginalFilename(),
                true,
                rangeHeader
        );
    }

    @org.springframework.web.bind.annotation.GetMapping("collaborative/files/{id}/thumbnail")
    @ResponseBody
    public ResponseEntity<byte[]> openCollaborativeFileThumbnail(@PathVariable("id") Long id,
                                                                 @RequestParam("accessId") Long accessId,
                                                                 @AuthenticationPrincipal DriveUserDetails userDetails) {
        CollaborativeAccessDto access = collaborativeAccessService.resolveReceivedAccess(userDetails.getUser().getId(), accessId);
        if (!collaborativeAccessService.canReadFile(access, id)) {
            return ResponseEntity.notFound().build();
        }
        FileItemDto fileItemDto = fileService.findById(id);
        if (fileItemDto == null || !fileItemDto.isImagePreview()) {
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

    @org.springframework.web.bind.annotation.GetMapping("collaborative/folders/{id}/download")
    @ResponseBody
    public ResponseEntity<?> downloadCollaborativeFolder(@PathVariable("id") Long id,
                                                         @RequestParam("accessId") Long accessId,
                                                         @AuthenticationPrincipal DriveUserDetails userDetails) {
        CollaborativeAccessDto access = collaborativeAccessService.resolveReceivedAccess(userDetails.getUser().getId(), accessId);
        if (!collaborativeAccessService.canReadFolder(access, id)) {
            return ResponseEntity.notFound().build();
        }
        FolderDto folderDto = folderService.findById(id);
        if (folderDto == null) {
            return ResponseEntity.notFound().build();
        }
        var archivePath = folderArchiveService.buildFolderArchiveTempFile(folderDto);
        if (archivePath == null) {
            return ResponseEntity.notFound().build();
        }
        return MediaResponseSupport.buildEphemeralPathResponse(
                archivePath,
                MediaType.APPLICATION_OCTET_STREAM,
                folderDto.getName() + ".zip",
                false,
                null
        );
    }

    @org.springframework.web.bind.annotation.GetMapping("files/{id}/content")
    @ResponseBody
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

        return MediaResponseSupport.buildPathResponse(
                path,
                mimeTypePolicyService.resolveResponseMediaType(fileItemDto.getMimeType()),
                fileItemDto.getOriginalFilename(),
                true,
                rangeHeader
        );
    }

    @org.springframework.web.bind.annotation.GetMapping("files/{id}/preview-text")
    @ResponseBody
    public ResponseEntity<String> openFileTextPreview(@PathVariable("id") Long id,
                                                      @AuthenticationPrincipal DriveUserDetails userDetails) {
        FileItemDto fileItemDto = fileService.findByIdAndOwnerId(id, userDetails.getUser().getId());
        if (fileItemDto == null || !fileItemDto.isTextPreviewAllowed()) {
            return ResponseEntity.notFound().build();
        }
        byte[] content = fileService.findContentBytesByFileId(id);
        if (content == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(new String(content, StandardCharsets.UTF_8));
    }

    @org.springframework.web.bind.annotation.GetMapping("files/{id}/preview-audio")
    @ResponseBody
    public ResponseEntity<byte[]> openFileAudioPreview(@PathVariable("id") Long id,
                                                       @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader,
                                                       @AuthenticationPrincipal DriveUserDetails userDetails) {
        FileItemDto fileItemDto = fileService.findByIdAndOwnerId(id, userDetails.getUser().getId());
        if (fileItemDto == null || !fileItemDto.isAudioPreview()) {
            return ResponseEntity.notFound().build();
        }
        byte[] content = fileService.findContentBytesByFileId(id);
        return MediaResponseSupport.buildResponse(
                content,
                mimeTypePolicyService.resolveResponseMediaType(fileItemDto.getMimeType()),
                fileItemDto.getOriginalFilename(),
                true,
                rangeHeader
        );
    }

    @org.springframework.web.bind.annotation.GetMapping("files/{id}/thumbnail")
    @ResponseBody
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
                             @ModelAttribute("cabinetState") CabinetPageStateDto cabinetState,
                             @AuthenticationPrincipal DriveUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        CabinetPageStateDto normalizedState = cabinetMvcSupport.normalizeState(cabinetState);
        FileItemDto fileItemDto = fileService.findByIdAndOwnerId(id, userDetails.getUser().getId());
        String error = fileService.deleteByIdAndOwnerId(id, userDetails.getUser().getId());
        if (error != null) {
            redirectAttributes.addFlashAttribute("fileError", error);
        } else {
            if (fileItemDto != null) {
                cabinetMvcSupport.log(userDetails.getUser().getId(), "FILE_DELETE", "FILE", id, "Удален файл " + fileItemDto.getOriginalFilename());
            }
            redirectAttributes.addFlashAttribute("fileSuccess", "Файл удален.");
        }
        return cabinetMvcSupport.redirectCabinet(normalizedState);
    }

    @PostMapping("folders/{id}/delete")
    public String deleteFolder(@PathVariable("id") Long id,
                               @ModelAttribute("cabinetState") CabinetPageStateDto cabinetState,
                               @AuthenticationPrincipal DriveUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        CabinetPageStateDto normalizedState = cabinetMvcSupport.normalizeState(cabinetState);
        FolderDto folderDto = folderService.findByIdAndOwnerId(id, userDetails.getUser().getId());
        String error = folderDeleteService.deleteByIdAndOwnerId(id, userDetails.getUser().getId());
        if (error != null) {
            redirectAttributes.addFlashAttribute("folderError", error);
        } else {
            if (folderDto != null) {
                cabinetMvcSupport.log(userDetails.getUser().getId(), "FOLDER_DELETE", "FOLDER", id, "Удалена папка " + folderDto.getName());
            }
            redirectAttributes.addFlashAttribute("folderSuccess", "Папка удалена.");
        }
        return cabinetMvcSupport.redirectCabinet(normalizedState);
    }

    @PostMapping("items/delete")
    public String deleteItem(@RequestParam(name = "resourceType", required = false) String resourceType,
                             @RequestParam(name = "resourceId", required = false) Long resourceId,
                             @ModelAttribute("cabinetState") CabinetPageStateDto cabinetState,
                             @AuthenticationPrincipal DriveUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        CabinetPageStateDto normalizedState = cabinetMvcSupport.normalizeState(cabinetState);
        if (resourceType == null || resourceType.isBlank() || resourceId == null) {
            redirectAttributes.addFlashAttribute("fileError", "Не выбран объект для удаления.");
            return cabinetMvcSupport.redirectCabinet(normalizedState);
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
                    cabinetMvcSupport.log(userDetails.getUser().getId(), "FOLDER_DELETE", "FOLDER", resourceId, "Удалена папка " + folderDto.getName());
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
                    cabinetMvcSupport.log(userDetails.getUser().getId(), "FILE_DELETE", "FILE", resourceId, "Удален файл " + fileItemDto.getOriginalFilename());
                }
                redirectAttributes.addFlashAttribute("fileSuccess", "Файл удален.");
            }
        } else {
            redirectAttributes.addFlashAttribute("fileError", "Неизвестный тип объекта.");
        }

        return cabinetMvcSupport.redirectCabinet(normalizedState);
    }

    @PostMapping("items/rename")
    public String renameItem(@Valid @ModelAttribute("itemRenameDto") ItemRenameDto itemRenameDto,
                             BindingResult bindingResult,
                             @ModelAttribute("cabinetState") CabinetPageStateDto cabinetState,
                             @AuthenticationPrincipal DriveUserDetails userDetails,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        CabinetPageStateDto normalizedState = cabinetMvcSupport.normalizeState(cabinetState);
        if (bindingResult.hasErrors()) {
            cabinetMvcSupport.fillCabinetModel(model, userDetails, normalizedState);
            cabinetMvcSupport.prepareItemModalState(model, "item-rename-modal", itemRenameDto.getResourceType(), itemRenameDto.getResourceId());
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
            cabinetMvcSupport.fillCabinetModel(model, userDetails, normalizedState);
            model.addAttribute("renameError", error);
            cabinetMvcSupport.prepareItemModalState(model, "item-rename-modal", itemRenameDto.getResourceType(), itemRenameDto.getResourceId());
            return "cabinet/index";
        }

        cabinetMvcSupport.log(userDetails.getUser().getId(), "ITEM_RENAME", resourceType, itemRenameDto.getResourceId(),
                "Переименование в " + itemRenameDto.getNewName());
        if ("FOLDER".equals(resourceType)) {
            redirectAttributes.addFlashAttribute("folderSuccess", successMessage);
        } else {
            redirectAttributes.addFlashAttribute("fileSuccess", successMessage);
        }
        return cabinetMvcSupport.redirectCabinet(normalizedState);
    }

    @PostMapping("items/move")
    public String moveItem(@Valid @ModelAttribute("itemMoveDto") ItemMoveDto itemMoveDto,
                           BindingResult bindingResult,
                           @ModelAttribute("cabinetState") CabinetPageStateDto cabinetState,
                           @AuthenticationPrincipal DriveUserDetails userDetails,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        CabinetPageStateDto normalizedState = cabinetMvcSupport.normalizeState(cabinetState);
        if (bindingResult.hasErrors()) {
            cabinetMvcSupport.fillCabinetModel(model, userDetails, normalizedState);
            cabinetMvcSupport.prepareItemModalState(model, "item-move-modal", itemMoveDto.getResourceType(), itemMoveDto.getResourceId());
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
            cabinetMvcSupport.fillCabinetModel(model, userDetails, normalizedState);
            model.addAttribute("moveError", error);
            cabinetMvcSupport.prepareItemModalState(model, "item-move-modal", itemMoveDto.getResourceType(), itemMoveDto.getResourceId());
            return "cabinet/index";
        }

        cabinetMvcSupport.log(userDetails.getUser().getId(), "ITEM_MOVE", resourceType, itemMoveDto.getResourceId(),
                "Перемещен в папку " + itemMoveDto.getTargetFolderId());
        if ("FOLDER".equals(resourceType)) {
            redirectAttributes.addFlashAttribute("folderSuccess", successMessage);
        } else {
            redirectAttributes.addFlashAttribute("fileSuccess", successMessage);
        }
        return cabinetMvcSupport.redirectCabinet(normalizedState);
    }

    @PostMapping("items/bulk/delete")
    public String bulkDelete(@ModelAttribute("cabinetState") CabinetPageStateDto cabinetState,
                             @RequestParam(name = "selectedFileIds", required = false) String selectedFileIds,
                             @RequestParam(name = "selectedFolderIds", required = false) String selectedFolderIds,
                             @AuthenticationPrincipal DriveUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        CabinetPageStateDto normalizedState = cabinetMvcSupport.normalizeState(cabinetState);
        List<Long> fileIds = cabinetMvcSupport.parseIds(selectedFileIds);
        List<Long> folderIds = cabinetMvcSupport.parseIds(selectedFolderIds);
        if (fileIds.isEmpty() && folderIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("fileError", "Не выбраны объекты для удаления.");
            return cabinetMvcSupport.redirectCabinet(normalizedState);
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
                cabinetMvcSupport.log(userDetails.getUser().getId(), "FILE_DELETE", "FILE", fileId, "Массовое удаление файла " + file.getOriginalFilename());
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
                cabinetMvcSupport.log(userDetails.getUser().getId(), "FOLDER_DELETE", "FOLDER", folderId, "Массовое удаление папки " + folder.getName());
            } else {
                failedItems++;
            }
        }
        redirectAttributes.addFlashAttribute("fileSuccess", "Удалено файлов: " + deletedFiles + ", папок: " + deletedFolders + ".");
        if (failedItems > 0) {
            redirectAttributes.addFlashAttribute("fileError", "Не удалось обработать объектов: " + failedItems + ".");
        }
        return cabinetMvcSupport.redirectCabinet(normalizedState);
    }

    @PostMapping("items/bulk/move")
    public String bulkMove(@ModelAttribute("cabinetState") CabinetPageStateDto cabinetState,
                           @RequestParam(name = "targetFolderId", required = false) Long targetFolderId,
                           @RequestParam(name = "selectedFileIds", required = false) String selectedFileIds,
                           @RequestParam(name = "selectedFolderIds", required = false) String selectedFolderIds,
                           @AuthenticationPrincipal DriveUserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        CabinetPageStateDto normalizedState = cabinetMvcSupport.normalizeState(cabinetState);
        List<Long> fileIds = cabinetMvcSupport.parseIds(selectedFileIds);
        List<Long> folderIds = cabinetMvcSupport.parseIds(selectedFolderIds);
        if (fileIds.isEmpty() && folderIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("fileError", "Не выбраны объекты для перемещения.");
            return cabinetMvcSupport.redirectCabinet(normalizedState);
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
                cabinetMvcSupport.log(userDetails.getUser().getId(), "ITEM_MOVE", "FILE", fileId, "Массовое перемещение файла " + file.getOriginalFilename());
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
                cabinetMvcSupport.log(userDetails.getUser().getId(), "ITEM_MOVE", "FOLDER", folderId, "Массовое перемещение папки " + folder.getName());
            } else {
                failedItems++;
            }
        }
        redirectAttributes.addFlashAttribute("fileSuccess", "Перемещено файлов: " + movedFiles + ", папок: " + movedFolders + ".");
        if (failedItems > 0) {
            redirectAttributes.addFlashAttribute("fileError", "Не удалось обработать объектов: " + failedItems + ".");
        }
        return cabinetMvcSupport.redirectCabinet(normalizedState);
    }

    @PostMapping("shares")
    public String createShare(@ModelAttribute("shareLinkCreateDto") ShareLinkCreateDto shareLinkCreateDto,
                              @ModelAttribute("cabinetState") CabinetPageStateDto cabinetState,
                              @RequestParam(name = "resourceType", required = false) String resourceType,
                              @RequestParam(name = "resourceId", required = false) Long resourceId,
                              @AuthenticationPrincipal DriveUserDetails userDetails,
                              HttpServletRequest request,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        CabinetPageStateDto normalizedState = cabinetMvcSupport.normalizeState(cabinetState);
        if ((shareLinkCreateDto.getResourceType() == null || shareLinkCreateDto.getResourceType().isBlank()) && resourceType != null) {
            shareLinkCreateDto.setResourceType(resourceType);
        }
        if (shareLinkCreateDto.getResourceId() == null && resourceId != null) {
            shareLinkCreateDto.setResourceId(resourceId);
        }

        String validationError = shareLinkService.validateShareLinkCreate(userDetails.getUser().getId(), shareLinkCreateDto);
        if (validationError != null) {
            cabinetMvcSupport.fillCabinetModel(model, userDetails, normalizedState);
            model.addAttribute("shareError", validationError);
            return "cabinet/index";
        }

        ShareLinkDto shareLink = shareLinkService.createShareLink(userDetails.getUser().getId(), shareLinkCreateDto);
        if (shareLink == null) {
            cabinetMvcSupport.fillCabinetModel(model, userDetails, normalizedState);
            model.addAttribute("shareError", "Не удалось создать публичную ссылку. Проверьте тип ресурса и данные формы.");
            return "cabinet/index";
        }

        String shareUrl = cabinetMvcSupport.buildShareUrl(request, shareLink.getToken());
        cabinetMvcSupport.log(userDetails.getUser().getId(), "SHARE_CREATE", shareLink.getResourceType(), shareLink.getResourceId(),
                "Создана публичная ссылка " + shareLink.getToken());
        redirectAttributes.addFlashAttribute("shareSuccess", "Публичная ссылка создана.");
        redirectAttributes.addFlashAttribute("shareUrl", shareUrl);
        redirectAttributes.addFlashAttribute("shareOpenResourceId", shareLink.getResourceId());
        redirectAttributes.addFlashAttribute("shareOpenResourceType", shareLink.getResourceType());
        return cabinetMvcSupport.redirectCabinet(normalizedState);
    }

    @PostMapping("shares/delete")
    public String deleteShare(@ModelAttribute("cabinetState") CabinetPageStateDto cabinetState,
                              @RequestParam(name = "resourceType", required = false) String resourceType,
                              @RequestParam(name = "resourceId", required = false) Long resourceId,
                              @AuthenticationPrincipal DriveUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        CabinetPageStateDto normalizedState = cabinetMvcSupport.normalizeState(cabinetState);
        String error = shareLinkService.deleteShareLink(userDetails.getUser().getId(), resourceType, resourceId);
        if (error != null) {
            redirectAttributes.addFlashAttribute("shareError", error);
        } else {
            cabinetMvcSupport.log(userDetails.getUser().getId(), "SHARE_DELETE", resourceType, resourceId, "Удалена публичная ссылка");
            redirectAttributes.addFlashAttribute("shareSuccess", "Публичная ссылка удалена.");
        }
        return cabinetMvcSupport.redirectCabinet(normalizedState);
    }
}
