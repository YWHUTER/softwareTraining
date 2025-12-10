package com.campus.news.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

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
     * 按日期过滤：闭区间 [startDate, endDate]
     * 仅选择 startDate 时，按当天过滤
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    /**
     * 排序字段: date(按日期), views(按浏览量/热度)
     */
    private String sortBy;
    /**
     * 排序方向: asc(升序), desc(降序)，默认desc
     */
    private String sortOrder = "desc";
}
