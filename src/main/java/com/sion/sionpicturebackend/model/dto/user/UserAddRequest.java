package com.sion.sionpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author : wick
 * @Date : 2024/12/11 17:57
 */
@Data
public class UserAddRequest implements Serializable {


    /**
     * 账号
     */
    private String userAccount;


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
