package com.campus.news.controller;

import com.campus.news.common.Result;
import com.campus.news.entity.Tag;
import com.campus.news.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签控制器
 */
@RestController
@RequestMapping("/tag")
@RequiredArgsConstructor
public class TagController {
    
    private final TagService tagService;
    
    /**
     * 获取热门标签（标签云）
     */
    @GetMapping("/hot")
    public Result<List<Tag>> getHotTags(
            @RequestParam(defaultValue = "30") int limit
    ) {
        List<Tag> tags = tagService.getHotTags(limit);
        return Result.success(tags);
    }
    
    /**
     * 获取所有标签
     */
    @GetMapping("/list")
    public Result<List<Tag>> getAllTags() {
        List<Tag> tags = tagService.getAllTags();
        return Result.success(tags);
    }
    
    /**
     * 获取文章的标签
     */
    @GetMapping("/article/{articleId}")
    public Result<List<Tag>> getArticleTags(@PathVariable Long articleId) {
        List<Tag> tags = tagService.getArticleTags(articleId);
        return Result.success(tags);
    }
}
