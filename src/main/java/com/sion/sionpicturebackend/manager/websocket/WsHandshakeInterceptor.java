package com.sion.sionpicturebackend.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import com.sion.sionpicturebackend.auth.SpaceUserAuthManager;
import com.sion.sionpicturebackend.auth.model.SpaceUserPermissionConstant;
import com.sion.sionpicturebackend.model.entity.Picture;
import com.sion.sionpicturebackend.model.entity.Space;
import com.sion.sionpicturebackend.model.entity.User;
import com.sion.sionpicturebackend.model.enums.SpaceTypeEnum;
import com.sion.sionpicturebackend.service.PictureService;
import com.sion.sionpicturebackend.service.SpaceService;
import com.sion.sionpicturebackend.service.UserService;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @Author : wick
 * @Date : 2025/5/14 16:59
 */
@Component
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;


    /**
     * @param request
     * @param response
     * @param wsHandler
     * @param attributes 给WebSocketSession 会话设置属性
     * @return boolean
     */
    @Override
    public boolean beforeHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response, @NotNull WebSocketHandler wsHandler, @NotNull Map<String, Object> attributes) {
        if (request instanceof ServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

            // 从请求中获取图片 id
            String pictureId = servletRequest.getParameter("pictureId");
            if (pictureId == null) {
                log.error("pictureId is null");
                return false;
            }

            // 从请求中获取用户信息
            User loginUser = userService.getLoginUser(servletRequest);
            if (ObjUtil.isEmpty(loginUser)) {
                log.error("用户未登录,拒绝握手");
                return false;
            }

            // 检查用户是否有权限访问该图片
            Picture picture = pictureService.getById(pictureId);
            if (picture == null) {
                log.error("图片不存在,拒绝握手");
                return false;
            }
            Long spaceId = picture.getSpaceId();
            Space space = null;
            if (spaceId != null) {
                space = spaceService.getById(spaceId);
                if (space == null) {
                    log.error("图片所属空间不存在,拒绝握手");
                    return false;
                }
                if (space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()) {
                    log.info("图片所属空间不是团队空间,拒绝握手");
                    return false;
                }
            }

            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.info("用户没有图片编辑权限,拒绝握手");
                return false;
            }

            // 将图片 id 存入 attributes 中，供后续处理使用
            attributes.put("user", loginUser);
            attributes.put("userId", loginUser.getId());
            // 记得转换为 Long 类型
            attributes.put("pictureId", Long.valueOf(pictureId));


        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
