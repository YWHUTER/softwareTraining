package com.campus.news.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.news.entity.Article;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
}
