package com.evorsio.mybox.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {

    private List<SearchResult> results;  // 搜索结果列表

    private SearchMetadata metadata;  // 搜索元数据

    /**
     * 搜索元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchMetadata {
        private long totalResults;  // 总结果数
        private long totalPages;  // 总页数
        private int currentPage;  // 当前页码
        private int pageSize;  // 每页大小
        private SearchType searchType;  // 搜索类型
        private String query;  // 搜索关键词
        private long searchTimeMs;  // 搜索耗时（毫秒）
        private boolean hasMore;  // 是否有更多结果

        // 统计信息
        private int fulltextMatches;  // 全文匹配数
        private int semanticMatches;  // 语义匹配数
        private int hybridMatches;  // 混合匹配数

        // 索引状态
        private long indexedFiles;  // 已索引文件数
        private long vectorizedFiles;  // 已向量化文件数
        private double indexCoverage;  // 索引覆盖率（0-1）
    }

    /**
     * 创建空的搜索响应
     */
    public static SearchResponse empty(String query, SearchType searchType) {
        return SearchResponse.builder()
                .results(List.of())
                .metadata(SearchMetadata.builder()
                        .totalResults(0)
                        .totalPages(0)
                        .currentPage(1)
                        .pageSize(20)
                        .searchType(searchType)
                        .query(query)
                        .searchTimeMs(0)
                        .hasMore(false)
                        .fulltextMatches(0)
                        .semanticMatches(0)
                        .hybridMatches(0)
                        .indexedFiles(0)
                        .vectorizedFiles(0)
                        .indexCoverage(0.0)
                        .build())
                .build();
    }
}
