package com.evorsio.mybox.folder.internal.mapper;

import com.evorsio.mybox.folder.Folder;
import com.evorsio.mybox.folder.FolderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 文件夹实体与 DTO 转换 Mapper
 */
@Mapper(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {} // 如果有复杂的转换逻辑，可以注入其他 Service
)
public interface FolderMapper {

    /**
     * Folder 实体转 FolderResponse
     * 注意：Folder 使用 id 作为主键（Integer），不是 folderId
     */
    @org.mapstruct.Mapping(target = "hasChildren", expression = "java(folder.getFolderCount() != null && folder.getFolderCount() > 0)")
    @org.mapstruct.Mapping(target = "isRoot", expression = "java(folder.getParentFolderId() == null)")
    FolderResponse toResponse(Folder folder);

    /**
     * Folder 实体列表转 FolderResponse 列表
     */
    List<FolderResponse> toResponseList(List<Folder> folders);

    /**
     * 创建 Folder 实体（不包含自动生成的字段）
     * 用于从 DTO 创建新实体时
     */
    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "userId", ignore = true)
    @org.mapstruct.Mapping(target = "status", ignore = true)
    @org.mapstruct.Mapping(target = "fullPath", ignore = true)
    @org.mapstruct.Mapping(target = "level", ignore = true)
    @org.mapstruct.Mapping(target = "createdAt", ignore = true)
    @org.mapstruct.Mapping(target = "updatedAt", ignore = true)
    @org.mapstruct.Mapping(target = "deletedAt", ignore = true)
    @org.mapstruct.Mapping(target = "fileCount", ignore = true)
    @org.mapstruct.Mapping(target = "folderCount", ignore = true)
    @org.mapstruct.Mapping(target = "totalFileCount", ignore = true)
    @org.mapstruct.Mapping(target = "totalSize", ignore = true)
    Folder toEntityForCreate(com.evorsio.mybox.folder.FolderCreateRequest request);

    /**
     * 更新 Folder 实体的非关键字段
     * 用于更新元数据时
     */
    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "folderId", ignore = true)
    @org.mapstruct.Mapping(target = "userId", ignore = true)
    @org.mapstruct.Mapping(target = "primaryDeviceId", ignore = true)
    @org.mapstruct.Mapping(target = "parentFolderId", ignore = true)
    @org.mapstruct.Mapping(target = "folderName", ignore = true)
    @org.mapstruct.Mapping(target = "fullPath", ignore = true)
    @org.mapstruct.Mapping(target = "level", ignore = true)
    @org.mapstruct.Mapping(target = "status", ignore = true)
    @org.mapstruct.Mapping(target = "folderType", ignore = true)
    @org.mapstruct.Mapping(target = "createdAt", ignore = true)
    @org.mapstruct.Mapping(target = "updatedAt", ignore = true)
    @org.mapstruct.Mapping(target = "deletedAt", ignore = true)
    @org.mapstruct.Mapping(target = "fileCount", ignore = true)
    @org.mapstruct.Mapping(target = "folderCount", ignore = true)
    @org.mapstruct.Mapping(target = "totalFileCount", ignore = true)
    @org.mapstruct.Mapping(target = "totalSize", ignore = true)
    void updateEntityFromMetadataRequest(
            com.evorsio.mybox.folder.FolderMetadataUpdateRequest request,
            @MappingTarget Folder folder
    );
}
