package org.agty.drive.services;

import org.agty.drive.config.AppTime;
import org.agty.drive.dto.AdminUserCreateDto;
import org.agty.drive.dto.InviteAcceptDto;
import org.agty.drive.dto.UserDto;
import org.agty.drive.dto.UserInviteCreateDto;
import org.agty.drive.dto.UserInviteDto;
import org.agty.drive.repository.UserInviteRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserInviteService {

    private final UserInviteRepository userInviteRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserInviteService(UserInviteRepository userInviteRepository,
                             UserService userService,
                             PasswordEncoder passwordEncoder) {
        this.userInviteRepository = userInviteRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserInviteDto> findAll() {
        return userInviteRepository.findAll();
    }

    public UserInviteDto findByToken(String token) {
        return userInviteRepository.findByToken(token);
    }

    public boolean isAvailable(UserInviteDto invite) {
        if (invite == null || !Boolean.TRUE.equals(invite.getIsEnabled()) || invite.getUsedAt() != null) {
            return false;
        }
        var expiresAt = AppTime.parseDatabaseDateTime(invite.getExpiresAt());
        return expiresAt == null || expiresAt.isAfter(AppTime.now());
    }

    public String validateCreate(UserInviteCreateDto dto) {
        if (dto == null) {
            return "Данные инвайта не переданы.";
        }
        AdminUserCreateDto userDto = new AdminUserCreateDto();
        userDto.setLogin(dto.getLogin());
        userDto.setEmail(dto.getEmail());
        userDto.setDisplayName(dto.getDisplayName());
        userDto.setPassword("temporary-password");
        userDto.setRoleCode(dto.getRoleCode());
        userDto.setStatusCode(dto.getStatusCode());
        userDto.setStorageQuotaMb(dto.getStorageQuotaMb());

        String userError = userService.validateAdminCreateForInvite(userDto);
        if (userError != null) {
            return userError;
        }
        if (dto.getExpiresInHours() != null && dto.getExpiresInHours() <= 0) {
            return "Срок инвайта должен быть больше 0 часов.";
        }
        return null;
    }

    public UserInviteDto create(Long actorUserId, UserInviteCreateDto dto) {
        if (actorUserId == null || dto == null) {
            return null;
        }
        UserInviteDto invite = new UserInviteDto();
        invite.setCreatedBy(actorUserId);
        invite.setToken(UUID.randomUUID().toString().replace("-", ""));
        invite.setLogin(dto.getLogin() == null ? null : dto.getLogin().trim());
        invite.setEmail(normalize(dto.getEmail()));
        invite.setDisplayName(normalize(dto.getDisplayName()));
        invite.setRoleCode(dto.getRoleCode() == null ? "ROLE_USER" : dto.getRoleCode().trim());
        invite.setStatusCode(dto.getStatusCode() == null ? "ACTIVE" : dto.getStatusCode().trim());
        invite.setStorageQuotaBytes((dto.getStorageQuotaMb() == null ? 100L : dto.getStorageQuotaMb()) * 1024L * 1024L);
        invite.setIsEnabled(true);
        if (dto.getExpiresInHours() != null && dto.getExpiresInHours() > 0) {
            invite.setExpiresAt(AppTime.formatForDatabase(AppTime.now().plusHours(dto.getExpiresInHours())));
        }
        return userInviteRepository.save(invite);
    }

    public String disable(Long inviteId) {
        UserInviteDto invite = userInviteRepository.findById(inviteId);
        if (invite == null) {
            return "Инвайт не найден.";
        }
        invite.setIsEnabled(false);
        return userInviteRepository.save(invite) == null ? "Не удалось отключить инвайт." : null;
    }

    public String validateAccept(UserInviteDto invite, InviteAcceptDto dto) {
        if (!isAvailable(invite)) {
            return "Инвайт недоступен или срок его действия истек.";
        }
        if (dto == null) {
            return "Данные регистрации не переданы.";
        }
        if (dto.getPassword() == null || dto.getPassword().length() < 8) {
            return "Пароль должен содержать минимум 8 символов.";
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            return "Подтверждение пароля не совпадает.";
        }
        if (userService.findByLogin(invite.getLogin()) != null) {
            return "Пользователь с таким логином уже существует.";
        }
        return null;
    }

    public UserDto accept(UserInviteDto invite, InviteAcceptDto dto) {
        if (invite == null || dto == null) {
            return null;
        }
        AdminUserCreateDto createDto = new AdminUserCreateDto();
        createDto.setLogin(invite.getLogin());
        createDto.setEmail(invite.getEmail());
        createDto.setDisplayName(invite.getDisplayName());
        createDto.setPassword(dto.getPassword());
        createDto.setRoleCode(invite.getRoleCode());
        createDto.setStatusCode(invite.getStatusCode());
        createDto.setStorageQuotaMb(invite.getStorageQuotaMb());
        UserDto created = userService.createByAdmin(invite.getCreatedBy(), createDto);
        if (created == null) {
            return null;
        }

        invite.setIsEnabled(false);
        invite.setUsedAt(AppTime.nowForDatabase());
        invite.setInvitedUserId(created.getId());
        userInviteRepository.save(invite);
        return created;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
