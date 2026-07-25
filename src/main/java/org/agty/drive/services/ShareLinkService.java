package org.agty.drive.services;

import org.agty.drive.config.AppTime;
import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FolderDto;
import org.agty.drive.dto.ShareLinkCreateDto;
import org.agty.drive.dto.ShareLinkDto;
import org.agty.drive.repository.ShareLinkRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

}
