package com.sion.sionpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author : wick
 * @Date : 2024/12/11 17:58
 */
@Data
public class UserUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;


    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}
