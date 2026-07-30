package org.agty.drive.web.controllers.mvc.cabinet;

import jakarta.servlet.http.HttpServletRequest;
import org.agty.drive.config.ApplicationInfo;
import org.agty.drive.dto.CollaborativeAccessDto;
import org.agty.drive.dto.CollaborativeFolderShareDto;
import org.agty.drive.dto.CabinetPageStateDto;
import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FolderDto;
import org.agty.drive.dto.SharedLibraryItemDto;
import org.agty.drive.security.service.DriveUserDetails;
import org.agty.drive.services.AuditLogService;
import org.agty.drive.services.CollaborativeAccessService;
import org.agty.drive.services.FileService;
import org.agty.drive.services.FolderService;
import org.agty.drive.services.ShareLinkService;
import org.agty.drive.services.UserService;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

@Component
public class CabinetMvcSupport {

    private final FolderService folderService;
    private final FileService fileService;
    private final ShareLinkService shareLinkService;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final CabinetRoutes cabinetRoutes;
    private final ApplicationInfo applicationInfo;
    private final CollaborativeAccessService collaborativeAccessService;

    public CabinetMvcSupport(FolderService folderService,
                             FileService fileService,
                             ShareLinkService shareLinkService,
                             UserService userService,
                             AuditLogService auditLogService,
                             CabinetRoutes cabinetRoutes,
                             ApplicationInfo applicationInfo,
                             CollaborativeAccessService collaborativeAccessService) {
        this.folderService = folderService;
        this.fileService = fileService;
        this.shareLinkService = shareLinkService;
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.cabinetRoutes = cabinetRoutes;
        this.applicationInfo = applicationInfo;
        this.collaborativeAccessService = collaborativeAccessService;
    }

    public void fillCabinetModel(Model model,
                                 DriveUserDetails userDetails,
                                 String section,
                                 Long currentFolderId,
                                 String viewMode,
                                 String sortMode,
                                 String searchQuery,
                                 String searchScope,
                                 Integer page,
                                 Integer pageSize) {
        CabinetPageStateDto state = new CabinetPageStateDto();
        state.setSection(section);
        state.setCurrentFolderId(currentFolderId);
        state.setView(viewMode);
        state.setSort(sortMode);
        state.setQ(searchQuery);
        state.setScope(searchScope);
        state.setPage(page);
        state.setSize(pageSize);
        state.setShareStatus(resolveSharedStatusFilter());
        state.setShareType(resolveSharedTypeFilter());
        fillCabinetModel(model, userDetails, state);
    }

    public void fillCabinetModel(Model model,
                                 DriveUserDetails userDetails,
                                 CabinetPageStateDto state) {
        CabinetPageStateDto normalizedState = normalizeState(state);
        String normalizedSection = normalizedState.getSection();
        CabinetViewState viewState = buildViewState(
                normalizedSection,
                normalizedState.getView(),
                normalizedState.getSort(),
                normalizedState.getQ(),
                normalizedState.getScope(),
                normalizedState.getPage(),
                normalizedState.getSize()
        );
        Long ownerId = userDetails.getUser().getId();
        boolean mediaLibraryMode = isMediaLibrarySection(normalizedSection);
        boolean sharedLibraryMode = isSharedLibrarySection(normalizedSection);
        FolderDto currentFolder = mediaLibraryMode || sharedLibraryMode
                ? null
                : (normalizedState.getCurrentFolderId() == null ? null : folderService.findByIdAndOwnerId(normalizedState.getCurrentFolderId(), ownerId));
        long filesSize = fileService.sumSizeByOwnerId(ownerId);
        var currentUser = userService.findById(ownerId);
        long storageQuotaBytes = currentUser.getStorageQuotaBytes() == null ? 0L : currentUser.getStorageQuotaBytes();
        int storageUsagePercent = storageQuotaBytes <= 0
                ? 0
                : (int) Math.min(100L, Math.round((filesSize * 100.0d) / storageQuotaBytes));
        Long scopeFolderId = currentFolder == null ? null : currentFolder.getId();
        String scopeFolderPath = currentFolder == null ? null : currentFolder.getPathKey();
        String sharedStatusFilter = normalizeSharedStatusFilter(normalizedState.getShareStatus());
        String sharedTypeFilter = normalizeSharedTypeFilter(normalizedState.getShareType());
        List<FolderDto> folders;
        List<FileItemDto> files;
        List<SharedLibraryItemDto> sharedItems;
        CabinetPage cabinetPage;

        if (sharedLibraryMode) {
            sharedItems = shareLinkService.findLibraryByCreator(
                    ownerId,
                    viewState.searchQuery(),
                    viewState.sortMode(),
                    sharedStatusFilter,
                    sharedTypeFilter
            );
            cabinetPage = paginateFlatEntries(sharedItems.size(), viewState.page(), viewState.pageSize());
            sharedItems = sliceSharedItems(sharedItems, cabinetPage.pageOffset(), cabinetPage.pageSize());
            folders = List.of();
            files = List.of();
        } else if (mediaLibraryMode) {
            files = fileService.findMediaLibraryByOwnerId(ownerId, viewState.searchQuery(), normalizedSection, viewState.sortMode());
            cabinetPage = paginateFlatEntries(files.size(), viewState.page(), viewState.pageSize());
            files = sliceFiles(files, cabinetPage.pageOffset(), cabinetPage.pageSize());
            folders = List.of();
            sharedItems = List.of();
        } else {
            long folderCount = folderService.countSearchByOwnerId(ownerId, viewState.searchQuery(), scopeFolderId, scopeFolderPath, viewState.searchScope());
            long fileCount = fileService.countSearchByOwnerId(ownerId, viewState.searchQuery(), scopeFolderId, scopeFolderPath, viewState.searchScope());
            cabinetPage = paginateCabinetEntries(folderCount, fileCount, viewState.page(), viewState.pageSize());
            folders = folderService.searchByOwnerId(
                    ownerId,
                    viewState.searchQuery(),
                    scopeFolderId,
                    scopeFolderPath,
                    viewState.searchScope(),
                    viewState.sortMode(),
                    cabinetPage.folderOffset(),
                    cabinetPage.folderLimit()
            );
            files = fileService.searchByOwnerId(
                    ownerId,
                    viewState.searchQuery(),
                    scopeFolderId,
                    scopeFolderPath,
                    viewState.searchScope(),
                    viewState.sortMode(),
                    cabinetPage.fileOffset(),
                    cabinetPage.fileLimit()
            );
            sharedItems = List.of();
        }

        model.addAttribute("pageTitle", resolveCabinetPageTitle(normalizedSection, currentFolder));
        model.addAttribute("cabinetRoutes", cabinetRoutes);
        model.addAttribute("cabinetState", normalizedState);
        model.addAttribute("cabinetSection", normalizedSection);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("folders", folders);
        long totalFoldersCount = folderService.countByOwnerId(ownerId);
        long totalFilesCount = fileService.countByOwnerId(ownerId);
        model.addAttribute("foldersCount", totalFoldersCount);
        model.addAttribute("files", files);
        model.addAttribute("filesCount", totalFilesCount);
        model.addAttribute("diskEmpty", totalFoldersCount == 0L && totalFilesCount == 0L);
        model.addAttribute("folderShareLinksByFolderId", shareLinkService.findLatestFolderShareLinks(folders));
        model.addAttribute("shareLinksByFileId", shareLinkService.findLatestFileShareLinks(files));
        model.addAttribute("collaborativeSharesByFolderId", "files".equals(normalizedSection)
                ? collaborativeAccessService.mapProvidedByFolderId(ownerId)
                : java.util.Map.of());
        model.addAttribute("sharedItems", sharedItems);
        model.addAttribute("filesSizeTitle", org.agty.utils.AgtyUtils.filesizeToTitle(filesSize, "ru"));
        model.addAttribute("storageQuotaTitle", currentUser.getStorageQuotaTitle());
        model.addAttribute("storageUsagePercent", storageUsagePercent);
        model.addAttribute("currentFolder", currentFolder);
        model.addAttribute("currentFolderId", currentFolder == null ? null : currentFolder.getId());
        model.addAttribute("breadcrumbs", buildBreadcrumbs(ownerId, currentFolder));
        model.addAttribute("moveFolderOptions", folderService.buildMoveOptions(ownerId));
        model.addAttribute("uploadFolderOptions", folderService.buildMoveOptions(ownerId));
        model.addAttribute("uploadExistingFileNamesByFolderId", fileService.buildExistingFileNamesByFolderId(ownerId));
        model.addAttribute("viewMode", viewState.viewMode());
        model.addAttribute("sortMode", viewState.sortMode());
        model.addAttribute("searchQuery", viewState.searchQuery());
        model.addAttribute("searchScope", viewState.searchScope());
        model.addAttribute("libraryMode", normalizedSection);
        model.addAttribute("sharedStatusFilter", sharedStatusFilter == null ? "all" : sharedStatusFilter);
        model.addAttribute("sharedTypeFilter", sharedTypeFilter == null ? "all" : sharedTypeFilter);
        model.addAttribute("sharedSummary", shareLinkService.summarizeLibraryStatuses(
                sharedLibraryMode
                        ? shareLinkService.findLibraryByCreator(ownerId, viewState.searchQuery(), viewState.sortMode(), null, null)
                        : List.of()
        ));
        model.addAttribute("currentPage", cabinetPage.currentPage());
        model.addAttribute("pageSize", cabinetPage.pageSize());
        model.addAttribute("totalPages", cabinetPage.totalPages());
        model.addAttribute("totalItems", cabinetPage.totalItems());
        model.addAttribute("pageSizeOptions", List.of(20, 50, 100));
        model.addAttribute("searchActive", !viewState.searchQuery().isBlank());
    }

    public void fillProfileModel(Model model, DriveUserDetails userDetails) {
        Long ownerId = userDetails.getUser().getId();
        model.addAttribute("pageTitle", "Профиль");
        model.addAttribute("cabinetRoutes", cabinetRoutes);
        model.addAttribute("cabinetSection", "profile");
        model.addAttribute("currentUser", userService.findById(ownerId));
        model.addAttribute("profileSecuritySettingsDto", userService.getProfileSecuritySettings(ownerId));
        model.addAttribute("foldersCount", folderService.countByOwnerId(ownerId));
        model.addAttribute("filesSizeTitle", org.agty.utils.AgtyUtils.filesizeToTitle(fileService.sumSizeByOwnerId(ownerId), "ru"));
        model.addAttribute("storageQuotaTitle", userService.findById(ownerId).getStorageQuotaTitle());
    }

    public void fillCollaborativeModel(Model model,
                                       DriveUserDetails userDetails,
                                       Long accessId,
                                       Long folderId,
                                       String viewMode,
                                       String sortMode,
                                       Integer page,
                                       Integer pageSize) {
        Long userId = userDetails.getUser().getId();
        CabinetPageStateDto state = new CabinetPageStateDto();
        state.setSection("collaborative");
        state.setCollaborativeAccessId(accessId);
        state.setCurrentFolderId(folderId);
        state.setView(normalizeViewMode(viewMode));
        state.setSort(normalizeSortMode(sortMode, "collaborative"));
        state.setPage(normalizePage(page));
        state.setSize(normalizePageSize(pageSize));
        state.setQ("");
        state.setScope("collaborative");

        var currentUser = userService.findById(userId);
        long filesSize = fileService.sumSizeByOwnerId(userId);
        long storageQuotaBytes = currentUser.getStorageQuotaBytes() == null ? 0L : currentUser.getStorageQuotaBytes();
        int storageUsagePercent = storageQuotaBytes <= 0
                ? 0
                : (int) Math.min(100L, Math.round((filesSize * 100.0d) / storageQuotaBytes));

        model.addAttribute("cabinetRoutes", cabinetRoutes);
        model.addAttribute("cabinetState", state);
        model.addAttribute("cabinetSection", "collaborative");
        model.addAttribute("libraryMode", "collaborative");
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("foldersCount", folderService.countByOwnerId(userId));
        model.addAttribute("filesSizeTitle", org.agty.utils.AgtyUtils.filesizeToTitle(filesSize, "ru"));
        model.addAttribute("storageQuotaTitle", currentUser.getStorageQuotaTitle());
        model.addAttribute("storageUsagePercent", storageUsagePercent);
        model.addAttribute("viewMode", state.getView());
        model.addAttribute("sortMode", state.getSort());
        model.addAttribute("searchQuery", "");
        model.addAttribute("searchScope", "collaborative");
        model.addAttribute("sharedStatusFilter", "all");
        model.addAttribute("sharedTypeFilter", "all");
        model.addAttribute("uploadExistingFileNamesByFolderId", java.util.Map.of());
        model.addAttribute("currentPage", 1);
        model.addAttribute("pageSize", state.getSize());
        model.addAttribute("pageSizeOptions", List.of(20, 50, 100));
        model.addAttribute("searchActive", false);
        model.addAttribute("folderShareLinksByFolderId", java.util.Map.of());
        model.addAttribute("shareLinksByFileId", java.util.Map.of());
        model.addAttribute("sharedItems", List.of());
        model.addAttribute("sharedSummary", java.util.Map.of());

        if (accessId == null) {
            model.addAttribute("pageTitle", "Совместный доступ");
            model.addAttribute("collaborativeBrowsing", false);
            model.addAttribute("providedCollaborativeFolders", collaborativeAccessService.findProvidedFolders(userId));
            model.addAttribute("receivedCollaborativeFolders", collaborativeAccessService.findReceivedFolders(userId));
            model.addAttribute("currentFolder", null);
            model.addAttribute("currentFolderId", null);
            model.addAttribute("breadcrumbs", List.of());
            model.addAttribute("folders", List.of());
            model.addAttribute("files", List.of());
            model.addAttribute("moveFolderOptions", List.of());
            model.addAttribute("uploadFolderOptions", List.of());
            model.addAttribute("totalPages", 1);
            model.addAttribute("totalItems", 0);
            return;
        }

        CollaborativeAccessDto access = collaborativeAccessService.resolveReceivedAccess(userId, accessId);
        model.addAttribute("uploadExistingFileNamesByFolderId", fileService.buildExistingFileNamesByFolderId(access == null ? null : access.getOwnerId()));
        model.addAttribute("collaborativeBrowsing", true);
        model.addAttribute("providedCollaborativeFolders", List.<CollaborativeFolderShareDto>of());
        model.addAttribute("receivedCollaborativeFolders", List.<CollaborativeFolderShareDto>of());
        model.addAttribute("collaborativeAccess", access);
        model.addAttribute("collaborativeCanWrite", collaborativeAccessService.canWrite(access));
        model.addAttribute("collaborativeCanDelete", collaborativeAccessService.canDelete(access));

        if (access == null) {
            model.addAttribute("pageTitle", "Совместный доступ");
            model.addAttribute("collaborativeError", "Совместный доступ не найден.");
            model.addAttribute("currentFolder", null);
            model.addAttribute("currentFolderId", null);
            model.addAttribute("breadcrumbs", List.of());
            model.addAttribute("folders", List.of());
            model.addAttribute("files", List.of());
            model.addAttribute("moveFolderOptions", List.of());
            model.addAttribute("uploadFolderOptions", List.of());
            model.addAttribute("totalPages", 1);
            model.addAttribute("totalItems", 0);
            return;
        }

        FolderDto rootFolder = folderService.findById(access.getFolderId());
        FolderDto currentFolder = collaborativeAccessService.resolveAccessibleFolder(access, folderId);
        model.addAttribute("pageTitle", currentFolder == null ? "Совместный доступ" : currentFolder.getName());
        model.addAttribute("collaborativeUnlocked", isCollaborativeUnlocked(access));
        model.addAttribute("collaborativeRootFolder", rootFolder);

        if (currentFolder == null || rootFolder == null) {
            model.addAttribute("collaborativeError", "Совместная папка недоступна.");
            model.addAttribute("currentFolder", null);
            model.addAttribute("currentFolderId", null);
            model.addAttribute("breadcrumbs", List.of());
            model.addAttribute("folders", List.of());
            model.addAttribute("files", List.of());
            model.addAttribute("moveFolderOptions", List.of());
            model.addAttribute("uploadFolderOptions", List.of());
            model.addAttribute("totalPages", 1);
            model.addAttribute("totalItems", 0);
            return;
        }

        model.addAttribute("currentFolder", currentFolder);
        model.addAttribute("currentFolderId", currentFolder.getId());
        model.addAttribute("breadcrumbs", buildCollaborativeBreadcrumbs(rootFolder, currentFolder));
        model.addAttribute("moveFolderOptions", folderService.buildMoveOptions(access.getOwnerId()));
        model.addAttribute("uploadFolderOptions", folderService.buildMoveOptions(access.getOwnerId()));

        if (!isCollaborativeUnlocked(access)) {
            model.addAttribute("folders", List.of());
            model.addAttribute("files", List.of());
            model.addAttribute("totalPages", 1);
            model.addAttribute("totalItems", 0);
            return;
        }

        List<FolderDto> folders = folderService.findByOwnerIdAndParentId(access.getOwnerId(), currentFolder.getId());
        List<FileItemDto> files = fileService.findByOwnerIdAndFolderId(access.getOwnerId(), currentFolder.getId());
        model.addAttribute("folders", folders);
        model.addAttribute("files", files);
        model.addAttribute("totalPages", 1);
        model.addAttribute("totalItems", folders.size() + files.size());
    }

    public void prepareItemModalState(Model model, String modalName, String resourceType, Long resourceId) {
        model.addAttribute("itemOpenModal", modalName);
        model.addAttribute("itemOpenResourceType", resourceType);
        model.addAttribute("itemOpenResourceId", resourceId);
    }

    public String redirectCabinet(String section,
                                  Long folderId,
                                  String viewMode,
                                  String sortMode,
                                  String searchQuery,
                                  String searchScope,
                                  Integer page,
                                  Integer pageSize) {
        CabinetPageStateDto state = new CabinetPageStateDto();
        state.setSection(section);
        state.setCurrentFolderId(folderId);
        state.setView(viewMode);
        state.setSort(sortMode);
        state.setQ(searchQuery);
        state.setScope(searchScope);
        state.setPage(page);
        state.setSize(pageSize);
        state.setShareStatus(resolveSharedStatusFilter());
        state.setShareType(resolveSharedTypeFilter());
        state.setCollaborativeAccessId(currentCollaborativeAccessId());
        return redirectCabinet(state);
    }

    public String redirectCabinet(CabinetPageStateDto state) {
        CabinetPageStateDto normalizedState = normalizeState(state);
        CabinetViewState viewState = buildViewState(
                normalizedState.getSection(),
                normalizedState.getView(),
                normalizedState.getSort(),
                normalizedState.getQ(),
                normalizedState.getScope(),
                normalizedState.getPage(),
                normalizedState.getSize()
        );
        String normalizedSection = normalizedState.getSection();
        StringBuilder builder = new StringBuilder("redirect:");
        builder.append(switch (normalizedSection) {
            case "photos" -> "/cabinet/photos";
            case "videos" -> "/cabinet/videos";
            case "shared" -> "/cabinet/shared";
            case "collaborative" -> "/cabinet/collaborative";
            default -> "/cabinet";
        });

        List<String> params = new ArrayList<>();
        if ("files".equals(normalizedSection) && normalizedState.getCurrentFolderId() != null) {
            params.add("folderId=" + normalizedState.getCurrentFolderId());
        }
        if ("collaborative".equals(normalizedSection) && normalizedState.getCollaborativeAccessId() != null) {
            params.add("accessId=" + normalizedState.getCollaborativeAccessId());
        }
        if ("collaborative".equals(normalizedSection) && normalizedState.getCurrentFolderId() != null) {
            params.add("folderId=" + normalizedState.getCurrentFolderId());
        }
        if (!"list".equals(viewState.viewMode())) {
            params.add("view=" + viewState.viewMode());
        }
        String defaultSort = defaultSort(normalizedSection);
        if (!defaultSort.equals(viewState.sortMode())) {
            params.add("sort=" + viewState.sortMode());
        }
        if (!viewState.searchQuery().isBlank()) {
            params.add("q=" + viewState.searchQuery());
        }
        if ("files".equals(normalizedSection) && !"current".equals(viewState.searchScope())) {
            params.add("scope=" + viewState.searchScope());
        }
        String sharedStatusFilter = normalizeSharedStatusFilter(normalizedState.getShareStatus());
        String sharedTypeFilter = normalizeSharedTypeFilter(normalizedState.getShareType());
        if ("shared".equals(normalizedSection) && sharedStatusFilter != null) {
            params.add("shareStatus=" + sharedStatusFilter);
        }
        if ("shared".equals(normalizedSection) && sharedTypeFilter != null) {
            params.add("shareType=" + sharedTypeFilter);
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

    public CabinetPageStateDto normalizeState(CabinetPageStateDto state) {
        CabinetPageStateDto normalized = new CabinetPageStateDto();
        String section = state == null ? null : state.getSection();
        String normalizedSection = normalizeSection(section);
        normalized.setSection(normalizedSection);
        normalized.setCurrentFolderId(state == null ? null : state.getCurrentFolderId());
        normalized.setView(normalizeViewMode(state == null ? null : state.getView()));
        normalized.setSort(normalizeSortMode(state == null ? null : state.getSort(), normalizedSection));
        normalized.setQ(normalizeSearchQuery(state == null ? null : state.getQ()));
        normalized.setScope(normalizeSearchScope(normalizedSection, state == null ? null : state.getScope()));
        normalized.setShareStatus(normalizeSharedStatusFilter(state == null ? null : state.getShareStatus()));
        normalized.setShareType(normalizeSharedTypeFilter(state == null ? null : state.getShareType()));
        normalized.setCollaborativeAccessId(state == null ? null : state.getCollaborativeAccessId());
        normalized.setPage(normalizePage(state == null ? null : state.getPage()));
        normalized.setSize(normalizePageSize(state == null ? null : state.getSize()));
        return normalized;
    }

    public String normalizeSection(String section) {
        if (section == null || section.isBlank()) {
            return "files";
        }
        return switch (section.trim().toLowerCase(Locale.ROOT)) {
            case "photos", "videos", "shared", "collaborative", "profile" -> section.trim().toLowerCase(Locale.ROOT);
            default -> "files";
        };
    }

    public String normalizeViewMode(String value) {
        return "grid".equalsIgnoreCase(value) ? "grid" : "list";
    }

    public String normalizeSearchQuery(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        return normalized.isBlank() ? "" : normalized;
    }

    public int normalizePage(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    public int normalizePageSize(Integer value) {
        if (value == null) {
            return 20;
        }
        return switch (value) {
            case 50, 100 -> value;
            default -> 20;
        };
    }

    public String normalizeSearchScope(String section, String value) {
        if (!"files".equals(normalizeSection(section))) {
            return normalizeSection(section);
        }
        if (value == null) {
            return "current";
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "tree", "all" -> value.trim().toLowerCase(Locale.ROOT);
            default -> "current";
        };
    }

    public String normalizeSortMode(String value, String section) {
        String normalizedSection = normalizeSection(section);
        if (value == null || value.isBlank()) {
            return defaultSort(normalizedSection);
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("shared".equals(normalizedSection)) {
            return switch (normalized) {
                case "name_asc", "name_desc", "date_newest", "date_oldest", "type_asc", "expiry_asc", "expiry_desc", "status_asc" -> normalized;
                case "size_asc" -> "expiry_asc";
                case "size_desc" -> "expiry_desc";
                default -> "date_newest";
            };
        }
        if (isMediaLibrarySection(normalizedSection)) {
            return switch (normalized) {
                case "name_asc", "name_desc", "date_newest", "date_oldest", "size_asc", "size_desc", "type_asc" -> normalized;
                default -> "date_newest";
            };
        }
        return switch (normalized) {
            case "name_desc", "date_newest", "date_oldest", "size_desc", "size_asc", "type_asc" -> normalized;
            default -> "name_asc";
        };
    }

    public CabinetViewState buildViewState(String section,
                                           String viewMode,
                                           String sortMode,
                                           String searchQuery,
                                           String searchScope,
                                           Integer page,
                                           Integer pageSize) {
        String normalizedSection = normalizeSection(section);
        String normalizedScope = normalizeSearchScope(normalizedSection, searchScope);
        return new CabinetViewState(
                normalizeViewMode(viewMode),
                normalizeSortMode(sortMode, normalizedSection),
                normalizeSearchQuery(searchQuery),
                normalizedScope,
                normalizePage(page),
                normalizePageSize(pageSize)
        );
    }

    public String buildPathKey(Long ownerId, Long parentId, String folderName) {
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

    public List<Long> parseIds(String value) {
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

    public String buildShareUrl(HttpServletRequest request, String token) {
        return applicationInfo.resolveBaseUri(request) + "/s/" + token;
    }

    public void log(Long ownerId, String actionCode, String resourceType, Long resourceId, String details) {
        auditLogService.log(ownerId, actionCode, resourceType, resourceId, details);
    }

    private boolean isMediaLibrarySection(String section) {
        return "photos".equals(section) || "videos".equals(section);
    }

    private boolean isSharedLibrarySection(String section) {
        return "shared".equals(section);
    }

    private String resolveCabinetPageTitle(String section, FolderDto currentFolder) {
        return switch (normalizeSection(section)) {
            case "photos" -> "Фото";
            case "videos" -> "Видео";
            case "shared" -> "Открытый доступ";
            case "collaborative" -> "Совместный доступ";
            case "profile" -> "Профиль";
            default -> currentFolder != null && currentFolder.getName() != null && !currentFolder.getName().isBlank()
                    ? currentFolder.getName()
                    : "Файлы";
        };
    }

    private String defaultSort(String section) {
        return switch (normalizeSection(section)) {
            case "photos", "videos", "shared", "collaborative" -> "date_newest";
            default -> "name_asc";
        };
    }

    private boolean isCollaborativeUnlocked(CollaborativeAccessDto access) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null || attributes.getRequest() == null) {
            return access != null && !access.isPasswordProtected();
        }
        return collaborativeAccessService.isUnlocked(attributes.getRequest().getSession(), access);
    }

    private List<FolderDto> buildCollaborativeBreadcrumbs(FolderDto rootFolder, FolderDto currentFolder) {
        if (rootFolder == null || currentFolder == null) {
            return List.of();
        }
        LinkedList<FolderDto> breadcrumbs = new LinkedList<>();
        FolderDto pointer = currentFolder;
        while (pointer != null) {
            breadcrumbs.addFirst(pointer);
            if (pointer.getId().equals(rootFolder.getId()) || pointer.getParentId() == null) {
                break;
            }
            pointer = folderService.findById(pointer.getParentId());
        }
        return breadcrumbs;
    }

    private Long currentCollaborativeAccessId() {
        String value = currentRequestParam("accessId");
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private List<FileItemDto> sliceFiles(List<FileItemDto> files, int offset, int pageSize) {
        int fromIndex = Math.min(offset, files.size());
        int toIndex = Math.min(files.size(), fromIndex + pageSize);
        return files.subList(fromIndex, toIndex);
    }

    private List<SharedLibraryItemDto> sliceSharedItems(List<SharedLibraryItemDto> items, int offset, int pageSize) {
        int fromIndex = Math.min(offset, items.size());
        int toIndex = Math.min(items.size(), fromIndex + pageSize);
        return items.subList(fromIndex, toIndex);
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

    private String resolveSharedStatusFilter() {
        return normalizeSharedStatusFilter(currentRequestParam("shareStatus"));
    }

    private String resolveSharedTypeFilter() {
        return normalizeSharedTypeFilter(currentRequestParam("shareType"));
    }

    private String normalizeSharedStatusFilter(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) {
            return null;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "active", "expiring", "expired", "without_expiry" -> value.trim().toLowerCase(Locale.ROOT);
            default -> null;
        };
    }

    private String normalizeSharedTypeFilter(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) {
            return null;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "files", "folders" -> value.trim().toLowerCase(Locale.ROOT);
            default -> null;
        };
    }

    private String currentRequestParam(String name) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null || attributes.getRequest() == null) {
            return null;
        }
        return attributes.getRequest().getParameter(name);
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
                totalItems,
                offset
        );
    }

    private CabinetPage paginateFlatEntries(int totalItems, int page, int pageSize) {
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));
        int currentPage = Math.min(page, totalPages);
        int offset = Math.max(0, (currentPage - 1) * pageSize);
        return new CabinetPage(0, 0, 0, 0, currentPage, totalPages, pageSize, totalItems, offset);
    }

    public record CabinetViewState(String viewMode,
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
                               int totalItems,
                               int pageOffset) {
    }
}
