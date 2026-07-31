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

import org.agty.drive.dto.FolderDto;
import org.agty.drive.dto.FolderMoveOptionDto;
import org.agty.drive.repository.FolderRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FolderService {

    private final FolderRepository folderRepository;
    private final ExpirationPolicyService expirationPolicyService;

    public FolderService(FolderRepository folderRepository,
                         ExpirationPolicyService expirationPolicyService) {
        this.folderRepository = folderRepository;
        this.expirationPolicyService = expirationPolicyService;
    }

    public List<FolderDto> findRootFoldersByOwnerId(Long ownerId) {
        return folderRepository.findRootFoldersByOwnerId(ownerId);
    }

    public List<FolderDto> findAllByOwnerId(Long ownerId) {
        return folderRepository.findAllByOwnerId(ownerId);
    }

    public List<FolderDto> findByOwnerIdAndParentId(Long ownerId, Long parentId) {
        return folderRepository.findByOwnerIdAndParentId(ownerId, parentId);
    }

    public List<FolderDto> searchByOwnerId(Long ownerId,
                                           String query,
                                           Long currentFolderId,
                                           String currentFolderPath,
                                           String scope) {
        return folderRepository.searchByOwnerId(ownerId, query, currentFolderId, currentFolderPath, scope);
    }

    public long countSearchByOwnerId(Long ownerId,
                                     String query,
                                     Long currentFolderId,
                                     String currentFolderPath,
                                     String scope) {
        return folderRepository.countSearchByOwnerId(ownerId, query, currentFolderId, currentFolderPath, scope);
    }

    public List<FolderDto> searchByOwnerId(Long ownerId,
                                           String query,
                                           Long currentFolderId,
                                           String currentFolderPath,
                                           String scope,
                                           String sortMode,
                                           int offset,
                                           int limit) {
        return folderRepository.searchByOwnerId(ownerId, query, currentFolderId, currentFolderPath, scope, sortMode, offset, limit);
    }

    public long countByOwnerId(Long ownerId) {
        return folderRepository.countByOwnerId(ownerId);
    }

    public long countAll() {
        return folderRepository.countAll();
    }

    public boolean existsByOwnerIdAndParentIdAndName(Long ownerId, Long parentId, String name) {
        return folderRepository.existsByOwnerIdAndParentIdAndName(ownerId, parentId, name);
    }

    public boolean existsByOwnerIdAndParentIdAndName(Long ownerId, Long parentId, String name, Long excludeId) {
        return folderRepository.existsByOwnerIdAndParentIdAndName(ownerId, parentId, name, excludeId);
    }

    public FolderDto findByIdAndOwnerId(Long id, Long ownerId) {
        return folderRepository.findByIdAndOwnerId(id, ownerId);
    }

    public FolderDto findById(Long id) {
        return folderRepository.findById(id);
    }

    public FolderDto save(FolderDto folderDto) {
        return folderRepository.save(folderDto);
    }

    public List<FolderDto> findExpiredActiveFolders() {
        return folderRepository.findExpiredActiveFolders();
    }

    public String renameByIdAndOwnerId(Long id, Long ownerId, String newName) {
        FolderDto folderDto = findByIdAndOwnerId(id, ownerId);
        if (folderDto == null) {
            return "Папка не найдена.";
        }
        return relocateByIdAndOwnerId(id, ownerId, folderDto.getParentId(), newName);
    }

    public String moveByIdAndOwnerId(Long id, Long ownerId, Long targetFolderId) {
        FolderDto folderDto = findByIdAndOwnerId(id, ownerId);
        if (folderDto == null) {
            return "Папка не найдена.";
        }
        return relocateByIdAndOwnerId(id, ownerId, targetFolderId, folderDto.getName());
    }

    public String relocateByIdAndOwnerId(Long id, Long ownerId, Long targetFolderId, String targetName) {
        FolderDto folderDto = findByIdAndOwnerId(id, ownerId);
        if (folderDto == null) {
            return "Папка не найдена.";
        }

        String normalizedName = normalizeName(targetName);
        if (normalizedName == null) {
            return "Введите название папки.";
        }

        if (targetFolderId != null) {
            FolderDto targetFolder = findByIdAndOwnerId(targetFolderId, ownerId);
            if (targetFolder == null) {
                return "Целевая директория не найдена.";
            }
            if (folderDto.getId().equals(targetFolder.getId())) {
                return "Нельзя переместить папку в саму себя.";
            }
            String folderPath = folderDto.getPathKey() == null ? "" : folderDto.getPathKey();
            String targetPath = targetFolder.getPathKey() == null ? "" : targetFolder.getPathKey();
            if (!folderPath.isBlank() && targetPath.equals(folderPath)) {
                return "Нельзя переместить папку в саму себя.";
            }
            if (!folderPath.isBlank() && targetPath.startsWith(folderPath + "/")) {
                return "Нельзя переместить папку в дочернюю директорию.";
            }
        }

        boolean sameParent = folderDto.getParentId() == null
                ? targetFolderId == null
                : folderDto.getParentId().equals(targetFolderId);
        if (sameParent && normalizedName.equals(folderDto.getName())) {
            return null;
        }

        if (existsByOwnerIdAndParentIdAndName(ownerId, targetFolderId, normalizedName, folderDto.getId())) {
            return "В целевой директории уже есть папка с таким названием.";
        }

        folderDto.setParentId(targetFolderId);
        folderDto.setName(normalizedName);
        folderDto.setPathKey(buildPathKey(ownerId, targetFolderId, normalizedName));
        FolderDto saved = save(folderDto);
        if (saved == null) {
            return "Не удалось обновить папку.";
        }

        updateChildPathKeys(saved);
        return null;
    }

    public List<FolderMoveOptionDto> buildMoveOptions(Long ownerId) {
        List<FolderMoveOptionDto> result = new ArrayList<>();
        result.add(new FolderMoveOptionDto(null, "Корень диска", ""));
        appendMoveOptions(result, ownerId, null, "");
        return result;
    }

    public String buildPathKeyForCreate(Long ownerId, Long parentId, String folderName) {
        return buildPathKey(ownerId, parentId, folderName);
    }

    public String validateExpirationInput(String expiresAtInput) {
        return expirationPolicyService.validateExpirationInput(expiresAtInput);
    }

    public void normalizeExpiration(FolderDto folderDto) {
        if (folderDto == null) {
            return;
        }
        folderDto.setExpiresAt(expirationPolicyService.normalizeExpirationInput(folderDto.getExpiresAt()));
    }

    public String updateExpirationByIdAndOwnerId(Long id, Long ownerId, String expiresAtInput, boolean expiresUnlimited) {
        FolderDto folderDto = findByIdAndOwnerId(id, ownerId);
        if (folderDto == null) {
            return "Папка не найдена.";
        }

        if (expiresUnlimited) {
            folderDto.setExpiresAt(null);
        } else {
            String expirationError = expirationPolicyService.validateExpirationInput(expiresAtInput);
            if (expirationError != null) {
                return expirationError;
            }
            folderDto.setExpiresAt(expirationPolicyService.normalizeExpirationInput(expiresAtInput));
        }

        return save(folderDto) == null ? "Не удалось обновить свойства папки." : null;
    }

    private void appendMoveOptions(List<FolderMoveOptionDto> result, Long ownerId, Long parentId, String prefix) {
        List<FolderDto> folders = findByOwnerIdAndParentId(ownerId, parentId);
        for (FolderDto folder : folders) {
            String currentPath = prefix.isBlank() ? folder.getName() : prefix + " / " + folder.getName();
            result.add(new FolderMoveOptionDto(folder.getId(), currentPath, folder.getPathKey()));
            appendMoveOptions(result, ownerId, folder.getId(), currentPath);
        }
    }

    private void updateChildPathKeys(FolderDto parentFolder) {
        if (parentFolder == null || parentFolder.getId() == null || parentFolder.getOwnerId() == null) {
            return;
        }

        List<FolderDto> childFolders = findByOwnerIdAndParentId(parentFolder.getOwnerId(), parentFolder.getId());
        for (FolderDto childFolder : childFolders) {
            childFolder.setPathKey(buildPathKey(parentFolder.getOwnerId(), parentFolder.getId(), childFolder.getName()));
            FolderDto savedChild = save(childFolder);
            if (savedChild != null) {
                updateChildPathKeys(savedChild);
            }
        }
    }

    private String buildPathKey(Long ownerId, Long parentId, String folderName) {
        String slug = slugify(folderName);
        if (parentId == null) {
            return "/" + slug;
        }

        FolderDto parentFolder = findByIdAndOwnerId(parentId, ownerId);
        if (parentFolder == null || parentFolder.getPathKey() == null || parentFolder.getPathKey().isBlank()) {
            return "/" + slug;
        }
        return parentFolder.getPathKey() + "/" + slug;
    }

    private String normalizeName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String slugify(String value) {
        String normalized = normalizeName(value);
        if (normalized == null) {
            return "folder";
        }
        return normalized.toLowerCase().replace(' ', '-');
    }
}
