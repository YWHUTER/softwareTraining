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
    
    public PageResult<Video> getLikedVideos(Long userId, Integer current, Integer size) {
        // 获取用户点赞的视频ID列表
        List<Long> likedVideoIds = videoLikeService.getLikedVideoIds(userId);
        
        if (likedVideoIds.isEmpty()) {
            return new PageResult<>(0L, List.of(), (long) current, (long) size);
        }
        
        Page<Video> page = new Page<>(current, size);
        QueryWrapper<Video> wrapper = new QueryWrapper<>();
        wrapper.in("id", likedVideoIds)
               .eq("status", 1)
               .orderByDesc("created_at");
        
        Page<Video> resultPage = videoMapper.selectPage(page, wrapper);
        resultPage.getRecords().forEach(video -> {
            enrichVideo(video, userId);
            video.setIsLiked(true); // 肯定是点赞过的
        });
        
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords(),
                resultPage.getCurrent(), resultPage.getSize());
    }
    
    public PageResult<Video> getMyVideos(Long userId, Integer current, Integer size) {
        Page<Video> page = new Page<>(current, size);
        QueryWrapper<Video> wrapper = new QueryWrapper<>();
        wrapper.eq("author_id", userId)
               .eq("status", 1)
               .orderByDesc("created_at");
        
        Page<Video> resultPage = videoMapper.selectPage(page, wrapper);
        resultPage.getRecords().forEach(video -> enrichVideo(video, userId));
        
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords(),
                resultPage.getCurrent(), resultPage.getSize());
    }
    
    /**
     * 获取相关视频推荐（基于分类和作者）
     */
    public List<Video> getRelatedVideos(Long videoId, int count, Long currentUserId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return List.of();
        }
        
        QueryWrapper<Video> wrapper = new QueryWrapper<>();
        wrapper.ne("id", videoId)
               .eq("is_approved", 1)
               .eq("status", 1);
        
        // 优先同分类的视频
        if (video.getCategoryId() != null) {
            wrapper.and(w -> w.eq("category_id", video.getCategoryId())
                    .or().eq("author_id", video.getAuthorId()));
        } else {
            wrapper.eq("author_id", video.getAuthorId());
        }
        
        wrapper.orderByDesc("view_count")
               .last("LIMIT " + count);
        
        List<Video> videos = videoMapper.selectList(wrapper);
        
        // 如果相关视频不够，补充热门视频
        if (videos.size() < count) {
            List<Long> existingIds = videos.stream().map(Video::getId).toList();
            existingIds = new java.util.ArrayList<>(existingIds);
            existingIds.add(videoId);
            
            QueryWrapper<Video> hotWrapper = new QueryWrapper<>();
            hotWrapper.notIn("id", existingIds)
                     .eq("is_approved", 1)
                     .eq("status", 1)
                     .orderByDesc("view_count")
                     .last("LIMIT " + (count - videos.size()));
            
            videos.addAll(videoMapper.selectList(hotWrapper));
        }
        
        videos.forEach(v -> enrichVideo(v, currentUserId));
        return videos;
    }
    
    /**
     * 获取用户频道信息
     */
    public java.util.Map<String, Object> getChannelInfo(Long userId, Long currentUserId) {
        User user = userService.getUserInfo(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 统计视频数量
        QueryWrapper<Video> countWrapper = new QueryWrapper<>();
        countWrapper.eq("author_id", userId)
                   .eq("is_approved", 1)
                   .eq("status", 1);
        long videoCount = videoMapper.selectCount(countWrapper);
        
        // 统计总播放量
        QueryWrapper<Video> viewWrapper = new QueryWrapper<>();
        viewWrapper.eq("author_id", userId)
                  .eq("is_approved", 1)
                  .eq("status", 1)
                  .select("COALESCE(SUM(view_count), 0) as total_views");
        java.util.Map<String, Object> viewResult = videoMapper.selectMaps(viewWrapper).stream().findFirst().orElse(null);
        long totalViews = viewResult != null && viewResult.get("total_views") != null 
                ? ((Number) viewResult.get("total_views")).longValue() : 0;
        
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("user", user);
        result.put("videoCount", videoCount);
        result.put("totalViews", totalViews);
        result.put("subscriberCount", user.getFollowerCount());
        
        return result;
    }
    
    /**
     * 获取频道视频列表
     */
    public PageResult<Video> getChannelVideos(Long userId, Integer current, Integer size, 
                                               String sortBy, Long currentUserId) {
        Page<Video> page = new Page<>(current, size);
        QueryWrapper<Video> wrapper = new QueryWrapper<>();
        wrapper.eq("author_id", userId)
               .eq("is_approved", 1)
               .eq("status", 1);
        
        // 排序
        if ("popular".equals(sortBy)) {
            wrapper.orderByDesc("view_count");
        } else {
            wrapper.orderByDesc("created_at");
        }
        
        Page<Video> resultPage = videoMapper.selectPage(page, wrapper);
        resultPage.getRecords().forEach(video -> enrichVideo(video, currentUserId));
        
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords(),
                resultPage.getCurrent(), resultPage.getSize());
    }
    
    /**
     * 高级搜索视频
     */
    public PageResult<Video> searchVideos(String keyword, Integer current, Integer size,
                                          String categoryCode, String sortBy, String duration,
                                          String uploadDate, Long currentUserId) {
        Page<Video> page = new Page<>(current, size);
        QueryWrapper<Video> wrapper = new QueryWrapper<>();
        
        wrapper.eq("is_approved", 1)
               .eq("status", 1);
        
        // 关键词搜索
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("title", keyword)
                    .or().like("description", keyword)
                    .or().like("channel_name", keyword));
        }
        
        // 分类筛选
        if (categoryCode != null && !categoryCode.isEmpty()) {
            VideoCategory category = videoCategoryService.getByCode(categoryCode);
            if (category != null) {
                wrapper.eq("category_id", category.getId());
            }
        }
        
        // 时长筛选
        if (duration != null) {
            switch (duration) {
                case "short" -> wrapper.lt("duration_seconds", 240); // 小于4分钟
                case "medium" -> wrapper.between("duration_seconds", 240, 1200); // 4-20分钟
                case "long" -> wrapper.gt("duration_seconds", 1200); // 大于20分钟
            }
        }
        
        // 上传时间筛选
        if (uploadDate != null) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            switch (uploadDate) {
                case "hour" -> wrapper.gt("created_at", now.minusHours(1));
                case "today" -> wrapper.gt("created_at", now.toLocalDate().atStartOfDay());
                case "week" -> wrapper.gt("created_at", now.minusWeeks(1));
                case "month" -> wrapper.gt("created_at", now.minusMonths(1));
                case "year" -> wrapper.gt("created_at", now.minusYears(1));
            }
        }
        
        // 排序
        switch (sortBy != null ? sortBy : "relevance") {
            case "date" -> wrapper.orderByDesc("created_at");
            case "views" -> wrapper.orderByDesc("view_count");
            case "rating" -> wrapper.orderByDesc("like_count");
            default -> {
                // relevance: 综合排序（标题匹配优先 + 播放量）
                if (keyword != null && !keyword.isEmpty()) {
                    wrapper.orderByDesc("CASE WHEN title LIKE '%" + keyword + "%' THEN 1 ELSE 0 END");
                }
                wrapper.orderByDesc("view_count");
            }
        }
        
        Page<Video> resultPage = videoMapper.selectPage(page, wrapper);
        resultPage.getRecords().forEach(video -> enrichVideo(video, currentUserId));
        
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords(),
                resultPage.getCurrent(), resultPage.getSize());
    }
    
    /**
     * 获取搜索建议
     */
    public List<String> getSearchSuggestions(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return List.of();
        }
        
        QueryWrapper<Video> wrapper = new QueryWrapper<>();
        wrapper.like("title", keyword)
               .eq("is_approved", 1)
               .eq("status", 1)
               .select("DISTINCT title")
               .orderByDesc("view_count")
               .last("LIMIT 10");
        
        return videoMapper.selectList(wrapper)
                .stream()
                .map(Video::getTitle)
                .toList();
    }
    
    /**
     * 获取最新视频
     */
    public List<Video> getLatestVideos(int count, Long currentUserId) {
        QueryWrapper<Video> wrapper = new QueryWrapper<>();
        wrapper.eq("is_approved", 1)
               .eq("status", 1)
               .orderByDesc("created_at")
               .last("LIMIT " + count);
        
        List<Video> videos = videoMapper.selectList(wrapper);
        videos.forEach(v -> enrichVideo(v, currentUserId));
        return videos;
    }
    
    /**
     * 获取视频统计信息
     */
    public java.util.Map<String, Object> getVideoStats(Long videoId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new BusinessException("视频不存在");
        }
        
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("viewCount", video.getViewCount());
        stats.put("likeCount", video.getLikeCount());
        stats.put("commentCount", video.getCommentCount());
        stats.put("createdAt", video.getCreatedAt());
        
        return stats;
    }
    
    /**
     * 根据ID列表获取视频（保持顺序）
     */
    public List<Video> getVideosByIds(List<Long> videoIds, Long currentUserId) {
        if (videoIds == null || videoIds.isEmpty()) {
            return List.of();
        }
        
        QueryWrapper<Video> wrapper = new QueryWrapper<>();
        wrapper.in("id", videoIds)
               .eq("status", 1);
        
        List<Video> videos = videoMapper.selectList(wrapper);
        
        // 按照传入的ID顺序排序
        java.util.Map<Long, Video> videoMap = videos.stream()
                .collect(java.util.stream.Collectors.toMap(Video::getId, v -> v));
        
        List<Video> orderedVideos = videoIds.stream()
                .map(videoMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        
        orderedVideos.forEach(video -> enrichVideo(video, currentUserId));
        return orderedVideos;
    }
}
