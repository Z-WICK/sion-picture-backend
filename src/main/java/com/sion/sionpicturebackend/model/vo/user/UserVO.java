package com.sion.sionpicturebackend.model.vo.user;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author : wick
 * @Date : 2024/12/11 18:09
 */
@Data
public class UserVO implements Serializable {

    /**
     * id
     */
    private Long id;

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


    /**
     * 创建时间
     */
    private Date createTime;


    private static final long serialVersionUID = 1L;
}