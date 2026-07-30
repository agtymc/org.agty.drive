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

import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FolderDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ExpiredItemCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ExpiredItemCleanupService.class);

    private final FileService fileService;
    private final FolderService folderService;
    private final FolderDeleteService folderDeleteService;

    public ExpiredItemCleanupService(FileService fileService,
                                     FolderService folderService,
                                     FolderDeleteService folderDeleteService) {
        this.fileService = fileService;
        this.folderService = folderService;
        this.folderDeleteService = folderDeleteService;
    }

    @Scheduled(fixedDelay = 60000L, initialDelay = 60000L)
    public void cleanupExpiredItems() {
        for (FileItemDto file : fileService.findExpiredActiveFiles()) {
            String error = fileService.deleteByIdAndOwnerId(file.getId(), file.getOwnerId());
            if (error != null && !"Файл не найден.".equals(error)) {
                log.warn("Failed to delete expired file {}: {}", file.getId(), error);
            }
        }

        for (FolderDto folder : folderService.findExpiredActiveFolders()) {
            String error = folderDeleteService.deleteByIdAndOwnerId(folder.getId(), folder.getOwnerId());
            if (error != null && !"Папка не найдена.".equals(error)) {
                log.warn("Failed to delete expired folder {}: {}", folder.getId(), error);
            }
        }
    }
}
