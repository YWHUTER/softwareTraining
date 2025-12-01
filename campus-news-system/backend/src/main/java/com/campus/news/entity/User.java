package com.campus.news.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    
    @JsonIgnore
    private String password;
    
    private String email;
    private String realName;
    private String phone;
    private String avatar;
    private Long collegeId;
    private String studentId;
    
    /**
     * 用户状态: 0-禁用 1-正常
     */
    private Integer status;
    
    /**
     * 关注数
     */
    private Integer followingCount;
    
    /**
     * 粉丝数
     */
    private Integer followerCount;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableField(exist = false)
    private List<Role> roles;
    
    @TableField(exist = false)
    private College college;
    
    /**
     * 当前用户是否已关注此用户（非数据库字段）
     */
    @TableField(exist = false)
    private Boolean isFollowed;
}
