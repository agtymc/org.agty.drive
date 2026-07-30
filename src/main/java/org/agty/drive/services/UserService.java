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
import org.agty.drive.config.ApplicationInfo;
import org.agty.drive.dto.AdminUserCreateDto;
import org.agty.drive.dto.OpenRegistrationDto;
import org.agty.drive.dto.ProfileSecuritySettingsDto;
import org.agty.drive.dto.UserDto;
import org.agty.drive.dto.UsersRoleDictionaryDto;
import org.agty.drive.dto.UsersStatusDictionaryDto;
import org.agty.drive.repository.UserRepository;
import org.agty.drive.security.totp.TotpSupport;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsersRoleDictionaryService usersRoleDictionaryService;
    private final UsersStatusDictionaryService usersStatusDictionaryService;
    private final ApplicationInfo applicationInfo;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       UsersRoleDictionaryService usersRoleDictionaryService,
                       UsersStatusDictionaryService usersStatusDictionaryService,
                       ApplicationInfo applicationInfo) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.usersRoleDictionaryService = usersRoleDictionaryService;
        this.usersStatusDictionaryService = usersStatusDictionaryService;
        this.applicationInfo = applicationInfo;
    }

    public UserDto findByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    public UserDto findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public UserDto findById(Long id) {
        return userRepository.findById(id);
    }

    public long countAll() {
        return userRepository.countAll();
    }

    public UserDto save(UserDto userDto) {
        return userRepository.save(userDto);
    }

    public List<UserDto> findAll() {
        return userRepository.findAll();
    }

    public String updateStorageQuota(Long userId, Long storageQuotaMb) {
        if (userId == null) {
            return "Пользователь не найден.";
        }
        if (storageQuotaMb == null || storageQuotaMb <= 0) {
            return "Укажите квоту больше 0 МБ.";
        }

        UserDto user = userRepository.findById(userId);
        if (user == null) {
            return "Пользователь не найден.";
        }

        user.setStorageQuotaBytes(storageQuotaMb * 1024L * 1024L);
        UserDto saved = userRepository.save(user);
        return saved == null ? "Не удалось сохранить квоту." : null;
    }

    public ProfileSecuritySettingsDto getProfileSecuritySettings(Long userId) {
        UserDto user = userRepository.findById(userId);
        if (user == null) {
            return null;
        }

        ProfileSecuritySettingsDto dto = new ProfileSecuritySettingsDto();
        dto.setEmail(user.getEmail());
        dto.setTwoFactorEmailEnabled(Boolean.TRUE.equals(user.getTwoFactorEmailEnabled()));
        dto.setTwoFactorTotpEnabled(Boolean.TRUE.equals(user.getTwoFactorTotpEnabled()));
        dto.setTwoFactorTotpSecret(user.getTwoFactorTotpSecret());
        if (user.getTwoFactorTotpSecret() != null && !user.getTwoFactorTotpSecret().isBlank()) {
            dto.setTwoFactorTotpUri(TotpSupport.buildOtpAuthUri(applicationInfo.getTitle(), user.getLogin(), user.getTwoFactorTotpSecret()));
        }
        return dto;
    }

    public String updateProfileSecuritySettings(Long userId, ProfileSecuritySettingsDto settingsDto) {
        if (userId == null) {
            return "Пользователь не найден.";
        }
        if (settingsDto == null) {
            return "Настройки не переданы.";
        }

        UserDto user = userRepository.findById(userId);
        if (user == null) {
            return "Пользователь не найден.";
        }

        String email = settingsDto.getEmail() == null ? null : settingsDto.getEmail().trim();
        boolean email2faEnabled = Boolean.TRUE.equals(settingsDto.getTwoFactorEmailEnabled());
        boolean totpEnabled = Boolean.TRUE.equals(settingsDto.getTwoFactorTotpEnabled());

        if (email2faEnabled && (email == null || email.isBlank())) {
            return "Для включения 2FA по E-mail укажите адрес электронной почты.";
        }

        user.setEmail(email == null || email.isBlank() ? null : email);
        user.setTwoFactorEmailEnabled(email2faEnabled);

        if (totpEnabled) {
            if (user.getTwoFactorTotpSecret() == null || user.getTwoFactorTotpSecret().isBlank()) {
                user.setTwoFactorTotpSecret(TotpSupport.generateSecret());
                user.setTwoFactorTotpCreatedAt(AppTime.nowForDatabase());
            }
            user.setTwoFactorTotpEnabled(true);
        } else {
            user.setTwoFactorTotpEnabled(false);
            user.setTwoFactorTotpSecret(null);
            user.setTwoFactorTotpCreatedAt(null);
        }

        UserDto saved = userRepository.save(user);
        return saved == null ? "Не удалось сохранить настройки безопасности." : null;
    }

    public String changePassword(Long userId, String currentPassword, String newPassword, String confirmPassword) {
        if (userId == null) {
            return "Пользователь не найден.";
        }
        if (currentPassword == null || currentPassword.isBlank()) {
            return "Укажите текущий пароль.";
        }
        if (newPassword == null || newPassword.isBlank()) {
            return "Укажите новый пароль.";
        }
        if (newPassword.length() < 8) {
            return "Новый пароль должен содержать минимум 8 символов.";
        }
        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            return "Подтверждение пароля не совпадает.";
        }

        UserDto user = userRepository.findById(userId);
        if (user == null) {
            return "Пользователь не найден.";
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return "Текущий пароль введен неверно.";
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            return "Новый пароль должен отличаться от текущего.";
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        UserDto saved = userRepository.save(user);
        return saved == null ? "Не удалось сохранить новый пароль." : null;
    }

    public UserDto createByAdmin(Long actorUserId, AdminUserCreateDto createDto) {
        if (createDto == null) {
            return null;
        }
        String login = normalize(createDto.getLogin());
        String email = normalize(createDto.getEmail());
        String displayName = normalize(createDto.getDisplayName());
        String roleCode = normalize(createDto.getRoleCode());
        String statusCode = normalize(createDto.getStatusCode());

        UserDto dto = new UserDto();
        dto.setLogin(login);
        dto.setEmail(email);
        dto.setDisplayName(displayName);
        dto.setRoleCode(roleCode);
        dto.setStatusCode(statusCode);
        dto.setPasswordHash(passwordEncoder.encode(createDto.getPassword()));
        dto.setStorageQuotaBytes((createDto.getStorageQuotaMb() == null ? 100L : createDto.getStorageQuotaMb()) * 1024L * 1024L);
        dto.setCreatedBy(actorUserId);
        dto.setTwoFactorEmailEnabled(false);
        dto.setTwoFactorTotpEnabled(false);
        return userRepository.save(dto);
    }

    public String validateAdminCreate(AdminUserCreateDto createDto) {
        if (createDto == null) {
            return "Данные пользователя не переданы.";
        }
        String password = createDto.getPassword();
        if (password == null || password.length() < 8) {
            return "Пароль должен содержать минимум 8 символов.";
        }
        return validateAdminCreateForInvite(createDto);
    }

    public String validateOpenRegistration(OpenRegistrationDto registrationDto) {
        if (registrationDto == null) {
            return "Данные регистрации не переданы.";
        }
        if (registrationDto.getPassword() == null || registrationDto.getPassword().length() < 8) {
            return "Пароль должен содержать минимум 8 символов.";
        }
        if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
            return "Подтверждение пароля не совпадает.";
        }

        AdminUserCreateDto createDto = new AdminUserCreateDto();
        createDto.setLogin(registrationDto.getLogin());
        createDto.setEmail(registrationDto.getEmail());
        createDto.setDisplayName(registrationDto.getDisplayName());
        createDto.setPassword(registrationDto.getPassword());
        createDto.setRoleCode("ROLE_USER");
        createDto.setStatusCode("ACTIVE");
        createDto.setStorageQuotaMb(100L);
        return validateAdminCreate(createDto);
    }

    public UserDto registerOpenUser(OpenRegistrationDto registrationDto) {
        if (registrationDto == null) {
            return null;
        }
        AdminUserCreateDto createDto = new AdminUserCreateDto();
        createDto.setLogin(registrationDto.getLogin());
        createDto.setEmail(registrationDto.getEmail());
        createDto.setDisplayName(registrationDto.getDisplayName());
        createDto.setPassword(registrationDto.getPassword());
        createDto.setRoleCode("ROLE_USER");
        createDto.setStatusCode("ACTIVE");
        createDto.setStorageQuotaMb(100L);
        return createByAdmin(null, createDto);
    }

    public String validateAdminCreateForInvite(AdminUserCreateDto createDto) {
        if (createDto == null) {
            return "Данные пользователя не переданы.";
        }
        String login = normalize(createDto.getLogin());
        if (login == null) {
            return "Логин обязателен.";
        }
        if (userRepository.existsByLogin(login, null)) {
            return "Пользователь с таким логином уже существует.";
        }
        String email = normalize(createDto.getEmail());
        if (email != null && userRepository.existsByEmail(email, null)) {
            return "Пользователь с таким E-mail уже существует.";
        }
        if (createDto.getStorageQuotaMb() == null || createDto.getStorageQuotaMb() <= 0) {
            return "Квота должна быть больше 0 МБ.";
        }
        if (!dictionaryContainsRole(createDto.getRoleCode())) {
            return "Выбрана неизвестная роль.";
        }
        if (!dictionaryContainsStatus(createDto.getStatusCode())) {
            return "Выбран неизвестный статус.";
        }
        return null;
    }

    public String updateAccess(Long userId, String roleCode, String statusCode) {
        return updateAccess(userId, roleCode, statusCode, null);
    }

    public String updateAccess(Long userId, String roleCode, String statusCode, Long storageQuotaMb) {
        if (userId == null) {
            return "Пользователь не найден.";
        }
        UserDto user = userRepository.findById(userId);
        if (user == null) {
            return "Пользователь не найден.";
        }
        String normalizedRole = normalize(roleCode);
        String normalizedStatus = normalize(statusCode);
        if (!dictionaryContainsRole(normalizedRole)) {
            return "Выбрана неизвестная роль.";
        }
        if (!dictionaryContainsStatus(normalizedStatus)) {
            return "Выбран неизвестный статус.";
        }
        if (storageQuotaMb != null && storageQuotaMb <= 0) {
            return "Укажите квоту больше 0 МБ.";
        }
        user.setRoleCode(normalizedRole);
        user.setStatusCode(normalizedStatus);
        if (storageQuotaMb != null) {
            user.setStorageQuotaBytes(storageQuotaMb * 1024L * 1024L);
        }
        return userRepository.save(user) == null ? "Не удалось обновить права доступа." : null;
    }

    public String blockUser(Long userId) {
        return updateAccessStatus(userId, "BLOCKED");
    }

    public String activateUser(Long userId) {
        return updateAccessStatus(userId, "ACTIVE");
    }

    private String updateAccessStatus(Long userId, String statusCode) {
        UserDto user = userRepository.findById(userId);
        if (user == null) {
            return "Пользователь не найден.";
        }
        user.setStatusCode(statusCode);
        return userRepository.save(user) == null ? "Не удалось обновить статус пользователя." : null;
    }

    private boolean dictionaryContainsRole(String code) {
        if (code == null) {
            return false;
        }
        for (UsersRoleDictionaryDto dto : usersRoleDictionaryService.findAll()) {
            if (code.equalsIgnoreCase(dto.getCode())) {
                return true;
            }
        }
        return false;
    }

    private boolean dictionaryContainsStatus(String code) {
        if (code == null) {
            return false;
        }
        for (UsersStatusDictionaryDto dto : usersStatusDictionaryService.findAll()) {
            if (code.equalsIgnoreCase(dto.getCode())) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
