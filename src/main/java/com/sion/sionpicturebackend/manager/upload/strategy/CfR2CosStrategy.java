package com.sion.sionpicturebackend.manager.upload.strategy;

import cn.hutool.core.io.FileUtil;
import com.sion.sionpicturebackend.exception.BusinessException;
import com.sion.sionpicturebackend.exception.ErrorCode;
import com.sion.sionpicturebackend.manager.R2CosManager;
import com.sion.sionpicturebackend.model.dto.file.UploadPictureResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import javax.annotation.Resource;
import java.io.File;

/**
 * @Author : wick
 * @Date : 2025/5/27 15:28
 */
@Component
public class CfR2CosStrategy implements CosStrategy {

    @Resource
    private R2CosManager r2CosManager;

    @Value("${r2.client.public-domain}")
    private String publicDomain;


    /**
     * 上传图片到 cloudflare r2
     *
     * @param uploadPath
     * @param file
     * @return {@link UploadPictureResult }
     * @throws Exception
     */
    @Override
    public UploadPictureResult uploadPicture(String originFilename, String uploadPath, File file) throws Exception {

        // 上传图片到对象存储
        PutObjectResponse putObjectResponse = r2CosManager.putObjectR2(uploadPath, file);
        if (putObjectResponse == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传失败");
        }
        // todo 自行实现图片解析信息


        return buildResult(originFilename, uploadPath);
    }


    /**
     * 封装结果信息
     *
     * @return {@link UploadPictureResult }
     */
    public UploadPictureResult buildResult(String originFilename, String uploadPath) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        String resultUrl = String.format("%s%s", publicDomain, uploadPath);
        uploadPictureResult.setUrl(resultUrl);
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        // 默认缩略图就是原图，毕竟不用出口流量也无所谓
        uploadPictureResult.setThumbnailUrl(resultUrl);
        uploadPictureResult.setCosTunnel(1);
        return uploadPictureResult;
    }
}
