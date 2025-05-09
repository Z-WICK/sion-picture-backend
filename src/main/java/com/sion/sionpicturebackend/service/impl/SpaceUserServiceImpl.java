package com.sion.sionpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sion.sionpicturebackend.exception.ErrorCode;
import com.sion.sionpicturebackend.exception.ThrowUtils;
import com.sion.sionpicturebackend.mapper.SpaceUserMapper;
import com.sion.sionpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.sion.sionpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.sion.sionpicturebackend.model.entity.Space;
import com.sion.sionpicturebackend.model.entity.SpaceUser;
import com.sion.sionpicturebackend.model.entity.User;
import com.sion.sionpicturebackend.model.enums.SpaceRoleEnum;
import com.sion.sionpicturebackend.model.vo.SpaceUserVO;
import com.sion.sionpicturebackend.model.vo.space.SpaceVO;
import com.sion.sionpicturebackend.model.vo.user.UserVO;
import com.sion.sionpicturebackend.service.SpaceService;
import com.sion.sionpicturebackend.service.SpaceUserService;
import com.sion.sionpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author wick
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2025-05-08 19:25:24
 */
@Service
@Slf4j
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserService {

    @Resource
    private  UserService userService;

    @Resource
    @Lazy
    private SpaceService spaceService;

    /**
     * 添加空间成员
     *
     * @param spaceUserAddRequest
     * @return long
     */
    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.NOT_FOUND_ERROR, "请求参数为空");
        SpaceUser spaceUser = new SpaceUser();

        BeanUtils.copyProperties(spaceUserAddRequest, spaceUser);
        validSpaceUser(spaceUser, true);

        // 数据库操作
        boolean result = this.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "添加失败");
        return spaceUser.getId();
    }

    /**
     * 验证空间角色
     *
     * @param spaceUser
     * @param add
     */
    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR, "用户不存在");

        // 创建时，空间 ID 和用户 ID 必填
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();

        if (add) {
            ThrowUtils.throwIf(ObjUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR, "空间 ID 和用户 ID 不能为空");

            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户不存在");

            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");

        }
        // 检验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        ThrowUtils.throwIf(
                spaceRole != null && spaceRoleEnum == null,
                ErrorCode.NOT_FOUND_ERROR,
                "空间角色不存在"
        );
    }

    /**
     * 查询空间用户
     *
     * @param spaceUserQueryRequest
     * @return {@link QueryWrapper }<{@link SpaceUser }>
     */
    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest){
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();

        if(spaceUserQueryRequest == null){
            return queryWrapper;
        }

        // 从对象中取值
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();

        queryWrapper.eq(ObjUtil.isNotEmpty(id),"id",id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId),"spaceId",spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId),"userId",userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole),"spaceRole",spaceRole);

        return queryWrapper;
    }

    /**
     * 获取空间成员封装类(单个)
     *
     * @param spaceUser
     * @param request
     * @return {@link SpaceUserVO }
     */
    @Override
    public SpaceUserVO getSpaceUserVo(SpaceUser spaceUser, HttpServletRequest request){
        // 对象转封装类
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);

        // 关联查询用户信息
        Long userId = spaceUser.getUserId();
        if(userId != null && userId > 0){
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceUserVO.setUser(userVO);
        }

        // 关联查询空间信息
        Long spaceId = spaceUser.getSpaceId();
        if(spaceId != null && spaceId > 0){
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
            spaceUserVO.setSpace(spaceVO);
        }
        return spaceUserVO;
    }

    /**
     * 获取空间成员封装类(列表)
     *
     * @param spaceUserList
     * @return {@link List }<{@link SpaceUserVO }>
     */
    @Override
    public List<SpaceUserVO> getSpaceUserVoList(List<SpaceUser> spaceUserList){
        // 判断输入列表是否为空
        if(CollUtil.isEmpty(spaceUserList)){
            return Collections.emptyList();
        }

        // 对象列表 =》 封装对象列表
        List<SpaceUserVO> spaceUserVOList = spaceUserList
                .stream()
                .map(SpaceUserVO::objToVo)
                .collect(Collectors.toList());

        // 1.收集需要关联查询的用户 ID 和 空间 ID
        Set<Long> userIdSet = spaceUserList
                .stream()
                .map(SpaceUser::getUserId)
                .collect(Collectors.toSet());

        Set<Long> spaceIdSet = spaceUserList
                .stream()
                .map(SpaceUser::getSpaceId)
                .collect(Collectors.toSet());

        // 2. 批量查询用户和空间
        // 根据用户ID集合，获取用户列表，并将用户列表按照用户ID进行分组
        Map<Long,List<User>> userIdUserListMap = userService
                .listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        // 根据spaceIdSet中的id集合，从spaceService中获取对应的space列表
        Map<Long,List<Space>> spaceIdSpaceListMap = spaceService
                .listByIds(spaceIdSet)
                // 将space列表转换为流
                .stream()
                // 将流中的元素按照space的id进行分组
                .collect(Collectors.groupingBy(Space::getId));

        // 3. 封装 SpaceUserVo 的用户和空间信息
        spaceUserVOList.forEach(spaceUserVO -> {
            // 获取用户ID
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();

            // 填充用户信息
            User user = null;

            if(userIdUserListMap.containsKey(userId)){
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceUserVO.setUser(userService.getUserVO(user));

            // 填充空间信息
            Space space = null;
            if(spaceIdSpaceListMap.containsKey(spaceId)){
                space = spaceIdSpaceListMap.get(spaceId).get(0);
            }
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
        });
        return spaceUserVOList;

    }

}




