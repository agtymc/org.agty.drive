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

import jakarta.servlet.http.HttpServletRequest;
import org.agty.drive.config.AppTime;
import org.agty.drive.config.ApplicationInfo;
import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FolderDto;
import org.agty.drive.security.service.DriveUserDetails;
import org.agty.drive.services.FileContentStorageService;
import org.agty.drive.services.MimeTypePolicyService;
import org.agty.drive.services.WebDavService;
import org.agty.drive.web.MediaResponseSupport;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RestController
public class WebDavController {

    private static final String DAV_BASE_PATH = "/dav";
    private static final String DAV_SHARE_BASE_PATH = "/dav-share";
    private static final String DAV_HEADER_VALUE = "1, 2";
    private static final String ALLOW_HEADER_VALUE = "OPTIONS, PROPFIND, GET, HEAD, PUT, MKCOL, DELETE, MOVE, COPY, LOCK, UNLOCK";

    private final WebDavService webDavService;
    private final FileContentStorageService fileContentStorageService;
    private final MimeTypePolicyService mimeTypePolicyService;
    private final ApplicationInfo applicationInfo;

    public WebDavController(WebDavService webDavService,
                            FileContentStorageService fileContentStorageService,
                            MimeTypePolicyService mimeTypePolicyService,
                            ApplicationInfo applicationInfo) {
        this.webDavService = webDavService;
        this.fileContentStorageService = fileContentStorageService;
        this.mimeTypePolicyService = mimeTypePolicyService;
        this.applicationInfo = applicationInfo;
    }

    @RequestMapping({DAV_BASE_PATH, DAV_BASE_PATH + "/**"})
    public ResponseEntity<?> handle(HttpServletRequest request,
                                    @RequestHeader(name = "Depth", required = false) String depthHeader,
                                    @RequestHeader(name = "Destination", required = false) String destinationHeader,
                                    @RequestHeader(name = "Overwrite", required = false) String overwriteHeader,
                                    @RequestHeader(name = HttpHeaders.RANGE, required = false) String rangeHeader,
                                    @AuthenticationPrincipal DriveUserDetails userDetails) {
        if (userDetails == null || userDetails.getUser() == null || userDetails.getUser().getId() == null) {
            return withDavHeaders(ResponseEntity.status(401)).build();
        }

        String method = request.getMethod() == null ? "" : request.getMethod().trim().toUpperCase(Locale.ROOT);
        String requestPath = extractDavPath(request);
        Long ownerId = userDetails.getUser().getId();
        WebDavContext context = WebDavContext.userRoot(ownerId);

        return handleWithContext(request, method, requestPath, depthHeader, destinationHeader, overwriteHeader, rangeHeader, context);
    }

    public ResponseEntity<?> handleWithContext(HttpServletRequest request,
                                               String method,
                                               String requestPath,
                                               String depthHeader,
                                               String destinationHeader,
                                               String overwriteHeader,
                                               String rangeHeader,
                                               WebDavContext context) {
        if (context == null || context.ownerId() == null) {
            return unauthorized();
        }

        return switch (method) {
            case "OPTIONS" -> options();
            case "PROPFIND" -> propFind(context, requestPath, depthHeader, request);
            case "GET", "HEAD" -> get(context, requestPath, rangeHeader);
            case "PUT" -> put(context, requestPath, request);
            case "MKCOL" -> mkcol(context, requestPath);
            case "DELETE" -> delete(context, requestPath);
            case "MOVE" -> move(context, requestPath, destinationHeader, overwriteHeader, request);
            case "COPY" -> copy(context, requestPath, destinationHeader, overwriteHeader, request);
            case "LOCK" -> lock(context, requestPath, request);
            case "UNLOCK" -> unlock(context, requestPath);
            default -> withDavHeaders(ResponseEntity.status(405).header(HttpHeaders.ALLOW, ALLOW_HEADER_VALUE)).build();
        };
    }

    public ResponseEntity<?> unauthorized() {
        return withDavHeaders(ResponseEntity.status(401)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"AGTY WebDAV\", charset=\"UTF-8\""))
                .build();
    }

    private ResponseEntity<?> options() {
        return withDavHeaders(ResponseEntity.ok()
                .header(HttpHeaders.ALLOW, ALLOW_HEADER_VALUE)
                .header("MS-Author-Via", "DAV"))
                .build();
    }

    private ResponseEntity<?> propFind(WebDavContext context,
                                       String requestPath,
                                       String depthHeader,
                                       HttpServletRequest request) {
        WebDavService.DavResource resource = webDavService.resolve(context.ownerId(), requestPath, context.rootFolderId());
        if (resource == null) {
            return withDavHeaders(ResponseEntity.status(404)).build();
        }

        int depth = parseDepth(depthHeader, resource.isCollection());
        List<PropFindEntry> entries = new ArrayList<>();
        entries.add(buildEntry(resource, requestPath, request, context));
        if (depth > 0 && resource.isCollection()) {
            for (WebDavService.DavResource child : webDavService.listChildren(context.ownerId(), resource, context.rootFolderId())) {
                entries.add(buildEntry(child, childPath(requestPath, child.name()), request, context));
            }
        }

        String xml = buildPropFindResponse(entries);
        ResponseEntity.BodyBuilder builder = withDavHeaders(ResponseEntity.status(207))
                .contentType(MediaType.APPLICATION_XML);
        return builder.body(xml);
    }

    private ResponseEntity<?> get(WebDavContext context, String requestPath, String rangeHeader) {
        WebDavService.DavResource resource = webDavService.resolve(context.ownerId(), requestPath, context.rootFolderId());
        if (resource == null) {
            return withDavHeaders(ResponseEntity.status(404)).build();
        }
        if (resource.isCollection()) {
            String html = buildCollectionIndex(context, resource, requestPath);
            return withDavHeaders(ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML))
                    .body(html);
        }

        FileItemDto file = resource.file();
        Path path = fileContentStorageService.resolveExistingPath(file.getStorageName());
        ResponseEntity<Resource> response = MediaResponseSupport.buildPathResponse(
                path,
                mimeTypePolicyService.resolveResponseMediaType(file.getMimeType()),
                file.getOriginalFilename(),
                true,
                rangeHeader
        );
        return withDavHeaders(ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .eTag(buildEtag(resource))
                .lastModified(resolveLastModifiedMillis(resource)))
                .body(response.getBody());
    }

    private String buildCollectionIndex(WebDavContext context,
                                        WebDavService.DavResource resource,
                                        String requestPath) {
        String normalizedPath = webDavService.normalizePath(requestPath);
        List<WebDavService.DavResource> children = webDavService.listChildren(context.ownerId(), resource, context.rootFolderId());
        StringBuilder builder = new StringBuilder();
        builder.append("""
                <!DOCTYPE html>
                <html lang="ru">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>WebDAV</title>
                  <style>
                    body { font-family: system-ui, sans-serif; margin: 32px; color: #1f2937; background: #f8fafc; }
                    main { max-width: 880px; margin: 0 auto; background: #fff; border: 1px solid #e5e7eb; border-radius: 16px; padding: 24px; }
                    h1 { margin: 0 0 8px; font-size: 24px; }
                    p { margin: 0 0 20px; color: #6b7280; }
                    ul { list-style: none; padding: 0; margin: 0; }
                    li { border-top: 1px solid #e5e7eb; }
                    li:first-child { border-top: 0; }
                    a { display: flex; justify-content: space-between; gap: 16px; padding: 14px 0; color: #111827; text-decoration: none; }
                    a:hover { color: #2563eb; }
                    .meta { color: #6b7280; font-size: 14px; }
                  </style>
                </head>
                <body>
                <main>
                """);
        builder.append("<h1>").append(escapeXml(resource.isRoot() ? context.rootDisplayName() : resource.name())).append("</h1>");
        builder.append("<p>Папка WebDAV: ").append(escapeXml(normalizedPath)).append("</p>");
        builder.append("<ul>");
        if (!"/".equals(normalizedPath)) {
            String parentPath = parentPath(normalizedPath);
            builder.append("<li><a href=\"")
                    .append(escapeHtmlAttribute(buildBrowserHref(context.mountBasePath(), parentPath, true)))
                    .append("\"><span>..</span><span class=\"meta\">Вверх</span></a></li>");
        }
        for (WebDavService.DavResource child : children) {
            boolean collection = child.isCollection();
            String childPath = childPath(normalizedPath, child.name());
            String href = buildBrowserHref(context.mountBasePath(), childPath, collection);
            builder.append("<li><a href=\"")
                    .append(escapeHtmlAttribute(href))
                    .append("\"><span>")
                    .append(escapeXml(child.name()))
                    .append(collection ? "/" : "")
                    .append("</span><span class=\"meta\">")
                    .append(collection ? "Папка" : formatSize(child.file() == null ? 0L : child.file().getSizeBytes()))
                    .append("</span></a></li>");
        }
        builder.append("</ul></main></body></html>");
        return builder.toString();
    }

    private ResponseEntity<?> put(WebDavContext context, String requestPath, HttpServletRequest request) {
        if (!context.allowWrite()) {
            return withDavHeaders(ResponseEntity.status(403)).build();
        }
        String normalizedPath = webDavService.normalizePath(requestPath);
        if (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
            return withDavHeaders(ResponseEntity.status(409)).build();
        }

        try (InputStream inputStream = request.getInputStream()) {
            WebDavService.DavOperationResult result = webDavService.putFile(
                    context.ownerId(),
                    normalizedPath,
                    request.getContentType(),
                    inputStream,
                    context.rootFolderId()
            );
            return mapOperationResult(result);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private ResponseEntity<?> mkcol(WebDavContext context, String requestPath) {
        if (!context.allowWrite()) {
            return withDavHeaders(ResponseEntity.status(403)).build();
        }
        return mapOperationResult(webDavService.createFolder(context.ownerId(), requestPath, context.rootFolderId()));
    }

    private ResponseEntity<?> delete(WebDavContext context, String requestPath) {
        if (!context.allowWrite()) {
            return withDavHeaders(ResponseEntity.status(403)).build();
        }
        return mapOperationResult(webDavService.delete(context.ownerId(), requestPath, context.rootFolderId()));
    }

    private ResponseEntity<?> move(WebDavContext context,
                                   String requestPath,
                                   String destinationHeader,
                                   String overwriteHeader,
                                   HttpServletRequest request) {
        if (!context.allowWrite()) {
            return withDavHeaders(ResponseEntity.status(403)).build();
        }
        String destinationPath = extractDestinationPath(destinationHeader, request, context.mountBasePath());
        if (destinationPath == null) {
            return withDavHeaders(ResponseEntity.status(400)).build();
        }
        return mapOperationResult(webDavService.move(context.ownerId(), requestPath, destinationPath, allowOverwrite(overwriteHeader), context.rootFolderId()));
    }

    private ResponseEntity<?> copy(WebDavContext context,
                                   String requestPath,
                                   String destinationHeader,
                                   String overwriteHeader,
                                   HttpServletRequest request) {
        String destinationPath = extractDestinationPath(destinationHeader, request, context.mountBasePath());
        if (destinationPath == null) {
            return withDavHeaders(ResponseEntity.status(400)).build();
        }
        return mapOperationResult(webDavService.copy(context.ownerId(), requestPath, destinationPath, allowOverwrite(overwriteHeader), context.rootFolderId()));
    }

    private ResponseEntity<?> lock(WebDavContext context, String requestPath, HttpServletRequest request) {
        if (!context.allowWrite()) {
            return withDavHeaders(ResponseEntity.status(403)).build();
        }

        WebDavService.DavResource resource = webDavService.resolve(context.ownerId(), requestPath, context.rootFolderId());
        String lockToken = "opaquelocktoken:" + java.util.UUID.randomUUID();
        String href = buildHref(request, requestPath, resource != null && resource.isCollection(), context.mountBasePath());
        String body = buildLockResponse(href, lockToken);
        ResponseEntity.BodyBuilder builder = withDavHeaders(ResponseEntity.status(resource == null ? 201 : 200))
                .contentType(MediaType.APPLICATION_XML)
                .header("Lock-Token", "<" + lockToken + ">");
        return builder.body(body);
    }

    private ResponseEntity<?> unlock(WebDavContext context, String requestPath) {
        if (!context.allowWrite()) {
            return withDavHeaders(ResponseEntity.status(403)).build();
        }
        WebDavService.DavResource resource = webDavService.resolve(context.ownerId(), requestPath, context.rootFolderId());
        if (resource == null) {
            return withDavHeaders(ResponseEntity.status(404)).build();
        }
        return withDavHeaders(ResponseEntity.status(204)).build();
    }

    private ResponseEntity<?> mapOperationResult(WebDavService.DavOperationResult result) {
        ResponseEntity.BodyBuilder builder = withDavHeaders(ResponseEntity.status(result.status()));
        if (result.message() != null && !result.message().isBlank()) {
            builder.contentType(MediaType.TEXT_PLAIN);
            return builder.body(result.message());
        }
        return builder.build();
    }

    private ResponseEntity.BodyBuilder withDavHeaders(ResponseEntity.BodyBuilder builder) {
        return builder
                .header("DAV", DAV_HEADER_VALUE)
                .header(HttpHeaders.ALLOW, ALLOW_HEADER_VALUE);
    }

    public String extractDavPath(HttpServletRequest request) {
        return extractDavPath(request, DAV_BASE_PATH);
    }

    public String extractDavPath(HttpServletRequest request, String mountBasePath) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String path = requestUri.substring(Math.min(requestUri.length(), contextPath.length()));
        if (!path.startsWith(mountBasePath)) {
            return "/";
        }
        String davPath = path.substring(mountBasePath.length());
        if (davPath.isBlank()) {
            return "/";
        }
        return UriUtils.decode(davPath, StandardCharsets.UTF_8);
    }

    private String extractDestinationPath(String destinationHeader, HttpServletRequest request) {
        return extractDestinationPath(destinationHeader, request, DAV_BASE_PATH);
    }

    private String extractDestinationPath(String destinationHeader, HttpServletRequest request, String mountBasePath) {
        if (destinationHeader == null || destinationHeader.isBlank()) {
            return null;
        }

        String candidate = destinationHeader.trim();
        URI uri = URI.create(candidate);
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            path = candidate;
        }

        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        if (!contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (!path.startsWith(mountBasePath)) {
            return null;
        }
        String davPath = path.substring(mountBasePath.length());
        return davPath.isBlank() ? "/" : UriUtils.decode(davPath, StandardCharsets.UTF_8);
    }

    private boolean allowOverwrite(String overwriteHeader) {
        return overwriteHeader == null || !"F".equalsIgnoreCase(overwriteHeader.trim());
    }

    private int parseDepth(String depthHeader, boolean collection) {
        if (!collection) {
            return 0;
        }
        if (depthHeader == null || depthHeader.isBlank()) {
            return 1;
        }
        String value = depthHeader.trim();
        if ("0".equals(value)) {
            return 0;
        }
        return 1;
    }

    private PropFindEntry buildEntry(WebDavService.DavResource resource, String resourcePath, HttpServletRequest request, WebDavContext context) {
        boolean collection = resource.isCollection();
        String href = buildHref(request, resourcePath, collection, context.mountBasePath());
        String displayName = resource.isRoot() ? context.rootDisplayName() : resource.name();
        String contentType = resource.file() == null ? null : resource.file().getMimeType();
        long size = resource.file() == null || resource.file().getSizeBytes() == null ? 0L : resource.file().getSizeBytes();
        String creationDate = formatIsoDate(resource);
        String modifiedDate = formatRfc1123Date(resource);
        String etag = buildEtag(resource);
        return new PropFindEntry(href, displayName, collection, contentType, size, creationDate, modifiedDate, etag);
    }

    private String childPath(String parentPath, String childName) {
        String normalizedParent = webDavService.normalizePath(parentPath);
        if (!normalizedParent.endsWith("/")) {
            normalizedParent += "/";
        }
        return normalizedParent + childName;
    }

    private String parentPath(String path) {
        String normalizedPath = webDavService.normalizePath(path);
        if ("/".equals(normalizedPath)) {
            return "/";
        }
        String trimmed = normalizedPath.endsWith("/") && normalizedPath.length() > 1
                ? normalizedPath.substring(0, normalizedPath.length() - 1)
                : normalizedPath;
        int slashIndex = trimmed.lastIndexOf('/');
        if (slashIndex <= 0) {
            return "/";
        }
        return trimmed.substring(0, slashIndex);
    }

    private String buildBrowserHref(String mountBasePath, String resourcePath, boolean collection) {
        String normalizedPath = webDavService.normalizePath(resourcePath);
        String encodedPath = "/".equals(normalizedPath)
                ? "/"
                : "/" + UriUtils.encodePathSegment(normalizedPath.substring(1), StandardCharsets.UTF_8);
        String href = mountBasePath + encodedPath;
        if (collection && !href.endsWith("/")) {
            href += "/";
        }
        return href;
    }

    private String buildHref(HttpServletRequest request, String resourcePath, boolean collection, String mountBasePath) {
        String normalizedPath = webDavService.normalizePath(resourcePath);
        String fullPath = mountBasePath + ("/".equals(normalizedPath) ? "/" : normalizedPath);
        if (collection && !fullPath.endsWith("/")) {
            fullPath += "/";
        }
        return request.getContextPath() + fullPath;
    }

    private long resolveLastModifiedMillis(WebDavService.DavResource resource) {
        LocalDateTime value = null;
        if (resource.folder() != null) {
            value = AppTime.parseDatabaseDateTime(resource.folder().getUpdatedAt());
            if (value == null) {
                value = AppTime.parseDatabaseDateTime(resource.folder().getCreatedAt());
            }
        } else if (resource.file() != null) {
            value = AppTime.parseDatabaseDateTime(resource.file().getUpdatedAt());
            if (value == null) {
                value = AppTime.parseDatabaseDateTime(resource.file().getCreatedAt());
            }
        }
        return value == null ? -1L : value.atZone(AppTime.getZoneId()).toInstant().toEpochMilli();
    }

    private String formatIsoDate(WebDavService.DavResource resource) {
        LocalDateTime value = null;
        if (resource.folder() != null) {
            value = AppTime.parseDatabaseDateTime(resource.folder().getCreatedAt());
        } else if (resource.file() != null) {
            value = AppTime.parseDatabaseDateTime(resource.file().getCreatedAt());
        }
        if (value == null) {
            return "";
        }
        return value.atZone(AppTime.getZoneId()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private String formatRfc1123Date(WebDavService.DavResource resource) {
        LocalDateTime value = null;
        if (resource.folder() != null) {
            value = AppTime.parseDatabaseDateTime(resource.folder().getUpdatedAt());
            if (value == null) {
                value = AppTime.parseDatabaseDateTime(resource.folder().getCreatedAt());
            }
        } else if (resource.file() != null) {
            value = AppTime.parseDatabaseDateTime(resource.file().getUpdatedAt());
            if (value == null) {
                value = AppTime.parseDatabaseDateTime(resource.file().getCreatedAt());
            }
        }
        if (value == null) {
            return "";
        }
        return ZonedDateTime.of(value, AppTime.getZoneId()).format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    private String buildEtag(WebDavService.DavResource resource) {
        if (resource.file() != null) {
            FileItemDto file = resource.file();
            String basis = file.getChecksumSha256() == null || file.getChecksumSha256().isBlank()
                    ? String.valueOf(file.getId())
                    : file.getChecksumSha256();
            return "\"" + basis + "\"";
        }
        if (resource.folder() != null) {
            FolderDto folder = resource.folder();
            String basis = folder.getUpdatedAt() == null || folder.getUpdatedAt().isBlank()
                    ? String.valueOf(folder.getId())
                    : folder.getUpdatedAt() + ":" + folder.getId();
            return "\"" + basis.hashCode() + "\"";
        }
        return "\"root\"";
    }

    private String buildPropFindResponse(List<PropFindEntry> entries) {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        builder.append("<d:multistatus xmlns:d=\"DAV:\">");
        for (PropFindEntry entry : entries) {
            builder.append("<d:response>");
            builder.append("<d:href>").append(escapeXml(entry.href())).append("</d:href>");
            builder.append("<d:propstat><d:prop>");
            builder.append("<d:displayname>").append(escapeXml(entry.displayName())).append("</d:displayname>");
            builder.append("<d:resourcetype>");
            if (entry.collection()) {
                builder.append("<d:collection/>");
            }
            builder.append("</d:resourcetype>");
            builder.append("<d:getcontentlength>").append(entry.size()).append("</d:getcontentlength>");
            if (entry.contentType() != null && !entry.contentType().isBlank()) {
                builder.append("<d:getcontenttype>").append(escapeXml(entry.contentType())).append("</d:getcontenttype>");
            }
            if (entry.creationDate() != null && !entry.creationDate().isBlank()) {
                builder.append("<d:creationdate>").append(escapeXml(entry.creationDate())).append("</d:creationdate>");
            }
            if (entry.modifiedDate() != null && !entry.modifiedDate().isBlank()) {
                builder.append("<d:getlastmodified>").append(escapeXml(entry.modifiedDate())).append("</d:getlastmodified>");
            }
            if (entry.etag() != null && !entry.etag().isBlank()) {
                builder.append("<d:getetag>").append(escapeXml(entry.etag())).append("</d:getetag>");
            }
            builder.append("<d:supportedlock>")
                    .append("<d:lockentry><d:lockscope><d:exclusive/></d:lockscope><d:locktype><d:write/></d:locktype></d:lockentry>")
                    .append("</d:supportedlock>");
            builder.append("<d:lockdiscovery/>");
            builder.append("</d:prop>");
            builder.append("<d:status>HTTP/1.1 200 OK</d:status>");
            builder.append("</d:propstat>");
            builder.append("</d:response>");
        }
        builder.append("</d:multistatus>");
        return builder.toString();
    }

    private String buildLockResponse(String href, String lockToken) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <d:prop xmlns:d="DAV:">
                  <d:lockdiscovery>
                    <d:activelock>
                      <d:locktype><d:write/></d:locktype>
                      <d:lockscope><d:exclusive/></d:lockscope>
                      <d:depth>infinity</d:depth>
                      <d:owner><d:href>%s</d:href></d:owner>
                      <d:timeout>Second-3600</d:timeout>
                      <d:locktoken><d:href>%s</d:href></d:locktoken>
                    </d:activelock>
                  </d:lockdiscovery>
                </d:prop>
                """.formatted(escapeXml(href), escapeXml(lockToken));
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String escapeHtmlAttribute(String value) {
        return escapeXml(value);
    }

    private String formatSize(Long sizeBytes) {
        long value = sizeBytes == null ? 0L : Math.max(0L, sizeBytes);
        if (value < 1024) {
            return value + " B";
        }
        if (value < 1024 * 1024) {
            return (value / 1024) + " KB";
        }
        return (value / (1024 * 1024)) + " MB";
    }

    private record PropFindEntry(String href,
                                 String displayName,
                                 boolean collection,
                                 String contentType,
                                 long size,
                                 String creationDate,
                                 String modifiedDate,
                                 String etag) {
    }

    public record WebDavContext(Long ownerId,
                                Long rootFolderId,
                                boolean allowWrite,
                                String rootDisplayName,
                                String mountBasePath) {
        public static WebDavContext userRoot(Long ownerId) {
            return new WebDavContext(ownerId, null, true, "AGTY/DRIVE", DAV_BASE_PATH);
        }

        public static WebDavContext folder(Long ownerId, Long rootFolderId, boolean allowWrite, String rootDisplayName, String token) {
            return new WebDavContext(ownerId, rootFolderId, allowWrite, rootDisplayName, DAV_SHARE_BASE_PATH + "/" + token);
        }
    }
}
