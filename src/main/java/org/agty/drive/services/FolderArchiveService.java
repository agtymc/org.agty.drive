package org.agty.drive.services;

import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FolderDto;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FolderArchiveService {

    private final FolderService folderService;
    private final FileService fileService;
    private final FileContentStorageService fileContentStorageService;

    public FolderArchiveService(FolderService folderService,
                                FileService fileService,
                                FileContentStorageService fileContentStorageService) {
        this.folderService = folderService;
        this.fileService = fileService;
        this.fileContentStorageService = fileContentStorageService;
    }

    public byte[] buildFolderArchive(FolderDto rootFolder) {
        if (rootFolder == null || rootFolder.getId() == null || rootFolder.getOwnerId() == null) {
            return null;
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            addFolder(zipOutputStream, rootFolder.getOwnerId(), rootFolder, sanitizeFolderName(rootFolder.getName()) + "/");
            zipOutputStream.finish();
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new RuntimeException("Failed to build folder archive", exception);
        }
    }

    private void addFolder(ZipOutputStream zipOutputStream,
                           Long ownerId,
                           FolderDto folder,
                           String pathPrefix) throws IOException {
        ZipEntry folderEntry = new ZipEntry(pathPrefix);
        zipOutputStream.putNextEntry(folderEntry);
        zipOutputStream.closeEntry();

        List<FileItemDto> files = fileService.findByOwnerIdAndFolderId(ownerId, folder.getId());
        for (FileItemDto file : files) {
            if (file == null || file.getStorageName() == null || file.getOriginalFilename() == null) {
                continue;
            }
            byte[] content = fileContentStorageService.read(file.getStorageName());
            if (content == null) {
                continue;
            }

            ZipEntry fileEntry = new ZipEntry(pathPrefix + sanitizeFileName(file.getOriginalFilename()));
            zipOutputStream.putNextEntry(fileEntry);
            zipOutputStream.write(content);
            zipOutputStream.closeEntry();
        }

        List<FolderDto> folders = folderService.findByOwnerIdAndParentId(ownerId, folder.getId());
        for (FolderDto childFolder : folders) {
            addFolder(zipOutputStream, ownerId, childFolder, pathPrefix + sanitizeFolderName(childFolder.getName()) + "/");
        }
    }

    private String sanitizeFolderName(String value) {
        if (value == null || value.isBlank()) {
            return "folder";
        }
        return value.replace("\\", "_").replace("/", "_").trim();
    }

    private String sanitizeFileName(String value) {
        if (value == null || value.isBlank()) {
            return "file";
        }
        return value.replace("\\", "_").replace("/", "_").trim();
    }
}
