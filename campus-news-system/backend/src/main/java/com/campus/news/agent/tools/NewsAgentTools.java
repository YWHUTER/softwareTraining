package com.campus.news.agent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.news.entity.*;
import com.campus.news.mapper.*;
import com.campus.news.service.*;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent工具类 - 提供给AI Agent使用的各种工具方法
 * 使用@Tool注解标记的方法会被LangChain4j自动识别为可调用工具
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsAgentTools {

    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;
    private final UserFollowMapper userFollowMapper;
    private final ArticleLikeMapper articleLikeMapper;
    private final ArticleFavoriteMapper articleFavoriteMapper;
    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;
    private final NotificationMapper notificationMapper;
    private final ArticleService articleService;
    private final UserService userService;
    private final CommentService commentService;
    private final ArticleFavoriteService articleFavoriteService;
    private final FollowService followService;
    private final TagService tagService;

    /**
     * 搜索文章
     * @param keyword 搜索关键词
     * @param limit 返回数量限制
     * @return 文章列表描述
     */
    @Tool("搜索新闻文章，可以根据关键词搜索标题和内容")
    public String searchArticles(String keyword, int limit) {
        log.info("Agent工具：搜索文章 - 关键词: {}, 限制: {}", keyword, limit);
        
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(Article::getTitle, keyword)
                        .or().like(Article::getContent, keyword))
               .eq(Article::getStatus, 1)  // 只搜索已发布的
               .orderByDesc(Article::getCreatedAt)
               .last("LIMIT " + Math.min(limit, 20));  // 最多返回20条
        
        List<Article> articles = articleMapper.selectList(wrapper);
        
        if (articles.isEmpty()) {
            return "没有找到包含关键词 \"" + keyword + "\" 的文章。";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("找到 ").append(articles.size()).append(" 篇相关文章：\n\n");
        
        for (int i = 0; i < articles.size(); i++) {
            Article article = articles.get(i);
            result.append((i + 1)).append(". 《").append(article.getTitle()).append("》\n");
            result.append("   - 作者：").append(getUserName(article.getAuthorId())).append("\n");
            result.append("   - 版块：").append(getBoardTypeName(article.getBoardType())).append("\n");
            result.append("   - 浏览量：").append(article.getViewCount()).append("\n");
            result.append("   - 发布时间：").append(article.getCreatedAt()).append("\n");
            result.append("   - 链接：/article/").append(article.getId()).append("\n");
            
            // 添加摘要
            String summary = article.getSummary();
            if (summary != null && !summary.isEmpty()) {
                result.append("   - 摘要：").append(summary.length() > 100 ? 
                    summary.substring(0, 100) + "..." : summary).append("\n");
            }
            result.append("\n");
        }
        
        return result.toString();
    }

    /**
     * 获取热门文章
     * @param limit 返回数量
     * @return 热门文章列表
     */
    @Tool("获取浏览量最高的热门文章排行榜")
    public String getHotArticles(int limit) {
        log.info("Agent工具：获取热门文章 - 限制: {}", limit);
        
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, 1)
               .orderByDesc(Article::getViewCount)
               .last("LIMIT " + Math.min(limit, 10));
        
        List<Article> articles = articleMapper.selectList(wrapper);
        
        StringBuilder result = new StringBuilder("🔥 热门文章排行榜：\n\n");
        for (int i = 0; i < articles.size(); i++) {
            Article article = articles.get(i);
            String medal = i < 3 ? (i == 0 ? "🥇" : i == 1 ? "🥈" : "🥉") : "  ";
            result.append(medal).append(" ").append((i + 1)).append(". 《")
                  .append(article.getTitle()).append("》\n");
            result.append("      浏览量：").append(article.getViewCount())
                  .append(" | 点赞：").append(article.getLikeCount())
                  .append(" | 评论：").append(article.getCommentCount()).append("\n");
            result.append("      链接：/article/").append(article.getId()).append("\n\n");
        }
        
        return result.toString();
    }

    /**
     * 获取系统统计数据
     * @return 统计信息
     */
    @Tool("获取系统的统计数据，包括文章总数、用户总数、浏览量等")
    public String getSystemStats() {
        log.info("Agent工具：获取系统统计");
        
        // 文章统计
        Long totalArticles = articleMapper.selectCount(null);
        Long publishedArticles = articleMapper.selectCount(
            new LambdaQueryWrapper<Article>().eq(Article::getStatus, 1)
        );
        
        // 用户统计
        Long totalUsers = userMapper.selectCount(null);
        
        // 评论统计
        Long totalComments = commentMapper.selectCount(null);
        
        // 计算总浏览量
        LambdaQueryWrapper<Article> viewWrapper = new LambdaQueryWrapper<>();
        viewWrapper.select(Article::getViewCount);
        List<Article> articleViews = articleMapper.selectList(viewWrapper);
        long totalViews = articleViews.stream()
            .mapToLong(Article::getViewCount)
            .sum();
        
        // 今日数据（简化处理）
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        Long todayArticles = articleMapper.selectCount(
            new LambdaQueryWrapper<Article>()
                .ge(Article::getCreatedAt, today)
        );
        
        StringBuilder result = new StringBuilder("📊 系统数据统计：\n\n");
        result.append("📝 文章统计：\n");
        result.append("  • 总文章数：").append(totalArticles).append(" 篇\n");
        result.append("  • 已发布：").append(publishedArticles).append(" 篇\n");
        result.append("  • 今日新增：").append(todayArticles).append(" 篇\n\n");
        
        result.append("👥 用户统计：\n");
        result.append("  • 注册用户：").append(totalUsers).append(" 人\n\n");
        
        result.append("📊 互动数据：\n");
        result.append("  • 总浏览量：").append(totalViews).append(" 次\n");
        result.append("  • 总评论数：").append(totalComments).append(" 条\n");
        
        return result.toString();
    }

    /**
     * 创建文章草稿
     * @param title 标题
     * @param content 内容
     * @param summary 摘要
     * @param boardType 版块类型（OFFICIAL/CAMPUS/COLLEGE）
     * @return 创建结果
     */
    @Tool("创建新的文章草稿，需要提供标题、内容、摘要和版块类型")
    public String createArticleDraft(String title, String content, String summary, String boardType) {
        log.info("Agent工具：创建文章草稿 - 标题: {}", title);
        
        try {
            // 验证版块类型
            if (!"OFFICIAL".equalsIgnoreCase(boardType) && 
                !"CAMPUS".equalsIgnoreCase(boardType) && 
                !"COLLEGE".equalsIgnoreCase(boardType)) {
                return "错误：无效的版块类型 \"" + boardType + "\"，请使用 OFFICIAL（官方）、CAMPUS（校园）或 COLLEGE（学院）。";
            }
            
            // 创建文章（这里简化处理，实际应该通过Service层）
            Article article = new Article();
            article.setTitle(title);
            article.setContent(content);
            article.setSummary(summary);
            article.setBoardType(boardType.toUpperCase());
            article.setAuthorId(1L); // 默认使用系统用户，实际应该从上下文获取
            article.setStatus(1); // 正常状态
            article.setViewCount(0);
            article.setLikeCount(0);
            article.setCommentCount(0);
            article.setIsApproved(0); // 待审核
            article.setIsPinned(0);
            article.setCreatedAt(LocalDateTime.now());
            article.setUpdatedAt(LocalDateTime.now());
            
            articleMapper.insert(article);
            
            return "✅ 文章草稿创建成功！\n" +
                   "标题：《" + title + "》\n" +
                   "版块：" + getBoardTypeName(boardType) + "\n" +
                   "文章ID：" + article.getId() + "\n" +
                   "状态：草稿\n" +
                   "提示：草稿已保存，需要管理员审核后才能发布。";
        } catch (Exception e) {
            log.error("创建文章失败", e);
            return "❌ 创建文章失败：" + e.getMessage();
        }
    }

    /**
     * 获取用户排行榜
     * @param type 排行类型（followers/articles）
     * @param limit 数量限制
     * @return 排行榜信息
     */
    @Tool("获取用户排行榜，可以按粉丝数或文章数排序")
    public String getUserRanking(String type, int limit) {
        log.info("Agent工具：获取用户排行 - 类型: {}, 限制: {}", type, limit);
        
        List<User> users;
        String title;
        
        if ("followers".equalsIgnoreCase(type)) {
            // 按粉丝数排序
            users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                    .orderByDesc(User::getFollowerCount)
                    .last("LIMIT " + Math.min(limit, 10))
            );
            title = "👥 粉丝排行榜：";
        } else if ("articles".equalsIgnoreCase(type)) {
            // 按文章数排序（需要关联查询，这里简化处理）
            users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                    .orderByDesc(User::getId) // 简化处理
                    .last("LIMIT " + Math.min(limit, 10))
            );
            title = "✍️ 创作排行榜：";
        } else {
            return "请指定正确的排行类型：followers（粉丝数）或 articles（文章数）";
        }
        
        StringBuilder result = new StringBuilder(title).append("\n\n");
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            String medal = i < 3 ? (i == 0 ? "🥇" : i == 1 ? "🥈" : "🥉") : "  ";
            result.append(medal).append(" ").append((i + 1)).append(". ")
                  .append(user.getRealName() != null ? user.getRealName() : user.getUsername())
                  .append("\n");
            
            if ("followers".equalsIgnoreCase(type)) {
                result.append("      粉丝：").append(user.getFollowerCount()).append(" 人\n");
            }
            
            // 获取用户文章数
            Long articleCount = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>()
                    .eq(Article::getAuthorId, user.getId())
                    .eq(Article::getStatus, 1)
            );
            result.append("      文章：").append(articleCount).append(" 篇\n\n");
        }
        
        return result.toString();
    }

    /**
     * 获取最新文章
     * @param limit 数量限制
     * @return 最新文章列表
     */
    @Tool("获取最新发布的文章列表")
    public String getLatestArticles(int limit) {
        log.info("Agent工具：获取最新文章 - 限制: {}", limit);
        
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, 1)
               .orderByDesc(Article::getCreatedAt)
               .last("LIMIT " + Math.min(limit, 10));
        
        List<Article> articles = articleMapper.selectList(wrapper);
        
        StringBuilder result = new StringBuilder("📰 最新文章：\n\n");
        for (int i = 0; i < articles.size(); i++) {
            Article article = articles.get(i);
            result.append((i + 1)).append(". 《").append(article.getTitle()).append("》\n");
            result.append("   发布于：").append(article.getCreatedAt()).append("\n");
            result.append("   作者：").append(getUserName(article.getAuthorId())).append("\n");
            result.append("   链接：/article/").append(article.getId()).append("\n\n");
        }
        
        return result.toString();
    }

    /**
     * 获取版块列表
     * @return 所有版块信息
     */
    @Tool("获取所有文章版块列表")
    public String getBoardTypes() {
        log.info("Agent工具：获取版块列表");
        
        Map<String, String> boardTypes = new HashMap<>();
        boardTypes.put("OFFICIAL", "官方版块");
        boardTypes.put("CAMPUS", "校园版块");
        boardTypes.put("COLLEGE", "学院版块");
        
        StringBuilder result = new StringBuilder("📑 文章版块列表：\n\n");
        for (Map.Entry<String, String> entry : boardTypes.entrySet()) {
            // 统计该版块下的文章数
            Long articleCount = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>()
                    .eq(Article::getBoardType, entry.getKey())
                    .eq(Article::getStatus, 1)
            );
            
            result.append("• ").append(entry.getValue())
                  .append(" (").append(articleCount).append(" 篇文章)\n");
            result.append("  代码：").append(entry.getKey()).append("\n\n");
        }
        
        return result.toString();
    }

    // 辅助方法
    private String getUserName(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            return user.getRealName() != null ? user.getRealName() : user.getUsername();
        }
        return "未知用户";
    }

    private String getBoardTypeName(String boardType) {
        if (boardType == null) return "未分类";
        switch (boardType.toUpperCase()) {
            case "OFFICIAL":
                return "官方版块";
            case "CAMPUS":
                return "校园版块";
            case "COLLEGE":
                return "学院版块";
            default:
                return boardType;
        }
    }
    
    /**
     * 点赞文章
     * @param articleId 文章ID
     * @return 点赞结果
     */
    @Tool("给指定ID的文章点赞")
    public String likeArticle(Long articleId) {
        log.info("Agent工具：点赞文章 - 文章ID: {}", articleId);
        
        try {
            // 获取文章信息
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                return "❌ 文章不存在，ID: " + articleId;
            }
            
            // 默认使用系统用户ID为1进行点赞
            Long userId = 1L;
            
            // 检查是否已经点赞
            LambdaQueryWrapper<ArticleLike> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ArticleLike::getArticleId, articleId)
                   .eq(ArticleLike::getUserId, userId);
            ArticleLike existing = articleLikeMapper.selectOne(wrapper);
            
            if (existing != null) {
                return "ℹ️ 您已经点赞过文章《" + article.getTitle() + "》了";
            }
            
            // 创建点赞记录
            ArticleLike like = new ArticleLike();
            like.setArticleId(articleId);
            like.setUserId(userId);
            articleLikeMapper.insert(like);
            
            // 更新文章点赞数
            article.setLikeCount(article.getLikeCount() + 1);
            articleMapper.updateById(article);
            
            return "👍 成功点赞文章《" + article.getTitle() + "》\n" +
                   "当前点赞数：" + article.getLikeCount();
        } catch (Exception e) {
            log.error("点赞文章失败", e);
            return "❌ 点赞失败：" + e.getMessage();
        }
    }
    
    /**
     * 取消点赞文章
     * @param articleId 文章ID
     * @return 取消点赞结果
     */
    @Tool("取消对指定ID文章的点赞")
    public String unlikeArticle(Long articleId) {
        log.info("Agent工具：取消点赞文章 - 文章ID: {}", articleId);
        
        try {
            // 获取文章信息
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                return "❌ 文章不存在，ID: " + articleId;
            }
            
            // 默认使用系统用户ID为1
            Long userId = 1L;
            
            // 查找点赞记录
            LambdaQueryWrapper<ArticleLike> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ArticleLike::getArticleId, articleId)
                   .eq(ArticleLike::getUserId, userId);
            ArticleLike existing = articleLikeMapper.selectOne(wrapper);
            
            if (existing == null) {
                return "ℹ️ 您还没有点赞文章《" + article.getTitle() + "》";
            }
            
            // 删除点赞记录
            articleLikeMapper.deleteById(existing.getId());
            
            // 更新文章点赞数
            article.setLikeCount(Math.max(0, article.getLikeCount() - 1));
            articleMapper.updateById(article);
            
            return "✅ 已取消点赞文章《" + article.getTitle() + "》\n" +
                   "当前点赞数：" + article.getLikeCount();
        } catch (Exception e) {
            log.error("取消点赞失败", e);
            return "❌ 取消点赞失败：" + e.getMessage();
        }
    }
    
    /**
     * 根据关键词搜索文章并点赞
     * @param keyword 搜索关键词
     * @return 点赞结果
     */
    @Tool("搜索包含指定关键词的文章并点赞第一篇")
    public String searchAndLikeArticle(String keyword) {
        log.info("Agent工具：搜索并点赞文章 - 关键词: {}", keyword);
        
        try {
            // 搜索文章
            LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
            wrapper.and(w -> w.like(Article::getTitle, keyword)
                            .or().like(Article::getContent, keyword))
                   .eq(Article::getStatus, 1)
                   .eq(Article::getIsApproved, 1)  // 只搜索已审核的
                   .orderByDesc(Article::getViewCount)  // 按浏览量排序
                   .last("LIMIT 1");
            
            Article article = articleMapper.selectOne(wrapper);
            
            if (article == null) {
                return "❌ 没有找到包含关键词 \"" + keyword + "\" 的文章";
            }
            
            // 点赞找到的文章
            return "🔍 找到文章《" + article.getTitle() + "》\n" +
                   likeArticle(article.getId());
        } catch (Exception e) {
            log.error("搜索并点赞失败", e);
            return "❌ 搜索并点赞失败：" + e.getMessage();
        }
    }
    
    /**
     * 发表评论
     * @param articleId 文章ID
     * @param content 评论内容
     * @return 评论结果
     */
    @Tool("给指定文章发表评论")
    public String postComment(Long articleId, String content) {
        log.info("Agent工具：发表评论 - 文章ID: {}, 内容: {}", articleId, content);
        
        try {
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                return "❌ 文章不存在，ID: " + articleId;
            }
            
            Comment comment = new Comment();
            comment.setArticleId(articleId);
            comment.setUserId(1L); // 系统用户
            comment.setContent(content);
            comment.setLikeCount(0);
            comment.setCreatedAt(LocalDateTime.now());
            comment.setUpdatedAt(LocalDateTime.now());
            
            commentMapper.insert(comment);
            
            // 更新文章评论数
            article.setCommentCount(article.getCommentCount() + 1);
            articleMapper.updateById(article);
            
            return "💬 成功在文章《" + article.getTitle() + "》下发表评论：\n" +
                   "\"" + content + "\"\n" +
                   "评论ID：" + comment.getId();
        } catch (Exception e) {
            log.error("发表评论失败", e);
            return "❌ 发表评论失败：" + e.getMessage();
        }
    }
    
    /**
     * 收藏文章
     * @param articleId 文章ID
     * @return 收藏结果
     */
    @Tool("收藏指定的文章")
    public String favoriteArticle(Long articleId) {
        log.info("Agent工具：收藏文章 - 文章ID: {}", articleId);
        
        try {
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                return "❌ 文章不存在，ID: " + articleId;
            }
            
            Long userId = 1L;
            
            // 检查是否已收藏
            LambdaQueryWrapper<ArticleFavorite> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ArticleFavorite::getArticleId, articleId)
                   .eq(ArticleFavorite::getUserId, userId);
            ArticleFavorite existing = articleFavoriteMapper.selectOne(wrapper);
            
            if (existing != null) {
                return "ℹ️ 您已经收藏过文章《" + article.getTitle() + "》了";
            }
            
            ArticleFavorite favorite = new ArticleFavorite();
            favorite.setArticleId(articleId);
            favorite.setUserId(userId);
            articleFavoriteMapper.insert(favorite);
            
            return "⭐ 成功收藏文章《" + article.getTitle() + "》";
        } catch (Exception e) {
            log.error("收藏文章失败", e);
            return "❌ 收藏失败：" + e.getMessage();
        }
    }
    
    /**
     * 获取文章评论
     * @param articleId 文章ID
     * @param limit 限制数量
     * @return 评论列表
     */
    @Tool("获取指定文章的评论列表")
    public String getArticleComments(Long articleId, int limit) {
        log.info("Agent工具：获取文章评论 - 文章ID: {}", articleId);
        
        try {
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                return "❌ 文章不存在，ID: " + articleId;
            }
            
            LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Comment::getArticleId, articleId)
                   .orderByDesc(Comment::getLikeCount)
                   .orderByDesc(Comment::getCreatedAt)
                   .last("LIMIT " + Math.min(limit, 10));
            
            List<Comment> comments = commentMapper.selectList(wrapper);
            
            if (comments.isEmpty()) {
                return "文章《" + article.getTitle() + "》暂无评论";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("文章《").append(article.getTitle()).append("》的热门评论：\n\n");
            
            for (int i = 0; i < comments.size(); i++) {
                Comment comment = comments.get(i);
                result.append(i + 1).append(". ")
                      .append(getUserName(comment.getUserId()))
                      .append("：").append(comment.getContent())
                      .append("\n   👍 ").append(comment.getLikeCount())
                      .append(" | ").append(comment.getCreatedAt()).append("\n\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("获取评论失败", e);
            return "❌ 获取评论失败：" + e.getMessage();
        }
    }
    
    /**
     * 关注用户
     * @param targetUsername 目标用户名
     * @return 关注结果
     */
    @Tool("关注指定用户")
    public String followUser(String targetUsername) {
        log.info("Agent工具：关注用户 - 用户名: {}", targetUsername);
        
        try {
            // 查找目标用户
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, targetUsername);
            User targetUser = userMapper.selectOne(wrapper);
            
            if (targetUser == null) {
                return "❌ 用户不存在：" + targetUsername;
            }
            
            Long followerId = 1L; // 系统用户
            
            // 检查是否已关注
            LambdaQueryWrapper<UserFollow> followWrapper = new LambdaQueryWrapper<>();
            followWrapper.eq(UserFollow::getFollowerId, followerId)
                        .eq(UserFollow::getFollowingId, targetUser.getId());
            UserFollow existing = userFollowMapper.selectOne(followWrapper);
            
            if (existing != null) {
                return "ℹ️ 您已经关注了用户 " + targetUsername;
            }
            
            // 创建关注关系
            UserFollow follow = new UserFollow();
            follow.setFollowerId(followerId);
            follow.setFollowingId(targetUser.getId());
            userFollowMapper.insert(follow);
            
            // 更新粉丝数
            targetUser.setFollowerCount(targetUser.getFollowerCount() + 1);
            userMapper.updateById(targetUser);
            
            return "✅ 成功关注用户 " + targetUsername + "\n" +
                   "该用户现有粉丝：" + targetUser.getFollowerCount() + " 人";
        } catch (Exception e) {
            log.error("关注用户失败", e);
            return "❌ 关注失败：" + e.getMessage();
        }
    }
    
    /**
     * 获取用户收藏的文章
     * @param limit 限制数量
     * @return 收藏文章列表
     */
    @Tool("获取我收藏的文章列表")
    public String getMyFavorites(int limit) {
        log.info("Agent工具：获取收藏文章");
        
        try {
            Long userId = 1L;
            
            LambdaQueryWrapper<ArticleFavorite> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ArticleFavorite::getUserId, userId)
                   .orderByDesc(ArticleFavorite::getCreatedAt)
                   .last("LIMIT " + Math.min(limit, 10));
            
            List<ArticleFavorite> favorites = articleFavoriteMapper.selectList(wrapper);
            
            if (favorites.isEmpty()) {
                return "您还没有收藏任何文章";
            }
            
            StringBuilder result = new StringBuilder("⭐ 我的收藏文章：\n\n");
            
            for (int i = 0; i < favorites.size(); i++) {
                Article article = articleMapper.selectById(favorites.get(i).getArticleId());
                if (article != null) {
                    result.append(i + 1).append(". 《").append(article.getTitle()).append("》\n");
                    result.append("   作者：").append(getUserName(article.getAuthorId())).append("\n");
                    result.append("   收藏时间：").append(favorites.get(i).getCreatedAt()).append("\n\n");
                }
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("获取收藏失败", e);
            return "❌ 获取收藏失败：" + e.getMessage();
        }
    }
    
    /**
     * 获取标签相关文章
     * @param tagName 标签名称
     * @param limit 限制数量
     * @return 文章列表
     */
    @Tool("获取包含指定标签的文章")
    public String getArticlesByTag(String tagName, int limit) {
        log.info("Agent工具：获取标签文章 - 标签: {}", tagName);
        
        try {
            // 查找标签
            LambdaQueryWrapper<Tag> tagWrapper = new LambdaQueryWrapper<>();
            tagWrapper.eq(Tag::getName, tagName);
            Tag tag = tagMapper.selectOne(tagWrapper);
            
            if (tag == null) {
                return "❌ 标签不存在：" + tagName;
            }
            
            // 查找包含该标签的文章ID
            LambdaQueryWrapper<ArticleTag> articleTagWrapper = new LambdaQueryWrapper<>();
            articleTagWrapper.eq(ArticleTag::getTagId, tag.getId());
            List<ArticleTag> articleTags = articleTagMapper.selectList(articleTagWrapper);
            
            if (articleTags.isEmpty()) {
                return "没有找到包含标签 #" + tagName + " 的文章";
            }
            
            List<Long> articleIds = articleTags.stream()
                .map(ArticleTag::getArticleId)
                .collect(Collectors.toList());
            
            // 查询文章详情
            LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<>();
            articleWrapper.in(Article::getId, articleIds)
                         .eq(Article::getStatus, 1)
                         .orderByDesc(Article::getViewCount)
                         .last("LIMIT " + Math.min(limit, 10));
            
            List<Article> articles = articleMapper.selectList(articleWrapper);
            
            StringBuilder result = new StringBuilder();
            result.append("🏷️ 标签 #").append(tagName).append(" 的相关文章：\n\n");
            
            for (int i = 0; i < articles.size(); i++) {
                Article article = articles.get(i);
                result.append(i + 1).append(". 《").append(article.getTitle()).append("》\n");
                result.append("   浏览量：").append(article.getViewCount()).append("\n");
                result.append("   点赞数：").append(article.getLikeCount()).append("\n\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("获取标签文章失败", e);
            return "❌ 获取标签文章失败：" + e.getMessage();
        }
    }
    
    /**
     * 获取所有标签
     * @return 标签列表
     */
    @Tool("获取系统中所有的标签列表")
    public String getAllTags() {
        log.info("Agent工具：获取所有标签");
        
        try {
            List<Tag> tags = tagMapper.selectList(null);
            
            if (tags.isEmpty()) {
                return "系统中暂无标签";
            }
            
            // 统计每个标签的文章数
            StringBuilder result = new StringBuilder("🏷️ 系统标签列表：\n\n");
            
            for (Tag tag : tags) {
                LambdaQueryWrapper<ArticleTag> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(ArticleTag::getTagId, tag.getId());
                Long articleCount = articleTagMapper.selectCount(wrapper);
                
                result.append("• #").append(tag.getName())
                      .append(" (").append(articleCount).append("篇文章)\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("获取标签失败", e);
            return "❌ 获取标签失败：" + e.getMessage();
        }
    }
    
    /**
     * 获取用户的关注列表
     * @param username 用户名
     * @param limit 限制数量
     * @return 关注列表
     */
    @Tool("查看用户的关注列表")
    public String getUserFollowing(String username, int limit) {
        log.info("Agent工具：获取用户关注列表 - 用户: {}", username);
        
        try {
            // 查找用户
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(User::getUsername, username);
            User user = userMapper.selectOne(userWrapper);
            
            if (user == null) {
                return "❌ 用户不存在：" + username;
            }
            
            // 获取关注列表
            LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserFollow::getFollowerId, user.getId())
                   .orderByDesc(UserFollow::getCreatedAt)
                   .last("LIMIT " + Math.min(limit, 10));
            
            List<UserFollow> follows = userFollowMapper.selectList(wrapper);
            
            if (follows.isEmpty()) {
                return username + " 还没有关注任何人";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("👥 ").append(username).append(" 的关注列表：\n\n");
            
            for (int i = 0; i < follows.size(); i++) {
                User followedUser = userMapper.selectById(follows.get(i).getFollowingId());
                if (followedUser != null) {
                    result.append(i + 1).append(". ").append(followedUser.getUsername())
                          .append(" (").append(followedUser.getRealName()).append(")\n");
                    result.append("   粉丝数：").append(followedUser.getFollowerCount()).append("\n\n");
                }
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("获取关注列表失败", e);
            return "❌ 获取关注列表失败：" + e.getMessage();
        }
    }
    
    /**
     * 根据文章标题模糊搜索并评论
     * @param keywords 关键词（支持模糊匹配）
     * @param comment 评论内容
     * @return 评论结果
     */
    @Tool("根据关键词模糊搜索文章并发表评论，支持不完整的标题")
    public String searchAndComment(String keywords, String comment) {
        log.info("Agent工具：模糊搜索并评论 - 关键词: {}, 评论: {}", keywords, comment);
        
        try {
            // 将关键词分割，支持多个关键词
            String[] keywordArray = keywords.split("\\s+");
            
            // 构建模糊查询
            LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Article::getStatus, 1)
                   .eq(Article::getIsApproved, 1);
            
            // 对每个关键词进行模糊匹配
            for (String keyword : keywordArray) {
                if (!keyword.trim().isEmpty()) {
                    wrapper.and(w -> w.like(Article::getTitle, keyword)
                                    .or().like(Article::getSummary, keyword));
                }
            }
            
            wrapper.orderByDesc(Article::getViewCount)  // 优先选择热门文章
                   .orderByDesc(Article::getCreatedAt)
                   .last("LIMIT 3");  // 获取前3个候选
            
            List<Article> articles = articleMapper.selectList(wrapper);
            
            if (articles.isEmpty()) {
                return "❌ 没有找到与 \"" + keywords + "\" 相关的文章\n" +
                       "💡 提示：请尝试使用更简单的关键词";
            }
            
            // 如果只有一篇，直接评论
            if (articles.size() == 1) {
                Article article = articles.get(0);
                return "🔍 找到文章《" + article.getTitle() + "》\n" +
                       postComment(article.getId(), comment);
            }
            
            // 如果有多篇，显示候选列表并选择第一篇最相关的
            StringBuilder result = new StringBuilder();
            result.append("🔍 找到多篇相关文章，将对最热门的文章进行评论：\n\n");
            
            for (int i = 0; i < articles.size(); i++) {
                Article article = articles.get(i);
                if (i == 0) {
                    result.append("✅ ");
                } else {
                    result.append("   ");
                }
                result.append((i + 1)).append(". 《").append(article.getTitle()).append("》")
                      .append(" (浏览:").append(article.getViewCount()).append(")\n");
            }
            
            // 对第一篇（最相关的）进行评论
            Article selectedArticle = articles.get(0);
            result.append("\n").append(postComment(selectedArticle.getId(), comment));
            
            return result.toString();
        } catch (Exception e) {
            log.error("模糊搜索并评论失败", e);
            return "❌ 操作失败：" + e.getMessage();
        }
    }
    
    /**
     * 智能搜索文章（支持极度模糊的查询）
     * @param vague 模糊描述
     * @param limit 限制数量
     * @return 文章列表
     */
    @Tool("根据模糊描述智能搜索文章，支持口语化表达")
    public String smartSearchArticles(String vague, int limit) {
        log.info("Agent工具：智能搜索 - 描述: {}", vague);
        
        try {
            // 提取可能的关键词
            String cleanedQuery = vague.replaceAll("[那个|这个|一篇|关于|有关|讲|说|写]", " ").trim();
            String[] keywords = cleanedQuery.split("\\s+");
            
            // 构建查询
            LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Article::getStatus, 1);
            
            // 宽松的匹配策略
            wrapper.and(w -> {
                for (String keyword : keywords) {
                    if (keyword.length() > 1) {  // 忽略单字符
                        w.or().like(Article::getTitle, keyword)
                         .or().like(Article::getSummary, keyword)
                         .or().like(Article::getContent, keyword);
                    }
                }
            });
            
            wrapper.orderByDesc(Article::getViewCount)
                   .last("LIMIT " + Math.min(limit, 10));
            
            List<Article> articles = articleMapper.selectList(wrapper);
            
            if (articles.isEmpty()) {
                return "😅 没找到相关文章，换个关键词试试？\n" +
                       "💡 提示：可以试试 \"AI\"、\"宿舍\"、\"活动\" 等关键词";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("🎯 根据 \"").append(vague).append("\" 找到以下文章：\n\n");
            
            for (int i = 0; i < articles.size(); i++) {
                Article article = articles.get(i);
                result.append(i + 1).append(". 《").append(article.getTitle()).append("》\n");
                
                // 显示摘要的前50个字符
                String summary = article.getSummary();
                if (summary != null && !summary.isEmpty()) {
                    result.append("   ").append(summary.length() > 50 ? 
                        summary.substring(0, 50) + "..." : summary).append("\n");
                }
                result.append("   📊 浏览:").append(article.getViewCount())
                      .append(" 👍:").append(article.getLikeCount())
                      .append(" 💬:").append(article.getCommentCount())
                      .append("\n\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("智能搜索失败", e);
            return "❌ 搜索失败：" + e.getMessage();
        }
    }
    
    /**
     * 根据文章标题搜索并收藏
     * @param title 文章标题
     * @return 收藏结果
     */
    @Tool("根据文章标题搜索文章并收藏")
    public String searchAndFavorite(String title) {
        log.info("Agent工具：搜索并收藏 - 标题: {}", title);
        
        try {
            // 搜索文章
            LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(Article::getTitle, title)
                   .eq(Article::getStatus, 1)
                   .eq(Article::getIsApproved, 1)
                   .orderByDesc(Article::getViewCount)
                   .last("LIMIT 1");
            
            Article article = articleMapper.selectOne(wrapper);
            
            if (article == null) {
                return "❌ 没有找到标题包含 \"" + title + "\" 的文章";
            }
            
            // 找到文章后收藏
            return "🔍 找到文章《" + article.getTitle() + "》(ID: " + article.getId() + ")\n" +
                   favoriteArticle(article.getId());
        } catch (Exception e) {
            log.error("搜索并收藏失败", e);
            return "❌ 操作失败：" + e.getMessage();
        }
    }
    
    /**
     * 获取文章详细信息
     * @param articleId 文章ID
     * @return 文章详情
     */
    @Tool("获取指定ID文章的详细信息")
    public String getArticleDetail(Long articleId) {
        log.info("Agent工具：获取文章详情 - ID: {}", articleId);
        
        try {
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                return "❌ 文章不存在，ID: " + articleId;
            }
            
            StringBuilder result = new StringBuilder();
            result.append("📄 文章详情\n");
            result.append("═".repeat(30)).append("\n");
            result.append("标题：《").append(article.getTitle()).append("》\n");
            result.append("作者：").append(getUserName(article.getAuthorId())).append("\n");
            result.append("版块：").append(getBoardTypeName(article.getBoardType())).append("\n");
            result.append("发布时间：").append(article.getCreatedAt()).append("\n");
            result.append("状态：").append(article.getIsApproved() == 1 ? "已审核" : "待审核").append("\n");
            result.append("\n📊 互动数据：\n");
            result.append("• 浏览量：").append(article.getViewCount()).append("\n");
            result.append("• 点赞数：").append(article.getLikeCount()).append("\n");
            result.append("• 评论数：").append(article.getCommentCount()).append("\n");
            result.append("\n📝 摘要：\n");
            result.append(article.getSummary() != null ? article.getSummary() : "暂无摘要").append("\n");
            result.append("\n🔗 链接：/article/").append(article.getId()).append("\n");
            
            return result.toString();
        } catch (Exception e) {
            log.error("获取文章详情失败", e);
            return "❌ 获取失败：" + e.getMessage();
        }
    }
    
    /**
     * 搜索用户
     * @param keyword 搜索关键词（用户名或真实姓名）
     * @param limit 限制数量
     * @return 用户列表
     */
    @Tool("搜索用户，支持用户名和真实姓名")
    public String searchUsers(String keyword, int limit) {
        log.info("Agent工具：搜索用户 - 关键词: {}", keyword);
        
        try {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.and(w -> w.like(User::getUsername, keyword)
                            .or().like(User::getRealName, keyword))
                   .orderByDesc(User::getFollowerCount)
                   .last("LIMIT " + Math.min(limit, 10));
            
            List<User> users = userMapper.selectList(wrapper);
            
            if (users.isEmpty()) {
                return "没有找到包含 \"" + keyword + "\" 的用户";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("👥 找到以下用户：\n\n");
            
            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);
                result.append(i + 1).append(". ").append(user.getUsername());
                if (user.getRealName() != null) {
                    result.append(" (").append(user.getRealName()).append(")");
                }
                result.append("\n");
                result.append("   粉丝：").append(user.getFollowerCount()).append(" 人\n");
                
                // 统计用户文章数
                Long articleCount = articleMapper.selectCount(
                    new LambdaQueryWrapper<Article>()
                        .eq(Article::getAuthorId, user.getId())
                        .eq(Article::getStatus, 1)
                );
                result.append("   文章：").append(articleCount).append(" 篇\n\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("搜索用户失败", e);
            return "❌ 搜索失败：" + e.getMessage();
        }
    }
    
    /**
     * 批量操作 - 给多篇文章点赞
     * @param keyword 搜索关键词
     * @param count 点赞数量
     * @return 批量点赞结果
     */
    @Tool("批量给包含关键词的文章点赞")
    public String batchLikeArticles(String keyword, int count) {
        log.info("Agent工具：批量点赞 - 关键词: {}, 数量: {}", keyword, count);
        
        try {
            // 搜索文章
            LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
            wrapper.and(w -> w.like(Article::getTitle, keyword)
                            .or().like(Article::getContent, keyword))
                   .eq(Article::getStatus, 1)
                   .orderByDesc(Article::getViewCount)
                   .last("LIMIT " + Math.min(count, 5)); // 最多5篇
            
            List<Article> articles = articleMapper.selectList(wrapper);
            
            if (articles.isEmpty()) {
                return "没有找到包含 \"" + keyword + "\" 的文章";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("🎯 批量点赞执行结果：\n\n");
            
            int successCount = 0;
            for (Article article : articles) {
                try {
                    // 检查是否已点赞
                    Long userId = 1L;
                    LambdaQueryWrapper<ArticleLike> likeWrapper = new LambdaQueryWrapper<>();
                    likeWrapper.eq(ArticleLike::getArticleId, article.getId())
                               .eq(ArticleLike::getUserId, userId);
                    ArticleLike existing = articleLikeMapper.selectOne(likeWrapper);
                    
                    if (existing == null) {
                        ArticleLike like = new ArticleLike();
                        like.setArticleId(article.getId());
                        like.setUserId(userId);
                        articleLikeMapper.insert(like);
                        
                        article.setLikeCount(article.getLikeCount() + 1);
                        articleMapper.updateById(article);
                        
                        result.append("✅ 《").append(article.getTitle()).append("》点赞成功\n");
                        successCount++;
                    } else {
                        result.append("ℹ️ 《").append(article.getTitle()).append("》已点赞过\n");
                    }
                } catch (Exception e) {
                    result.append("❌ 《").append(article.getTitle()).append("》点赞失败\n");
                }
            }
            
            result.append("\n📊 统计：成功点赞 ").append(successCount).append(" 篇文章");
            return result.toString();
        } catch (Exception e) {
            log.error("批量点赞失败", e);
            return "❌ 批量操作失败：" + e.getMessage();
        }
    }
    
    /**
     * 数据分析 - 获取今日热点
     * @return 今日热点分析
     */
    @Tool("分析今日热点新闻和趋势")
    public String analyzeTodayTrends() {
        log.info("Agent工具：分析今日热点");
        
        try {
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            
            // 今日发布的文章
            LambdaQueryWrapper<Article> todayWrapper = new LambdaQueryWrapper<>();
            todayWrapper.ge(Article::getCreatedAt, todayStart)
                       .eq(Article::getStatus, 1)
                       .orderByDesc(Article::getViewCount);
            List<Article> todayArticles = articleMapper.selectList(todayWrapper);
            
            // 今日评论最多的文章
            LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
            commentWrapper.ge(Comment::getCreatedAt, todayStart);
            List<Comment> todayComments = commentMapper.selectList(commentWrapper);
            
            Map<Long, Integer> articleCommentCount = new HashMap<>();
            for (Comment comment : todayComments) {
                articleCommentCount.merge(comment.getArticleId(), 1, Integer::sum);
            }
            
            // 统计结果
            StringBuilder result = new StringBuilder("📊 今日热点分析报告\n");
            result.append("═".repeat(30)).append("\n\n");
            
            result.append("📈 今日数据概览：\n");
            result.append("• 发布文章：").append(todayArticles.size()).append(" 篇\n");
            result.append("• 新增评论：").append(todayComments.size()).append(" 条\n");
            
            int totalViews = todayArticles.stream().mapToInt(Article::getViewCount).sum();
            result.append("• 总浏览量：").append(totalViews).append(" 次\n\n");
            
            if (!todayArticles.isEmpty()) {
                result.append("🔥 今日最热文章：\n");
                List<Article> topArticles = todayArticles.stream()
                    .limit(3)
                    .collect(Collectors.toList());
                
                for (int i = 0; i < topArticles.size(); i++) {
                    Article article = topArticles.get(i);
                    result.append(i + 1).append(". 《").append(article.getTitle()).append("》\n");
                    result.append("   浏览量：").append(article.getViewCount())
                          .append(" | 点赞：").append(article.getLikeCount())
                          .append(" | 评论：").append(article.getCommentCount()).append("\n");
                }
            }
            
            result.append("\n💡 趋势分析：\n");
            if (todayArticles.size() > 5) {
                result.append("• 今日内容发布活跃，建议关注热门话题\n");
            } else {
                result.append("• 今日内容产出较少，可以发布新内容获得更多关注\n");
            }
            
            if (totalViews > 1000) {
                result.append("• 用户活跃度高，是推广内容的好时机\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("分析失败", e);
            return "❌ 分析失败：" + e.getMessage();
        }
    }
}
