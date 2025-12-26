package com.evorsio.mybox.common.constants;

import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class RedisKeyConstants {

    // Redis 类型名
    public static final String REDIS_REFRESH_TOKEN = "refreshToken";


    //格式：<project>:<module>:<type>:<userId>
    public static String refreshTokenKey(UUID userId) {
        return composeKey(
                MyboxConstants.PROJECT,
                MyboxConstants.MODULE_AUTH,
                REDIS_REFRESH_TOKEN,
                userId.toString()
        );
    }

    private static String composeKey(String... parts) {
        return String.join(":", parts);
    }
}
