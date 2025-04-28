package com.sion.sionpicturebackend.api.imagesearch.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author wick
 * @date 2025/04/27
 */
@Data
public class ImageSearchResult implements Serializable {

    /**
     * 缩略图地址
     */
    private String thumbUrl;

    /**
     * 来源地址
     */
    private String fromUrl;
}
