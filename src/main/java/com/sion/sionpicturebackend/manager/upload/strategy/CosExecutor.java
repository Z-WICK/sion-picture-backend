package com.sion.sionpicturebackend.manager.upload.strategy;

import com.sion.sionpicturebackend.model.dto.file.UploadPictureResult;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.File;

@Data
@Component
public class CosExecutor {
    private CosStrategy strategy;

    public UploadPictureResult uploadPicture(String originFilename, String uploadPath, File file) throws Exception {
        return strategy.uploadPicture(originFilename, uploadPath, file);
    }
}