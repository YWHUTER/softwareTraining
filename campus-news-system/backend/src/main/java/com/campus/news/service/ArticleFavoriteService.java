package com.campus.news.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.news.entity.ArticleFavorite;
import com.campus.news.mapper.ArticleFavoriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleFavoriteService extends ServiceImpl<ArticleFavoriteMapper, ArticleFavorite> {
    
    private final ArticleFavoriteMapper articleFavoriteMapper;
    
    @Transactional
    public boolean toggleFavorite(Long articleId, Long userId) {
        QueryWrapper<ArticleFavorite> wrapper = new QueryWrapper<>();
        wrapper.eq("article_id", articleId).eq("user_id", userId);
        
        ArticleFavorite existing = articleFavoriteMapper.selectOne(wrapper);
        
        if (existing != null) {
            articleFavoriteMapper.deleteById(existing.getId());
            return false;
        } else {
            ArticleFavorite favorite = new ArticleFavorite();
            favorite.setArticleId(articleId);
            favorite.setUserId(userId);
            articleFavoriteMapper.insert(favorite);
            return true;
        }
    }
    
    public boolean isFavorited(Long articleId, Long userId) {
        QueryWrapper<ArticleFavorite> wrapper = new QueryWrapper<>();
        wrapper.eq("article_id", articleId).eq("user_id", userId);
        return articleFavoriteMapper.selectCount(wrapper) > 0;
    }
}
