package com.sion.sionpicturebackend.manager.cache;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sion.sionpicturebackend.model.dto.picture.PictureQueryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author : wick
 * @Date : 2025/4/23 20:47
 */
@Component
@Slf4j
public class CacheManager {

    //构造本地缓存，设置缓存容量和过期时间
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10000L)
            //缓存5分钟
            .expireAfterWrite(5L, TimeUnit.MINUTES)
            .build();

    @Resource
    private HotKeyTracker hotKeyTracker;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    /**
     * 构建缓存key
     *
     * @param pictureQueryRequest
     * @return {@link String }
     */
    public String buildCacheKey(PictureQueryRequest pictureQueryRequest) {
        // 构建缓存key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = "sionpicture:listPictureVOByPage:" + hashKey;

        return cacheKey;
    }


    /**
     * 查询本地缓存
     *
     * @param cacheKey
     * @return {@link String }
     */
    public String getLocalCache(String cacheKey) {
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        return cachedValue;
    }


    /**
     * 写入本地缓存
     *
     * @param cacheKey
     * @param value
     */
    public void putLocalCache(String cacheKey, String value) {
        if (cacheKey == null || value == null) {
            log.warn("忽略缓存操作，key 或 value 为 null，key={}, value={}", cacheKey, value);
            return;
        }
        LOCAL_CACHE.put(cacheKey, value);
    }


    /**
     * 获取分布式缓存
     *
     * @param cacheKey
     * @return {@link String }
     */
    public String getRedisCache(String cacheKey) {
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        valueOps = stringRedisTemplate.opsForValue();
        String cachedValue = valueOps.get(cacheKey);
        return cachedValue;
    }

    /**
     * 写入分布式缓存
     *
     * @param cacheKey
     * @param cacheValue
     */
    public void setRedisCache(String cacheKey, String cacheValue) {
        //5-10分钟随机过期，防止雪崩
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        valueOps.set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
    }

    /**
     * 定时缓存热门key，到本地
     */
    @Scheduled(fixedRate = 60000)
    public void cacheHotKeys() {
        List<String> hotKeys = hotKeyTracker.getHotKeys();
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        for (String key : hotKeys) {
            String value = valueOps.get(key);
            if (StrUtil.isNotBlank(value)) {
                LOCAL_CACHE.put(key, value); // Caffeine 或 ConcurrentMap
                log.info("热Key已缓存至本地：{}", key);
            }
        }
        hotKeyTracker.clear();
    }


}
