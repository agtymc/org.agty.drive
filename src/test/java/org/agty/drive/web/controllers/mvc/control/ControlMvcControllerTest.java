package org.agty.drive.web.controllers.mvc.control;

import org.agty.drive.support.IntegrationTestBootstrap;
import org.agty.drive.dto.UserDto;
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
}
