# 🎓 WHUT Campus News System - 智能校园新闻管理系统

## 📋 项目概述

WHUT Campus News System 是一个基于 **Spring Boot + Vue 3 + Python** 开发的智能化校园新闻管理平台。系统不仅提供传统的新闻发布、浏览、互动功能，更集成了 AI 智能助手、智能推荐算法、用户画像分析等前沿技术，为用户带来全新的智能化体验。

## ✨ 核心特性

### 🤖 AI Agent 智能助手（基于 LangChain4j）
- **智能任务执行** - Agent 可自动理解意图并执行复杂任务
- **模糊搜索评论** - 只需提供关键词即可搜索文章并评论
- **批量操作** - 批量点赞、收藏、关注等自动化操作
- **数据分析** - 自动分析今日热点、趋势预测
- **内容创作** - 自动生成新闻稿并发布

### 🧠 智能推荐算法（Python 微服务）
- **协同过滤推荐** - 基于用户行为矩阵分解(SVD)，发现相似用户偏好
- **内容推荐** - 基于 TF-IDF 和余弦相似度，分析文章内容相似性
- **热门推荐** - 基于浏览量、点赞数、评论数的热度排行
- **混合推荐** - 融合多种算法，权重可配置
- **相似文章** - 智能推荐相关内容

### 👤 用户画像分析
- **兴趣标签分析** - 基于用户行为自动提取兴趣偏好
- **分类偏好统计** - 分析用户对不同新闻分类的偏好
- **活跃时间模式** - 识别用户活跃时段和活跃日
- **阅读深度等级** - 评估用户参与度（新手/轻度/普通/活跃/资深）
- **用户类型标签** - 自动标记（收藏达人、评论活跃、夜猫子等）
- **相似用户发现** - 基于兴趣的用户匹配

### 📰 新闻管理系统
- **文章发布** - 富文本编辑器，支持图片、视频
- **分类管理** - 官方新闻、全校新闻、学院新闻
- **互动功能** - 点赞、评论、收藏、分享
- **热门排行** - 浏览量、点赞数多维度排序
- **搜索过滤** - 关键词搜索、标签筛选、日期筛选

### 👥 用户系统
- **用户注册登录** - JWT Token 认证
- **个人中心** - 资料编辑、头像上传、用户画像展示
- **社交功能** - 关注/粉丝系统
- **权限管理** - 管理员、教师、学生多角色

### 🎨 UI/UX 设计
- **玻璃拟态风格** - 现代化毛玻璃特效设计
- **响应式布局** - 适配桌面端和移动端
- **流畅动画** - 平滑过渡和交互反馈

## 🛠️ 技术架构

### 前端技术栈
| 技术 | 说明 |
|------|------|
| Vue 3 | 渐进式 JavaScript 框架 |
| Element Plus | 企业级 UI 组件库 |
| Vite | 下一代前端构建工具 |
| Vue Router | 路由管理 |
| Pinia | 状态管理 |
| Axios | HTTP 请求库 |
| TailwindCSS | 原子化 CSS 框架 |

### 后端技术栈
| 技术 | 说明 |
|------|------|
| Spring Boot 3.4 | 微服务框架 |
| MyBatis Plus | ORM 框架 |
| MySQL 8.0 | 关系型数据库 |
| Redis | 缓存中间件 |
| JWT | 身份认证 |
| LangChain4j | AI Agent 框架 |

### 算法服务技术栈（Python）
| 技术 | 说明 |
|------|------|
| FastAPI | 高性能 Web 框架 |
| Scikit-learn | 机器学习库 |
| Pandas/NumPy | 数据处理 |
| Jieba | 中文分词 |
| SQLAlchemy | ORM 框架 |

## 📁 项目结构

```
campus-news-system/
├── frontend/                 # Vue 3 前端
│   ├── src/
│   │   ├── api/             # API 接口
│   │   ├── components/      # 公共组件
│   │   ├── views/           # 页面视图
│   │   ├── stores/          # Pinia 状态管理
│   │   ├── router/          # 路由配置
│   │   └── styles/          # 全局样式
│   └── package.json
│
├── backend/                  # Spring Boot 后端
│   ├── src/main/java/
│   │   └── com/campus/news/
│   │       ├── controller/  # 控制器
│   │       ├── service/     # 业务逻辑
│   │       ├── entity/      # 实体类
│   │       ├── mapper/      # MyBatis 映射
│   │       ├── config/      # 配置类
│   │       └── agent/       # AI Agent
│   └── pom.xml
│
├── algorithm/                # Python 算法服务
│   ├── main.py              # FastAPI 入口
│   ├── config.py            # 配置文件
│   ├── models/
│   │   ├── data_loader.py       # 数据加载
│   │   ├── content_based.py     # 内容推荐
│   │   ├── collaborative_filter.py  # 协同过滤
│   │   ├── hybrid_recommender.py    # 混合推荐
│   │   └── user_profile.py      # 用户画像
│   └── requirements.txt
│
└── database/                 # 数据库脚本
    └── campus_news_system.sql
```

## 🚀 快速开始

### 环境要求
- Node.js 18+
- Java 22
- Python 3.10+
- MySQL 8.0+
- Maven 3.6+

### 1. 数据库初始化

```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE campus_news_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入数据库脚本
mysql -u root -p campus_news_system < database/campus_news_system.sql
```

### 2. 后端部署

```bash
cd campus-news-system/backend

# 修改配置文件 src/main/resources/application.yml
# 配置数据库连接、Redis、AI API 等

# 安装依赖并运行
mvn clean install
mvn spring-boot:run
```

### 3. 算法服务部署

```bash
cd campus-news-system/algorithm

# 创建 conda 环境
conda create -n campus-recommendation python=3.10
conda activate campus-recommendation

# 安装依赖
pip install -r requirements.txt

# 配置环境变量
cp .env.example .env
# 编辑 .env 配置数据库连接

# 启动服务
python main.py
```

### 4. 前端部署

```bash
cd campus-news-system/frontend

# 安装依赖
npm install

# 开发环境运行
npm run dev

# 生产环境构建
npm run build
```

### 5. 访问系统

- 前端: http://localhost:5173
- 后端 API: http://localhost:8080/api
- 算法服务: http://localhost:5000/docs

## 📡 API 接口

### 推荐服务 API
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/recommend` | POST | 个性化推荐 |
| `/api/recommend/hot` | GET | 热门推荐 |
| `/api/similar/{article_id}` | GET | 相似文章 |
| `/api/profile/{user_id}` | GET | 用户画像 |
| `/api/profile/{user_id}/interests` | GET | 兴趣标签 |
| `/api/profile/{user_id}/activity` | GET | 活跃时间 |

### Agent 工具
| 工具 | 说明 |
|------|------|
| searchAndComment | 模糊搜索文章并评论 |
| batchLikeArticles | 批量点赞文章 |
| smartSearchArticles | 智能搜索文章 |
| analyzeTodayTrends | 分析今日热点 |

## 🎯 使用示例

### Agent 智能对话
```
用户: 给那个AI的文章评论666
Agent: 找到文章《AI造福计算机学生》，已成功评论

用户: 分析今日热点
Agent: 今日发布文章15篇，新增评论89条，总浏览量3520次...
```

### 推荐算法
```python
# 获取个性化推荐
POST /api/recommend
{
  "user_id": 1,
  "top_n": 10,
  "exclude_ids": []
}

# 获取用户画像
GET /api/profile/1
# 返回: 兴趣标签、分类偏好、活跃时间、阅读等级等
```

## 📊 项目亮点

1. **三端分离架构** - Java 后端 + Vue 前端 + Python 算法服务
2. **智能推荐系统** - 混合推荐算法，个性化内容分发
3. **用户画像分析** - 多维度用户行为分析
4. **AI Agent 集成** - LangChain4j 智能任务执行
5. **现代化 UI** - 玻璃拟态设计风格
6. **完整功能闭环** - 发布、浏览、互动、推荐全流程

## 📝 相关文档

- [AI 功能使用指南](./campus-news-system/AI_FEATURES_GUIDE.md)
- [API 接口文档](./campus-news-system/API_DOCS.md)
- [算法服务文档](./campus-news-system/algorithm/README.md)
- [功能特性说明](./campus-news-system/FEATURES.md)

## 📄 开源协议

本项目采用 [MIT](LICENSE) 协议开源

## 👥 开发团队

- **项目负责人**: WHUT Software Engineering Team
- **技术栈**: Full Stack Development

---

<p align="center">
  Made with ❤️ by WHUT Software Engineering Team
</p>
<p align="center">
  © 2025 WHUT Campus News System. All rights reserved.
</p>
