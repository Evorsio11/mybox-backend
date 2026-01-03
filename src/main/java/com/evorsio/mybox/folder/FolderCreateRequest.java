package com.evorsio.mybox.folder;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 创建文件夹请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderCreateRequest {

    /**
     * 文件夹名称
     */
    @NotBlank(message = "文件夹名称不能为空")
    private String folderName;

    /**
     * 父文件夹 ID（null 表示创建在根目录）
     */
    private UUID parentFolderId;

    /**
     * 文件夹颜色（可选）
     */
    private String color;

    /**
     * 文件夹图标（可选）
     */
    private String iconName;

    /**
     * 文件夹描述（可选）
     */
    private String description;
}
