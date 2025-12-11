package com.campus.news.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.news.common.PageResult;
import com.campus.news.dto.VideoCreateRequest;
import com.campus.news.dto.VideoQueryRequest;
import com.campus.news.entity.User;
import com.campus.news.entity.Video;
import com.campus.news.entity.VideoCategory;
import com.campus.news.exception.BusinessException;
import com.campus.news.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoService extends ServiceImpl<VideoMapper, Video> {
    
    private final VideoMapper videoMapper;
    private final UserService userService;
    private final VideoCategoryService videoCategoryService;
    private final VideoLikeService videoLikeService;
    
    @Transactional
    public Video createVideo(VideoCreateRequest request, Long userId) {
        User user = userService.getUserInfo(userId);
        
        Video video = new Video();
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setVideoUrl(request.getVideoUrl());
        video.setThumbnail(request.getThumbnail());
        video.setDuration(request.getDuration());
        video.setDurationSeconds(request.getDurationSeconds());
        video.setFileSize(request.getFileSize());
        video.setCategoryId(request.getCategoryId());
        video.setAuthorId(userId);
        video.setChannelName(request.getChannelName() != null ? request.getChannelName() : user.getRealName());
        video.setViewCount(0);
        video.setLikeCount(0);
        video.setCommentCount(0);
        video.setIsApproved(1); // 自动审核通过
        video.setStatus(1);
        
        videoMapper.insert(video);
        return video;
    }
    
    public PageResult<Video> getVideoList(VideoQueryRequest request, Long currentUserId) {
        Page<Video> page = new Page<>(request.getCurrent(), request.getSize());
        QueryWrapper<Video> wrapper = new QueryWrapper<>();
        
        // 分类筛选
        if (request.getCategoryId() != null) {
            wrapper.eq("category_id", request.getCategoryId());
        }
        if (request.getCategoryCode() != null && !request.getCategoryCode().isEmpty()) {
            VideoCategory category = videoCategoryService.getByCode(request.getCategoryCode());
            if (category != null) {
                wrapper.eq("category_id", category.getId());
            }
        }
        
        if (request.getAuthorId() != null) {
            wrapper.eq("author_id", request.getAuthorId());
        }
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like("title", request.getKeyword())
                    .or().like("description", request.getKeyword()));
        }
        if (request.getIsApproved() != null) {
            wrapper.eq("is_approved", request.getIsApproved());
        } else {
            // 默认只显示已审核通过的视频
            wrapper.eq("is_approved", 1);
        }
        
        // 排序
        String sortBy = request.getSortBy();
        boolean isAsc = "asc".equalsIgnoreCase(request.getSortOrder());
        
        if ("views".equals(sortBy)) {
            if (isAsc) {
                wrapper.orderByAsc("view_count");
            } else {
                wrapper.orderByDesc("view_count");
            }
        } else {
            if (isAsc) {
                wrapper.orderByAsc("created_at");
            } else {
                wrapper.orderByDesc("created_at");
            }
        }
        
        Page<Video> resultPage = videoMapper.selectPage(page, wrapper);
        
        // 填充关联数据
        resultPage.getRecords().forEach(video -> enrichVideo(video, currentUserId));
        
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords(),
                resultPage.getCurrent(), resultPage.getSize());
    }
    
    public Video getVideoDetail(Long id, Long currentUserId) {
        Video video = videoMapper.selectById(id);
        if (video == null) {
            throw new BusinessException("视频不存在");
        }
        
        // 增加播放量
        incrementViewCount(id);
        video.setViewCount(video.getViewCount() + 1);
        
        enrichVideo(video, currentUserId);
        return video;
    }
    
    private void enrichVideo(Video video, Long currentUserId) {
        video.setAuthor(userService.getUserInfo(video.getAuthorId()));
        if (video.getCategoryId() != null) {
            video.setCategory(videoCategoryService.getById(video.getCategoryId()));
        }
        if (currentUserId != null) {
            video.setIsLiked(videoLikeService.isLiked(video.getId(), currentUserId));
        }
    }
    
    private void incrementViewCount(Long videoId) {
        videoMapper.update(null, new UpdateWrapper<Video>()
                .eq("id", videoId)
                .setSql("view_count = view_count + 1"));
    }
    
    @Transactional
    public boolean updateVideo(Long id, VideoCreateRequest request, Long userId) {
        Video video = videoMapper.selectById(id);
        if (video == null) {
            throw new BusinessException("视频不存在");
        }
        
        User user = userService.getUserInfo(userId);
        List<String> roleNames = user.getRoles().stream().map(r -> r.getRoleName()).toList();
        
        if (!video.getAuthorId().equals(userId) && !roleNames.contains("ADMIN")) {
            throw new BusinessException("无权限修改该视频");
        }
        
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setThumbnail(request.getThumbnail());
        video.setCategoryId(request.getCategoryId());
        video.setChannelName(request.getChannelName());
        
        return videoMapper.updateById(video) > 0;
    }
    
    @Transactional
    public boolean deleteVideo(Long id, Long userId) {
        Video video = videoMapper.selectById(id);
        if (video == null) {
            throw new BusinessException("视频不存在");
        }
        
        User user = userService.getUserInfo(userId);
        List<String> roleNames = user.getRoles().stream().map(r -> r.getRoleName()).toList();
        
        if (!video.getAuthorId().equals(userId) && !roleNames.contains("ADMIN")) {
            throw new BusinessException("无权限删除该视频");
        }
        
        return videoMapper.deleteById(id) > 0;
    }
    
    @Transactional
    public boolean approveVideo(Long id, Integer isApproved) {
        Video video = new Video();
        video.setId(id);
        video.setIsApproved(isApproved);
        return videoMapper.updateById(video) > 0;
    }
    
    public List<Video> getHotVideos(int count) {
        QueryWrapper<Video> wrapper = new QueryWrapper<>();
        wrapper.eq("is_approved", 1)
               .eq("status", 1)
               .orderByDesc("view_count")
               .last("LIMIT " + count);
        return videoMapper.selectList(wrapper);
    }
}
