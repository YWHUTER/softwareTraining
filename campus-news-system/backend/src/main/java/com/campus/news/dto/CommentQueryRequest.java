package com.campus.news.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class CommentQueryRequest {
    private Long current = 1L;
    private Long size = 10L;
    /**
     * received: 收到的评论；sent: 我发出的评论
     */
    private String type = "received";
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}

