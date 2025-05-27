package com.sion.sionpicturebackend.manager.upload.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.sion.sionpicturebackend.config.CosClientConfig;
import com.sion.sionpicturebackend.manager.CosManager;
import com.sion.sionpicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;

/**
 * @Author : wick
 * @Date : 2025/5/27 15:28
 */
@Slf4j
@Component
public class TencentCosStrategy implements CosStrategy {

    @Resource
    private CosManager cosManager;


    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 上传图片到 腾讯云
     *
     * @param uploadPath
     * @param file
     * @return {@link UploadPictureResult }
     * @throws Exception
     */
    @Override
    public UploadPictureResult uploadPicture(String originFilename, String uploadPath, File file) throws Exception {

        // 上传图片到对象存储
        // 使用cosManager将文件上传到指定的路径uploadPath，并返回上传结果
        PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
        // 从上传结果中获取图片的原始信息
        ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

        // 从上传结果中获取图片处理结果
        ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
        // 从处理结果中获取对象列表
        List<CIObject> objectList = processResults.getObjectList();
        CIObject compressedCiObject = null;
        CIObject thumbnailCiObject = null;
        // 检查对象列表是否不为空
        if (CollUtil.isNotEmpty(objectList)) {
            // 获取第一个压缩后的图片对象
            compressedCiObject = objectList.get(0);

            // 缩略图默认等于压缩图
            thumbnailCiObject = compressedCiObject;

            // 有生成缩略图，才得到缩略图
            if (objectList.size() > 1) {
                // 从对象列表中获取索引为1的对象，即第二个对象
                thumbnailCiObject = objectList.get(1);
            }
            // 封装压缩图返回结果并返回
            return buildResult(originFilename, compressedCiObject, thumbnailCiObject, imageInfo);

        }

        return buildResult(originFilename, file, uploadPath, imageInfo);
    }

    /**
     * 封装返回结果（腾讯云压缩版）
     *
     * @param originFilename
     * @param compressedCiObject
     * @return {@link UploadPictureResult }
     */
    // 定义一个私有方法buildResult，用于构建上传图片的结果对象
    private UploadPictureResult buildResult(String originFilename,
                                            CIObject compressedCiObject,
                                            CIObject thumbnailCiObject,
                                            ImageInfo imageInfo) {
        // 创建一个UploadPictureResult对象
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        // 获取压缩后的图片宽度
        int picWidth = compressedCiObject.getWidth();
        // 获取压缩后的图片高度
        int picHeight = compressedCiObject.getHeight();
        // 计算图片的宽高比，并保留两位小数
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        // 设置图片名称，使用FileUtil工具类获取文件的主名（去除扩展名）
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        // 设置图片宽度
        uploadPictureResult.setPicWidth(picWidth);
        // 设置图片高度
        uploadPictureResult.setPicHeight(picHeight);
        // 设置图片的宽高比
        uploadPictureResult.setPicScale(picScale);
        // 设置图片的格式
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
        // 设置图片的大小，转换为long类型
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        // 设置图片为压缩后的地址
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        // 设置缩略图
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        // 设置图片的平均颜色值
        uploadPictureResult.setPicColor(imageInfo.getAve());
        uploadPictureResult.setCosTunnel(0);
        return uploadPictureResult;
    }


    /**
     * 封装返回结果
     *
     * @param originFilename
     * @param file
     * @param uploadPath
     * @param imageInfo
     * @return {@link UploadPictureResult }
     */
    private UploadPictureResult buildResult(String originFilename, File file, String uploadPath, ImageInfo
            imageInfo) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setPicColor(imageInfo.getAve());
        uploadPictureResult.setCosTunnel(0);
        return uploadPictureResult;
    }

}


