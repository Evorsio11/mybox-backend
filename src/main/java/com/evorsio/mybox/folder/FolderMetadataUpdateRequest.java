package com.evorsio.mybox.folder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件夹元数据更新请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderMetadataUpdateRequest {

    /**
     * 文件夹颜色（十六进制颜色码）
     */
    private String color;

    /**
     * 图标名称
     */
    private String iconName;

    /**
     * 文件夹描述
     */
    private String description;

    /**
     * 是否收藏
     */
    private Boolean isStarred;

    /**
     * 自定义排序序号
     */
    private Integer sortOrder;
}
