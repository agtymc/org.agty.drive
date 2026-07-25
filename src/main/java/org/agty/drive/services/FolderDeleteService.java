package org.agty.drive.services;

import org.agty.drive.config.AppTime;
import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FolderDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FolderDeleteService {

    private final FolderService folderService;
    private final FileService fileService;

    public FolderDeleteService(FolderService folderService, FileService fileService) {
        this.folderService = folderService;
        this.fileService = fileService;
    }

    public String deleteByIdAndOwnerId(Long id, Long ownerId) {
        FolderDto folderDto = folderService.findByIdAndOwnerId(id, ownerId);
        if (folderDto == null) {
            return "Папка не найдена.";
        }

        return deleteRecursive(folderDto, ownerId);
    }

    private String deleteRecursive(FolderDto folderDto, Long ownerId) {
        List<FileItemDto> files = fileService.findByOwnerIdAndFolderId(ownerId, folderDto.getId());
        for (FileItemDto file : files) {
            String fileError = fileService.deleteByIdAndOwnerId(file.getId(), ownerId);
            if (fileError != null) {
                return fileError;
            }
        }

        List<FolderDto> childFolders = folderService.findByOwnerIdAndParentId(ownerId, folderDto.getId());
        for (FolderDto childFolder : childFolders) {
            String folderError = deleteRecursive(childFolder, ownerId);
            if (folderError != null) {
                return folderError;
            }
        }

        folderDto.setDeletedAt(AppTime.nowForDatabase());
        return folderService.save(folderDto) == null ? "Не удалось удалить папку." : null;
    }
}
