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

package org.agty.drive.services;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MimeTypePolicyServiceTest {

    private final MimeTypePolicyService mimeTypePolicyService = new MimeTypePolicyService();

    @Test
    void shouldFallbackBlockedUploadMimeTypeByExtension() {
        assertEquals("text/plain", mimeTypePolicyService.normalizeUploadedMimeType("text/html", "txt"));
    }

    @Test
    void shouldFallbackUnknownUploadMimeTypeToOctetStream() {
        assertEquals(MediaType.APPLICATION_OCTET_STREAM_VALUE,
                mimeTypePolicyService.normalizeUploadedMimeType("badmime", "bin"));
    }

    @Test
    void shouldResolveBlockedResponseMimeTypeAsOctetStream() {
        assertEquals(MediaType.APPLICATION_OCTET_STREAM,
                mimeTypePolicyService.resolveResponseMediaType("image/svg+xml"));
    }

    @Test
    void shouldResolveValidResponseMimeType() {
        assertEquals(MediaType.TEXT_PLAIN, mimeTypePolicyService.resolveResponseMediaType("text/plain; charset=UTF-8"));
    }
}
