package com.sion.sionpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sion.sionpicturebackend.model.dto.picture.*;
import com.sion.sionpicturebackend.model.entity.Picture;
import com.sion.sionpicturebackend.model.entity.User;
import com.sion.sionpicturebackend.model.vo.picture.PictureVO;
import org.springframework.scheduling.annotation.Async;

import javax.servlet.http.HttpServletRequest;

/**
 * @author wick
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2024-12-16 17:41:47
 */
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return {@link PictureVO }
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser
    );

    /**
     * 构建查询条件，查询图片
     *
     * @param pictureQueryRequest
     * @return {@link QueryWrapper }<{@link Picture }>
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 编写获取图片封装的方法，
     * 可以为原有的图片关联创建用户的信息。
     *
     * @param picture
     * @param request
     * @return {@link PictureVO }
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 图片分页
     *
     * @param picturePage
     * @param request
     * @return {@link Page }<{@link PictureVO }>
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 校验图片
     *
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);


    /**
     * 审核员填充参数
     * 自动过审核
     *
     * @param picture
     * @param loginUser
     */
    void fileReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return {@link Integer }
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);


    /**
     * 清理图片
     * @param oldPicture
     */
    @Async
    void clearPictureFile(Picture oldPicture);

    /**
     * 删除图片
     *
     * @param pictureId
     * @param loginUser
     */
    void deletePicture(long pictureId, User loginUser);

    /**
     * 更新图片
     *
     * @param pictureEditRequest
     * @param loginUser
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 检查图片权限（谁可以看到）
     *
     * @param loginUser
     * @param picture
     */
    void checkPictureAuth(User loginUser, Picture picture);
}
