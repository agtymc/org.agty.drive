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
import org.agty.drive.dto.WebDavFolderAccessCreateDto;
import org.agty.drive.dto.WebDavFolderAccessDto;
import org.agty.drive.repository.WebDavFolderAccessRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WebDavFolderAccessService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final WebDavFolderAccessRepository webDavFolderAccessRepository;
    private final FolderService folderService;
    private final PasswordEncoder passwordEncoder;

    public WebDavFolderAccessService(WebDavFolderAccessRepository webDavFolderAccessRepository,
                                     FolderService folderService,
                                     PasswordEncoder passwordEncoder) {
        this.webDavFolderAccessRepository = webDavFolderAccessRepository;
        this.folderService = folderService;
        this.passwordEncoder = passwordEncoder;
    }

    public WebDavFolderAccessDto findByOwnerAndFolder(Long ownerId, Long folderId) {
        return webDavFolderAccessRepository.findLatestByOwnerAndFolder(ownerId, folderId);
    }

    public Map<Long, WebDavFolderAccessDto> mapByFolderId(Long ownerId) {
        return webDavFolderAccessRepository.findLatestByOwner(ownerId).stream()
                .collect(Collectors.toMap(WebDavFolderAccessDto::getFolderId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    public String validateCreate(Long ownerId, WebDavFolderAccessCreateDto dto) {
        if (ownerId == null) {
            return "Пользователь не найден.";
        }
        if (dto == null || dto.getFolderId() == null) {
            return "Папка для WebDAV не выбрана.";
        }
        FolderDto folder = folderService.findByIdAndOwnerId(dto.getFolderId(), ownerId);
        if (folder == null) {
            return "Папка для WebDAV не найдена.";
        }

        String login = normalizeLogin(dto.getLoginName());
        if (login == null) {
            return "Укажите логин WebDAV не короче 3 символов.";
        }

        WebDavFolderAccessDto existing = findByOwnerAndFolder(ownerId, dto.getFolderId());
        String password = dto.getPassword() == null ? "" : dto.getPassword().trim();
        if ((existing == null || Boolean.TRUE.equals(dto.getRotateToken())) && password.isBlank()) {
            return "Укажите пароль WebDAV.";
        }
        if (!password.isBlank() && password.length() < 8) {
            return "Пароль WebDAV должен содержать минимум 8 символов.";
        }
        return null;
    }

    public SaveResult saveFolderAccess(Long ownerId, WebDavFolderAccessCreateDto dto) {
        String error = validateCreate(ownerId, dto);
        if (error != null) {
            return SaveResult.error(error);
        }

        WebDavFolderAccessDto existing = findByOwnerAndFolder(ownerId, dto.getFolderId());
        String previousToken = existing == null ? null : existing.getAccessToken();
        String previousPasswordHash = existing == null ? null : existing.getPasswordHash();
        if (existing != null) {
            webDavFolderAccessRepository.disableByOwnerAndFolder(ownerId, dto.getFolderId());
        }

        String plainPassword = dto.getPassword() == null ? "" : dto.getPassword().trim();
        String accessToken = previousToken != null && !Boolean.TRUE.equals(dto.getRotateToken())
                ? previousToken
                : UUID.randomUUID().toString().replace("-", "");
        String passwordHash = !plainPassword.isBlank()
                ? passwordEncoder.encode(plainPassword)
                : previousPasswordHash;

        WebDavFolderAccessDto record = new WebDavFolderAccessDto();
        record.setOwnerId(ownerId);
        record.setFolderId(dto.getFolderId());
        record.setLoginName(normalizeLogin(dto.getLoginName()));
        record.setAllowWrite(Boolean.TRUE.equals(dto.getAllowWrite()));
        record.setIsEnabled(!Boolean.FALSE.equals(dto.getEnabled()));
        record.setAccessToken(accessToken);
        record.setPasswordHash(passwordHash);

        WebDavFolderAccessDto saved = webDavFolderAccessRepository.save(record);
        return saved == null ? SaveResult.error("Не удалось сохранить настройку WebDAV.") : SaveResult.saved(saved, plainPassword);
    }

    public AuthenticatedAccess authenticate(String token, String login, String password) {
        WebDavFolderAccessDto access = webDavFolderAccessRepository.findByToken(token);
        if (access == null || !Boolean.TRUE.equals(access.getIsEnabled())) {
            return null;
        }
        if (login == null || password == null) {
            return null;
        }
        if (!access.getLoginName().equals(login.trim())) {
            return null;
        }
        if (!passwordEncoder.matches(password, access.getPasswordHash())) {
            return null;
        }

        FolderDto folder = folderService.findByIdAndOwnerId(access.getFolderId(), access.getOwnerId());
        if (folder == null) {
            return null;
        }
        return new AuthenticatedAccess(access, folder);
    }

    public String buildSuggestedPassword() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeLogin(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() < 3 ? null : normalized;
    }

    public record AuthenticatedAccess(WebDavFolderAccessDto access, FolderDto rootFolder) {
        public boolean allowWrite() {
            return Boolean.TRUE.equals(access.getAllowWrite());
        }
    }

    public record SaveResult(WebDavFolderAccessDto access, String plainPassword, String error) {
        public static SaveResult error(String error) {
            return new SaveResult(null, null, error);
        }

        public static SaveResult saved(WebDavFolderAccessDto access, String plainPassword) {
            return new SaveResult(access, plainPassword == null || plainPassword.isBlank() ? null : plainPassword, null);
        }

        public boolean success() {
            return access != null && error == null;
        }
    }
}
