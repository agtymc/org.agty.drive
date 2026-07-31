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

package org.agty.drive.web.controllers.api;

import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FolderDto;
import org.agty.drive.dto.UserDto;
import org.agty.drive.dto.WebDavFolderAccessCreateDto;
import org.agty.drive.security.service.DriveUserDetails;
import org.agty.drive.services.FileService;
import org.agty.drive.services.FolderService;
import org.agty.drive.services.UserService;
import org.agty.drive.services.WebDavFolderAccessService;
import org.agty.drive.support.IntegrationTestBootstrap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebDavControllerTest extends IntegrationTestBootstrap {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private FolderService folderService;

    @Autowired
    private FileService fileService;

    @Autowired
    private WebDavFolderAccessService webDavFolderAccessService;

    @Test
    void shouldSupportBasicWebDavFlow() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        String folderName = "dav-" + UUID.randomUUID();
        String fileName = "hello-" + UUID.randomUUID() + ".txt";
        String movedName = "moved-" + UUID.randomUUID() + ".txt";
        String copiedName = "copied-" + UUID.randomUUID() + ".txt";
        byte[] payload = ("webdav payload " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(request(HttpMethod.OPTIONS, "/dav")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(header().string("DAV", "1, 2"))
                .andExpect(header().string(HttpHeaders.ALLOW, org.hamcrest.Matchers.containsString("PROPFIND")))
                .andExpect(header().string(HttpHeaders.ALLOW, org.hamcrest.Matchers.containsString("LOCK")));

        mockMvc.perform(request(HttpMethod.valueOf("MKCOL"), "/dav/{folder}", folderName)
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isCreated())
                .andExpect(header().string("DAV", "1, 2"));

        FolderDto folder = findRootFolderByName(user.getId(), folderName);
        assertNotNull(folder);

        mockMvc.perform(request(HttpMethod.PUT, "/dav/{folder}/{file}", folderName, fileName)
                        .contentType("text/plain")
                        .content(payload)
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isCreated())
                .andExpect(header().string("DAV", "1, 2"));

        FileItemDto savedFile = fileService.findByOwnerIdAndFolderId(user.getId(), folder.getId()).stream()
                .filter(file -> fileName.equals(file.getOriginalFilename()))
                .findFirst()
                .orElse(null);
        assertNotNull(savedFile);

        mockMvc.perform(request(HttpMethod.valueOf("PROPFIND"), "/dav/{folder}", folderName)
                        .header("Depth", "1")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isMultiStatus())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(folderName)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(fileName)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("supportedlock")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("lockdiscovery")));

        mockMvc.perform(request(HttpMethod.valueOf("LOCK"), "/dav/{folder}/{file}", folderName, fileName)
                        .contentType("application/xml")
                        .content("""
                                <?xml version="1.0" encoding="utf-8" ?>
                                <D:lockinfo xmlns:D='DAV:'>
                                  <D:lockscope><D:exclusive/></D:lockscope>
                                  <D:locktype><D:write/></D:locktype>
                                  <D:owner><D:href>test</D:href></D:owner>
                                </D:lockinfo>
                                """)
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(header().string("Lock-Token", org.hamcrest.Matchers.containsString("opaquelocktoken:")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("activelock")));

        mockMvc.perform(request(HttpMethod.valueOf("UNLOCK"), "/dav/{folder}/{file}", folderName, fileName)
                        .header("Lock-Token", "<opaquelocktoken:test>")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/dav/{folder}/{file}", folderName, fileName)
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString("text/plain")))
                .andExpect(content().bytes(payload));

        mockMvc.perform(request(HttpMethod.valueOf("MOVE"), "/dav/{folder}/{file}", folderName, fileName)
                        .header("Destination", "/dav/" + movedName)
                        .header("Overwrite", "T")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isNoContent());

        FileItemDto movedFile = fileService.findByIdAndOwnerId(savedFile.getId(), user.getId());
        assertNotNull(movedFile);
        assertNull(movedFile.getFolderId());
        assertEquals(movedName, movedFile.getOriginalFilename());

        mockMvc.perform(request(HttpMethod.valueOf("COPY"), "/dav/{file}", movedName)
                        .header("Destination", "/dav/" + copiedName)
                        .header("Overwrite", "T")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isCreated());

        FileItemDto copiedFile = fileService.findByOwnerIdAndFolderId(user.getId(), null).stream()
                .filter(file -> copiedName.equals(file.getOriginalFilename()))
                .findFirst()
                .orElse(null);
        assertNotNull(copiedFile);

        mockMvc.perform(request(HttpMethod.DELETE, "/dav/{file}", copiedName)
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isNoContent());

        FileItemDto deletedCopy = fileService.findByIdAndOwnerId(copiedFile.getId(), user.getId());
        org.junit.jupiter.api.Assertions.assertTrue(deletedCopy == null || deletedCopy.getId() == null);
    }

    @Test
    void shouldSupportFolderScopedWebDavFlow() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        String folderName = "shared-dav-" + UUID.randomUUID();
        String fileName = "inside-" + UUID.randomUUID() + ".txt";
        String movedName = "renamed-" + UUID.randomUUID() + ".txt";
        String copiedName = "copied-" + UUID.randomUUID() + ".txt";
        String login = "client-" + UUID.randomUUID().toString().substring(0, 8);
        String password = "Secret-" + UUID.randomUUID();
        byte[] payload = ("shared webdav payload " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);

        FolderDto folderDto = new FolderDto();
        folderDto.setOwnerId(user.getId());
        folderDto.setName(folderName);
        folderDto.setPathKey(folderService.buildPathKeyForCreate(user.getId(), null, folderName));
        FolderDto folder = folderService.save(folderDto);
        assertNotNull(folder);

        WebDavFolderAccessCreateDto accessCreateDto = new WebDavFolderAccessCreateDto();
        accessCreateDto.setFolderId(folder.getId());
        accessCreateDto.setLoginName(login);
        accessCreateDto.setPassword(password);
        accessCreateDto.setAllowWrite(true);
        accessCreateDto.setEnabled(true);
        accessCreateDto.setRotateToken(false);

        WebDavFolderAccessService.SaveResult saveResult = webDavFolderAccessService.saveFolderAccess(user.getId(), accessCreateDto);
        org.junit.jupiter.api.Assertions.assertTrue(saveResult.success());
        String token = saveResult.access().getAccessToken();
        String authorization = basic(login, password);

        mockMvc.perform(request(HttpMethod.OPTIONS, "/dav-share/{token}", token)
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(header().string("DAV", "1, 2"))
                .andExpect(header().string(HttpHeaders.ALLOW, org.hamcrest.Matchers.containsString("LOCK")));

        mockMvc.perform(request(HttpMethod.PUT, "/dav-share/{token}/{file}", token, fileName)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType("text/plain")
                        .content(payload))
                .andExpect(status().isCreated());

        FileItemDto savedFile = fileService.findByOwnerIdAndFolderId(user.getId(), folder.getId()).stream()
                .filter(file -> fileName.equals(file.getOriginalFilename()))
                .findFirst()
                .orElse(null);
        assertNotNull(savedFile);

        mockMvc.perform(request(HttpMethod.valueOf("PROPFIND"), "/dav-share/{token}", token)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .header("Depth", "1"))
                .andExpect(status().isMultiStatus())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(folderName)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(fileName)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("supportedlock")));

        mockMvc.perform(get("/dav-share/{token}/", token)
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString("text/html")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(fileName)));

        mockMvc.perform(request(HttpMethod.valueOf("LOCK"), "/dav-share/{token}/{file}", token, fileName)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType("application/xml")
                        .content("""
                                <?xml version="1.0" encoding="utf-8" ?>
                                <D:lockinfo xmlns:D='DAV:'>
                                  <D:lockscope><D:exclusive/></D:lockscope>
                                  <D:locktype><D:write/></D:locktype>
                                  <D:owner><D:href>client</D:href></D:owner>
                                </D:lockinfo>
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Lock-Token", org.hamcrest.Matchers.containsString("opaquelocktoken:")));

        mockMvc.perform(request(HttpMethod.valueOf("UNLOCK"), "/dav-share/{token}/{file}", token, fileName)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .header("Lock-Token", "<opaquelocktoken:test>"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/dav-share/{token}/{file}", token, fileName)
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(content().bytes(payload));

        mockMvc.perform(request(HttpMethod.valueOf("MOVE"), "/dav-share/{token}/{file}", token, fileName)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .header("Destination", "/dav-share/" + token + "/" + movedName)
                        .header("Overwrite", "T"))
                .andExpect(status().isNoContent());

        FileItemDto movedFile = fileService.findByIdAndOwnerId(savedFile.getId(), user.getId());
        assertNotNull(movedFile);
        assertEquals(folder.getId(), movedFile.getFolderId());
        assertEquals(movedName, movedFile.getOriginalFilename());

        mockMvc.perform(request(HttpMethod.valueOf("COPY"), "/dav-share/{token}/{file}", token, movedName)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .header("Destination", "/dav-share/" + token + "/" + copiedName)
                        .header("Overwrite", "T"))
                .andExpect(status().isCreated());

        FileItemDto copiedFile = fileService.findByOwnerIdAndFolderId(user.getId(), folder.getId()).stream()
                .filter(file -> copiedName.equals(file.getOriginalFilename()))
                .findFirst()
                .orElse(null);
        assertNotNull(copiedFile);
    }

    @Test
    void shouldDenyWriteForReadOnlyFolderScopedWebDav() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        String folderName = "readonly-dav-" + UUID.randomUUID();
        String login = "reader-" + UUID.randomUUID().toString().substring(0, 8);
        String password = "Secret-" + UUID.randomUUID();

        FolderDto folderDto = new FolderDto();
        folderDto.setOwnerId(user.getId());
        folderDto.setName(folderName);
        folderDto.setPathKey(folderService.buildPathKeyForCreate(user.getId(), null, folderName));
        FolderDto folder = folderService.save(folderDto);
        assertNotNull(folder);

        WebDavFolderAccessCreateDto accessCreateDto = new WebDavFolderAccessCreateDto();
        accessCreateDto.setFolderId(folder.getId());
        accessCreateDto.setLoginName(login);
        accessCreateDto.setPassword(password);
        accessCreateDto.setAllowWrite(false);
        accessCreateDto.setEnabled(true);
        accessCreateDto.setRotateToken(false);

        WebDavFolderAccessService.SaveResult saveResult = webDavFolderAccessService.saveFolderAccess(user.getId(), accessCreateDto);
        org.junit.jupiter.api.Assertions.assertTrue(saveResult.success());
        String token = saveResult.access().getAccessToken();

        mockMvc.perform(request(HttpMethod.PUT, "/dav-share/{token}/forbidden.txt", token)
                        .header(HttpHeaders.AUTHORIZATION, basic(login, password))
                        .contentType("text/plain")
                        .content("test".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isForbidden());
    }

    private FolderDto findRootFolderByName(Long ownerId, String name) {
        return folderService.findRootFoldersByOwnerId(ownerId).stream()
                .filter(folder -> name.equals(folder.getName()))
                .findFirst()
                .orElse(null);
    }

    private String basic(String login, String password) {
        String raw = login + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
