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
