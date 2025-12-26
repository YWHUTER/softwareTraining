package com.campus.news.service;

import com.campus.news.ai.AIService;
import com.campus.news.dto.AiChatRequest;
import com.campus.news.dto.AiChatResponse;
import com.campus.news.entity.Article;
import com.campus.news.entity.Comment;
import com.campus.news.entity.User;
import com.campus.news.mapper.CommentMapper;
import com.campus.news.mapper.UserMapper;
import com.campus.news.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * ============================================================================
 * AI 评论机器人服务 (AiCommentRobotService)
 * ============================================================================
 * 
 * 【功能说明】
 * 类似微博的"罗伯特"功能，AI 会自动对新发布的文章生成一条相关评论
 * 
 * 【工作原理】
 * 1. 文章发布后，异步触发 AI 评论生成
 * 2. AI 分析文章标题和内容，生成一条有趣、相关的评论
 * 3. 以 AI 机器人用户的身份发布评论
 * 
 * 【答辩要点】
 * Q: AI 评论是怎么生成的？
 * A: 通过 Prompt Engineering，让大模型扮演一个友好的评论者，
 *    根据文章内容生成自然、有趣的评论
 * 
 * Q: 为什么用异步？
 * A: 不阻塞文章发布流程，提升用户体验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCommentRobotService {

    private final AIService aiService;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final ArticleMapper articleMapper;  // 直接使用 Mapper 避免循环依赖
    
    // AI 机器人用户名（需要在数据库中预先创建）
    private static final String AI_ROBOT_USERNAME = "WHUTGPT";
    
    // 评论风格模板
    private static final String[] COMMENT_STYLES = {
        "友好鼓励型",
        "幽默风趣型", 
        "专业点评型",
        "好奇提问型",
        "共鸣感慨型"
    };

    /**
     * 为文章生成 AI 评论（异步执行）
     * 
     * @param articleId 文章ID
     */
    @Async
    public void generateCommentForArticle(Long articleId) {
        try {
            // 延迟几秒，模拟真人阅读时间
            Thread.sleep(3000 + new Random().nextInt(5000));
            
            // 获取文章信息（直接使用 Mapper 避免循环依赖）
            Article article = articleMapper.selectById(articleId);
            if (article == null || article.getStatus() != 1) {
                log.warn("文章不存在或已删除，跳过AI评论: {}", articleId);
                return;
            }
            
            // 获取 AI 机器人用户
            User robotUser = getRobotUser();
            if (robotUser == null) {
                log.error("AI机器人用户不存在，请先创建用户: {}", AI_ROBOT_USERNAME);
                return;
            }
            
            // 生成评论内容
            String commentContent = generateComment(article);
            if (commentContent == null || commentContent.trim().isEmpty()) {
                log.warn("AI评论生成失败，跳过");
                return;
            }
            
            // 创建评论
            Comment comment = new Comment();
            comment.setArticleId(articleId);
            comment.setUserId(robotUser.getId());
            comment.setContent(commentContent);
            comment.setLikeCount(0);
            comment.setStatus(1);
            
            commentMapper.insert(comment);
            
            // 更新文章评论数（直接使用 Mapper）
            article.setCommentCount(article.getCommentCount() + 1);
            articleMapper.updateById(article);
            
            log.info("AI评论生成成功 - 文章: {}, 评论: {}", article.getTitle(), commentContent);
            
        } catch (Exception e) {
            log.error("AI评论生成异常", e);
        }
    }

    /**
     * 生成评论内容
     */
    private String generateComment(Article article) {
        try {
            // 随机选择评论风格
            String style = COMMENT_STYLES[new Random().nextInt(COMMENT_STYLES.length)];
            
            // 截取文章内容（避免太长）
            String content = article.getContent();
            if (content != null && content.length() > 500) {
                content = content.substring(0, 500) + "...";
            }
            
            // 构建 Prompt
            String prompt = buildCommentPrompt(article.getTitle(), content, style);
            
            // 调用 AI 服务
            AiChatRequest request = new AiChatRequest();
            request.setQuestion(prompt);
            AiChatResponse response = aiService.chat(request);
            
            String comment = response.getAnswer();
            
            // 清理评论内容（去除引号、多余空白等）
            comment = cleanComment(comment);
            
            return comment;
            
        } catch (Exception e) {
            log.error("调用AI生成评论失败", e);
            return getDefaultComment(article.getTitle());
        }
    }

    /**
     * 构建评论生成的 Prompt
     */
    private String buildCommentPrompt(String title, String content, String style) {
        return String.format("""
            你是一个校园新闻系统的AI评论助手"WHUTGPT"，请为以下文章写一条评论。
            
            【评论要求】
            1. 风格：%s
            2. 长度：15-50字，简洁有力
            3. 内容：与文章主题相关，自然真实
            4. 语气：友好、积极、有校园气息
            5. 可以适当使用1-2个emoji表情
            6. 不要使用"这篇文章"、"作者"等词汇，像真实用户一样评论
            7. 直接输出评论内容，不要加引号或其他格式
            
            【文章标题】
            %s
            
            【文章内容】
            %s
            
            请直接输出评论内容：
            """, style, title, content != null ? content : "（无内容）");
    }

    /**
     * 清理评论内容
     */
    private String cleanComment(String comment) {
        if (comment == null) return null;
        
        // 去除首尾引号
        comment = comment.trim();
        if (comment.startsWith("\"") && comment.endsWith("\"")) {
            comment = comment.substring(1, comment.length() - 1);
        }
        if (comment.startsWith("「") && comment.endsWith("」")) {
            comment = comment.substring(1, comment.length() - 1);
        }
        
        // 去除"评论："等前缀
        comment = comment.replaceAll("^(评论[：:]|回复[：:])", "");
        
        // 限制长度
        if (comment.length() > 100) {
            comment = comment.substring(0, 100);
        }
        
        return comment.trim();
    }

    /**
     * 获取默认评论（AI 调用失败时使用）
     */
    private String getDefaultComment(String title) {
        String[] defaults = {
            "写得不错，期待更多精彩内容！👍",
            "感谢分享，学到了很多～",
            "这个话题很有意思，关注了！",
            "支持一下，继续加油！💪",
            "内容很充实，收藏了～"
        };
        return defaults[new Random().nextInt(defaults.length)];
    }

    /**
     * 获取 AI 机器人用户
     */
    private User getRobotUser() {
        return userMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("username", AI_ROBOT_USERNAME)
        );
    }
    
    /**
     * 检查 AI 机器人用户是否存在
     */
    public boolean isRobotUserExists() {
        return getRobotUser() != null;
    }
    
    /**
     * 获取机器人用户名
     */
    public String getRobotUsername() {
        return AI_ROBOT_USERNAME;
    }
}
