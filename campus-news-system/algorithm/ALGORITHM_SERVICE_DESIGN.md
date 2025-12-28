# 校园新闻系统 - 算法服务设计文档

## 一、系统架构概述

### 1.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端 (Vue.js)                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ HotWordCloud│  │AlgorithmDash│  │    其他组件              │  │
│  └──────┬──────┘  └──────┬──────┘  └───────────┬─────────────┘  │
└─────────┼────────────────┼─────────────────────┼────────────────┘
          │                │                     │
          ▼                ▼                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                   后端 Java (Spring Boot)                        │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │              AnalysisController (分析服务控制器)              ││
│  │  - /analysis/hotwords/*     热词分析接口                     ││
│  │  - /analysis/user/*         用户聚类接口                     ││
│  │  - /analysis/predict/*      热度预测接口                     ││
│  └──────────────────────────┬──────────────────────────────────┘│
└─────────────────────────────┼───────────────────────────────────┘
                              │ HTTP调用
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                 算法服务 Python (FastAPI)                        │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │
│  │ HotWordsAna- │ │UserClustering│ │TrendPredictor│            │
│  │    lyzer     │ │   Analyzer   │ │              │            │
│  └──────────────┘ └──────────────┘ └──────────────┘            │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │
│  │HybridRecomm- │ │VideoRecomm-  │ │UserProfile-  │            │
│  │   ender      │ │   ender      │ │  Analyzer    │            │
│  └──────────────┘ └──────────────┘ └──────────────┘            │
│  ┌─────────────────────────────────────────────────┐            │
│  │              工具层 (utils)                      │            │
│  │  CacheManager  │  PerformanceMonitor            │            │
│  └─────────────────────────────────────────────────┘            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        MySQL 数据库                              │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 服务端口配置

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 Vue | 5173 | Vite开发服务器 |
| 后端 Java | 3000 | Spring Boot服务 |
| 算法服务 Python | 5000 | FastAPI服务 |
| MySQL | 3306 | 数据库服务 |

---

## 二、算法模块详细设计

### 2.1 热词分析模块 (HotWordsAnalyzer)

#### 2.1.1 核心算法

**TF-IDF (词频-逆文档频率)**
```
TF-IDF(t,d) = TF(t,d) × IDF(t)

其中：
- TF(t,d) = 词t在文档d中出现的次数 / 文档d的总词数
- IDF(t) = log(文档总数 / 包含词t的文档数 + 1)
```

**算法流程：**
```
1. 数据收集
   ├── 获取文章标题和内容
   ├── 获取视频标题和描述
   └── 合并文本数据

2. 文本预处理
   ├── HTML标签清理
   ├── jieba中文分词
   └── 停用词过滤

3. TF-IDF关键词提取
   ├── 构建TF-IDF向量化器
   ├── 计算词频矩阵
   └── 提取高权重关键词

4. 传统方法补充
   ├── jieba.analyse提取关键词
   └── 基于浏览量加权

5. 结果融合
   ├── TF-IDF结果权重70%
   ├── 传统方法权重30%
   └── 综合排序输出
```

#### 2.1.2 API接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/hotwords` | GET | 获取热门关键词 |
| `/api/hotwords/trending` | GET | 获取上升趋势热词 |
| `/api/hotwords/emerging` | GET | 检测新兴话题 |
| `/api/hotwords/sentiment` | GET | 热词情感分析 |
| `/api/hotwords/correlation` | GET | 关键词相关性分析 |

#### 2.1.3 答辩要点

**Q: 为什么使用TF-IDF而不是简单词频统计？**
> A: TF-IDF考虑了词在整个语料库中的分布，能够降低常见词的权重，突出真正有区分度的关键词。

**Q: 如何处理中文分词？**
> A: 使用jieba分词库，并添加了领域专用词典（校园、科技、教育等），提高分词准确性。

---

### 2.2 用户聚类模块 (UserClusteringAnalyzer)

#### 2.2.1 核心算法

**K-Means聚类算法**
```
目标函数：最小化簇内平方和
J = Σ Σ ||x - μk||²

算法步骤：
1. 随机初始化K个聚类中心
2. 将每个样本分配到最近的聚类中心
3. 重新计算每个簇的中心
4. 重复步骤2-3直到收敛
```

**特征工程：**
```python
特征维度（17维）：
├── 基础统计特征
│   ├── article_count      # 发布文章数
│   ├── video_count        # 发布视频数
│   ├── comment_count      # 评论数
│   ├── like_count         # 点赞数
│   ├── favorite_count     # 收藏数
│   └── view_count         # 浏览数
│
├── 行为特征
│   ├── avg_read_time      # 平均阅读时长
│   ├── activity_score     # 活跃度评分
│   ├── content_diversity  # 内容多样性
│   └── social_score       # 社交评分
│
└── 高级特征
    ├── device_diversity           # 设备多样性
    ├── active_hours_pattern       # 活跃时段模式
    ├── reading_depth              # 阅读深度
    ├── content_preference_strength # 内容偏好强度
    ├── interaction_quality        # 互动质量
    ├── temporal_consistency       # 时间一致性
    └── influence_score            # 影响力评分
```

**聚类数优化（多指标综合评估）：**
```
综合评分 = 0.3×轮廓系数 + 0.2×Calinski-Harabasz + 
           0.2×(1-Davies-Bouldin) + 0.2×稳定性 + 0.1×肘部法则
```

#### 2.2.2 用户类型定义

| 类型ID | 名称 | 图标 | 特征描述 |
|--------|------|------|----------|
| 0 | 活跃创作者 | ✍️ | 高活跃度、高创作量、高影响力 |
| 1 | 深度阅读者 | 📚 | 高阅读深度、偏好长文、内容多样 |
| 2 | 社交达人 | � || 高社交评分、互动频繁、评论多 |
| 3 | 视频爱好者 | 🎬 | 偏好视频内容、视频创作多 |
| 4 | 潜水用户 | 👀 | 低活跃度、以浏览为主 |

#### 2.2.3 答辩要点

**Q: 为什么选择K-Means算法？**
> A: K-Means算法简单高效，适合处理大规模数据，且聚类结果易于解释。

**Q: 如何确定最优聚类数K？**
> A: 使用多指标综合评估：轮廓系数、Calinski-Harabasz指数、Davies-Bouldin指数、稳定性评分和肘部法则，加权计算综合得分选择最优K。

**Q: 什么是轮廓系数？**
> A: 轮廓系数衡量样本与自身簇的相似度与最近其他簇的相似度之差，范围[-1,1]，越接近1表示聚类效果越好。

---

### 2.3 热度预测模块 (TrendPredictor)

#### 2.3.1 核心算法

**多模型集成预测**
```
使用的模型：
├── LinearRegression    # 线性回归（基准模型）
├── Ridge              # 岭回归（L2正则化）
├── Lasso              # Lasso回归（L1正则化）
├── ElasticNet         # 弹性网络（L1+L2）
├── RandomForest       # 随机森林（集成学习）
└── GradientBoosting   # 梯度提升（集成学习）

模型权重：
- linear: 0.10
- ridge: 0.15
- lasso: 0.10
- elastic: 0.10
- forest: 0.30  # 主力模型
- gbdt: 0.25    # 主力模型
```

**特征工程：**
```python
文章特征：
├── title_length       # 标题长度
├── content_length     # 内容长度
├── has_image          # 是否有图片
├── hour_of_day        # 发布小时
├── day_of_week        # 星期几
├── is_weekend         # 是否周末
├── author_article_count # 作者文章数
├── author_avg_views   # 作者平均浏览量
├── category_popularity # 分类热度
├── tag_count          # 标签数量
└── days_since_publish # 发布天数

视频特征：
├── title_length       # 标题长度
├── has_thumbnail      # 是否有封面
├── hour_of_day        # 上传小时
├── day_of_week        # 星期几
├── is_weekend         # 是否周末
├── duration_minutes   # 视频时长
├── author_video_count # 作者视频数
├── author_avg_views   # 作者平均播放量
├── category_popularity # 分类热度
└── days_since_publish # 发布天数
```

#### 2.3.2 预测流程

```
1. 特征提取
   ├── 从数据库获取内容数据
   ├── 提取多维度特征
   └── 特征标准化

2. 多项式特征扩展
   ├── 生成2阶多项式特征
   └── 捕获特征间交互关系

3. 多模型训练
   ├── 训练6种回归模型
   ├── 交叉验证评估性能
   └── 记录各模型指标

4. 集成预测
   ├── 各模型独立预测
   ├── 加权融合结果
   └── 输出最终预测值
```

#### 2.3.3 答辩要点

**Q: 为什么使用多模型集成？**
> A: 不同模型有不同优势，线性模型可解释性强，树模型能捕获非线性关系。集成可以综合各模型优点，提高预测准确性和鲁棒性。

**Q: 什么是岭回归？**
> A: 岭回归在普通线性回归基础上加入L2正则化项（λΣβ²），防止模型过拟合，特别适合特征间存在多重共线性的情况。

**Q: 随机森林的优势是什么？**
> A: 随机森林通过构建多棵决策树并取平均，能处理非线性关系，对异常值不敏感，还可以评估特征重要性。

---

### 2.4 视频推荐模块 (VideoRecommender)

#### 2.4.1 核心算法

**Wilson Score（威尔逊评分）**
```
公式：
Wilson = (p + z²/2n - z×√(p(1-p)/n + z²/4n²)) / (1 + z²/n)

其中：
- p = 正向互动率（点赞+评论×2）/ 播放量
- n = 播放量（样本量）
- z = 1.96（95%置信度）

优势：考虑样本量，避免少量高赞视频排名过高
```

**混合推荐策略：**
```
个性化推荐权重分配：
├── 内容相似度推荐: 40%
│   └── 基于TF-IDF的视频相似度
├── 分类偏好推荐: 30%
│   └── 用户历史观看分类
└── 热门补充: 30%
    └── Wilson Score排序

冷启动策略：
├── 热门推荐: 70%
└── 分类偏好: 30%
```

#### 2.4.2 答辩要点

**Q: 什么是Wilson Score？**
> A: Wilson Score是一种考虑样本量的评分算法，使用置信区间下界作为评分。它能避免"5个人评价全是好评"比"1000人评价95%好评"排名更高的问题。

**Q: 如何处理冷启动问题？**
> A: 对于新用户（历史交互<3次），采用70%热门推荐+30%分类偏好的混合策略；对于新视频，通过时间衰减因子给予新内容更高曝光机会。

---

### 2.5 文章推荐模块 (HybridRecommender)

#### 2.5.1 混合推荐架构

```
┌─────────────────────────────────────────────────────┐
│                  HybridRecommender                   │
├─────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │
│  │ContentBased │  │Collaborative│  │  Popularity │ │
│  │  Filtering  │  │  Filtering  │  │   Based     │ │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘ │
│         │                │                │        │
│         └────────────────┼────────────────┘        │
│                          ▼                         │
│              ┌─────────────────────┐               │
│              │   加权融合策略      │               │
│              │ CB:40% CF:35% PB:25%│               │
│              └─────────────────────┘               │
└─────────────────────────────────────────────────────┘
```

#### 2.5.2 答辩要点

**Q: 为什么使用混合推荐？**
> A: 单一推荐算法各有局限：协同过滤有冷启动问题，内容推荐缺乏多样性。混合推荐结合多种算法优势，提高推荐质量和覆盖率。

---

### 2.6 协同过滤模块 (CollaborativeFilterRecommender)

#### 2.6.1 核心算法

**SVD矩阵分解（奇异值分解）**
```
用户-物品评分矩阵分解：
R ≈ U × Σ × V^T

其中：
- R: 用户-物品评分矩阵 (m×n)
- U: 用户隐因子矩阵 (m×k)
- Σ: 奇异值对角矩阵 (k×k)
- V: 物品隐因子矩阵 (n×k)
- k: 隐因子维度 (默认50)
```

**算法流程：**
```
1. 数据准备
   ├── 收集用户交互数据（点赞、收藏、评论）
   ├── 构建用户-物品交互矩阵
   └── 稀疏矩阵表示 (CSR格式)

2. 矩阵分解
   ├── TruncatedSVD降维
   ├── 提取用户隐因子
   └── 提取物品隐因子

3. 评分预测
   ├── 计算用户-物品隐因子内积
   └── 预测用户对未交互物品的评分

4. 推荐生成
   ├── 排除已交互物品
   └── 按预测评分排序输出
```

**交互权重设计：**
| 行为类型 | 权重 | 说明 |
|----------|------|------|
| 点赞 | 3 | 明确的正向反馈 |
| 评论 | 4 | 深度参与 |
| 收藏 | 5 | 最强正向信号 |

#### 2.6.2 答辩要点

**Q: 什么是协同过滤？**
> A: 协同过滤是基于"相似用户有相似偏好"的假设，通过分析用户群体的行为数据来预测个体用户的偏好。

**Q: 为什么使用SVD分解？**
> A: SVD可以将高维稀疏的用户-物品矩阵压缩到低维稠密空间，既能降低计算复杂度，又能发现用户和物品的潜在特征。

**Q: 协同过滤的优缺点？**
> A: 优点是不需要物品内容信息，能发现用户潜在兴趣；缺点是存在冷启动问题，新用户/新物品无法推荐。

---

### 2.7 基于内容的推荐模块 (ContentBasedRecommender)

#### 2.7.1 核心算法

**TF-IDF + 余弦相似度**
```
文章相似度计算：
similarity(A, B) = cos(θ) = (A · B) / (||A|| × ||B||)

其中：
- A, B: 文章的TF-IDF向量
- 范围: [0, 1]，越接近1越相似
```

**特征构建：**
```python
文章特征文本 = 标题 + 摘要 + 标签 + 分类

处理流程：
├── jieba中文分词
├── 停用词过滤
├── TF-IDF向量化 (max_features=5000)
└── 构建文章特征矩阵
```

#### 2.7.2 答辩要点

**Q: 基于内容推荐的原理？**
> A: 分析物品的内容特征，找出与用户历史喜欢物品相似的其他物品进行推荐。

**Q: 与协同过滤的区别？**
> A: 内容推荐基于物品特征相似度，不需要其他用户数据，能解决新物品冷启动；协同过滤基于用户行为相似度，能发现跨领域兴趣。

---

### 2.8 用户画像模块 (UserProfileAnalyzer)

#### 2.8.1 画像维度

```python
用户画像结构：
├── basic_info          # 基本信息（用户名、学院等）
├── interest_tags       # 兴趣标签（权重排序）
├── category_preference # 分类偏好（各分类占比）
├── activity_pattern    # 活跃时间模式（小时分布）
├── behavior_stats      # 行为统计（点赞/收藏/评论数）
├── reading_level       # 阅读深度等级
└── user_type           # 用户类型标签
```

**兴趣标签提取：**
```
1. 收集用户交互的所有文章
2. 提取文章标签和关键词
3. 按交互权重加权统计
4. 输出Top-N兴趣标签
```

#### 2.8.2 答辩要点

**Q: 用户画像有什么用？**
> A: 用户画像用于个性化推荐、精准营销、用户分群分析，帮助理解用户需求和行为特征。

**Q: 如何构建用户兴趣标签？**
> A: 通过分析用户的点赞、收藏、评论等交互行为，提取交互文章的标签和关键词，按交互权重加权统计得到用户兴趣标签。

---

### 2.9 数据加载模块 (DataLoader)

#### 2.9.1 功能说明

```python
数据加载接口：
├── get_user_interactions()  # 获取用户交互数据
├── get_articles()           # 获取文章数据
├── get_videos()             # 获取视频数据
├── get_users()              # 获取用户数据
├── get_user_profile()       # 获取单用户画像数据
└── get_user_stats()         # 获取用户统计数据
```

**数据库连接：**
- 使用SQLAlchemy ORM
- 连接池管理 (pool_recycle=3600)
- 异常处理和日志记录

#### 2.9.2 答辩要点

**Q: 为什么使用SQLAlchemy？**
> A: SQLAlchemy提供ORM和连接池管理，简化数据库操作，支持多种数据库，便于维护和扩展。

---

## 三、工具层设计

### 3.1 缓存管理器 (CacheManager)

```python
功能特性：
├── 内存缓存 - 基于字典的快速缓存
├── TTL管理 - 支持过期时间设置
├── LRU淘汰 - 基于访问时间的缓存淘汰
├── 线程安全 - 使用RLock保证并发安全
└── 统计功能 - 命中率、大小等统计

使用方式：
@cached(ttl=1800, prefix="hot_words")
def get_hot_words(self, top_n: int = 50):
    ...
```

### 3.2 性能监控器 (PerformanceMonitor)

```python
功能特性：
├── 执行时间记录 - 记录函数执行耗时
├── 成功率统计 - 统计函数调用成功率
├── 慢查询检测 - 记录超过阈值的慢查询
├── 百分位数统计 - P50/P90/P95/P99
└── 错误追踪 - 记录最近的错误信息

使用方式：
@monitor_performance("user_clustering.train")
def train(self):
    ...
```

---

## 四、API接口汇总

### 4.1 热词分析接口

| 接口路径 | 方法 | 参数 | 说明 |
|----------|------|------|------|
| `/api/hotwords` | GET | top_n, days | 获取热门关键词 |
| `/api/hotwords/trending` | GET | top_n | 获取上升趋势热词 |
| `/api/hotwords/emerging` | GET | window_days, threshold | 检测新兴话题 |
| `/api/hotwords/sentiment` | GET | top_n | 热词情感分析 |
| `/api/hotwords/correlation` | GET | top_n | 关键词相关性 |
| `/api/hotwords/wordcloud` | GET | style, top_n | 词云数据 |

### 4.2 用户聚类接口

| 接口路径 | 方法 | 参数 | 说明 |
|----------|------|------|------|
| `/api/user/clustering/type/{user_id}` | GET | - | 获取用户类型 |
| `/api/user/clustering/all` | GET | - | 获取所有用户类型 |
| `/api/user/clustering/distribution` | GET | - | 用户类型分布 |

### 4.3 热度预测接口

| 接口路径 | 方法 | 参数 | 说明 |
|----------|------|------|------|
| `/api/predict/article/{id}` | GET | - | 预测文章热度 |
| `/api/predict/video/{id}` | GET | - | 预测视频热度 |
| `/api/predict/trending` | GET | type, top_n | 热度上升内容 |
| `/api/predict/platform-stats` | GET | - | 平台趋势统计 |

### 4.4 推荐接口

| 接口路径 | 方法 | 参数 | 说明 |
|----------|------|------|------|
| `/api/recommend` | POST | user_id, top_n | 个性化文章推荐 |
| `/api/recommend/hot` | GET | top_n | 热门文章推荐 |
| `/api/similar/{article_id}` | GET | top_n | 相似文章推荐 |
| `/api/video/recommend` | POST | user_id, top_n | 个性化视频推荐 |
| `/api/video/similar/{video_id}` | GET | top_n | 相似视频推荐 |

---

## 五、服务调用流程

### 5.1 热词分析调用流程

```
前端 HotWordCloud.vue
    │
    │ GET /api/analysis/hotwords
    ▼
后端 AnalysisController.java
    │
    │ RestTemplate.getForEntity()
    ▼
算法服务 main.py
    │
    │ hot_words_analyzer.get_hot_words()
    ▼
HotWordsAnalyzer.get_hot_words()
    │
    ├── _collect_texts()        # 收集文本
    ├── _extract_keywords_tfidf() # TF-IDF提取
    ├── _extract_keywords_traditional() # 传统方法
    ├── _combine_keyword_results() # 结果融合
    ├── _calculate_trends()     # 计算趋势
    └── _categorize_keywords()  # 分类标注
    │
    ▼
返回热词列表 [{word, weight, count, trend, category}, ...]
```

### 5.2 用户聚类调用流程

```
前端 AlgorithmDashboard.vue
    │
    │ GET /api/analysis/user/distribution
    ▼
后端 AnalysisController.java
    │
    │ RestTemplate.getForEntity()
    ▼
算法服务 main.py
    │
    │ user_clustering.get_cluster_distribution()
    ▼
UserClusteringAnalyzer
    │
    ├── _extract_user_features()  # 提取17维特征
    ├── _evaluate_and_select_features() # 特征选择
    ├── _apply_feature_weights_and_scaling() # 特征加权
    ├── _find_optimal_clusters()  # 最优K选择
    ├── KMeans.fit_predict()      # K-Means聚类
    └── _analyze_clusters()       # 智能类型映射
    │
    ▼
返回分布统计 {total, distribution: [{type, name, count, percentage}, ...]}
```

### 5.3 热度预测调用流程

```
前端 AlgorithmDashboard.vue
    │
    │ GET /api/analysis/predict/trending
    ▼
后端 AnalysisController.java
    │
    │ RestTemplate.getForEntity()
    ▼
算法服务 main.py
    │
    │ trend_predictor.get_trending_content()
    ▼
TrendPredictor
    │
    ├── _extract_article_features() # 提取文章特征
    ├── _extract_video_features()   # 提取视频特征
    ├── 多模型预测
    │   ├── LinearRegression
    │   ├── Ridge
    │   ├── RandomForest
    │   └── GradientBoosting
    └── _calculate_growth_rate()    # 计算增长率
    │
    ▼
返回趋势内容 [{type, id, title, current_views, predicted_views_7d, growth_rate}, ...]
```

---

## 六、性能优化策略

### 6.1 缓存策略

| 模块 | 缓存时间 | 说明 |
|------|----------|------|
| 热词分析 | 30分钟 | 热词变化相对缓慢 |
| 用户聚类 | 1小时 | 用户类型相对稳定 |
| 热度预测 | 30分钟 | 预测结果定期更新 |
| 推荐结果 | 10分钟 | 保证推荐新鲜度 |

### 6.2 模型训练策略

- 定时训练：每小时自动重新训练
- 增量更新：新数据触发增量训练
- 异步训练：不阻塞API响应

### 6.3 降级策略

当算法服务不可用时，后端Java服务提供本地降级方案：
- 热词分析：从文章/视频标题提取关键词
- 用户分布：返回模拟的典型分布数据
- 热度预测：基于当前浏览量简单预测

---

## 七、部署和启动

### 7.1 启动顺序

```bash
1. 启动MySQL数据库
2. 启动算法服务 (Python)
   cd algorithm
   python main.py
   
3. 启动后端服务 (Java)
   cd backend
   mvn spring-boot:run
   
4. 启动前端服务 (Vue)
   cd frontend
   npm run dev
```

### 7.2 环境要求

**Python环境：**
- Python 3.8+
- scikit-learn
- pandas, numpy
- jieba
- fastapi, uvicorn

**Java环境：**
- JDK 17+
- Maven 3.6+
- Spring Boot 3.x

---

## 八、答辩常见问题汇总

### 8.1 算法相关

1. **Q: 项目中使用了哪些机器学习算法？**
   > A: K-Means聚类（用户分类）、TF-IDF（热词提取）、多元线性回归和随机森林（热度预测）、协同过滤和内容推荐（推荐系统）。

2. **Q: 如何评估聚类效果？**
   > A: 使用轮廓系数（Silhouette Score），范围[-1,1]，越接近1表示聚类效果越好。当前系统聚类质量约0.3，属于中等水平。

3. **Q: 推荐系统如何解决冷启动问题？**
   > A: 对新用户采用热门推荐+分类偏好的混合策略；对新内容通过时间衰减因子给予更高曝光。

### 8.2 架构相关

1. **Q: 为什么算法服务独立部署？**
   > A: 算法计算密集，独立部署可以：1）独立扩展；2）使用Python生态的ML库；3）不影响主服务稳定性。

2. **Q: 服务之间如何通信？**
   > A: 后端Java服务通过HTTP REST API调用算法服务，使用RestTemplate进行同步调用。

3. **Q: 如何保证服务高可用？**
   > A: 1）缓存机制减少重复计算；2）降级策略保证基本功能；3）性能监控及时发现问题。

### 8.3 推荐系统相关

1. **Q: 什么是协同过滤？有哪些类型？**
   > A: 协同过滤是基于用户群体行为的推荐方法。分为：1）基于用户的CF（找相似用户）；2）基于物品的CF（找相似物品）；3）基于模型的CF（矩阵分解）。本系统使用SVD矩阵分解。

2. **Q: 什么是SVD矩阵分解？**
   > A: SVD（奇异值分解）将用户-物品评分矩阵分解为用户隐因子矩阵和物品隐因子矩阵的乘积，通过隐因子向量的内积预测评分。

3. **Q: 基于内容的推荐和协同过滤有什么区别？**
   > A: 内容推荐分析物品特征找相似物品，不需要其他用户数据，能解决新物品冷启动；协同过滤分析用户行为找相似用户/物品，能发现跨领域兴趣，但有冷启动问题。

4. **Q: 为什么使用混合推荐？**
   > A: 单一算法各有局限，混合推荐结合多种算法优势：内容推荐解决新物品冷启动，协同过滤发现潜在兴趣，热门推荐保证基本质量。

5. **Q: 什么是Wilson Score？为什么用它？**
   > A: Wilson Score是考虑样本量的评分算法，使用置信区间下界。它能避免"5人全好评"比"1000人95%好评"排名更高的问题，更公平地评估内容质量。

### 8.4 数据处理相关

1. **Q: 如何处理中文文本？**
   > A: 使用jieba分词库进行中文分词，添加领域专用词典，过滤停用词，然后用TF-IDF向量化。

2. **Q: 什么是TF-IDF？**
   > A: TF-IDF = 词频(TF) × 逆文档频率(IDF)。TF衡量词在文档中的重要性，IDF降低常见词权重。能有效提取文档关键词。

3. **Q: 用户画像包含哪些维度？**
   > A: 包括：兴趣标签、分类偏好、活跃时间模式、行为统计（点赞/收藏/评论）、阅读深度等级、用户类型标签。

### 8.5 性能优化相关

1. **Q: 如何优化算法服务性能？**
   > A: 1）多级缓存（内存缓存+TTL）；2）异步训练不阻塞请求；3）模型预加载；4）稀疏矩阵优化存储。

2. **Q: 缓存策略是怎样的？**
   > A: 热词30分钟、用户聚类1小时、热度预测30分钟、推荐结果10分钟。根据数据变化频率设置不同TTL。

---

## 九、算法模块文件清单

| 文件名 | 模块 | 核心算法 |
|--------|------|----------|
| hot_words.py | 热词分析 | TF-IDF |
| user_clustering.py | 用户聚类 | K-Means |
| trend_predictor.py | 热度预测 | 线性回归、随机森林、GBDT |
| video_recommender.py | 视频推荐 | Wilson Score、TF-IDF |
| hybrid_recommender.py | 混合推荐 | 加权融合 |
| collaborative_filter.py | 协同过滤 | SVD矩阵分解 |
| content_based.py | 内容推荐 | TF-IDF、余弦相似度 |
| user_profile.py | 用户画像 | 特征统计 |
| data_loader.py | 数据加载 | SQLAlchemy |

---

*文档版本: 1.1*
*更新日期: 2025-12-27*
