package com.evorsio.mybox.folder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 文件夹移动请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderMoveRequest {

    /**
     * 目标父文件夹 ID（业务 UUID，null 表示移动到根目录）
     */
    private UUID targetParentFolderId;
}
