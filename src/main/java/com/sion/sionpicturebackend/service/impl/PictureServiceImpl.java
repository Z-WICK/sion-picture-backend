package com.sion.sionpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sion.sionpicturebackend.domain.Picture;
import com.sion.sionpicturebackend.domain.User;
import com.sion.sionpicturebackend.exception.BusinessException;
import com.sion.sionpicturebackend.exception.ErrorCode;
import com.sion.sionpicturebackend.exception.ThrowUtils;
import com.sion.sionpicturebackend.manager.FileManager;
import com.sion.sionpicturebackend.mapper.PictureMapper;
import com.sion.sionpicturebackend.model.dto.file.UploadPictureResult;
import com.sion.sionpicturebackend.model.dto.picture.PictureQueryRequest;
import com.sion.sionpicturebackend.model.dto.picture.PictureReviewRequest;
import com.sion.sionpicturebackend.model.dto.picture.PictureUploadRequest;
import com.sion.sionpicturebackend.model.enums.PictureReviewStatusEnum;
import com.sion.sionpicturebackend.model.vo.picture.PictureVO;
import com.sion.sionpicturebackend.model.vo.user.UserVO;
import com.sion.sionpicturebackend.service.PictureService;
import com.sion.sionpicturebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author wick
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2024-12-16 17:41:47
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    private FileManager fileManager;
    @Resource
    private UserService userService;



    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 用于判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }


        // 如果是更新图片，需要检验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture==null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

            // 仅本人或管理员可更新
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"仅本人或管理员可更新");
            }
        }

        // 上传图片，得到信息
        // 按照用户id划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);

        //构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl("https://" + uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());


        //如果 pictureId 不为空，则更新图片信息，反之新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和 编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "图片上传失败");
        return PictureVO.objToVo(picture);
    }


    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest != null) {
            return queryWrapper;
        }

        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();

        // 拼接查询条件

        if (StrUtil.isNotBlank(searchText)) {
            //需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText));

        }

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);

        //JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            //遍历tags数组
            for (String tag : tags) {
                //将tag添加到查询条件中
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }

        //排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;

    }


    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        //对象封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);

        //关联查询用户信息
        Long userId = picture.getUserId();
        if (ObjUtil.isNotEmpty(userId) && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }


    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        // 获取图片列表
        List<Picture> pictureList = picturePage.getRecords();
        // 创建图片VO分页对象
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(),
                picturePage.getSize(),
                picturePage.getTotal()
        );

        // 判断pictureList是否为空
        if (CollUtil.isEmpty(pictureList)) {
            // 如果pictureList为空，则直接返回pictureVOPage
            return pictureVOPage;
        }

        /**
         * 注意，这里我们做了个小优化，
         * 不是针对每条数据都查询一次用户，而是先获取到要查询的用户 id 列表，
         * 只发送一次查询用户表的请求，再将查到的值设置到图片对象中。
         * */

        // 对象列表 =》 封装对象列表
        //picture -> PictureVO.objToVo(picture)
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());

        //1.关联查询用户信息
        // 将pictureList中的userId提取出来，存入userIdSet中
        // picture -> picture.getUserId()
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        // 根据userIdSet中的userId，从userService中获取对应的User列表，存入userIdUserListMap中
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 2.填充信息
        // 遍历pictureVOList
        pictureVOList.forEach(pictureVO -> {
            // 获取pictureVO中的userId
            Long userId = pictureVO.getUserId();
            // 初始化user
            User user = null;
            // 如果userIdUserListMap中包含userId
            if (userIdUserListMap.containsKey(userId)) {
                // 获取userIdUserListMap中userId对应的User列表中的第一个User
                user = userIdUserListMap.get(userId).get(0);
            }
            // 将user转换为UserVO并设置到pictureVO中
            pictureVO.setUser(userService.getUserVO(user));
        });

        // 将pictureVOList设置到pictureVOPage中
        pictureVOPage.setRecords(pictureVOList);
        // 返回pictureVOPage
        return pictureVOPage;

    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);

        //从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();

        //修改数据时，id不能为空， 有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url长度不能超过1024");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 1024, ErrorCode.PARAMS_ERROR, "introduction长度不能超过1024");
        }


    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 1, 校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);

        if(id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2, 判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.PARAMS_ERROR, "图片不存在");

        // 3， 校验审核状态是否重复，已经是改状态
        if (oldPicture.getReviewStatus().equals(reviewStatus)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }

        // 4.数据库操作

        /**
         *
         * MybatisPlus 会更新含有的字段
         * 如果用旧的对象来更新，会把所有的字段都更新一遍
         * 不如新建一个对象
         * 把需要的字段更新就好了，这样性能更高
        */
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());

        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "审核失败");


    }


    @Override
    public void fileReviewParams(Picture picture, User loginUser){
        if(userService.isAdmin(loginUser)){


        }

    }



}




