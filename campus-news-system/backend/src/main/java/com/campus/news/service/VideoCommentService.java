package com.campus.news.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.news.common.PageResult;
import com.campus.news.entity.VideoComment;
import com.campus.news.entity.VideoCommentLike;
import com.campus.news.entity.Video;
import com.campus.news.exception.BusinessException;
import com.campus.news.mapper.VideoCommentMapper;
import com.campus.news.mapper.VideoCommentLikeMapper;
import com.campus.news.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoCommentService extends ServiceImpl<VideoCommentMapper, VideoComment> {
    
    private final VideoCommentMapper commentMapper;
    private final VideoCommentLikeMapper commentLikeMapper;
    private final VideoMapper videoMapper;
    private final UserService userService;
    
    @Transactional
    public VideoComment createComment(Long videoId, String content, Long parentId, Long userId) {
        // 验证视频存在
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new BusinessException("视频不存在");
        }
        
        VideoComment comment = new VideoComment();
        comment.setVideoId(videoId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setLikeCount(0);
        comment.setStatus(1);
        
        // 如果是回复，设置回复目标用户
        if (parentId != null) {
            VideoComment parent = commentMapper.selectById(parentId);
            if (parent != null) {
                comment.setReplyToUserId(parent.getUserId());
            }
        }
        
        commentMapper.insert(comment);
        
        // 更新视频评论数
        videoMapper.update(null, new UpdateWrapper<Video>()
                .eq("id", videoId)
                .setSql("comment_count = comment_count + 1"));
        
        // 填充用户信息
        comment.setUser(userService.getUserInfo(userId));
        
        return comment;
    }
    
    public PageResult<VideoComment> getComments(Long videoId, Integer current, Integer size, 
                                                 String sortBy, Long currentUserId) {
        Page<VideoComment> page = new Page<>(current, size);
        QueryWrapper<VideoComment> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId)
               .eq("status", 1)
               .isNull("parent_id"); // 只查询顶级评论
        
        // 排序
        if ("hot".equals(sortBy)) {
            wrapper.orderByDesc("like_count");
        } else {
            wrapper.orderByDesc("created_at");
        }
        
        Page<VideoComment> resultPage = commentMapper.selectPage(page, wrapper);
        
        // 填充用户信息和回复
        resultPage.getRecords().forEach(comment -> {
            enrichComment(comment, currentUserId);
            // 加载回复（最多3条）
            comment.setReplies(getReplies(comment.getId(), 3, currentUserId));
        });
        
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords(),
                resultPage.getCurrent(), resultPage.getSize());
    }
    
    public List<VideoComment> getReplies(Long parentId, int limit, Long currentUserId) {
        QueryWrapper<VideoComment> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", parentId)
               .eq("status", 1)
               .orderByAsc("created_at")
               .last("LIMIT " + limit);
        
        List<VideoComment> replies = commentMapper.selectList(wrapper);
        replies.forEach(reply -> enrichComment(reply, currentUserId));
        return replies;
    }
    
    public List<VideoComment> getAllReplies(Long parentId, Long currentUserId) {
        QueryWrapper<VideoComment> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", parentId)
               .eq("status", 1)
               .orderByAsc("created_at");
        
        List<VideoComment> replies = commentMapper.selectList(wrapper);
        replies.forEach(reply -> enrichComment(reply, currentUserId));
        return replies;
    }
    
    private void enrichComment(VideoComment comment, Long currentUserId) {
        comment.setUser(userService.getUserInfo(comment.getUserId()));
        if (comment.getReplyToUserId() != null) {
            comment.setReplyToUser(userService.getUserInfo(comment.getReplyToUserId()));
        }
        if (currentUserId != null) {
            comment.setIsLiked(isCommentLiked(comment.getId(), currentUserId));
        }
    }
    
    @Transactional
    public boolean deleteComment(Long commentId, Long userId) {
        VideoComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        
        // 只有评论作者可以删除
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("无权限删除该评论");
        }
        
        // 软删除
        comment.setStatus(0);
        commentMapper.updateById(comment);
        
        // 更新视频评论数
        videoMapper.update(null, new UpdateWrapper<Video>()
                .eq("id", comment.getVideoId())
                .setSql("comment_count = comment_count - 1"));
        
        return true;
    }
    
    @Transactional
    public boolean toggleCommentLike(Long commentId, Long userId) {
        QueryWrapper<VideoCommentLike> wrapper = new QueryWrapper<>();
        wrapper.eq("comment_id", commentId).eq("user_id", userId);
        VideoCommentLike existing = commentLikeMapper.selectOne(wrapper);
        
        if (existing != null) {
            // 取消点赞
            commentLikeMapper.deleteById(existing.getId());
            commentMapper.update(null, new UpdateWrapper<VideoComment>()
                    .eq("id", commentId)
                    .setSql("like_count = like_count - 1"));
            return false;
        } else {
            // 点赞
            VideoCommentLike like = new VideoCommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            commentLikeMapper.insert(like);
            commentMapper.update(null, new UpdateWrapper<VideoComment>()
                    .eq("id", commentId)
                    .setSql("like_count = like_count + 1"));
            return true;
        }
    }
    
    public boolean isCommentLiked(Long commentId, Long userId) {
        QueryWrapper<VideoCommentLike> wrapper = new QueryWrapper<>();
        wrapper.eq("comment_id", commentId).eq("user_id", userId);
        return commentLikeMapper.selectCount(wrapper) > 0;
    }
    
    public int getCommentCount(Long videoId) {
        QueryWrapper<VideoComment> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId).eq("status", 1);
        return commentMapper.selectCount(wrapper).intValue();
    }
}
