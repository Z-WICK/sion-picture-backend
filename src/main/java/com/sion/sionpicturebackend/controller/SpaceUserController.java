package com.sion.sionpicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.sion.sionpicturebackend.auth.annotation.SaSpaceCheckPermission;
import com.sion.sionpicturebackend.auth.model.SpaceUserPermissionConstant;
import com.sion.sionpicturebackend.common.BaseResponse;
import com.sion.sionpicturebackend.common.DeleteRequest;
import com.sion.sionpicturebackend.common.ResultUtils;
import com.sion.sionpicturebackend.exception.BusinessException;
import com.sion.sionpicturebackend.exception.ErrorCode;
import com.sion.sionpicturebackend.exception.ThrowUtils;
import com.sion.sionpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.sion.sionpicturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.sion.sionpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.sion.sionpicturebackend.model.entity.SpaceUser;
import com.sion.sionpicturebackend.model.entity.User;
import com.sion.sionpicturebackend.model.vo.SpaceUserVO;
import com.sion.sionpicturebackend.service.SpaceUserService;
import com.sion.sionpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @Author : wick
 * @Date : 2025/5/9 01:23
 */
@RestController
@RequestMapping("/spaceUser")
@Slf4j
public class SpaceUserController {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    /**
     * 添加成员到空间
     *
     * @param spaceUserAddRequest
     * @param request
     * @return {@link BaseResponse }<{@link Long }>
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        long id = spaceUserService.addSpaceUser(spaceUserAddRequest);
        return ResultUtils.success(id);

    }

    /**
     * 从空间移除成员
     *
     * @param deleteRequest
     * @param request
     * @return {@link BaseResponse }<{@link Boolean }>
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(
                deleteRequest == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR,
                "请求参数为空"
        );

        Long id = deleteRequest.getId();

        // 判断是否存在
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR, "空间用户不存在");

        // 操作数据库
        boolean result = spaceUserService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "删除失败");
        return ResultUtils.success(true);

    }

    /**
     * 查询某个成员在某个空间的信息
     *
     * @param spaceUserQueryRequest
     * @return {@link BaseResponse }<{@link SpaceUser }>
     */
    @PostMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        // 参数校验
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");

        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();

        ThrowUtils.throwIf(ObjUtil.hasEmpty(spaceId, userId), ErrorCode.NOT_FOUND_ERROR);

        // 查询数据库
        SpaceUser spaceUser = spaceUserService.getOne(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(spaceUser);

    }

    /**
     * 查询成员信息列表
     *
     * @param spaceUserQueryRequest
     * @param request
     * @return {@link BaseResponse }<{@link List }<{@link SpaceUserVO }>>
     */
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest,
                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        List<SpaceUser> spaceUserList = spaceUserService.list(
                spaceUserService.getQueryWrapper(spaceUserQueryRequest));

        List<SpaceUserVO> spaceUserVoList = spaceUserService.getSpaceUserVoList(spaceUserList);
        return ResultUtils.success(spaceUserVoList);

    }

    /**
     * 编辑成员信息（设置权限）
     *
     * @param spaceUserEditRequest
     * @param request
     * @return {@link BaseResponse }<{@link Boolean }>
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest,
                                               HttpServletRequest request) {
        if (spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 将实体类 和 DTO 进行转换
        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserEditRequest, spaceUser);

        // 数据校验
        spaceUserService.validSpaceUser(spaceUser, false);

        // 判断是否存在
        long id = spaceUserEditRequest.getId();
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);

        // 操作数据
        boolean result = spaceUserService.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);

    }

    /**
     * 查询我加入的团队空间列表
     *
     * @param request
     * @return {@link BaseResponse }<{@link List }<{@link SpaceUserVO }>>
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(loginUser.getId());
        List<SpaceUser> spaceUserList = spaceUserService.list(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        return ResultUtils.success(spaceUserService.getSpaceUserVoList(spaceUserList));

    }
}
