package com.sion.sionpicturebackend.model.dto.picture;

import lombok.Data;

/**
 * @Author : wick
 * @Date : 2024/12/20 20:44
 */
@Data
public class PictureUploadByBatchRequest {

    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 每次请求的图片数量
     */
    private Integer count = 10;

    /**
     * 名称前缀
     */
    private String namePrefix;


}
