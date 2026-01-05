package com.evorsio.mybox.search;

/**
 * 索引状态枚举
 */
public enum IndexStatus {
    PENDING,       // 待索引
    INDEXING,      // 索引中
    INDEXED,       // 已索引
    FAILED,        // 索引失败
    OUTDATED       // 索引过期（文件已更新，需要重新索引）
}
