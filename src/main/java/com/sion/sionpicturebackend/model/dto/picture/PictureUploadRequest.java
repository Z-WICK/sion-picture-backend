package com.sion.sionpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author : wick
 * @Date : 2024/12/16 18:06
 */
@Data
public class PictureUploadRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;
    /**
     * 文件地址
     */
    private String fileUrl;
    /**
     * 图片名称
     */
    private String picName;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 图片主色调
     */
    private String picColor;


}
