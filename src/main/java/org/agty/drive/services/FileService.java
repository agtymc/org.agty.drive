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
import org.agty.drive.dto.FileUploadDto;
import org.agty.drive.dto.FolderDto;
import org.agty.drive.repository.ShareLinkRepository;
import org.agty.drive.dto.UserDto;
import org.agty.drive.repository.FileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
@Service
public class FileService {

    private static final long DEFAULT_STORAGE_QUOTA_BYTES = 100L * 1024L * 1024L;

    private final FileRepository fileRepository;
    private final FolderService folderService;
    private final FileContentStorageService fileContentStorageService;
    private final ImageThumbnailService imageThumbnailService;
    private final UserService userService;
    private final FilenamePolicyService filenamePolicyService;
    private final MimeTypePolicyService mimeTypePolicyService;
    private final ExpirationPolicyService expirationPolicyService;
    private final ShareLinkRepository shareLinkRepository;

    public FileService(FileRepository fileRepository,
                       FolderService folderService,
                       FileContentStorageService fileContentStorageService,
                       ImageThumbnailService imageThumbnailService,
                       UserService userService,
                       FilenamePolicyService filenamePolicyService,
                       MimeTypePolicyService mimeTypePolicyService,
                       ExpirationPolicyService expirationPolicyService,
                       ShareLinkRepository shareLinkRepository) {
        this.fileRepository = fileRepository;
        this.folderService = folderService;
        this.fileContentStorageService = fileContentStorageService;
        this.imageThumbnailService = imageThumbnailService;
        this.userService = userService;
        this.filenamePolicyService = filenamePolicyService;
        this.mimeTypePolicyService = mimeTypePolicyService;
        this.expirationPolicyService = expirationPolicyService;
        this.shareLinkRepository = shareLinkRepository;
    }

    public List<FileItemDto> findAllByOwnerId(Long ownerId) {
        return fileRepository.findAllByOwnerId(ownerId);
    }

    public List<FileItemDto> findMediaLibraryByOwnerId(Long ownerId,
                                                       String query,
                                                       String scope,
                                                       String sortMode) {
        String normalizedScope = scope == null ? "" : scope.trim().toLowerCase(Locale.ROOT);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return findAllByOwnerId(ownerId).stream()
                .filter(file -> "photos".equals(normalizedScope) ? file.isImagePreview() : file.isVideoPreview())
                .filter(file -> normalizedQuery.isBlank()
                        || containsIgnoreCase(file.getOriginalFilename(), normalizedQuery)
                        || containsIgnoreCase(file.getDescription(), normalizedQuery))
                .sorted(buildMediaComparator(sortMode))
                .toList();
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

    public long countByOwnerId(Long ownerId) {
        return fileRepository.countByOwnerId(ownerId);
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

    public InputStream openContentStream(Long fileId, Long ownerId) {
        FileItemDto fileItemDto = ownerId == null
                ? findById(fileId)
                : findByIdAndOwnerId(fileId, ownerId);
        if (fileItemDto == null) {
            return null;
        }
        return fileContentStorageService.openStream(fileItemDto.getStorageName());
    }

    public String validateUpload(Long ownerId, FileUploadDto uploadDto) {
        if (ownerId == null || uploadDto == null) {
            return "Пользователь не найден.";
        }

        MultipartFile multipartFile = uploadDto.getFile();
        if (multipartFile == null || multipartFile.isEmpty()) {
            return "Файл не выбран.";
        }

        if (filenamePolicyService.normalizeFilename(multipartFile.getOriginalFilename()) == null) {
            return "Некорректное имя файла.";
        }

        if (uploadDto.getFolderId() != null && !folderExistsForOwner(ownerId, uploadDto.getFolderId())) {
            return "Папка для загрузки не найдена.";
        }

        String normalizedFilename = filenamePolicyService.normalizeFilename(multipartFile.getOriginalFilename());
        if (fileRepository.existsByOwnerIdAndFolderIdAndOriginalFilename(ownerId, uploadDto.getFolderId(), normalizedFilename, null)
                && !Boolean.TRUE.equals(uploadDto.getOverwriteExisting())) {
            return "В этой директории уже есть файл с таким названием.";
        }

        String expirationError = expirationPolicyService.validateExpirationInput(uploadDto.getExpiresAt());
        if (expirationError != null) {
            return expirationError;
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
        Path tempPath = null;
        String finalStorageName = null;
        boolean repositorySaved = false;
        try {
            String originalFilename = filenamePolicyService.normalizeFilename(multipartFile.getOriginalFilename());
            String extension = extractExtension(originalFilename);
            String mimeType = mimeTypePolicyService.normalizeUploadedMimeType(multipartFile.getContentType(), extension);
            tempPath = fileContentStorageService.createTempFile();
            String checksumSha256;
            long actualSizeBytes = 0L;
            MessageDigest digest = newSha256Digest();

            try (InputStream inputStream = multipartFile.getInputStream();
                 OutputStream outputStream = Files.newOutputStream(tempPath)) {
                byte[] buffer = new byte[1024 * 1024];
                int read;
                while ((read = inputStream.read(buffer)) >= 0) {
                    if (read == 0) {
                        continue;
                    }
                    digest.update(buffer, 0, read);
                    outputStream.write(buffer, 0, read);
                    actualSizeBytes += read;
                }
            }
            checksumSha256 = HexFormat.of().formatHex(digest.digest());

            if (Boolean.TRUE.equals(uploadDto.getOverwriteExisting())) {
                FileItemDto existingFile = fileRepository.findByOwnerIdAndFolderIdAndOriginalFilename(ownerId, uploadDto.getFolderId(), originalFilename);
                if (existingFile != null) {
                    String deleteError = deleteByIdAndOwnerId(existingFile.getId(), ownerId);
                    if (deleteError != null) {
                        return null;
                    }
                }
            }

            FileItemDto fileItemDto = new FileItemDto();
            fileItemDto.setOwnerId(ownerId);
            fileItemDto.setFolderId(uploadDto.getFolderId());
            fileItemDto.setOriginalFilename(originalFilename == null || originalFilename.isBlank() ? "file" : originalFilename);
            fileItemDto.setMimeType(mimeType);
            fileItemDto.setExtension(extension);
            fileItemDto.setSizeBytes(actualSizeBytes);
            fileItemDto.setChecksumSha256(checksumSha256);
            finalStorageName = StoragePathSupport.buildStorageName(
                    fileItemDto.getChecksumSha256(),
                    extension,
                    AppTime.today()
            );
            fileItemDto.setStorageName(finalStorageName);
            fileItemDto.setDescription(uploadDto.getDescription());
            fileItemDto.setExpiresAt(expirationPolicyService.normalizeExpirationInput(uploadDto.getExpiresAt()));
            fileItemDto.setPreviewStatus("NONE");
            fileItemDto.setIsImage(fileItemDto.getMimeType().startsWith("image/"));
            fileItemDto.setIsVideo(fileItemDto.getMimeType().startsWith("video/"));

            fileContentStorageService.moveIntoStorage(tempPath, finalStorageName);
            tempPath = null;

            FileItemDto saved = saveUploadedFileRecord(
                    fileItemDto,
                    ownerId,
                    uploadDto.getFolderId(),
                    originalFilename,
                    Boolean.TRUE.equals(uploadDto.getOverwriteExisting())
            );
            if (saved == null || saved.getId() == null) {
                fileContentStorageService.delete(finalStorageName);
                return null;
            }
            repositorySaved = true;

            if (saved.isImagePreview()) {
                saved.setPreviewStatus(imageThumbnailService.generateForFile(saved) ? "READY" : "FAILED");
                FileItemDto updated = fileRepository.save(saved);
                return updated == null ? saved : updated;
            }
            return saved;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            if (finalStorageName != null && !repositorySaved) {
                try {
                    fileContentStorageService.delete(finalStorageName);
                } catch (RuntimeException ignored) {
                }
            }
            throw e;
        } finally {
            if (tempPath != null) {
                try {
                    fileContentStorageService.deleteTempFile(tempPath);
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    public FileItemDto uploadStream(Long ownerId,
                                    Long folderId,
                                    String originalFilename,
                                    String contentType,
                                    InputStream inputStream,
                                    boolean overwriteExisting) {
        String normalizedFilename = filenamePolicyService.normalizeFilename(originalFilename);
        if (ownerId == null || normalizedFilename == null || inputStream == null) {
            return null;
        }
        if (folderId != null && !folderExistsForOwner(ownerId, folderId)) {
            return null;
        }

        Path tempPath = null;
        String finalStorageName = null;
        boolean repositorySaved = false;
        try {
            String extension = extractExtension(normalizedFilename);
            String mimeType = mimeTypePolicyService.normalizeUploadedMimeType(contentType, extension);
            tempPath = fileContentStorageService.createTempFile();
            MessageDigest digest = newSha256Digest();
            long actualSizeBytes = 0L;

            try (OutputStream outputStream = Files.newOutputStream(tempPath)) {
                byte[] buffer = new byte[1024 * 1024];
                int read;
                while ((read = inputStream.read(buffer)) >= 0) {
                    if (read == 0) {
                        continue;
                    }
                    digest.update(buffer, 0, read);
                    outputStream.write(buffer, 0, read);
                    actualSizeBytes += read;
                }
            }

            FileItemDto existingFile = fileRepository.findByOwnerIdAndFolderIdAndOriginalFilename(ownerId, folderId, normalizedFilename);
            if (existingFile != null && !overwriteExisting) {
                return null;
            }

            long quotaBytes = resolveQuotaBytes(ownerId);
            long usedBytes = sumSizeByOwnerId(ownerId);
            long existingSize = existingFile == null || existingFile.getSizeBytes() == null ? 0L : Math.max(0L, existingFile.getSizeBytes());
            long projectedUsage = overwriteExisting ? usedBytes - existingSize + actualSizeBytes : usedBytes + actualSizeBytes;
            if (projectedUsage > quotaBytes) {
                return null;
            }

            String checksumSha256 = HexFormat.of().formatHex(digest.digest());
            FileItemDto fileItemDto = new FileItemDto();
            fileItemDto.setOwnerId(ownerId);
            fileItemDto.setFolderId(folderId);
            fileItemDto.setOriginalFilename(normalizedFilename);
            fileItemDto.setMimeType(mimeType);
            fileItemDto.setExtension(extension);
            fileItemDto.setSizeBytes(actualSizeBytes);
            fileItemDto.setChecksumSha256(checksumSha256);
            finalStorageName = StoragePathSupport.buildStorageName(
                    checksumSha256,
                    extension,
                    AppTime.today()
            );
            fileItemDto.setStorageName(finalStorageName);
            fileItemDto.setPreviewStatus("NONE");
            fileItemDto.setIsImage(fileItemDto.getMimeType().startsWith("image/"));
            fileItemDto.setIsVideo(fileItemDto.getMimeType().startsWith("video/"));

            fileContentStorageService.moveIntoStorage(tempPath, finalStorageName);
            tempPath = null;

            FileItemDto saved = saveUploadedFileRecord(
                    fileItemDto,
                    ownerId,
                    folderId,
                    normalizedFilename,
                    overwriteExisting
            );
            if (saved == null || saved.getId() == null) {
                fileContentStorageService.delete(finalStorageName);
                return null;
            }
            repositorySaved = true;

            if (saved.isImagePreview()) {
                saved.setPreviewStatus(imageThumbnailService.generateForFile(saved) ? "READY" : "FAILED");
                FileItemDto updated = fileRepository.save(saved);
                return updated == null ? saved : updated;
            }
            return saved;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            if (finalStorageName != null && !repositorySaved) {
                try {
                    fileContentStorageService.delete(finalStorageName);
                } catch (RuntimeException ignored) {
                }
            }
            throw e;
        } finally {
            if (tempPath != null) {
                try {
                    fileContentStorageService.deleteTempFile(tempPath);
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    private FileItemDto saveUploadedFileRecord(FileItemDto fileItemDto,
                                               Long ownerId,
                                               Long folderId,
                                               String originalFilename,
                                               boolean overwriteExisting) {
        try {
            return fileRepository.save(fileItemDto);
        } catch (RuntimeException exception) {
            if (!overwriteExisting || !isFileNameUniqueConflict(exception)) {
                throw exception;
            }

            FileItemDto existingFile = fileRepository.findByOwnerIdAndFolderIdAndOriginalFilename(ownerId, folderId, originalFilename);
            if (existingFile == null) {
                throw exception;
            }

            String deleteError = deleteByIdAndOwnerId(existingFile.getId(), ownerId);
            if (deleteError != null) {
                throw new RuntimeException(deleteError, exception);
            }

            return fileRepository.save(fileItemDto);
        }
    }

    private boolean isFileNameUniqueConflict(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                if ("23505".equals(sqlException.getSQLState())) {
                    String message = sqlException.getMessage();
                    return message != null && message.contains("agdrv_files_folder_name_uq");
                }
            }
            current = current.getCause();
        }
        return false;
    }

    public String deleteByIdAndOwnerId(Long id, Long ownerId) {
        FileItemDto fileItemDto = findByIdAndOwnerId(id, ownerId);
        if (fileItemDto == null) {
            return "Файл не найден.";
        }

        String storageName = fileItemDto.getStorageName();
        shareLinkRepository.disableAllByResource("FILE", fileItemDto.getId());
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
        return relocateByIdAndOwnerId(id, ownerId, fileItemDto.getFolderId(), newName);
    }

    public String moveByIdAndOwnerId(Long id, Long ownerId, Long targetFolderId) {
        FileItemDto fileItemDto = findByIdAndOwnerId(id, ownerId);
        if (fileItemDto == null) {
            return "Файл не найден.";
        }
        return relocateByIdAndOwnerId(id, ownerId, targetFolderId, fileItemDto.getOriginalFilename());
    }

    public String relocateByIdAndOwnerId(Long id, Long ownerId, Long targetFolderId, String targetName) {
        FileItemDto fileItemDto = findByIdAndOwnerId(id, ownerId);
        if (fileItemDto == null) {
            return "Файл не найден.";
        }

        String normalizedName = filenamePolicyService.normalizeFilename(targetName);
        if (normalizedName == null) {
            return "Введите название файла.";
        }

        if (targetFolderId != null && !folderExistsForOwner(ownerId, targetFolderId)) {
            return "Целевая директория не найдена.";
        }

        boolean sameFolder = fileItemDto.getFolderId() == null
                ? targetFolderId == null
                : fileItemDto.getFolderId().equals(targetFolderId);
        if (sameFolder && normalizedName.equals(fileItemDto.getOriginalFilename())) {
            return null;
        }

        if (fileRepository.existsByOwnerIdAndFolderIdAndOriginalFilename(ownerId, targetFolderId, normalizedName, fileItemDto.getId())) {
            return "В целевой директории уже есть файл с таким названием.";
        }

        fileItemDto.setFolderId(targetFolderId);
        fileItemDto.setOriginalFilename(normalizedName);
        fileItemDto.setExtension(extractExtension(normalizedName));
        return fileRepository.save(fileItemDto) == null ? "Не удалось обновить файл." : null;
    }

    public List<FileItemDto> findExpiredActiveFiles() {
        return fileRepository.findExpiredActiveFiles();
    }

    public String updateExpirationByIdAndOwnerId(Long id, Long ownerId, String expiresAtInput, boolean expiresUnlimited) {
        FileItemDto fileItemDto = findByIdAndOwnerId(id, ownerId);
        if (fileItemDto == null) {
            return "Файл не найден.";
        }

        if (expiresUnlimited) {
            fileItemDto.setExpiresAt(null);
        } else {
            String expirationError = expirationPolicyService.validateExpirationInput(expiresAtInput);
            if (expirationError != null) {
                return expirationError;
            }
            fileItemDto.setExpiresAt(expirationPolicyService.normalizeExpirationInput(expiresAtInput));
        }

        return fileRepository.save(fileItemDto) == null ? "Не удалось обновить свойства файла." : null;
    }

    public java.util.Map<Long, String> buildExistingFileNamesByFolderId(Long ownerId) {
        java.util.Map<Long, java.util.LinkedHashSet<String>> grouped = new java.util.LinkedHashMap<>();
        for (FileItemDto file : findAllByOwnerId(ownerId)) {
            if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
                continue;
            }
            grouped.computeIfAbsent(file.getFolderId(), key -> new java.util.LinkedHashSet<>())
                    .add(file.getOriginalFilename().trim().toLowerCase(java.util.Locale.ROOT));
        }

        java.util.Map<Long, String> result = new java.util.LinkedHashMap<>();
        for (var entry : grouped.entrySet()) {
            result.put(entry.getKey(), String.join("\n", entry.getValue()));
        }
        return result;
    }

    public String buildExistingFileNamesForFolderId(Long ownerId, Long folderId) {
        return buildExistingFileNamesByFolderId(ownerId).get(folderId);
    }

    private boolean folderExistsForOwner(Long ownerId, Long folderId) {
        if (folderId == null) {
            return true;
        }

        return folderService.findByIdAndOwnerId(folderId, ownerId) != null;
    }

    private long resolveQuotaBytes(Long ownerId) {
        UserDto user = userService.findById(ownerId);
        return user == null || user.getStorageQuotaBytes() == null || user.getStorageQuotaBytes() <= 0
                ? DEFAULT_STORAGE_QUOTA_BYTES
                : user.getStorageQuotaBytes();
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

    private MessageDigest newSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private Comparator<FileItemDto> buildMediaComparator(String sortMode) {
        Comparator<FileItemDto> byNameAsc = Comparator
                .comparing((FileItemDto file) -> normalizedString(file.getOriginalFilename()))
                .thenComparing(FileItemDto::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<FileItemDto> byDateNewest = Comparator
                .comparing((FileItemDto file) -> parseDateTime(file.getCreatedAt()), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing((FileItemDto file) -> normalizedString(file.getOriginalFilename()))
                .thenComparing(FileItemDto::getId, Comparator.nullsLast(Comparator.reverseOrder()));
        Comparator<FileItemDto> byDateOldest = Comparator
                .comparing((FileItemDto file) -> parseDateTime(file.getCreatedAt()), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing((FileItemDto file) -> normalizedString(file.getOriginalFilename()))
                .thenComparing(FileItemDto::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<FileItemDto> bySizeDesc = Comparator
                .comparing((FileItemDto file) -> file.getSizeBytes() == null ? 0L : file.getSizeBytes(), Comparator.reverseOrder())
                .thenComparing((FileItemDto file) -> normalizedString(file.getOriginalFilename()))
                .thenComparing(FileItemDto::getId, Comparator.nullsLast(Comparator.reverseOrder()));
        Comparator<FileItemDto> bySizeAsc = Comparator
                .comparing((FileItemDto file) -> file.getSizeBytes() == null ? 0L : file.getSizeBytes())
                .thenComparing((FileItemDto file) -> normalizedString(file.getOriginalFilename()))
                .thenComparing(FileItemDto::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<FileItemDto> byTypeAsc = Comparator
                .comparing((FileItemDto file) -> normalizedString(file.getExtension()))
                .thenComparing((FileItemDto file) -> normalizedString(file.getOriginalFilename()))
                .thenComparing(FileItemDto::getId, Comparator.nullsLast(Comparator.naturalOrder()));

        String normalizedSortMode = sortMode == null ? "" : sortMode.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedSortMode) {
            case "name_desc" -> byNameAsc.reversed();
            case "date_oldest" -> byDateOldest;
            case "size_desc" -> bySizeDesc;
            case "size_asc" -> bySizeAsc;
            case "type_asc" -> byTypeAsc;
            case "date_newest" -> byDateNewest;
            default -> byNameAsc;
        };
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String normalizedString(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private LocalDateTime parseDateTime(String value) {
        return AppTime.parseDatabaseDateTime(value);
    }

}
