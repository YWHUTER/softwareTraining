# 校园新闻发布系统

## 项目简介

校园新闻发布系统是一个基于前后端分离架构的全栈Web应用，旨在为高校提供一个功能完善的新闻与信息发布平台。系统支持多角色管理、多板块发布、文章审核、评论互动等核心功能。

## 技术栈

### 后端技术
- **Spring Boot 3.2.0** - 核心框架
- **Spring Security + JWT** - 安全认证
- **MyBatis-Plus 3.5.5** - ORM框架
- **MySQL 8.0+** - 数据库
- **Redis** - 缓存中间件
- **Swagger 3 (SpringDoc)** - API文档

### 前端技术
- **Vue 3.4** - 前端框架
- **Vite 5.0** - 构建工具
- **Element Plus 2.4** - UI组件库
- **Vue Router 4.2** - 路由管理
- **Pinia 2.1** - 状态管理
- **Axios 1.6** - HTTP客户端
- **Quill Editor** - 富文本编辑器

## 系统功能

### 1. 用户系统
- ✅ 用户注册与登录（JWT认证）
- ✅ 三种角色：管理员 / 教师 / 学生
- ✅ 用户资料管理
- ✅ 学院绑定

### 2. 文章系统
- ✅ 三类板块：官方新闻 / 全校新闻 / 学院新闻
- ✅ 发布、编辑、删除文章
- ✅ 富文本内容编辑
- ✅ 文章置顶
- ✅ 文章审核（可选）
- ✅ 浏览量统计（Redis缓存）
- ✅ 点赞与收藏

### 3. 评论系统
- ✅ 发布评论
- ✅ 回复评论（支持一层嵌套）
- ✅ 删除评论

### 4. 管理后台
- ✅ 数据统计概览
- ✅ 用户管理（启用/禁用）
- ✅ 文章管理（审核/置顶/删除）
- ✅ 学院管理（增删改查）

### 5. AI模块（预留）
- 📌 文章智能推荐
- 📌 内容审核辅助
- 📌 自动摘要生成
- 📌 智能问答

> **注意**：AI功能暂未实现，仅保留代码结构扩展点。

## 项目结构

```
campus-news-system/
├── backend/                          # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/campus/news/
│   │   │   │   ├── ai/              # AI模块（预留）
│   │   │   │   ├── common/          # 通用响应类
│   │   │   │   ├── config/          # 配置类
│   │   │   │   ├── controller/      # 控制器层
│   │   │   │   ├── dto/             # 数据传输对象
│   │   │   │   ├── entity/          # 实体类
│   │   │   │   ├── exception/       # 异常处理
│   │   │   │   ├── mapper/          # Mapper接口
│   │   │   │   ├── security/        # 安全配置
│   │   │   │   ├── service/         # 服务层
│   │   │   │   └── util/            # 工具类
│   │   │   └── resources/
│   │   │       └── application.yml  # 配置文件
│   │   └── test/
│   └── pom.xml                       # Maven依赖
│
├── frontend/                         # 前端项目
│   ├── src/
│   │   ├── api/                     # API接口封装
│   │   ├── assets/                  # 静态资源
│   │   ├── layouts/                 # 布局组件
│   │   ├── router/                  # 路由配置
│   │   ├── stores/                  # 状态管理
│   │   ├── utils/                   # 工具函数
│   │   ├── views/                   # 页面组件
│   │   │   ├── admin/              # 管理后台页面
│   │   │   ├── ArticleDetail.vue  # 文章详情
│   │   │   ├── Board.vue           # 板块页面
│   │   │   ├── Home.vue            # 首页
│   │   │   ├── Login.vue           # 登录页
│   │   │   ├── Profile.vue         # 个人中心
│   │   │   ├── Publish.vue         # 发布文章
│   │   │   └── Register.vue        # 注册页
│   │   ├── App.vue
│   │   └── main.js
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
│
└── database/
    └── campus_news_system.sql       # 数据库脚本
```

## 数据库设计

系统包含以下8个核心数据表：

| 表名 | 说明 |
|------|------|
| `user` | 用户表 |
| `role` | 角色表 |
| `user_role` | 用户角色关联表 |
| `college` | 学院表 |
| `article` | 文章表 |
| `comment` | 评论表 |
| `article_like` | 文章点赞表 |
| `article_favorite` | 文章收藏表 |

## 部署指南

### 环境要求

- **JDK**: 17+
- **Maven**: 3.6+
- **Node.js**: 16+
- **MySQL**: 8.0+
- **Redis**: 6.0+ (可选)

### 1. 数据库初始化

```bash
# 登录MySQL
mysql -u root -p

# 执行数据库脚本
source /path/to/campus-news-system/database/campus_news_system.sql

# 或使用命令
mysql -u root -p < campus-news-system/database/campus_news_system.sql
```

**默认管理员账号**：
- 用户名：`admin`
- 密码：`admin123`

### 2. 后端部署

```bash
# 进入后端目录
cd campus-news-system/backend

# 修改配置文件 src/main/resources/application.yml
# 配置数据库连接信息：
#   spring.datasource.url
#   spring.datasource.username
#   spring.datasource.password
# 配置Redis连接信息（如不使用Redis可注释相关配置）

# 编译打包
mvn clean package -DskipTests

# 运行
java -jar target/campus-news-system-1.0.0.jar

# 或直接运行（开发模式）
mvn spring-boot:run
```

后端服务默认运行在 `http://localhost:8080/api`

**Swagger文档地址**: `http://localhost:8080/api/swagger-ui.html`

### 3. 前端部署

```bash
# 进入前端目录
cd campus-news-system/frontend

# 安装依赖
npm install

# 开发模式运行
npm run dev

# 生产环境打包
npm run build
```

前端服务默认运行在 `http://localhost:3000`

### 4. 生产环境部署

#### 后端生产部署

```bash
# 打包
mvn clean package -DskipTests

# 使用nohup后台运行
nohup java -jar target/campus-news-system-1.0.0.jar > app.log 2>&1 &

# 或使用systemd服务管理
# 创建服务文件 /etc/systemd/system/campus-news.service
```

#### 前端生产部署

```bash
# 打包
npm run build

# 使用Nginx部署
# 将 dist 目录内容复制到Nginx静态文件目录
# 配置反向代理到后端API
```

**Nginx配置示例**:

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    root /var/www/campus-news/dist;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## API接口文档

### 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 用户登录 |
| POST | `/api/auth/register` | 用户注册 |

### 用户接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/info` | 获取当前用户信息 |
| GET | `/api/user/list` | 获取用户列表（管理员） |
| PUT | `/api/user/status/{userId}` | 更新用户状态（管理员） |

### 文章接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/article/list` | 获取文章列表 |
| GET | `/api/article/detail/{id}` | 获取文章详情 |
| POST | `/api/article/create` | 创建文章 |
| PUT | `/api/article/update/{id}` | 更新文章 |
| DELETE | `/api/article/delete/{id}` | 删除文章 |
| POST | `/api/article/like/{id}` | 点赞/取消点赞 |
| POST | `/api/article/favorite/{id}` | 收藏/取消收藏 |
| PUT | `/api/article/pin/{id}` | 置顶文章（管理员） |
| PUT | `/api/article/approve/{id}` | 审核文章（管理员） |

### 评论接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/comment/list` | 获取评论列表 |
| POST | `/api/comment/create` | 创建评论 |
| DELETE | `/api/comment/delete/{id}` | 删除评论 |

### 学院接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/college/list` | 获取学院列表 |
| POST | `/api/college/create` | 创建学院（管理员） |
| PUT | `/api/college/update` | 更新学院（管理员） |
| DELETE | `/api/college/delete/{id}` | 删除学院（管理员） |

## 权限说明

### 角色权限

| 角色 | 权限 |
|------|------|
| **管理员 (ADMIN)** | 所有权限 |
| **教师 (TEACHER)** | 发布官方新闻、全校新闻、学院新闻 |
| **学生 (STUDENT)** | 发布全校新闻、学院新闻 |

### 板块发布权限

| 板块类型 | 可发布角色 |
|----------|-----------|
| 官方新闻 (OFFICIAL) | 管理员、教师 |
| 全校新闻 (CAMPUS) | 所有用户 |
| 学院新闻 (COLLEGE) | 所属学院的师生 |

## 开发说明

### 后端开发

1. **添加新接口**：在对应的Controller中添加方法
2. **业务逻辑**：在Service层实现
3. **数据访问**：在Mapper/Entity中定义
4. **全局异常处理**：统一在GlobalExceptionHandler中处理
5. **API文档**：使用Swagger注解自动生成

### 前端开发

1. **新增页面**：在`views`目录创建Vue组件
2. **添加路由**：在`router/index.js`中配置
3. **API调用**：在`api`目录封装接口
4. **状态管理**：使用Pinia管理全局状态
5. **UI组件**：使用Element Plus组件库

## 常见问题

### 1. 后端启动失败

- 检查MySQL是否启动且数据库已创建
- 检查`application.yml`中数据库配置是否正确
- 检查JDK版本是否为17+

### 2. 前端API调用失败

- 检查后端服务是否启动
- 检查`vite.config.js`中代理配置
- 检查浏览器控制台网络请求状态

### 3. JWT认证失败

- 检查token是否过期（默认7天）
- 检查请求头是否携带`Authorization`
- 清除localStorage重新登录

### 4. Redis连接失败

- 如不使用Redis，可在`application.yml`中注释相关配置
- 检查Redis服务是否启动
- 检查Redis连接配置是否正确

## 后续扩展

### AI模块接入

系统已预留AI模块接口（`com.campus.news.ai`包），可接入以下功能：

1. **智能推荐**：基于用户行为推荐相关文章
2. **内容审核**：自动检测敏感词和违规内容
3. **摘要生成**：自动生成文章摘要
4. **智能问答**：基于文章内容的智能问答

### 功能优化建议

- [ ] 文章标签系统
- [ ] 高级搜索功能
- [ ] 用户关注功能
- [ ] 消息通知系统
- [ ] 文件上传功能
- [ ] 数据导出功能
- [ ] 移动端适配

## 许可证

MIT License

## 联系方式

如有问题或建议，欢迎反馈。

---

**开发时间**: 2024年12月  
**版本**: v1.0.0
