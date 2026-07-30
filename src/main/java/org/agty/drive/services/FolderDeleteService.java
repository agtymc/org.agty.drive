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
import org.agty.drive.repository.CollaborativeAccessRepository;
import org.agty.drive.repository.ShareLinkRepository;
import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FolderDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FolderDeleteService {

    private final FolderService folderService;
    private final FileService fileService;
    private final ShareLinkRepository shareLinkRepository;
    private final CollaborativeAccessRepository collaborativeAccessRepository;

    public FolderDeleteService(FolderService folderService,
                               FileService fileService,
                               ShareLinkRepository shareLinkRepository,
                               CollaborativeAccessRepository collaborativeAccessRepository) {
        this.folderService = folderService;
        this.fileService = fileService;
        this.shareLinkRepository = shareLinkRepository;
        this.collaborativeAccessRepository = collaborativeAccessRepository;
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

        shareLinkRepository.disableAllByResource("FOLDER", folderDto.getId());
        collaborativeAccessRepository.disableByFolder(folderDto.getId());
        folderDto.setDeletedAt(AppTime.nowForDatabase());
        return folderService.save(folderDto) == null ? "Не удалось удалить папку." : null;
    }
}
