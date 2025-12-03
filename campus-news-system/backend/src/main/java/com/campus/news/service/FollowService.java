package com.campus.news.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.news.common.PageResult;
import com.campus.news.entity.Article;
import com.campus.news.entity.User;
import com.campus.news.entity.UserFollow;
import com.campus.news.mapper.ArticleMapper;
import com.campus.news.mapper.UserFollowMapper;
import com.campus.news.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 关注服务
 */
@Service
@RequiredArgsConstructor
public class FollowService {
    
    private final UserFollowMapper userFollowMapper;
    private final UserMapper userMapper;
    private final ArticleMapper articleMapper;
    @Lazy
    private final RealtimeNotificationService realtimeNotificationService;
    
    /**
     * 关注/取消关注用户
     */
    @Transactional
    public boolean toggleFollow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new RuntimeException("不能关注自己");
        }
        
        QueryWrapper<UserFollow> wrapper = new QueryWrapper<>();
        wrapper.eq("follower_id", followerId)
               .eq("following_id", followingId);
        
        UserFollow existing = userFollowMapper.selectOne(wrapper);
        
        if (existing != null) {
            // 取消关注
            userFollowMapper.deleteById(existing.getId());
            // 更新计数
            updateFollowCount(followerId, followingId, -1);
            return false;
        } else {
            // 添加关注
            UserFollow follow = new UserFollow();
            follow.setFollowerId(followerId);
            follow.setFollowingId(followingId);
            userFollowMapper.insert(follow);
            // 更新计数
            updateFollowCount(followerId, followingId, 1);
            
            // 🔔 发送实时通知
            realtimeNotificationService.sendFollowNotification(followingId, followerId);
            
            return true;
        }
    }
    
    /**
     * 更新关注数和粉丝数
     */
    private void updateFollowCount(Long followerId, Long followingId, int delta) {
        // 更新关注者的关注数
        User follower = userMapper.selectById(followerId);
        if (follower != null) {
            int newCount = (follower.getFollowingCount() == null ? 0 : follower.getFollowingCount()) + delta;
            follower.setFollowingCount(Math.max(0, newCount));
            userMapper.updateById(follower);
        }
        
        // 更新被关注者的粉丝数
        User following = userMapper.selectById(followingId);
        if (following != null) {
            int newCount = (following.getFollowerCount() == null ? 0 : following.getFollowerCount()) + delta;
            following.setFollowerCount(Math.max(0, newCount));
            userMapper.updateById(following);
        }
    }
    
    /**
     * 检查是否已关注
     */
    public boolean isFollowing(Long followerId, Long followingId) {
        QueryWrapper<UserFollow> wrapper = new QueryWrapper<>();
        wrapper.eq("follower_id", followerId)
               .eq("following_id", followingId);
        return userFollowMapper.selectCount(wrapper) > 0;
    }
    
    /**
     * 获取关注列表（我关注的人）
     */
    public PageResult<User> getFollowingList(Long userId, int current, int size) {
        // 获取关注的用户ID列表
        QueryWrapper<UserFollow> followWrapper = new QueryWrapper<>();
        followWrapper.eq("follower_id", userId)
                     .orderByDesc("created_at");
        
        Page<UserFollow> followPage = new Page<>(current, size);
        Page<UserFollow> followResult = userFollowMapper.selectPage(followPage, followWrapper);
        
        List<User> users = new ArrayList<>();
        for (UserFollow follow : followResult.getRecords()) {
            User user = userMapper.selectById(follow.getFollowingId());
            if (user != null) {
                user.setPassword(null);
                user.setIsFollowed(true);
                users.add(user);
            }
        }
        
        return new PageResult<>(followResult.getTotal(), users, followResult.getCurrent(), followResult.getSize());
    }
    
    /**
     * 获取粉丝列表
     */
    public PageResult<User> getFollowerList(Long userId, int current, int size, Long currentUserId) {
        // 获取粉丝的用户ID列表
        QueryWrapper<UserFollow> followWrapper = new QueryWrapper<>();
        followWrapper.eq("following_id", userId)
                     .orderByDesc("created_at");
        
        Page<UserFollow> followPage = new Page<>(current, size);
        Page<UserFollow> followResult = userFollowMapper.selectPage(followPage, followWrapper);
        
        List<User> users = new ArrayList<>();
        for (UserFollow follow : followResult.getRecords()) {
            User user = userMapper.selectById(follow.getFollowerId());
            if (user != null) {
                user.setPassword(null);
                // 检查当前用户是否关注了这个粉丝
                if (currentUserId != null) {
                    user.setIsFollowed(isFollowing(currentUserId, user.getId()));
                }
                users.add(user);
            }
        }
        
        return new PageResult<>(followResult.getTotal(), users, followResult.getCurrent(), followResult.getSize());
    }
    
    /**
     * 获取关注的人发布的文章（动态流）
     */
    public PageResult<Article> getFollowingArticles(Long userId, int current, int size) {
        // 获取关注的用户ID列表
        QueryWrapper<UserFollow> followWrapper = new QueryWrapper<>();
        followWrapper.eq("follower_id", userId);
        List<UserFollow> follows = userFollowMapper.selectList(followWrapper);
        
        if (follows.isEmpty()) {
            return new PageResult<>(0L, new ArrayList<>(), (long) current, (long) size);
        }
        
        List<Long> followingIds = follows.stream()
                .map(UserFollow::getFollowingId)
                .collect(Collectors.toList());
        
        // 查询这些用户的文章
        QueryWrapper<Article> articleWrapper = new QueryWrapper<>();
        articleWrapper.in("author_id", followingIds)
                      .eq("is_approved", 1)
                      .orderByDesc("created_at");
        
        Page<Article> page = new Page<>(current, size);
        Page<Article> result = articleMapper.selectPage(page, articleWrapper);
        
        // 填充作者信息
        for (Article article : result.getRecords()) {
            User author = userMapper.selectById(article.getAuthorId());
            if (author != null) {
                author.setPassword(null);
                article.setAuthor(author);
            }
        }
        
        return new PageResult<>(result.getTotal(), result.getRecords(), result.getCurrent(), result.getSize());
    }
    
    /**
     * 获取推荐用户（可以关注的用户）
     */
    public List<User> getRecommendUsers(Long userId, int limit) {
        // 获取已关注的用户ID
        QueryWrapper<UserFollow> followWrapper = new QueryWrapper<>();
        followWrapper.eq("follower_id", userId);
        List<UserFollow> follows = userFollowMapper.selectList(followWrapper);
        List<Long> excludeIds = follows.stream()
                .map(UserFollow::getFollowingId)
                .collect(Collectors.toList());
        excludeIds.add(userId); // 排除自己
        
        // 查询有发布文章的用户（简化查询）
        QueryWrapper<User> userWrapper = new QueryWrapper<>();
        if (!excludeIds.isEmpty()) {
            userWrapper.notIn("id", excludeIds);
        }
        userWrapper.orderByDesc("follower_count")
                   .last("LIMIT " + (limit * 2)); // 多查一些以便筛选
        
        List<User> users = userMapper.selectList(userWrapper);
        
        List<User> recommendUsers = new ArrayList<>();
        for (User user : users) {
            if (user != null) {
                user.setPassword(null);
                user.setIsFollowed(false);
                recommendUsers.add(user);
                if (recommendUsers.size() >= limit) break;
            }
        }
        
        return recommendUsers;
    }
    
    /**
     * 获取用户统计信息（文章数、粉丝数、获赞总数）
     */
    public Map<String, Object> getUserStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            stats.put("articleCount", 0);
            stats.put("followerCount", 0);
            stats.put("totalLikes", 0);
            return stats;
        }
        
        // 文章数
        QueryWrapper<Article> articleWrapper = new QueryWrapper<>();
        articleWrapper.eq("author_id", userId).eq("is_approved", 1);
        long articleCount = articleMapper.selectCount(articleWrapper);
        
        // 粉丝数
        int followerCount = user.getFollowerCount() != null ? user.getFollowerCount() : 0;
        
        // 获赞总数（所有文章的点赞数之和）
        QueryWrapper<Article> likeWrapper = new QueryWrapper<>();
        likeWrapper.eq("author_id", userId);
        likeWrapper.select("IFNULL(SUM(like_count), 0) as total_likes");
        List<Map<String, Object>> likeResult = articleMapper.selectMaps(likeWrapper);
        long totalLikes = 0;
        if (likeResult != null && !likeResult.isEmpty() && likeResult.get(0) != null) {
            Object val = likeResult.get(0).get("total_likes");
            if (val != null) {
                totalLikes = ((Number) val).longValue();
            }
        }
        
        stats.put("articleCount", articleCount);
        stats.put("followerCount", followerCount);
        stats.put("totalLikes", totalLikes);
        
        return stats;
    }
}
