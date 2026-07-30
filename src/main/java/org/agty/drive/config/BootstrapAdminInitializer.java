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

package org.agty.drive.config;

import org.agty.drive.dto.UserDto;
import org.agty.drive.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapAdminInitializer {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    @Bean
    public ApplicationRunner bootstrapAdminRunner(UserService userService, PasswordEncoder passwordEncoder) {
        return args -> {
            String login = LocalConfig.getString("bootstrap.admin.login", "admin").trim();
            String password = LocalConfig.getString("bootstrap.admin.password", "admin");
            String displayName = LocalConfig.getString("bootstrap.admin.display_name", "Administrator");

            UserDto configuredUser = userService.findByLogin(login);
            if (shouldRefreshSeededAdmin(userService.countAll(), configuredUser, login, displayName)) {
                configuredUser.setPasswordHash(passwordEncoder.encode(password));
                configuredUser.setDisplayName(displayName);

                UserDto updated = userService.save(configuredUser);
                if (updated != null && updated.getId() != null) {
                    log.info("Refreshed seeded bootstrap admin login={} from config", login);
                } else {
                    log.error("Failed to refresh seeded bootstrap admin login={} from config", login);
                }
                return;
            }

            if (configuredUser != null) {
                return;
            }

            UserDto defaultAdmin = userService.findByLogin("admin");
            if (shouldReplaceSeededAdmin(userService.countAll(), defaultAdmin, login)) {
                defaultAdmin.setLogin(login);
                defaultAdmin.setPasswordHash(passwordEncoder.encode(password));
                defaultAdmin.setDisplayName(displayName);

                UserDto updated = userService.save(defaultAdmin);
                if (updated != null && updated.getId() != null) {
                    log.info("Updated seeded bootstrap admin login={} to configured login={}", "admin", login);
                } else {
                    log.error("Failed to update seeded bootstrap admin to configured login={}", login);
                }
                return;
            }

            UserDto userDto = new UserDto();
            userDto.setLogin(login);
            userDto.setPasswordHash(passwordEncoder.encode(password));
            userDto.setRoleCode("ROLE_ADMIN");
            userDto.setStatusCode("ACTIVE");
            userDto.setDisplayName(displayName);
            userDto.setStorageQuotaBytes(100L * 1024L * 1024L);

            UserDto saved = userService.save(userDto);
            if (saved != null && saved.getId() != null) {
                log.info("Created bootstrap admin user login={}", login);
            } else {
                log.error("Failed to create bootstrap admin user login={}", login);
            }
        };
    }

    private boolean shouldRefreshSeededAdmin(long totalUsers, UserDto configuredUser, String configuredLogin, String configuredDisplayName) {
        if (totalUsers != 1L || configuredUser == null) {
            return false;
        }
        if (!"ROLE_ADMIN".equals(configuredUser.getRoleCode())) {
            return false;
        }
        if (!configuredLogin.equals(configuredUser.getLogin())) {
            return false;
        }
        return true;
    }

    private boolean shouldReplaceSeededAdmin(long totalUsers, UserDto defaultAdmin, String configuredLogin) {
        if (totalUsers != 1L || defaultAdmin == null) {
            return false;
        }
        if (!"admin".equals(defaultAdmin.getLogin())) {
            return false;
        }
        if (!"ROLE_ADMIN".equals(defaultAdmin.getRoleCode())) {
            return false;
        }
        return !"admin".equals(configuredLogin);
    }
}
