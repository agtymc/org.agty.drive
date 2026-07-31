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

package org.agty.drive.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.agty.drive.security.service.DriveUserDetails;
import org.agty.drive.services.WebDavFolderAccessService;
import org.agty.drive.web.controllers.api.WebDavController;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

@Component
public class WebDavRequestHandler implements HttpRequestHandler {

    private final WebDavController webDavController;
    private final WebDavFolderAccessService webDavFolderAccessService;

    public WebDavRequestHandler(WebDavController webDavController,
                                WebDavFolderAccessService webDavFolderAccessService) {
        this.webDavController = webDavController;
        this.webDavFolderAccessService = webDavFolderAccessService;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ResponseEntity<?> entity;
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String path = requestUri.substring(Math.min(requestUri.length(), contextPath.length()));
        if (path.toLowerCase(Locale.ROOT).startsWith("/dav-share/")) {
            entity = handleSharedRequest(request, path);
        } else {
            entity = webDavController.handle(
                    request,
                    request.getHeader("Depth"),
                    request.getHeader("Destination"),
                    request.getHeader("Overwrite"),
                    request.getHeader(HttpHeaders.RANGE),
                    resolveCurrentUser()
            );
        }
        writeResponse(request, response, entity);
    }

    private ResponseEntity<?> handleSharedRequest(HttpServletRequest request, String requestPath) {
        String token = extractShareToken(requestPath);
        if (token == null) {
            return ResponseEntity.status(404).build();
        }
        Credentials credentials = decodeBasicCredentials(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (credentials == null) {
            return webDavController.unauthorized();
        }
        WebDavFolderAccessService.AuthenticatedAccess access =
                webDavFolderAccessService.authenticate(token, credentials.login(), credentials.password());
        if (access == null) {
            return webDavController.unauthorized();
        }

        String method = request.getMethod() == null ? "" : request.getMethod().trim().toUpperCase(Locale.ROOT);
        String relativePath = webDavController.extractDavPath(request, "/dav-share/" + token);
        return webDavController.handleWithContext(
                request,
                method,
                relativePath,
                request.getHeader("Depth"),
                request.getHeader("Destination"),
                request.getHeader("Overwrite"),
                request.getHeader(HttpHeaders.RANGE),
                WebDavController.WebDavContext.folder(
                        access.access().getOwnerId(),
                        access.rootFolder().getId(),
                        access.allowWrite(),
                        access.rootFolder().getName(),
                        token
                )
        );
    }

    private String extractShareToken(String path) {
        if (path == null || !path.startsWith("/dav-share/")) {
            return null;
        }
        String value = path.substring("/dav-share/".length());
        int slashIndex = value.indexOf('/');
        return slashIndex >= 0 ? value.substring(0, slashIndex) : value;
    }

    private Credentials decodeBasicCredentials(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Basic ")) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(authorizationHeader.substring(6).trim());
            String text = new String(decoded, StandardCharsets.UTF_8);
            int separatorIndex = text.indexOf(':');
            if (separatorIndex < 0) {
                return null;
            }
            return new Credentials(text.substring(0, separatorIndex), text.substring(separatorIndex + 1));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private DriveUserDetails resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof DriveUserDetails details ? details : null;
    }

    private void writeResponse(HttpServletRequest request,
                               HttpServletResponse response,
                               ResponseEntity<?> entity) throws IOException {
        response.setStatus(entity.getStatusCode().value());
        entity.getHeaders().forEach((headerName, values) -> {
            for (String value : values) {
                response.addHeader(headerName, value);
            }
        });

        if ("HEAD".equalsIgnoreCase(request.getMethod())) {
            return;
        }

        Object body = entity.getBody();
        if (body == null) {
            return;
        }

        if (body instanceof String text) {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            if (response.getContentType() == null) {
                response.setContentType("text/plain;charset=UTF-8");
            }
            response.getOutputStream().write(bytes);
            return;
        }

        if (body instanceof byte[] bytes) {
            response.getOutputStream().write(bytes);
            return;
        }

        if (body instanceof Resource resource) {
            try (InputStream inputStream = resource.getInputStream()) {
                inputStream.transferTo(response.getOutputStream());
            }
        }
    }

    private record Credentials(String login, String password) {
    }
}
