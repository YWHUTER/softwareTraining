package com.campus.news.service;

import com.campus.news.config.VideoStorageConfig;
import com.campus.news.exception.BusinessException;
import com.jcraft.jsch.*;
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
    
    private final VideoStorageConfig config;
    
    /**
     * 上传视频到远程服务器
     * @param file 视频文件
     * @return 视频访问URL
     */
    public String uploadVideo(MultipartFile file) {
        validateFile(file);
        
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString() + "." + extension;
        String dateDir = LocalDate.now().toString();
        String remotePath = config.getRemotePath() + "/" + dateDir;
        String remoteFile = remotePath + "/" + newFilename;
        
        Session session = null;
        ChannelSftp channelSftp = null;
        
        try {
            session = createSession();
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            
            // 确保远程目录存在
            createRemoteDirectory(channelSftp, remotePath);
            
            // 上传文件
            try (InputStream inputStream = file.getInputStream()) {
                channelSftp.put(inputStream, remoteFile);
            }
            
            log.info("视频上传成功: {}", remoteFile);
            return config.getBaseUrl() + "/" + dateDir + "/" + newFilename;
            
        } catch (Exception e) {
            log.error("视频上传失败", e);
            throw new BusinessException("视频上传失败: " + e.getMessage());
        } finally {
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }
    
    /**
     * 上传缩略图到远程服务器
     */
    public String uploadThumbnail(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String newFilename = "thumb_" + UUID.randomUUID().toString() + "." + extension;
        String dateDir = LocalDate.now().toString();
        String remotePath = config.getRemotePath() + "/thumbnails/" + dateDir;
        String remoteFile = remotePath + "/" + newFilename;
        
        Session session = null;
        ChannelSftp channelSftp = null;
        
        try {
            session = createSession();
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            
            createRemoteDirectory(channelSftp, remotePath);
            
            try (InputStream inputStream = file.getInputStream()) {
                channelSftp.put(inputStream, remoteFile);
            }
            
            log.info("缩略图上传成功: {}", remoteFile);
            return config.getBaseUrl() + "/thumbnails/" + dateDir + "/" + newFilename;
            
        } catch (Exception e) {
            log.error("缩略图上传失败", e);
            throw new BusinessException("缩略图上传失败: " + e.getMessage());
        } finally {
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }
    
    /**
     * 删除远程文件
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(config.getBaseUrl())) {
            return;
        }
        
        String relativePath = fileUrl.replace(config.getBaseUrl(), "");
        String remoteFile = config.getRemotePath() + relativePath;
        
        Session session = null;
        ChannelSftp channelSftp = null;
        
        try {
            session = createSession();
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            channelSftp.rm(remoteFile);
            log.info("文件删除成功: {}", remoteFile);
        } catch (Exception e) {
            log.warn("文件删除失败: {}", e.getMessage());
        } finally {
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }
    
    private Session createSession() throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());
        session.setPassword(config.getPassword());
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(30000);
        return session;
    }
    
    private void createRemoteDirectory(ChannelSftp channelSftp, String remotePath) {
        String[] folders = remotePath.split("/");
        StringBuilder currentPath = new StringBuilder();
        
        for (String folder : folders) {
            if (folder.isEmpty()) continue;
            currentPath.append("/").append(folder);
            try {
                channelSftp.cd(currentPath.toString());
            } catch (SftpException e) {
                try {
                    channelSftp.mkdir(currentPath.toString());
                    channelSftp.cd(currentPath.toString());
                } catch (SftpException ex) {
                    log.warn("创建目录失败: {}", currentPath);
                }
            }
        }
    }
    
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的视频文件");
        }
        
        if (file.getSize() > config.getMaxFileSize()) {
            throw new BusinessException("视频文件大小不能超过 " + (config.getMaxFileSize() / 1024 / 1024) + "MB");
        }
        
        String extension = getFileExtension(file.getOriginalFilename());
        boolean isAllowed = Arrays.stream(config.getAllowedFormats())
                .anyMatch(format -> format.equalsIgnoreCase(extension));
        
        if (!isAllowed) {
            throw new BusinessException("不支持的视频格式，支持的格式: " + String.join(", ", config.getAllowedFormats()));
        }
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
