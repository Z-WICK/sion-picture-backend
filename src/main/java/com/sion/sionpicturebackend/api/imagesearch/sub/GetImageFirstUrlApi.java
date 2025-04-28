package com.sion.sionpicturebackend.api.imagesearch.sub;

import com.sion.sionpicturebackend.exception.BusinessException;
import com.sion.sionpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author : wick
 * @Date : 2025/4/27 18:31
 */
@Slf4j
public class GetImageFirstUrlApi {

    /**
     * @param url
     * @return {@link String }
     */
    public static String getImageFirstUrl(String url) {
        try {
            // 使用Jsoup 获取HTML 内容
            Document document = Jsoup.connect(url).timeout(5000).get();

            // 获取所有<Script> 标签
            Elements scriptElements = document.getElementsByTag("script");

            // 遍历所有<Script> 标签 找到 firstUrl
            for (Element script : scriptElements) {
                String scriptContent = script.html();
                if (scriptContent.contains("\"firstUrl\"")) {
                    // 正则表达式提取 firstUrl 的值
                    Pattern pattern = Pattern.compile("\"firstUrl\"\\s*:\\s*\"(.*?)\"");
                    Matcher matcher = pattern.matcher(scriptContent);
                    if (matcher.find()) {
                        String firstUrl = matcher.group(1);
                        // 处理转义字符
                        firstUrl = firstUrl.replace("\\/", "/");
                        return firstUrl;
                    }
                }

            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"未找到firstUrl");

        } catch (IOException e) {
            log.error("获取firstUrl失败",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"搜索失败");
        }
    }

    public static void main(String[] args) {
        // 请求目标URL
        String url = "https://graph.baidu.com/s?card_key=&entrance=GENERAL&extUiData%5BisLogoShow%5D=1&f=all&isLogoShow=1&session_id=14250574104140620020&sign=12691d835bd75954bd2ad01745822621&tpl_from=pc";
        String imageFirstUrl = getImageFirstUrl(url);
        System.out.println("搜索成功，结果 URL：" + imageFirstUrl);
    }


}
