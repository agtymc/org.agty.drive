package org.agty.drive.web.controllers.mvc.control;

import jakarta.validation.Valid;
import org.agty.drive.dto.AdminUserAccessUpdateDto;
import org.agty.drive.dto.AdminUserCreateDto;
import org.agty.drive.dto.AuditLogDto;
import org.agty.drive.dto.UserDto;
import org.agty.drive.dto.UserInviteCreateDto;
import org.agty.drive.dto.UserInviteDto;
import org.agty.drive.security.service.DriveUserDetails;
import org.agty.drive.services.AuditLogService;
import org.agty.drive.services.FileService;
import org.agty.drive.services.FolderService;
import org.agty.drive.services.UserInviteService;
import org.agty.drive.services.UserService;
import org.agty.drive.services.UsersRoleDictionaryService;
import org.agty.drive.services.UsersStatusDictionaryService;
import org.agty.utils.AgtyUtils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/control")
public class ControlMvcController {
    private static final int DEFAULT_AUDIT_PAGE = 1;
    private static final int DEFAULT_AUDIT_PAGE_SIZE = 25;


    private final UserService userService;
    private final FolderService folderService;
    private final FileService fileService;
    private final UsersRoleDictionaryService usersRoleDictionaryService;
    private final UsersStatusDictionaryService usersStatusDictionaryService;
    private final AuditLogService auditLogService;
    private final UserInviteService userInviteService;

    public ControlMvcController(UserService userService,
                                FolderService folderService,
                                FileService fileService,
                                UsersRoleDictionaryService usersRoleDictionaryService,
                                UsersStatusDictionaryService usersStatusDictionaryService,
                                AuditLogService auditLogService,
                                UserInviteService userInviteService) {
        this.userService = userService;
        this.folderService = folderService;
        this.fileService = fileService;
        this.usersRoleDictionaryService = usersRoleDictionaryService;
        this.usersStatusDictionaryService = usersStatusDictionaryService;
        this.auditLogService = auditLogService;
        this.userInviteService = userInviteService;
    }

    @ModelAttribute("adminUserCreateDto")
    public AdminUserCreateDto adminUserCreateForm() {
        AdminUserCreateDto dto = new AdminUserCreateDto();
        dto.setRoleCode("ROLE_USER");
        dto.setStatusCode("ACTIVE");
        dto.setStorageQuotaMb(100L);
        return dto;
    }

    @ModelAttribute("adminUserAccessUpdateDto")
    public AdminUserAccessUpdateDto adminUserAccessUpdateForm() {
        return new AdminUserAccessUpdateDto();
    }

    @ModelAttribute("userInviteCreateDto")
    public UserInviteCreateDto userInviteCreateForm() {
        UserInviteCreateDto dto = new UserInviteCreateDto();
        dto.setRoleCode("ROLE_USER");
        dto.setStatusCode("ACTIVE");
        dto.setStorageQuotaMb(100L);
        dto.setExpiresInHours(72);
        return dto;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal DriveUserDetails userDetails, Model model) {
        fillControlModel(model, userDetails, "overview", DEFAULT_AUDIT_PAGE, DEFAULT_AUDIT_PAGE_SIZE, "date_desc");
        return "control/index";
    }

    @GetMapping("{section}")
    public String section(@PathVariable("section") String section,
                          @RequestParam(name = "page", required = false) Integer page,
                          @RequestParam(name = "size", required = false) Integer size,
                          @RequestParam(name = "sort", required = false) String sort,
                          @RequestParam(name = "createdDate", required = false) String createdDate,
                          @RequestParam(name = "actorLogin", required = false) String actorLogin,
                          @RequestParam(name = "actionCode", required = false) String actionCode,
                          @RequestParam(name = "resourceQuery", required = false) String resourceQuery,
                          @RequestParam(name = "details", required = false) String details,
                          @AuthenticationPrincipal DriveUserDetails userDetails,
                          Model model) {
        fillControlModel(model, userDetails, section, page, size, sort, createdDate, actorLogin, actionCode, resourceQuery, details);
        return "control/index";
    }

    @PostMapping("users/quota")
    public String updateUserQuota(@RequestParam("userId") Long userId,
                                  @RequestParam("storageQuotaMb") Long storageQuotaMb,
                                  @RequestParam(name = "returnSection", required = false) String returnSection,
                                  @AuthenticationPrincipal DriveUserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        String error = userService.updateStorageQuota(userId, storageQuotaMb);
        if (error != null) {
            redirectAttributes.addFlashAttribute("quotaError", error);
        } else {
            redirectAttributes.addFlashAttribute("quotaSuccess", "Квота пользователя обновлена.");
            auditLogService.log(userDetails.getUser().getId(), "ADMIN_UPDATE_QUOTA", "USER", userId,
                    "Квота пользователя обновлена до " + storageQuotaMb + " МБ");
        }
        return redirectControl(normalizeSection(returnSection));
    }

    @PostMapping("users/create")
    public String createUser(@Valid @ModelAttribute("adminUserCreateDto") AdminUserCreateDto adminUserCreateDto,
                             BindingResult bindingResult,
                             @RequestParam(name = "returnSection", required = false) String returnSection,
                             @AuthenticationPrincipal DriveUserDetails userDetails,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        String section = normalizeSection(returnSection);
        if (bindingResult.hasErrors()) {
            fillControlModel(model, userDetails, section, DEFAULT_AUDIT_PAGE, DEFAULT_AUDIT_PAGE_SIZE, "date_desc");
            return "control/index";
        }
        String error = userService.validateAdminCreate(adminUserCreateDto);
        if (error != null) {
            fillControlModel(model, userDetails, section, DEFAULT_AUDIT_PAGE, DEFAULT_AUDIT_PAGE_SIZE, "date_desc");
            model.addAttribute("userCreateError", error);
            return "control/index";
        }
        var created = userService.createByAdmin(userDetails.getUser().getId(), adminUserCreateDto);
        if (created == null) {
            fillControlModel(model, userDetails, section, DEFAULT_AUDIT_PAGE, DEFAULT_AUDIT_PAGE_SIZE, "date_desc");
            model.addAttribute("userCreateError", "Не удалось создать пользователя.");
            return "control/index";
        }
        auditLogService.log(userDetails.getUser().getId(), "ADMIN_CREATE_USER", "USER", created.getId(),
                "Создан пользователь " + created.getLogin());
        redirectAttributes.addFlashAttribute("userCreateSuccess", "Пользователь создан.");
        return redirectControl(section);
    }

    @PostMapping("users/access")
    public String updateUserAccess(@Valid @ModelAttribute("adminUserAccessUpdateDto") AdminUserAccessUpdateDto dto,
                                   BindingResult bindingResult,
                                   @RequestParam(name = "returnSection", required = false) String returnSection,
                                   @AuthenticationPrincipal DriveUserDetails userDetails,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        String section = normalizeSection(returnSection);
        if (bindingResult.hasErrors()) {
            fillControlModel(model, userDetails, section, DEFAULT_AUDIT_PAGE, DEFAULT_AUDIT_PAGE_SIZE, "date_desc");
            return "control/index";
        }
        String error = userService.updateAccess(dto.getUserId(), dto.getRoleCode(), dto.getStatusCode(), dto.getStorageQuotaMb());
        if (error != null) {
            fillControlModel(model, userDetails, section, DEFAULT_AUDIT_PAGE, DEFAULT_AUDIT_PAGE_SIZE, "date_desc");
            model.addAttribute("userAccessError", error);
            return "control/index";
        }
        auditLogService.log(userDetails.getUser().getId(), "ADMIN_UPDATE_ACCESS", "USER", dto.getUserId(),
                "Обновлены роль=" + dto.getRoleCode() + ", статус=" + dto.getStatusCode() + ", квота=" + dto.getStorageQuotaMb() + " МБ");
        redirectAttributes.addFlashAttribute("userAccessSuccess", "Пользователь обновлен.");
        return redirectControl(section);
    }

    @PostMapping("users/{userId}/block")
    public String blockUser(@RequestParam(name = "activate", required = false, defaultValue = "false") boolean activate,
                            @RequestParam(name = "userId", required = false) Long formUserId,
                            @RequestParam(name = "returnSection", required = false) String returnSection,
                            @org.springframework.web.bind.annotation.PathVariable("userId") Long pathUserId,
                            @AuthenticationPrincipal DriveUserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        Long userId = formUserId != null ? formUserId : pathUserId;
        String section = normalizeSection(returnSection);
        String error = activate ? userService.activateUser(userId) : userService.blockUser(userId);
        if (error != null) {
            redirectAttributes.addFlashAttribute("userAccessError", error);
        } else {
            auditLogService.log(userDetails.getUser().getId(), activate ? "ADMIN_ACTIVATE_USER" : "ADMIN_BLOCK_USER",
                    "USER", userId, activate ? "Пользователь активирован" : "Пользователь заблокирован");
            redirectAttributes.addFlashAttribute("userAccessSuccess", activate ? "Пользователь активирован." : "Пользователь заблокирован.");
        }
        return redirectControl(section);
    }

    @PostMapping("registration/invites/create")
    public String createInvite(@Valid @ModelAttribute("userInviteCreateDto") UserInviteCreateDto dto,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal DriveUserDetails userDetails,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            fillControlModel(model, userDetails, "registration", DEFAULT_AUDIT_PAGE, DEFAULT_AUDIT_PAGE_SIZE, "date_desc");
            return "control/index";
        }

        String error = userInviteService.validateCreate(dto);
        if (error != null) {
            fillControlModel(model, userDetails, "registration", DEFAULT_AUDIT_PAGE, DEFAULT_AUDIT_PAGE_SIZE, "date_desc");
            model.addAttribute("inviteError", error);
            return "control/index";
        }

        UserInviteDto invite = userInviteService.create(userDetails.getUser().getId(), dto);
        if (invite == null) {
            fillControlModel(model, userDetails, "registration", DEFAULT_AUDIT_PAGE, DEFAULT_AUDIT_PAGE_SIZE, "date_desc");
            model.addAttribute("inviteError", "Не удалось создать инвайт.");
            return "control/index";
        }

        auditLogService.log(userDetails.getUser().getId(), "ADMIN_CREATE_INVITE", "INVITE", invite.getId(),
                "Создан инвайт для логина " + invite.getLogin());
        redirectAttributes.addFlashAttribute("inviteSuccess", "Инвайт создан: " + invite.getInviteUrl());
        return "redirect:/control/registration";
    }

    @PostMapping("registration/invites/{inviteId}/disable")
    public String disableInvite(@PathVariable("inviteId") Long inviteId,
                                @AuthenticationPrincipal DriveUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        String error = userInviteService.disable(inviteId);
        if (error != null) {
            redirectAttributes.addFlashAttribute("inviteError", error);
        } else {
            auditLogService.log(userDetails.getUser().getId(), "ADMIN_DISABLE_INVITE", "INVITE", inviteId, "Инвайт отключен");
            redirectAttributes.addFlashAttribute("inviteSuccess", "Инвайт отключен.");
        }
        return "redirect:/control/registration";
    }

    private void fillControlModel(Model model,
                                  DriveUserDetails userDetails,
                                  String section,
                                  Integer auditPage,
                                  Integer auditPageSize,
                                  String auditSort) {
        fillControlModel(model, userDetails, section, auditPage, auditPageSize, auditSort, null, null, null, null, null);
    }

    private void fillControlModel(Model model,
                                  DriveUserDetails userDetails,
                                  String section,
                                  Integer auditPage,
                                  Integer auditPageSize,
                                  String auditSort,
                                  String auditCreatedDate,
                                  String auditActorLogin,
                                  String auditActionCode,
                                  String auditResourceQuery,
                                  String auditDetails) {
        String activeSection = normalizeSection(section);
        List<UserDto> users = userService.findAll();
        List<UserInviteDto> invites = userInviteService.findAll();
        String normalizedAuditSort = normalizeAuditSort(auditSort);
        int normalizedAuditPageSize = normalizeAuditPageSize(auditPageSize);
        String normalizedAuditCreatedDate = normalizeAuditDate(auditCreatedDate);
        String normalizedAuditActorLogin = normalizeAuditFilter(auditActorLogin);
        String normalizedAuditActionCode = normalizeAuditFilter(auditActionCode);
        String normalizedAuditResourceQuery = normalizeAuditFilter(auditResourceQuery);
        String normalizedAuditDetails = normalizeAuditFilter(auditDetails);
        boolean filteredAudit = normalizedAuditCreatedDate != null
                || normalizedAuditActorLogin != null
                || normalizedAuditActionCode != null
                || normalizedAuditResourceQuery != null
                || normalizedAuditDetails != null;
        long auditTotalItems = filteredAudit
                ? auditLogService.countFiltered(normalizedAuditCreatedDate, normalizedAuditActorLogin, normalizedAuditActionCode, normalizedAuditResourceQuery, normalizedAuditDetails)
                : auditLogService.countAll();
        int auditTotalPages = Math.max(1, (int) Math.ceil(auditTotalItems / (double) normalizedAuditPageSize));
        int normalizedAuditPage = normalizeAuditPage(auditPage, auditTotalPages);
        int auditOffset = (normalizedAuditPage - 1) * normalizedAuditPageSize;
        List<AuditLogDto> auditLogs = "audit".equals(activeSection)
                ? auditLogService.findPage(
                        normalizedAuditSort,
                        auditOffset,
                        normalizedAuditPageSize,
                        normalizedAuditCreatedDate,
                        normalizedAuditActorLogin,
                        normalizedAuditActionCode,
                        normalizedAuditResourceQuery,
                        normalizedAuditDetails
                )
                : auditLogService.findRecent(8);

        long activeUsers = users.stream().filter(user -> "ACTIVE".equalsIgnoreCase(user.getStatusCode())).count();
        long blockedUsers = users.stream().filter(user -> "BLOCKED".equalsIgnoreCase(user.getStatusCode())).count();
        long adminsCount = users.stream().filter(user -> "ROLE_ADMIN".equalsIgnoreCase(user.getRoleCode())).count();
        long filesCount = fileService.countAll();
        long filesSizeBytes = fileService.sumSizeAll();

        model.addAttribute("title", "AGTY/DRIVE Control");
        model.addAttribute("currentUser", userDetails.getUser());
        model.addAttribute("activeSection", activeSection);
        model.addAttribute("sectionTitle", sectionTitle(activeSection));
        model.addAttribute("sectionDescription", sectionDescription(activeSection));
        model.addAttribute("usersCount", users.size());
        model.addAttribute("foldersCount", folderService.countAll());
        model.addAttribute("activeUsersCount", activeUsers);
        model.addAttribute("blockedUsersCount", blockedUsers);
        model.addAttribute("adminsCount", adminsCount);
        model.addAttribute("users", users);
        model.addAttribute("filesCount", filesCount);
        model.addAttribute("filesSizeTitle", AgtyUtils.filesizeToTitle(filesSizeBytes, "ru"));
        model.addAttribute("usersRolesDictionary", usersRoleDictionaryService.findAll());
        model.addAttribute("usersStatusesDictionary", usersStatusDictionaryService.findAll());
        model.addAttribute("registrationInvites", invites);
        model.addAttribute("auditLogs", auditLogs);
        model.addAttribute("recentAuditLogs", "audit".equals(activeSection) ? auditLogService.findRecent(8) : auditLogs);
        model.addAttribute("auditSort", normalizedAuditSort);
        model.addAttribute("auditPage", normalizedAuditPage);
        model.addAttribute("auditPageSize", normalizedAuditPageSize);
        model.addAttribute("auditTotalItems", auditTotalItems);
        model.addAttribute("auditTotalPages", auditTotalPages);
        model.addAttribute("auditHasPrevious", normalizedAuditPage > 1);
        model.addAttribute("auditHasNext", normalizedAuditPage < auditTotalPages);
        model.addAttribute("auditCreatedDate", normalizedAuditCreatedDate);
        model.addAttribute("auditActorLogin", normalizedAuditActorLogin);
        model.addAttribute("auditActionCode", normalizedAuditActionCode);
        model.addAttribute("auditResourceQuery", normalizedAuditResourceQuery);
        model.addAttribute("auditDetails", normalizedAuditDetails);
    }

    private String normalizeSection(String value) {
        if (value == null || value.isBlank()) {
            return "overview";
        }
        return switch (value.trim().toLowerCase()) {
            case "users", "create", "dictionaries", "audit", "registration" -> value.trim().toLowerCase();
            default -> "overview";
        };
    }

    private String redirectControl(String section) {
        String normalized = normalizeSection(section);
        return "overview".equals(normalized) ? "redirect:/control" : "redirect:/control/" + normalized;
    }

    private String normalizeAuditSort(String value) {
        if (value == null || value.isBlank()) {
            return "date_desc";
        }
        return switch (value.trim().toLowerCase()) {
            case "date_asc" -> "date_asc";
            default -> "date_desc";
        };
    }

    private int normalizeAuditPage(Integer value, int totalPages) {
        if (value == null || value < 1) {
            return 1;
        }
        return Math.min(value, Math.max(1, totalPages));
    }

    private int normalizeAuditPageSize(Integer value) {
        if (value == null) {
            return DEFAULT_AUDIT_PAGE_SIZE;
        }
        return switch (value) {
            case 10, 25, 50, 100 -> value;
            default -> DEFAULT_AUDIT_PAGE_SIZE;
        };
    }

    private String normalizeAuditDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.matches("\\d{4}-\\d{2}-\\d{2}") ? normalized : null;
    }

    private String normalizeAuditFilter(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String sectionTitle(String section) {
        return switch (normalizeSection(section)) {
            case "users" -> "Пользователи";
            case "create" -> "Создание пользователя";
            case "registration" -> "Регистрация";
            case "dictionaries" -> "Справочники";
            case "audit" -> "Аудит";
            default -> "Обзор системы";
        };
    }

    private String sectionDescription(String section) {
        return switch (normalizeSection(section)) {
            case "users" -> "Управление ролями, статусами, квотами и доступом пользователей.";
            case "create" -> "Создание новых учетных записей с ролями, статусами и квотой.";
            case "registration" -> "Закрытая регистрация, инвайты и параметры выдачи доступа.";
            case "dictionaries" -> "Просмотр системных справочников ролей и статусов.";
            case "audit" -> "Последние административные и пользовательские действия в системе.";
            default -> "Ключевые показатели и быстрый доступ к административным операциям.";
        };
    }
}
