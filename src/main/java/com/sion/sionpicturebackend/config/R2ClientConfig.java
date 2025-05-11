package com.sion.sionpicturebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Data
@Configuration
@ConfigurationProperties(prefix = "r2.client")
public class R2ClientConfig {

    // 访问密钥ID
    private String accessKeyId;
    // 秘密访问密钥
    private String secretAccessKey;
    // 端点
    private String endpoint;
    // 桶名
    private String bucketName;
    // 区域
    private String region;
    // 公开域名，用于构建文件访问URL
    private String publicDomain;

    @Bean
    public S3Client s3Client() {
        // 配置 AWS 凭证
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

        // 创建并配置 S3Client
        return S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))  // Cloudflare R2 的自定义 endpoint
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }
}