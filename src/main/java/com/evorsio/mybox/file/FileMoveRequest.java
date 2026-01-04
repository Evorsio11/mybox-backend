package com.evorsio.mybox.file;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件移动请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMoveRequest {
    /**
     * 目标文件夹 ID（null 表示移动到根目录/未分类）
     */
    private UUID targetFolderId;
}
