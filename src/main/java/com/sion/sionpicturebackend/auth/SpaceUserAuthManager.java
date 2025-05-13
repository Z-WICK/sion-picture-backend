package com.sion.sionpicturebackend.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.sion.sionpicturebackend.auth.model.SpaceUserAuthConfig;
import com.sion.sionpicturebackend.auth.model.SpaceUserRole;
import com.sion.sionpicturebackend.model.entity.Space;
import com.sion.sionpicturebackend.model.entity.SpaceUser;
import com.sion.sionpicturebackend.model.entity.User;
import com.sion.sionpicturebackend.model.enums.SpaceRoleEnum;
import com.sion.sionpicturebackend.model.enums.SpaceTypeEnum;
import com.sion.sionpicturebackend.service.SpaceUserService;
import com.sion.sionpicturebackend.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author : wick
 * @Date : 2025/5/9 02:20
 */
@Component
public class SpaceUserAuthManager {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }


    /**
     * 根据角色获取权限列表
     *
     * @param spaceUserRole
     * @return {@link List }<{@link String } >
     */
    public List<String > getPermissionsByRole(String spaceUserRole){
        if(StrUtil.isBlank(spaceUserRole)){
            return new ArrayList<>();
        }

        // 找到匹配的角色
        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(r -> spaceUserRole.equals(r.getKey()))
                .findFirst()
                .orElse(null);
        if (role == null) {
            return new ArrayList<>();
        }
        return role.getPermissions();
    }


    /**
     * 获取权限列表
     *
     * @param space
     * @param loginUser
     * @return {@link List }<{@link String }>
     */
    public List<String> getPermissionList(Space space, User loginUser){
        if(loginUser == null){
            return new ArrayList<>();
        }

        // 管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());

        //公共图库
        if(space == null){
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
            return new ArrayList<>();
        }

        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());

        if(spaceTypeEnum == null){
            return new ArrayList<>();
        }

        // 根据空间获取对应的权限
        switch (spaceTypeEnum){
            case PRIVATE:
                // 私有空间，仅本人或管理员有所有权限
                if(space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)){
                    return ADMIN_PERMISSIONS;
                }else {
                    return new ArrayList<>();
                }
            case TEAM:
                // 团队空间，根据用户角色获取权限
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if(spaceUser == null){
                    return new ArrayList<>();
                }else {
                    return getPermissionsByRole(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }

}
