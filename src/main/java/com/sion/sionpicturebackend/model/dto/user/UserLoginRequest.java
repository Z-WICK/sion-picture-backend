package com.sion.sionpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author : wick
 * @Date : 2024/12/11 16:23
 */
@Data
public class UserLoginRequest implements Serializable {
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;
}
