package com.evorsio.mybox.search;

/**
 * 搜索类型枚举
 */
public enum SearchType {
    FULLTEXT,      // 全文搜索（基于元数据的模糊搜索）
    SEMANTIC,      // 语义搜索（基于AI向量的相似度搜索）
    HYBRID         // 混合搜索（结合全文和语义搜索）
}
