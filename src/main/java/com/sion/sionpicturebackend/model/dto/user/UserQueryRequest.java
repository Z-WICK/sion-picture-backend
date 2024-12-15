package com.sion.sionpicturebackend.model.dto.user;

import com.sion.sionpicturebackend.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author : wick
 * @Date : 2024/12/11 17:57
 */
@Data
public class UserQueryRequest extends PageRequest implements Serializable {

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
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;


    private static final long serialVersionUID = 1L;
}
