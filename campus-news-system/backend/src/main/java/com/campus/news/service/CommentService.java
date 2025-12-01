package com.campus.news.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.news.dto.CommentCreateRequest;
import com.campus.news.entity.Article;
import com.campus.news.entity.Comment;
import com.campus.news.exception.BusinessException;
import com.campus.news.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService extends ServiceImpl<CommentMapper, Comment> {
    
    private final CommentMapper commentMapper;
    private final UserService userService;
    private final ArticleService articleService;
    
    @Transactional
    public Comment createComment(CommentCreateRequest request, Long userId) {
        Comment comment = new Comment();
        comment.setArticleId(request.getArticleId());
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setParentId(request.getParentId());
        comment.setLikeCount(0);
        comment.setStatus(1);
        
        commentMapper.insert(comment);
        
        // 更新文章评论数
        updateArticleCommentCount(request.getArticleId(), 1);
        
        return comment;
    }
    
    public List<Comment> getCommentList(Long articleId) {
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("article_id", articleId)
                .isNull("parent_id")
                .orderByDesc("created_at");
        
        List<Comment> comments = commentMapper.selectList(wrapper);
        
        // 填充用户信息和回复
        comments.forEach(comment -> {
            comment.setUser(userService.getUserInfo(comment.getUserId()));
            
            // 获取回复
            QueryWrapper<Comment> replyWrapper = new QueryWrapper<>();
            replyWrapper.eq("parent_id", comment.getId())
                    .orderByAsc("created_at");
            List<Comment> replies = commentMapper.selectList(replyWrapper);
            replies.forEach(reply -> reply.setUser(userService.getUserInfo(reply.getUserId())));
            comment.setReplies(replies);
        });
        
        return comments;
    }
    
    @Transactional
    public boolean deleteComment(Long id, Long userId) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("无权限删除该评论");
        }
        
        commentMapper.deleteById(id);
        updateArticleCommentCount(comment.getArticleId(), -1);
        
        return true;
    }
    
    private void updateArticleCommentCount(Long articleId, int delta) {
        Article article = articleService.getById(articleId);
        if (article != null) {
            article.setCommentCount(article.getCommentCount() + delta);
            articleService.updateById(article);
        }
    }
}
