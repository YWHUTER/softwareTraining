package com.campus.news.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.news.entity.ArticleTag;
import com.campus.news.entity.Tag;
import com.campus.news.mapper.ArticleTagMapper;
import com.campus.news.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 标签服务
 */
@Service
@RequiredArgsConstructor
public class TagService extends ServiceImpl<TagMapper, Tag> {
    
    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;
    
    /**
     * 获取热门标签（用于标签云）
     */
    public List<Tag> getHotTags(int limit) {
        return tagMapper.getHotTags(limit);
    }
    
    /**
     * 获取所有标签
     */
    public List<Tag> getAllTags() {
        QueryWrapper<Tag> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("use_count");
        return tagMapper.selectList(wrapper);
    }
    
    /**
     * 获取文章的标签
     */
    public List<Tag> getArticleTags(Long articleId) {
        return articleTagMapper.getTagsByArticleId(articleId);
    }
    
    /**
     * 为文章添加标签
     */
    @Transactional
    public void addTagsToArticle(Long articleId, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return;
        
        for (String tagName : tagNames) {
            tagName = tagName.trim();
            if (tagName.isEmpty()) continue;
            
            // 查找或创建标签
            Tag tag = tagMapper.findByName(tagName);
            if (tag == null) {
                tag = new Tag();
                tag.setName(tagName);
                tag.setUseCount(1);
                tagMapper.insert(tag);
            } else {
                // 增加使用次数
                tag.setUseCount(tag.getUseCount() + 1);
                tagMapper.updateById(tag);
            }
            
            // 创建文章-标签关联
            QueryWrapper<ArticleTag> wrapper = new QueryWrapper<>();
            wrapper.eq("article_id", articleId).eq("tag_id", tag.getId());
            if (articleTagMapper.selectCount(wrapper) == 0) {
                ArticleTag articleTag = new ArticleTag();
                articleTag.setArticleId(articleId);
                articleTag.setTagId(tag.getId());
                articleTagMapper.insert(articleTag);
            }
        }
    }
    
    /**
     * 移除文章的所有标签
     */
    @Transactional
    public void removeArticleTags(Long articleId) {
        // 先获取文章的标签，减少使用次数
        List<Tag> tags = getArticleTags(articleId);
        for (Tag tag : tags) {
            tag.setUseCount(Math.max(0, tag.getUseCount() - 1));
            tagMapper.updateById(tag);
        }
        
        // 删除关联
        QueryWrapper<ArticleTag> wrapper = new QueryWrapper<>();
        wrapper.eq("article_id", articleId);
        articleTagMapper.delete(wrapper);
    }
    
    /**
     * 更新文章标签
     */
    @Transactional
    public void updateArticleTags(Long articleId, List<String> tagNames) {
        removeArticleTags(articleId);
        addTagsToArticle(articleId, tagNames);
    }
}
