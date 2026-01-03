package com.evorsio.mybox.folder;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件夹重命名请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderRenameRequest {
    /**
     * 新文件夹名称
     */
    @NotBlank(message = "文件夹名称不能为空")
    private String folderName;
}
