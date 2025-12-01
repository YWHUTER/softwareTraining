package com.campus.news.controller;

import com.campus.news.common.Result;
import com.campus.news.entity.College;
import com.campus.news.service.CollegeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "学院接口")
@RestController
@RequestMapping("/college")
@RequiredArgsConstructor
public class CollegeController {
    
    private final CollegeService collegeService;
    
    @Operation(summary = "获取学院列表")
    @GetMapping("/list")
    public Result<List<College>> getCollegeList() {
        return Result.success(collegeService.list());
    }
    
    @Operation(summary = "创建学院")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public Result<Boolean> createCollege(@RequestBody College college) {
        return Result.success(collegeService.save(college));
    }
    
    @Operation(summary = "更新学院")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/update")
    public Result<Boolean> updateCollege(@RequestBody College college) {
        return Result.success(collegeService.updateById(college));
    }
    
    @Operation(summary = "删除学院")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteCollege(@PathVariable Long id) {
        return Result.success(collegeService.removeById(id));
    }
}
