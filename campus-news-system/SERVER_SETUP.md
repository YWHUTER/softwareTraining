# 视频服务器配置指南

## 服务器信息
- IP: 203.195.182.166
- 操作系统: CentOS 7.6 64bit
- 用途: 存储和提供视频文件访问

## 1. 服务器初始化

### 1.1 安装Nginx (CentOS 7)
```bash
# 安装EPEL源
sudo yum install epel-release -y

# 安装Nginx
sudo yum install nginx -y

# 启动Nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

### 1.2 创建视频存储目录
```bash
sudo mkdir -p /var/www/videos
sudo mkdir -p /var/www/videos/thumbnails
sudo chown -R www-data:www-data /var/www/videos
sudo chmod -R 755 /var/www/videos
```

## 2. Nginx配置

### 2.1 创建视频服务配置 (CentOS)
```bash
sudo vi /etc/nginx/conf.d/videos.conf
```

添加以下内容:
```nginx
server {
    listen 80;
    server_name 203.195.182.166;

    # 视频文件目录
    location /videos {
        alias /var/www/videos;
        
        # 允许跨域访问
        add_header Access-Control-Allow-Origin *;
        add_header Access-Control-Allow-Methods 'GET, OPTIONS';
        add_header Access-Control-Allow-Headers 'Range';
        
        # 支持视频范围请求(拖动进度条)
        add_header Accept-Ranges bytes;
        
        # 缓存设置
        expires 7d;
        add_header Cache-Control "public, immutable";
        
        # MIME类型
        types {
            video/mp4 mp4;
            video/webm webm;
            video/x-msvideo avi;
            video/quicktime mov;
            video/x-matroska mkv;
            video/x-flv flv;
            video/x-ms-wmv wmv;
            image/jpeg jpg jpeg;
            image/png png;
            image/gif gif;
        }
    }
    
    # 健康检查
    location /health {
        return 200 'OK';
        add_header Content-Type text/plain;
    }
}
```

### 2.2 启用配置 (CentOS)
```bash
# 测试配置
sudo nginx -t

# 重载Nginx
sudo systemctl reload nginx
```

## 3. 防火墙配置 (CentOS 7)

```bash
# 开放HTTP端口
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --reload

# 如果firewalld未运行，可以使用iptables
# sudo iptables -I INPUT -p tcp --dport 80 -j ACCEPT
# sudo service iptables save
```

## 4. 后端配置

在 `application.yml` 中配置视频服务器信息:

```yaml
video:
  storage:
    host: 203.195.182.166
    port: 22
    username: root
    password: 你的服务器密码
    remote-path: /var/www/videos
    base-url: http://203.195.182.166/videos
    max-file-size: 524288000  # 500MB
```

或者使用环境变量:
```bash
export VIDEO_SERVER_HOST=203.195.182.166
export VIDEO_SERVER_PORT=22
export VIDEO_SERVER_USER=root
export VIDEO_SERVER_PASSWORD=你的密码
```

## 5. 测试

### 5.1 测试SSH连接
```bash
ssh root@203.195.182.166
```

### 5.2 测试视频访问
上传视频后，访问:
```
http://203.195.182.166/videos/2024-12-11/xxx.mp4
```

## 6. 安全建议

1. **使用SSH密钥认证** 而非密码
2. **配置HTTPS** 使用Let's Encrypt免费证书
3. **限制上传文件大小** 在Nginx中配置 `client_max_body_size`
4. **定期清理** 过期或无效的视频文件

## 7. 可选: HTTPS配置

```bash
# 安装certbot
sudo apt install certbot python3-certbot-nginx -y

# 获取证书(需要域名)
sudo certbot --nginx -d your-domain.com
```

## 8. 数据库初始化

在MySQL中执行视频表创建脚本:
```bash
mysql -u root -p campus_news_system < database/video_tables.sql
```
