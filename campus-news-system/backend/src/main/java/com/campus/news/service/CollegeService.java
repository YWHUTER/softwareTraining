package com.campus.news.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.news.entity.College;
import com.campus.news.mapper.CollegeMapper;
import org.springframework.stereotype.Service;

@Service
public class CollegeService extends ServiceImpl<CollegeMapper, College> {
}
