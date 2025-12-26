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
import com.campus.news.entity.Comment;
import com.campus.news.entity.User;
import com.campus.news.mapper.CommentMapper;
import com.campus.news.exception.BusinessException;
import com.campus.news.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService extends ServiceImpl<ArticleMapper, Article> {
    
    private final ArticleMapper articleMapper;
    private final UserService userService;
    private final CollegeService collegeService;
    private final ArticleLikeService articleLikeService;
    private final ArticleFavoriteService articleFavoriteService;
    private final CommentMapper commentMapper;
    private final TagService tagService;
    
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
        article.setCategory(request.getCategory());
        article.setCollegeId(request.getCollegeId());
        article.setIsPinned(request.getIsPinned());
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        article.setIsApproved(0); // 默认待审核，需要管理员审核后才能展示
        article.setStatus(1);
        
        articleMapper.insert(article);
        
        // 处理标签
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            tagService.addTagsToArticle(article.getId(), request.getTags());
        }
        
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
        
        // MARKETPLACE 和 CAMPUS 板块所有登录用户都可以发布
    }
    
    public PageResult<Article> getArticleList(ArticleQueryRequest request, Long currentUserId) {
        Page<Article> page = new Page<>(request.getCurrent(), request.getSize());
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        
        if (request.getBoardType() != null) {
            wrapper.eq("board_type", request.getBoardType());
        }
        if (request.getCategory() != null && !request.getCategory().isEmpty()) {
            wrapper.eq("category", request.getCategory());
        }
        if (request.getCollegeId() != null) {
            wrapper.eq("college_id", request.getCollegeId());
        }
        if (request.getAuthorId() != null) {
            wrapper.eq("author_id", request.getAuthorId());
        }
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            // 关键词条件加括号，避免 OR 影响后续条件
            wrapper.and(w -> w.like("title", request.getKeyword())
                    .or().like("content", request.getKeyword()));
        }
        if (request.getIsApproved() != null) {
            wrapper.eq("is_approved", request.getIsApproved());
        }
        if (request.getIsPinned() != null) {
            wrapper.eq("is_pinned", request.getIsPinned());
        }
        // 日期过滤：闭区间 [startDate, endDate]，若仅传入单个日期则按当天过滤
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        if (startDate != null && endDate == null) {
            endDate = startDate;
        } else if (startDate == null && endDate != null) {
            startDate = endDate;
        }
        if (startDate != null) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay(); // 小于次日零点，确保包含当日
            wrapper.ge("created_at", startDateTime);
            wrapper.lt("created_at", endDateTime);
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
        
        // 查询最火评论（按点赞数降序取第一条）
        QueryWrapper<Comment> commentWrapper = new QueryWrapper<>();
        commentWrapper.eq("article_id", article.getId())
                .orderByDesc("like_count")
                .last("LIMIT 1");
        Comment hotComment = commentMapper.selectOne(commentWrapper);
        if (hotComment != null) {
            // 填充评论用户信息
            hotComment.setUser(userService.getUserInfo(hotComment.getUserId()));
            article.setHotComment(hotComment);
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
        
        boolean result = articleMapper.updateById(article) > 0;
        
        // 更新标签
        if (request.getTags() != null) {
            tagService.updateArticleTags(id, request.getTags());
        }
        
        return result;
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

    /**
     * 获取热门文章(按浏览量+点赞数排序)
     */
    public List<Article> getHotArticles(int count) {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("is_approved", 1)
               .eq("status", 1)
               .orderByDesc("view_count")
               .orderByDesc("like_count")
               .last("LIMIT " + count);
        return articleMapper.selectList(wrapper);
    }

    /**
     * 根据ID列表批量获取文章
     */
    public List<Article> getArticlesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.in("id", ids)
               .eq("is_approved", 1)
               .eq("status", 1);
        return articleMapper.selectList(wrapper);
    }

    public java.util.Map<String, Object> getPublicStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        // 1. 文章总数 (已审核通过)
        long articleCount = count(new QueryWrapper<Article>().eq("is_approved", 1));
        stats.put("articleCount", articleCount);
        
        // 2. 总浏览量
        QueryWrapper<Article> viewWrapper = new QueryWrapper<>();
        viewWrapper.select("IFNULL(SUM(view_count), 0) as total_views").eq("is_approved", 1);
        java.util.Map<String, Object> viewResult = articleMapper.selectMaps(viewWrapper)
                .stream().findFirst().orElse(new java.util.HashMap<>());
        stats.put("viewCount", viewResult.getOrDefault("total_views", 0));
        
        // 3. 互动评论数
        long commentCount = commentMapper.selectCount(new QueryWrapper<>());
        stats.put("commentCount", commentCount);
        
        // 4. 用户总数
        long userCount = userService.count();
        stats.put("userCount", userCount);
        
        return stats;
    }

    /**
     * 获取校园集市活跃用户（按发布动态数量排序）
     */
    public java.util.List<java.util.Map<String, Object>> getMarketplaceActiveUsers(int limit) {
        java.util.List<java.util.Map<String, Object>> activeUsers = new java.util.ArrayList<>();
        
        try {
            // 查询 MARKETPLACE 板块已审核通过的文章，按作者分组统计数量
            QueryWrapper<Article> wrapper = new QueryWrapper<>();
            wrapper.select("author_id", "COUNT(*) as post_count")
                   .eq("board_type", "MARKETPLACE")
                   .eq("is_approved", 1)
                   .eq("status", 1)
                   .groupBy("author_id")
                   .orderByDesc("post_count")
                   .last("LIMIT " + limit);
            
            java.util.List<java.util.Map<String, Object>> results = articleMapper.selectMaps(wrapper);
            
            if (results == null || results.isEmpty()) {
                return activeUsers;
            }
            
            for (java.util.Map<String, Object> row : results) {
                Object authorIdObj = row.get("author_id");
                Object postCountObj = row.get("post_count");
                
                if (authorIdObj == null || postCountObj == null) {
                    continue;
                }
                
                Long authorId = ((Number) authorIdObj).longValue();
                Long postCount = ((Number) postCountObj).longValue();
                
                try {
                    User user = userService.getUserInfo(authorId);
                    if (user != null) {
                        java.util.Map<String, Object> userInfo = new java.util.HashMap<>();
                        userInfo.put("id", user.getId());
                        userInfo.put("realName", user.getRealName());
                        userInfo.put("avatar", user.getAvatar());
                        userInfo.put("postCount", postCount);
                        activeUsers.add(userInfo);
                    }
                } catch (Exception e) {
                    // 用户不存在，跳过
                }
            }
        } catch (Exception e) {
            // 查询失败返回空列表
        }
        
        return activeUsers;
    }

    /**
     * 获取校园集市分类统计（按动态数量排序）
     */
    public java.util.List<java.util.Map<String, Object>> getMarketplaceCategoryStats() {
        java.util.List<java.util.Map<String, Object>> categoryStats = new java.util.ArrayList<>();
        
        // 分类名称映射
        java.util.Map<String, String> categoryLabels = new java.util.HashMap<>();
        categoryLabels.put("daily", "日常");
        categoryLabels.put("trade", "交易");
        categoryLabels.put("help", "互助");
        categoryLabels.put("activity", "组队");
        categoryLabels.put("lost", "失物");
        categoryLabels.put("study", "学习");
        categoryLabels.put("sports", "运动");
        
        try {
            // 查询 MARKETPLACE 板块已审核通过的文章，按分类分组统计数量
            QueryWrapper<Article> wrapper = new QueryWrapper<>();
            wrapper.select("category", "COUNT(*) as count")
                   .eq("board_type", "MARKETPLACE")
                   .eq("is_approved", 1)
                   .eq("status", 1)
                   .isNotNull("category")
                   .ne("category", "")
                   .groupBy("category")
                   .orderByDesc("count");
            
            java.util.List<java.util.Map<String, Object>> results = articleMapper.selectMaps(wrapper);
            
            if (results == null || results.isEmpty()) {
                return categoryStats;
            }
            
            for (java.util.Map<String, Object> row : results) {
                Object categoryObj = row.get("category");
                Object countObj = row.get("count");
                
                if (categoryObj == null || countObj == null) {
                    continue;
                }
                
                String category = categoryObj.toString();
                Long count = ((Number) countObj).longValue();
                String label = categoryLabels.getOrDefault(category, category);
                
                java.util.Map<String, Object> stat = new java.util.HashMap<>();
                stat.put("category", category);
                stat.put("label", label);
                stat.put("count", count);
                categoryStats.add(stat);
            }
        } catch (Exception e) {
            // 查询失败返回空列表
        }
        
        return categoryStats;
    }
}
