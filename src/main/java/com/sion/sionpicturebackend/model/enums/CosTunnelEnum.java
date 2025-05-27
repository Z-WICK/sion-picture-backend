package com.sion.sionpicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 对象存储通道枚举
 *
 * @author wick
 * @date 2025/05/27
 */
@Getter
public enum CosTunnelEnum {
    TENCENT("腾讯云", 0),
    CFR2("cloudflareR2", 1);

    private final String text;
    private final int value;

    CosTunnelEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static CosTunnelEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (CosTunnelEnum cosTunnelEnum : CosTunnelEnum.values()) {
            if (cosTunnelEnum.value == value) {
                return cosTunnelEnum;
            }
        }
        return null;
    }
}
