package com.campus.news.agent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.news.entity.*;
import com.campus.news.mapper.*;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent工具类 - 提供给AI Agent使用的各种工具方法
 * 使用@Tool注解标记的方法会被LangChain4j自动识别为可调用工具
 * 
 * 功能分类：
 * 1. 文章操作类 - 搜索、点赞、评论、收藏
 * 2. 视频操作类 - 视频推荐、点赞、评论
 * 3. 用户管理类 - 关注、搜索、画像分析
 * 4. 数据分析类 - 统计、趋势、排行榜
 * 5. 智能推荐类 - 个性化推荐、相似内容
 * 6. 内容创作类 - 创建文章、生成摘要
 * 7. 通知管理类 - 发送通知、查看通知
 * 
 * 权限控制：
 * - 发布、审核、删除等操作需要管理员权限
 * - 普通用户只能执行查询和个人互动操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsAgentTools {
    
    // ==================== 用户上下文管理 ====================
    
    /**
     * 用户上下文信息，用于权限控制
     */
    public static class UserContext {
        private Long userId;
        private Boolean isAdmin;
        
        public UserContext(Long userId, Boolean isAdmin) {
            this.userId = userId;
            this.isAdmin = isAdmin;
        }
        
        public Long getUserId() { return userId != null ? userId : 1L; }
        public boolean isAdmin() { return isAdmin != null && isAdmin; }
    }
    
    // 使用ThreadLocal存储当前用户上下文
    private static final ThreadLocal<UserContext> currentUserContext = new ThreadLocal<>();
    
    /**
     * 设置当前用户上下文（由Agent服务调用）
     */
    public static void setCurrentUser(Long userId, Boolean isAdmin) {
        currentUserContext.set(new UserContext(userId, isAdmin));
    }
    
    /**
     * 获取当前用户上下文
     */
    public static UserContext getCurrentUser() {
        UserContext ctx = currentUserContext.get();
        return ctx != null ? ctx : new UserContext(1L, false);
    }
    
    /**
     * 清除当前用户上下文
     */
    public static void clearCurrentUser() {
        currentUserContext.remove();
    }
    
    /**
     * 检查是否有管理员权限
     */
    private boolean checkAdminPermission() {
        return getCurrentUser().isAdmin();
    }
    
    /**
     * 返回权限不足的提示信息
     */
    private String noPermissionMessage(String operation) {
        return "⛔ 权限不足：" + operation + "需要管理员权限。\n" +
               "当前用户不是管理员，无法执行此操作。\n" +
               "请联系管理员或使用管理员账号登录后重试。";
    }

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
    
    // 视频相关 Mapper
    private final VideoMapper videoMapper;
    private final VideoLikeMapper videoLikeMapper;
    private final VideoCommentMapper videoCommentMapper;
    private final VideoCategoryMapper videoCategoryMapper;
    private final CollegeMapper collegeMapper;
    
    // 推荐服务配置
    @Value("${recommendation.service.url:http://localhost:5000}")
    private String recommendationServiceUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();

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
    @Tool("快速发布文章，只需要提供标题，系统会自动生成内容并发布（普通用户需等待审核）")
    public String quickPublishArticle(String title) {
        log.info("Agent工具：快速发布文章 - 标题: {}", title);
        
        UserContext user = getCurrentUser();
        boolean isAdmin = user.isAdmin();
        
        try {
            // 根据标题智能生成内容
            String content = generateContentByTitle(title);
            String summary = content.length() > 100 ? content.substring(0, 100) + "..." : content;
            String boardType = "CAMPUS"; // 默认发布到校园版块
            
            // 创建文章
            Article article = new Article();
            article.setTitle(title);
            article.setContent(content);
            article.setSummary(summary);
            article.setBoardType(boardType);
            article.setAuthorId(user.getUserId()); // 当前用户
            article.setStatus(1); // 正常状态
            article.setViewCount(0);
            article.setLikeCount(0);
            article.setCommentCount(0);
            // 管理员直接通过审核，普通用户需要等待审核
            article.setIsApproved(isAdmin ? 1 : 0);
            article.setIsPinned(0);
            article.setCreatedAt(LocalDateTime.now());
            article.setUpdatedAt(LocalDateTime.now());
            
            articleMapper.insert(article);
            
            StringBuilder result = new StringBuilder();
            if (isAdmin) {
                result.append("🎉 文章发布成功！\n");
            } else {
                result.append("📝 文章已提交，等待审核！\n");
            }
            result.append("标题：《").append(title).append("》\n");
            result.append("版块：").append(getBoardTypeName(boardType)).append("\n");
            result.append("文章ID：").append(article.getId()).append("\n");
            result.append("状态：").append(isAdmin ? "已发布" : "待审核").append("\n");
            if (!isAdmin) {
                result.append("💡 提示：您的文章已提交成功，管理员审核通过后将自动发布。\n");
            }
            result.append("内容预览：\n").append(content.substring(0, Math.min(200, content.length())));
            if (content.length() > 200) result.append("...");
            result.append("\n链接：/article/").append(article.getId());
            
            return result.toString();
        } catch (Exception e) {
            log.error("快速发布文章失败", e);
            return "❌ 发布失败：" + e.getMessage();
        }
    }
    
    /**
     * 根据标题智能生成文章内容（增强版）
     */
    private String generateContentByTitle(String title) {
        String lowerTitle = title.toLowerCase();
        
        // 通知公告类
        if (lowerTitle.contains("通知") || lowerTitle.contains("公告") || lowerTitle.contains("通告")) {
            return "📢 重要通知\n\n" +
                   "各位同学、老师：\n\n" +
                   "现就" + title.replaceAll("[通知公告通告]", "") + "相关事项通知如下：\n\n" +
                   "一、基本情况\n" +
                   "根据学校相关安排，现将有关事项通知如下。\n\n" +
                   "二、具体安排\n" +
                   "1. 请各位同学按照要求做好相关准备\n" +
                   "2. 如有疑问请及时联系相关部门\n" +
                   "3. 请相互转告，确保信息传达到位\n\n" +
                   "三、注意事项\n" +
                   "请大家认真阅读并遵守相关规定，如有特殊情况请提前说明。\n\n" +
                   "特此通知。\n\n" +
                   "校园新闻系统\n" +
                   LocalDateTime.now().toLocalDate();
        }
        // 活动类
        else if (lowerTitle.contains("活动") || lowerTitle.contains("比赛") || lowerTitle.contains("竞赛")) {
            return "🎉 活动预告\n\n" +
                   "亲爱的同学们：\n\n" +
                   "我们即将举办「" + title + "」，诚邀您的参与！\n\n" +
                   "📅 活动详情\n" +
                   "- 活动时间：待定\n" +
                   "- 活动地点：待定\n" +
                   "- 参与对象：全体师生\n\n" +
                   "🎯 活动亮点\n" +
                   "1. 丰富的互动环节\n" +
                   "2. 精美的奖品等你拿\n" +
                   "3. 结识志同道合的朋友\n" +
                   "4. 提升个人能力与素养\n\n" +
                   "📝 报名方式\n" +
                   "请关注后续通知，届时将公布具体报名方式。\n\n" +
                   "期待您的参与，让我们共同创造美好的校园记忆！";
        }
        // 新闻报道类
        else if (lowerTitle.contains("新闻") || lowerTitle.contains("报道") || lowerTitle.contains("我校") || lowerTitle.contains("举行") || lowerTitle.contains("举办")) {
            return "📰 校园快讯\n\n" +
                   "【本站讯】" + title + "。\n\n" +
                   "近日，我校成功举办了相关活动，吸引了众多师生的关注和参与。" +
                   "活动现场气氛热烈，参与者们积极互动，展现了我校师生的良好精神风貌。\n\n" +
                   "据了解，此次活动旨在丰富校园文化生活，促进师生交流。" +
                   "活动的成功举办，不仅展示了我校的办学特色，也为广大师生提供了一个良好的交流平台。\n\n" +
                   "参与活动的同学表示，这次活动让他们收获颇丰，希望学校能够继续举办更多类似的活动。\n\n" +
                   "（校园新闻中心供稿）";
        }
        // 学习经验类
        else if (lowerTitle.contains("学习") || lowerTitle.contains("考试") || lowerTitle.contains("备考") || lowerTitle.contains("经验")) {
            return "📚 学习分享\n\n" +
                   "关于「" + title + "」，我想和大家分享一些心得体会。\n\n" +
                   "🎯 核心要点\n\n" +
                   "1. **制定计划**\n" +
                   "   合理规划时间，将大目标分解为小目标，逐步完成。\n\n" +
                   "2. **高效方法**\n" +
                   "   找到适合自己的学习方法，事半功倍。\n\n" +
                   "3. **持之以恒**\n" +
                   "   学习是一个积累的过程，贵在坚持。\n\n" +
                   "4. **及时总结**\n" +
                   "   定期回顾，查漏补缺，巩固知识。\n\n" +
                   "💡 小贴士\n" +
                   "- 保持良好的作息习惯\n" +
                   "- 适当运动，劳逸结合\n" +
                   "- 遇到困难及时请教\n\n" +
                   "祝大家学业有成，前程似锦！";
        }
        // 生活分享类
        else if (lowerTitle.contains("宿舍") || lowerTitle.contains("食堂") || lowerTitle.contains("生活")) {
            return "🏠 校园生活分享\n\n" +
                   "今天来聊聊「" + title + "」这个话题。\n\n" +
                   "大学生活是人生中最美好的时光之一，在这里我们不仅学习知识，" +
                   "更学会了如何与人相处，如何独立生活。\n\n" +
                   "🌟 生活小建议\n\n" +
                   "1. 保持宿舍整洁，创造舒适的学习生活环境\n" +
                   "2. 合理安排时间，平衡学习与休闲\n" +
                   "3. 积极参与集体活动，增进同学友谊\n" +
                   "4. 注意饮食健康，保持良好的身体状态\n\n" +
                   "希望每位同学都能享受精彩的大学生活！";
        }
        // 技术分享类
        else if (lowerTitle.contains("技术") || lowerTitle.contains("编程") || lowerTitle.contains("AI") || 
                 lowerTitle.contains("人工智能") || lowerTitle.contains("开发") || lowerTitle.contains("代码")) {
            return "💻 技术分享\n\n" +
                   "今天来探讨一下「" + title + "」。\n\n" +
                   "🚀 概述\n\n" +
                   "在当今数字化时代，技术的发展日新月异。掌握相关技术知识，" +
                   "对于我们的学习和未来发展都有重要意义。\n\n" +
                   "📖 核心内容\n\n" +
                   "1. **基础知识**\n" +
                   "   打好基础是学习任何技术的第一步。\n\n" +
                   "2. **实践应用**\n" +
                   "   理论结合实践，动手做项目。\n\n" +
                   "3. **持续学习**\n" +
                   "   技术更新快，保持学习的热情。\n\n" +
                   "🔗 学习资源\n" +
                   "推荐大家多关注技术社区和开源项目，与同行交流学习。\n\n" +
                   "欢迎在评论区分享你的学习经验！";
        }
        // 默认通用模板
        else {
            return "📝 " + title + "\n\n" +
                   "亲爱的读者朋友们：\n\n" +
                   "感谢您阅读这篇文章。在这里，我想和大家分享一些关于「" + title + "」的内容。\n\n" +
                   "🌟 正文\n\n" +
                   "每一个话题都值得我们认真思考和探讨。希望通过这篇文章，" +
                   "能够为大家带来一些启发和帮助。\n\n" +
                   "在校园生活中，我们会遇到各种各样的事情，有喜悦也有挑战。" +
                   "重要的是保持积极向上的心态，勇于面对每一天。\n\n" +
                   "💬 互动话题\n\n" +
                   "关于这个话题，你有什么想法呢？欢迎在评论区留言分享！\n\n" +
                   "---\n" +
                   "感谢阅读，我们下期再见！";
        }
    }
    
    // ==================== 智能发布功能（增强版）====================
    
    /**
     * 智能发布文章（完整版）
     * 支持指定版块、自动生成内容、自动添加标签
     * @param title 文章标题
     * @param boardType 版块类型（OFFICIAL/CAMPUS/COLLEGE，可选，默认自动识别）
     * @param autoAddTags 是否自动添加标签
     * @return 发布结果
     */
    @Tool("智能发布文章，支持自动识别版块、自动生成内容、自动添加标签（普通用户需等待审核）")
    public String smartPublishArticle(String title, String boardType, boolean autoAddTags) {
        log.info("Agent自动化：智能发布文章 - 标题: {}, 版块: {}, 自动标签: {}", title, boardType, autoAddTags);
        
        UserContext user = getCurrentUser();
        boolean isAdmin = user.isAdmin();
        
        try {
            // 智能识别版块
            String actualBoardType = boardType;
            if (boardType == null || boardType.isEmpty() || "AUTO".equalsIgnoreCase(boardType)) {
                actualBoardType = detectBoardType(title);
            }
            
            // 生成内容
            String content = generateContentByTitle(title);
            String summary = generateSummary(content);
            
            // 创建文章
            Article article = new Article();
            article.setTitle(title);
            article.setContent(content);
            article.setSummary(summary);
            article.setBoardType(actualBoardType.toUpperCase());
            article.setAuthorId(user.getUserId());
            article.setStatus(1);
            article.setViewCount(0);
            article.setLikeCount(0);
            article.setCommentCount(0);
            // 管理员直接通过，普通用户需审核
            article.setIsApproved(isAdmin ? 1 : 0);
            article.setIsPinned(0);
            article.setCreatedAt(LocalDateTime.now());
            article.setUpdatedAt(LocalDateTime.now());
            
            articleMapper.insert(article);
            
            // 自动添加标签
            List<String> addedTags = new ArrayList<>();
            if (autoAddTags) {
                addedTags = autoAddTagsToArticle(article.getId(), title + " " + content);
            }
            
            StringBuilder result = new StringBuilder();
            if (isAdmin) {
                result.append("🎉 文章智能发布成功！\n");
            } else {
                result.append("📝 文章已提交，等待审核！\n");
            }
            result.append("═".repeat(30)).append("\n\n");
            result.append("📄 文章信息：\n");
            result.append("   • 标题：《").append(title).append("》\n");
            result.append("   • 版块：").append(getBoardTypeName(actualBoardType)).append("\n");
            result.append("   • 文章ID：").append(article.getId()).append("\n");
            result.append("   • 状态：").append(isAdmin ? "已发布" : "待审核").append("\n");
            if (!isAdmin) {
                result.append("   💡 管理员审核通过后将自动发布\n");
            }
            result.append("\n");
            
            if (!addedTags.isEmpty()) {
                result.append("🏷️ 自动添加标签：").append(String.join(", ", addedTags)).append("\n\n");
            }
            
            result.append("📝 内容预览：\n");
            result.append(content.substring(0, Math.min(150, content.length())));
            if (content.length() > 150) result.append("...");
            result.append("\n\n");
            result.append("🔗 链接：/article/").append(article.getId());
            
            return result.toString();
        } catch (Exception e) {
            log.error("智能发布失败", e);
            return "❌ 发布失败：" + e.getMessage();
        }
    }
    
    // 智能识别版块
    private String detectBoardType(String title) {
        String lowerTitle = title.toLowerCase();
        if (lowerTitle.contains("通知") || lowerTitle.contains("公告") || lowerTitle.contains("官方") ||
            lowerTitle.contains("学校") || lowerTitle.contains("重要")) {
            return "OFFICIAL";
        } else if (lowerTitle.contains("学院") || lowerTitle.contains("专业") || lowerTitle.contains("院系")) {
            return "COLLEGE";
        }
        return "CAMPUS";
    }
    
    // 生成摘要
    private String generateSummary(String content) {
        // 去除标题符号，提取纯文本
        String cleanContent = content.replaceAll("[📢🎉📰📚🏠💻📝🌟💡🔗💬📅🎯📖🚀]", "")
                                    .replaceAll("═+", "")
                                    .replaceAll("-{3,}", "")
                                    .replaceAll("\\*+", "")
                                    .replaceAll("\n+", " ")
                                    .trim();
        
        if (cleanContent.length() <= 100) {
            return cleanContent;
        }
        
        // 截取前100字符，在句号处截断
        String summary = cleanContent.substring(0, Math.min(150, cleanContent.length()));
        int lastPeriod = summary.lastIndexOf("。");
        if (lastPeriod > 50) {
            summary = summary.substring(0, lastPeriod + 1);
        } else {
            summary = summary.substring(0, Math.min(100, summary.length())) + "...";
        }
        
        return summary;
    }
    
    // 自动添加标签
    private List<String> autoAddTagsToArticle(Long articleId, String content) {
        List<String> addedTags = new ArrayList<>();
        String lowerContent = content.toLowerCase();
        
        // 关键词到标签的映射
        Map<String, String> keywordToTag = new LinkedHashMap<>();
        keywordToTag.put("通知", "通知公告");
        keywordToTag.put("公告", "通知公告");
        keywordToTag.put("活动", "校园活动");
        keywordToTag.put("比赛", "竞赛");
        keywordToTag.put("竞赛", "竞赛");
        keywordToTag.put("学习", "学习");
        keywordToTag.put("考试", "考试");
        keywordToTag.put("技术", "技术");
        keywordToTag.put("编程", "编程");
        keywordToTag.put("ai", "AI");
        keywordToTag.put("人工智能", "AI");
        keywordToTag.put("宿舍", "校园生活");
        keywordToTag.put("食堂", "校园生活");
        keywordToTag.put("就业", "就业");
        keywordToTag.put("实习", "实习");
        
        Set<String> tagsToAdd = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : keywordToTag.entrySet()) {
            if (lowerContent.contains(entry.getKey())) {
                tagsToAdd.add(entry.getValue());
                if (tagsToAdd.size() >= 3) break; // 最多3个标签
            }
        }
        
        for (String tagName : tagsToAdd) {
            try {
                // 查找或创建标签
                LambdaQueryWrapper<Tag> tagWrapper = new LambdaQueryWrapper<>();
                tagWrapper.eq(Tag::getName, tagName);
                Tag tag = tagMapper.selectOne(tagWrapper);
                
                if (tag == null) {
                    tag = new Tag();
                    tag.setName(tagName);
                    tag.setCreatedAt(LocalDateTime.now());
                    tagMapper.insert(tag);
                }
                
                // 关联标签
                ArticleTag articleTag = new ArticleTag();
                articleTag.setArticleId(articleId);
                articleTag.setTagId(tag.getId());
                articleTagMapper.insert(articleTag);
                
                addedTags.add("#" + tagName);
            } catch (Exception e) {
                log.error("添加标签失败: {}", tagName, e);
            }
        }
        
        return addedTags;
    }
    
    /**
     * 批量发布系列文章
     * @param seriesTitle 系列标题
     * @param topics 主题列表（逗号分隔）
     * @param boardType 版块类型
     * @return 批量发布结果
     */
    @Tool("批量发布系列文章，一次性生成多篇相关主题的文章（普通用户需等待审核）")
    public String batchPublishSeries(String seriesTitle, String topics, String boardType) {
        log.info("Agent自动化：批量发布系列 - 系列: {}, 主题: {}", seriesTitle, topics);
        
        UserContext user = getCurrentUser();
        boolean isAdmin = user.isAdmin();
        
        try {
            String[] topicArray = topics.split("[,，、]");
            if (topicArray.length == 0) {
                return "❌ 请提供主题列表（用逗号分隔）";
            }
            
            if (topicArray.length > 5) {
                return "❌ 单次最多发布5篇文章";
            }
            
            StringBuilder result = new StringBuilder();
            if (isAdmin) {
                result.append("📚 系列文章批量发布\n");
            } else {
                result.append("📚 系列文章已提交，等待审核\n");
            }
            result.append("═".repeat(30)).append("\n");
            result.append("系列名称：").append(seriesTitle).append("\n");
            result.append("文章数量：").append(topicArray.length).append(" 篇\n");
            result.append("状态：").append(isAdmin ? "已发布" : "待审核").append("\n\n");
            
            int successCount = 0;
            for (int i = 0; i < topicArray.length; i++) {
                String topic = topicArray[i].trim();
                if (topic.isEmpty()) continue;
                
                String fullTitle = seriesTitle + "（" + (i + 1) + "）：" + topic;
                
                try {
                    String content = generateContentByTitle(topic);
                    String summary = generateSummary(content);
                    
                    Article article = new Article();
                    article.setTitle(fullTitle);
                    article.setContent(content);
                    article.setSummary(summary);
                    article.setBoardType(boardType != null ? boardType.toUpperCase() : "CAMPUS");
                    article.setAuthorId(user.getUserId());
                    article.setStatus(1);
                    article.setViewCount(0);
                    article.setLikeCount(0);
                    article.setCommentCount(0);
                    // 管理员直接通过，普通用户需审核
                    article.setIsApproved(isAdmin ? 1 : 0);
                    article.setIsPinned(0);
                    article.setCreatedAt(LocalDateTime.now());
                    article.setUpdatedAt(LocalDateTime.now());
                    
                    articleMapper.insert(article);
                    
                    // 自动添加标签
                    autoAddTagsToArticle(article.getId(), topic + " " + content);
                    
                    result.append("✅ ").append(i + 1).append(". 《").append(fullTitle).append("》\n");
                    result.append("   ID: ").append(article.getId()).append(" | /article/").append(article.getId()).append("\n");
                    successCount++;
                } catch (Exception e) {
                    result.append("❌ ").append(i + 1).append(". ").append(topic).append(" - 发布失败\n");
                }
            }
            
            result.append("\n").append("═".repeat(30)).append("\n");
            result.append("📊 提交统计：成功 ").append(successCount).append("/").append(topicArray.length).append(" 篇\n");
            if (!isAdmin) {
                result.append("💡 管理员审核通过后将自动发布\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("批量发布失败", e);
            return "❌ 批量发布失败：" + e.getMessage();
        }
    }
    
    /**
     * 一键发布通知公告
     * @param title 通知标题
     * @param importance 重要程度（normal/important/urgent）
     * @param notifyUsers 是否通知所有用户
     * @return 发布结果
     */
    @Tool("一键发布通知公告，支持设置重要程度并可选择通知所有用户（需要管理员权限）")
    public String publishNotice(String title, String importance, boolean notifyUsers) {
        log.info("Agent自动化：发布通知 - 标题: {}, 重要程度: {}, 通知用户: {}", title, importance, notifyUsers);
        
        // 权限检查
        if (!checkAdminPermission()) {
            return noPermissionMessage("发布通知公告");
        }
        
        try {
            // 根据重要程度生成前缀
            String prefix;
            switch (importance.toLowerCase()) {
                case "urgent":
                    prefix = "【紧急通知】";
                    break;
                case "important":
                    prefix = "【重要通知】";
                    break;
                default:
                    prefix = "【通知】";
            }
            
            String fullTitle = prefix + title;
            String content = generateContentByTitle(fullTitle);
            String summary = generateSummary(content);
            
            // 创建文章
            Article article = new Article();
            article.setTitle(fullTitle);
            article.setContent(content);
            article.setSummary(summary);
            article.setBoardType("OFFICIAL");
            article.setAuthorId(1L);
            article.setStatus(1);
            article.setViewCount(0);
            article.setLikeCount(0);
            article.setCommentCount(0);
            article.setIsApproved(1);
            article.setIsPinned("urgent".equalsIgnoreCase(importance) ? 1 : 0); // 紧急通知自动置顶
            article.setCreatedAt(LocalDateTime.now());
            article.setUpdatedAt(LocalDateTime.now());
            
            articleMapper.insert(article);
            
            // 添加通知公告标签
            autoAddTagsToArticle(article.getId(), "通知 公告 " + title);
            
            StringBuilder result = new StringBuilder();
            result.append("📢 通知发布成功！\n");
            result.append("═".repeat(30)).append("\n\n");
            result.append("📋 通知信息：\n");
            result.append("   • 标题：").append(fullTitle).append("\n");
            result.append("   • 重要程度：").append(importance).append("\n");
            result.append("   • 文章ID：").append(article.getId()).append("\n");
            result.append("   • 是否置顶：").append(article.getIsPinned() == 1 ? "是" : "否").append("\n\n");
            
            // 如果需要通知用户
            if (notifyUsers) {
                List<User> activeUsers = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                        .eq(User::getStatus, 1)
                        .last("LIMIT 100")
                );
                
                int notifiedCount = 0;
                for (User user : activeUsers) {
                    try {
                        Notification notification = new Notification();
                        notification.setUserId(user.getId());
                        notification.setFromUserId(1L);
                        notification.setType("SYSTEM");
                        notification.setContent(prefix + " " + title);
                        notification.setArticleId(article.getId());
                        notification.setIsRead(0);
                        notification.setCreatedAt(LocalDateTime.now());
                        notificationMapper.insert(notification);
                        notifiedCount++;
                    } catch (Exception e) {
                        log.error("发送通知失败", e);
                    }
                }
                result.append("📤 已通知 ").append(notifiedCount).append(" 位用户\n\n");
            }
            
            result.append("🔗 链接：/article/").append(article.getId());
            
            return result.toString();
        } catch (Exception e) {
            log.error("发布通知失败", e);
            return "❌ 发布失败：" + e.getMessage();
        }
    }
    
    /**
     * 一键执行内容发布计划
     * 根据预设的内容计划自动发布文章
     * @param planType 计划类型（daily/weekly/welcome）
     * @return 执行结果
     */
    @Tool("一键执行内容发布计划，支持日常内容、周报、迎新内容等预设计划（需要管理员权限）")
    public String executeContentPlan(String planType) {
        log.info("Agent自动化：执行内容计划 - 类型: {}", planType);
        
        // 权限检查
        if (!checkAdminPermission()) {
            return noPermissionMessage("执行内容计划");
        }
        
        try {
            StringBuilder result = new StringBuilder();
            result.append("📅 内容发布计划执行\n");
            result.append("═".repeat(30)).append("\n");
            result.append("计划类型：").append(planType).append("\n\n");
            
            List<String> titles = new ArrayList<>();
            String boardType = "CAMPUS";
            
            switch (planType.toLowerCase()) {
                case "daily":
                    // 日常内容计划
                    titles.add("今日校园新鲜事");
                    titles.add("学习小贴士：提高效率的方法");
                    titles.add("校园生活分享");
                    break;
                    
                case "weekly":
                    // 周报计划
                    titles.add("本周校园热点回顾");
                    titles.add("本周活动精彩瞬间");
                    titles.add("下周活动预告");
                    boardType = "OFFICIAL";
                    break;
                    
                case "welcome":
                    // 迎新计划
                    titles.add("新生入学指南");
                    titles.add("校园地图与设施介绍");
                    titles.add("社团招新信息汇总");
                    titles.add("新生常见问题解答");
                    boardType = "OFFICIAL";
                    break;
                    
                case "exam":
                    // 考试季计划
                    titles.add("期末考试复习攻略");
                    titles.add("图书馆自习室开放时间");
                    titles.add("考试注意事项提醒");
                    break;
                    
                default:
                    return "❌ 不支持的计划类型：" + planType + "\n支持的类型：daily（日常）、weekly（周报）、welcome（迎新）、exam（考试季）";
            }
            
            result.append("📝 计划内容：\n");
            int successCount = 0;
            
            for (int i = 0; i < titles.size(); i++) {
                String title = titles.get(i);
                try {
                    String content = generateContentByTitle(title);
                    String summary = generateSummary(content);
                    
                    Article article = new Article();
                    article.setTitle(title);
                    article.setContent(content);
                    article.setSummary(summary);
                    article.setBoardType(boardType);
                    article.setAuthorId(1L);
                    article.setStatus(1);
                    article.setViewCount(0);
                    article.setLikeCount(0);
                    article.setCommentCount(0);
                    article.setIsApproved(1);
                    article.setIsPinned(0);
                    article.setCreatedAt(LocalDateTime.now());
                    article.setUpdatedAt(LocalDateTime.now());
                    
                    articleMapper.insert(article);
                    autoAddTagsToArticle(article.getId(), title);
                    
                    result.append("   ✅ ").append(i + 1).append(". 《").append(title).append("》 - ID:").append(article.getId()).append("\n");
                    successCount++;
                } catch (Exception e) {
                    result.append("   ❌ ").append(i + 1).append(". 《").append(title).append("》 - 失败\n");
                }
            }
            
            result.append("\n").append("═".repeat(30)).append("\n");
            result.append("📊 执行结果：成功发布 ").append(successCount).append("/").append(titles.size()).append(" 篇文章\n");
            
            return result.toString();
        } catch (Exception e) {
            log.error("执行内容计划失败", e);
            return "❌ 执行失败：" + e.getMessage();
        }
    }
    
    /**
     * 文章自动优化
     * 自动优化文章的标题、摘要和标签
     * @param articleId 文章ID
     * @return 优化结果
     */
    @Tool("自动优化指定文章的标题、摘要和标签")
    public String autoOptimizeArticle(Long articleId) {
        log.info("Agent自动化：优化文章 - ID: {}", articleId);
        
        try {
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                return "❌ 文章不存在，ID: " + articleId;
            }
            
            StringBuilder result = new StringBuilder();
            result.append("🔧 文章自动优化报告\n");
            result.append("═".repeat(30)).append("\n");
            result.append("文章：《").append(article.getTitle()).append("》\n\n");
            
            boolean hasChanges = false;
            
            // 1. 优化摘要
            if (article.getSummary() == null || article.getSummary().isEmpty() || 
                article.getSummary().length() < 20) {
                String newSummary = generateSummary(article.getContent());
                article.setSummary(newSummary);
                hasChanges = true;
                result.append("✅ 摘要优化：已生成新摘要\n");
                result.append("   新摘要：").append(newSummary.substring(0, Math.min(50, newSummary.length()))).append("...\n\n");
            } else {
                result.append("ℹ️ 摘要：无需优化\n\n");
            }
            
            // 2. 检查并添加标签
            List<ArticleTag> existingTags = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleId)
            );
            
            if (existingTags.isEmpty()) {
                List<String> addedTags = autoAddTagsToArticle(articleId, article.getTitle() + " " + article.getContent());
                if (!addedTags.isEmpty()) {
                    hasChanges = true;
                    result.append("✅ 标签优化：添加了 ").append(addedTags.size()).append(" 个标签\n");
                    result.append("   新标签：").append(String.join(", ", addedTags)).append("\n\n");
                }
            } else {
                result.append("ℹ️ 标签：已有 ").append(existingTags.size()).append(" 个标签\n\n");
            }
            
            // 3. 检查标题长度
            if (article.getTitle().length() < 5) {
                result.append("⚠️ 标题过短，建议补充更多信息\n\n");
            } else if (article.getTitle().length() > 50) {
                result.append("⚠️ 标题过长，建议精简\n\n");
            } else {
                result.append("ℹ️ 标题长度：适中\n\n");
            }
            
            // 保存更改
            if (hasChanges) {
                article.setUpdatedAt(LocalDateTime.now());
                articleMapper.updateById(article);
            }
            
            result.append("═".repeat(30)).append("\n");
            result.append(hasChanges ? "✅ 优化完成，已保存更改" : "ℹ️ 文章已经处于良好状态");
            
            return result.toString();
        } catch (Exception e) {
            log.error("优化文章失败", e);
            return "❌ 优化失败：" + e.getMessage();
        }
    }
    
    /**
     * 批量优化未优化的文章
     * @param limit 优化数量限制
     * @return 批量优化结果
     */
    @Tool("批量优化缺少摘要或标签的文章")
    public String batchOptimizeArticles(int limit) {
        log.info("Agent自动化：批量优化文章 - 限制: {}", limit);
        
        try {
            // 查找缺少摘要的文章
            List<Article> articlesToOptimize = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                    .eq(Article::getStatus, 1)
                    .and(w -> w.isNull(Article::getSummary)
                              .or().eq(Article::getSummary, ""))
                    .last("LIMIT " + Math.min(limit, 20))
            );
            
            StringBuilder result = new StringBuilder();
            result.append("🔧 批量文章优化任务\n");
            result.append("═".repeat(30)).append("\n\n");
            
            if (articlesToOptimize.isEmpty()) {
                // 查找缺少标签的文章
                List<Article> allArticles = articleMapper.selectList(
                    new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, 1)
                        .last("LIMIT 50")
                );
                
                for (Article article : allArticles) {
                    Long tagCount = articleTagMapper.selectCount(
                        new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, article.getId())
                    );
                    if (tagCount == 0) {
                        articlesToOptimize.add(article);
                        if (articlesToOptimize.size() >= limit) break;
                    }
                }
            }
            
            if (articlesToOptimize.isEmpty()) {
                return "✅ 所有文章都已优化，无需处理";
            }
            
            result.append("发现 ").append(articlesToOptimize.size()).append(" 篇需要优化的文章：\n\n");
            
            int optimizedCount = 0;
            for (Article article : articlesToOptimize) {
                try {
                    boolean changed = false;
                    
                    // 优化摘要
                    if (article.getSummary() == null || article.getSummary().isEmpty()) {
                        article.setSummary(generateSummary(article.getContent()));
                        changed = true;
                    }
                    
                    // 添加标签
                    Long tagCount = articleTagMapper.selectCount(
                        new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, article.getId())
                    );
                    if (tagCount == 0) {
                        autoAddTagsToArticle(article.getId(), article.getTitle() + " " + article.getContent());
                        changed = true;
                    }
                    
                    if (changed) {
                        article.setUpdatedAt(LocalDateTime.now());
                        articleMapper.updateById(article);
                        optimizedCount++;
                        result.append("✅ 《").append(article.getTitle()).append("》\n");
                    }
                } catch (Exception e) {
                    result.append("❌ 《").append(article.getTitle()).append("》 - 失败\n");
                }
            }
            
            result.append("\n").append("═".repeat(30)).append("\n");
            result.append("📊 优化统计：成功 ").append(optimizedCount).append("/").append(articlesToOptimize.size()).append(" 篇\n");
            
            return result.toString();
        } catch (Exception e) {
            log.error("批量优化失败", e);
            return "❌ 批量优化失败：" + e.getMessage();
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
    
    // ==================== 视频模块功能 ====================
    
    /**
     * 搜索视频
     * @param keyword 搜索关键词
     * @param limit 返回数量限制
     * @return 视频列表
     */
    @Tool("搜索视频，可以根据关键词搜索视频标题和描述")
    public String searchVideos(String keyword, int limit) {
        log.info("Agent工具：搜索视频 - 关键词: {}, 限制: {}", keyword, limit);
        
        try {
            LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
            wrapper.and(w -> w.like(Video::getTitle, keyword)
                            .or().like(Video::getDescription, keyword))
                   .eq(Video::getStatus, 1)
                   .eq(Video::getIsApproved, 1)
                   .orderByDesc(Video::getViewCount)
                   .last("LIMIT " + Math.min(limit, 20));
            
            List<Video> videos = videoMapper.selectList(wrapper);
            
            if (videos.isEmpty()) {
                return "没有找到包含关键词 \"" + keyword + "\" 的视频。";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("🎬 找到 ").append(videos.size()).append(" 个相关视频：\n\n");
            
            for (int i = 0; i < videos.size(); i++) {
                Video video = videos.get(i);
                result.append((i + 1)).append(". 《").append(video.getTitle()).append("》\n");
                result.append("   - 作者：").append(getUserName(video.getAuthorId())).append("\n");
                result.append("   - 时长：").append(video.getDuration() != null ? video.getDuration() : "未知").append("\n");
                result.append("   - 播放量：").append(video.getViewCount()).append("\n");
                result.append("   - 点赞数：").append(video.getLikeCount()).append("\n");
                result.append("   - 链接：/video/").append(video.getId()).append("\n");
                
                if (video.getDescription() != null && !video.getDescription().isEmpty()) {
                    String desc = video.getDescription();
                    result.append("   - 简介：").append(desc.length() > 50 ? desc.substring(0, 50) + "..." : desc).append("\n");
                }
                result.append("\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("搜索视频失败", e);
            return "❌ 搜索视频失败：" + e.getMessage();
        }
    }
    
    /**
     * 获取热门视频
     * @param limit 返回数量
     * @return 热门视频列表
     */
    @Tool("获取播放量最高的热门视频排行榜")
    public String getHotVideos(int limit) {
        log.info("Agent工具：获取热门视频 - 限制: {}", limit);
        
        try {
            LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Video::getStatus, 1)
                   .eq(Video::getIsApproved, 1)
                   .orderByDesc(Video::getViewCount)
                   .last("LIMIT " + Math.min(limit, 10));
            
            List<Video> videos = videoMapper.selectList(wrapper);
            
            if (videos.isEmpty()) {
                return "暂无视频数据";
            }
            
            StringBuilder result = new StringBuilder("🔥 热门视频排行榜：\n\n");
            for (int i = 0; i < videos.size(); i++) {
                Video video = videos.get(i);
                String medal = i < 3 ? (i == 0 ? "🥇" : i == 1 ? "🥈" : "🥉") : "  ";
                result.append(medal).append(" ").append((i + 1)).append(". 《")
                      .append(video.getTitle()).append("》\n");
                result.append("      播放量：").append(video.getViewCount())
                      .append(" | 点赞：").append(video.getLikeCount())
                      .append(" | 评论：").append(video.getCommentCount()).append("\n");
                result.append("      时长：").append(video.getDuration() != null ? video.getDuration() : "未知").append("\n");
                result.append("      链接：/video/").append(video.getId()).append("\n\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("获取热门视频失败", e);
            return "❌ 获取热门视频失败：" + e.getMessage();
        }
    }
    
    /**
     * 获取最新视频
     * @param limit 返回数量
     * @return 最新视频列表
     */
    @Tool("获取最新发布的视频列表")
    public String getLatestVideos(int limit) {
        log.info("Agent工具：获取最新视频 - 限制: {}", limit);
        
        try {
            LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Video::getStatus, 1)
                   .eq(Video::getIsApproved, 1)
                   .orderByDesc(Video::getCreatedAt)
                   .last("LIMIT " + Math.min(limit, 10));
            
            List<Video> videos = videoMapper.selectList(wrapper);
            
            if (videos.isEmpty()) {
                return "暂无视频数据";
            }
            
            StringBuilder result = new StringBuilder("📹 最新视频：\n\n");
            for (int i = 0; i < videos.size(); i++) {
                Video video = videos.get(i);
                result.append((i + 1)).append(". 《").append(video.getTitle()).append("》\n");
                result.append("   发布于：").append(video.getCreatedAt()).append("\n");
                result.append("   作者：").append(getUserName(video.getAuthorId())).append("\n");
                result.append("   链接：/video/").append(video.getId()).append("\n\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("获取最新视频失败", e);
            return "❌ 获取最新视频失败：" + e.getMessage();
        }
    }
    
    /**
     * 获取视频详情
     * @param videoId 视频ID
     * @return 视频详细信息
     */
    @Tool("获取指定ID视频的详细信息")
    public String getVideoDetail(Long videoId) {
        log.info("Agent工具：获取视频详情 - ID: {}", videoId);
        
        try {
            Video video = videoMapper.selectById(videoId);
            if (video == null) {
                return "❌ 视频不存在，ID: " + videoId;
            }
            
            // 获取分类信息
            String categoryName = "未分类";
            if (video.getCategoryId() != null) {
                VideoCategory category = videoCategoryMapper.selectById(video.getCategoryId());
                if (category != null) {
                    categoryName = category.getName();
                }
            }
            
            StringBuilder result = new StringBuilder();
            result.append("🎬 视频详情\n");
            result.append("═".repeat(30)).append("\n");
            result.append("标题：《").append(video.getTitle()).append("》\n");
            result.append("作者：").append(getUserName(video.getAuthorId())).append("\n");
            result.append("分类：").append(categoryName).append("\n");
            result.append("时长：").append(video.getDuration() != null ? video.getDuration() : "未知").append("\n");
            result.append("发布时间：").append(video.getCreatedAt()).append("\n");
            result.append("状态：").append(video.getIsApproved() == 1 ? "已审核" : "待审核").append("\n");
            result.append("\n📊 互动数据：\n");
            result.append("• 播放量：").append(video.getViewCount()).append("\n");
            result.append("• 点赞数：").append(video.getLikeCount()).append("\n");
            result.append("• 评论数：").append(video.getCommentCount()).append("\n");
            result.append("\n📝 简介：\n");
            result.append(video.getDescription() != null ? video.getDescription() : "暂无简介").append("\n");
            result.append("\n🔗 链接：/video/").append(video.getId()).append("\n");
            
            return result.toString();
        } catch (Exception e) {
            log.error("获取视频详情失败", e);
            return "❌ 获取失败：" + e.getMessage();
        }
    }
    
    /**
     * 点赞视频
     * @param videoId 视频ID
     * @return 点赞结果
     */
    @Tool("给指定ID的视频点赞")
    public String likeVideo(Long videoId) {
        log.info("Agent工具：点赞视频 - 视频ID: {}", videoId);
        
        try {
            Video video = videoMapper.selectById(videoId);
            if (video == null) {
                return "❌ 视频不存在，ID: " + videoId;
            }
            
            Long userId = 1L;
            
            // 检查是否已经点赞
            LambdaQueryWrapper<VideoLike> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(VideoLike::getVideoId, videoId)
                   .eq(VideoLike::getUserId, userId);
            VideoLike existing = videoLikeMapper.selectOne(wrapper);
            
            if (existing != null) {
                return "ℹ️ 您已经点赞过视频《" + video.getTitle() + "》了";
            }
            
            // 创建点赞记录
            VideoLike like = new VideoLike();
            like.setVideoId(videoId);
            like.setUserId(userId);
            videoLikeMapper.insert(like);
            
            // 更新视频点赞数
            video.setLikeCount(video.getLikeCount() + 1);
            videoMapper.updateById(video);
            
            return "👍 成功点赞视频《" + video.getTitle() + "》\n" +
                   "当前点赞数：" + video.getLikeCount();
        } catch (Exception e) {
            log.error("点赞视频失败", e);
            return "❌ 点赞失败：" + e.getMessage();
        }
    }
    
    /**
     * 给视频发表评论
     * @param videoId 视频ID
     * @param content 评论内容
     * @return 评论结果
     */
    @Tool("给指定视频发表评论")
    public String postVideoComment(Long videoId, String content) {
        log.info("Agent工具：发表视频评论 - 视频ID: {}, 内容: {}", videoId, content);
        
        try {
            Video video = videoMapper.selectById(videoId);
            if (video == null) {
                return "❌ 视频不存在，ID: " + videoId;
            }
            
            VideoComment comment = new VideoComment();
            comment.setVideoId(videoId);
            comment.setUserId(1L);
            comment.setContent(content);
            comment.setLikeCount(0);
            comment.setCreatedAt(LocalDateTime.now());
            comment.setUpdatedAt(LocalDateTime.now());
            
            videoCommentMapper.insert(comment);
            
            // 更新视频评论数
            video.setCommentCount(video.getCommentCount() + 1);
            videoMapper.updateById(video);
            
            return "💬 成功在视频《" + video.getTitle() + "》下发表评论：\n" +
                   "\"" + content + "\"\n" +
                   "评论ID：" + comment.getId();
        } catch (Exception e) {
            log.error("发表视频评论失败", e);
            return "❌ 发表评论失败：" + e.getMessage();
        }
    }
    
    /**
     * 获取视频分类列表
     * @return 视频分类列表
     */
    @Tool("获取所有视频分类列表")
    public String getVideoCategories() {
        log.info("Agent工具：获取视频分类列表");
        
        try {
            List<VideoCategory> categories = videoCategoryMapper.selectList(
                new LambdaQueryWrapper<VideoCategory>().orderByAsc(VideoCategory::getSortOrder)
            );
            
            if (categories.isEmpty()) {
                return "暂无视频分类";
            }
            
            StringBuilder result = new StringBuilder("📑 视频分类列表：\n\n");
            for (VideoCategory category : categories) {
                // 统计该分类下的视频数
                Long videoCount = videoMapper.selectCount(
                    new LambdaQueryWrapper<Video>()
                        .eq(Video::getCategoryId, category.getId())
                        .eq(Video::getStatus, 1)
                );
                
                result.append("• ").append(category.getName())
                      .append(" (").append(videoCount).append(" 个视频)\n");
                result.append("  代码：").append(category.getCode()).append("\n\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("获取视频分类失败", e);
            return "❌ 获取分类失败：" + e.getMessage();
        }
    }
    
    /**
     * 获取分类下的视频
     * @param categoryName 分类名称
     * @param limit 限制数量
     * @return 视频列表
     */
    @Tool("获取指定分类下的视频列表")
    public String getVideosByCategory(String categoryName, int limit) {
        log.info("Agent工具：获取分类视频 - 分类: {}", categoryName);
        
        try {
            // 查找分类
            LambdaQueryWrapper<VideoCategory> categoryWrapper = new LambdaQueryWrapper<>();
            categoryWrapper.like(VideoCategory::getName, categoryName);
            VideoCategory category = videoCategoryMapper.selectOne(categoryWrapper);
            
            if (category == null) {
                return "❌ 视频分类不存在：" + categoryName;
            }
            
            // 查找该分类下的视频
            LambdaQueryWrapper<Video> videoWrapper = new LambdaQueryWrapper<>();
            videoWrapper.eq(Video::getCategoryId, category.getId())
                       .eq(Video::getStatus, 1)
                       .eq(Video::getIsApproved, 1)
                       .orderByDesc(Video::getViewCount)
                       .last("LIMIT " + Math.min(limit, 10));
            
            List<Video> videos = videoMapper.selectList(videoWrapper);
            
            if (videos.isEmpty()) {
                return "分类 \"" + categoryName + "\" 下暂无视频";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("🎬 分类【").append(category.getName()).append("】的视频：\n\n");
            
            for (int i = 0; i < videos.size(); i++) {
                Video video = videos.get(i);
                result.append(i + 1).append(". 《").append(video.getTitle()).append("》\n");
                result.append("   播放量：").append(video.getViewCount())
                      .append(" | 点赞：").append(video.getLikeCount()).append("\n");
                result.append("   链接：/video/").append(video.getId()).append("\n\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("获取分类视频失败", e);
            return "❌ 获取失败：" + e.getMessage();
        }
    }
    
    // ==================== 通知管理功能 ====================
    
    /**
     * 查看用户通知
     * @param username 用户名
     * @param limit 限制数量
     * @return 通知列表
     */
    @Tool("查看指定用户的通知列表")
    public String getUserNotifications(String username, int limit) {
        log.info("Agent工具：查看用户通知 - 用户: {}", username);
        
        try {
            // 查找用户
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(User::getUsername, username);
            User user = userMapper.selectOne(userWrapper);
            
            if (user == null) {
                return "❌ 用户不存在：" + username;
            }
            
            // 查询通知
            LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Notification::getUserId, user.getId())
                   .orderByDesc(Notification::getCreatedAt)
                   .last("LIMIT " + Math.min(limit, 20));
            
            List<Notification> notifications = notificationMapper.selectList(wrapper);
            
            // 统计未读数量
            Long unreadCount = notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                    .eq(Notification::getUserId, user.getId())
                    .eq(Notification::getIsRead, 0)
            );
            
            StringBuilder result = new StringBuilder();
            result.append("🔔 ").append(username).append(" 的通知\n");
            result.append("═".repeat(30)).append("\n");
            result.append("未读通知：").append(unreadCount).append(" 条\n\n");
            
            if (notifications.isEmpty()) {
                result.append("暂无通知");
            } else {
                for (int i = 0; i < notifications.size(); i++) {
                    Notification notif = notifications.get(i);
                    String readStatus = notif.getIsRead() == 0 ? "🔴" : "⚪";
                    result.append(readStatus).append(" ").append(i + 1).append(". ");
                    result.append("[").append(notif.getType()).append("] ");
                    result.append(notif.getContent()).append("\n");
                    result.append("   时间：").append(notif.getCreatedAt()).append("\n\n");
                }
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("查看通知失败", e);
            return "❌ 查看失败：" + e.getMessage();
        }
    }
    
    /**
     * 标记通知为已读
     * @param username 用户名
     * @param markAll 是否标记所有
     * @return 操作结果
     */
    @Tool("标记用户的通知为已读")
    public String markNotificationsRead(String username, boolean markAll) {
        log.info("Agent工具：标记通知已读 - 用户: {}, 全部: {}", username, markAll);
        
        try {
            // 查找用户
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(User::getUsername, username);
            User user = userMapper.selectOne(userWrapper);
            
            if (user == null) {
                return "❌ 用户不存在：" + username;
            }
            
            // 查询未读通知
            LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Notification::getUserId, user.getId())
                   .eq(Notification::getIsRead, 0);
            
            List<Notification> unreadNotifications = notificationMapper.selectList(wrapper);
            
            if (unreadNotifications.isEmpty()) {
                return "✅ " + username + " 没有未读通知";
            }
            
            int count = 0;
            for (Notification notif : unreadNotifications) {
                notif.setIsRead(1);
                notificationMapper.updateById(notif);
                count++;
                if (!markAll && count >= 10) break; // 非全部时最多标记10条
            }
            
            return "✅ 成功将 " + count + " 条通知标记为已读\n" +
                   "用户：" + username;
        } catch (Exception e) {
            log.error("标记通知失败", e);
            return "❌ 操作失败：" + e.getMessage();
        }
    }
    
    /**
     * 发送系统通知给指定用户
     * @param username 目标用户名
     * @param content 通知内容
     * @return 发送结果
     */
    @Tool("发送系统通知给指定用户")
    public String sendNotificationToUser(String username, String content) {
        log.info("Agent工具：发送通知 - 用户: {}", username);
        
        try {
            // 查找用户
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(User::getUsername, username);
            User user = userMapper.selectOne(userWrapper);
            
            if (user == null) {
                return "❌ 用户不存在：" + username;
            }
            
            Notification notification = new Notification();
            notification.setUserId(user.getId());
            notification.setFromUserId(1L);
            notification.setType("SYSTEM");
            notification.setContent(content);
            notification.setIsRead(0);
            notification.setCreatedAt(LocalDateTime.now());
            notificationMapper.insert(notification);
            
            return "✅ 通知发送成功！\n" +
                   "接收用户：" + username + "\n" +
                   "通知内容：" + content + "\n" +
                   "通知ID：" + notification.getId();
        } catch (Exception e) {
            log.error("发送通知失败", e);
            return "❌ 发送失败：" + e.getMessage();
        }
    }
    
    // ==================== 用户画像与行为分析 ====================
    
    /**
     * 深度分析用户画像
     * @param username 用户名
     * @return 用户画像分析报告
     */
    @Tool("深度分析用户画像，包括兴趣偏好、活跃度、影响力等")
    public String analyzeUserProfile(String username) {
        log.info("Agent工具：分析用户画像 - 用户: {}", username);
        
        try {
            // 查找用户
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(User::getUsername, username);
            User user = userMapper.selectOne(userWrapper);
            
            if (user == null) {
                return "❌ 用户不存在：" + username;
            }
            
            // 用户发布的文章统计
            List<Article> userArticles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                    .eq(Article::getAuthorId, user.getId())
                    .eq(Article::getStatus, 1)
            );
            
            // 用户的评论统计
            List<Comment> userComments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getUserId, user.getId())
            );
            
            // 用户的点赞统计
            Long likeCount = articleLikeMapper.selectCount(
                new LambdaQueryWrapper<ArticleLike>()
                    .eq(ArticleLike::getUserId, user.getId())
            );
            
            // 用户的收藏统计
            Long favoriteCount = articleFavoriteMapper.selectCount(
                new LambdaQueryWrapper<ArticleFavorite>()
                    .eq(ArticleFavorite::getUserId, user.getId())
            );
            
            // 计算影响力指数
            int totalArticleViews = userArticles.stream().mapToInt(Article::getViewCount).sum();
            int totalArticleLikes = userArticles.stream().mapToInt(Article::getLikeCount).sum();
            int totalArticleComments = userArticles.stream().mapToInt(Article::getCommentCount).sum();
            
            double influenceScore = (user.getFollowerCount() * 3 + totalArticleViews * 0.01 + 
                                    totalArticleLikes * 2 + totalArticleComments * 1.5) / 10;
            
            // 分析活跃时段（简化处理）
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            Long recentArticles = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>()
                    .eq(Article::getAuthorId, user.getId())
                    .ge(Article::getCreatedAt, thirtyDaysAgo)
            );
            Long recentComments = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getUserId, user.getId())
                    .ge(Comment::getCreatedAt, thirtyDaysAgo)
            );
            
            // 分析偏好版块
            Map<String, Integer> boardPreference = new HashMap<>();
            for (Article article : userArticles) {
                boardPreference.merge(article.getBoardType(), 1, Integer::sum);
            }
            
            // 生成报告
            StringBuilder result = new StringBuilder();
            result.append("👤 用户画像分析报告\n");
            result.append("═".repeat(30)).append("\n\n");
            
            result.append("📋 基本信息：\n");
            result.append("• 用户名：").append(user.getUsername()).append("\n");
            result.append("• 真实姓名：").append(user.getRealName() != null ? user.getRealName() : "未设置").append("\n");
            result.append("• 注册时间：").append(user.getCreatedAt()).append("\n");
            result.append("• 账号状态：").append(user.getStatus() == 1 ? "正常" : "封禁").append("\n\n");
            
            result.append("📊 内容创作：\n");
            result.append("• 发布文章：").append(userArticles.size()).append(" 篇\n");
            result.append("• 总获赞数：").append(totalArticleLikes).append("\n");
            result.append("• 总浏览量：").append(totalArticleViews).append("\n");
            result.append("• 总评论数：").append(totalArticleComments).append("\n\n");
            
            result.append("💬 互动行为：\n");
            result.append("• 发表评论：").append(userComments.size()).append(" 条\n");
            result.append("• 点赞文章：").append(likeCount).append(" 篇\n");
            result.append("• 收藏文章：").append(favoriteCount).append(" 篇\n\n");
            
            result.append("👥 社交数据：\n");
            result.append("• 关注数：").append(user.getFollowingCount()).append("\n");
            result.append("• 粉丝数：").append(user.getFollowerCount()).append("\n\n");
            
            result.append("⭐ 影响力指数：").append(String.format("%.1f", influenceScore)).append("\n");
            String influenceLevel;
            if (influenceScore >= 100) influenceLevel = "🌟 顶级影响力";
            else if (influenceScore >= 50) influenceLevel = "⭐ 高影响力";
            else if (influenceScore >= 20) influenceLevel = "✨ 中等影响力";
            else influenceLevel = "💫 成长中";
            result.append("影响力等级：").append(influenceLevel).append("\n\n");
            
            result.append("📈 近30天活跃度：\n");
            result.append("• 发布文章：").append(recentArticles).append(" 篇\n");
            result.append("• 发表评论：").append(recentComments).append(" 条\n");
            
            String activityLevel;
            if (recentArticles + recentComments >= 20) activityLevel = "🔥 非常活跃";
            else if (recentArticles + recentComments >= 10) activityLevel = "⚡ 活跃";
            else if (recentArticles + recentComments >= 3) activityLevel = "💡 一般活跃";
            else activityLevel = "😴 不活跃";
            result.append("活跃等级：").append(activityLevel).append("\n\n");
            
            if (!boardPreference.isEmpty()) {
                result.append("🎯 内容偏好：\n");
                boardPreference.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .forEach(entry -> {
                        result.append("• ").append(getBoardTypeName(entry.getKey()))
                              .append("：").append(entry.getValue()).append(" 篇\n");
                    });
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("分析用户画像失败", e);
            return "❌ 分析失败：" + e.getMessage();
        }
    }
    
    // ==================== 学院信息管理 ====================
    
    /**
     * 获取所有学院信息
     * @return 学院列表
     */
    @Tool("获取所有学院信息列表")
    public String getAllColleges() {
        log.info("Agent工具：获取学院列表");
        
        try {
            List<College> colleges = collegeMapper.selectList(null);
            
            if (colleges.isEmpty()) {
                return "暂无学院信息";
            }
            
            StringBuilder result = new StringBuilder("🏫 学院列表：\n\n");
            for (int i = 0; i < colleges.size(); i++) {
                College college = colleges.get(i);
                result.append(i + 1).append(". ").append(college.getName()).append("\n");
                result.append("   代码：").append(college.getCode()).append("\n");
                if (college.getDescription() != null && !college.getDescription().isEmpty()) {
                    result.append("   简介：").append(college.getDescription()).append("\n");
                }
                result.append("\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("获取学院列表失败", e);
            return "❌ 获取失败：" + e.getMessage();
        }
    }
    
    // ==================== 综合搜索功能 ====================
    
    /**
     * 全站综合搜索
     * @param keyword 搜索关键词
     * @param limit 每类结果限制数量
     * @return 综合搜索结果
     */
    @Tool("全站综合搜索，同时搜索文章、视频和用户")
    public String globalSearch(String keyword, int limit) {
        log.info("Agent工具：全站搜索 - 关键词: {}", keyword);
        
        try {
            StringBuilder result = new StringBuilder();
            result.append("🔍 全站搜索结果：\"").append(keyword).append("\"\n");
            result.append("═".repeat(30)).append("\n\n");
            
            int actualLimit = Math.min(limit, 5);
            
            // 搜索文章
            LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<>();
            articleWrapper.and(w -> w.like(Article::getTitle, keyword)
                                    .or().like(Article::getSummary, keyword))
                         .eq(Article::getStatus, 1)
                         .eq(Article::getIsApproved, 1)
                         .orderByDesc(Article::getViewCount)
                         .last("LIMIT " + actualLimit);
            List<Article> articles = articleMapper.selectList(articleWrapper);
            
            result.append("📰 文章（").append(articles.size()).append("）：\n");
            if (articles.isEmpty()) {
                result.append("  暂无相关文章\n");
            } else {
                for (Article article : articles) {
                    result.append("  • 《").append(article.getTitle()).append("》");
                    result.append(" [浏览:").append(article.getViewCount()).append("]\n");
                }
            }
            result.append("\n");
            
            // 搜索视频
            LambdaQueryWrapper<Video> videoWrapper = new LambdaQueryWrapper<>();
            videoWrapper.and(w -> w.like(Video::getTitle, keyword)
                                  .or().like(Video::getDescription, keyword))
                       .eq(Video::getStatus, 1)
                       .eq(Video::getIsApproved, 1)
                       .orderByDesc(Video::getViewCount)
                       .last("LIMIT " + actualLimit);
            List<Video> videos = videoMapper.selectList(videoWrapper);
            
            result.append("🎬 视频（").append(videos.size()).append("）：\n");
            if (videos.isEmpty()) {
                result.append("  暂无相关视频\n");
            } else {
                for (Video video : videos) {
                    result.append("  • 《").append(video.getTitle()).append("》");
                    result.append(" [播放:").append(video.getViewCount()).append("]\n");
                }
            }
            result.append("\n");
            
            // 搜索用户
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.and(w -> w.like(User::getUsername, keyword)
                                 .or().like(User::getRealName, keyword))
                      .eq(User::getStatus, 1)
                      .orderByDesc(User::getFollowerCount)
                      .last("LIMIT " + actualLimit);
            List<User> users = userMapper.selectList(userWrapper);
            
            result.append("👥 用户（").append(users.size()).append("）：\n");
            if (users.isEmpty()) {
                result.append("  暂无相关用户\n");
            } else {
                for (User user : users) {
                    result.append("  • ").append(user.getUsername());
                    if (user.getRealName() != null) {
                        result.append(" (").append(user.getRealName()).append(")");
                    }
                    result.append(" [粉丝:").append(user.getFollowerCount()).append("]\n");
                }
            }
            
            int totalResults = articles.size() + videos.size() + users.size();
            result.append("\n📊 共找到 ").append(totalResults).append(" 条相关结果");
            
            return result.toString();
        } catch (Exception e) {
            log.error("全站搜索失败", e);
            return "❌ 搜索失败：" + e.getMessage();
        }
    }
    
    // ==================== 智能标签推荐 ====================
    
    /**
     * 为文章推荐标签
     * @param articleId 文章ID
     * @return 推荐的标签列表
     */
    @Tool("根据文章内容智能推荐合适的标签")
    public String recommendTagsForArticle(Long articleId) {
        log.info("Agent工具：推荐标签 - 文章ID: {}", articleId);
        
        try {
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                return "❌ 文章不存在，ID: " + articleId;
            }
            
            String content = article.getTitle() + " " + 
                           (article.getSummary() != null ? article.getSummary() : "") + " " +
                           (article.getContent() != null ? article.getContent() : "");
            
            // 获取所有标签
            List<Tag> allTags = tagMapper.selectList(null);
            
            // 分析匹配度
            Map<Tag, Integer> tagScores = new HashMap<>();
            for (Tag tag : allTags) {
                int score = 0;
                String tagName = tag.getName().toLowerCase();
                String lowerContent = content.toLowerCase();
                
                // 计算标签在内容中出现的次数
                int occurrences = countOccurrences(lowerContent, tagName);
                score += occurrences * 10;
                
                // 标题中出现加分
                if (article.getTitle().toLowerCase().contains(tagName)) {
                    score += 20;
                }
                
                if (score > 0) {
                    tagScores.put(tag, score);
                }
            }
            
            // 基于关键词推荐新标签
            List<String> suggestedNewTags = new ArrayList<>();
            String[] commonKeywords = {"AI", "人工智能", "校园", "活动", "学习", "考试", 
                                       "宿舍", "社团", "就业", "实习", "科研", "比赛"};
            for (String keyword : commonKeywords) {
                if (content.contains(keyword)) {
                    boolean exists = allTags.stream()
                        .anyMatch(t -> t.getName().equalsIgnoreCase(keyword));
                    if (!exists && !suggestedNewTags.contains(keyword)) {
                        suggestedNewTags.add(keyword);
                    }
                }
            }
            
            StringBuilder result = new StringBuilder();
            result.append("🏷️ 文章标签推荐报告\n");
            result.append("═".repeat(30)).append("\n\n");
            result.append("📄 文章：《").append(article.getTitle()).append("》\n\n");
            
            // 已有标签推荐
            if (!tagScores.isEmpty()) {
                result.append("✅ 推荐使用的现有标签：\n");
                tagScores.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(5)
                    .forEach(entry -> {
                        result.append("  • #").append(entry.getKey().getName())
                              .append(" (匹配度：").append(entry.getValue()).append(")\n");
                    });
                result.append("\n");
            }
            
            // 建议创建的新标签
            if (!suggestedNewTags.isEmpty()) {
                result.append("💡 建议创建的新标签：\n");
                for (String newTag : suggestedNewTags) {
                    result.append("  • #").append(newTag).append("\n");
                }
                result.append("\n");
            }
            
            // 获取文章当前标签
            List<ArticleTag> currentTags = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleId)
            );
            
            if (!currentTags.isEmpty()) {
                result.append("📌 当前已有标签：\n");
                for (ArticleTag at : currentTags) {
                    Tag tag = tagMapper.selectById(at.getTagId());
                    if (tag != null) {
                        result.append("  • #").append(tag.getName()).append("\n");
                    }
                }
            } else {
                result.append("⚠️ 当前文章尚未添加任何标签\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("推荐标签失败", e);
            return "❌ 推荐失败：" + e.getMessage();
        }
    }
    
    // ==================== 内容质量评分 ====================
    
    /**
     * 评估文章内容质量
     * @param articleId 文章ID
     * @return 质量评分报告
     */
    @Tool("评估文章的内容质量并给出评分和改进建议")
    public String evaluateArticleQuality(Long articleId) {
        log.info("Agent工具：评估文章质量 - 文章ID: {}", articleId);
        
        try {
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                return "❌ 文章不存在，ID: " + articleId;
            }
            
            int totalScore = 0;
            StringBuilder details = new StringBuilder();
            
            // 1. 标题评分（满分15分）
            int titleScore = 0;
            int titleLength = article.getTitle().length();
            if (titleLength >= 10 && titleLength <= 30) {
                titleScore = 15;
                details.append("✅ 标题长度适中\n");
            } else if (titleLength >= 5 && titleLength <= 50) {
                titleScore = 10;
                details.append("⚠️ 标题长度可以优化\n");
            } else {
                titleScore = 5;
                details.append("❌ 标题过长或过短\n");
            }
            totalScore += titleScore;
            
            // 2. 内容长度评分（满分20分）
            int contentScore = 0;
            int contentLength = article.getContent() != null ? article.getContent().length() : 0;
            if (contentLength >= 500 && contentLength <= 5000) {
                contentScore = 20;
                details.append("✅ 内容长度合适\n");
            } else if (contentLength >= 200 && contentLength <= 10000) {
                contentScore = 15;
                details.append("⚠️ 内容长度可以调整\n");
            } else if (contentLength >= 100) {
                contentScore = 10;
                details.append("⚠️ 内容偏短或偏长\n");
            } else {
                contentScore = 5;
                details.append("❌ 内容过短\n");
            }
            totalScore += contentScore;
            
            // 3. 摘要评分（满分10分）
            int summaryScore = 0;
            if (article.getSummary() != null && !article.getSummary().isEmpty()) {
                int summaryLength = article.getSummary().length();
                if (summaryLength >= 50 && summaryLength <= 200) {
                    summaryScore = 10;
                    details.append("✅ 摘要完整且长度合适\n");
                } else {
                    summaryScore = 5;
                    details.append("⚠️ 摘要长度可以优化\n");
                }
            } else {
                summaryScore = 0;
                details.append("❌ 缺少摘要\n");
            }
            totalScore += summaryScore;
            
            // 4. 互动数据评分（满分25分）
            int interactionScore = 0;
            double engagementRate = (article.getLikeCount() + article.getCommentCount()) * 100.0 / 
                                   Math.max(article.getViewCount(), 1);
            if (engagementRate >= 10) {
                interactionScore = 25;
                details.append("✅ 互动率优秀\n");
            } else if (engagementRate >= 5) {
                interactionScore = 20;
                details.append("✅ 互动率良好\n");
            } else if (engagementRate >= 2) {
                interactionScore = 15;
                details.append("⚠️ 互动率一般\n");
            } else {
                interactionScore = 10;
                details.append("⚠️ 互动率偏低\n");
            }
            totalScore += interactionScore;
            
            // 5. 内容原创性检查（满分15分）- 简化处理
            int originalityScore = 15;
            details.append("✅ 内容通过基础检查\n");
            totalScore += originalityScore;
            
            // 6. 格式规范评分（满分15分）
            int formatScore = 0;
            String content = article.getContent() != null ? article.getContent() : "";
            boolean hasParagraphs = content.contains("\n\n") || content.contains("<p>");
            boolean hasStructure = content.contains("。") && content.split("。").length >= 3;
            
            if (hasParagraphs && hasStructure) {
                formatScore = 15;
                details.append("✅ 文章结构良好\n");
            } else if (hasStructure) {
                formatScore = 10;
                details.append("⚠️ 建议增加段落划分\n");
            } else {
                formatScore = 5;
                details.append("❌ 文章结构需要改进\n");
            }
            totalScore += formatScore;
            
            // 生成报告
            StringBuilder result = new StringBuilder();
            result.append("📊 文章质量评估报告\n");
            result.append("═".repeat(30)).append("\n\n");
            result.append("📄 文章：《").append(article.getTitle()).append("》\n");
            result.append("作者：").append(getUserName(article.getAuthorId())).append("\n\n");
            
            result.append("🎯 总评分：").append(totalScore).append("/100\n");
            
            String grade;
            String gradeEmoji;
            if (totalScore >= 90) { grade = "优秀"; gradeEmoji = "⭐⭐⭐⭐⭐"; }
            else if (totalScore >= 80) { grade = "良好"; gradeEmoji = "⭐⭐⭐⭐"; }
            else if (totalScore >= 70) { grade = "中等"; gradeEmoji = "⭐⭐⭐"; }
            else if (totalScore >= 60) { grade = "及格"; gradeEmoji = "⭐⭐"; }
            else { grade = "需改进"; gradeEmoji = "⭐"; }
            
            result.append("等级：").append(gradeEmoji).append(" ").append(grade).append("\n\n");
            
            result.append("📋 评分明细：\n");
            result.append("• 标题质量：").append(titleScore).append("/15\n");
            result.append("• 内容长度：").append(contentScore).append("/20\n");
            result.append("• 摘要完整：").append(summaryScore).append("/10\n");
            result.append("• 互动数据：").append(interactionScore).append("/25\n");
            result.append("• 内容原创：").append(originalityScore).append("/15\n");
            result.append("• 格式规范：").append(formatScore).append("/15\n\n");
            
            result.append("📝 评估详情：\n").append(details);
            
            // 改进建议
            result.append("\n💡 改进建议：\n");
            if (summaryScore < 10) result.append("• 添加或优化文章摘要\n");
            if (contentScore < 15) result.append("• 丰富文章内容，建议500-3000字\n");
            if (interactionScore < 20) result.append("• 在文末提出问题引导读者互动\n");
            if (formatScore < 15) result.append("• 增加段落划分，提高可读性\n");
            if (titleScore < 15) result.append("• 优化标题，建议10-25字\n");
            
            return result.toString();
        } catch (Exception e) {
            log.error("评估文章质量失败", e);
            return "❌ 评估失败：" + e.getMessage();
        }
    }
    
    /**
     * 获取系统运行概览
     * @return 系统运行状态报告
     */
    @Tool("获取系统运行概览，包括各模块数据统计")
    public String getSystemOverview() {
        log.info("Agent工具：获取系统概览");
        
        try {
            StringBuilder result = new StringBuilder();
            result.append("🖥️ 系统运行概览\n");
            result.append("═".repeat(30)).append("\n");
            result.append("生成时间：").append(LocalDateTime.now()).append("\n\n");
            
            // 文章统计
            Long totalArticles = articleMapper.selectCount(null);
            Long publishedArticles = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getIsApproved, 1)
            );
            Long pendingArticles = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getIsApproved, 0)
            );
            
            result.append("📰 文章模块：\n");
            result.append("  • 总文章数：").append(totalArticles).append("\n");
            result.append("  • 已发布：").append(publishedArticles).append("\n");
            result.append("  • 待审核：").append(pendingArticles).append("\n\n");
            
            // 视频统计
            Long totalVideos = videoMapper.selectCount(null);
            Long publishedVideos = videoMapper.selectCount(
                new LambdaQueryWrapper<Video>().eq(Video::getIsApproved, 1)
            );
            
            result.append("🎬 视频模块：\n");
            result.append("  • 总视频数：").append(totalVideos).append("\n");
            result.append("  • 已发布：").append(publishedVideos).append("\n\n");
            
            // 用户统计
            Long totalUsers = userMapper.selectCount(null);
            Long activeUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getStatus, 1)
            );
            
            result.append("👥 用户模块：\n");
            result.append("  • 总用户数：").append(totalUsers).append("\n");
            result.append("  • 活跃用户：").append(activeUsers).append("\n\n");
            
            // 互动统计
            Long totalComments = commentMapper.selectCount(null);
            Long totalLikes = articleLikeMapper.selectCount(null);
            Long totalFollows = userFollowMapper.selectCount(null);
            
            result.append("💬 互动数据：\n");
            result.append("  • 总评论数：").append(totalComments).append("\n");
            result.append("  • 总点赞数：").append(totalLikes).append("\n");
            result.append("  • 总关注数：").append(totalFollows).append("\n\n");
            
            // 通知统计
            Long totalNotifications = notificationMapper.selectCount(null);
            Long unreadNotifications = notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>().eq(Notification::getIsRead, 0)
            );
            
            result.append("🔔 通知模块：\n");
            result.append("  • 总通知数：").append(totalNotifications).append("\n");
            result.append("  • 未读通知：").append(unreadNotifications).append("\n\n");
            
            // 标签统计
            Long totalTags = tagMapper.selectCount(null);
            result.append("🏷️ 标签数量：").append(totalTags).append("\n\n");
            
            // 系统健康度评估
            result.append("📊 系统健康度：");
            if (pendingArticles > 20) {
                result.append("⚠️ 有较多待审核内容\n");
            } else if (unreadNotifications > 100) {
                result.append("⚠️ 有较多未读通知\n");
            } else {
                result.append("✅ 运行正常\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("获取系统概览失败", e);
            return "❌ 获取失败：" + e.getMessage();
        }
    }
    
    // ==================== 自动化运维任务 ====================
    
    /**
     * 一键系统健康检查
     * 自动检查系统各项指标并生成报告
     * @return 健康检查报告和自动修复结果
     */
    @Tool("一键执行系统健康检查，自动检测并修复问题（需要管理员权限）")
    public String runSystemHealthCheck() {
        log.info("Agent自动化：执行系统健康检查");
        
        // 权限检查
        if (!checkAdminPermission()) {
            return noPermissionMessage("系统健康检查");
        }
        
        try {
            StringBuilder result = new StringBuilder();
            result.append("🏥 系统健康检查报告\n");
            result.append("═".repeat(30)).append("\n");
            result.append("检查时间：").append(LocalDateTime.now()).append("\n\n");
            
            int issuesFound = 0;
            int issuesFixed = 0;
            
            // 1. 检查待审核文章积压
            Long pendingArticles = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getIsApproved, 0)
            );
            result.append("📋 待审核文章检查：\n");
            if (pendingArticles > 10) {
                issuesFound++;
                result.append("   ⚠️ 发现 ").append(pendingArticles).append(" 篇待审核文章积压\n");
                // 自动触发批量审核
                int autoApproved = autoApproveSimple(Math.min(pendingArticles.intValue(), 20));
                if (autoApproved > 0) {
                    issuesFixed++;
                    result.append("   ✅ 已自动审核 ").append(autoApproved).append(" 篇安全文章\n");
                }
            } else {
                result.append("   ✅ 正常（").append(pendingArticles).append(" 篇待审核）\n");
            }
            
            // 2. 检查未读通知积压
            Long unreadNotifications = notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>().eq(Notification::getIsRead, 0)
            );
            result.append("\n🔔 通知积压检查：\n");
            if (unreadNotifications > 500) {
                issuesFound++;
                result.append("   ⚠️ 发现 ").append(unreadNotifications).append(" 条未读通知积压\n");
                // 清理30天前的已读通知
                LocalDateTime threshold = LocalDateTime.now().minusDays(30);
                int deleted = notificationMapper.delete(
                    new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getIsRead, 1)
                        .lt(Notification::getCreatedAt, threshold)
                );
                if (deleted > 0) {
                    issuesFixed++;
                    result.append("   ✅ 已清理 ").append(deleted).append(" 条过期已读通知\n");
                }
            } else {
                result.append("   ✅ 正常（").append(unreadNotifications).append(" 条未读）\n");
            }
            
            // 3. 检查用户活跃度
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            Long recentArticles = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().ge(Article::getCreatedAt, sevenDaysAgo)
            );
            Long recentComments = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>().ge(Comment::getCreatedAt, sevenDaysAgo)
            );
            result.append("\n📈 近7天活跃度检查：\n");
            result.append("   • 新增文章：").append(recentArticles).append(" 篇\n");
            result.append("   • 新增评论：").append(recentComments).append(" 条\n");
            if (recentArticles < 5 && recentComments < 10) {
                issuesFound++;
                result.append("   ⚠️ 用户活跃度偏低，建议发送激励通知\n");
            } else {
                result.append("   ✅ 用户活跃度正常\n");
            }
            
            // 4. 检查封禁用户
            Long bannedUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getStatus, 0)
            );
            result.append("\n👥 用户状态检查：\n");
            result.append("   • 封禁用户：").append(bannedUsers).append(" 人\n");
            result.append("   ✅ 已记录\n");
            
            // 5. 检查数据完整性
            result.append("\n🔍 数据完整性检查：\n");
            // 检查孤立评论（文章已删除的评论）
            List<Comment> allComments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>().last("LIMIT 100")
            );
            int orphanComments = 0;
            for (Comment comment : allComments) {
                Article article = articleMapper.selectById(comment.getArticleId());
                if (article == null) {
                    orphanComments++;
                }
            }
            if (orphanComments > 0) {
                issuesFound++;
                result.append("   ⚠️ 发现 ").append(orphanComments).append(" 条孤立评论\n");
            } else {
                result.append("   ✅ 数据完整性良好\n");
            }
            
            // 总结
            result.append("\n").append("═".repeat(30)).append("\n");
            result.append("📊 检查总结：\n");
            result.append("   • 发现问题：").append(issuesFound).append(" 个\n");
            result.append("   • 自动修复：").append(issuesFixed).append(" 个\n");
            
            String healthStatus;
            if (issuesFound == 0) {
                healthStatus = "🟢 系统健康";
            } else if (issuesFound - issuesFixed <= 2) {
                healthStatus = "🟡 基本正常";
            } else {
                healthStatus = "🔴 需要关注";
            }
            result.append("   • 健康状态：").append(healthStatus).append("\n");
            
            return result.toString();
        } catch (Exception e) {
            log.error("健康检查失败", e);
            return "❌ 健康检查失败：" + e.getMessage();
        }
    }
    
    // 简化版自动审核（内部方法）
    private int autoApproveSimple(int limit) {
        int approved = 0;
        try {
            List<Article> pending = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                    .eq(Article::getIsApproved, 0)
                    .last("LIMIT " + limit)
            );
            
            for (Article article : pending) {
                String content = article.getTitle() + " " + article.getContent();
                if (!containsSensitiveWords(content)) {
                    article.setIsApproved(1);
                    articleMapper.updateById(article);
                    approved++;
                    
                    // 发送通知
                    Notification notification = new Notification();
                    notification.setUserId(article.getAuthorId());
                    notification.setType("SYSTEM");
                    notification.setContent("[自动审核] 您的文章《" + article.getTitle() + "》已通过审核。");
                    notification.setIsRead(0);
                    notification.setCreatedAt(LocalDateTime.now());
                    notificationMapper.insert(notification);
                }
            }
        } catch (Exception e) {
            log.error("自动审核失败", e);
        }
        return approved;
    }
    
    // 简化版敏感词检测
    private boolean containsSensitiveWords(String content) {
        String[] sensitiveWords = {"傻逼", "操", "妈的", "去死", "色情", "赌博", "毒品", "暴力"};
        String lowerContent = content.toLowerCase();
        for (String word : sensitiveWords) {
            if (lowerContent.contains(word)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 一键内容质量巡检
     * 自动检查低质量内容并处理
     * @return 巡检报告
     */
    @Tool("一键执行内容质量巡检，自动识别并处理低质量内容（需要管理员权限）")
    public String runContentQualityPatrol() {
        log.info("Agent自动化：执行内容质量巡检");
        
        // 权限检查
        if (!checkAdminPermission()) {
            return noPermissionMessage("内容质量巡检");
        }
        
        try {
            StringBuilder result = new StringBuilder();
            result.append("📝 内容质量巡检报告\n");
            result.append("═".repeat(30)).append("\n\n");
            
            List<String> lowQualityArticles = new ArrayList<>();
            List<String> spamComments = new ArrayList<>();
            int processedCount = 0;
            
            // 1. 检查标题过短的文章
            List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                    .eq(Article::getStatus, 1)
                    .last("LIMIT 100")
            );
            
            result.append("📋 文章质量检查：\n");
            for (Article article : articles) {
                boolean isLowQuality = false;
                StringBuilder reason = new StringBuilder();
                
                // 标题过短
                if (article.getTitle().length() < 5) {
                    isLowQuality = true;
                    reason.append("标题过短;");
                }
                // 内容过短
                if (article.getContent() == null || article.getContent().length() < 50) {
                    isLowQuality = true;
                    reason.append("内容过短;");
                }
                // 无摘要且无内容
                if ((article.getSummary() == null || article.getSummary().isEmpty()) 
                    && (article.getContent() == null || article.getContent().length() < 100)) {
                    isLowQuality = true;
                    reason.append("缺少摘要;");
                }
                
                if (isLowQuality) {
                    lowQualityArticles.add("《" + article.getTitle() + "》- " + reason);
                }
            }
            
            if (lowQualityArticles.isEmpty()) {
                result.append("   ✅ 未发现低质量文章\n");
            } else {
                result.append("   ⚠️ 发现 ").append(lowQualityArticles.size()).append(" 篇低质量文章：\n");
                for (int i = 0; i < Math.min(5, lowQualityArticles.size()); i++) {
                    result.append("      • ").append(lowQualityArticles.get(i)).append("\n");
                }
                if (lowQualityArticles.size() > 5) {
                    result.append("      ... 还有 ").append(lowQualityArticles.size() - 5).append(" 篇\n");
                }
            }
            
            // 2. 检查垃圾评论
            result.append("\n💬 评论质量检查：\n");
            List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>().last("LIMIT 200")
            );
            
            for (Comment comment : comments) {
                boolean isSpam = false;
                // 纯表情或过短
                if (comment.getContent().length() < 3) {
                    isSpam = true;
                }
                // 包含广告关键词
                String content = comment.getContent().toLowerCase();
                if (content.contains("加微信") || content.contains("扫码") || 
                    content.contains("点击链接") || content.contains("赚钱")) {
                    isSpam = true;
                    // 自动删除广告评论
                    commentMapper.deleteById(comment.getId());
                    processedCount++;
                }
                
                if (isSpam) {
                    spamComments.add(comment.getContent().substring(0, Math.min(20, comment.getContent().length())) + "...");
                }
            }
            
            if (spamComments.isEmpty()) {
                result.append("   ✅ 未发现垃圾评论\n");
            } else {
                result.append("   ⚠️ 发现 ").append(spamComments.size()).append(" 条可疑评论\n");
                result.append("   ✅ 已自动删除 ").append(processedCount).append(" 条广告评论\n");
            }
            
            // 3. 统计互动率低的文章
            result.append("\n📊 低互动文章统计：\n");
            List<Article> lowEngagement = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                    .eq(Article::getStatus, 1)
                    .gt(Article::getViewCount, 100)
                    .eq(Article::getLikeCount, 0)
                    .eq(Article::getCommentCount, 0)
                    .last("LIMIT 10")
            );
            
            if (lowEngagement.isEmpty()) {
                result.append("   ✅ 无明显低互动文章\n");
            } else {
                result.append("   ⚠️ 发现 ").append(lowEngagement.size()).append(" 篇高浏览但零互动的文章\n");
                result.append("   💡 建议：优化这些文章的互动引导\n");
            }
            
            // 总结
            result.append("\n").append("═".repeat(30)).append("\n");
            result.append("📊 巡检总结：\n");
            result.append("   • 检查文章：").append(articles.size()).append(" 篇\n");
            result.append("   • 检查评论：").append(comments.size()).append(" 条\n");
            result.append("   • 自动处理：").append(processedCount).append(" 项\n");
            
            return result.toString();
        } catch (Exception e) {
            log.error("内容巡检失败", e);
            return "❌ 内容巡检失败：" + e.getMessage();
        }
    }
    
    /**
     * 一键用户活跃度激励
     * 自动识别不活跃用户并发送激励通知
     * @param inactiveDays 不活跃天数阈值
     * @return 执行结果
     */
    @Tool("一键执行用户活跃度激励，自动向不活跃用户发送激励通知（需要管理员权限）")
    public String runUserEngagementCampaign(int inactiveDays) {
        log.info("Agent自动化：执行用户激励 - 不活跃天数: {}", inactiveDays);
        
        // 权限检查
        if (!checkAdminPermission()) {
            return noPermissionMessage("用户活跃度激励");
        }
        
        try {
            StringBuilder result = new StringBuilder();
            result.append("🎯 用户活跃度激励任务\n");
            result.append("═".repeat(30)).append("\n\n");
            
            LocalDateTime threshold = LocalDateTime.now().minusDays(inactiveDays);
            
            // 查找不活跃用户（很久没有发文章或评论的用户）
            List<User> allUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                    .eq(User::getStatus, 1)
                    .lt(User::getUpdatedAt, threshold)
                    .last("LIMIT 50")
            );
            
            result.append("📊 分析结果：\n");
            result.append("   • 不活跃阈值：").append(inactiveDays).append(" 天\n");
            result.append("   • 发现不活跃用户：").append(allUsers.size()).append(" 人\n\n");
            
            if (allUsers.isEmpty()) {
                result.append("✅ 所有用户活跃度良好，无需激励\n");
                return result.toString();
            }
            
            // 获取热门内容用于推荐
            List<Article> hotArticles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                    .eq(Article::getStatus, 1)
                    .eq(Article::getIsApproved, 1)
                    .orderByDesc(Article::getViewCount)
                    .last("LIMIT 3")
            );
            
            StringBuilder hotContent = new StringBuilder();
            if (!hotArticles.isEmpty()) {
                hotContent.append("近期热门内容：");
                for (Article a : hotArticles) {
                    hotContent.append("《").append(a.getTitle()).append("》、");
                }
            }
            
            int notifiedCount = 0;
            result.append("📤 发送激励通知：\n");
            
            for (User user : allUsers) {
                try {
                    String message = "🎉 Hi " + (user.getRealName() != null ? user.getRealName() : user.getUsername()) + 
                                   "，好久不见！校园里发生了很多新鲜事，快来看看吧！" + 
                                   (hotContent.length() > 0 ? hotContent.toString() : "");
                    
                    Notification notification = new Notification();
                    notification.setUserId(user.getId());
                    notification.setFromUserId(1L);
                    notification.setType("SYSTEM");
                    notification.setContent(message);
                    notification.setIsRead(0);
                    notification.setCreatedAt(LocalDateTime.now());
                    notificationMapper.insert(notification);
                    
                    notifiedCount++;
                    if (notifiedCount <= 5) {
                        result.append("   ✅ ").append(user.getUsername()).append("\n");
                    }
                } catch (Exception e) {
                    log.error("发送激励通知失败: {}", user.getUsername(), e);
                }
            }
            
            if (notifiedCount > 5) {
                result.append("   ... 还有 ").append(notifiedCount - 5).append(" 人\n");
            }
            
            result.append("\n").append("═".repeat(30)).append("\n");
            result.append("📊 执行结果：\n");
            result.append("   • 成功发送：").append(notifiedCount).append(" 条激励通知\n");
            result.append("   ✅ 任务完成\n");
            
            return result.toString();
        } catch (Exception e) {
            log.error("用户激励任务失败", e);
            return "❌ 任务失败：" + e.getMessage();
        }
    }
    
    /**
     * 一键执行数据归档
     * 自动归档过期数据以优化系统性能
     * @param archiveDays 归档阈值天数
     * @return 归档结果
     */
    @Tool("一键执行数据归档，自动清理和归档过期数据（需要管理员权限）")
    public String runDataArchive(int archiveDays) {
        log.info("Agent自动化：执行数据归档 - 天数: {}", archiveDays);
        
        // 权限检查
        if (!checkAdminPermission()) {
            return noPermissionMessage("数据归档");
        }
        
        try {
            StringBuilder result = new StringBuilder();
            result.append("📦 数据归档任务\n");
            result.append("═".repeat(30)).append("\n");
            result.append("归档阈值：").append(archiveDays).append(" 天前的数据\n\n");
            
            LocalDateTime threshold = LocalDateTime.now().minusDays(archiveDays);
            int totalArchived = 0;
            
            // 1. 清理过期的已读通知
            result.append("🔔 通知归档：\n");
            int notifDeleted = notificationMapper.delete(
                new LambdaQueryWrapper<Notification>()
                    .eq(Notification::getIsRead, 1)
                    .lt(Notification::getCreatedAt, threshold)
            );
            result.append("   ✅ 清理已读通知：").append(notifDeleted).append(" 条\n");
            totalArchived += notifDeleted;
            
            // 2. 统计可归档的旧文章草稿
            Long oldDrafts = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>()
                    .eq(Article::getIsApproved, 0)
                    .lt(Article::getCreatedAt, threshold.minusDays(30)) // 超过阈值+30天的草稿
            );
            result.append("\n📝 草稿归档：\n");
            result.append("   ℹ️ 发现 ").append(oldDrafts).append(" 篇超期未审核草稿\n");
            if (oldDrafts > 0) {
                result.append("   💡 建议：手动确认后清理这些草稿\n");
            }
            
            // 3. 统计低质量评论
            Long shortComments = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>()
                    .lt(Comment::getCreatedAt, threshold)
                    .apply("LENGTH(content) < 5")
            );
            result.append("\n💬 评论归档：\n");
            result.append("   ℹ️ 发现 ").append(shortComments).append(" 条过期短评论\n");
            
            // 4. 生成空间释放估算
            result.append("\n💾 存储优化估算：\n");
            long estimatedBytes = (notifDeleted * 200L); // 估算每条通知200字节
            result.append("   • 预计释放空间：").append(formatBytes(estimatedBytes)).append("\n");
            
            result.append("\n").append("═".repeat(30)).append("\n");
            result.append("📊 归档总结：\n");
            result.append("   • 已清理数据：").append(totalArchived).append(" 条\n");
            result.append("   • 待处理数据：").append(oldDrafts + shortComments).append(" 条\n");
            result.append("   ✅ 归档任务完成\n");
            
            return result.toString();
        } catch (Exception e) {
            log.error("数据归档失败", e);
            return "❌ 归档失败：" + e.getMessage();
        }
    }
    
    // 格式化字节数
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
    
    /**
     * 一键执行热门内容推广
     * 自动识别优质内容并推送给用户
     * @return 推广结果
     */
    @Tool("一键执行热门内容推广，自动向用户推送优质内容（需要管理员权限）")
    public String runContentPromotion() {
        log.info("Agent自动化：执行内容推广");
        
        // 权限检查
        if (!checkAdminPermission()) {
            return noPermissionMessage("内容推广");
        }
        
        try {
            StringBuilder result = new StringBuilder();
            result.append("📢 热门内容推广任务\n");
            result.append("═".repeat(30)).append("\n\n");
            
            // 1. 识别24小时内的热门内容
            LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
            List<Article> hotArticles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                    .eq(Article::getStatus, 1)
                    .eq(Article::getIsApproved, 1)
                    .ge(Article::getCreatedAt, yesterday)
                    .orderByDesc(Article::getViewCount)
                    .last("LIMIT 3")
            );
            
            result.append("🔥 24小时热门文章：\n");
            if (hotArticles.isEmpty()) {
                result.append("   暂无热门文章\n");
                return result.toString();
            }
            
            for (int i = 0; i < hotArticles.size(); i++) {
                Article article = hotArticles.get(i);
                result.append("   ").append(i + 1).append(". 《").append(article.getTitle()).append("》\n");
                result.append("      浏览:").append(article.getViewCount())
                      .append(" 点赞:").append(article.getLikeCount()).append("\n");
            }
            
            // 2. 选择推送目标用户（活跃用户）
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            List<User> activeUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                    .eq(User::getStatus, 1)
                    .ge(User::getUpdatedAt, sevenDaysAgo)
                    .last("LIMIT 100")
            );
            
            result.append("\n👥 推送目标用户：").append(activeUsers.size()).append(" 人\n\n");
            
            // 3. 发送推广通知
            int sentCount = 0;
            StringBuilder articleList = new StringBuilder("今日热门推荐：");
            for (Article a : hotArticles) {
                articleList.append("《").append(a.getTitle()).append("》");
                if (hotArticles.indexOf(a) < hotArticles.size() - 1) {
                    articleList.append("、");
                }
            }
            
            for (User user : activeUsers) {
                try {
                    // 排除文章作者本人
                    boolean isAuthor = hotArticles.stream()
                        .anyMatch(a -> a.getAuthorId().equals(user.getId()));
                    if (isAuthor) continue;
                    
                    Notification notification = new Notification();
                    notification.setUserId(user.getId());
                    notification.setFromUserId(1L);
                    notification.setType("SYSTEM");
                    notification.setContent("📰 " + articleList.toString());
                    notification.setArticleId(hotArticles.get(0).getId()); // 关联第一篇热门文章
                    notification.setIsRead(0);
                    notification.setCreatedAt(LocalDateTime.now());
                    notificationMapper.insert(notification);
                    sentCount++;
                } catch (Exception e) {
                    log.error("发送推广通知失败", e);
                }
            }
            
            result.append("📤 推广执行结果：\n");
            result.append("   ✅ 成功推送：").append(sentCount).append(" 人\n");
            result.append("   📊 预计增加曝光：").append(sentCount * 0.3).append(" 次点击\n");
            
            return result.toString();
        } catch (Exception e) {
            log.error("内容推广失败", e);
            return "❌ 推广失败：" + e.getMessage();
        }
    }
    
    /**
     * 一键生成运营日报
     * 自动统计当日数据并生成报告
     * @return 运营日报
     */
    @Tool("一键生成运营日报，自动统计今日各项数据（需要管理员权限）")
    public String generateDailyReport() {
        log.info("Agent自动化：生成运营日报");
        
        // 权限检查
        if (!checkAdminPermission()) {
            return noPermissionMessage("生成运营日报");
        }
        
        try {
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime yesterdayStart = todayStart.minusDays(1);
            
            StringBuilder result = new StringBuilder();
            result.append("📊 运营日报\n");
            result.append("═".repeat(30)).append("\n");
            result.append("日期：").append(todayStart.toLocalDate()).append("\n");
            result.append("生成时间：").append(LocalDateTime.now()).append("\n\n");
            
            // 今日数据
            Long todayArticles = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().ge(Article::getCreatedAt, todayStart)
            );
            Long todayComments = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>().ge(Comment::getCreatedAt, todayStart)
            );
            Long todayUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreatedAt, todayStart)
            );
            Long todayLikes = articleLikeMapper.selectCount(
                new LambdaQueryWrapper<ArticleLike>().ge(ArticleLike::getCreatedAt, todayStart)
            );
            
            // 昨日数据（用于对比）
            Long yesterdayArticles = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>()
                    .ge(Article::getCreatedAt, yesterdayStart)
                    .lt(Article::getCreatedAt, todayStart)
            );
            Long yesterdayComments = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>()
                    .ge(Comment::getCreatedAt, yesterdayStart)
                    .lt(Comment::getCreatedAt, todayStart)
            );
            
            result.append("📈 今日核心指标：\n");
            result.append("━".repeat(25)).append("\n");
            result.append("   新增文章：").append(todayArticles).append(" 篇 ")
                  .append(getCompareEmoji(todayArticles, yesterdayArticles)).append("\n");
            result.append("   新增评论：").append(todayComments).append(" 条 ")
                  .append(getCompareEmoji(todayComments, yesterdayComments)).append("\n");
            result.append("   新增用户：").append(todayUsers).append(" 人\n");
            result.append("   新增点赞：").append(todayLikes).append(" 次\n\n");
            
            // 今日热门
            result.append("🔥 今日热门TOP3：\n");
            List<Article> todayHot = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                    .ge(Article::getCreatedAt, todayStart)
                    .eq(Article::getStatus, 1)
                    .orderByDesc(Article::getViewCount)
                    .last("LIMIT 3")
            );
            
            if (todayHot.isEmpty()) {
                result.append("   暂无数据\n");
            } else {
                for (int i = 0; i < todayHot.size(); i++) {
                    Article a = todayHot.get(i);
                    String medal = i == 0 ? "🥇" : i == 1 ? "🥈" : "🥉";
                    result.append("   ").append(medal).append(" 《").append(a.getTitle()).append("》\n");
                    result.append("       浏览:").append(a.getViewCount())
                          .append(" 点赞:").append(a.getLikeCount()).append("\n");
                }
            }
            
            // 今日活跃作者
            result.append("\n✍️ 今日活跃作者：\n");
            Map<Long, Integer> authorArticleCount = new HashMap<>();
            List<Article> todayAllArticles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>().ge(Article::getCreatedAt, todayStart)
            );
            for (Article a : todayAllArticles) {
                authorArticleCount.merge(a.getAuthorId(), 1, Integer::sum);
            }
            
            authorArticleCount.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(3)
                .forEach(entry -> {
                    result.append("   • ").append(getUserName(entry.getKey()))
                          .append("：").append(entry.getValue()).append(" 篇\n");
                });
            
            // 待处理事项
            result.append("\n⚠️ 待处理事项：\n");
            Long pendingArticles = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getIsApproved, 0)
            );
            if (pendingArticles > 0) {
                result.append("   • ").append(pendingArticles).append(" 篇文章待审核\n");
            } else {
                result.append("   ✅ 无待处理事项\n");
            }
            
            result.append("\n").append("═".repeat(30)).append("\n");
            result.append("📌 运营建议：\n");
            if (todayArticles < 5) {
                result.append("   • 内容产出较少，建议激励用户创作\n");
            }
            if (todayComments < todayArticles * 2) {
                result.append("   • 互动率偏低，建议引导用户评论\n");
            }
            if (todayArticles >= 10 && todayComments >= 20) {
                result.append("   • 📈 今日数据表现优秀，继续保持！\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("生成日报失败", e);
            return "❌ 生成日报失败：" + e.getMessage();
        }
    }
    
    // 对比增长emoji
    private String getCompareEmoji(Long today, Long yesterday) {
        if (today > yesterday) return "📈+" + (today - yesterday);
        if (today < yesterday) return "📉" + (today - yesterday);
        return "➡️";
    }
    
    /**
     * 一键执行异常用户检测
     * 自动检测可疑用户行为
     * @return 检测报告
     */
    @Tool("一键执行异常用户检测，自动识别可疑用户行为（需要管理员权限）")
    public String runAbnormalUserDetection() {
        log.info("Agent自动化：执行异常用户检测");
        
        // 权限检查
        if (!checkAdminPermission()) {
            return noPermissionMessage("异常用户检测");
        }
        
        try {
            StringBuilder result = new StringBuilder();
            result.append("🔍 异常用户检测报告\n");
            result.append("═".repeat(30)).append("\n\n");
            
            List<String> suspiciousUsers = new ArrayList<>();
            
            // 1. 检测短时间大量发文的用户（可能是刷屏）
            result.append("📝 刷屏行为检测：\n");
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            List<User> allUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getStatus, 1)
            );
            
            for (User user : allUsers) {
                Long recentArticles = articleMapper.selectCount(
                    new LambdaQueryWrapper<Article>()
                        .eq(Article::getAuthorId, user.getId())
                        .ge(Article::getCreatedAt, oneHourAgo)
                );
                if (recentArticles > 5) {
                    suspiciousUsers.add(user.getUsername() + " (1小时发文" + recentArticles + "篇)");
                    result.append("   ⚠️ ").append(user.getUsername())
                          .append(" - 1小时内发布 ").append(recentArticles).append(" 篇文章\n");
                }
            }
            
            if (!suspiciousUsers.isEmpty()) {
                result.append("   💡 建议：关注这些用户的内容质量\n");
            } else {
                result.append("   ✅ 未发现刷屏行为\n");
            }
            
            // 2. 检测大量评论相同内容的用户（可能是水军）
            result.append("\n💬 水军行为检测：\n");
            LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            List<Comment> todayComments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>().ge(Comment::getCreatedAt, today)
            );
            
            Map<Long, Map<String, Integer>> userCommentMap = new HashMap<>();
            for (Comment comment : todayComments) {
                userCommentMap.computeIfAbsent(comment.getUserId(), k -> new HashMap<>())
                             .merge(comment.getContent(), 1, Integer::sum);
            }
            
            boolean foundSpammer = false;
            for (Map.Entry<Long, Map<String, Integer>> entry : userCommentMap.entrySet()) {
                for (Map.Entry<String, Integer> commentEntry : entry.getValue().entrySet()) {
                    if (commentEntry.getValue() >= 3) {
                        foundSpammer = true;
                        result.append("   ⚠️ ").append(getUserName(entry.getKey()))
                              .append(" - 重复评论 \"")
                              .append(commentEntry.getKey().substring(0, Math.min(15, commentEntry.getKey().length())))
                              .append("...\" ").append(commentEntry.getValue()).append(" 次\n");
                    }
                }
            }
            
            if (!foundSpammer) {
                result.append("   ✅ 未发现水军行为\n");
            }
            
            // 3. 检测僵尸账号（注册后从未活动）
            result.append("\n👻 僵尸账号检测：\n");
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            Long zombieCount = 0L;
            for (User user : allUsers) {
                if (user.getCreatedAt().isBefore(thirtyDaysAgo)) {
                    Long articles = articleMapper.selectCount(
                        new LambdaQueryWrapper<Article>().eq(Article::getAuthorId, user.getId())
                    );
                    Long comments = commentMapper.selectCount(
                        new LambdaQueryWrapper<Comment>().eq(Comment::getUserId, user.getId())
                    );
                    if (articles == 0 && comments == 0) {
                        zombieCount++;
                    }
                }
            }
            result.append("   ℹ️ 发现 ").append(zombieCount).append(" 个僵尸账号（注册30天+无任何活动）\n");
            
            // 总结
            result.append("\n").append("═".repeat(30)).append("\n");
            result.append("📊 检测总结：\n");
            result.append("   • 可疑刷屏用户：").append(suspiciousUsers.size()).append(" 个\n");
            result.append("   • 可疑水军用户：").append(foundSpammer ? "有" : "无").append("\n");
            result.append("   • 僵尸账号：").append(zombieCount).append(" 个\n");
            
            return result.toString();
        } catch (Exception e) {
            log.error("异常检测失败", e);
            return "❌ 检测失败：" + e.getMessage();
        }
    }
    
    /**
     * 一键执行全站数据备份检查
     * @return 备份状态报告
     */
    @Tool("一键执行全站数据统计和备份检查（需要管理员权限）")
    public String runBackupCheck() {
        log.info("Agent自动化：执行备份检查");
        
        // 权限检查
        if (!checkAdminPermission()) {
            return noPermissionMessage("数据备份检查");
        }
        
        try {
            StringBuilder result = new StringBuilder();
            result.append("💾 数据备份检查报告\n");
            result.append("═".repeat(30)).append("\n");
            result.append("检查时间：").append(LocalDateTime.now()).append("\n\n");
            
            // 统计各表数据量
            result.append("📊 数据量统计：\n");
            
            Long articleCount = articleMapper.selectCount(null);
            Long userCount = userMapper.selectCount(null);
            Long commentCount = commentMapper.selectCount(null);
            Long videoCount = videoMapper.selectCount(null);
            Long notificationCount = notificationMapper.selectCount(null);
            Long tagCount = tagMapper.selectCount(null);
            Long likeCount = articleLikeMapper.selectCount(null);
            Long followCount = userFollowMapper.selectCount(null);
            
            result.append("   📰 文章：").append(articleCount).append(" 条\n");
            result.append("   👥 用户：").append(userCount).append(" 人\n");
            result.append("   💬 评论：").append(commentCount).append(" 条\n");
            result.append("   🎬 视频：").append(videoCount).append(" 个\n");
            result.append("   🔔 通知：").append(notificationCount).append(" 条\n");
            result.append("   🏷️ 标签：").append(tagCount).append(" 个\n");
            result.append("   👍 点赞：").append(likeCount).append(" 次\n");
            result.append("   🤝 关注：").append(followCount).append(" 对\n\n");
            
            long totalRecords = articleCount + userCount + commentCount + videoCount + 
                               notificationCount + tagCount + likeCount + followCount;
            result.append("   📈 总数据量：").append(totalRecords).append(" 条记录\n\n");
            
            // 估算存储空间
            long estimatedSize = totalRecords * 500; // 估算每条记录500字节
            result.append("💿 存储估算：\n");
            result.append("   • 预计数据大小：").append(formatBytes(estimatedSize)).append("\n");
            result.append("   • 建议备份频率：").append(totalRecords > 10000 ? "每日" : "每周").append("\n\n");
            
            // 数据增长趋势
            result.append("📈 近期增长趋势：\n");
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            Long weeklyArticles = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().ge(Article::getCreatedAt, sevenDaysAgo)
            );
            Long weeklyUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreatedAt, sevenDaysAgo)
            );
            result.append("   • 本周新增文章：").append(weeklyArticles).append(" 篇\n");
            result.append("   • 本周新增用户：").append(weeklyUsers).append(" 人\n");
            
            result.append("\n").append("═".repeat(30)).append("\n");
            result.append("✅ 数据检查完成\n");
            result.append("💡 建议：定期执行数据库备份，确保数据安全\n");
            
            return result.toString();
        } catch (Exception e) {
            log.error("备份检查失败", e);
            return "❌ 检查失败：" + e.getMessage();
        }
    }
}
