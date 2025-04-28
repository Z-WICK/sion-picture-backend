package com.sion.sionpicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.sion.sionpicturebackend.config.CosClientConfig;
import com.sion.sionpicturebackend.exception.BusinessException;
import com.sion.sionpicturebackend.exception.ErrorCode;
import com.sion.sionpicturebackend.manager.CosManager;
import com.sion.sionpicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * @Author : wick
 * @Date : 2024/12/18 22:35
 */
@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    protected CosManager cosManager;

    @Resource
    protected CosClientConfig cosClientConfig;

    /**
     * 模板方法，定义上传流程
     */
    public final UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1. 校验图片
        validPicture(inputSource);

        // 2. 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originFilename = getOriginFilename(inputSource);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);

        File file = null;
        try {
            // 3. 创建临时文件
            file = File.createTempFile(uploadPath, null);
            // 处理文件来源（本地或 URL）
            processFile(inputSource, file);

            // 4. 上传图片到对象存储
            // 使用cosManager将文件上传到指定的路径uploadPath，并返回上传结果
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 从上传结果中获取图片的原始信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            // 从上传结果中获取图片处理结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            // 从处理结果中获取对象列表
            List<CIObject> objectList = processResults.getObjectList();
            // 检查对象列表是否不为空
            if (CollUtil.isNotEmpty(objectList)) {
                // 获取第一个压缩后的图片对象
                CIObject compressedCiObject = objectList.get(0);

                // 缩略图默认等于压缩图
                CIObject thumbnailCiObject = compressedCiObject;

                // 有生成缩略图，才得到缩略图
                if (objectList.size() > 1) {
                    // 从对象列表中获取索引为1的对象，即第二个对象
                    thumbnailCiObject = objectList.get(1);
                }
                // 封装压缩图返回结果并返回
                return buildResult(originFilename, compressedCiObject, thumbnailCiObject,imageInfo);
            }

            // 5. 封装返回结果
            return buildResult(originFilename, file, uploadPath, imageInfo);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 6. 清理临时文件
            deleteTempFile(file);
        }
    }

    /**
     * 校验输入源（本地文件或 URL）
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

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
    private UploadPictureResult buildResult(String originFilename, File file, String uploadPath, ImageInfo imageInfo) {
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
        return uploadPictureResult;
    }

    /**
     * 删除临时文件
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }

}
