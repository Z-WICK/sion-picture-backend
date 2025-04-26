package com.sion.sionpicturebackend.review;

import com.sion.sionpicturebackend.model.entity.Picture;
import com.sion.sionpicturebackend.model.entity.User;

/**
 * @Author : wick
 * @Date : 2025/4/25 22:31
 */
public class PictureReviewExecutor {
    private  ReviewStrategy reviewStrategy;

    public PictureReviewExecutor(ReviewStrategy reviewStrategy) {
        this.reviewStrategy = reviewStrategy;
    }

    public void executeReview(Picture picture, User loginUser) {
        reviewStrategy.review(picture, loginUser);
    }
}
