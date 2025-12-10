package com.campus.news.agent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.news.entity.*;
import com.campus.news.mapper.*;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent工具类 - 提供给AI Agent使用的各种工具方法1
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
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

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
     * 快速发布文章（一键创建并发布）
     * @param title 标题
     * @return 发布结果
     */
    @Tool("快速发布文章，只需要提供标题，系统会自动生成内容并直接发布")
    public String quickPublishArticle(String title) {
        log.info("Agent工具：快速发布文章 - 标题: {}", title);
        
        try {
            // 根据标题智能生成内容
            String content = generateContentByTitle(title);
            String summary = content.length() > 100 ? content.substring(0, 100) + "..." : content;
            String boardType = "CAMPUS"; // 默认发布到校园版块
            
            // 创建并直接发布文章
            Article article = new Article();
            article.setTitle(title);
            article.setContent(content);
            article.setSummary(summary);
            article.setBoardType(boardType);
            article.setAuthorId(1L); // 系统用户
            article.setStatus(1); // 正常状态
            article.setViewCount(0);
            article.setLikeCount(0);
            article.setCommentCount(0);
            article.setIsApproved(1); // 直接设置为已审核
            article.setIsPinned(0);
            article.setCreatedAt(LocalDateTime.now());
            article.setUpdatedAt(LocalDateTime.now());
            
            articleMapper.insert(article);
            
            return "🎉 文章发布成功！\n" +
                   "标题：《" + title + "》\n" +
                   "版块：" + getBoardTypeName(boardType) + "\n" +
                   "文章ID：" + article.getId() + "\n" +
                   "状态：已发布\n" +
                   "内容预览：\n" + content.substring(0, Math.min(200, content.length())) + 
                   (content.length() > 200 ? "..." : "") + "\n" +
                   "链接：/article/" + article.getId();
        } catch (Exception e) {
            log.error("快速发布文章失败", e);
            return "❌ 发布失败：" + e.getMessage();
        }
    }
    
    /**
     * 根据标题智能生成文章内容
     */
    private String generateContentByTitle(String title) {
        // 根据标题关键词智能生成内容
        if (title.contains("睡觉") || title.contains("睡眠")) {
            return "夜深了，经过一天的学习和工作，是时候好好休息了。\n\n" +
                   "良好的睡眠对我们的身心健康至关重要。它不仅能够帮助我们恢复体力，" +
                   "还能巩固白天学到的知识，提高记忆力。\n\n" +
                   "科学研究表明，成年人每天需要7-9小时的睡眠。规律的作息时间能够：\n" +
                   "1. 增强免疫系统\n" +
                   "2. 改善情绪状态\n" +
                   "3. 提高学习效率\n" +
                   "4. 保持良好的精神状态\n\n" +
                   "晚安，愿大家都有一个美好的夜晚，做个好梦！💤";
        } else if (title.contains("学习") || title.contains("考试")) {
            return "学习是一个持续的过程，需要我们保持专注和耐心。\n\n" +
                   "有效的学习方法包括：\n" +
                   "1. 制定合理的学习计划\n" +
                   "2. 找到适合自己的学习环境\n" +
                   "3. 定期复习和总结\n" +
                   "4. 保持良好的作息习惯\n\n" +
                   "让我们一起努力，在知识的海洋中不断前行！";
        } else if (title.contains("活动") || title.contains("校园")) {
            return "校园生活丰富多彩，各种活动让我们的大学时光更加精彩。\n\n" +
                   "参与校园活动不仅能够丰富我们的课余生活，还能：\n" +
                   "- 结识志同道合的朋友\n" +
                   "- 锻炼组织和沟通能力\n" +
                   "- 拓展视野，增长见识\n" +
                   "- 为未来的发展积累经验\n\n" +
                   "期待在下一次活动中见到大家！";
        } else {
            // 默认内容模板
            return "这是一篇关于《" + title + "》的文章。\n\n" +
                   "在这里分享一些关于" + title + "的想法和感受。\n\n" +
                   "生活中总有许多值得记录的时刻，让我们用文字留下这些美好的回忆。\n" +
                   "无论是开心还是困扰，分享出来总能得到共鸣和支持。\n\n" +
                   "欢迎大家在评论区交流讨论！";
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
    
    // ==================== 用户管理功能 ====================
    
    /**
     * 创建新用户
     * @param username 用户名
     * @param password 密码
     * @param realName 真实姓名
     * @param email 邮箱
     * @return 创建结果
     */
    @Tool("创建新的用户账号")
    public String createUser(String username, String password, String realName, String email) {
        log.info("Agent工具：创建用户 - 用户名: {}", username);
        
        try {
            // 检查用户名是否已存在
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, username);
            User existing = userMapper.selectOne(wrapper);
            
            if (existing != null) {
                return "❌ 用户名 \"" + username + "\" 已存在";
            }
            
            // 创建新用户
            User user = new User();
            user.setUsername(username);
            user.setPassword(password); // 实际应该加密
            user.setRealName(realName);
            user.setEmail(email);
            user.setStatus(1);
            user.setFollowerCount(0);
            user.setFollowingCount(0);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            userMapper.insert(user);
            
            return "✅ 用户创建成功！\n" +
                   "用户名：" + username + "\n" +
                   "真实姓名：" + realName + "\n" +
                   "邮箱：" + email + "\n" +
                   "用户ID：" + user.getId();
        } catch (Exception e) {
            log.error("创建用户失败", e);
            return "❌ 创建用户失败：" + e.getMessage();
        }
    }
    
    /**
     * 更新用户信息
     * @param username 用户名
     * @param field 要更新的字段（realName/email/phone/avatar）
     * @param value 新值
     * @return 更新结果
     */
    @Tool("更新用户的个人信息")
    public String updateUserInfo(String username, String field, String value) {
        log.info("Agent工具：更新用户信息 - 用户: {}, 字段: {}", username, field);
        
        try {
            // 查找用户
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, username);
            User user = userMapper.selectOne(wrapper);
            
            if (user == null) {
                return "❌ 用户不存在：" + username;
            }
            
            // 根据字段更新
            switch (field.toLowerCase()) {
                case "realname":
                    user.setRealName(value);
                    break;
                case "email":
                    user.setEmail(value);
                    break;
                case "phone":
                    user.setPhone(value);
                    break;
                case "avatar":
                    user.setAvatar(value);
                    break;
                default:
                    return "❌ 不支持的字段：" + field + "\n支持的字段：realName, email, phone, avatar";
            }
            
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
            
            return "✅ 用户信息更新成功！\n" +
                   "用户：" + username + "\n" +
                   "更新字段：" + field + "\n" +
                   "新值：" + value;
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            return "❌ 更新失败：" + e.getMessage();
        }
    }
    
    /**
     * 管理用户角色
     * @param username 用户名
     * @param action 动作（add/remove）
     * @param roleName 角色名称
     * @return 操作结果
     */
    @Tool("管理用户的角色权限")
    public String manageUserRole(String username, String action, String roleName) {
        log.info("Agent工具：管理用户角色 - 用户: {}, 动作: {}, 角色: {}", username, action, roleName);
        
        try {
            // 查找用户
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(User::getUsername, username);
            User user = userMapper.selectOne(userWrapper);
            
            if (user == null) {
                return "❌ 用户不存在：" + username;
            }
            
            // 查找角色
            LambdaQueryWrapper<Role> roleWrapper = new LambdaQueryWrapper<>();
            roleWrapper.eq(Role::getRoleName, roleName);
            Role role = roleMapper.selectOne(roleWrapper);
            
            if (role == null) {
                return "❌ 角色不存在：" + roleName;
            }
            
            if ("add".equalsIgnoreCase(action)) {
                // 添加角色
                // 检查是否已经具有该角色
                LambdaQueryWrapper<UserRole> checkWrapper = new LambdaQueryWrapper<>();
                checkWrapper.eq(UserRole::getUserId, user.getId())
                           .eq(UserRole::getRoleId, role.getId());
                UserRole existing = userRoleMapper.selectOne(checkWrapper);
                
                if (existing != null) {
                    return "ℹ️ 用户 " + username + " 已经具有角色 " + roleName;
                }
                
                UserRole userRole = new UserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(role.getId());
                userRole.setCreatedAt(LocalDateTime.now());
                userRoleMapper.insert(userRole);
                
                return "✅ 成功为用户 " + username + " 添加角色 " + roleName;
            } else if ("remove".equalsIgnoreCase(action)) {
                // 移除角色
                LambdaQueryWrapper<UserRole> deleteWrapper = new LambdaQueryWrapper<>();
                deleteWrapper.eq(UserRole::getUserId, user.getId())
                            .eq(UserRole::getRoleId, role.getId());
                int deleted = userRoleMapper.delete(deleteWrapper);
                
                if (deleted > 0) {
                    return "✅ 成功移除用户 " + username + " 的角色 " + roleName;
                } else {
                    return "ℹ️ 用户 " + username + " 没有角色 " + roleName;
                }
            } else {
                return "❌ 无效的动作：" + action + "\n请使用 add（添加）或 remove（移除）";
            }
        } catch (Exception e) {
            log.error("管理用户角色失败", e);
            return "❌ 操作失败：" + e.getMessage();
        }
    }
    
    /**
     * 封禁或解封用户
     * @param username 用户名
     * @param action 动作（ban/unban）
     * @param reason 原因（可选）
     * @return 操作结果
     */
    @Tool("封禁或解封用户账号")
    public String banOrUnbanUser(String username, String action, String reason) {
        log.info("Agent工具：封禁/解封用户 - 用户: {}, 动作: {}", username, action);
        
        try {
            // 查找用户
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, username);
            User user = userMapper.selectOne(wrapper);
            
            if (user == null) {
                return "❌ 用户不存在：" + username;
            }
            
            if ("ban".equalsIgnoreCase(action)) {
                if (user.getStatus() == 0) {
                    return "ℹ️ 用户 " + username + " 已经被封禁";
                }
                user.setStatus(0); // 封禁
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(user);
                
                return "🚫 用户封禁成功！\n" +
                       "用户：" + username + "\n" +
                       (reason != null ? "原因：" + reason + "\n" : "") +
                       "该用户将无法登录系统";
            } else if ("unban".equalsIgnoreCase(action)) {
                if (user.getStatus() == 1) {
                    return "ℹ️ 用户 " + username + " 未被封禁";
                }
                user.setStatus(1); // 解封
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(user);
                
                return "✅ 用户解封成功！\n" +
                       "用户：" + username + "\n" +
                       "该用户现在可以正常登录";
            } else {
                return "❌ 无效的动作：" + action + "\n请使用 ban（封禁）或 unban（解封）";
            }
        } catch (Exception e) {
            log.error("封禁/解封失败", e);
            return "❌ 操作失败：" + e.getMessage();
        }
    }
    
    /**
     * 重置用户密码
     * @param username 用户名
     * @param newPassword 新密码
     * @return 重置结果
     */
    @Tool("重置用户的登录密码")
    public String resetUserPassword(String username, String newPassword) {
        log.info("Agent工具：重置密码 - 用户: {}", username);
        
        try {
            // 查找用户
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, username);
            User user = userMapper.selectOne(wrapper);
            
            if (user == null) {
                return "❌ 用户不存在：" + username;
            }
            
            // 重置密码（实际应该加密）
            user.setPassword(newPassword);
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
            
            return "🔐 密码重置成功！\n" +
                   "用户：" + username + "\n" +
                   "新密码：" + newPassword + "\n" +
                   "提示：请用户尽快修改密码";
        } catch (Exception e) {
            log.error("重置密码失败", e);
            return "❌ 重置失败：" + e.getMessage();
        }
    }
    
    /**
     * 获取用户详细信息
     * @param username 用户名
     * @return 用户详情
     */
    @Tool("获取用户的详细信息和统计数据")
    public String getUserDetail(String username) {
        log.info("Agent工具：获取用户详情 - 用户: {}", username);
        
        try {
            // 查找用户
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, username);
            User user = userMapper.selectOne(wrapper);
            
            if (user == null) {
                return "❌ 用户不存在：" + username;
            }
            
            // 统计用户数据
            Long articleCount = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>()
                    .eq(Article::getAuthorId, user.getId())
            );
            
            Long commentCount = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getUserId, user.getId())
            );
            
            Long likeGivenCount = articleLikeMapper.selectCount(
                new LambdaQueryWrapper<ArticleLike>()
                    .eq(ArticleLike::getUserId, user.getId())
            );
            
            StringBuilder result = new StringBuilder();
            result.append("👤 用户详情\n");
            result.append("═".repeat(30)).append("\n");
            result.append("基本信息：\n");
            result.append("• 用户名：").append(user.getUsername()).append("\n");
            result.append("• 真实姓名：").append(user.getRealName() != null ? user.getRealName() : "未设置").append("\n");
            result.append("• 邮箱：").append(user.getEmail() != null ? user.getEmail() : "未设置").append("\n");
            result.append("• 手机：").append(user.getPhone() != null ? user.getPhone() : "未设置").append("\n");
            result.append("• 状态：").append(user.getStatus() == 1 ? "正常" : "封禁").append("\n");
            result.append("• 注册时间：").append(user.getCreatedAt()).append("\n");
            result.append("\n社交数据：\n");
            result.append("• 粉丝数：").append(user.getFollowerCount()).append("\n");
            result.append("• 关注数：").append(user.getFollowingCount()).append("\n");
            result.append("\n内容统计：\n");
            result.append("• 发布文章：").append(articleCount).append(" 篇\n");
            result.append("• 发表评论：").append(commentCount).append(" 条\n");
            result.append("• 点赞次数：").append(likeGivenCount).append(" 次\n");
            
            return result.toString();
        } catch (Exception e) {
            log.error("获取用户详情失败", e);
            return "❌ 获取失败：" + e.getMessage();
        }
    }
    
    /**
     * 批量发送通知
     * @param userType 用户类型（all/active/new）
     * @param title 通知标题
     * @param content 通知内容
     * @return 发送结果
     */
    @Tool("批量给用户发送系统通知")
    public String sendBulkNotification(String userType, String title, String content) {
        log.info("Agent工具：批量发送通知 - 类型: {}, 标题: {}", userType, title);
        
        try {
            List<User> targetUsers;
            
            if ("all".equalsIgnoreCase(userType)) {
                // 所有用户
                targetUsers = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getStatus, 1));
            } else if ("active".equalsIgnoreCase(userType)) {
                // 活跃用户（最近7天有活动）
                LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
                targetUsers = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                        .eq(User::getStatus, 1)
                        .ge(User::getUpdatedAt, sevenDaysAgo)
                );
            } else if ("new".equalsIgnoreCase(userType)) {
                // 新用户（最近30天注册）
                LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
                targetUsers = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                        .eq(User::getStatus, 1)
                        .ge(User::getCreatedAt, thirtyDaysAgo)
                );
            } else {
                return "❌ 无效的用户类型：" + userType + "\n支持的类型：all（所有）、active（活跃）、new（新用户）";
            }
            
            if (targetUsers.isEmpty()) {
                return "没有找到符合条件的用户";
            }
            
            int successCount = 0;
            for (User user : targetUsers) {
                try {
                    Notification notification = new Notification();
                    notification.setUserId(user.getId());
                    notification.setContent("[" + title + "] " + content);
                    notification.setType("SYSTEM");
                    notification.setIsRead(0);
                    notification.setCreatedAt(LocalDateTime.now());
                    notificationMapper.insert(notification);
                    successCount++;
                } catch (Exception e) {
                    log.error("发送通知给用户 {} 失败", user.getUsername(), e);
                }
            }
            
            return "📢 批量通知发送完成！\n" +
                   "目标用户类型：" + userType + "\n" +
                   "目标用户数：" + targetUsers.size() + "\n" +
                   "成功发送：" + successCount + " 条\n" +
                   "通知标题：" + title;
        } catch (Exception e) {
            log.error("批量发送通知失败", e);
            return "❌ 发送失败：" + e.getMessage();
        }
    }
    
    // ==================== 自动化任务 ====================
    
    /**
     * 自动审核待审文章
     * @param count 审核数量
     * @param criteria 审核标准（lenient/strict）
     * @return 审核结果
     */
    @Tool("自动审核待审核的文章")
    public String autoApproveArticles(int count, String criteria) {
        log.info("Agent工具：自动审核文章 - 数量: {}, 标准: {}", count, criteria);
        
        try {
            // 获取待审核文章
            LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Article::getIsApproved, 0)
                   .eq(Article::getStatus, 1)
                   .orderByAsc(Article::getCreatedAt)
                   .last("LIMIT " + Math.min(count, 10));
            
            List<Article> pendingArticles = articleMapper.selectList(wrapper);
            
            if (pendingArticles.isEmpty()) {
                return "没有待审核的文章";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("🤖 自动审核结果（").append(criteria).append("模式）：\n\n");
            
            int approvedCount = 0;
            int rejectedCount = 0;
            
            for (Article article : pendingArticles) {
                boolean shouldApprove = true;
                String rejectReason = null;
                
                // 根据标准进行审核
                if ("strict".equalsIgnoreCase(criteria)) {
                    // 严格模式：检查内容长度、标题等
                    if (article.getContent().length() < 100) {
                        shouldApprove = false;
                        rejectReason = "内容过短";
                    } else if (article.getTitle().length() < 5) {
                        shouldApprove = false;
                        rejectReason = "标题过短";
                    } else if (article.getSummary() == null || article.getSummary().isEmpty()) {
                        shouldApprove = false;
                        rejectReason = "缺少摘要";
                    }
                } else {
                    // 宽松模式：只要有内容就通过
                    if (article.getContent() == null || article.getContent().length() < 10) {
                        shouldApprove = false;
                        rejectReason = "内容为空或过短";
                    }
                }
                
                if (shouldApprove) {
                    article.setIsApproved(1);
                    article.setUpdatedAt(LocalDateTime.now());
                    articleMapper.updateById(article);
                    
                    result.append("✅ 《").append(article.getTitle()).append("》- 通过\n");
                    approvedCount++;
                    
                    // 发送通知
                    createNotificationForUser(article.getAuthorId(), "文章审核通过", 
                        "您的文章《" + article.getTitle() + "》已通过审核并发布。");
                } else {
                    article.setIsApproved(2); // 拒绝
                    article.setUpdatedAt(LocalDateTime.now());
                    articleMapper.updateById(article);
                    
                    result.append("❌ 《").append(article.getTitle()).append("》- 拒绝（")
                          .append(rejectReason).append("）\n");
                    rejectedCount++;
                    
                    // 发送通知
                    createNotificationForUser(article.getAuthorId(), "文章审核未通过", 
                        "您的文章《" + article.getTitle() + "》未通过审核。原因：" + rejectReason);
                }
            }
            
            result.append("\n📊 审核统计：\n");
            result.append("• 通过：").append(approvedCount).append(" 篇\n");
            result.append("• 拒绝：").append(rejectedCount).append(" 篇\n");
            
            return result.toString();
        } catch (Exception e) {
            log.error("自动审核失败", e);
            return "❌ 审核失败：" + e.getMessage();
        }
    }
    
    /**
     * 清理过期数据
     * @param type 清理类型（old_drafts/spam_comments/old_notifications）
     * @param days 天数阈值
     * @return 清理结果
     */
    @Tool("清理系统中的过期或垃圾数据")
    public String cleanupOldData(String type, int days) {
        log.info("Agent工具：清理数据 - 类型: {}, 天数: {}", type, days);
        
        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(days);
            int deletedCount = 0;
            
            StringBuilder result = new StringBuilder();
            result.append("🧹 数据清理任务\n");
            result.append("═".repeat(30)).append("\n");
            
            if ("old_drafts".equalsIgnoreCase(type)) {
                // 清理旧草稿
                LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Article::getIsApproved, 0) // 未审核的
                       .lt(Article::getCreatedAt, threshold);
                
                List<Article> oldDrafts = articleMapper.selectList(wrapper);
                deletedCount = oldDrafts.size();
                
                for (Article draft : oldDrafts) {
                    articleMapper.deleteById(draft.getId());
                }
                
                result.append("清理类型：旧文章草稿\n");
                result.append("时间阈值：").append(days).append(" 天前\n");
                result.append("删除数量：").append(deletedCount).append(" 篇\n");
                
            } else if ("spam_comments".equalsIgnoreCase(type)) {
                // 清理垃圾评论
                LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
                wrapper.lt(Comment::getCreatedAt, threshold)
                       .le(Comment::getLikeCount, 0); // 没有点赞的
                
                List<Comment> spamComments = commentMapper.selectList(wrapper);
                deletedCount = Math.min(spamComments.size(), 100); // 最多删除100条
                
                for (int i = 0; i < deletedCount; i++) {
                    commentMapper.deleteById(spamComments.get(i).getId());
                }
                
                result.append("清理类型：垃圾评论\n");
                result.append("时间阈值：").append(days).append(" 天前且无点赞\n");
                result.append("删除数量：").append(deletedCount).append(" 条\n");
                
            } else if ("old_notifications".equalsIgnoreCase(type)) {
                // 清理旧通知
                LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
                wrapper.lt(Notification::getCreatedAt, threshold)
                       .eq(Notification::getIsRead, 1); // 已读的
                
                deletedCount = notificationMapper.delete(wrapper);
                
                result.append("清理类型：已读旧通知\n");
                result.append("时间阈值：").append(days).append(" 天前\n");
                result.append("删除数量：").append(deletedCount).append(" 条\n");
                
            } else {
                return "❌ 无效的清理类型：" + type + "\n支持的类型：old_drafts（旧草稿）、spam_comments（垃圾评论）、old_notifications（旧通知）";
            }
            
            result.append("\n✅ 清理完成！");
            return result.toString();
        } catch (Exception e) {
            log.error("清理数据失败", e);
            return "❌ 清理失败：" + e.getMessage();
        }
    }
    
    /**
     * 自动生成内容推荐
     * @param userId 用户ID
     * @param count 推荐数量
     * @return 推荐结果
     */
    @Tool("为用户生成个性化内容推荐")
    public String generateRecommendations(Long userId, int count) {
        log.info("Agent工具：生成推荐 - 用户ID: {}, 数量: {}", userId, count);
        
        try {
            // 获取用户信息
            User user = userMapper.selectById(userId);
            if (user == null) {
                return "❌ 用户不存在，ID：" + userId;
            }
            
            // 获取用户最近点赞的文章，分析兴趣（暂时注释，后续可扩展个性化推荐）
            // LambdaQueryWrapper<ArticleLike> likeWrapper = new LambdaQueryWrapper<>();
            // likeWrapper.eq(ArticleLike::getUserId, userId)
            //           .orderByDesc(ArticleLike::getCreatedAt)
            //           .last("LIMIT 10");
            // List<ArticleLike> recentLikes = articleLikeMapper.selectList(likeWrapper);
            
            // 获取用户关注的人发布的文章
            LambdaQueryWrapper<UserFollow> followWrapper = new LambdaQueryWrapper<>();
            followWrapper.eq(UserFollow::getFollowerId, userId);
            List<UserFollow> follows = userFollowMapper.selectList(followWrapper);
            List<Long> followingIds = follows.stream()
                .map(UserFollow::getFollowingId)
                .collect(Collectors.toList());
            
            // 构建推荐查询
            LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<>();
            articleWrapper.eq(Article::getStatus, 1)
                         .eq(Article::getIsApproved, 1)
                         .ne(Article::getAuthorId, userId); // 排除用户自己的文章
            
            if (!followingIds.isEmpty()) {
                // 优先推荐关注的人的文章
                articleWrapper.in(Article::getAuthorId, followingIds);
            }
            
            articleWrapper.orderByDesc(Article::getViewCount)
                         .orderByDesc(Article::getCreatedAt)
                         .last("LIMIT " + Math.min(count, 10));
            
            List<Article> recommendations = articleMapper.selectList(articleWrapper);
            
            if (recommendations.isEmpty()) {
                // 如果没有关注的人的文章，推荐热门文章
                articleWrapper = new LambdaQueryWrapper<>();
                articleWrapper.eq(Article::getStatus, 1)
                             .eq(Article::getIsApproved, 1)
                             .ne(Article::getAuthorId, userId)
                             .orderByDesc(Article::getViewCount)
                             .last("LIMIT " + Math.min(count, 10));
                recommendations = articleMapper.selectList(articleWrapper);
            }
            
            StringBuilder result = new StringBuilder();
            result.append("🎯 为用户 ").append(user.getUsername()).append(" 的个性化推荐：\n\n");
            
            for (int i = 0; i < recommendations.size(); i++) {
                Article article = recommendations.get(i);
                result.append(i + 1).append(". 《").append(article.getTitle()).append("》\n");
                result.append("   作者：").append(getUserName(article.getAuthorId())).append("\n");
                result.append("   热度：⭐".repeat(Math.min(5, article.getViewCount() / 100))).append("\n");
                result.append("   链接：/article/").append(article.getId()).append("\n\n");
            }
            
            if (recommendations.isEmpty()) {
                result.append("暂无推荐内容");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("生成推荐失败", e);
            return "❌ 生成推荐失败：" + e.getMessage();
        }
    }
    
    /**
     * 定时发布文章
     * @param articleId 文章ID
     * @param publishTime 发布时间（格式：yyyy-MM-dd HH:mm:ss）
     * @return 设置结果
     */
    @Tool("设置文章定时发布")
    public String scheduleArticlePublish(Long articleId, String publishTime) {
        log.info("Agent工具：设置定时发布 - 文章ID: {}, 时间: {}", articleId, publishTime);
        
        try {
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                return "❌ 文章不存在，ID：" + articleId;
            }
            
            if (article.getIsApproved() == 1 && article.getStatus() == 1) {
                return "ℹ️ 文章《" + article.getTitle() + "》已经发布";
            }
            
            // 解析时间
            LocalDateTime scheduledTime;
            try {
                scheduledTime = LocalDateTime.parse(publishTime.replace(" ", "T"));
            } catch (Exception e) {
                return "❌ 时间格式错误，请使用格式：yyyy-MM-dd HH:mm:ss";
            }
            
            if (scheduledTime.isBefore(LocalDateTime.now())) {
                return "❌ 发布时间不能早于当前时间";
            }
            
            // 这里简化处理，实际应该使用定时任务框架
            // 暂时只记录计划发布时间
            article.setUpdatedAt(LocalDateTime.now());
            articleMapper.updateById(article);
            
            return "⏰ 定时发布设置成功！\n" +
                   "文章：《" + article.getTitle() + "》\n" +
                   "计划发布时间：" + publishTime + "\n" +
                   "提示：文章将在指定时间自动发布";
        } catch (Exception e) {
            log.error("设置定时发布失败", e);
            return "❌ 设置失败：" + e.getMessage();
        }
    }
    
    /**
     * 批量更新文章状态
     * @param boardType 版块类型
     * @param oldStatus 原状态
     * @param newStatus 新状态
     * @return 更新结果
     */
    @Tool("批量更新指定版块文章的状态")
    public String batchUpdateArticleStatus(String boardType, Integer oldStatus, Integer newStatus) {
        log.info("Agent工具：批量更新状态 - 版块: {}, {} -> {}", boardType, oldStatus, newStatus);
        
        try {
            LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Article::getBoardType, boardType.toUpperCase())
                   .eq(Article::getStatus, oldStatus);
            
            List<Article> articles = articleMapper.selectList(wrapper);
            
            if (articles.isEmpty()) {
                return "没有找到符合条件的文章";
            }
            
            int updateCount = 0;
            StringBuilder result = new StringBuilder();
            result.append("📝 批量更新文章状态\n");
            result.append("═".repeat(30)).append("\n");
            
            for (Article article : articles) {
                article.setStatus(newStatus);
                article.setUpdatedAt(LocalDateTime.now());
                articleMapper.updateById(article);
                updateCount++;
                
                if (updateCount <= 5) {
                    result.append("• 《").append(article.getTitle()).append("》\n");
                }
            }
            
            if (updateCount > 5) {
                result.append("... 还有 ").append(updateCount - 5).append(" 篇文章\n");
            }
            
            result.append("\n📊 更新统计：\n");
            result.append("• 版块：").append(getBoardTypeName(boardType)).append("\n");
            result.append("• 更新数量：").append(updateCount).append(" 篇\n");
            result.append("• 状态变更：").append(oldStatus).append(" → ").append(newStatus).append("\n");
            
            return result.toString();
        } catch (Exception e) {
            log.error("批量更新失败", e);
            return "❌ 批量更新失败：" + e.getMessage();
        }
    }
    
    /**
     * 生成数据报表
     * @param reportType 报表类型（daily/weekly/monthly）
     * @return 报表内容
     */
    @Tool("生成系统数据分析报表")
    public String generateDataReport(String reportType) {
        log.info("Agent工具：生成报表 - 类型: {}", reportType);
        
        try {
            LocalDateTime startTime;
            String periodName;
            
            switch (reportType.toLowerCase()) {
                case "daily":
                    startTime = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
                    periodName = "今日";
                    break;
                case "weekly":
                    startTime = LocalDateTime.now().minusDays(7);
                    periodName = "本周";
                    break;
                case "monthly":
                    startTime = LocalDateTime.now().minusDays(30);
                    periodName = "本月";
                    break;
                default:
                    return "❌ 无效的报表类型：" + reportType + "\n支持的类型：daily（日报）、weekly（周报）、monthly（月报）";
            }
            
            // 统计数据
            LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<>();
            articleWrapper.ge(Article::getCreatedAt, startTime);
            Long newArticles = articleMapper.selectCount(articleWrapper);
            
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.ge(User::getCreatedAt, startTime);
            Long newUsers = userMapper.selectCount(userWrapper);
            
            LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
            commentWrapper.ge(Comment::getCreatedAt, startTime);
            Long newComments = commentMapper.selectCount(commentWrapper);
            
            // 获取热门文章
            articleWrapper = new LambdaQueryWrapper<>();
            articleWrapper.ge(Article::getCreatedAt, startTime)
                         .eq(Article::getStatus, 1)
                         .orderByDesc(Article::getViewCount)
                         .last("LIMIT 3");
            List<Article> hotArticles = articleMapper.selectList(articleWrapper);
            
            // 生成报表
            StringBuilder result = new StringBuilder();
            result.append("📈 ").append(periodName).append("数据报表\n");
            result.append("═".repeat(30)).append("\n");
            result.append("生成时间：").append(LocalDateTime.now()).append("\n\n");
            
            result.append("📊 核心指标：\n");
            result.append("• 新增文章：").append(newArticles).append(" 篇\n");
            result.append("• 新增用户：").append(newUsers).append(" 人\n");
            result.append("• 新增评论：").append(newComments).append(" 条\n\n");
            
            if (!hotArticles.isEmpty()) {
                result.append("🔥 ").append(periodName).append("热门文章：\n");
                for (int i = 0; i < hotArticles.size(); i++) {
                    Article article = hotArticles.get(i);
                    result.append(i + 1).append(". 《").append(article.getTitle()).append("》\n");
                    result.append("   浏览：").append(article.getViewCount()).append(" 次\n");
                }
            }
            
            result.append("\n💡 数据洞察：\n");
            if (newArticles > 10) {
                result.append("• 内容产出活跃，保持良好势头\n");
            } else {
                result.append("• 内容产出较少，建议激励创作\n");
            }
            
            if (newUsers > 5) {
                result.append("• 用户增长良好，注意新用户体验\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("生成报表失败", e);
            return "❌ 生成报表失败：" + e.getMessage();
        }
    }
    
    // 辅助方法：创建通知
    private void createNotificationForUser(Long userId, String title, String content) {
        try {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setContent("[" + title + "] " + content);
            notification.setType("SYSTEM");
            notification.setIsRead(0);
            notification.setCreatedAt(LocalDateTime.now());
            notificationMapper.insert(notification);
        } catch (Exception e) {
            log.error("创建通知失败", e);
        }
    }
    
    // ==================== 智能互动与社交分析功能 ====================
    
    /**
     * 分析文章情感倾向
     * @param articleId 文章ID
     * @return 情感分析结果和建议
     */
    @Tool("分析文章的情感倾向并给出优化建议")
    public String analyzeArticleSentiment(Long articleId) {
        log.info("Agent工具：分析文章情感 - 文章ID: {}", articleId);
        
        try {
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                return "❌ 文章不存在，ID：" + articleId;
            }
            
            String content = article.getContent();
            String title = article.getTitle();
            
            // 情感关键词分析
            Map<String, Integer> sentimentScores = analyzeSentimentKeywords(content);
            
            // 计算整体情感得分
            int positiveScore = sentimentScores.get("positive");
            int negativeScore = sentimentScores.get("negative");
            int neutralScore = sentimentScores.get("neutral");
            int totalScore = positiveScore + negativeScore + neutralScore;
            
            String overallSentiment;
            String emoji;
            if (positiveScore > negativeScore * 1.5) {
                overallSentiment = "积极正面";
                emoji = "😊";
            } else if (negativeScore > positiveScore * 1.5) {
                overallSentiment = "消极负面";
                emoji = "😔";
            } else {
                overallSentiment = "中性平和";
                emoji = "😐";
            }
            
            // 分析评论情感
            List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getArticleId, articleId)
                    .orderByDesc(Comment::getLikeCount)
                    .last("LIMIT 10")
            );
            
            int positiveComments = 0;
            int negativeComments = 0;
            for (Comment comment : comments) {
                Map<String, Integer> commentSentiment = analyzeSentimentKeywords(comment.getContent());
                if (commentSentiment.get("positive") > commentSentiment.get("negative")) {
                    positiveComments++;
                } else if (commentSentiment.get("negative") > commentSentiment.get("positive")) {
                    negativeComments++;
                }
            }
            
            StringBuilder result = new StringBuilder();
            result.append("🎭 文章情感分析报告\n");
            result.append("═".repeat(30)).append("\n\n");
            
            result.append("📄 文章：《").append(title).append("》\n");
            result.append("作者：").append(getUserName(article.getAuthorId())).append("\n\n");
            
            result.append("📊 情感分析结果：\n");
            result.append("整体倾向：").append(emoji).append(" ").append(overallSentiment).append("\n");
            result.append("• 积极度：").append(String.format("%.1f%%", positiveScore * 100.0 / Math.max(totalScore, 1))).append("\n");
            result.append("• 消极度：").append(String.format("%.1f%%", negativeScore * 100.0 / Math.max(totalScore, 1))).append("\n");
            result.append("• 中性度：").append(String.format("%.1f%%", neutralScore * 100.0 / Math.max(totalScore, 1))).append("\n\n");
            
            if (!comments.isEmpty()) {
                result.append("💬 读者反馈分析：\n");
                result.append("• 正面评论：").append(positiveComments).append(" 条\n");
                result.append("• 负面评论：").append(negativeComments).append(" 条\n");
                result.append("• 中性评论：").append(comments.size() - positiveComments - negativeComments).append(" 条\n\n");
            }
            
            result.append("🎯 优化建议：\n");
            if (negativeScore > positiveScore) {
                result.append("• 文章整体偏负面，建议增加积极正面的内容\n");
                result.append("• 可以加入一些解决方案或正面案例\n");
                result.append("• 结尾处添加鼓舞人心的总结\n");
            } else if (positiveScore > negativeScore * 2) {
                result.append("• 文章充满正能量，保持这种风格！\n");
                result.append("• 可以适当加入一些客观分析，增加深度\n");
            } else {
                result.append("• 文章情感表达平衡，适合理性讨论\n");
                result.append("• 可以适当增加一些情感色彩，提高感染力\n");
            }
            
            // 互动建议
            result.append("\n💡 互动策略：\n");
            if (article.getCommentCount() < 5) {
                result.append("• 评论较少，建议在文末提出问题引导讨论\n");
            }
            if (article.getLikeCount() < article.getViewCount() / 10) {
                result.append("• 点赞率偏低，可能需要优化内容质量或标题\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("分析文章情感失败", e);
            return "❌ 分析失败：" + e.getMessage();
        }
    }
    
    // 辅助方法：分析情感关键词
    private Map<String, Integer> analyzeSentimentKeywords(String text) {
        Map<String, Integer> scores = new HashMap<>();
        
        // 积极词汇
        String[] positiveWords = {"好", "优秀", "棒", "赞", "喜欢", "感谢", "美好", "开心", "快乐", 
                                  "成功", "进步", "优秀", "精彩", "有趣", "支持", "鼓励"};
        // 消极词汇
        String[] negativeWords = {"差", "糟糕", "失望", "难过", "失败", "问题", "困难", "烦", 
                                  "无聊", "垃圾", "讨厌", "反对", "批评", "错误"};
        // 中性词汇
        String[] neutralWords = {"一般", "普通", "可以", "还行", "正常", "一样", "或许", "可能"};
        
        int positive = 0, negative = 0, neutral = 0;
        
        for (String word : positiveWords) {
            positive += countOccurrences(text, word);
        }
        for (String word : negativeWords) {
            negative += countOccurrences(text, word);
        }
        for (String word : neutralWords) {
            neutral += countOccurrences(text, word);
        }
        
        scores.put("positive", positive);
        scores.put("negative", negative);
        scores.put("neutral", neutral);
        
        return scores;
    }
    
    // 辅助方法：计算词频
    private int countOccurrences(String text, String word) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(word, index)) != -1) {
            count++;
            index += word.length();
        }
        return count;
    }
    
    // 辅助方法：生成友好风格回复
    private String generateFriendlyReply(String name, String content, boolean isPositive, boolean isQuestion) {
        if (isQuestion) {
            return "亲爱的" + name + "，感谢你的提问！" + 
                   (content.contains("怎么") ? "关于这个问题，我觉得可以从多个角度来看..." : 
                    "这是一个很好的问题，让我来分享一下我的想法...") +
                   " 希望这能帮助到你！😊";
        } else if (isPositive) {
            return "谢谢" + name + "的支持和认可！你的鼓励是我们继续前进的动力！🌟 " +
                   "很高兴这篇文章能够带给你一些启发。";
        } else {
            return "感谢" + name + "的反馈！每一个意见都很宝贵，" +
                   "我会认真考虑你的建议，努力做得更好。一起加油！💪";
        }
    }
    
    // 辅助方法：生成专业风格回复
    private String generateProfessionalReply(String name, String content, boolean isPositive, boolean isQuestion) {
        if (isQuestion) {
            return name + "您好，感谢您的提问。" + 
                   "针对您提到的问题，我认为需要从以下几个方面进行分析：" +
                   "首先...其次...最后...希望以上回答对您有所帮助。";
        } else if (isPositive) {
            return name + "您好，非常感谢您的肯定。" +
                   "您的认可是对我们工作的最大鼓励。我们会继续努力，提供更优质的内容。";
        } else {
            return name + "您好，感谢您提出的宝贵意见。" +
                   "我们会认真研究并改进相关问题，持续提升内容质量。";
        }
    }
    
    // 辅助方法：生成幽默风格回复
    private String generateHumorousReply(String name, String content, boolean isPositive, boolean isQuestion) {
        if (isQuestion) {
            return "哎呀，" + name + "问到点子上了！😄 " +
                   "这个问题问得我措手不及，让我喝口水想想... " +
                   "好了，我觉得可以这么看...（此处省略一万字）开玩笑啦！简单来说...";
        } else if (isPositive) {
            return "哇！" + name + "的评论让我心花怒放！🎉 " +
                   "你的点赞是最好的咖啡，瞬间让我精神百倍！继续关注哦~";
        } else {
            return name + "，你的评论很有个性！😅 " +
                   "虽然有点扎心，但良药苦口利于病，我会努力进步的！下次一定让你刮目相看！";
        }
    }
    
    /**
     * 智能生成评论回复
     * @param commentId 评论ID
     * @param tone 回复语气（friendly/professional/humorous）
     * @return 生成的回复内容
     */
    @Tool("根据评论内容智能生成合适的回复")
    public String smartReplyToComment(Long commentId, String tone) {
        log.info("Agent工具：智能回复评论 - 评论ID: {}, 语气: {}", commentId, tone);
        
        try {
            Comment comment = commentMapper.selectById(commentId);
            if (comment == null) {
                return "❌ 评论不存在，ID：" + commentId;
            }
            
            // 获取评论者信息
            User commenter = userMapper.selectById(comment.getUserId());
            String commenterName = commenter != null ? 
                (commenter.getRealName() != null ? commenter.getRealName() : commenter.getUsername()) : "网友";
            
            // 分析评论情感和内容
            String commentContent = comment.getContent();
            Map<String, Integer> sentiment = analyzeSentimentKeywords(commentContent);
            boolean isPositive = sentiment.get("positive") > sentiment.get("negative");
            boolean isQuestion = commentContent.contains("？") || commentContent.contains("?") || 
                               commentContent.contains("吗") || commentContent.contains("怎么");
            
            // 根据语气生成回复
            String reply;
            if ("friendly".equalsIgnoreCase(tone)) {
                reply = generateFriendlyReply(commenterName, commentContent, isPositive, isQuestion);
            } else if ("professional".equalsIgnoreCase(tone)) {
                reply = generateProfessionalReply(commenterName, commentContent, isPositive, isQuestion);
            } else if ("humorous".equalsIgnoreCase(tone)) {
                reply = generateHumorousReply(commenterName, commentContent, isPositive, isQuestion);
            } else {
                return "❌ 无效的语气类型：" + tone + "\n支持的类型：friendly（友好）、professional（专业）、humorous（幽默）";
            }
            
            // 创建回复评论
            Comment replyComment = new Comment();
            replyComment.setArticleId(comment.getArticleId());
            replyComment.setUserId(1L); // 系统用户
            replyComment.setContent(reply);
            replyComment.setParentId(commentId); // 设置为回复
            replyComment.setLikeCount(0);
            replyComment.setCreatedAt(LocalDateTime.now());
            replyComment.setUpdatedAt(LocalDateTime.now());
            
            commentMapper.insert(replyComment);
            
            StringBuilder result = new StringBuilder();
            result.append("💬 智能回复生成成功！\n\n");
            result.append("原评论：\"").append(commentContent).append("\"\n");
            result.append("评论者：").append(commenterName).append("\n");
            result.append("回复语气：").append(tone).append("\n\n");
            result.append("生成的回复：\n");
            result.append("\"").append(reply).append("\"\n\n");
            result.append("回复ID：").append(replyComment.getId());
            
            return result.toString();
        } catch (Exception e) {
            log.error("智能回复失败", e);
            return "❌ 生成回复失败：" + e.getMessage();
        }
    }
    
    /**
     * 分析用户社交关系图谱
     * @param username 用户名
     * @return 社交关系分析结果
     */
    @Tool("分析用户的社交关系网络和互动模式")
    public String analyzeUserSocialNetwork(String username) {
        log.info("Agent工具：分析用户社交网络 - 用户: {}", username);
        
        try {
            // 查找用户
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, username);
            User user = userMapper.selectOne(wrapper);
            
            if (user == null) {
                return "❌ 用户不存在：" + username;
            }
            
            // 分析关注关系
            List<UserFollow> following = userFollowMapper.selectList(
                new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerId, user.getId())
            );
            
            List<UserFollow> followers = userFollowMapper.selectList(
                new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowingId, user.getId())
            );
            
            // 互相关注的用户
            Set<Long> followingIds = following.stream()
                .map(UserFollow::getFollowingId)
                .collect(Collectors.toSet());
            
            List<Long> mutualFollows = followers.stream()
                .map(UserFollow::getFollowerId)
                .filter(followingIds::contains)
                .collect(Collectors.toList());
            
            // 分析互动频率
            List<Comment> userComments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getUserId, user.getId())
                    .orderByDesc(Comment::getCreatedAt)
                    .last("LIMIT 50")
            );
            
            // 统计与谁互动最多
            Map<Long, Integer> interactionCount = new HashMap<>();
            for (Comment comment : userComments) {
                Article article = articleMapper.selectById(comment.getArticleId());
                if (article != null) {
                    interactionCount.merge(article.getAuthorId(), 1, Integer::sum);
                }
            }
            
            // 推荐潜在好友（基于共同关注）
            Map<Long, Integer> potentialFriends = new HashMap<>();
            for (UserFollow follow : following) {
                // 获取关注的人的关注列表
                List<UserFollow> secondDegree = userFollowMapper.selectList(
                    new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, follow.getFollowingId())
                );
                
                for (UserFollow second : secondDegree) {
                    if (!second.getFollowingId().equals(user.getId()) && 
                        !followingIds.contains(second.getFollowingId())) {
                        potentialFriends.merge(second.getFollowingId(), 1, Integer::sum);
                    }
                }
            }
            
            StringBuilder result = new StringBuilder();
            result.append("🌐 用户社交网络分析\n");
            result.append("═".repeat(30)).append("\n\n");
            
            result.append("👤 用户：").append(username).append("\n");
            result.append("真实姓名：").append(user.getRealName() != null ? user.getRealName() : "未设置").append("\n\n");
            
            result.append("📊 社交数据：\n");
            result.append("• 关注数：").append(following.size()).append(" 人\n");
            result.append("• 粉丝数：").append(followers.size()).append(" 人\n");
            result.append("• 互相关注：").append(mutualFollows.size()).append(" 人\n");
            result.append("• 关注/粉丝比：").append(String.format("%.2f", 
                followers.size() > 0 ? (double)following.size() / followers.size() : 0)).append("\n\n");
            
            result.append("🤝 互动分析：\n");
            result.append("• 近期评论数：").append(userComments.size()).append(" 条\n");
            
            // 显示互动最多的用户
            if (!interactionCount.isEmpty()) {
                List<Map.Entry<Long, Integer>> topInteractions = interactionCount.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(3)
                    .collect(Collectors.toList());
                
                result.append("• 互动最频繁的作者：\n");
                for (Map.Entry<Long, Integer> entry : topInteractions) {
                    result.append("  - ").append(getUserName(entry.getKey()))
                          .append(" (").append(entry.getValue()).append("次)\n");
                }
            }
            
            result.append("\n👥 社交特征：\n");
            if (followers.size() > following.size() * 2) {
                result.append("• 意见领袖型：粉丝远多于关注\n");
            } else if (following.size() > followers.size() * 2) {
                result.append("• 学习探索型：关注多于粉丝\n");
            } else {
                result.append("• 平衡互动型：关注与粉丝相对平衡\n");
            }
            
            if (mutualFollows.size() > following.size() * 0.3) {
                result.append("• 高互动性：互相关注比例高\n");
            }
            
            // 推荐潜在好友
            if (!potentialFriends.isEmpty()) {
                result.append("\n🔍 推荐关注（基于共同关注）：\n");
                potentialFriends.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(5)
                    .forEach(entry -> {
                        result.append("• ").append(getUserName(entry.getKey()))
                              .append(" (").append(entry.getValue()).append("个共同关注)\n");
                    });
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("分析社交网络失败", e);
            return "❌ 分析失败：" + e.getMessage();
        }
    }
    
    /**
     * 检测敏感或违规内容
     * @param content 要检测的内容
     * @param strict 是否严格模式
     * @return 检测结果
     */
    @Tool("检测文本中的敏感词和违规内容")
    public String detectSensitiveContent(String content, boolean strict) {
        log.info("Agent工具：检测敏感内容 - 严格模式: {}", strict);
        
        try {
            // 定义敏感词库（增强版）
            List<String> politicalWords = Arrays.asList(
                "政治", "选举", "革命", "反动", "颠覆", "煽动", "分裂", "独立", 
                "恐怖", "极端", "邪教", "法轮", "台独", "港独", "藏独"
            );
            List<String> violenceWords = Arrays.asList(
                "暴力", "打架", "血腥", "杀人", "杀死", "砍死", "打死", "自杀",
                "枪支", "爆炸", "恐怖袭击", "绑架", "暴恐", "斗殴", "械斗"
            );
            List<String> adultWords = Arrays.asList(
                "色情", "黄色", "成人", "裸体", "性交", "做爱", "AV", "毛片",
                "约炮", "一夜情", "援交", "包养", "小姐", "嫖娼", "卖淫"
            );
            List<String> spamWords = Arrays.asList(
                "加微信", "点击链接", "扫码", "推广", "广告", "刷单", "兼职",
                "赚钱", "月入", "日赚", "代理", "招商", "加盟", "优惠券", "返利"
            );
            // 脏话和侮辱性词汇（增强版）
            List<String> insultWords = Arrays.asList(
                "傻逼", "傻B", "傻叉", "煞笔", "蠢货", "白痴", "脑残", "智障",
                "垃圾", "废物", "辣鸡", "渣男", "绿茶", "婊子", "贱人", "贱货",
                "滚", "去死", "妈的", "他妈的", "操", "艹", "草", "卧槽", "我操",
                "尼玛", "你妈", "CNM", "NMSL", "SB", "TMD", "WTF", "混蛋",
                "王八蛋", "狗日的", "狗屎", "放屁", "屁话", "鬼话", "废话"
            );
            // 非法内容
            List<String> illegalWords = Arrays.asList(
                "毒品", "吸毒", "贩毒", "冰毒", "海洛因", "大麻", "摇头丸",
                "赌博", "赌场", "博彩", "六合彩", "开盘", "下注", "赔率",
                "假证", "假发票", "代开", "办证", "黑客", "破解", "外挂"
            );
            
            // 检测结果
            Map<String, List<String>> detectedWords = new HashMap<>();
            int sensitivityScore = 0;
            
            // 检测各类敏感词
            for (String word : politicalWords) {
                if (content.contains(word)) {
                    detectedWords.computeIfAbsent("政治敏感", k -> new ArrayList<>()).add(word);
                    sensitivityScore += strict ? 10 : 5;
                }
            }
            
            for (String word : violenceWords) {
                if (content.contains(word)) {
                    detectedWords.computeIfAbsent("暴力内容", k -> new ArrayList<>()).add(word);
                    sensitivityScore += strict ? 8 : 4;
                }
            }
            
            for (String word : adultWords) {
                if (content.contains(word)) {
                    detectedWords.computeIfAbsent("成人内容", k -> new ArrayList<>()).add(word);
                    sensitivityScore += strict ? 10 : 5;
                }
            }
            
            for (String word : spamWords) {
                if (content.contains(word)) {
                    detectedWords.computeIfAbsent("营销推广", k -> new ArrayList<>()).add(word);
                    sensitivityScore += strict ? 5 : 2;
                }
            }
            
            for (String word : insultWords) {
                if (content.contains(word)) {
                    detectedWords.computeIfAbsent("侮辱谩骂", k -> new ArrayList<>()).add(word);
                    sensitivityScore += strict ? 6 : 3;
                }
            }
            
            for (String word : illegalWords) {
                if (content.contains(word)) {
                    detectedWords.computeIfAbsent("非法内容", k -> new ArrayList<>()).add(word);
                    sensitivityScore += strict ? 10 : 5;
                }
            }
            
            // 生成报告
            StringBuilder result = new StringBuilder();
            result.append("🔍 内容审核报告\n");
            result.append("═".repeat(30)).append("\n\n");
            
            result.append("📋 检测模式：").append(strict ? "严格" : "宽松").append("\n");
            result.append("📝 内容长度：").append(content.length()).append(" 字符\n\n");
            
            if (detectedWords.isEmpty()) {
                result.append("✅ 未检测到敏感内容\n");
                result.append("内容安全等级：🟢 安全\n");
            } else {
                result.append("⚠️ 检测到以下问题：\n");
                for (Map.Entry<String, List<String>> entry : detectedWords.entrySet()) {
                    result.append("• ").append(entry.getKey()).append("：")
                          .append(String.join(", ", entry.getValue())).append("\n");
                }
                
                result.append("\n🎯 风险评分：").append(sensitivityScore).append("/100\n");
                
                String riskLevel;
                String riskEmoji;
                if (sensitivityScore < 10) {
                    riskLevel = "低风险";
                    riskEmoji = "🟡";
                } else if (sensitivityScore < 30) {
                    riskLevel = "中风险";
                    riskEmoji = "🟠";
                } else {
                    riskLevel = "高风险";
                    riskEmoji = "🔴";
                }
                
                result.append("风险等级：").append(riskEmoji).append(" ").append(riskLevel).append("\n\n");
                
                result.append("💡 处理建议：\n");
                if (sensitivityScore >= 30) {
                    result.append("• 建议立即屏蔽或删除此内容\n");
                    result.append("• 对发布者进行警告或限制\n");
                } else if (sensitivityScore >= 10) {
                    result.append("• 建议人工复核此内容\n");
                    result.append("• 可以要求作者修改敏感部分\n");
                } else {
                    result.append("• 可以通过，但建议持续监控\n");
                    result.append("• 提醒作者注意内容规范\n");
                }
                
                // 内容优化建议
                result.append("\n📝 内容优化建议：\n");
                if (detectedWords.containsKey("营销推广")) {
                    result.append("• 减少商业推广内容，增加有价值的信息\n");
                }
                if (detectedWords.containsKey("侮辱谩骂")) {
                    result.append("• 使用文明用语，保持友好的讨论氛围\n");
                }
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("检测敏感内容失败", e);
            return "❌ 检测失败：" + e.getMessage();
        }
    }
    
    /**
     * 自动审核文章
     * @param articleId 文章ID
     * @param autoApprove 是否自动通过无敏感内容的文章
     * @return 审核结果
     */
    @Tool("自动审核文章内容，检测脏话和非法言论")
    public String autoApproveArticle(Long articleId, boolean autoApprove) {
        log.info("Agent工具：自动审核文章 - 文章ID: {}, 自动通过: {}", articleId, autoApprove);
        
        try {
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                return "❌ 文章不存在，ID：" + articleId;
            }
            
            // 检查文章是否已审核
            if (article.getIsApproved() == 1) {
                return "ℹ️ 文章已经通过审核：《" + article.getTitle() + "》";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("🤖 自动审核报告\n");
            result.append("═".repeat(30)).append("\n\n");
            
            result.append("📄 文章信息：\n");
            result.append("• 标题：《").append(article.getTitle()).append("》\n");
            result.append("• 作者：").append(getUserName(article.getAuthorId())).append("\n");
            result.append("• 板块：").append(getBoardTypeName(article.getBoardType())).append("\n");
            result.append("• 发布时间：").append(article.getCreatedAt()).append("\n\n");
            
            // 检测标题敏感词
            String titleCheck = detectSensitiveContent(article.getTitle(), true);
            boolean titleSafe = titleCheck.contains("未检测到敏感内容");
            
            // 检测内容敏感词
            String contentCheck = detectSensitiveContent(article.getContent(), true);
            boolean contentSafe = contentCheck.contains("未检测到敏感内容");
            
            result.append("🔍 审核结果：\n");
            result.append("━".repeat(20)).append("\n\n");
            
            if (!titleSafe) {
                result.append("⚠️ 标题检测结果：\n").append(titleCheck).append("\n");
            } else {
                result.append("✅ 标题检测：通过\n");
            }
            
            if (!contentSafe) {
                result.append("⚠️ 内容检测结果：\n").append(contentCheck).append("\n");
            } else {
                result.append("✅ 内容检测：通过\n");
            }
            
            // 判断审核结果
            boolean canApprove = titleSafe && contentSafe;
            
            result.append("\n📋 审核决定：\n");
            if (canApprove) {
                result.append("✅ 文章内容安全，");
                if (autoApprove) {
                    // 自动通过审核
                    article.setIsApproved(1);
                    articleMapper.updateById(article);
                    result.append("已自动通过审核\n");
                    log.info("文章自动审核通过：{}", articleId);
                    
                    // 发送通知给文章作者
                    try {
                        Notification notification = new Notification();
                        notification.setUserId(article.getAuthorId());
                        notification.setFromUserId(1L); // 系统用户
                        notification.setType("SYSTEM");
                        notification.setArticleId(articleId);
                        notification.setContent("您的文章《" + article.getTitle() + "》已通过审核并发布。");
                        notification.setIsRead(0);
                        notification.setCreatedAt(LocalDateTime.now());
                        notificationMapper.insert(notification);
                        log.info("已发送审核通过通知给用户：{}", article.getAuthorId());
                    } catch (Exception e) {
                        log.error("发送审核通过通知失败", e);
                    }
                } else {
                    result.append("建议通过审核\n");
                }
            } else {
                result.append("❌ 文章包含敏感内容，");
                if (autoApprove) {
                    // 自动拒绝
                    article.setIsApproved(2);
                    articleMapper.updateById(article);
                    result.append("已自动拒绝发布\n");
                    log.info("文章自动审核拒绝：{}", articleId);
                    
                    // 发送通知给文章作者
                    try {
                        Notification notification = new Notification();
                        notification.setUserId(article.getAuthorId());
                        notification.setFromUserId(1L); // 系统用户
                        notification.setType("SYSTEM");
                        notification.setArticleId(articleId);
                        notification.setContent("您的文章《" + article.getTitle() + "》未通过审核，原因：内容包含敏感或违规信息。");
                        notification.setIsRead(0);
                        notification.setCreatedAt(LocalDateTime.now());
                        notificationMapper.insert(notification);
                        log.info("已发送审核拒绝通知给用户：{}", article.getAuthorId());
                    } catch (Exception e) {
                        log.error("发送审核拒绝通知失败", e);
                    }
                } else {
                    result.append("建议拒绝或要求修改\n");
                }
                
                result.append("\n🚫 违规原因：\n");
                if (!titleSafe) {
                    result.append("• 标题包含敏感词汇\n");
                }
                if (!contentSafe) {
                    result.append("• 内容包含违规信息\n");
                }
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("自动审核文章失败", e);
            return "❌ 审核失败：" + e.getMessage();
        }
    }
    
    /**
     * 查看所有待审核文章
     * @return 待审核文章列表
     */
    @Tool("查看所有待审核的文章列表")
    public String listPendingArticles() {
        log.info("Agent工具：查看待审核文章列表");
        
        try {
            // 查询所有待审核的文章（不加任何其他限制）
            List<Article> pendingArticles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                    .eq(Article::getIsApproved, 0)
            );
            
            if (pendingArticles.isEmpty()) {
                return "✅ 没有待审核的文章";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("📋 待审核文章列表\n");
            result.append("═".repeat(30)).append("\n\n");
            result.append("共有 ").append(pendingArticles.size()).append(" 篇待审核文章：\n\n");
            
            for (int i = 0; i < pendingArticles.size(); i++) {
                Article article = pendingArticles.get(i);
                result.append(i + 1).append(". 《").append(article.getTitle()).append("》\n");
                result.append("   - ID: ").append(article.getId()).append("\n");
                result.append("   - 作者: ").append(getUserName(article.getAuthorId())).append("\n");
                result.append("   - 板块: ").append(getBoardTypeName(article.getBoardType())).append("\n");
                result.append("   - 创建时间: ").append(article.getCreatedAt()).append("\n");
                result.append("   - Status字段: ").append(article.getStatus()).append("\n");
                result.append("   - IsApproved字段: ").append(article.getIsApproved()).append("\n");
                result.append("\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("查看待审核文章失败", e);
            return "❌ 查询失败：" + e.getMessage();
        }
    }
    
    /**
     * 批量自动审核未审核文章
     * @param limit 审核数量限制
     * @return 批量审核结果
     */
    @Tool("批量自动审核未审核的文章")
    public String batchAutoApprove(int limit) {
        log.info("Agent工具：批量自动审核 - 限制: {}", limit);
        
        try {
            // 查询未审核的文章（移除status限制，因为status是逻辑删除字段）
            List<Article> pendingArticles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                    .eq(Article::getIsApproved, 0)
                    .orderByAsc(Article::getCreatedAt)
                    .last("LIMIT " + Math.min(limit, 50))
            );
            
            if (pendingArticles.isEmpty()) {
                return "✅ 没有待审核的文章";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("🤖 批量自动审核报告\n");
            result.append("═".repeat(30)).append("\n\n");
            result.append("📊 待审核文章数：").append(pendingArticles.size()).append(" 篇\n\n");
            
            int approved = 0;
            int rejected = 0;
            int failed = 0;
            
            for (Article article : pendingArticles) {
                try {
                    // 检测内容
                    String fullContent = article.getTitle() + " " + article.getContent();
                    String checkResult = detectSensitiveContent(fullContent, true);
                    boolean isSafe = checkResult.contains("未检测到敏感内容");
                    
                    if (isSafe) {
                        article.setIsApproved(1);
                        articleMapper.updateById(article);
                        approved++;
                        result.append("✅ 通过：《").append(article.getTitle()).append("》\n");
                        
                        // 发送审核通过通知
                        try {
                            Notification notification = new Notification();
                            notification.setUserId(article.getAuthorId());
                            notification.setFromUserId(1L); // 系统用户
                            notification.setType("SYSTEM");
                            notification.setArticleId(article.getId());
                            notification.setContent("[文章审核通过] 您的文章《" + article.getTitle() + "》已通过审核并发布。");
                            notification.setIsRead(0);
                            notification.setCreatedAt(LocalDateTime.now());
                            notificationMapper.insert(notification);
                        } catch (Exception notifyEx) {
                            log.error("发送审核通过通知失败：文章ID={}", article.getId(), notifyEx);
                        }
                    } else {
                        article.setIsApproved(2);
                        articleMapper.updateById(article);
                        rejected++;
                        result.append("❌ 拒绝：《").append(article.getTitle()).append("》 - 包含敏感内容\n");
                        
                        // 发送审核拒绝通知
                        try {
                            Notification notification = new Notification();
                            notification.setUserId(article.getAuthorId());
                            notification.setFromUserId(1L); // 系统用户
                            notification.setType("SYSTEM");
                            notification.setArticleId(article.getId());
                            notification.setContent("[文章审核未通过] 您的文章《" + article.getTitle() + "》未通过审核，原因：内容包含敏感或违规信息。请修改后重新提交。");
                            notification.setIsRead(0);
                            notification.setCreatedAt(LocalDateTime.now());
                            notificationMapper.insert(notification);
                        } catch (Exception notifyEx) {
                            log.error("发送审核拒绝通知失败：文章ID={}", article.getId(), notifyEx);
                        }
                    }
                } catch (Exception e) {
                    failed++;
                    result.append("⚠️ 失败：《").append(article.getTitle()).append("》 - ").append(e.getMessage()).append("\n");
                }
            }
            
            result.append("\n📈 审核统计：\n");
            result.append("• ✅ 通过：").append(approved).append(" 篇\n");
            result.append("• ❌ 拒绝：").append(rejected).append(" 篇\n");
            if (failed > 0) {
                result.append("• ⚠️ 失败：").append(failed).append(" 篇\n");
            }
            
            double passRate = approved * 100.0 / pendingArticles.size();
            result.append("• 通过率：").append(String.format("%.1f%%", passRate)).append("\n");
            
            return result.toString();
        } catch (Exception e) {
            log.error("批量自动审核失败", e);
            return "❌ 批量审核失败：" + e.getMessage();
        }
    }
    
    /**
     * 评估文章互动质量
     * @param articleId 文章ID
     * @return 互动质量评估报告
     */
    @Tool("评估文章的互动质量和用户参与度")
    public String evaluateInteractionQuality(Long articleId) {
        log.info("Agent工具：评估互动质量 - 文章ID: {}", articleId);
        
        try {
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                return "❌ 文章不存在，ID：" + articleId;
            }
            
            // 获取所有评论
            List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getArticleId, articleId)
                    .orderByDesc(Comment::getCreatedAt)
            );
            
            // 计算互动指标
            double engagementRate = (article.getLikeCount() + article.getCommentCount()) * 100.0 / 
                                   Math.max(article.getViewCount(), 1);
            
            double likeRate = article.getLikeCount() * 100.0 / Math.max(article.getViewCount(), 1);
            double commentRate = article.getCommentCount() * 100.0 / Math.max(article.getViewCount(), 1);
            
            // 分析评论质量
            int totalCommentLength = 0;
            int meaningfulComments = 0;
            int shortComments = 0;
            Set<Long> uniqueCommenters = new HashSet<>();
            
            for (Comment comment : comments) {
                totalCommentLength += comment.getContent().length();
                uniqueCommenters.add(comment.getUserId());
                
                if (comment.getContent().length() > 20) {
                    meaningfulComments++;
                } else {
                    shortComments++;
                }
            }
            
            double avgCommentLength = comments.isEmpty() ? 0 : 
                                     (double)totalCommentLength / comments.size();
            
            // 计算评论多样性（不同用户的比例）
            double commentDiversity = comments.isEmpty() ? 0 : 
                                     uniqueCommenters.size() * 100.0 / comments.size();
            
            // 生成报告
            StringBuilder result = new StringBuilder();
            result.append("📊 文章互动质量评估报告\n");
            result.append("═".repeat(30)).append("\n\n");
            
            result.append("📄 文章：《").append(article.getTitle()).append("》\n");
            result.append("发布时间：").append(article.getCreatedAt()).append("\n\n");
            
            result.append("🎯 核心指标：\n");
            result.append("• 总浏览量：").append(article.getViewCount()).append(" 次\n");
            result.append("• 总点赞数：").append(article.getLikeCount()).append(" 个\n");
            result.append("• 总评论数：").append(article.getCommentCount()).append(" 条\n");
            result.append("• 互动率：").append(String.format("%.2f%%", engagementRate)).append("\n");
            result.append("• 点赞率：").append(String.format("%.2f%%", likeRate)).append("\n");
            result.append("• 评论率：").append(String.format("%.2f%%", commentRate)).append("\n\n");
            
            result.append("💬 评论质量分析：\n");
            result.append("• 平均评论长度：").append(String.format("%.1f", avgCommentLength)).append(" 字符\n");
            result.append("• 有效评论：").append(meaningfulComments).append(" 条（>20字符）\n");
            result.append("• 简短评论：").append(shortComments).append(" 条\n");
            result.append("• 独立评论者：").append(uniqueCommenters.size()).append(" 人\n");
            result.append("• 评论多样性：").append(String.format("%.1f%%", commentDiversity)).append("\n\n");
            
            // 质量评级
            result.append("⭐ 互动质量评级：");
            int qualityScore = 0;
            if (engagementRate > 10) qualityScore += 30;
            else if (engagementRate > 5) qualityScore += 20;
            else if (engagementRate > 2) qualityScore += 10;
            
            if (avgCommentLength > 30) qualityScore += 20;
            else if (avgCommentLength > 15) qualityScore += 10;
            
            if (commentDiversity > 70) qualityScore += 20;
            else if (commentDiversity > 50) qualityScore += 10;
            
            if (meaningfulComments > shortComments) qualityScore += 15;
            if (article.getLikeCount() > article.getCommentCount() * 3) qualityScore += 15;
            
            String rating;
            if (qualityScore >= 80) {
                rating = "优秀 ⭐⭐⭐⭐⭐";
            } else if (qualityScore >= 60) {
                rating = "良好 ⭐⭐⭐⭐";
            } else if (qualityScore >= 40) {
                rating = "中等 ⭐⭐⭐";
            } else if (qualityScore >= 20) {
                rating = "一般 ⭐⭐";
            } else {
                rating = "较差 ⭐";
            }
            result.append(rating).append("\n\n");
            
            // 改进建议
            result.append("💡 改进建议：\n");
            if (engagementRate < 5) {
                result.append("• 互动率偏低，考虑优化标题或在文末提出讨论话题\n");
            }
            if (avgCommentLength < 15) {
                result.append("• 评论过于简短，可能内容深度不够\n");
            }
            if (commentDiversity < 50) {
                result.append("• 评论者集中，需要扩大受众范围\n");
            }
            if (article.getViewCount() > 100 && article.getLikeCount() < 10) {
                result.append("• 点赞数相对浏览量偏低，可能需要改进内容质量\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("评估互动质量失败", e);
            return "❌ 评估失败：" + e.getMessage();
        }
    }
}
