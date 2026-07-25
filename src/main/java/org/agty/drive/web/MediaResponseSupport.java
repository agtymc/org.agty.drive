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
            return new ResponseEntity<>(new FileSystemResource(path), headers, HttpStatus.OK);
        }

        if (!range.valid()) {
            headers.set(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength);
            return new ResponseEntity<>(headers, HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        }

        long rangeLength = (long) range.end() - range.start() + 1L;
        headers.setContentLength(rangeLength);
        headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + range.start() + "-" + range.end() + "/" + contentLength);
        return new ResponseEntity<>(new InputStreamResource(openRangeStream(path, range.start(), rangeLength)), headers, HttpStatus.PARTIAL_CONTENT);
    }

    private static InputStream openRangeStream(Path path, long offset, long length) {
        try {
            InputStream inputStream = Files.newInputStream(path);
            if (offset > 0) {
                inputStream.skipNBytes(offset);
            }
            return new LimitedInputStream(inputStream, length);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open ranged stream for " + path, e);
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
}
