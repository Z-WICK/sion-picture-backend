package com.sion.sionpicturebackend.api.imagesearch.sub;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.sion.sionpicturebackend.exception.BusinessException;
import com.sion.sionpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author : wick
 * @Date : 2025/4/27 14:35
 */
@Slf4j
public class GetImagePageUrlApi {

    /**
     * 获取以图搜图页面地址
     *
     * @param imageUrl
     * @return {@link String }
     */
    public static String getImagePageUrl(String imageUrl) {
        // 1. 准本请求参数

        Map<String,Object> formData = new HashMap<>();
        formData.put("image",imageUrl);
        formData.put("tn","pc");
        formData.put("from","pc");
        formData.put("image_source","PC_UPLOAD_URL");

        // 获取当前时间戳
        long uptime = System.currentTimeMillis();

        // 请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;

        try{
            // 2. 发送 POST 请求到百度接口
            HttpResponse response = HttpRequest.post(url)
                    .header("acs-token", RandomUtil.randomString(1))
                    .form(formData)
                    .timeout(5000)
                    .execute();
            // 判断响应状态
            if(HttpStatus.HTTP_OK != response.getStatus()){
                log.error("响应状态码：{}",response.getStatus());
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"接口调用失败");
            }

            // 解析响应
            String responseBody = response.body();
            Map<String,Object> result = JSONUtil.toBean(responseBody, Map.class);

            // 3. 处理响应结果
            if(result == null || !Integer.valueOf(0).equals(result.get("status"))){
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"接口调用失败");
            }
            Map<String,Object> data = (Map<String,Object>) result.get("data");
            String rawurl = (String) data.get("url");

            // 对URL 进行编码
            String searchResultUrl = URLUtil.decode(rawurl, StandardCharsets.UTF_8);

            // 如果URL为空
            if(searchResultUrl == null ){
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"未返回有效结果");

            }
            return searchResultUrl;
        }catch (Exception e){
            log.error("搜素失败",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"搜素失败");
        }

    }

    public static void main(String[] args) {
        // 测试以图搜图
        String imageUrl = "https://s2.loli.net/2025/04/07/mX7oNBrPASn2ydL.jpg";
        String result = getImagePageUrl(imageUrl);
        System.out.println("搜素成功，结果URL："+result);
    }
}
