package com.sion.sionpicturebackend.controller;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.sion.sionpicturebackend.annotation.AuthCheck;
import com.sion.sionpicturebackend.common.BaseResponse;
import com.sion.sionpicturebackend.common.ResultUtils;
import com.sion.sionpicturebackend.constant.UserConstant;
import com.sion.sionpicturebackend.exception.BusinessException;
import com.sion.sionpicturebackend.exception.ErrorCode;
import com.sion.sionpicturebackend.manager.CosManager;
import com.sion.sionpicturebackend.manager.R2CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * @Author : wick
 * @Date : 2024/12/15 20:40
 */
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private CosManager cosManager;

    @Resource
    private R2CosManager r2CosManager;


    /**
     * 测试文件上传
     *
     * @param multipartFile
     * @return {@link BaseResponse }<{@link String }>
     */
    @PostMapping("/test/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        //文件目录
        String filename = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/%s", filename);
        File file = null;

        try {
            // 上传文件

            // 腾讯云需要file对象，在这里做一步转换
            //创建一个临时空文件
            file = File.createTempFile(filepath, null);
            //前端传来的文件写入这个临时文件
            multipartFile.transferTo(file);
            cosManager.putObject(filepath, file);
            // 返回可访问地址
            return ResultUtils.success(filepath);

        } catch (IOException e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }


        }
    }

    /**
     * 测试文件上传 r2
     *
     * @param multipartFile
     * @return {@link BaseResponse }<{@link String }>
     */
    @PostMapping("/test/r2/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> testUploadFileR2(@RequestPart("file") MultipartFile multipartFile) {
        //文件目录
        String filename = multipartFile.getOriginalFilename();
        String filepath = String.format("test/%s", filename);
        File file = null;

        try {
            // 上传文件

            // 腾讯云需要file对象，在这里做一步转换
            //创建一个临时空文件
            file = File.createTempFile(filepath, null);
            //前端传来的文件写入这个临时文件
            multipartFile.transferTo(file);
            r2CosManager.putObjectR2(filepath, file);
            // 返回可访问地址
            return ResultUtils.success(filepath);

        } catch (IOException e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }


        }
    }

    /**
     * 测试文件下载
     *
     * @param filepath 文件路径
     * @param response 响应对象
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            // 处理下载到的流
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }

        /**
     * 测试文件下载 r2
     *
     * @param filepath 文件路径
     * @param response 响应对象
     */
    /**
     * 测试文件下载 R2
     *
     * @param filepath 文件路径
     * @param response HttpServletResponse
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/r2/download")
    public void testDownloadFileR2(@RequestParam String filepath, HttpServletResponse response) throws IOException {
        // 获取 R2 对象流
        ResponseInputStream<GetObjectResponse> objectR2 = null;
        try {
            objectR2 = r2CosManager.getObjectR2(filepath); // 从 R2 获取对象
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);

            // 使用输入流将 R2 对象流直接写入响应输出流
            IOUtils.copy(objectR2, response.getOutputStream());
            response.getOutputStream().flush(); // 确保所有数据写入响应流

        } catch (Exception e) {
            log.error("File download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (objectR2 != null) {
                objectR2.close(); // 关闭流
            }
        }
    }


}
