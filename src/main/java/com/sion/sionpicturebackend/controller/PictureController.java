package com.sion.sionpicturebackend.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sion.sionpicturebackend.annotation.AuthCheck;
import com.sion.sionpicturebackend.common.BaseResponse;
import com.sion.sionpicturebackend.common.DeleteRequest;
import com.sion.sionpicturebackend.common.ResultUtils;
import com.sion.sionpicturebackend.constant.UserConstant;
import com.sion.sionpicturebackend.domain.Picture;
import com.sion.sionpicturebackend.domain.User;
import com.sion.sionpicturebackend.exception.BusinessException;
import com.sion.sionpicturebackend.exception.ErrorCode;
import com.sion.sionpicturebackend.exception.ThrowUtils;
import com.sion.sionpicturebackend.model.dto.picture.*;
import com.sion.sionpicturebackend.model.enums.PictureReviewStatusEnum;
import com.sion.sionpicturebackend.model.vo.picture.PictureTagCategory;
import com.sion.sionpicturebackend.model.vo.picture.PictureVO;
import com.sion.sionpicturebackend.service.PictureService;
import com.sion.sionpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @Author : wick
 * @Date : 2024/12/16 23:36
 */
@RestController
@RequestMapping("/picture")
public class PictureController {
    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;


    /**
     * 上传图片
     *
     * @param multipartFile
     * @param pictureUploadRequest
     * @param request
     * @return {@link BaseResponse }<{@link PictureVO }>
     */
    @PostMapping("/upload")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);


    }

    /**
     * 删除图片
     *
     * @param deleteRequest
     * @param request
     * @return {@link BaseResponse }<{@link Boolean }>
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();

        //判断是否存在
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        //仅本人或管理员可删除
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        //操作数据库
        boolean result = pictureService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 更新图片
     *
     * @param pictureUpdateRequest
     * @return {@link BaseResponse }<{@link Boolean }>
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 实体类和DTO进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);

        // 注意将list转为string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));

        // 数据校验
        pictureService.validPicture(picture);

        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        //补充审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fileReviewParams(picture, loginUser);

        //操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);

    }


    /**
     * 根据id获取图片（仅管理员可用）
     *
     * @param id
     * @param request
     * @return {@link BaseResponse }<{@link Picture }>
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "图片id不能<=0");
        //查询数据
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 获取封装类
        return ResultUtils.success(picture);
    }


    /**
     * 根据id获取图片(脱敏版)
     *
     * @param id
     * @param request
     * @return {@link BaseResponse }<{@link PictureVO }>
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "图片id不能<=0");

        //查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        //获取封装类
        return ResultUtils.success(pictureService.getPictureVO(picture, request));

    }


    /**
     * 分页获取图片列表（仅管理员可用）
     *
     * @param pictureQueryRequest
     * @return {@link BaseResponse }<{@link Picture }>
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();

        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));

        return ResultUtils.success(picturePage);

    }

    /**
     * 分页获取图片列表(脱敏版)
     *
     * @param pictureQueryRequest
     * @return {@link BaseResponse }<{@link Page }<{@link PictureVO }>>
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();

        //限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "不允许查询过多数据");
        //普通用户默认只能查看已过审核的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));



        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));

    }

    /**
     * 编辑图片（给用户使用）
     *
     * @param pictureEditRequest
     * @param request
     * @return {@link BaseResponse }<{@link Page }<{@link PictureVO }>>
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 在此处将实体类和DTO进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);

        // 注意将list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));

        //设置编辑时间
        picture.setUpdateTime(new Date());

        //数据校验
        pictureService.validPicture(picture);
        User loginUser = userService.getLoginUser(request);

        //判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        //仅本人或管理员可编辑
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        pictureService.fileReviewParams(picture,loginUser);
        //操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);


    }

    /**
     * 图片分类标签
     *
     * @return {@link BaseResponse }<{@link PictureTagCategory }>
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 审核图片
     *
     * @param pictureReviewRequest
     * @param request
     * @return {@link BaseResponse }<{@link Boolean }>
     */
    @PostMapping("/review")
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {

        // 判断pictureReviewRequest是否为空，如果为空则抛出参数错误异常
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用pictureService的doPictureReview方法，传入pictureReviewRequest和loginUser
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        // 返回成功结果
        return ResultUtils.success(true);


    }
}
