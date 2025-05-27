package com.sion.sionpicturebackend.manager.upload.strategy;

import com.sion.sionpicturebackend.model.dto.file.UploadPictureResult;

import java.io.File;

public interface CosStrategy {
    /**
     * 上传图片
     *
     * @param uploadPath
     * @param file
     * @return
     * @throws Exception
     */

     UploadPictureResult uploadPicture(String originFilename, String uploadPath, File file) throws Exception;
}
