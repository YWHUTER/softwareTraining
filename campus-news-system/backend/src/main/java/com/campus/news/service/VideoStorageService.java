package com.campus.news.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.campus.news.config.OssConfig;
import com.campus.news.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoStorageService {
    
    private final OssConfig ossConfig;
    private final OSS ossClient;
    
    /**
     * 上传视频到阿里云OSS
     */
    public String uploadVideo(MultipartFile file) {
        validateFile(file);
        
        String extension = getFileExtension(file.getOriginalFilename());
        String newFilename = UUID.randomUUID().toString() + "." + extension;
        String dateDir = LocalDate.now().toString();
        String objectKey = ossConfig.getVideoDir() + "/" + dateDir + "/" + newFilename;
        
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            
            ossClient.putObject(ossConfig.getBucketName(), objectKey, inputStream, metadata);
            
            String url = ossConfig.getBaseUrl() + "/" + objectKey;
            log.info("视频上传成功: {}", url);
            return url;
        } catch (Exception e) {
            log.error("视频上传失败", e);
            throw new BusinessException("视频上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传缩略图到阿里云OSS
     */
    public String uploadThumbnail(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        
        String extension = getFileExtension(file.getOriginalFilename());
        String newFilename = "thumb_" + UUID.randomUUID().toString() + "." + extension;
        String dateDir = LocalDate.now().toString();
        String objectKey = ossConfig.getThumbnailDir() + "/" + dateDir + "/" + newFilename;
        
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            
            ossClient.putObject(ossConfig.getBucketName(), objectKey, inputStream, metadata);
            
            String url = ossConfig.getBaseUrl() + "/" + objectKey;
            log.info("缩略图上传成功: {}", url);
            return url;
        } catch (Exception e) {
            log.error("缩略图上传失败", e);
            throw new BusinessException("缩略图上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除OSS文件
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(ossConfig.getBaseUrl())) {
            return;
        }
        
        String objectKey = fileUrl.replace(ossConfig.getBaseUrl() + "/", "");
        
        try {
            ossClient.deleteObject(ossConfig.getBucketName(), objectKey);
            log.info("文件删除成功: {}", objectKey);
        } catch (Exception e) {
            log.warn("文件删除失败: {}", e.getMessage());
        }
    }
    
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的视频文件");
        }
        
        if (file.getSize() > ossConfig.getMaxFileSize()) {
            throw new BusinessException("视频文件大小不能超过 " + (ossConfig.getMaxFileSize() / 1024 / 1024) + "MB");
        }
        
        String extension = getFileExtension(file.getOriginalFilename());
        boolean isAllowed = Arrays.stream(ossConfig.getAllowedFormats())
                .anyMatch(format -> format.equalsIgnoreCase(extension));
        
        if (!isAllowed) {
            throw new BusinessException("不支持的视频格式，支持的格式: " + String.join(", ", ossConfig.getAllowedFormats()));
        }
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
