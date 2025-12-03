package com.campus.news.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.news.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {
    
    /**
     * 获取热门标签（按使用次数排序）
     */
    @Select("SELECT * FROM tag ORDER BY use_count DESC LIMIT #{limit}")
    List<Tag> getHotTags(int limit);
    
    /**
     * 根据名称查找标签
     */
    @Select("SELECT * FROM tag WHERE name = #{name}")
    Tag findByName(String name);
}
