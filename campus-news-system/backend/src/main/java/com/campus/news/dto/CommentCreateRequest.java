package com.campus.news.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentCreateRequest {
    @NotNull(message = "文章ID不能为空")
    private Long articleId;
    
    @NotBlank(message = "评论内容不能为空")
    private String content;
    
    private Long parentId;
    
    /**
     * 回复目标用户ID（多级回复时使用）
     */
    private Long replyToUserId;
    
    /**
     * 根评论ID（所有回复归属的顶级评论）
     */
    private Long rootId;
}
