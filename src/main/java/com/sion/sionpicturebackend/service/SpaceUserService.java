package com.sion.sionpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sion.sionpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.sion.sionpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.sion.sionpicturebackend.model.entity.SpaceUser;
import com.sion.sionpicturebackend.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author wick
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-05-08 19:25:24
*/
public interface SpaceUserService extends IService<SpaceUser> {

    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    void validSpaceUser(SpaceUser spaceUser, boolean add);

    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    SpaceUserVO getSpaceUserVo(SpaceUser spaceUser, HttpServletRequest request);

    List<SpaceUserVO> getSpaceUserVoList(List<SpaceUser> spaceUserList);
}
