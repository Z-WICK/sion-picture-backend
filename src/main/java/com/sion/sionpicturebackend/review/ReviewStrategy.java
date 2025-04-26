package com.sion.sionpicturebackend.review;

import com.sion.sionpicturebackend.model.entity.Picture;
import com.sion.sionpicturebackend.model.entity.User;

public interface ReviewStrategy {
    void review(Picture picture, User loginUser);
}
