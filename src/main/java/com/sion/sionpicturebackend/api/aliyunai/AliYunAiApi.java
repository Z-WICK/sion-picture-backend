package com.sion.sionpicturebackend.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.sion.sionpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.sion.sionpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.sion.sionpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.sion.sionpicturebackend.exception.BusinessException;
import com.sion.sionpicturebackend.exception.ErrorCode;
import com.sion.sionpicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Author : wick
 * @Date : 2025/4/29 16:23
 */
@Component
@Slf4j
public class AliYunAiApi {

    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";
    // 查询任务状态
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";
    // 读取配置文件
    @Value("${aliYunAi.apiKey}")
    private String apiKey;


    /**
     * 创建任务
     *
     * @param createOutPaintingTaskRequest
     * @return {@link CreateOutPaintingTaskResponse }
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        ThrowUtils.throwIf(createOutPaintingTaskRequest == null, ErrorCode.OPERATION_ERROR, "扩图参数为空");

        // 发送请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                // 必须开启异步处理，设置为enable。
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("创建任务失败，响应码：{}", httpResponse.getStatus());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            String errorCode = response.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                String errorMessage = response.getMessage();
                log.error("创建AI 扩图片失败，错误码：{}，错误信息：{}", errorCode, errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }

            return response;


        }


    }

    /**
     * 查询创建的任务
     *
     * @param taskId
     * @return {@link GetOutPaintingTaskResponse }
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isEmpty(taskId), ErrorCode.OPERATION_ERROR, "任务id不可空");
        // 发送请求
        try (HttpResponse httpResponse = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .execute()) {
            ThrowUtils.throwIf(!httpResponse.isOk(), ErrorCode.OPERATION_ERROR, "查询任务失败");


            return JSONUtil.toBean(httpResponse.body(),GetOutPaintingTaskResponse.class);
        }

    }

}
