package com.campus.news.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.news.entity.Article;
import com.campus.news.entity.ArticleLike;
import com.campus.news.mapper.ArticleLikeMapper;
import com.campus.news.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleLikeService extends ServiceImpl<ArticleLikeMapper, ArticleLike> {
    
    private final ArticleLikeMapper articleLikeMapper;
    private final ArticleMapper articleMapper;
    @Lazy
    private final RealtimeNotificationService realtimeNotificationService;
    
    @Transactional
    public boolean toggleLike(Long articleId, Long userId) {
        QueryWrapper<ArticleLike> wrapper = new QueryWrapper<>();
        wrapper.eq("article_id", articleId).eq("user_id", userId);
        
        ArticleLike existing = articleLikeMapper.selectOne(wrapper);
        
        if (existing != null) {
            // 取消点赞
            articleLikeMapper.deleteById(existing.getId());
            updateArticleLikeCount(articleId, -1);
            return false;
        } else {
            // 点赞
            ArticleLike like = new ArticleLike();
            like.setArticleId(articleId);
            like.setUserId(userId);
            articleLikeMapper.insert(like);
            updateArticleLikeCount(articleId, 1);
            
            // 🔔 发送实时通知
            Article article = articleMapper.selectById(articleId);
            if (article != null) {
                realtimeNotificationService.sendLikeNotification(
                    article.getAuthorId(), userId, articleId, article.getTitle());
            }
            
            return true;
        }
    }
    
    public boolean isLiked(Long articleId, Long userId) {
        QueryWrapper<ArticleLike> wrapper = new QueryWrapper<>();
        wrapper.eq("article_id", articleId).eq("user_id", userId);
        return articleLikeMapper.selectCount(wrapper) > 0;
    }
    
    private void updateArticleLikeCount(Long articleId, int delta) {
        Article article = articleMapper.selectById(articleId);
        if (article != null) {
            article.setLikeCount(article.getLikeCount() + delta);
            articleMapper.updateById(article);
        }
    }
}
