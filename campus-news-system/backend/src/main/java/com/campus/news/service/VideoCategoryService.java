package com.campus.news.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.news.entity.VideoCategory;
import com.campus.news.mapper.VideoCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoCategoryService extends ServiceImpl<VideoCategoryMapper, VideoCategory> {
    
    private final VideoCategoryMapper videoCategoryMapper;
    
    public List<VideoCategory> getAllCategories() {
        QueryWrapper<VideoCategory> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort_order");
        return videoCategoryMapper.selectList(wrapper);
    }
    
    public VideoCategory getByCode(String code) {
        QueryWrapper<VideoCategory> wrapper = new QueryWrapper<>();
        wrapper.eq("code", code);
        return videoCategoryMapper.selectOne(wrapper);
    }
}
