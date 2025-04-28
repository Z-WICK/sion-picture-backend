package com.sion.sionpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author : wick
 * @Date : 2025/4/28 16:09
 */
@Data
public class SearchPictureByColorRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 图片主色调
     */
    private String picColor;
    /**
     * 空间 id
     */
    private Long spaceId;
}
