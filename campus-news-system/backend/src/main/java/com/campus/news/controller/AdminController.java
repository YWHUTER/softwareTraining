package com.campus.news.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.news.common.Result;
import com.campus.news.entity.Article;
import com.campus.news.entity.Comment;
import com.campus.news.entity.User;
import com.campus.news.mapper.ArticleMapper;
import com.campus.news.mapper.CollegeMapper;
import com.campus.news.mapper.CommentMapper;
import com.campus.news.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 管理后台控制器
 * 提供统计数据等管理功能
 */
@Tag(name = "管理后台接口")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final UserMapper userMapper;
    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final CollegeMapper collegeMapper;
    
    /**
     * 获取系统统计数据
     * GET /api/admin/statistics
     */
    @Operation(summary = "获取系统统计数据")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 用户总数
        long userCount = userMapper.selectCount(new QueryWrapper<>());
        statistics.put("userCount", userCount);
        
        // 文章总数（已审核）
        long articleCount = articleMapper.selectCount(
            new QueryWrapper<Article>().eq("is_approved", 1)
        );
        statistics.put("articleCount", articleCount);
        
        // 待审核文章数
        long pendingCount = articleMapper.selectCount(
            new QueryWrapper<Article>().eq("is_approved", 0)
        );
        statistics.put("pendingCount", pendingCount);
        
        // 评论总数
        long commentCount = commentMapper.selectCount(new QueryWrapper<>());
        statistics.put("commentCount", commentCount);
        
        // 学院数量
        long collegeCount = collegeMapper.selectCount(new QueryWrapper<>());
        statistics.put("collegeCount", collegeCount);
        
        // 各板块文章数
        long officialCount = articleMapper.selectCount(
            new QueryWrapper<Article>().eq("is_approved", 1).eq("board_type", "OFFICIAL")
        );
        long campusCount = articleMapper.selectCount(
            new QueryWrapper<Article>().eq("is_approved", 1).eq("board_type", "CAMPUS")
        );
        long collegeArticleCount = articleMapper.selectCount(
            new QueryWrapper<Article>().eq("is_approved", 1).eq("board_type", "COLLEGE")
        );
        statistics.put("officialCount", officialCount);
        statistics.put("campusCount", campusCount);
        statistics.put("collegeArticleCount", collegeArticleCount);
        
        // 总浏览量
        QueryWrapper<Article> viewWrapper = new QueryWrapper<>();
        viewWrapper.eq("is_approved", 1).select("IFNULL(SUM(view_count), 0) as total_views");
        Map<String, Object> viewResult = articleMapper.selectMaps(viewWrapper)
            .stream().findFirst().orElse(new HashMap<>());
        statistics.put("totalViews", viewResult.getOrDefault("total_views", 0));
        
        return Result.success(statistics);
    }
    
    /**
     * 获取图表数据
     * GET /api/admin/chart-data
     */
    @Operation(summary = "获取图表数据")
    @GetMapping("/chart-data")
    public Result<Map<String, Object>> getChartData() {
        Map<String, Object> chartData = new HashMap<>();
        
        // 1. 文章分类饼图数据
        List<Map<String, Object>> categoryData = new ArrayList<>();
        
        long officialCount = articleMapper.selectCount(
            new QueryWrapper<Article>().eq("is_approved", 1).eq("board_type", "OFFICIAL")
        );
        long campusCount = articleMapper.selectCount(
            new QueryWrapper<Article>().eq("is_approved", 1).eq("board_type", "CAMPUS")
        );
        long collegeCount = articleMapper.selectCount(
            new QueryWrapper<Article>().eq("is_approved", 1).eq("board_type", "COLLEGE")
        );
        
        categoryData.add(Map.of("name", "官方新闻", "value", officialCount));
        categoryData.add(Map.of("name", "全校新闻", "value", campusCount));
        categoryData.add(Map.of("name", "学院新闻", "value", collegeCount));
        chartData.put("categoryData", categoryData);
        
        // 2. 近7天文章发布趋势
        List<String> dates = new ArrayList<>();
        List<Long> articleCounts = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dates.add(date.format(formatter));
            
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            
            long count = articleMapper.selectCount(
                new QueryWrapper<Article>()
                    .ge("created_at", startOfDay)
                    .lt("created_at", endOfDay)
            );
            articleCounts.add(count);
        }
        
        chartData.put("trendDates", dates);
        chartData.put("trendCounts", articleCounts);
        
        // 3. 热门文章排行（按浏览量）
        QueryWrapper<Article> hotWrapper = new QueryWrapper<>();
        hotWrapper.eq("is_approved", 1)
                  .orderByDesc("view_count")
                  .last("LIMIT 5");
        List<Article> hotArticles = articleMapper.selectList(hotWrapper);
        
        List<String> hotTitles = new ArrayList<>();
        List<Integer> hotViews = new ArrayList<>();
        for (Article article : hotArticles) {
            // 截取标题，最多显示10个字符
            String title = article.getTitle();
            if (title.length() > 10) {
                title = title.substring(0, 10) + "...";
            }
            hotTitles.add(title);
            hotViews.add(article.getViewCount());
        }
        chartData.put("hotTitles", hotTitles);
        chartData.put("hotViews", hotViews);
        
        // 4. 文章审核状态饼图
        long approvedCount = articleMapper.selectCount(
            new QueryWrapper<Article>().eq("is_approved", 1)
        );
        long pendingCount = articleMapper.selectCount(
            new QueryWrapper<Article>().eq("is_approved", 0)
        );
        long rejectedCount = articleMapper.selectCount(
            new QueryWrapper<Article>().eq("is_approved", 2)
        );
        
        List<Map<String, Object>> statusData = new ArrayList<>();
        statusData.add(Map.of("name", "已通过", "value", approvedCount));
        statusData.add(Map.of("name", "待审核", "value", pendingCount));
        statusData.add(Map.of("name", "已拒绝", "value", rejectedCount));
        chartData.put("statusData", statusData);
        
        return Result.success(chartData);
    }
}
