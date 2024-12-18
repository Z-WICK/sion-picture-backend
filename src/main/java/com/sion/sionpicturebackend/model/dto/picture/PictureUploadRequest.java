package com.sion.sionpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author : wick
 * @Date : 2024/12/16 18:06
 */
@Data
public class PictureUploadRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
