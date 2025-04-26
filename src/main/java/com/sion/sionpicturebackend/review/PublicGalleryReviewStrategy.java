package com.sion.sionpicturebackend.review;

import com.sion.sionpicturebackend.model.entity.Picture;
import com.sion.sionpicturebackend.model.entity.User;
import com.sion.sionpicturebackend.model.enums.PictureReviewStatusEnum;
import com.sion.sionpicturebackend.model.enums.UserRoleEnum;

import java.util.Date;
import java.util.Objects;

/**
 * @Author : wick
 * @Date : 2025/4/25 22:27
 */
public class PublicGalleryReviewStrategy implements ReviewStrategy{

    @Override
    public void review(Picture picture, User loginUser) {
        if (Objects.equals(UserRoleEnum.getEnumByValue(loginUser.getUserRole()), UserRoleEnum.ADMIN)) {
            // 管理员自动通过审核
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动通过审核");
            picture.setReviewTime(new Date());
        } else {
            // 普通用户需要审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }
}
