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

import jakarta.servlet.http.HttpServletRequest;
import org.agty.utils.AgtyUtils;
import org.springframework.stereotype.Component;

@Component
public class ApplicationInfo {

    private static final String DEFAULT_TITLE = "AGTY/DRIVE";
    private static final String DEFAULT_ABOUT = "Secure file exchange workspace";

    public String getTitle() {
        return LocalConfig.getString("application.title", DEFAULT_TITLE);
    }

    public String getAbout() {
        return LocalConfig.getString("application.about", DEFAULT_ABOUT);
    }

    public String getUri() {
        return normalizeBaseUri(LocalConfig.getString("application.uri", ""));
    }

    public String resolveBaseUri(HttpServletRequest request) {
        String configuredUri = getUri();
        if (AgtyUtils.stringNonNullOrEmpty(configuredUri)) {
            return configuredUri;
        }
        if (request == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(request.getScheme()).append("://").append(request.getServerName());
        boolean standardPort = ("http".equalsIgnoreCase(request.getScheme()) && request.getServerPort() == 80)
                || ("https".equalsIgnoreCase(request.getScheme()) && request.getServerPort() == 443);
        if (!standardPort) {
            builder.append(":").append(request.getServerPort());
        }
        if (AgtyUtils.stringNonNullOrEmpty(request.getContextPath())) {
            builder.append(request.getContextPath());
        }
        return builder.toString();
    }

    public String pageTitle(String pageTitle) {
        if (!AgtyUtils.stringNonNullOrEmpty(pageTitle)) {
            return getTitle();
        }
        return pageTitle + " :: " + getTitle();
    }

    private String normalizeBaseUri(String value) {
        if (!AgtyUtils.stringNonNullOrEmpty(value)) {
            return "";
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
