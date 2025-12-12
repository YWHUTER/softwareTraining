package com.campus.news.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 视频观看历史服务（使用Redis缓存，不需要数据库）
 */
@Service
@RequiredArgsConstructor
public class VideoHistoryService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String HISTORY_KEY_PREFIX = "video:history:";
    private static final String DISLIKE_KEY_PREFIX = "video:dislike:";
    private static final String WATCH_LATER_KEY_PREFIX = "video:watchlater:";
    private static final String PROGRESS_KEY_PREFIX = "video:progress:";
    
    private static final int MAX_HISTORY_SIZE = 100;
    private static final long HISTORY_EXPIRE_DAYS = 90;
    
    /**
     * 添加观看历史
     */
    public void addHistory(Long userId, Long videoId) {
        String key = HISTORY_KEY_PREFIX + userId;
        
        // 使用ZSet存储，score为时间戳
        double score = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(key, videoId.toString(), score);
        
        // 限制历史记录数量
        Long size = redisTemplate.opsForZSet().size(key);
        if (size != null && size > MAX_HISTORY_SIZE) {
            redisTemplate.opsForZSet().removeRange(key, 0, size - MAX_HISTORY_SIZE - 1);
        }
        
        // 设置过期时间
        redisTemplate.expire(key, HISTORY_EXPIRE_DAYS, TimeUnit.DAYS);
    }
    
    /**
     * 获取观看历史
     */
    public List<Long> getHistory(Long userId, int page, int size) {
        String key = HISTORY_KEY_PREFIX + userId;
        
        int start = (page - 1) * size;
        int end = start + size - 1;
        
        // 按时间倒序获取
        Set<Object> result = redisTemplate.opsForZSet().reverseRange(key, start, end);
        
        if (result == null || result.isEmpty()) {
            return List.of();
        }
        
        return result.stream()
                .map(obj -> Long.parseLong(obj.toString()))
                .toList();
    }
    
    /**
     * 获取历史记录总数
     */
    public long getHistoryCount(Long userId) {
        String key = HISTORY_KEY_PREFIX + userId;
        Long size = redisTemplate.opsForZSet().size(key);
        return size != null ? size : 0;
    }
    
    /**
     * 删除单条历史记录
     */
    public void removeHistory(Long userId, Long videoId) {
        String key = HISTORY_KEY_PREFIX + userId;
        redisTemplate.opsForZSet().remove(key, videoId.toString());
    }
    
    /**
     * 清空观看历史
     */
    public void clearHistory(Long userId) {
        String key = HISTORY_KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }
    
    /**
     * 标记不喜欢
     */
    public void addDislike(Long userId, Long videoId) {
        String key = DISLIKE_KEY_PREFIX + userId;
        redisTemplate.opsForSet().add(key, videoId.toString());
        redisTemplate.expire(key, HISTORY_EXPIRE_DAYS, TimeUnit.DAYS);
    }
    
    /**
     * 取消不喜欢
     */
    public void removeDislike(Long userId, Long videoId) {
        String key = DISLIKE_KEY_PREFIX + userId;
        redisTemplate.opsForSet().remove(key, videoId.toString());
    }
    
    /**
     * 检查是否不喜欢
     */
    public boolean isDisliked(Long userId, Long videoId) {
        String key = DISLIKE_KEY_PREFIX + userId;
        Boolean result = redisTemplate.opsForSet().isMember(key, videoId.toString());
        return result != null && result;
    }
    
    /**
     * 添加到稍后观看
     */
    public void addWatchLater(Long userId, Long videoId) {
        String key = WATCH_LATER_KEY_PREFIX + userId;
        double score = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(key, videoId.toString(), score);
    }
    
    /**
     * 从稍后观看移除
     */
    public void removeWatchLater(Long userId, Long videoId) {
        String key = WATCH_LATER_KEY_PREFIX + userId;
        redisTemplate.opsForZSet().remove(key, videoId.toString());
    }
    
    /**
     * 获取稍后观看列表
     */
    public List<Long> getWatchLater(Long userId, int page, int size) {
        String key = WATCH_LATER_KEY_PREFIX + userId;
        
        int start = (page - 1) * size;
        int end = start + size - 1;
        
        Set<Object> result = redisTemplate.opsForZSet().reverseRange(key, start, end);
        
        if (result == null || result.isEmpty()) {
            return List.of();
        }
        
        return result.stream()
                .map(obj -> Long.parseLong(obj.toString()))
                .toList();
    }
    
    /**
     * 检查是否在稍后观看列表
     */
    public boolean isInWatchLater(Long userId, Long videoId) {
        String key = WATCH_LATER_KEY_PREFIX + userId;
        Double score = redisTemplate.opsForZSet().score(key, videoId.toString());
        return score != null;
    }
    
    /**
     * 保存观看进度
     */
    public void saveProgress(Long userId, Long videoId, int seconds) {
        String key = PROGRESS_KEY_PREFIX + userId + ":" + videoId;
        redisTemplate.opsForValue().set(key, seconds, Duration.ofDays(30));
    }
    
    /**
     * 获取观看进度
     */
    public int getProgress(Long userId, Long videoId) {
        String key = PROGRESS_KEY_PREFIX + userId + ":" + videoId;
        Object result = redisTemplate.opsForValue().get(key);
        return result != null ? (Integer) result : 0;
    }
}
