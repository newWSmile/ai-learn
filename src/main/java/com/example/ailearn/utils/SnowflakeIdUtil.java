package com.example.ailearn.utils;

import cn.hutool.core.util.IdUtil;

public class SnowflakeIdUtil {

    private SnowflakeIdUtil() {
    }

    public static String nextIdStr() {
        return IdUtil.getSnowflakeNextIdStr();
    }

}
