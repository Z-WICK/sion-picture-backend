package com.sion.sionpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import com.sion.sionpicturebackend.config.R2ClientConfig;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.annotation.Resource;
import java.io.File;
import java.util.Map;

/**
 * @Author : wick
 * @Date : 2024/05/11 21:00
 */
@Component
public class R2CosManager {

    @Resource
    private R2ClientConfig r2ClientConfig;

    @Resource
    private S3Client s3Client;

    // ... 一些操作 R2 的方法

    /**
     * 上传对象r2
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResponse putObjectR2(String key, File file) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(r2ClientConfig.getBucketName())
                .key(key)
                .contentType(FileUtil.getMimeType(file.getName()))
                .build();

        return s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
    }

    /**
     * 下载对象r2
     *
     * @param key 唯一键
     */
    public ResponseInputStream<GetObjectResponse> getObjectR2(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(r2ClientConfig.getBucketName())
                .key(key)
                .build();

        return s3Client.getObject(getObjectRequest);
    }

    /**
     * 上传对象（附带元数据）r2
     *
     * @param key  唯一键（上传文件的路径）
     * @param file 文件
     * @param metadata 元数据
     */
    public PutObjectResponse putObjectWithMetadataR2(String key, File file, Map<String, String> metadata) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(r2ClientConfig.getBucketName())
                .key(key)
                .contentType(FileUtil.getMimeType(file.getName()))
                .metadata(metadata)
                .build();

        return s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
    }

    /**
     * 上传图片对象r2
     * 注意：R2不支持腾讯云COS的图片处理功能，这里只是上传图片
     * 如需图片处理功能，需要在应用层实现或使用第三方服务
     *
     * @param key  唯一键（上传文件的路径）
     * @param file 文件
     */
    public PutObjectResponse putPictureObjectR2(String key, File file) {
        // 由于R2不支持像腾讯云COS那样的图片处理，这里只是普通上传
        // 如果需要生成缩略图等功能，需要在上传前或上传后单独处理

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(r2ClientConfig.getBucketName())
                .key(key)
                .contentType(FileUtil.getMimeType(file.getName()))
                .build();

        return s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
    }

    /**
     * 删除对象r2
     *
     * @param key 文件 key
     */
    public DeleteObjectResponse deleteObjectR2(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(r2ClientConfig.getBucketName())
                .key(key)
                .build();

        return s3Client.deleteObject(deleteObjectRequest);
    }

    /**
     * 检查对象是否存在r2
     *
     * @param key 唯一键
     * @return 是否存在
     */
    public boolean doesObjectExistR2(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(r2ClientConfig.getBucketName())
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    /**
     * 获取文件元数据r2
     *
     * @param key 唯一键
     * @return 对象头信息
     */
    public HeadObjectResponse getObjectMetadataR2(String key) {
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(r2ClientConfig.getBucketName())
                .key(key)
                .build();

        return s3Client.headObject(headObjectRequest);
    }



    /**
     * 复制对象r2
     *
     * @param sourceKey 源对象键
     * @param destinationKey 目标对象键
     * @return 复制结果
     */
    public CopyObjectResponse copyObjectR2(String sourceKey, String destinationKey) {
        String sourceBucket = r2ClientConfig.getBucketName();

        CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                .sourceBucket(sourceBucket)
                .sourceKey(sourceKey)
                .destinationBucket(sourceBucket)
                .destinationKey(destinationKey)
                .build();

        return s3Client.copyObject(copyObjectRequest);
    }

    /**
     * 上传字符串内容r2
     *
     * @param key 唯一键
     * @param content 字符串内容
     * @param contentType 内容类型
     * @return 上传结果
     */
    public PutObjectResponse putStringR2(String key, String content, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(r2ClientConfig.getBucketName())
                .key(key)
                .contentType(contentType)
                .build();

        return s3Client.putObject(putObjectRequest, RequestBody.fromString(content));
    }
}