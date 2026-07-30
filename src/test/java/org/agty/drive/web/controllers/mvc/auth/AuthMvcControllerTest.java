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

package org.agty.drive.web.controllers.mvc.auth;

import org.agty.drive.dto.UserDto;
import org.agty.drive.dto.UserInviteCreateDto;
import org.agty.drive.dto.UserInviteDto;
import org.agty.drive.support.IntegrationTestBootstrap;
import org.agty.drive.services.AppSettingService;
import org.agty.drive.services.UserInviteService;
import org.agty.drive.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthMvcControllerTest extends IntegrationTestBootstrap {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserInviteService userInviteService;

    @Autowired
    private UserService userService;

    @Autowired
    private AppSettingService appSettingService;

    @Test
    void shouldAcceptInviteAndCreateUser() throws Exception {
        UserDto admin = userService.findByLogin("admin");
        assertNotNull(admin);

        String login = "accepted-user-" + System.nanoTime();
        String email = login + "@example.com";

        UserInviteCreateDto createDto = new UserInviteCreateDto();
        createDto.setLogin(login);
        createDto.setEmail(email);
        createDto.setDisplayName("Accepted User");
        createDto.setRoleCode("ROLE_USER");
        createDto.setStatusCode("ACTIVE");
        createDto.setStorageQuotaMb(128L);
        createDto.setExpiresInHours(48);

        UserInviteDto invite = userInviteService.create(admin.getId(), createDto);
        assertNotNull(invite);
        assertNull(userService.findByLogin(login));

        mockMvc.perform(get("/invite/{token}", invite.getToken()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Регистрация по инвайту")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(login)));

        mockMvc.perform(post("/invite/{token}", invite.getToken())
                        .param("password", "invite-pass-123")
                        .param("confirmPassword", "invite-pass-123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?invited"));

        UserDto createdUser = userService.findByLogin(login);
        assertNotNull(createdUser);
        assertEquals(email, createdUser.getEmail());
        assertEquals(128L, createdUser.getStorageQuotaMb());

        UserInviteDto updatedInvite = userInviteService.findByToken(invite.getToken());
        assertNotNull(updatedInvite);
        assertFalse(Boolean.TRUE.equals(updatedInvite.getIsEnabled()));
        assertNotNull(updatedInvite.getUsedAt());
        assertEquals(createdUser.getId(), updatedInvite.getInvitedUserId());
    }

    @Test
    void shouldRejectDisabledInvite() throws Exception {
        UserDto admin = userService.findByLogin("admin");
        assertNotNull(admin);

        String login = "disabled-user-" + System.nanoTime();

        UserInviteCreateDto createDto = new UserInviteCreateDto();
        createDto.setLogin(login);
        createDto.setRoleCode("ROLE_USER");
        createDto.setStatusCode("ACTIVE");
        createDto.setStorageQuotaMb(64L);
        createDto.setExpiresInHours(24);

        UserInviteDto invite = userInviteService.create(admin.getId(), createDto);
        assertNotNull(invite);

        String disableError = userInviteService.disable(invite.getId());
        assertNull(disableError);
        UserInviteDto disabledInvite = userInviteService.findByToken(invite.getToken());
        assertNotNull(disabledInvite);
        assertFalse(Boolean.TRUE.equals(disabledInvite.getIsEnabled()));

        mockMvc.perform(get("/invite/{token}", invite.getToken()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Инвайт недоступен или срок его действия истек.")));
    }

    @Test
    void shouldRegisterOpenUserWhenModeEnabled() throws Exception {
        UserDto admin = userService.findByLogin("admin");
        assertNotNull(admin);
        org.junit.jupiter.api.Assertions.assertTrue(appSettingService.updateOpenRegistrationEnabled(true, admin.getId()));

        String login = "open-user-" + System.nanoTime();
        String email = login + "@example.com";

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Регистрация")));

        mockMvc.perform(post("/register")
                        .param("login", login)
                        .param("email", email)
                        .param("displayName", "Open User")
                        .param("password", "open-pass-123")
                        .param("confirmPassword", "open-pass-123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        UserDto createdUser = userService.findByLogin(login);
        assertNotNull(createdUser);
        assertEquals(email, createdUser.getEmail());
        assertEquals("ROLE_USER", createdUser.getRoleCode());
        assertEquals("ACTIVE", createdUser.getStatusCode());
    }

    @Test
    void shouldRejectOpenRegistrationWhenModeDisabled() throws Exception {
        UserDto admin = userService.findByLogin("admin");
        assertNotNull(admin);
        org.junit.jupiter.api.Assertions.assertTrue(appSettingService.updateOpenRegistrationEnabled(false, admin.getId()));

        String login = "closed-user-" + System.nanoTime();

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Открытая регистрация сейчас отключена.")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Зарегистрироваться"))));

        mockMvc.perform(post("/register")
                        .param("login", login)
                        .param("email", login + "@example.com")
                        .param("displayName", "Closed User")
                        .param("password", "closed-pass-123")
                        .param("confirmPassword", "closed-pass-123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Открытая регистрация сейчас отключена.")));

        assertNull(userService.findByLogin(login));
    }
}
