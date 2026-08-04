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

import jakarta.servlet.MultipartConfigElement;
import org.agty.drive.services.StoragePathSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.nio.file.Path;

@Configuration
public class MultipartUploadConfig {

    private final String contentDir;
    private final String uploadTempDir;

    public MultipartUploadConfig(@Value("${storage.content_dir:content}") String contentDir,
                                 @Value("${upload.temp_dir:}") String uploadTempDir) {
        this.contentDir = contentDir;
        this.uploadTempDir = uploadTempDir;
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setLocation(resolveUploadTempDir().toString());
        return factory.createMultipartConfig();
    }

    private Path resolveUploadTempDir() {
        if (StringUtils.hasText(uploadTempDir)) {
            return StoragePathSupport.resolveRootPath(uploadTempDir);
        }
        return StoragePathSupport.resolveRootPath(contentDir).resolve(".upload-staging").normalize();
    }
}
