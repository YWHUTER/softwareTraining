package com.campus.news.controller;

import com.campus.news.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Tag(name = "文件上传接口")
@RestController
@RequestMapping("/file")
public class FileController {

    @Value("${file.upload.path:}")
    private String configuredPath;
    
    private String uploadPath;

    @PostConstruct
    public void init() {
        // 如果配置了自定义路径，使用配置的路径；否则使用用户目录下的默认路径
        if (configuredPath != null && !configuredPath.trim().isEmpty()) {
            uploadPath = configuredPath;
        } else {
            // 默认使用用户目录下的固定路径，避免 Tomcat 临时目录问题
            String userHome = System.getProperty("user.home");
            uploadPath = userHome + File.separator + "campus-news-uploads";
        }
        
        // 确保上传目录存在且可写
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                log.error("无法创建上传目录: {}，请检查权限或在 application.yml 中配置 file.upload.path", dir.getAbsolutePath());
            }
        }
        
        // 检查目录是否可写
        if (dir.exists() && !dir.canWrite()) {
            log.error("上传目录无写入权限: {}，请检查权限或在 application.yml 中配置其他路径", dir.getAbsolutePath());
        }
        
        log.info("文件上传目录: {} (可写: {})", dir.getAbsolutePath(), dir.canWrite());
    }

    @Operation(summary = "上传图片")
    @PostMapping("/upload")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file, 
                                     HttpServletRequest request) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只支持上传图片文件");
        }

        // 验证文件大小 (最大 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error("文件大小不能超过 5MB");
        }

        try {
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + extension;

            // 按日期分目录存储
            String dateDir = java.time.LocalDate.now().toString();
            Path dirPath = Paths.get(uploadPath, dateDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // 保存文件
            Path filePath = dirPath.resolve(newFilename);
            file.transferTo(filePath.toFile());

            // 返回访问URL
            String fileUrl = "/api/file/image/" + dateDir + "/" + newFilename;
            log.info("文件上传成功: {}", fileUrl);

            return Result.success(fileUrl);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取图片")
    @GetMapping("/image/{date}/{filename}")
    public void getImage(@PathVariable String date, 
                        @PathVariable String filename,
                        jakarta.servlet.http.HttpServletResponse response) {
        try {
            Path filePath = Paths.get(uploadPath, date, filename);
            if (!Files.exists(filePath)) {
                response.setStatus(404);
                return;
            }

            // 设置响应类型
            String contentType = Files.probeContentType(filePath);
            if (contentType != null) {
                response.setContentType(contentType);
            }

            // 输出文件内容
            Files.copy(filePath, response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("获取图片失败", e);
            response.setStatus(500);
        }
    }
}
