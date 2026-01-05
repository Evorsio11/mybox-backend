package com.evorsio.mybox.common;

import lombok.extern.slf4j.Slf4j;

/**
 * 单位解析工具类
 * <p>
 * 统一处理时间、大小等单位的解析
 */
@Slf4j
public class SizeUnitParser {

    /**
     * 解析时间窗口字符串为秒数
     * <p>
     * 支持格式:
     * - 秒: 30s, 30S, 30
     * - 分钟: 5m, 5M
     * - 小时: 2h, 2H
     * - 天: 1d, 1D
     *
     * @param window 时间窗口字符串
     * @return 秒数
     */
    public static int parseTimeToSeconds(String window) {
        if (window == null || window.isEmpty()) {
            return 60; // 默认 60 秒
        }

        String trimmed = window.trim().toLowerCase();
        try {
            int value = Integer.parseInt(trimmed.substring(0, trimmed.length() - 1));

            if (trimmed.endsWith("s")) {
                // 秒: 30s -> 30
                return value;
            } else if (trimmed.endsWith("m")) {
                // 分钟: 5m -> 300
                return value * 60;
            } else if (trimmed.endsWith("h")) {
                // 小时: 2h -> 7200
                return value * 3600;
            } else if (trimmed.endsWith("d")) {
                // 天: 1d -> 86400
                return value * 86400;
            } else {
                // 纯数字，默认为秒
                return Integer.parseInt(trimmed);
            }
        } catch (NumberFormatException e) {
            log.warn("无法解析时间窗口: {}, 使用默认值 60 秒", window);
            return 60;
        }
    }

    /**
     * 解析大小字符串为字节数
     * <p>
     * 支持格式:
     * - 字节: 1024, 1024b, 1024B
     * - KB: 10kb, 10KB, 10K
     * - MB: 100mb, 100MB, 100M
     * - GB: 2gb, 2GB, 2G
     * - TB: 1tb, 1TB, 1T
     *
     * @param size 大小字符串
     * @return 字节数
     */
    public static long parseSizeToBytes(String size) {
        if (size == null || size.isEmpty()) {
            return 0L;
        }

        String trimmed = size.trim().toUpperCase();
        try {
            // 提取数字部分
            String numberPart = trimmed.replaceAll("[^0-9.]", "");
            double value = Double.parseDouble(numberPart);

            // 判断单位
            if (trimmed.endsWith("TB") || trimmed.endsWith("T")) {
                return (long) (value * 1024L * 1024L * 1024L * 1024L);
            } else if (trimmed.endsWith("GB") || trimmed.endsWith("G")) {
                return (long) (value * 1024L * 1024L * 1024L);
            } else if (trimmed.endsWith("MB") || trimmed.endsWith("M")) {
                return (long) (value * 1024L * 1024L);
            } else if (trimmed.endsWith("KB") || trimmed.endsWith("K")) {
                return (long) (value * 1024L);
            } else {
                // 默认为字节
                return (long) value;
            }
        } catch (NumberFormatException e) {
            log.warn("无法解析大小: {}, 返回 0", size);
            return 0L;
        }
    }

    /**
     * 将字节数格式化为人类可读的大小
     * <p>
     * 例如: 1024 -> 1KB, 1048576 -> 1MB
     *
     * @param bytes 字节数
     * @return 格式化后的大小字符串
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2fKB", bytes / 1024.0);
        } else if (bytes < 1024L * 1024L * 1024L) {
            return String.format("%.2fMB", bytes / (1024.0 * 1024.0));
        } else if (bytes < 1024L * 1024L * 1024L * 1024L) {
            return String.format("%.2fGB", bytes / (1024.0 * 1024.0 * 1024.0));
        } else {
            return String.format("%.2fTB", bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 将秒数格式化为人类可读的时间
     * <p>
     * 例如: 60 -> 1m, 3600 -> 1h, 86400 -> 1d
     *
     * @param seconds 秒数
     * @return 格式化后的时间字符串
     */
    public static String formatSeconds(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            if (seconds % 60 == 0) {
                return minutes + "m";
            } else {
                return minutes + "m" + (seconds % 60) + "s";
            }
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            if (seconds % 3600 == 0) {
                return hours + "h";
            } else {
                return hours + "h" + ((seconds % 3600) / 60) + "m";
            }
        } else {
            long days = seconds / 86400;
            if (seconds % 86400 == 0) {
                return days + "d";
            } else {
                return days + "d" + ((seconds % 86400) / 3600) + "h";
            }
        }
    }
}
