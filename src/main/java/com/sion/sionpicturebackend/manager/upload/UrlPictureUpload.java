package com.sion.sionpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import com.sion.sionpicturebackend.exception.BusinessException;
import com.sion.sionpicturebackend.exception.ErrorCode;
import com.sion.sionpicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class UrlPictureUpload extends PictureUploadTemplate {

    // 是否为阿里云扩图 - 对象存储
    boolean isAliOSS;


    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;

        // 校验文件地址不能为空
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");

        HttpResponse response = null;
        try {
            // 验证 URL 格式并获取协议
            // 创建一个URL对象，用于解析给定的文件URL
            URL url = new URL(fileUrl);
            // 获取URL的协议部分（如http或https）
            String protocol = url.getProtocol();
            // 检查协议是否为http或https
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                // 如果协议不是http或https，抛出一个业务异常
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持HTTP或HTTPS协议的文件地址");
            }

            // todo 策略模式优化
            // 更精确判断是否为阿里云AI扩图返回链接
            isAliOSS = fileUrl.contains("vigen-invi");

            Method method = isAliOSS ? Method.GET : Method.HEAD;

            response = HttpRequest.of(fileUrl, StandardCharsets.UTF_8)
                    .method(method)
                    .header("User-Agent", "Mozilla/5.0")
                    .timeout(3000)
                    .execute();

            // 校验响应状态码
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                log.error(response.body());
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件不存在或无法访问");
            }

            // 校验文件类型
            String contentType = response.header("Content-Type");
            List<String> allowedContentTypes = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
            if (StrUtil.isNotBlank(contentType) && !allowedContentTypes.contains(contentType)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件格式不支持");
            }

            // 校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                long contentLength = Long.parseLong(contentLengthStr);
                long maxFileSize = 2 * 1024 * 1024L; // 2MB
                // todo 会员可以保存扩图原图

                // 如果是阿里云AI扩图，则限制两倍大小
                if (isAliOSS || contentLength > maxFileSize * 2) {
                    return;
                }
                if (contentLength > maxFileSize) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小超过限制");
                }
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } finally {
            // 关闭响应
            if (response != null) {
                response.close();
            }
        }
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        // 从 URL 中提取文件名
        return FileUtil.mainName(fileUrl);
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;
        // 下载文件到临时目录
        HttpUtil.downloadFile(fileUrl, file);
    }
}
