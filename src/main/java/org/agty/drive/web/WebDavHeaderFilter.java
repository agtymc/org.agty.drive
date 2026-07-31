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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

@Component
public class WebDavHeaderFilter extends OncePerRequestFilter {

    private static final String DAV_BASE_PATH = "/dav";
    private static final String DAV_SHARE_BASE_PATH = "/dav-share";
    private static final String ALLOW_HEADER_VALUE = "OPTIONS, PROPFIND, GET, HEAD, PUT, MKCOL, DELETE, MOVE, COPY, LOCK, UNLOCK";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);

        if (!isWebDavRequest(request)) {
            return;
        }

        response.setHeader("DAV", "1, 2");
        response.setHeader("Allow", ALLOW_HEADER_VALUE);
        response.setHeader("Public", ALLOW_HEADER_VALUE);
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setHeader("MS-Author-Via", "DAV");
        }
    }

    private boolean isWebDavRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String path = requestUri.substring(Math.min(requestUri.length(), contextPath.length()));
        String normalizedPath = path.toLowerCase(Locale.ROOT);
        return DAV_BASE_PATH.equals(normalizedPath)
                || normalizedPath.startsWith(DAV_BASE_PATH + "/")
                || DAV_SHARE_BASE_PATH.equals(normalizedPath)
                || normalizedPath.startsWith(DAV_SHARE_BASE_PATH + "/");
    }
}
