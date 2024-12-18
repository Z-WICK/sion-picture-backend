package com.sion.sionpicturebackend.model.vo.picture;

import lombok.Data;

import java.util.List;

/**
 * @Author : wick
 * @Date : 2024/12/17 18:25
 */
@Data
public class PictureTagCategory {

    /**
     * 标签列表
     */
    private List<String> tagList;


    /**
     * 分类列表
     */
    private List<String> categoryList;

}
