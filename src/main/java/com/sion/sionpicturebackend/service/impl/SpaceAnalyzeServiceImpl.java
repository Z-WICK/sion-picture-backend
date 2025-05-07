package com.sion.sionpicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sion.sionpicturebackend.exception.BusinessException;
import com.sion.sionpicturebackend.exception.ErrorCode;
import com.sion.sionpicturebackend.exception.ThrowUtils;
import com.sion.sionpicturebackend.mapper.SpaceMapper;
import com.sion.sionpicturebackend.model.dto.space.analyze.*;
import com.sion.sionpicturebackend.model.entity.Picture;
import com.sion.sionpicturebackend.model.entity.Space;
import com.sion.sionpicturebackend.model.entity.User;
import com.sion.sionpicturebackend.model.vo.space.analyze.*;
import com.sion.sionpicturebackend.service.PictureService;
import com.sion.sionpicturebackend.service.SpaceAnalyzeService;
import com.sion.sionpicturebackend.service.SpaceService;
import com.sion.sionpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author : wick
 * @Date : 2025/5/6 18:18
 */
@Service
@Slf4j
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    private static void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        if (spaceAnalyzeRequest.isQueryAll()) {
            return;
        }

        if (spaceAnalyzeRequest.isQueryPublic()) {
            queryWrapper.isNull("spaceId");
            return;
        }

        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }

        throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误,未指定查询范围");
    }

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "参数错误");
        if (spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()) {
            // 查询所有图库或公共图库的权限校验
            // 仅管理员可访问
            boolean isAdmin = userService.isAdmin(loginUser);
            ThrowUtils.throwIf(!isAdmin, ErrorCode.NO_AUTH_ERROR, "非管理员无法访问公共图库");

            // 统计公共图库的资源使用
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            // 公共图库去掉spaceId 为空的条件
            if (!spaceUsageAnalyzeRequest.isQueryAll()) {
                queryWrapper.isNull("spaceId");
            }

            // 从pictureService获取基础Mapper，并执行查询操作，将结果存储在pictureObjList中
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            // 使用Java 8的Stream API对pictureObjList进行流处理
            // mapToLong方法将每个元素映射为long类型，如果元素是Long类型则直接转换，否则转换为0
            // sum方法对映射后的long值进行求和，得到usedSize
            long usedSize = pictureObjList.stream().mapToLong(result -> result instanceof Long ? (Long) result : 0).sum();
            // 获取pictureObjList的大小，即列表中的元素数量，存储在usedcount中
            long usedcount = pictureObjList.size();

            // 封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedcount);

            // 公共图库无上限，无比例
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            return spaceUsageAnalyzeResponse;


        } else {

            // 查询指定空间
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR, "空间ID不能为空");

            // 获取空间信息

            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

            // 权限校验：仅空间所有者或管理员可访问
            spaceService.checkSpaceAuth(loginUser, space);

            // 构造返回结果
            SpaceUsageAnalyzeResponse response = new SpaceUsageAnalyzeResponse();
            response.setUsedSize(space.getTotalSize());
            response.setMaxSize(space.getMaxSize());
            response.setUsedCount(space.getTotalCount());
            response.setMaxCount(space.getMaxCount());

            // 后端直接算好百分比返回给前端
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            response.setSizeUsageRatio(sizeUsageRatio);
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            response.setCountUsageRatio(countUsageRatio);

            return response;
        }
    }

    /**
     * 检查空间 查询权限
     *
     * @param spaceAnalyzeRequest
     * @param loginUser
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        // 检查权限
        if (spaceAnalyzeRequest.isQueryAll() || spaceAnalyzeRequest.isQueryPublic()) {
            // 查询所有图库或公共图库的权限校验
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "非管理员无法访问公共图库");

        } else {
            // 私有空间权限校验
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR, "空间ID不能为空");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            spaceService.checkSpaceAuth(loginUser, space);
        }
    }


    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");

        // 检查权限
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();

        // 根据分析范围补充查询条件
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);


        // 使用 Mybatis plus 分组查询
        queryWrapper.select("category AS category",
                        "count(*) AS count",
                        "sum(picSize) AS totalSize ")
                .groupBy("category");

        // 查询并转换结果
        return pictureService.getBaseMapper()
                .selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String category = result.get("category") != null ? result.get("category").toString() : "未分类";
                    long count = ((Number) result.get("count")).longValue();
                    Long totalSize = ((Number) result.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");

        // 检查权限
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);

        // 查询所有符合条件的标签
        queryWrapper.select("tags");

        // 从pictureService获取基础Mapper对象，并执行查询操作，传入queryWrapper作为查询条件
        List<String> tagJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                // 将查询结果转换为流（Stream）
                .stream()
                // 过滤掉为null的对象，使用ObjUtil工具类的isNotNull方法进行判断
                .filter(ObjUtil::isNotNull)
                // 将每个对象转换为字符串，使用Object类的toString方法
                .map(Object::toString)
                // 将转换后的字符串收集到一个List中
                .collect(Collectors.toList());

        // 合并所有标签并统计使用
        // 将tagJsonList中的每个JSON字符串转换为字符串列表，并统计每个标签的使用次数
        Map<String, Long> tagCountMap = tagJsonList.stream()
                // 将tagJsonList中的每个JSON字符串转换为字符串流
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                // 按标签进行分组，并统计每个标签的出现次数
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        // 转换为响应对象，按使用次数降序排序
        // 从tagCountMap中获取所有键值对，并将其转换为一个流
        return tagCountMap.entrySet().stream()
                // 对流中的元素进行排序，排序规则是按照每个键值对的值（Long类型）进行降序排列
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                // 将排序后的每个键值对映射为一个新的SpaceTagAnalyzeResponse对象
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                // 将映射后的流收集为一个列表并返回
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");

        // 检查权限
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);

        // 查询所有符合条件的图片大小
        queryWrapper.select("picSize");
        // 调用pictureService的getBaseMapper方法获取Mapper对象
        // 使用selectObjs方法根据queryWrapper查询数据库，返回一个包含查询结果的列表
        // 这里假设查询结果是一个包含图片大小的列表
        List<Long> picSizes = pictureService.getBaseMapper().selectObjs(queryWrapper)
                // 将查询结果转换为Stream流，以便进行后续操作
                .stream()
                // 使用map方法将每个元素转换为Long类型
                // 这里假设查询结果中的每个元素都是一个Number类型的对象
                // 使用((Number) size).longValue()将Number类型的对象转换为Long类型
                .map(size -> ((Number) size).longValue())
                // 使用collect方法将转换后的流收集到一个List中
                // Collectors.toList()是一个收集器，用于将流中的元素收集到一个List中
                .collect(Collectors.toList());

        // 定义分段范围，注意使用有序Map
        LinkedHashMap<String, Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<100KB", picSizes.stream().filter(size -> size < 100 * 1024).count());
        sizeRanges.put("100KB-500KB", picSizes.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        sizeRanges.put("500KB-1MB", picSizes.stream().filter(size -> size >= 500 * 1024 && size < 1 * 1024 * 1024).count());
        sizeRanges.put(">1MB", picSizes.stream().filter(size -> size >= 1 * 1024 * 1024).count());


        // 转换为响应对象
        return sizeRanges.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }


    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");

        // 检查权限
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);

        // 分析维度： 每日、每周、每月
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period", "COUNT(*) AS count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) AS period", "COUNT(*) AS count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period", "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }

        // 分组和排序
        queryWrapper.groupBy("period").orderByAsc("period");

        // 查询结果并转换
        List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);
        return queryResult.stream()
                .map(result -> {
                    String period = (String) result.get("period");
                    Long count = ((Number) result.get("count")).longValue();
                    return new SpaceUserAnalyzeResponse(period, count);
                })
                .collect(Collectors.toList());

    }

    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");

        // 仅管理员可查看空间排行
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "仅管理员可查看空间排行");

        // 构造查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id","spaceName", "userId","totalSize")
                .orderByDesc("totalSize")
                .last("LIMIT " + spaceRankAnalyzeRequest.getTopN());

        // 查询结果
        return spaceService.list(queryWrapper);

    }

}
