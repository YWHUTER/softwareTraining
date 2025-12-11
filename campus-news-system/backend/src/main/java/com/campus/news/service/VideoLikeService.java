package com.campus.news.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.news.entity.Video;
import com.campus.news.entity.VideoLike;
import com.campus.news.mapper.VideoLikeMapper;
import com.campus.news.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VideoLikeService extends ServiceImpl<VideoLikeMapper, VideoLike> {
    
    private final VideoLikeMapper videoLikeMapper;
    private final VideoMapper videoMapper;
    
    public boolean isLiked(Long videoId, Long userId) {
        QueryWrapper<VideoLike> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId).eq("user_id", userId);
        return videoLikeMapper.selectCount(wrapper) > 0;
    }
    
    @Transactional
    public boolean toggleLike(Long videoId, Long userId) {
        QueryWrapper<VideoLike> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId).eq("user_id", userId);
        VideoLike existing = videoLikeMapper.selectOne(wrapper);
        
        if (existing != null) {
            // 取消点赞
            videoLikeMapper.deleteById(existing.getId());
            updateLikeCount(videoId, -1);
            return false;
        } else {
            // 点赞
            VideoLike like = new VideoLike();
            like.setVideoId(videoId);
            like.setUserId(userId);
            videoLikeMapper.insert(like);
            updateLikeCount(videoId, 1);
            return true;
        }
    }
    
    private void updateLikeCount(Long videoId, int delta) {
        videoMapper.update(null, new UpdateWrapper<Video>()
                .eq("id", videoId)
                .setSql("like_count = like_count + " + delta));
    }
}
