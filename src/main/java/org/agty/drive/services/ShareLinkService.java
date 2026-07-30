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

package org.agty.drive.services;

import org.agty.drive.config.AppTime;
import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FolderDto;
import org.agty.drive.dto.ShareLinkCreateDto;
import org.agty.drive.dto.ShareLinkDto;
import org.agty.drive.dto.SharedLibraryItemDto;
import org.agty.drive.repository.ShareLinkRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ShareLinkService {

    private final ShareLinkRepository shareLinkRepository;
    private final FileService fileService;
    private final FolderService folderService;
    private final PasswordEncoder passwordEncoder;

    public ShareLinkService(ShareLinkRepository shareLinkRepository,
                            FileService fileService,
                            FolderService folderService,
                            PasswordEncoder passwordEncoder) {
        this.shareLinkRepository = shareLinkRepository;
        this.fileService = fileService;
        this.folderService = folderService;
        this.passwordEncoder = passwordEncoder;
    }

    public ShareLinkDto createShareLink(Long ownerId, ShareLinkCreateDto createDto) {
        if (ownerId == null || createDto == null || createDto.getResourceId() == null) {
            return null;
        }
        normalizeCreateDto(createDto);

        if ("FOLDER".equalsIgnoreCase(createDto.getResourceType())) {
            ShareLinkDto folderShare = createFolderShareLink(ownerId, createDto);
            if (folderShare != null) {
                return folderShare;
            }
        }
        if ("FILE".equalsIgnoreCase(createDto.getResourceType())) {
            ShareLinkDto fileShare = createFileShareLink(ownerId, createDto);
            if (fileShare != null) {
                return fileShare;
            }
        }

        FolderDto folder = folderService.findByIdAndOwnerId(createDto.getResourceId(), ownerId);
        if (folder != null) {
            createDto.setResourceType("FOLDER");
            return createFolderShareLink(ownerId, createDto);
        }

        FileItemDto file = fileService.findByIdAndOwnerId(createDto.getResourceId(), ownerId);
        if (file != null) {
            createDto.setResourceType("FILE");
            return createFileShareLink(ownerId, createDto);
        }

        return createFileShareLink(ownerId, createDto);
    }

    public String validateShareLinkCreate(Long ownerId, ShareLinkCreateDto createDto) {
        if (ownerId == null) {
            return "Пользователь не найден.";
        }
        if (createDto == null || createDto.getResourceId() == null) {
            return "Не выбран файл или папка для публичной ссылки.";
        }
        normalizeCreateDto(createDto);

        if ("FOLDER".equalsIgnoreCase(createDto.getResourceType())) {
            return folderService.findByIdAndOwnerId(createDto.getResourceId(), ownerId) == null
                    ? "Папка для публичной ссылки не найдена."
                    : null;
        }

        if ("FILE".equalsIgnoreCase(createDto.getResourceType())) {
            return fileService.findByIdAndOwnerId(createDto.getResourceId(), ownerId) == null
                    ? "Файл для публичной ссылки не найден."
                    : null;
        }

        if (folderService.findByIdAndOwnerId(createDto.getResourceId(), ownerId) != null) {
            return null;
        }
        if (fileService.findByIdAndOwnerId(createDto.getResourceId(), ownerId) != null) {
            return null;
        }
        return "Ресурс для публичной ссылки не найден.";
    }

    public ShareLinkDto createFileShareLink(Long ownerId, ShareLinkCreateDto createDto) {
        if (ownerId == null || createDto == null || createDto.getResourceId() == null) {
            return null;
        }

        FileItemDto file = fileService.findByIdAndOwnerId(createDto.getResourceId(), ownerId);
        if (file == null) {
            return null;
        }

        ShareLinkDto dto = new ShareLinkDto();
        dto.setCreatedBy(ownerId);
        dto.setToken(UUID.randomUUID().toString().replace("-", ""));
        dto.setResourceType("FILE");
        dto.setResourceId(file.getId());
        dto.setTitle(createDto.getTitle() == null || createDto.getTitle().isBlank() ? file.getOriginalFilename() : createDto.getTitle().trim());
        dto.setAllowDownload(!Boolean.FALSE.equals(createDto.getAllowDownload()));
        dto.setAllowPreview(Boolean.TRUE.equals(createDto.getAllowPreview()));
        dto.setIsEnabled(true);
        dto.setDownloadCount(0L);

        if (createDto.getPassword() != null && !createDto.getPassword().isBlank()) {
            dto.setPasswordHash(passwordEncoder.encode(createDto.getPassword()));
        }

        if (!Boolean.TRUE.equals(createDto.getExpiresUnlimited())
                && createDto.getExpiresInHours() != null
                && createDto.getExpiresInHours() > 0) {
            dto.setExpiresAt(AppTime.formatForDatabase(AppTime.now().plusHours(createDto.getExpiresInHours())));
        }

        return shareLinkRepository.save(dto);
    }

    public ShareLinkDto createFolderShareLink(Long ownerId, ShareLinkCreateDto createDto) {
        if (ownerId == null || createDto == null || createDto.getResourceId() == null) {
            return null;
        }

        FolderDto folder = folderService.findByIdAndOwnerId(createDto.getResourceId(), ownerId);
        if (folder == null) {
            return null;
        }

        ShareLinkDto dto = new ShareLinkDto();
        dto.setCreatedBy(ownerId);
        dto.setToken(UUID.randomUUID().toString().replace("-", ""));
        dto.setResourceType("FOLDER");
        dto.setResourceId(folder.getId());
        dto.setTitle(createDto.getTitle() == null || createDto.getTitle().isBlank() ? folder.getName() : createDto.getTitle().trim());
        dto.setAllowDownload(!Boolean.FALSE.equals(createDto.getAllowDownload()));
        dto.setAllowPreview(Boolean.TRUE.equals(createDto.getAllowPreview()));
        dto.setIsEnabled(true);
        dto.setDownloadCount(0L);

        if (createDto.getPassword() != null && !createDto.getPassword().isBlank()) {
            dto.setPasswordHash(passwordEncoder.encode(createDto.getPassword()));
        }

        if (!Boolean.TRUE.equals(createDto.getExpiresUnlimited())
                && createDto.getExpiresInHours() != null
                && createDto.getExpiresInHours() > 0) {
            dto.setExpiresAt(AppTime.formatForDatabase(AppTime.now().plusHours(createDto.getExpiresInHours())));
        }

        return shareLinkRepository.save(dto);
    }

    public ShareLinkDto findByToken(String token) {
        return shareLinkRepository.findByToken(token);
    }

    public ShareLinkDto findLatestFileShareLink(Long fileId) {
        return shareLinkRepository.findLatestByResource("FILE", fileId);
    }

    public ShareLinkDto findLatestFolderShareLink(Long folderId) {
        return shareLinkRepository.findLatestByResource("FOLDER", folderId);
    }

    public Map<Long, ShareLinkDto> findLatestFileShareLinks(List<FileItemDto> files) {
        Map<Long, ShareLinkDto> result = new LinkedHashMap<>();
        if (files == null || files.isEmpty()) {
            return result;
        }
        List<Long> fileIds = files.stream().map(FileItemDto::getId).toList();
        for (ShareLinkDto item : shareLinkRepository.findLatestByResourceTypeAndIds("FILE", fileIds)) {
            result.put(item.getResourceId(), item);
        }
        return result;
    }

    public Map<Long, ShareLinkDto> findLatestFolderShareLinks(List<FolderDto> folders) {
        Map<Long, ShareLinkDto> result = new LinkedHashMap<>();
        if (folders == null || folders.isEmpty()) {
            return result;
        }
        List<Long> folderIds = folders.stream().map(FolderDto::getId).toList();
        for (ShareLinkDto item : shareLinkRepository.findLatestByResourceTypeAndIds("FOLDER", folderIds)) {
            result.put(item.getResourceId(), item);
        }
        return result;
    }

    public List<SharedLibraryItemDto> findActiveLibraryByCreator(Long ownerId) {
        return findActiveLibraryByCreator(ownerId, null, null);
    }

    public List<SharedLibraryItemDto> findActiveLibraryByCreator(Long ownerId,
                                                                 String query,
                                                                 String sortMode) {
        return findLibraryByCreator(ownerId, query, sortMode, null, null);
    }

    public List<SharedLibraryItemDto> findLibraryByCreator(Long ownerId,
                                                           String query,
                                                           String sortMode,
                                                           String statusFilter,
                                                           String typeFilter) {
        if (ownerId == null) {
            return List.of();
        }

        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String normalizedStatusFilter = normalizeSharedStatusFilter(statusFilter);
        String normalizedTypeFilter = normalizeSharedTypeFilter(typeFilter);

        return buildOwnedSharedLibrary(ownerId).stream()
                .filter(item -> item != null)
                .filter(item -> normalizedQuery.isBlank()
                        || containsIgnoreCase(item.getResourceName(), normalizedQuery)
                        || containsIgnoreCase(item.getShareLink().getTitle(), normalizedQuery))
                .filter(item -> normalizedTypeFilter == null
                        || ("folders".equals(normalizedTypeFilter) && item.isFolder())
                        || ("files".equals(normalizedTypeFilter) && item.isFile()))
                .filter(item -> normalizedStatusFilter == null || normalizedStatusFilter.equals(item.getStatusCode()))
                .sorted(buildSharedLibraryComparator(sortMode))
                .collect(Collectors.toList());
    }

    private List<SharedLibraryItemDto> buildOwnedSharedLibrary(Long ownerId) {
        Map<Long, ShareLinkDto> latestFolderShareLinks = findLatestFolderShareLinks(folderService.findAllByOwnerId(ownerId));
        Map<Long, ShareLinkDto> latestFileShareLinks = findLatestFileShareLinks(fileService.findAllByOwnerId(ownerId));
        List<SharedLibraryItemDto> items = new java.util.ArrayList<>();

        for (FolderDto folder : folderService.findAllByOwnerId(ownerId)) {
            ShareLinkDto shareLink = latestFolderShareLinks.get(folder.getId());
            if (shareLink == null) {
                continue;
            }
            items.add(new SharedLibraryItemDto(shareLink, folder.getName(), "Папка"));
        }

        for (FileItemDto file : fileService.findAllByOwnerId(ownerId)) {
            ShareLinkDto shareLink = latestFileShareLinks.get(file.getId());
            if (shareLink == null) {
                continue;
            }
            SharedLibraryItemDto libraryItem = new SharedLibraryItemDto(shareLink, file.getOriginalFilename(), "Файл");
            libraryItem.setResourceMimeType(file.getMimeType());
            libraryItem.setResourceSizeBytes(file.getSizeBytes());
            libraryItem.setPreviewAvailable(file.isPreviewAvailable());
            libraryItem.setPreviewType(file.getPreviewType());
            items.add(libraryItem);
        }

        return items;
    }

    private void normalizeCreateDto(ShareLinkCreateDto createDto) {
        if (createDto == null) {
            return;
        }
        if (createDto.getTitle() != null) {
            createDto.setTitle(createDto.getTitle().trim());
        }
        if (createDto.getPassword() != null) {
            createDto.setPassword(createDto.getPassword().trim());
        }
        if (Boolean.TRUE.equals(createDto.getExpiresUnlimited())) {
            createDto.setExpiresInHours(null);
            return;
        }
        if (createDto.getExpiresInHours() != null && createDto.getExpiresInHours() <= 0) {
            createDto.setExpiresInHours(null);
        }
    }

    public Map<String, Long> summarizeLibraryStatuses(List<SharedLibraryItemDto> items) {
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("all", 0L);
        summary.put("active", 0L);
        summary.put("expiring", 0L);
        summary.put("expired", 0L);
        summary.put("without_expiry", 0L);
        summary.put("files", 0L);
        summary.put("folders", 0L);
        if (items == null || items.isEmpty()) {
            return summary;
        }

        for (SharedLibraryItemDto item : items) {
            summary.compute("all", (key, value) -> value == null ? 1L : value + 1L);
            summary.compute(item.getStatusCode(), (key, value) -> value == null ? 1L : value + 1L);
            summary.compute(item.isFolder() ? "folders" : "files", (key, value) -> value == null ? 1L : value + 1L);
        }
        return summary;
    }

    public boolean verifyPassword(ShareLinkDto shareLink, String password) {
        if (shareLink == null) {
            return false;
        }
        if (shareLink.getPasswordHash() == null || shareLink.getPasswordHash().isBlank()) {
            return true;
        }
        return password != null && passwordEncoder.matches(password, shareLink.getPasswordHash());
    }

    public boolean isAccessible(ShareLinkDto shareLink) {
        if (shareLink == null || Boolean.FALSE.equals(shareLink.getIsEnabled())) {
            return false;
        }
        if (shareLink.getMaxDownloads() != null
                && shareLink.getMaxDownloads() > 0
                && shareLink.getDownloadCount() != null
                && shareLink.getDownloadCount() >= shareLink.getMaxDownloads()) {
            return false;
        }
        if (shareLink.getExpiresAt() == null || shareLink.getExpiresAt().isBlank()) {
            return true;
        }
        var expiresAt = AppTime.parseDatabaseDateTime(shareLink.getExpiresAt());
        return expiresAt == null || expiresAt.isAfter(AppTime.now());
    }

    public void registerDownload(ShareLinkDto shareLink) {
        if (shareLink == null || shareLink.getId() == null) {
            return;
        }
        Long current = shareLink.getDownloadCount() == null ? 0L : shareLink.getDownloadCount();
        shareLink.setDownloadCount(current + 1);
        shareLinkRepository.save(shareLink);
    }

    public String deleteShareLink(Long ownerId, String resourceType, Long resourceId) {
        if (ownerId == null) {
            return "Пользователь не найден.";
        }
        if (resourceType == null || resourceType.isBlank() || resourceId == null) {
            return "Не выбран файл или папка для удаления публичной ссылки.";
        }

        if ("FOLDER".equalsIgnoreCase(resourceType) && folderService.findByIdAndOwnerId(resourceId, ownerId) == null) {
            return "Папка не найдена.";
        }
        if ("FILE".equalsIgnoreCase(resourceType) && fileService.findByIdAndOwnerId(resourceId, ownerId) == null) {
            return "Файл не найден.";
        }

        ShareLinkDto shareLink = shareLinkRepository.findLatestByResource(resourceType, resourceId);
        if (shareLink == null) {
            return "Публичная ссылка не найдена.";
        }
        if (!ownerId.equals(shareLink.getCreatedBy())) {
            return "Недостаточно прав для удаления публичной ссылки.";
        }

        int disabled = shareLinkRepository.disableByResource(resourceType, resourceId, ownerId);
        return disabled > 0 ? null : "Не удалось удалить публичную ссылку.";
    }

    private Comparator<SharedLibraryItemDto> buildSharedLibraryComparator(String sortMode) {
        Comparator<SharedLibraryItemDto> foldersFirst = Comparator
                .comparing((SharedLibraryItemDto item) -> !item.isFolder());
        Comparator<SharedLibraryItemDto> byNameAsc = Comparator
                .comparing((SharedLibraryItemDto item) -> normalizedString(item.getResourceName()))
                .thenComparing(item -> normalizedString(item.getShareLink() == null ? null : item.getShareLink().getToken()));
        Comparator<SharedLibraryItemDto> byDateNewest = Comparator
                .comparing((SharedLibraryItemDto item) -> AppTime.parseDatabaseDateTime(item.getShareLink() == null ? null : item.getShareLink().getCreatedAt()),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing((SharedLibraryItemDto item) -> normalizedString(item.getResourceName()));
        Comparator<SharedLibraryItemDto> byDateOldest = Comparator
                .comparing((SharedLibraryItemDto item) -> AppTime.parseDatabaseDateTime(item.getShareLink() == null ? null : item.getShareLink().getCreatedAt()),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing((SharedLibraryItemDto item) -> normalizedString(item.getResourceName()));
        Comparator<SharedLibraryItemDto> byTypeAsc = Comparator
                .comparing((SharedLibraryItemDto item) -> normalizedString(item.getResourceTypeTitle()))
                .thenComparing((SharedLibraryItemDto item) -> normalizedString(item.getResourceName()));
        Comparator<SharedLibraryItemDto> byExpiryAsc = Comparator
                .comparing((SharedLibraryItemDto item) -> AppTime.parseDatabaseDateTime(item.getShareLink() == null ? null : item.getShareLink().getExpiresAt()),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing((SharedLibraryItemDto item) -> normalizedString(item.getResourceName()));
        Comparator<SharedLibraryItemDto> byExpiryDesc = Comparator
                .comparing((SharedLibraryItemDto item) -> AppTime.parseDatabaseDateTime(item.getShareLink() == null ? null : item.getShareLink().getExpiresAt()),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing((SharedLibraryItemDto item) -> normalizedString(item.getResourceName()));
        Comparator<SharedLibraryItemDto> byStatusAsc = Comparator
                .comparingInt((SharedLibraryItemDto item) -> sharedStatusWeight(item.getStatusCode()))
                .thenComparing(byExpiryAsc)
                .thenComparing((SharedLibraryItemDto item) -> normalizedString(item.getResourceName()));

        String normalizedSortMode = sortMode == null ? "" : sortMode.trim().toLowerCase(Locale.ROOT);
        Comparator<SharedLibraryItemDto> baseComparator = switch (normalizedSortMode) {
            case "name_desc" -> byNameAsc.reversed();
            case "date_oldest" -> byDateOldest;
            case "type_asc" -> byTypeAsc;
            case "expiry_asc", "size_asc" -> byExpiryAsc;
            case "expiry_desc", "size_desc" -> byExpiryDesc;
            case "status_asc" -> byStatusAsc;
            case "date_newest" -> byDateNewest;
            default -> byNameAsc;
        };
        return foldersFirst.thenComparing(baseComparator);
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

    private int sharedStatusWeight(String statusCode) {
        return switch (statusCode == null ? "" : statusCode) {
            case "expired" -> 0;
            case "expiring" -> 1;
            case "active" -> 2;
            case "without_expiry" -> 3;
            default -> 4;
        };
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String normalizedString(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

}
