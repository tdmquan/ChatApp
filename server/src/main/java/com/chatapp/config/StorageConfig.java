package com.chatapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * MinIO tương thích S3 API, nên dùng AWS SDK v2 với path-style access.
 * S3Client gọi MinIO qua mạng nội bộ (vd. http://minio:9000), còn S3Presigner ký URL
 * theo endpoint mà trình duyệt truy cập được (vd. http://localhost:9000).
 */
@Configuration
public class StorageConfig {

    @Bean
    S3Client s3Client(AppProperties props) {
        AppProperties.Storage s = props.storage();
        return S3Client.builder()
                .endpointOverride(URI.create(s.endpoint()))
                .region(Region.of(s.region()))
                .credentialsProvider(credentials(s))
                .forcePathStyle(true)
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .build();
    }

    @Bean
    S3Presigner s3Presigner(AppProperties props) {
        AppProperties.Storage s = props.storage();
        return S3Presigner.builder()
                .endpointOverride(URI.create(s.publicEndpoint()))
                .region(Region.of(s.region()))
                .credentialsProvider(credentials(s))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private static StaticCredentialsProvider credentials(AppProperties.Storage s) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(s.accessKey(), s.secretKey()));
    }
}
