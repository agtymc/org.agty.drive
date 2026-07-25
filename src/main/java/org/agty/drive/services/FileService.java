package org.agty.drive.services;

import org.agty.drive.config.AppTime;
import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FileUploadDto;
import org.agty.drive.dto.FolderDto;
import org.agty.drive.dto.UserDto;
import org.agty.drive.repository.FileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
@Service
public class FileService {

    private static final long DEFAULT_STORAGE_QUOTA_BYTES = 100L * 1024L * 1024L;

    private final FileRepository fileRepository;
    private final FolderService folderService;
    private final FileContentStorageService fileContentStorageService;
    private final ImageThumbnailService imageThumbnailService;
    private final UserService userService;

    public FileService(FileRepository fileRepository,
                       FolderService folderService,
                       FileContentStorageService fileContentStorageService,
                       ImageThumbnailService imageThumbnailService,
                       UserService userService) {
        this.fileRepository = fileRepository;
        this.folderService = folderService;
        this.fileContentStorageService = fileContentStorageService;
        this.imageThumbnailService = imageThumbnailService;
        this.userService = userService;
    }

    public List<FileItemDto> findAllByOwnerId(Long ownerId) {
        return fileRepository.findAllByOwnerId(ownerId);
    }

    public List<FileItemDto> findByOwnerIdAndFolderId(Long ownerId, Long folderId) {
        return fileRepository.findByOwnerIdAndFolderId(ownerId, folderId);
    }

    public List<FileItemDto> searchByOwnerId(Long ownerId,
                                             String query,
                                             Long currentFolderId,
                                             String currentFolderPath,
                                             String scope) {
        return fileRepository.searchByOwnerId(ownerId, query, currentFolderId, currentFolderPath, scope);
    }

    public long countSearchByOwnerId(Long ownerId,
                                     String query,
                                     Long currentFolderId,
                                     String currentFolderPath,
                                     String scope) {
        return fileRepository.countSearchByOwnerId(ownerId, query, currentFolderId, currentFolderPath, scope);
    }

    public long countAll() {
        return fileRepository.countAll();
    }

    public List<FileItemDto> searchByOwnerId(Long ownerId,
                                             String query,
                                             Long currentFolderId,
                                             String currentFolderPath,
                                             String scope,
                                             String sortMode,
                                             int offset,
                                             int limit) {
        return fileRepository.searchByOwnerId(ownerId, query, currentFolderId, currentFolderPath, scope, sortMode, offset, limit);
    }

    public long sumSizeByOwnerId(Long ownerId) {
        return fileRepository.sumSizeByOwnerId(ownerId);
    }

    public long sumSizeAll() {
        return fileRepository.sumSizeAll();
    }

    public FileItemDto findByIdAndOwnerId(Long id, Long ownerId) {
        return fileRepository.findByIdAndOwnerId(id, ownerId);
    }

    public FileItemDto findById(Long id) {
        return fileRepository.findById(id);
    }

    public byte[] findContentBytesByFileId(Long fileId) {
        FileItemDto fileItemDto = fileRepository.findById(fileId);
        if (fileItemDto == null) {
            return null;
        }

        return fileContentStorageService.read(fileItemDto.getStorageName());
    }

    public byte[] findThumbnailBytesByFileId(Long fileId) {
        FileItemDto fileItemDto = fileRepository.findById(fileId);
        if (fileItemDto == null || fileItemDto.getStorageName() == null || fileItemDto.getStorageName().isBlank()) {
            return null;
        }

        return imageThumbnailService.readThumbnail(fileItemDto.getStorageName());
    }

    public String validateUpload(Long ownerId, FileUploadDto uploadDto) {
        if (ownerId == null || uploadDto == null) {
            return "Пользователь не найден.";
        }

        MultipartFile multipartFile = uploadDto.getFile();
        if (multipartFile == null || multipartFile.isEmpty()) {
            return "Файл не выбран.";
        }

        if (uploadDto.getFolderId() == null || !folderExistsForOwner(ownerId, uploadDto.getFolderId())) {
            return "Папка для загрузки не найдена.";
        }

        UserDto user = userService.findById(ownerId);
        long quotaBytes = user == null || user.getStorageQuotaBytes() == null || user.getStorageQuotaBytes() <= 0
                ? DEFAULT_STORAGE_QUOTA_BYTES
                : user.getStorageQuotaBytes();
        long usedBytes = sumSizeByOwnerId(ownerId);
        long fileSize = Math.max(0L, multipartFile.getSize());

        if (usedBytes + fileSize > quotaBytes) {
            return "Недостаточно места по квоте. Доступно: %s."
                    .formatted(org.agty.utils.AgtyUtils.filesizeToTitle(Math.max(0L, quotaBytes - usedBytes), "ru"));
        }

        return null;
    }

    public FileItemDto upload(Long ownerId, FileUploadDto uploadDto) {
        String validationError = validateUpload(ownerId, uploadDto);
        if (validationError != null) {
            return null;
        }

        MultipartFile multipartFile = uploadDto.getFile();
        try {
            byte[] content = multipartFile.getBytes();
            String originalFilename = multipartFile.getOriginalFilename();
            String extension = extractExtension(originalFilename);
            String mimeType = multipartFile.getContentType();

            FileItemDto fileItemDto = new FileItemDto();
            fileItemDto.setOwnerId(ownerId);
            fileItemDto.setFolderId(uploadDto.getFolderId());
            fileItemDto.setOriginalFilename(originalFilename == null || originalFilename.isBlank() ? "file" : originalFilename);
            fileItemDto.setMimeType(mimeType == null || mimeType.isBlank() ? "application/octet-stream" : mimeType);
            fileItemDto.setExtension(extension);
            fileItemDto.setSizeBytes(multipartFile.getSize());
            fileItemDto.setChecksumSha256(calculateSha256(content));
            fileItemDto.setStorageName(StoragePathSupport.buildStorageName(
                    fileItemDto.getChecksumSha256(),
                    extension,
                    AppTime.today()
            ));
            fileItemDto.setDescription(uploadDto.getDescription());
            fileItemDto.setPreviewStatus("NONE");
            fileItemDto.setIsImage(fileItemDto.getMimeType().startsWith("image/"));
            fileItemDto.setIsVideo(fileItemDto.getMimeType().startsWith("video/"));

            FileItemDto saved = fileRepository.save(fileItemDto);
            if (saved == null || saved.getId() == null) {
                return null;
            }

            fileContentStorageService.save(saved.getStorageName(), content);
            if (saved.isImagePreview()) {
                saved.setPreviewStatus(imageThumbnailService.generateForFile(saved, content) ? "READY" : "FAILED");
                FileItemDto updated = fileRepository.save(saved);
                return updated == null ? saved : updated;
            }
            return saved;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String deleteByIdAndOwnerId(Long id, Long ownerId) {
        FileItemDto fileItemDto = findByIdAndOwnerId(id, ownerId);
        if (fileItemDto == null) {
            return "Файл не найден.";
        }

        String storageName = fileItemDto.getStorageName();
        fileItemDto.setDeletedAt(AppTime.nowForDatabase());
        FileItemDto saved = fileRepository.save(fileItemDto);
        if (saved == null) {
            return "Не удалось удалить файл.";
        }

        if (storageName != null && !storageName.isBlank() && fileRepository.countActiveByStorageName(storageName, fileItemDto.getId()) == 0L) {
            fileContentStorageService.delete(storageName);
            imageThumbnailService.deleteThumbnail(storageName);
        }
        return null;
    }

    public String renameByIdAndOwnerId(Long id, Long ownerId, String newName) {
        FileItemDto fileItemDto = findByIdAndOwnerId(id, ownerId);
        if (fileItemDto == null) {
            return "Файл не найден.";
        }

        String normalizedName = normalizeFilename(newName);
        if (normalizedName == null) {
            return "Введите название файла.";
        }

        if (fileRepository.existsByOwnerIdAndFolderIdAndOriginalFilename(ownerId, fileItemDto.getFolderId(), normalizedName, fileItemDto.getId())) {
            return "В этой директории уже есть файл с таким названием.";
        }

        fileItemDto.setOriginalFilename(normalizedName);
        fileItemDto.setExtension(extractExtension(normalizedName));
        return fileRepository.save(fileItemDto) == null ? "Не удалось переименовать файл." : null;
    }

    public String moveByIdAndOwnerId(Long id, Long ownerId, Long targetFolderId) {
        FileItemDto fileItemDto = findByIdAndOwnerId(id, ownerId);
        if (fileItemDto == null) {
            return "Файл не найден.";
        }

        if (targetFolderId != null && !folderExistsForOwner(ownerId, targetFolderId)) {
            return "Целевая директория не найдена.";
        }

        if (fileItemDto.getFolderId() == null && targetFolderId == null) {
            return null;
        }
        if (fileItemDto.getFolderId() != null && fileItemDto.getFolderId().equals(targetFolderId)) {
            return null;
        }

        if (fileRepository.existsByOwnerIdAndFolderIdAndOriginalFilename(ownerId, targetFolderId, fileItemDto.getOriginalFilename(), fileItemDto.getId())) {
            return "В целевой директории уже есть файл с таким названием.";
        }

        fileItemDto.setFolderId(targetFolderId);
        return fileRepository.save(fileItemDto) == null ? "Не удалось переместить файл." : null;
    }

    private boolean folderExistsForOwner(Long ownerId, Long folderId) {
        if (folderId == null) {
            return true;
        }

        return folderService.findByIdAndOwnerId(folderId, ownerId) != null;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return null;
        }

        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return null;
        }

        return originalFilename.substring(dotIndex + 1).toLowerCase();
    }

    private String normalizeFilename(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String calculateSha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
