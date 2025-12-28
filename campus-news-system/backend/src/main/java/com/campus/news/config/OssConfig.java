package com.campus.news.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {
    
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String videoDir = "videos";
    private String thumbnailDir = "thumbnails";
    private long maxFileSize = 500 * 1024 * 1024L;
    
    /**
     * 允许的视频格式
     */
    private String[] allowedFormats = {"mp4", "avi", "mov", "mkv", "flv", "wmv", "webm"};
    
    /**
     * 获取文件访问的基础URL
     */
    public String getBaseUrl() {
        return "https://" + bucketName + "." + endpoint;
    }
    
    @Bean
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
