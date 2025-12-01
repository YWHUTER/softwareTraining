# 快速上手指南

## 🚀 5分钟快速启动

### 前置条件检查

```bash
# 检查Java版本 (需要 17+)
java -version

# 检查Maven版本 (需要 3.6+)
mvn -version

# 检查Node版本 (需要 16+)
node -v

# 检查MySQL是否运行
mysql -u root -p
```

### Step 1: 初始化数据库 (2分钟)

```bash
# 登录MySQL
mysql -u root -p

# 执行数据库脚本
source d:/softwareTraining/campus-news-system/database/campus_news_system.sql

# 或者使用命令行
mysql -u root -p < d:/softwareTraining/campus-news-system/database/campus_news_system.sql
```

### Step 2: 启动后端 (1分钟)

**方式一：使用启动脚本（推荐）**
```bash
# 双击运行
start-backend.bat
```

**方式二：手动启动**
```bash
cd backend
mvn spring-boot:run
```

✅ 看到以下日志说明启动成功：
```
Started CampusNewsApplication in X.XXX seconds
```

### Step 3: 启动前端 (2分钟)

**方式一：使用启动脚本（推荐）**
```bash
# 双击运行
start-frontend.bat
```

**方式二：手动启动**
```bash
cd frontend
npm install  # 首次运行
npm run dev
```

✅ 看到以下信息说明启动成功：
```
VITE vX.X.X  ready in XXX ms
➜  Local:   http://localhost:3000/
```

## 🎉 开始使用

### 1. 访问系统

打开浏览器访问：`http://localhost:3000`

### 2. 登录测试账号

**管理员账号：**
- 用户名：`admin`
- 密码：`admin123`

### 3. 注册新用户

1. 点击右上角"注册"
2. 填写用户信息
3. 选择角色（学生/教师）
4. 选择学院
5. 完成注册

### 4. 发布第一篇文章

1. 登录后点击"发布文章"
2. 填写标题和内容
3. 选择板块类型
4. 点击发布

## 📱 功能导航

### 普通用户功能

| 功能 | 路径 | 说明 |
|------|------|------|
| 首页 | `/` | 查看最新资讯 |
| 官方新闻 | `/board/OFFICIAL` | 查看官方新闻 |
| 全校新闻 | `/board/CAMPUS` | 查看全校新闻 |
| 学院新闻 | `/board/COLLEGE` | 查看学院新闻 |
| 文章详情 | `/article/:id` | 查看文章详情、评论 |
| 发布文章 | `/publish` | 发布新文章 |
| 个人中心 | `/profile` | 管理个人文章 |

### 管理员功能

| 功能 | 路径 | 说明 |
|------|------|------|
| 数据概览 | `/admin` | 查看系统统计 |
| 用户管理 | `/admin/users` | 管理所有用户 |
| 文章管理 | `/admin/articles` | 审核、置顶、删除文章 |
| 学院管理 | `/admin/colleges` | 管理学院信息 |

## 🔑 默认数据

### 预设角色

| ID | 角色名 | 说明 |
|----|--------|------|
| 1 | ADMIN | 管理员 |
| 2 | TEACHER | 教师 |
| 3 | STUDENT | 学生 |

### 预设学院

| ID | 学院名称 | 代码 |
|----|----------|------|
| 1 | 计算机科学与技术学院 | CS |
| 2 | 软件工程学院 | SE |
| 3 | 人工智能学院 | AI |
| 4 | 电子信息工程学院 | EE |
| 5 | 经济管理学院 | EM |

## 📖 API文档

启动后端后，访问Swagger文档：

**地址**: `http://localhost:8080/api/swagger-ui.html`

在这里可以：
- 查看所有API接口
- 在线测试API
- 查看请求/响应格式

## ⚙️ 配置修改

### 修改数据库配置

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_news_system
    username: root
    password: 你的密码
```

### 修改端口

**后端端口** (默认8080)：
```yaml
# backend/src/main/resources/application.yml
server:
  port: 8080
```

**前端端口** (默认3000)：
```js
// frontend/vite.config.js
export default defineConfig({
  server: {
    port: 3000
  }
})
```

### 禁用Redis（可选）

如果不使用Redis，注释以下配置：

```yaml
# backend/src/main/resources/application.yml
# spring:
#   data:
#     redis:
#       host: localhost
#       port: 6379
```

## 🐛 常见问题解决

### 问题1: 后端启动失败，提示数据库连接错误

**解决方案：**
1. 确认MySQL已启动
2. 检查数据库名是否为 `campus_news_system`
3. 检查用户名密码是否正确
4. 检查是否执行了数据库脚本

### 问题2: 前端白屏，控制台报错

**解决方案：**
1. 确认后端服务已启动
2. 检查浏览器控制台错误信息
3. 清除浏览器缓存
4. 检查 `vite.config.js` 代理配置

### 问题3: 登录后提示401未授权

**解决方案：**
1. 清除浏览器 localStorage
2. 重新登录
3. 检查JWT配置

### 问题4: 编译失败

**后端：**
```bash
cd backend
mvn clean
mvn package -DskipTests
```

**前端：**
```bash
cd frontend
rm -rf node_modules
npm install
```

### 问题5: 依赖下载慢

**Maven加速（使用阿里云镜像）：**

编辑 `~/.m2/settings.xml`，添加：
```xml
<mirror>
  <id>aliyun</id>
  <mirrorOf>central</mirrorOf>
  <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

**npm加速（使用淘宝镜像）：**
```bash
npm config set registry https://registry.npmmirror.com
```

## 📝 测试流程

### 基础功能测试

1. **注册登录**
   - [ ] 注册新用户（学生）
   - [ ] 登录系统
   - [ ] 查看个人信息

2. **发布文章**
   - [ ] 发布全校新闻
   - [ ] 发布学院新闻
   - [ ] 上传封面图片

3. **互动功能**
   - [ ] 浏览文章
   - [ ] 点赞文章
   - [ ] 收藏文章
   - [ ] 发表评论
   - [ ] 回复评论

4. **管理功能**（使用admin账号）
   - [ ] 查看数据统计
   - [ ] 审核文章
   - [ ] 置顶文章
   - [ ] 管理用户
   - [ ] 管理学院

## 🎓 学习路径

### 初学者路径

1. **理解项目结构** (30分钟)
   - 查看 `README.md`
   - 了解前后端目录结构

2. **运行项目** (30分钟)
   - 按照本指南启动项目
   - 测试基本功能

3. **学习后端** (2-3天)
   - Spring Boot基础
   - MyBatis-Plus使用
   - Spring Security认证

4. **学习前端** (2-3天)
   - Vue 3 Composition API
   - Element Plus组件
   - Axios请求封装

### 进阶路径

1. **添加新功能** (1周)
   - 文章标签系统
   - 用户关注功能
   - 消息通知

2. **性能优化** (3-5天)
   - Redis缓存优化
   - SQL查询优化
   - 前端性能优化

3. **部署上线** (2-3天)
   - 服务器部署
   - Nginx配置
   - 域名绑定

## 💡 最佳实践

### 开发建议

1. **使用IDE**
   - 后端推荐：IntelliJ IDEA
   - 前端推荐：VS Code

2. **代码规范**
   - 遵循项目现有代码风格
   - 添加必要的注释
   - 保持代码整洁

3. **版本控制**
   - 使用Git管理代码
   - 及时提交更改
   - 编写清晰的commit信息

4. **测试优先**
   - 新功能先测试
   - 保证核心功能可用
   - 及时修复bug

## 🎯 下一步计划

完成基础功能后，可以考虑：

- [ ] 接入AI大模型API
- [ ] 添加文件上传功能
- [ ] 实现消息通知系统
- [ ] 开发移动端应用
- [ ] 添加数据导出功能
- [ ] 集成第三方登录

## 📞 获取帮助

- 查看完整文档：`README.md`
- 查看API文档：`http://localhost:8080/api/swagger-ui.html`
- 查看项目总览：`PROJECT_SUMMARY.md`

---

**祝您使用愉快！🎉**
