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

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class MediaResponseSupport {

    private MediaResponseSupport() {
    }

    public static ResponseEntity<byte[]> buildResponse(byte[] content,
                                                       MediaType mediaType,
                                                       String filename,
                                                       boolean inline,
                                                       String rangeHeader) {
        if (content == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.setContentDisposition((inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(filename, StandardCharsets.UTF_8)
                .build());

        Range range = parseRange(rangeHeader, content.length);
        if (range == null) {
            headers.setContentLength(content.length);
            return new ResponseEntity<>(content, headers, HttpStatus.OK);
        }

        if (!range.valid()) {
            headers.set(HttpHeaders.CONTENT_RANGE, "bytes */" + content.length);
            return new ResponseEntity<>(headers, HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        }

        byte[] partialContent = Arrays.copyOfRange(content, range.start(), range.end() + 1);
        headers.setContentLength(partialContent.length);
        headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + range.start() + "-" + range.end() + "/" + content.length);
        return new ResponseEntity<>(partialContent, headers, HttpStatus.PARTIAL_CONTENT);
    }

    public static ResponseEntity<Resource> buildPathResponse(Path path,
                                                             MediaType mediaType,
                                                             String filename,
                                                             boolean inline,
                                                             String rangeHeader) {
        return buildPathResponse(path, mediaType, filename, inline, rangeHeader, false);
    }

    public static ResponseEntity<Resource> buildEphemeralPathResponse(Path path,
                                                                      MediaType mediaType,
                                                                      String filename,
                                                                      boolean inline,
                                                                      String rangeHeader) {
        return buildPathResponse(path, mediaType, filename, inline, rangeHeader, true);
    }

    private static ResponseEntity<Resource> buildPathResponse(Path path,
                                                              MediaType mediaType,
                                                              String filename,
                                                              boolean inline,
                                                              String rangeHeader,
                                                              boolean deleteOnClose) {
        if (path == null || !Files.exists(path) || !Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }

        long contentLength;
        try {
            contentLength = Files.size(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to determine content length for " + path, e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.setContentDisposition((inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(filename, StandardCharsets.UTF_8)
                .build());

        Range range = parseRange(rangeHeader, Math.toIntExact(Math.min(Integer.MAX_VALUE, contentLength)));
        if (range == null) {
            headers.setContentLength(contentLength);
            if (!deleteOnClose) {
                return new ResponseEntity<>(new FileSystemResource(path), headers, HttpStatus.OK);
            }
            return new ResponseEntity<>(new InputStreamResource(openAutoDeletingStream(path)), headers, HttpStatus.OK);
        }

        if (!range.valid()) {
            headers.set(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength);
            return new ResponseEntity<>(headers, HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        }

        long rangeLength = (long) range.end() - range.start() + 1L;
        headers.setContentLength(rangeLength);
        headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + range.start() + "-" + range.end() + "/" + contentLength);
        return new ResponseEntity<>(
                new InputStreamResource(openRangeStream(path, range.start(), rangeLength, deleteOnClose)),
                headers,
                HttpStatus.PARTIAL_CONTENT
        );
    }

    private static InputStream openRangeStream(Path path, long offset, long length) {
        return openRangeStream(path, offset, length, false);
    }

    private static InputStream openRangeStream(Path path, long offset, long length, boolean deleteOnClose) {
        try {
            InputStream inputStream = Files.newInputStream(path);
            if (offset > 0) {
                inputStream.skipNBytes(offset);
            }
            InputStream limited = new LimitedInputStream(inputStream, length);
            return deleteOnClose ? new DeletingInputStream(limited, path) : limited;
        } catch (IOException e) {
            throw new RuntimeException("Failed to open ranged stream for " + path, e);
        }
    }

    private static InputStream openAutoDeletingStream(Path path) {
        try {
            return new DeletingInputStream(Files.newInputStream(path), path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open stream for " + path, e);
        }
    }

    private static Range parseRange(String rangeHeader, int contentLength) {
        if (rangeHeader == null || rangeHeader.isBlank() || !rangeHeader.startsWith("bytes=")) {
            return null;
        }

        String value = rangeHeader.substring(6).trim();
        int commaIndex = value.indexOf(',');
        if (commaIndex >= 0) {
            value = value.substring(0, commaIndex).trim();
        }

        int dashIndex = value.indexOf('-');
        if (dashIndex < 0) {
            return Range.invalid();
        }

        String startPart = value.substring(0, dashIndex).trim();
        String endPart = value.substring(dashIndex + 1).trim();

        try {
            long start;
            long end;

            if (startPart.isEmpty()) {
                long suffixLength = Long.parseLong(endPart);
                if (suffixLength <= 0) {
                    return Range.invalid();
                }
                start = Math.max(0, contentLength - suffixLength);
                end = contentLength - 1L;
            } else {
                start = Long.parseLong(startPart);
                end = endPart.isEmpty() ? contentLength - 1L : Long.parseLong(endPart);
            }

            if (start < 0 || start >= contentLength) {
                return Range.invalid();
            }

            end = Math.min(end, contentLength - 1L);
            if (end < start) {
                return Range.invalid();
            }

            return new Range((int) start, (int) end, true);
        } catch (NumberFormatException exception) {
            return Range.invalid();
        }
    }

    private record Range(int start, int end, boolean valid) {
        private static Range invalid() {
            return new Range(0, 0, false);
        }
    }

    private static final class LimitedInputStream extends InputStream {
        private final InputStream delegate;
        private long remaining;

        private LimitedInputStream(InputStream delegate, long remaining) {
            this.delegate = delegate;
            this.remaining = Math.max(0L, remaining);
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int value = delegate.read();
            if (value >= 0) {
                remaining--;
            }
            return value;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int read = delegate.read(b, off, (int) Math.min(len, remaining));
            if (read > 0) {
                remaining -= read;
            }
            return read;
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

    private static final class DeletingInputStream extends InputStream {
        private final InputStream delegate;
        private final Path path;

        private DeletingInputStream(InputStream delegate, Path path) {
            this.delegate = delegate;
            this.path = path;
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return delegate.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            try {
                delegate.close();
            } finally {
                Files.deleteIfExists(path);
            }
        }
    }
}
