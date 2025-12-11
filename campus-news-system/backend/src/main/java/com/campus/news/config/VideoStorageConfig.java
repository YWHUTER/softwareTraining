package com.campus.news.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "video.storage")
public class VideoStorageConfig {
    
    /**
     * 远程服务器IP
     */
    private String host = "203.195.182.166";
    
    /**
     * SSH端口
     */
    private int port = 22;
    
    /**
     * SSH用户名
     */
    private String username = "root";
    
    /**
     * SSH密码
     */
    private String password;
    
    /**
     * 远程存储路径
     */
    private String remotePath = "/var/www/videos";
    
    /**
     * 视频访问基础URL
     */
    private String baseUrl = "http://203.195.182.166/videos";
    
    /**
     * 最大文件大小(字节) 默认500MB
     */
    private long maxFileSize = 500 * 1024 * 1024L;
    
    /**
     * 允许的视频格式
     */
    private String[] allowedFormats = {"mp4", "avi", "mov", "mkv", "flv", "wmv", "webm"};
}
