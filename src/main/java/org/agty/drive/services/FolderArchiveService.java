package org.agty.drive.services;

import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FolderDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FolderArchiveService {

    private final FolderService folderService;
    private final FileService fileService;
    private final FileContentStorageService fileContentStorageService;
    private final FilenamePolicyService filenamePolicyService;

    public FolderArchiveService(FolderService folderService,
                                FileService fileService,
                                FileContentStorageService fileContentStorageService,
                                FilenamePolicyService filenamePolicyService) {
        this.folderService = folderService;
        this.fileService = fileService;
        this.fileContentStorageService = fileContentStorageService;
        this.filenamePolicyService = filenamePolicyService;
    }

    public Path buildFolderArchiveTempFile(FolderDto rootFolder) {
        if (rootFolder == null || rootFolder.getId() == null || rootFolder.getOwnerId() == null) {
            return null;
        }

        try {
            Path archivePath = Files.createTempFile("agty-drive-folder-", ".zip");
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(archivePath))) {
                addFolder(zipOutputStream, rootFolder.getOwnerId(), rootFolder, filenamePolicyService.normalizeArchiveEntryName(rootFolder.getName(), "folder") + "/");
                zipOutputStream.finish();
            } catch (IOException exception) {
                Files.deleteIfExists(archivePath);
                throw exception;
            }
            return archivePath;
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
            try (InputStream contentStream = fileContentStorageService.openStream(file.getStorageName())) {
                if (contentStream == null) {
                    continue;
                }

                ZipEntry fileEntry = new ZipEntry(pathPrefix + filenamePolicyService.normalizeArchiveEntryName(file.getOriginalFilename(), "file"));
                zipOutputStream.putNextEntry(fileEntry);
                contentStream.transferTo(zipOutputStream);
                zipOutputStream.closeEntry();
            } catch (IOException exception) {
                continue;
            }
        }

        List<FolderDto> folders = folderService.findByOwnerIdAndParentId(ownerId, folder.getId());
        for (FolderDto childFolder : folders) {
            addFolder(zipOutputStream, ownerId, childFolder, pathPrefix + filenamePolicyService.normalizeArchiveEntryName(childFolder.getName(), "folder") + "/");
        }
    }
}
