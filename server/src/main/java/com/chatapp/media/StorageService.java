package com.chatapp.media;

import com.chatapp.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Service
public class StorageService {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final AppProperties.Storage config;

    public StorageService(S3Client s3, S3Presigner presigner, AppProperties props) {
        this.s3 = s3;
        this.presigner = presigner;
        this.config = props.storage();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureBucket() {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(config.bucket()).build());
        } catch (NoSuchBucketException e) {
            s3.createBucket(CreateBucketRequest.builder().bucket(config.bucket()).build());
            log.info("Created bucket {}", config.bucket());
        }
    }

    /** URL để client PUT file trực tiếp lên MinIO. Content-Type và Content-Length bị ký cố định. */
    public URL presignUpload(String key, String contentType, long size) {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(config.bucket())
                .key(key)
                .contentType(contentType)
                .contentLength(size)
                .build();
        return presigner.presignPutObject(r -> r.signatureDuration(config.uploadUrlTtl()).putObjectRequest(put)).url();
    }

    /** @param downloadAs nếu khác null, MinIO trả header Content-Disposition: attachment để trình duyệt tải về */
    public URL presignDownload(String key, String downloadAs) {
        GetObjectRequest.Builder get = GetObjectRequest.builder().bucket(config.bucket()).key(key);
        if (downloadAs != null) {
            get.responseContentDisposition(ContentDisposition.attachment()
                    .filename(downloadAs, StandardCharsets.UTF_8).build().toString());
        }
        return presigner.presignGetObject(r -> r.signatureDuration(config.downloadUrlTtl()).getObjectRequest(get.build())).url();
    }

    public Optional<HeadObjectResponse> head(String key) {
        try {
            return Optional.of(s3.headObject(HeadObjectRequest.builder().bucket(config.bucket()).key(key).build()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public void delete(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(config.bucket()).key(key).build());
        } catch (S3Exception e) {
            log.warn("Failed to delete object {}: {}", key, e.getMessage());
        }
    }
}
