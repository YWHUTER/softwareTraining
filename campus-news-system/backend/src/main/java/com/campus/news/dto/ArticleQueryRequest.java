package com.campus.news.dto;

import lombok.Data;

@Data
public class ArticleQueryRequest {
    private Long current = 1L;
    private Long size = 10L;
    private String boardType;
    private Long collegeId;
    private Long authorId;
    private String keyword;
    private Integer isApproved;
    private Integer isPinned;
    /**
     * 排序字段: date(按日期), views(按浏览量/热度)
     */
    private String sortBy;
    /**
     * 排序方向: asc(升序), desc(降序)，默认desc
     */
    private String sortOrder = "desc";
}
