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
import org.agty.drive.support.IntegrationTestBootstrap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BootstrapAdminInitializerTest extends IntegrationTestBootstrap {

    @Autowired
    private ApplicationRunner bootstrapAdminRunner;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplicationArguments applicationArguments;

    @Test
    void shouldNotOverrideExistingAdminPasswordWhenDatabaseAlreadyInitialized() throws Exception {
        UserDto admin = userService.findByLogin("admin");
        assertNotNull(admin);

        admin.setPasswordHash(passwordEncoder.encode("manual-password-123"));
        UserDto savedAdmin = userService.save(admin);
        assertNotNull(savedAdmin);

        String previousPasswordProperty = System.getProperty("bootstrap.admin.password");
        try {
            System.setProperty("bootstrap.admin.password", "config-password-456");

            bootstrapAdminRunner.run(applicationArguments);

            UserDto reloadedAdmin = userService.findByLogin("admin");
            assertNotNull(reloadedAdmin);
            assertTrue(passwordEncoder.matches("manual-password-123", reloadedAdmin.getPasswordHash()));
            assertTrue(!passwordEncoder.matches("config-password-456", reloadedAdmin.getPasswordHash()));
        } finally {
            if (previousPasswordProperty == null) {
                System.clearProperty("bootstrap.admin.password");
            } else {
                System.setProperty("bootstrap.admin.password", previousPasswordProperty);
            }
        }
    }
}
