package com.sion.sionpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sion.sionpicturebackend.model.entity.User;
import com.sion.sionpicturebackend.model.dto.user.UserQueryRequest;
import com.sion.sionpicturebackend.model.vo.user.LoginUserVO;
import com.sion.sionpicturebackend.model.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author wick
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2024-12-10 17:26:19
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     * @param userAccount
     * @param userPassword
     * @param request
     * @return {@link LoginUserVO }
     */
    LoginUserVO userLogin(String userAccount,String userPassword,HttpServletRequest request);

    /**
     * 用户注销
     * @param request
     * @return boolean
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * @param userPassword
     * @return {@link String }
     * @description 加密密码
     */
    String getEncryptPassword(String userPassword);


    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);


    /**
     * 获取当前登录用户信息(脱敏版）
     *
     * @param user
     * @return {@link LoginUserVO }
     */
    LoginUserVO getLoginUserVO(User user);


    /**
     * 根据用户信息获取用户VO
     *
     * @param user
     * @return {@link UserVO }
     */
    UserVO getUserVO(User user);

    /**
     *  根据用户列表获取用户VO列表
     *
     * @param userList
     * @return {@link List }<{@link UserVO }>
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     *
     *
     * @param userQueryRequest
     * @return {@link QueryWrapper }<{@link User }>
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);


    /**
     * 是否为管理员
     *
     * @param user
     * @return boolean
     */
    boolean isAdmin(User user);
}
