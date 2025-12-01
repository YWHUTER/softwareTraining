package com.campus.news.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.news.common.PageResult;
import com.campus.news.dto.ArticleCreateRequest;
import com.campus.news.dto.ArticleQueryRequest;
import com.campus.news.entity.Article;
import com.campus.news.entity.ArticleFavorite;
import com.campus.news.entity.ArticleLike;
import com.campus.news.entity.User;
import com.campus.news.exception.BusinessException;
import com.campus.news.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService extends ServiceImpl<ArticleMapper, Article> {
    
    private final ArticleMapper articleMapper;
    private final UserService userService;
    private final CollegeService collegeService;
    private final ArticleLikeService articleLikeService;
    private final ArticleFavoriteService articleFavoriteService;
    
    @Transactional
    public Article createArticle(ArticleCreateRequest request, Long userId) {
        User user = userService.getUserInfo(userId);
        
        // 权限验证
        validateArticlePermission(request.getBoardType(), user);
        
        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setSummary(request.getSummary());
        article.setCoverImage(request.getCoverImage());
        article.setAuthorId(userId);
        article.setBoardType(request.getBoardType());
        article.setCollegeId(request.getCollegeId());
        article.setIsPinned(request.getIsPinned());
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        article.setIsApproved(0); // 默认待审核，需要管理员审核后才能展示
        article.setStatus(1);
        
        articleMapper.insert(article);
        return article;
    }
    
    private void validateArticlePermission(String boardType, User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(r -> r.getRoleName())
                .toList();
        
        if ("OFFICIAL".equals(boardType)) {
            if (!roleNames.contains("ADMIN") && !roleNames.contains("TEACHER")) {
                throw new BusinessException("只有管理员和教师可以发布官方新闻");
            }
        }
        
        if ("COLLEGE".equals(boardType)) {
            if (user.getCollegeId() == null) {
                throw new BusinessException("您未绑定学院，无法发布学院新闻");
            }
        }
    }
    
    public PageResult<Article> getArticleList(ArticleQueryRequest request, Long currentUserId) {
        Page<Article> page = new Page<>(request.getCurrent(), request.getSize());
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        
        if (request.getBoardType() != null) {
            wrapper.eq("board_type", request.getBoardType());
        }
        if (request.getCollegeId() != null) {
            wrapper.eq("college_id", request.getCollegeId());
        }
        if (request.getAuthorId() != null) {
            wrapper.eq("author_id", request.getAuthorId());
        }
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            wrapper.like("title", request.getKeyword())
                    .or().like("content", request.getKeyword());
        }
        if (request.getIsApproved() != null) {
            wrapper.eq("is_approved", request.getIsApproved());
        }
        if (request.getIsPinned() != null) {
            wrapper.eq("is_pinned", request.getIsPinned());
        }
        
        // 首先按置顶排序
        wrapper.orderByDesc("is_pinned");
        
        // 根据排序参数设置排序规则
        String sortBy = request.getSortBy();
        boolean isAsc = "asc".equalsIgnoreCase(request.getSortOrder());
        
        if ("views".equals(sortBy)) {
            // 按浏览量(热度)排序
            if (isAsc) {
                wrapper.orderByAsc("view_count");
            } else {
                wrapper.orderByDesc("view_count");
            }
        } else {
            // 默认按日期排序
            if (isAsc) {
                wrapper.orderByAsc("created_at");
            } else {
                wrapper.orderByDesc("created_at");
            }
        }
        
        Page<Article> resultPage = articleMapper.selectPage(page, wrapper);
        
        // 填充关联数据
        resultPage.getRecords().forEach(article -> {
            enrichArticle(article, currentUserId);
        });
        
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords(),
                resultPage.getCurrent(), resultPage.getSize());
    }
    
    public Article getArticleDetail(Long id, Long currentUserId) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        
        // 增加浏览量
        incrementViewCount(id);
        
        // 重新获取更新后的浏览量
        Article updated = articleMapper.selectById(id);
        if (updated != null) {
            article.setViewCount(updated.getViewCount());
        }
        
        enrichArticle(article, currentUserId);
        return article;
    }
    
    private void enrichArticle(Article article, Long currentUserId) {
        article.setAuthor(userService.getUserInfo(article.getAuthorId()));
        if (article.getCollegeId() != null) {
            article.setCollege(collegeService.getById(article.getCollegeId()));
        }
        
        if (currentUserId != null) {
            article.setIsLiked(articleLikeService.isLiked(article.getId(), currentUserId));
            article.setIsFavorited(articleFavoriteService.isFavorited(article.getId(), currentUserId));
        }
    }
    
    private void incrementViewCount(Long articleId) {
        try {
            // 直接更新数据库浏览量 +1
            articleMapper.update(null, 
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Article>()
                    .eq("id", articleId)
                    .setSql("view_count = view_count + 1")
            );
        } catch (Exception ignored) {
            // 更新失败不影响文章详情正常返回
        }
    }
    
    @Transactional
    public boolean updateArticle(Long id, ArticleCreateRequest request, Long userId) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        
        User user = userService.getUserInfo(userId);
        List<String> roleNames = user.getRoles().stream().map(r -> r.getRoleName()).toList();
        
        if (!article.getAuthorId().equals(userId) && !roleNames.contains("ADMIN")) {
            throw new BusinessException("无权限修改该文章");
        }
        
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setSummary(request.getSummary());
        article.setCoverImage(request.getCoverImage());
        
        return articleMapper.updateById(article) > 0;
    }
    
    @Transactional
    public boolean deleteArticle(Long id, Long userId) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        
        User user = userService.getUserInfo(userId);
        List<String> roleNames = user.getRoles().stream().map(r -> r.getRoleName()).toList();
        
        if (!article.getAuthorId().equals(userId) && !roleNames.contains("ADMIN")) {
            throw new BusinessException("无权限删除该文章");
        }
        
        return articleMapper.deleteById(id) > 0;
    }
    
    @Transactional
    public boolean togglePinned(Long id, Integer isPinned) {
        Article article = new Article();
        article.setId(id);
        article.setIsPinned(isPinned);
        return articleMapper.updateById(article) > 0;
    }
    
    @Transactional
    public boolean approveArticle(Long id, Integer isApproved) {
        Article article = new Article();
        article.setId(id);
        article.setIsApproved(isApproved);
        return articleMapper.updateById(article) > 0;
    }
}
