package com.campus.news.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.news.common.PageResult;
import com.campus.news.dto.CommentCreateRequest;
import com.campus.news.dto.CommentQueryRequest;
import com.campus.news.entity.Article;
import com.campus.news.entity.Comment;
import com.campus.news.entity.User;
import com.campus.news.exception.BusinessException;
import com.campus.news.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService extends ServiceImpl<CommentMapper, Comment> {
    
    private final CommentMapper commentMapper;
    private final UserService userService;
    private final ArticleService articleService;
    private final NotificationService notificationService;
    @Lazy
    private final RealtimeNotificationService realtimeNotificationService;
    
    @Transactional
    public Comment createComment(CommentCreateRequest request, Long userId) {
        Comment comment = new Comment();
        comment.setArticleId(request.getArticleId());
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setParentId(request.getParentId());
        comment.setReplyToUserId(request.getReplyToUserId());
        comment.setRootId(request.getRootId());
        comment.setLikeCount(0);
        comment.setStatus(1);
        
        commentMapper.insert(comment);
        
        // 更新文章评论数
        updateArticleCommentCount(request.getArticleId(), 1);
        
        // 🔔 发送实时通知给文章作者
        Article article = articleService.getById(request.getArticleId());
        if (article != null && !article.getAuthorId().equals(userId)) {
            realtimeNotificationService.sendCommentNotification(
                article.getAuthorId(), userId, request.getArticleId(),
                article.getTitle(), request.getContent());
        }
        
        // 解析 @ 提及并发送通知
        notificationService.parseAndNotifyMentions(
            request.getContent(), 
            userId, 
            request.getArticleId(), 
            comment.getId()
        );
        
        // 如果是回复别人的评论，给被回复者发送通知
        if (request.getReplyToUserId() != null && !request.getReplyToUserId().equals(userId)) {
            User fromUser = userService.getUserInfo(userId);
            // 复用上面已查询的 article 变量
            String content = (fromUser != null ? fromUser.getRealName() : "某用户") 
                + " 回复了你在「" + (article != null ? article.getTitle() : "某篇文章") + "」的评论";
            notificationService.createNotification(
                request.getReplyToUserId(),
                userId,
                "REPLY",
                request.getArticleId(),
                comment.getId(),
                content
            );
        }
        
        return comment;
    }
    
    public List<Comment> getCommentList(Long articleId) {
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("article_id", articleId)
                .isNull("root_id")  // 顶级评论：rootId 为空
                .orderByDesc("created_at");
        
        List<Comment> comments = commentMapper.selectList(wrapper);
        
        // 填充用户信息和回复
        comments.forEach(comment -> {
            comment.setUser(userService.getUserInfo(comment.getUserId()));
            
            // 获取所有子回复（按 rootId 查询，包含多级回复）
            QueryWrapper<Comment> replyWrapper = new QueryWrapper<>();
            replyWrapper.eq("root_id", comment.getId())
                    .orderByAsc("created_at");
            List<Comment> replies = commentMapper.selectList(replyWrapper);
            
            // 填充回复的用户信息
            replies.forEach(reply -> {
                reply.setUser(userService.getUserInfo(reply.getUserId()));
                // 填充被回复用户的信息
                if (reply.getReplyToUserId() != null) {
                    reply.setReplyToUser(userService.getUserInfo(reply.getReplyToUserId()));
                }
            });
            
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
    
    public PageResult<Comment> getCommentHistory(CommentQueryRequest request, Long userId) {
        Page<Comment> page = new Page<>(request.getCurrent(), request.getSize());
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        
        String type = request.getType();
        boolean isSent = "sent".equalsIgnoreCase(type);
        if (isSent) {
            wrapper.eq("user_id", userId);
        } else {
            // 收到的评论：别人对我文章的评论
            wrapper.inSql("article_id", "select id from article where author_id = " + userId);
            wrapper.ne("user_id", userId); // 排除自己对自己文章的评论
        }
        
        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();
        if (start != null && end == null) {
            end = start;
        } else if (start == null && end != null) {
            start = end;
        }
        if (start != null) {
            LocalDateTime startTime = start.atStartOfDay();
            LocalDateTime endTime = end.plusDays(1).atStartOfDay();
            wrapper.ge("created_at", startTime);
            wrapper.lt("created_at", endTime);
        }
        
        wrapper.orderByDesc("created_at");
        
        Page<Comment> resultPage = commentMapper.selectPage(page, wrapper);
        resultPage.getRecords().forEach(comment -> enrichComment(comment));
        
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords(),
                resultPage.getCurrent(), resultPage.getSize());
    }
    
    private void enrichComment(Comment comment) {
        // 填充评论用户
        comment.setUser(userService.getUserInfo(comment.getUserId()));
        if (comment.getReplyToUserId() != null) {
            comment.setReplyToUser(userService.getUserInfo(comment.getReplyToUserId()));
        }
        // 填充文章
        Article article = articleService.getById(comment.getArticleId());
        comment.setArticle(article);
    }
}
