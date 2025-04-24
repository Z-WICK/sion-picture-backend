package com.sion.sionpicturebackend.manager.cache;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @Author : wick
 * @Date : 2025/4/23 20:40
 */
@Component
public class HotKeyTracker {
    // 定义一个线程安全的Map，用于存储每个键的计数器
    private final Map<String, AtomicInteger> counter = new ConcurrentHashMap<>();
    // 定义热点阈值，表示1分钟内被认为是热点的最小访问次数
    private final int threshold = 50; // 热点阈值（1分钟内）

    // 记录一次访问，传入的key对应的计数器值加1
    public void record(String key) {
        // 使用computeIfAbsent方法，如果Map中不存在该key，则初始化为新的AtomicInteger对象
        counter.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
    }

    // 获取当前的热点键列表，即访问次数超过阈值的键
    public List<String> getHotKeys() {
        return counter.entrySet().stream()
            // 过滤出访问次数大于等于阈值的键值对
            .filter(entry -> entry.getValue().get() >= threshold)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    // 清空计数器Map
    public void clear() {
        counter.clear();
    }
}
