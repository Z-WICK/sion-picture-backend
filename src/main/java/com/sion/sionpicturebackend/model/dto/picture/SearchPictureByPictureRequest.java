package com.sion.sionpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author : wick
 * @Date : 2025/4/27 22:44
 */
@Data
public class SearchPictureByPictureRequest implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    private static final long serialVersionUID = 1L;
}
