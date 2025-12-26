package com.evorsio.mybox.storage.domain;

import lombok.Getter;

@Getter
public enum FileStatus {

    /**
     * 正常可用
     * - 文件存在于 MinIO
     * - 用户可访问、下载、分享
     */
    ACTIVE,

    /**
     * 逻辑删除（回收站）
     * - 数据库仍存在
     * - MinIO 对象仍存在
     * - 可恢复
     */
    DELETED,

    /**
     * 上传中（尚未完成）
     * - 分片上传
     * - 断点续传
     * - 防止脏数据被当成可用文件
     */
    UPLOADING,

    /**
     * 上传失败
     * - MinIO 上传异常
     * - 数据库写入失败后的补偿状态
     */
    FAILED,

    /**
     * 被系统锁定
     * - 合规扫描失败
     * - 病毒扫描
     * - 管理员封禁
     */
    BLOCKED,

    /**
     * 物理删除
     * - 一般不会直接出现
     * - 仅用于删除任务 / 审计记录
     */
    PURGED
}
