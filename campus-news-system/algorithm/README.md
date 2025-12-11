# 🤖 校园新闻智能推荐服务

基于混合推荐算法的文章推荐微服务，为校园新闻系统提供个性化推荐能力。

## 📋 功能特性

### 推荐算法
- **协同过滤 (Collaborative Filtering)**: 基于用户行为矩阵分解(SVD)，发现相似用户的偏好
- **内容推荐 (Content-Based)**: 基于TF-IDF和余弦相似度，分析文章内容相似性
- **热门推荐 (Popularity-Based)**: 基于浏览量、点赞数、评论数的热度排行
- **混合推荐 (Hybrid)**: 融合以上三种算法，权重可配置

### 核心功能
- ✅ 个性化文章推荐
- ✅ 相似文章推荐
- ✅ 热门文章推荐
- ✅ 冷启动处理(新用户)
- ✅ 模型自动更新
- ✅ 服务降级机制
- ✅ **用户画像分析** (NEW)

## 🚀 快速开始

### 环境要求
- Python 3.9+
- MySQL 5.7+
- Redis (可选，用于缓存)

### 安装启动

**Windows:**
```bash
cd recommendation-service
start.bat
```

**Linux/Mac:**
```bash
cd recommendation-service
chmod +x start.sh
./start.sh
```

**手动启动:**
```bash
# 创建虚拟环境
python -m venv venv

# 激活虚拟环境
# Windows: venv\Scripts\activate
# Linux/Mac: source venv/bin/activate

# 安装依赖
pip install -r requirements.txt

# 配置环境变量
cp .env.example .env
# 编辑 .env 文件配置数据库连接

# 启动服务
python main.py
```

### 访问服务
- API文档: http://localhost:5000/docs
- 健康检查: http://localhost:5000/health

## 📡 API接口

### 1. 个性化推荐
```http
POST /api/recommend
Content-Type: application/json

{
  "user_id": 1,
  "top_n": 10,
  "exclude_ids": []
}
```

### 2. 相似文章
```http
GET /api/similar/{article_id}?top_n=6
```

### 3. 热门推荐
```http
GET /api/recommend/hot?top_n=10
```

### 4. 重新训练模型
```http
POST /api/retrain
```

### 5. 用户画像分析 (NEW)
```http
# 获取完整用户画像
GET /api/profile/{user_id}

# 获取用户兴趣标签
GET /api/profile/{user_id}/interests?top_n=10

# 获取用户活跃时间模式
GET /api/profile/{user_id}/activity

# 获取相似用户
GET /api/profile/{user_id}/similar-users?top_n=5
```

**用户画像返回数据示例:**
```json
{
  "user_id": 1,
  "interest_tags": [
    {"tag": "校园活动", "weight": 1.0, "raw_score": 15.5},
    {"tag": "学术讲座", "weight": 0.8, "raw_score": 12.4}
  ],
  "category_preference": [
    {"category": "CAMPUS", "name": "全校新闻", "count": 25, "percentage": 50.0}
  ],
  "activity_pattern": {
    "peak_hours": [10, 14, 20],
    "active_days": ["周一", "周三", "周五"]
  },
  "behavior_stats": {
    "total_interactions": 50,
    "like_count": 20,
    "favorite_count": 10,
    "comment_count": 5
  },
  "reading_level": {
    "level": "活跃读者",
    "score": 65,
    "description": "积极互动，有明确的阅读偏好"
  },
  "user_type": ["活跃用户", "评论活跃"]
}
```

## ⚙️ 配置说明

编辑 `.env` 文件:

```env
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=123456
DB_NAME=campus_news_system

# 服务配置
SERVICE_HOST=0.0.0.0
SERVICE_PORT=5000
```

编辑 `config.py` 调整推荐参数:

```python
RECOMMENDATION_CONFIG = {
    "default_count": 10,    # 默认推荐数量
    "cache_ttl": 3600,      # 缓存过期时间
    "min_interactions": 3,  # 启用协同过滤的最小交互次数
    "content_weight": 0.4,  # 内容推荐权重
    "cf_weight": 0.4,       # 协同过滤权重
    "hot_weight": 0.2       # 热门推荐权重
}
```

## 🏗️ 项目结构

```
algorithm/
├── main.py              # FastAPI应用入口
├── config.py            # 配置文件
├── requirements.txt     # Python依赖
├── models/
│   ├── __init__.py
│   ├── data_loader.py       # 数据加载模块
│   ├── content_based.py     # 内容推荐算法
│   ├── collaborative_filter.py  # 协同过滤算法
│   ├── hybrid_recommender.py    # 混合推荐器
│   └── user_profile.py      # 用户画像分析 (NEW)
├── start.bat            # Windows启动脚本
├── start.sh             # Linux启动脚本
└── .env.example         # 环境变量示例
```

## 🔧 与Java后端集成

Java后端通过HTTP调用本服务:

```java
// RecommendationService.java
@Value("${recommendation.service.url:http://localhost:5000}")
private String recommendationServiceUrl;

public List<Article> getRecommendations(Long userId, int count) {
    // 调用Python推荐服务
    String url = recommendationServiceUrl + "/api/recommend";
    // ...
}
```

配置 `application.yml`:
```yaml
recommendation:
  service:
    url: http://localhost:5000
```

## 📊 算法说明

### 混合推荐流程

```
用户请求 → 判断用户类型
    │
    ├─ 未登录用户 → 热门推荐
    │
    ├─ 新用户(交互<3次) → 70%热门 + 30%标签推荐
    │
    └─ 老用户 → 混合推荐
                  ├─ 40% 内容推荐(TF-IDF)
                  ├─ 40% 协同过滤(SVD)
                  └─ 20% 热门推荐
```

### 热度计算公式
```
热度分数 = 浏览量×1 + 点赞数×3 + 评论数×5
最终分数 = 热度分数 × 时间衰减因子(7天半衰期)
```

## 🎯 性能优化

- 模型每小时自动更新
- 支持手动触发训练
- 服务不可用时自动降级到热门推荐
- 使用稀疏矩阵优化内存占用

## 📝 注意事项

1. 首次启动会自动训练模型，可能需要几秒钟
2. 数据量较小时协同过滤效果有限，会自动增加热门推荐权重
3. 建议在生产环境配置Redis缓存提升性能
