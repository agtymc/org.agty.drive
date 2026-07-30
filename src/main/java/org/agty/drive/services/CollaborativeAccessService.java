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

import jakarta.servlet.http.HttpSession;
import org.agty.drive.dto.CollaborativeAccessCreateDto;
import org.agty.drive.dto.CollaborativeAccessDto;
import org.agty.drive.dto.CollaborativeFolderShareDto;
import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FolderDto;
import org.agty.drive.dto.UserDto;
import org.agty.drive.repository.CollaborativeAccessRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CollaborativeAccessService {

    private final CollaborativeAccessRepository collaborativeAccessRepository;
    private final FolderService folderService;
    private final FileService fileService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public CollaborativeAccessService(CollaborativeAccessRepository collaborativeAccessRepository,
                                      FolderService folderService,
                                      FileService fileService,
                                      UserService userService,
                                      PasswordEncoder passwordEncoder) {
        this.collaborativeAccessRepository = collaborativeAccessRepository;
        this.folderService = folderService;
        this.fileService = fileService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    public List<CollaborativeFolderShareDto> findProvidedFolders(Long ownerId) {
        return groupByFolder(collaborativeAccessRepository.findActiveByOwner(ownerId));
    }

    public List<CollaborativeFolderShareDto> findReceivedFolders(Long userId) {
        List<CollaborativeFolderShareDto> result = new ArrayList<>();
        for (CollaborativeAccessDto access : collaborativeAccessRepository.findActiveByTargetUser(userId)) {
            CollaborativeFolderShareDto dto = new CollaborativeFolderShareDto();
            dto.setAccessId(access.getId());
            dto.setFolderId(access.getFolderId());
            dto.setFolderName(access.getFolderName());
            dto.setFolderPathKey(access.getFolderPathKey());
            dto.setOwnerId(access.getOwnerId());
            dto.setOwnerLogin(access.getOwnerLogin());
            dto.setOwnerDisplayName(access.getOwnerDisplayName());
            dto.setAllowWrite(access.getAllowWrite());
            dto.setAllowDelete(access.getAllowDelete());
            dto.setPasswordProtected(access.isPasswordProtected());
            dto.setRecipientCount(1);
            result.add(dto);
        }
        return result;
    }

    public List<CollaborativeAccessDto> findByOwnerAndFolder(Long ownerId, Long folderId) {
        return collaborativeAccessRepository.findActiveByOwnerAndFolder(ownerId, folderId);
    }

    public Map<Long, CollaborativeFolderShareDto> mapProvidedByFolderId(Long ownerId) {
        return findProvidedFolders(ownerId).stream()
                .collect(Collectors.toMap(CollaborativeFolderShareDto::getFolderId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    public String validateCreate(Long ownerId, CollaborativeAccessCreateDto dto) {
        if (ownerId == null) {
            return "Пользователь не найден.";
        }
        if (dto == null || dto.getFolderId() == null) {
            return "Не выбрана папка для совместного доступа.";
        }
        FolderDto folder = folderService.findByIdAndOwnerId(dto.getFolderId(), ownerId);
        if (folder == null) {
            return "Папка для совместного доступа не найдена.";
        }
        List<String> logins = parseLogins(dto.getLogins());
        if (logins.isEmpty()) {
            return null;
        }
        for (String login : logins) {
            UserDto user = userService.findByLogin(login);
            if (user == null) {
                return "Пользователь с логином " + login + " не найден.";
            }
            if (ownerId.equals(user.getId())) {
                return "Нельзя открыть доступ к папке самому себе.";
            }
        }
        return null;
    }

    public String saveFolderAccess(Long ownerId, CollaborativeAccessCreateDto dto) {
        String validationError = validateCreate(ownerId, dto);
        if (validationError != null) {
            return validationError;
        }

        List<CollaborativeAccessDto> existing = collaborativeAccessRepository.findActiveByOwnerAndFolder(ownerId, dto.getFolderId());
        String preservedPasswordHash = existing.stream()
                .map(CollaborativeAccessDto::getPasswordHash)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
        collaborativeAccessRepository.disableByOwnerAndFolder(ownerId, dto.getFolderId());

        List<String> logins = parseLogins(dto.getLogins());
        if (logins.isEmpty()) {
            return null;
        }

        String passwordHash = dto.getPassword() == null || dto.getPassword().isBlank()
                ? preservedPasswordHash
                : passwordEncoder.encode(dto.getPassword().trim());

        for (String login : logins) {
            UserDto user = userService.findByLogin(login);
            if (user == null) {
                continue;
            }
            CollaborativeAccessDto access = new CollaborativeAccessDto();
            access.setOwnerId(ownerId);
            access.setFolderId(dto.getFolderId());
            access.setTargetUserId(user.getId());
            access.setPasswordHash(passwordHash);
            access.setAllowWrite(Boolean.TRUE.equals(dto.getAllowWrite()));
            access.setAllowDelete(Boolean.TRUE.equals(dto.getAllowDelete()));
            access.setIsEnabled(true);
            if (collaborativeAccessRepository.save(access) == null) {
                return "Не удалось сохранить совместный доступ.";
            }
        }
        return null;
    }

    public CollaborativeAccessDto resolveReceivedAccess(Long userId, Long accessId) {
        CollaborativeAccessDto access = collaborativeAccessRepository.findById(accessId);
        if (access == null || !Boolean.TRUE.equals(access.getIsEnabled())) {
            return null;
        }
        return userId != null && userId.equals(access.getTargetUserId()) ? access : null;
    }

    public FolderDto resolveAccessibleFolder(CollaborativeAccessDto access, Long folderId) {
        if (access == null || access.getFolderId() == null) {
            return null;
        }
        FolderDto rootFolder = folderService.findById(access.getFolderId());
        if (rootFolder == null) {
            return null;
        }
        if (folderId == null) {
            return rootFolder;
        }
        FolderDto currentFolder = folderService.findById(folderId);
        if (currentFolder == null || !isFolderInsideRoot(currentFolder, rootFolder)) {
            return null;
        }
        return currentFolder;
    }

    public boolean canReadFile(CollaborativeAccessDto access, Long fileId) {
        FileItemDto file = fileService.findById(fileId);
        if (file == null) {
            return false;
        }
        return canReadFolder(access, file.getFolderId());
    }

    public boolean canReadFolder(CollaborativeAccessDto access, Long folderId) {
        if (access == null || folderId == null) {
            return false;
        }
        FolderDto rootFolder = folderService.findById(access.getFolderId());
        FolderDto folder = folderService.findById(folderId);
        return rootFolder != null && folder != null && isFolderInsideRoot(folder, rootFolder);
    }

    public boolean canWrite(CollaborativeAccessDto access) {
        return access != null && Boolean.TRUE.equals(access.getAllowWrite());
    }

    public boolean canDelete(CollaborativeAccessDto access) {
        return access != null && Boolean.TRUE.equals(access.getAllowDelete());
    }

    public boolean isUnlocked(HttpSession session, CollaborativeAccessDto access) {
        if (access == null) {
            return false;
        }
        if (!access.isPasswordProtected()) {
            return true;
        }
        return session != null && Boolean.TRUE.equals(session.getAttribute(accessKey(access.getId())));
    }

    public boolean unlock(HttpSession session, CollaborativeAccessDto access, String password) {
        if (session == null || access == null) {
            return false;
        }
        if (!access.isPasswordProtected()) {
            return true;
        }
        if (password == null || !passwordEncoder.matches(password, access.getPasswordHash())) {
            return false;
        }
        session.setAttribute(accessKey(access.getId()), Boolean.TRUE);
        return true;
    }

    private boolean isFolderInsideRoot(FolderDto folder, FolderDto rootFolder) {
        if (folder == null || rootFolder == null || folder.getPathKey() == null || rootFolder.getPathKey() == null) {
            return false;
        }
        return folder.getId().equals(rootFolder.getId())
                || folder.getPathKey().equals(rootFolder.getPathKey())
                || folder.getPathKey().startsWith(rootFolder.getPathKey() + "/");
    }

    private List<CollaborativeFolderShareDto> groupByFolder(List<CollaborativeAccessDto> items) {
        Map<Long, List<CollaborativeAccessDto>> grouped = items.stream()
                .collect(Collectors.groupingBy(CollaborativeAccessDto::getFolderId, LinkedHashMap::new, Collectors.toList()));
        List<CollaborativeFolderShareDto> result = new ArrayList<>();
        for (List<CollaborativeAccessDto> group : grouped.values()) {
            CollaborativeAccessDto first = group.getFirst();
            CollaborativeFolderShareDto dto = new CollaborativeFolderShareDto();
            dto.setFolderId(first.getFolderId());
            dto.setFolderName(first.getFolderName());
            dto.setFolderPathKey(first.getFolderPathKey());
            dto.setOwnerId(first.getOwnerId());
            dto.setOwnerLogin(first.getOwnerLogin());
            dto.setOwnerDisplayName(first.getOwnerDisplayName());
            dto.setAllowWrite(first.getAllowWrite());
            dto.setAllowDelete(first.getAllowDelete());
            dto.setPasswordProtected(group.stream().anyMatch(CollaborativeAccessDto::isPasswordProtected));
            dto.setRecipientCount(group.size());
            dto.setRecipientLogins(group.stream().map(CollaborativeAccessDto::getTargetUserLogin).collect(Collectors.joining(", ")));
            result.add(dto);
        }
        return result;
    }

    private List<String> parseLogins(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> logins = new LinkedHashSet<>();
        for (String item : value.split("[,\\n\\r;]+")) {
            String normalized = item == null ? "" : item.trim();
            if (!normalized.isBlank()) {
                logins.add(normalized);
            }
        }
        return new ArrayList<>(logins);
    }

    private String accessKey(Long accessId) {
        return "collaborative_access_" + accessId;
    }
}
