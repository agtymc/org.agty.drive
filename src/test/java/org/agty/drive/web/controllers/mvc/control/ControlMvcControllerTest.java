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

package org.agty.drive.web.controllers.mvc.control;

import org.agty.drive.support.IntegrationTestBootstrap;
import org.agty.drive.dto.UserDto;
import org.agty.drive.services.UserInviteService;
import org.agty.drive.services.AppSettingService;
import org.agty.drive.security.service.DriveUserDetails;
import org.agty.drive.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ControlMvcControllerTest extends IntegrationTestBootstrap {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserInviteService userInviteService;

    @Autowired
    private AppSettingService appSettingService;

    @Test
    void shouldRenderAdminSectionsWithSidebar() throws Exception {
        UserDto admin = userService.findByLogin("admin");
        assertNotNull(admin);

        mockMvc.perform(get("/control")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Обзор")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Пользователи")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Справочники")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Аудит")));

        mockMvc.perform(get("/control/users")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Управление пользователями")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("returnSection\" value=\"users\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("class=\"quota-form\""))));

        mockMvc.perform(get("/control/create")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Новый пользователь")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("returnSection\" value=\"create\"")));

        mockMvc.perform(get("/control/dictionaries")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Словарь ролей")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Словарь статусов")));

        mockMvc.perform(get("/control/registration")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Инвайты на регистрацию")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Сохранить режим регистрации")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Всего инвайтов")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Создать инвайт")));

        mockMvc.perform(get("/control/audit")
                        .param("page", "1")
                        .param("size", "25")
                        .param("sort", "date_desc")
                        .param("actorLogin", "admin")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Лента аудита")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Новые сверху")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("На странице")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Фильтр")));
    }

    @Test
    void shouldRedirectBackToUsersSectionAfterAccessUpdateIncludingQuota() throws Exception {
        UserDto admin = userService.findByLogin("admin");
        assertNotNull(admin);

        mockMvc.perform(post("/control/users/access")
                        .param("userId", String.valueOf(admin.getId()))
                        .param("roleCode", admin.getRoleCode())
                        .param("statusCode", admin.getStatusCode())
                        .param("storageQuotaMb", "128")
                        .param("returnSection", "users")
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(admin))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/control/users"));

        UserDto updated = userService.findById(admin.getId());
        assertNotNull(updated);
        assertEquals(128L, updated.getStorageQuotaMb());
    }

    @Test
    void shouldCreateInviteAndRejectDuplicateActiveInvite() throws Exception {
        UserDto admin = userService.findByLogin("admin");
        assertNotNull(admin);

        String login = "invite-user-" + System.nanoTime();
        String email = login + "@example.com";

        mockMvc.perform(post("/control/registration/invites/create")
                        .param("login", login)
                        .param("email", email)
                        .param("displayName", "Invite User")
                        .param("roleCode", "ROLE_USER")
                        .param("statusCode", "ACTIVE")
                        .param("storageQuotaMb", "64")
                        .param("expiresInHours", "24")
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(admin))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/control/registration"));

        assertEquals(1L, userInviteService.findAll().stream().filter(item -> login.equals(item.getLogin())).count());

        mockMvc.perform(get("/control/registration")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/invite/")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(login)));

        mockMvc.perform(post("/control/registration/invites/create")
                        .param("login", login)
                        .param("email", email)
                        .param("displayName", "Invite User")
                        .param("roleCode", "ROLE_USER")
                        .param("statusCode", "ACTIVE")
                        .param("storageQuotaMb", "64")
                        .param("expiresInHours", "24")
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Для этого логина уже есть активный инвайт.")));
    }

    @Test
    void shouldUpdateOpenRegistrationMode() throws Exception {
        UserDto admin = userService.findByLogin("admin");
        assertNotNull(admin);

        mockMvc.perform(post("/control/registration/mode")
                        .param("openRegistrationEnabled", "true")
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(admin))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/control/registration"));

        org.junit.jupiter.api.Assertions.assertTrue(appSettingService.isOpenRegistrationEnabled());
    }
}
