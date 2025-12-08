# 🎓 WHUT Campus News System - 智能校园新闻管理系统
## 📋 项目概述

WHUT Campus News System 是一个基于 Spring Boot + Vue 3 开发的智能化校园新闻管理平台。系统不仅提供传统的新闻发布、浏览、互动功能，更集成了最前沿的 AI 技术，包括智能对话助手、Agent 自动化执行、智能摘要生成、情感分析等功能，为用户带来全新的智能化体验。

## ✨ 核心特性

### 🤖 AI Agent 智能助手（基于 LangChain4j）
- **智能任务执行** - Agent 可自动理解意图并执行复杂任务
- **模糊搜索评论** - 只需提供关键词即可搜索文章并评论
- **批量操作** - 批量点赞、收藏、关注等自动化操作
- **数据分析** - 自动分析今日热点、趋势预测
- **社交互动** - 智能关注用户、查看关注列表
- **内容创作** - 自动生成新闻稿并发布

### 💬 AI 对话助手
- **多模型支持** - Kimi、DeepSeek、豆包等多种 LLM
- **流式对话** - 打字机效果，实时响应
- **上下文记忆** - 保持对话连贯性
- **历史记录** - 对话和 Agent 任务分开管理

### 📰 新闻管理系统
- **文章发布** - 富文本编辑器，支持图片、视频
- **分类管理** - 官方、校园、学院多级分类
- **互动功能** - 点赞、评论、收藏、分享
- **热门排行** - 浏览量、点赞数多维度排序
- **搜索过滤** - 关键词搜索、标签筛选

### 👥 用户系统
- **用户注册登录** - JWT Token 认证
- **个人中心** - 资料编辑、头像上传
- **社交功能** - 关注/粉丝系统
- **权限管理** - 管理员、编辑、普通用户多角色
- **消息通知** - 实时推送系统通知

### 📊 数据统计
- **实时数据** - 今日文章、评论、浏览量
- **趋势分析** - 热门话题、用户活跃度
- **可视化图表** - ECharts 数据展示

## 🛠️ 技术架构

### 前端技术栈
```
├── Vue 3 - 渐进式 JavaScript 框架
├── Element Plus - 企业级 UI 组件库
├── Vue Router - 路由管理
├── Pinia - 状态管理
├── Axios - HTTP 请求库
├── ECharts - 数据可视化
└── Markdown-it - Markdown 渲染
```

### 后端技术栈
```
├── Spring Boot 2.7+ - 微服务框架
├── MyBatis Plus - ORM 框架
├── MySQL 8.0 - 关系型数据库
├── Redis - 缓存中间件
├── JWT - 身份认证
├── LangChain4j - AI Agent 框架
├── OpenAI API - LLM 接口
└── SSE - 服务器推送事件
```

### AI 技术集成
```
├── LangChain4j 0.34.0 - Agent 框架
├── OpenAI Compatible API - LLM 接口
├── NLP - 自然语言处理
├── 文本摘要算法 - 智能压缩
├── 情感分析引擎 - 情绪识别
└── 智能推荐算法 - 个性化推荐
```

## 🚀 快速开始

### 环境要求
- Node.js 16+
- Java 11+
- MySQL 8.0+
- Maven 3.6+

### 前端部署

```bash
# 克隆项目
git clone https://github.com/YWHUTER/softwareTraining.git

# 进入前端目录
cd campus-news-system/frontend

# 安装依赖
npm install

# 开发环境运行
npm run dev

# 生产环境构建
npm run build
```

### 后端部署

```bash
# 进入后端目录
cd campus-news-system/backend

# 安装依赖
mvn clean install

# 修改配置文件
# 编辑 src/main/resources/application.yml
# 配置数据库、Redis、AI API等

# 运行项目
mvn spring-boot:run
```

### 数据库初始化

```sql
# 创建数据库
CREATE DATABASE campus_news CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入数据库脚本
mysql -u root -p campus_news < database/campus_news.sql
```

## 📡 Agent 工具 API

### 搜索并评论
```java
@Tool("根据关键词模糊搜索文章并发表评论")
public String searchAndComment(String keywords, String comment)
```

### 批量点赞
```java
@Tool("批量给包含关键词的文章点赞")
public String batchLikeArticles(String keyword, int count)
```

### 智能搜索
```java
@Tool("根据模糊描述智能搜索文章")
public String smartSearchArticles(String vague, int limit)
```

### 数据分析
```java
@Tool("分析今日热点新闻和趋势")
public String analyzeTodayTrends()
```

## 🎯 Agent 使用示例

### 1. 模糊搜索评论
```
用户: 给那个AI的文章评论666
Agent: 找到文章《AI造福计算机学生》，已成功评论
```

### 2. 批量操作
```
用户: 给所有计算机相关的文章点赞
Agent: 找到5篇相关文章，成功点赞3篇（2篇已点赞过）
```

### 3. 智能分析
```
用户: 分析今日热点
Agent: 今日发布文章15篇，新增评论89条，总浏览量3520次...
```

## 📸 系统截图

### AI 对话界面
- 支持普通对话和 Agent 模式切换
- 左侧显示历史记录和快捷功能
- Agent 模式显示执行步骤

### 新闻浏览页面
- 卡片式布局
- 支持图片预览
- 显示互动数据

### 个人中心
- 用户资料管理
- 我的文章/收藏/关注
- 数据统计展示

## 📊 项目亮点

1. **技术创新** 
   - 集成 LangChain4j 框架，实现智能 Agent
   - 多模型 LLM 支持，提供多样化选择
   - SSE 流式输出，优化用户体验

2. **功能完整**
   - 覆盖新闻管理全流程
   - 社交功能丰富
   - 数据分析全面

3. **用户体验**
   - 响应式设计，支持移动端
   - 界面美观现代
   - 操作简单直观

4. **可扩展性**
   - 模块化设计
   - 插件式 AI 功能
   - 标准 RESTful API

## 📝 开发文档

- [AI 功能使用指南](./AI_FEATURES_GUIDE.md)
- [API 接口文档](./docs/API.md)
- [部署指南](./docs/DEPLOYMENT.md)
- [开发规范](./docs/DEVELOPMENT.md)

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 开源协议

本项目采用 [MIT](LICENSE) 协议开源

## 👥 开发团队

- **项目负责人**: WHUT Software Engineering Team
- **技术栈**: Full Stack Development
- **联系方式**: whut-campus-news@example.com

## 🙏 致谢

- 感谢 [LangChain4j](https://github.com/langchain4j/langchain4j) 提供优秀的 Agent 框架
- 感谢 [Element Plus](https://element-plus.org/) 提供美观的 UI 组件
- 感谢 [Spring Boot](https://spring.io/projects/spring-boot) 提供强大的后端框架

---

<p align="center">
  Made with ❤️ by WHUT Software Engineering Team
</p>
<p align="center">
  © 2024 WHUT Campus News System. All rights reserved.
</p>
