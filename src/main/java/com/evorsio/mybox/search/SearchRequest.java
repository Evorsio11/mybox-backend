package com.evorsio.mybox.search;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * 搜索请求DTO
 */
@Data
public class SearchRequest {

    @NotBlank(message = "搜索关键词不能为空")
    private String query;

    @NotNull(message = "搜索类型不能为空")
    private SearchType searchType = SearchType.HYBRID;

    /**
     * 搜索过滤条件
     */
    private UUID ownerId;  // 限定所有者

    private String contentType;  // MIME类型过滤

    private Long minSize;  // 最小文件大小（字节）

    private Long maxSize;  // 最大文件大小（字节）

    private UUID folderId;  // 限定文件夹

    /**
     * 分页参数
     */
    @Min(value = 1, message = "页码最小为1")
    private int page = 1;

    @Min(value = 1, message = "每页数量最小为1")
    private int size = 20;

    /**
     * 排序方式
     */
    private String sortBy = "relevance";  // relevance, date, size, name

    private String sortDirection = "desc";  // asc, desc

    /**
     * 语义搜索参数（仅用于 SEMANTIC 和 HYBRID 搜索）
     */
    @Min(value = 0, message = "相似度阈值在0-1之间")
    @Min(value = 1, message = "相似度阈值在0-1之间")
    private Double minSimilarity = 0.7;  // 最小相似度阈值（0-1）

    /**
     * 全文搜索参数（仅用于 FULLTEXT 和 HYBRID 搜索）
     */
    private boolean fuzzyMatch = true;  // 是否启用模糊匹配

    /**
     * 混合搜索权重（仅用于 HYBRID 搜索）
     */
    private double fulltextWeight = 0.5;  // 全文搜索权重（0-1）

    private double semanticWeight = 0.5;  // 语义搜索权重（0-1）
}
