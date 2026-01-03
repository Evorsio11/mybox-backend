package com.evorsio.mybox.folder;

/**
 * 文件夹状态枚举
 */
public enum FolderStatus {
    /**
     * 正常可用
     * - 文件夹可访问、可修改
     * - 用户可以进行文件操作
     */
    ACTIVE,

    /**
     * 已删除（软删除）
     * - 文件夹在回收站中
     * - 数据库仍存在
     * - 可恢复
     */
    DELETED,

    /**
     * 已归档
     * - 文件夹已归档
     * - 只读访问
     * - 不可修改
     */
    ARCHIVED
}
