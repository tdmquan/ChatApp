package com.chatapp.media;

import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Object key → đường dẫn tương đối mà client dùng để tải file.
 * Endpoint /api/files/** kiểm tra quyền rồi redirect sang presigned URL của MinIO.
 */
public final class FileUrls {

    public static final String PREFIX = "/api/files/";

    private FileUrls() {
    }

    public static String of(String objectKey) {
        if (objectKey == null) {
            return null;
        }
        return PREFIX + Arrays.stream(objectKey.split("/"))
                .map(segment -> UriUtils.encodePathSegment(segment, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));
    }
}
