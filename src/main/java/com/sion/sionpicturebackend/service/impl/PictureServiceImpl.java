package com.sion.sionpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sion.sionpicturebackend.exception.BusinessException;
import com.sion.sionpicturebackend.exception.ErrorCode;
import com.sion.sionpicturebackend.exception.ThrowUtils;
import com.sion.sionpicturebackend.manager.CosManager;
import com.sion.sionpicturebackend.manager.upload.FilePictureUpload;
import com.sion.sionpicturebackend.manager.upload.PictureUploadTemplate;
import com.sion.sionpicturebackend.manager.upload.UrlPictureUpload;
import com.sion.sionpicturebackend.mapper.PictureMapper;
import com.sion.sionpicturebackend.model.dto.file.UploadPictureResult;
import com.sion.sionpicturebackend.model.dto.picture.*;
import com.sion.sionpicturebackend.model.entity.Picture;
import com.sion.sionpicturebackend.model.entity.Space;
import com.sion.sionpicturebackend.model.entity.User;
import com.sion.sionpicturebackend.model.enums.PictureReviewStatusEnum;
import com.sion.sionpicturebackend.model.vo.picture.PictureVO;
import com.sion.sionpicturebackend.model.vo.user.UserVO;
import com.sion.sionpicturebackend.review.PictureReviewExecutor;
import com.sion.sionpicturebackend.review.PrivateSpaceReviewStrategy;
import com.sion.sionpicturebackend.review.PublicGalleryReviewStrategy;
import com.sion.sionpicturebackend.review.ReviewStrategy;
import com.sion.sionpicturebackend.service.PictureService;
import com.sion.sionpicturebackend.service.SpaceService;
import com.sion.sionpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
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
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    private UserService userService;


    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private CosManager cosManager;

    @Resource
    private SpaceService spaceService;

    @Resource
    private TransactionTemplate transactionTemplate;


    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);


        // 校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 必须空间创建人（管理员）才能上传
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅空间创建人（管理员）才能上传");
            }

            // 校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间容量不足");
            }
        }


        if (inputSource == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片为空");
        }

        // 用于判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }


        // 如果是更新图片，需要检验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

            // 仅本人或管理员可更新
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅本人或管理员可更新");
            }

            // 校验空间是否一致
            // 没传 spaceId , 则复用原有图片的spaceId
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 传了 spaceId , 必须和原有图片一致
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片空间ID不一致");
                }
            }
        }

        // 上传图片，得到信息
        // 按照用户id划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
            uploadPathPrefix = String.format("public/%s", loginUser.getId());

        } else {
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        // 生成上传路径前缀，格式为 "public/用户id"
        // 根据 inputSource 类型区分上传方式
        // 初始化图片上传模板为文件上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        // 判断 inputSource 是否为 String 类型
        if (inputSource instanceof String) {
            // 如果 inputSource 是 String 类型，则使用 URL 上传方式
            pictureUploadTemplate = urlPictureUpload;
        }
        // 使用选定的图片上传模板进行图片上传，并获取上传结果
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        //构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl("https://" + uploadPictureResult.getUrl());
        picture.setThumbnailUrl("https://" + uploadPictureResult.getThumbnailUrl());
        // 从uploadPictureResult对象中获取图片名称
        String picName = uploadPictureResult.getPicName();
        // 检查pictureUploadRequest对象是否不为null，并且其图片名称不为空白
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            // 如果条件满足，将picture对象的名称设置为uploadPictureResult中的图片名称
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 补充设置 spaceId
        picture.setSpaceId(spaceId);

        // 补充审核参数
        /*
        *   当 spaceId 为 null 时，isPublicGallery 变量被设置为 true
            当 spaceId 不为 null 时，isPublicGallery 变量被设置为 false
        * */
        boolean isPublicGallery = spaceId == null;
        fileReviewParams(picture, loginUser, isPublicGallery);

        //如果 pictureId 不为空，则更新图片信息，反之新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和 编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        // 开启事务
        // 将传入的spaceId赋值给finalSpaceId，确保在lambda表达式中可以访问到该变量
        Long finalSpaceId = spaceId;
        // 使用transactionTemplate执行数据库事务操作
        transactionTemplate.execute(status -> {
            // 调用saveOrUpdate方法保存或更新图片信息，返回操作结果
            boolean result = this.saveOrUpdate(picture);
            // 如果保存或更新操作失败，抛出操作异常
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败,数据库操作失败");

            // 如果finalSpaceId不为null，表示需要更新空间信息
            if (finalSpaceId != null) {
                // 使用lambdaUpdate方法构建更新条件
                boolean update = spaceService.lambdaUpdate()
                        // 设置更新条件，匹配id为finalSpaceId的空间记录
                        .eq(Space::getId, finalSpaceId)
                        // 使用SQL语句更新totalSize字段，增加上传图片的大小
                        .setSql("totalSize = totalSize + " + uploadPictureResult.getPicSize())
                        // 使用SQL语句更新totalCount字段，增加1
                        .setSql("totalCount = totalCount + 1")
                        // 执行更新操作，返回更新结果
                        .update();
                // 如果更新操作失败，抛出操作异常
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            // 返回保存或更新后的图片对象
            return picture;


        });

        // todo 可自行实现，如果是更新，可以清理图片资源
        // this.clearPictureFile(oldPicture);

        return PictureVO.objToVo(picture);
    }


    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
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
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();

        //审核字段
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();

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
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        // 创建一个查询条件构造器 queryWrapper
        // 调用 queryWrapper 的 isNull 方法，用于构建查询条件
        // 该方法的作用是添加一个判断条件，即指定字段 "spaceId" 的值为 NULL
        // 参数 nullSpaceId 在这里传入的是 null，表示需要判断的字段 "spaceId" 是否为 NULL
        queryWrapper.isNull(nullSpaceId, "spaceId");

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


    /**
     * 分页获取图片封装
     */
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

        /*
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

        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2, 判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.PARAMS_ERROR, "图片不存在");

        // 3， 校验审核状态是否重复，已经是改状态
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
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
    public void fileReviewParams(Picture picture, User loginUser, boolean isPublicGallery) {
        // 根据上传类型选择审核策略
        ReviewStrategy reviewStrategy;
        if (isPublicGallery) {
            reviewStrategy = new PublicGalleryReviewStrategy();
        } else {
            reviewStrategy = new PrivateSpaceReviewStrategy();
        }

        // 使用策略执行审核
        PictureReviewExecutor pictureReviewExecutor = new PictureReviewExecutor(reviewStrategy);
        pictureReviewExecutor.executeReview(picture, loginUser);
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        // 名称前缀默认等于搜索关键词
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        // 抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        // 解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        // 遍历元素，依次处理上传图片
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }
            // 处理图片的地址，防止转义或者和对象存储冲突的问题
            // codefather.cn?sion=dog，应该只保留 codefather.cn
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            try {
                // this 当前类中定义的方法
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功，id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        cosManager.deleteObject(oldPicture.getUrl());
        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }


    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        checkPictureAuth(loginUser, oldPicture);

        //开启事务
        // 使用事务模板执行数据库操作
        // 使用事务模板执行数据库操作
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

            // 释放额度
            Long spaceId = oldPicture.getSpaceId();
            if (spaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            // 这个随便返回不重要
            return true;

        });


        // 异步清理文件
        this.clearPictureFile(oldPicture);
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        checkPictureAuth(loginUser, oldPicture);
        // 补充审核参数
        boolean isPublicGallery = pictureEditRequest.getSpaceId() == null;
        this.fileReviewParams(picture, loginUser,isPublicGallery);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }


    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 公共图库，仅本人或管理员可操作
            if (!loginUser.getId().equals(picture.getUserId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "无权限操作");
            } else {
                // 私有空间，仅空间管理员可操作
                if (!picture.getUserId().equals(loginUser.getId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "私有空间，仅空间管理员可操作");
                }
            }
        }
    }


}




