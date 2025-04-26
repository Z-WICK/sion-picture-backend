package com.sion.sionpicturebackend.review;

import com.sion.sionpicturebackend.model.entity.Picture;
import com.sion.sionpicturebackend.model.entity.User;
import com.sion.sionpicturebackend.model.enums.PictureReviewStatusEnum;

import java.util.Date;

/**
 * @Author : wick
 * @Date : 2025/4/25 22:25
 */
public class PrivateSpaceReviewStrategy implements ReviewStrategy{
    @Override
    public void review(Picture picture, User loginUser) {
        // 隐私空间上传，不需要审核
        picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        picture.setReviewerId(loginUser.getId());
        picture.setReviewMessage("隐私空间无需审核，已通过");
        picture.setReviewTime(new Date());
    }
}
