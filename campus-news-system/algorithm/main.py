"""推荐服务主入口 - FastAPI应用"""
from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
import logging
import uvicorn

from config import DATABASE_CONFIG, REDIS_CONFIG, RECOMMENDATION_CONFIG, SERVICE_CONFIG
from models import (DataLoader, HybridRecommender, UserProfileAnalyzer, 
                   VideoRecommender, HotWordsAnalyzer, UserClusteringAnalyzer, TrendPredictor)

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 创建FastAPI应用
app = FastAPI(
    title="校园新闻智能推荐服务",
    description="基于混合推荐算法的文章推荐API",
    version="1.0.0"
)

# CORS配置
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 全局推荐器实例
data_loader = None
recommender = None
user_profile_analyzer = None
video_recommender = None
hot_words_analyzer = None
user_clustering = None
trend_predictor = None

# 请求/响应模型
class RecommendationItem(BaseModel):
    article_id: int
    score: float
    reason: str

class RecommendationResponse(BaseModel):
    success: bool
    data: List[RecommendationItem]
    message: str = ""

class SimilarArticleRequest(BaseModel):
    article_id: int
    top_n: int = 10

class UserRecommendRequest(BaseModel):
    user_id: Optional[int] = None
    top_n: int = 10
    exclude_ids: List[int] = []

@app.on_event("startup")
async def startup_event():
    """应用启动时初始化推荐系统"""
    global data_loader, recommender, user_profile_analyzer, video_recommender
    global hot_words_analyzer, user_clustering, trend_predictor
    
    logger.info("正在初始化推荐系统...")
    try:
        data_loader = DataLoader(DATABASE_CONFIG)
        recommender = HybridRecommender(data_loader, RECOMMENDATION_CONFIG)
        recommender.train()
        user_profile_analyzer = UserProfileAnalyzer(data_loader)
        video_recommender = VideoRecommender(data_loader, RECOMMENDATION_CONFIG)
        video_recommender.train()
        hot_words_analyzer = HotWordsAnalyzer(data_loader)
        user_clustering = UserClusteringAnalyzer(data_loader)
        user_clustering.train()
        trend_predictor = TrendPredictor(data_loader)
        trend_predictor.train()
        logger.info("推荐系统初始化完成（含视频推荐、热词分析、用户聚类、热度预测）")
    except Exception as e:
        logger.error(f"推荐系统初始化失败: {e}")

@app.get("/", tags=["健康检查"])
async def root():
    """服务健康检查"""
    return {"status": "ok", "service": "recommendation-service"}

@app.get("/health", tags=["健康检查"])
async def health_check():
    """详细健康检查"""
    return {
        "status": "healthy",
        "recommender_ready": recommender is not None,
        "last_train_time": recommender.last_train_time if recommender else 0
    }

@app.post("/api/recommend", response_model=RecommendationResponse, tags=["推荐"])
async def get_recommendations(request: UserRecommendRequest):
    """
    获取个性化推荐
    
    - user_id: 用户ID(可选，未登录用户传null)
    - top_n: 推荐数量，默认10
    - exclude_ids: 需要排除的文章ID列表
    """
    if recommender is None:
        raise HTTPException(status_code=503, detail="推荐服务未就绪")
    
    try:
        results = recommender.recommend(
            user_id=request.user_id,
            top_n=request.top_n,
            exclude_ids=request.exclude_ids
        )
        return RecommendationResponse(
            success=True,
            data=[RecommendationItem(**r) for r in results],
            message=f"成功获取{len(results)}条推荐"
        )
    except Exception as e:
        logger.error(f"推荐失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/recommend/hot", response_model=RecommendationResponse, tags=["推荐"])
async def get_hot_recommendations(
    top_n: int = Query(default=10, ge=1, le=50)
):
    """获取热门推荐(未登录用户)"""
    if recommender is None:
        raise HTTPException(status_code=503, detail="推荐服务未就绪")
    
    try:
        results = recommender.recommend(user_id=None, top_n=top_n)
        return RecommendationResponse(
            success=True,
            data=[RecommendationItem(**r) for r in results],
            message=f"成功获取{len(results)}条热门推荐"
        )
    except Exception as e:
        logger.error(f"热门推荐失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/recommend/user/{user_id}", response_model=RecommendationResponse, tags=["推荐"])
async def get_user_recommendations(
    user_id: int,
    top_n: int = Query(default=10, ge=1, le=50),
    exclude: str = Query(default="", description="排除的文章ID，逗号分隔")
):
    """
    获取指定用户的推荐(GET方式)
    """
    if recommender is None:
        raise HTTPException(status_code=503, detail="推荐服务未就绪")
    
    exclude_ids = [int(x) for x in exclude.split(",") if x.strip().isdigit()]
    
    try:
        results = recommender.recommend(
            user_id=user_id,
            top_n=top_n,
            exclude_ids=exclude_ids
        )
        return RecommendationResponse(
            success=True,
            data=[RecommendationItem(**r) for r in results],
            message=f"成功获取{len(results)}条推荐"
        )
    except Exception as e:
        logger.error(f"推荐失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/similar", response_model=RecommendationResponse, tags=["推荐"])
async def get_similar_articles(request: SimilarArticleRequest):
    """
    获取相似文章推荐
    
    - article_id: 文章ID
    - top_n: 推荐数量
    """
    if recommender is None:
        raise HTTPException(status_code=503, detail="推荐服务未就绪")
    
    try:
        results = recommender.get_similar_articles(
            article_id=request.article_id,
            top_n=request.top_n
        )
        return RecommendationResponse(
            success=True,
            data=[RecommendationItem(**r) for r in results],
            message=f"成功获取{len(results)}条相似文章"
        )
    except Exception as e:
        logger.error(f"相似文章推荐失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/similar/{article_id}", response_model=RecommendationResponse, tags=["推荐"])
async def get_similar_articles_get(
    article_id: int,
    top_n: int = Query(default=10, ge=1, le=50)
):
    """获取相似文章(GET方式)"""
    if recommender is None:
        raise HTTPException(status_code=503, detail="推荐服务未就绪")
    
    try:
        results = recommender.get_similar_articles(article_id, top_n)
        return RecommendationResponse(
            success=True,
            data=[RecommendationItem(**r) for r in results],
            message=f"成功获取{len(results)}条相似文章"
        )
    except Exception as e:
        logger.error(f"相似文章推荐失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/retrain", tags=["管理"])
async def retrain_model():
    """手动触发模型重新训练"""
    if recommender is None:
        raise HTTPException(status_code=503, detail="推荐服务未就绪")
    
    try:
        recommender.train()
        return {"success": True, "message": "模型重新训练完成"}
    except Exception as e:
        logger.error(f"模型训练失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ==================== 用户画像分析 API ====================

@app.get("/api/profile/{user_id}", tags=["用户画像"])
async def get_user_profile(user_id: int):
    """
    获取用户画像分析
    
    返回用户的完整画像信息，包括：
    - interest_tags: 兴趣标签（按权重排序）
    - category_preference: 分类偏好
    - activity_pattern: 活跃时间模式
    - behavior_stats: 行为统计
    - reading_level: 阅读深度等级
    - user_type: 用户类型标签
    """
    if user_profile_analyzer is None:
        raise HTTPException(status_code=503, detail="用户画像服务未就绪")
    
    try:
        profile = user_profile_analyzer.analyze_user(user_id)
        return {
            "success": True,
            "data": profile,
            "message": "用户画像分析完成"
        }
    except Exception as e:
        logger.error(f"用户画像分析失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/profile/{user_id}/interests", tags=["用户画像"])
async def get_user_interests(
    user_id: int,
    top_n: int = Query(default=10, ge=1, le=50)
):
    """获取用户兴趣标签"""
    if user_profile_analyzer is None:
        raise HTTPException(status_code=503, detail="用户画像服务未就绪")
    
    try:
        profile = user_profile_analyzer.analyze_user(user_id)
        interests = profile.get('interest_tags', [])[:top_n]
        return {
            "success": True,
            "data": interests,
            "message": f"获取到{len(interests)}个兴趣标签"
        }
    except Exception as e:
        logger.error(f"获取用户兴趣失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/profile/{user_id}/activity", tags=["用户画像"])
async def get_user_activity_pattern(user_id: int):
    """获取用户活跃时间模式"""
    if user_profile_analyzer is None:
        raise HTTPException(status_code=503, detail="用户画像服务未就绪")
    
    try:
        profile = user_profile_analyzer.analyze_user(user_id)
        return {
            "success": True,
            "data": profile.get('activity_pattern', {}),
            "message": "获取活跃时间模式成功"
        }
    except Exception as e:
        logger.error(f"获取活跃模式失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/profile/{user_id}/similar-users", tags=["用户画像"])
async def get_similar_users(
    user_id: int,
    top_n: int = Query(default=5, ge=1, le=20)
):
    """
    获取与指定用户兴趣相似的用户
    
    基于兴趣标签的余弦相似度计算
    """
    if user_profile_analyzer is None:
        raise HTTPException(status_code=503, detail="用户画像服务未就绪")
    
    try:
        similar_users = user_profile_analyzer.get_similar_users(user_id, top_n)
        return {
            "success": True,
            "data": similar_users,
            "message": f"找到{len(similar_users)}个相似用户"
        }
    except Exception as e:
        logger.error(f"查找相似用户失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))

# ==================== 视频推荐 API ====================

class VideoRecommendationItem(BaseModel):
    video_id: int
    score: float
    reason: str

class VideoRecommendationResponse(BaseModel):
    success: bool
    data: List[VideoRecommendationItem]
    message: str = ""

class VideoRecommendRequest(BaseModel):
    user_id: Optional[int] = None
    top_n: int = 10
    exclude_ids: List[int] = []
    category_id: Optional[int] = None


@app.post("/api/video/recommend", response_model=VideoRecommendationResponse, tags=["视频推荐"])
async def get_video_recommendations(request: VideoRecommendRequest):
    """
    获取个性化视频推荐
    
    - user_id: 用户ID(可选，未登录用户传null)
    - top_n: 推荐数量，默认10
    - exclude_ids: 需要排除的视频ID列表
    - category_id: 指定分类ID(可选)
    """
    if video_recommender is None:
        raise HTTPException(status_code=503, detail="视频推荐服务未就绪")
    
    try:
        results = video_recommender.recommend(
            user_id=request.user_id,
            top_n=request.top_n,
            exclude_ids=request.exclude_ids,
            category_id=request.category_id
        )
        return VideoRecommendationResponse(
            success=True,
            data=[VideoRecommendationItem(**r) for r in results],
            message=f"成功获取{len(results)}条视频推荐"
        )
    except Exception as e:
        logger.error(f"视频推荐失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/video/recommend/hot", response_model=VideoRecommendationResponse, tags=["视频推荐"])
async def get_hot_video_recommendations(
    top_n: int = Query(default=10, ge=1, le=50),
    category_id: Optional[int] = Query(default=None)
):
    """获取热门视频推荐(未登录用户)"""
    if video_recommender is None:
        raise HTTPException(status_code=503, detail="视频推荐服务未就绪")
    
    try:
        results = video_recommender.recommend(
            user_id=None, 
            top_n=top_n,
            category_id=category_id
        )
        return VideoRecommendationResponse(
            success=True,
            data=[VideoRecommendationItem(**r) for r in results],
            message=f"成功获取{len(results)}条热门视频推荐"
        )
    except Exception as e:
        logger.error(f"热门视频推荐失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/video/recommend/user/{user_id}", response_model=VideoRecommendationResponse, tags=["视频推荐"])
async def get_user_video_recommendations(
    user_id: int,
    top_n: int = Query(default=10, ge=1, le=50),
    exclude: str = Query(default="", description="排除的视频ID，逗号分隔"),
    category_id: Optional[int] = Query(default=None)
):
    """获取指定用户的视频推荐(GET方式)"""
    if video_recommender is None:
        raise HTTPException(status_code=503, detail="视频推荐服务未就绪")
    
    exclude_ids = [int(x) for x in exclude.split(",") if x.strip().isdigit()]
    
    try:
        results = video_recommender.recommend(
            user_id=user_id,
            top_n=top_n,
            exclude_ids=exclude_ids,
            category_id=category_id
        )
        return VideoRecommendationResponse(
            success=True,
            data=[VideoRecommendationItem(**r) for r in results],
            message=f"成功获取{len(results)}条视频推荐"
        )
    except Exception as e:
        logger.error(f"视频推荐失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/video/similar/{video_id}", response_model=VideoRecommendationResponse, tags=["视频推荐"])
async def get_similar_videos(
    video_id: int,
    top_n: int = Query(default=10, ge=1, le=50)
):
    """获取相似视频推荐"""
    if video_recommender is None:
        raise HTTPException(status_code=503, detail="视频推荐服务未就绪")
    
    try:
        results = video_recommender.get_similar_videos(video_id, top_n)
        return VideoRecommendationResponse(
            success=True,
            data=[VideoRecommendationItem(**r) for r in results],
            message=f"成功获取{len(results)}条相似视频"
        )
    except Exception as e:
        logger.error(f"相似视频推荐失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/video/retrain", tags=["管理"])
async def retrain_video_model():
    """手动触发视频推荐模型重新训练"""
    if video_recommender is None:
        raise HTTPException(status_code=503, detail="视频推荐服务未就绪")
    
    try:
        video_recommender.train()
        return {"success": True, "message": "视频推荐模型重新训练完成"}
    except Exception as e:
        logger.error(f"视频模型训练失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ==================== 热词分析 API ====================

@app.get("/api/hotwords", tags=["热词分析"])
async def get_hot_words(
    top_n: int = Query(default=30, ge=1, le=100),
    days: int = Query(default=7, ge=1, le=30)
):
    """
    获取热门关键词
    
    - top_n: 返回数量，默认30
    - days: 统计天数，默认7天
    
    返回：
    - word: 关键词
    - weight: 权重(1-100)
    - count: 出现次数
    - trend: 趋势(up/down/stable)
    - category: 主要分类
    """
    if hot_words_analyzer is None:
        raise HTTPException(status_code=503, detail="热词分析服务未就绪")
    
    try:
        words = hot_words_analyzer.get_hot_words(top_n, days)
        return {
            "success": True,
            "data": words,
            "message": f"获取到{len(words)}个热词"
        }
    except Exception as e:
        logger.error(f"获取热词失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/hotwords/trending", tags=["热词分析"])
async def get_trending_words(
    top_n: int = Query(default=10, ge=1, le=50)
):
    """获取上升趋势的热词"""
    if hot_words_analyzer is None:
        raise HTTPException(status_code=503, detail="热词分析服务未就绪")
    
    try:
        words = hot_words_analyzer.get_trending_words(top_n)
        return {
            "success": True,
            "data": words,
            "message": f"获取到{len(words)}个上升热词"
        }
    except Exception as e:
        logger.error(f"获取趋势热词失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/hotwords/category/{category}", tags=["热词分析"])
async def get_category_hot_words(
    category: str,
    top_n: int = Query(default=20, ge=1, le=50)
):
    """获取指定分类的热词"""
    if hot_words_analyzer is None:
        raise HTTPException(status_code=503, detail="热词分析服务未就绪")
    
    try:
        words = hot_words_analyzer.get_words_by_category(category, top_n)
        return {
            "success": True,
            "data": words,
            "message": f"获取到{len(words)}个{category}分类热词"
        }
    except Exception as e:
        logger.error(f"获取分类热词失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/hotwords/emerging", tags=["热词分析"])
async def get_emerging_topics(
    window_days: int = Query(default=3, ge=1, le=7),
    threshold: float = Query(default=1.5, ge=1.0, le=5.0)
):
    """
    检测新兴话题
    
    识别短期内快速增长的关键词
    - window_days: 检测窗口天数
    - threshold: 增长阈值倍数
    """
    if hot_words_analyzer is None:
        raise HTTPException(status_code=503, detail="热词分析服务未就绪")
    
    try:
        topics = hot_words_analyzer.detect_emerging_topics(window_days, threshold)
        return {
            "success": True,
            "data": topics,
            "message": f"检测到{len(topics)}个新兴话题"
        }
    except Exception as e:
        logger.error(f"检测新兴话题失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/hotwords/correlation", tags=["热词分析"])
async def get_keyword_correlation(
    top_n: int = Query(default=30, ge=10, le=50)
):
    """
    分析关键词相关性
    
    返回关键词之间的共现关系和聚类结果
    """
    if hot_words_analyzer is None:
        raise HTTPException(status_code=503, detail="热词分析服务未就绪")
    
    try:
        correlation = hot_words_analyzer.analyze_keyword_correlation(top_n)
        return {
            "success": True,
            "data": correlation,
            "message": "关键词相关性分析完成"
        }
    except Exception as e:
        logger.error(f"关键词相关性分析失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/hotwords/sentiment", tags=["热词分析"])
async def get_sentiment_distribution(
    top_n: int = Query(default=50, ge=10, le=100)
):
    """
    分析热词情感分布
    
    返回热词的积极/消极/中性情感分类
    """
    if hot_words_analyzer is None:
        raise HTTPException(status_code=503, detail="热词分析服务未就绪")
    
    try:
        sentiment = hot_words_analyzer.analyze_sentiment_distribution(top_n)
        return {
            "success": True,
            "data": sentiment,
            "message": "情感分布分析完成"
        }
    except Exception as e:
        logger.error(f"情感分布分析失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/hotwords/timeseries/{keyword}", tags=["热词分析"])
async def get_keyword_timeseries(
    keyword: str,
    days: int = Query(default=30, ge=7, le=90)
):
    """
    获取关键词时间序列数据
    
    用于绘制关键词趋势图表
    """
    if hot_words_analyzer is None:
        raise HTTPException(status_code=503, detail="热词分析服务未就绪")
    
    try:
        timeseries = hot_words_analyzer.generate_time_series(keyword, days)
        return {
            "success": True,
            "data": timeseries,
            "message": f"获取'{keyword}'时间序列数据成功"
        }
    except Exception as e:
        logger.error(f"获取时间序列失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/hotwords/compare", tags=["热词分析"])
async def compare_periods(
    period1_days: int = Query(default=7, ge=1, le=30),
    period2_days: int = Query(default=7, ge=1, le=30),
    offset_days: int = Query(default=7, ge=1, le=60),
    top_n: int = Query(default=30, ge=10, le=50)
):
    """
    对比两个时间段的热词变化
    
    - period1_days: 当前周期天数
    - period2_days: 对比周期天数
    - offset_days: 对比周期偏移天数
    """
    if hot_words_analyzer is None:
        raise HTTPException(status_code=503, detail="热词分析服务未就绪")
    
    try:
        comparison = hot_words_analyzer.compare_periods(
            period1_days, period2_days, offset_days, top_n
        )
        return {
            "success": True,
            "data": comparison,
            "message": "周期对比分析完成"
        }
    except Exception as e:
        logger.error(f"周期对比分析失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/hotwords/phrases", tags=["热词分析"])
async def get_topic_phrases(
    top_n: int = Query(default=20, ge=5, le=50)
):
    """
    提取主题短语
    
    返回2-3词组合的主题短语
    """
    if hot_words_analyzer is None:
        raise HTTPException(status_code=503, detail="热词分析服务未就绪")
    
    try:
        phrases = hot_words_analyzer.extract_topic_phrases(top_n)
        return {
            "success": True,
            "data": phrases,
            "message": f"提取到{len(phrases)}个主题短语"
        }
    except Exception as e:
        logger.error(f"提取主题短语失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/hotwords/wordcloud", tags=["热词分析"])
async def get_wordcloud_data(
    style: str = Query(default="default", description="样式: default/colorful/minimal"),
    top_n: int = Query(default=100, ge=20, le=200)
):
    """
    生成词云可视化数据
    
    返回适合前端词云组件的数据格式
    """
    if hot_words_analyzer is None:
        raise HTTPException(status_code=503, detail="热词分析服务未就绪")
    
    try:
        wordcloud = hot_words_analyzer.generate_wordcloud_data(style, top_n)
        return {
            "success": True,
            "data": wordcloud,
            "message": "词云数据生成成功"
        }
    except Exception as e:
        logger.error(f"生成词云数据失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/hotwords/comprehensive", tags=["热词分析"])
async def get_comprehensive_analysis(
    days: int = Query(default=7, ge=1, le=30),
    top_n: int = Query(default=50, ge=20, le=100)
):
    """
    获取综合热词分析报告
    
    包含多维度分析的完整报告：
    - 热词列表
    - 新兴话题
    - 关键词相关性
    - 情感分布
    - 主题短语
    - 周期对比
    - 词云数据
    """
    if hot_words_analyzer is None:
        raise HTTPException(status_code=503, detail="热词分析服务未就绪")
    
    try:
        report = hot_words_analyzer.get_comprehensive_analysis(days, top_n)
        return {
            "success": True,
            "data": report,
            "message": "综合分析报告生成成功"
        }
    except Exception as e:
        logger.error(f"生成综合分析报告失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/hotwords/dimension/{dimension}", tags=["热词分析"])
async def analyze_by_dimension(
    dimension: str,
    value: str = Query(..., description="维度值"),
    top_n: int = Query(default=20, ge=5, le=50)
):
    """
    按维度分析热词
    
    - dimension: 分析维度 (time/category/user_group)
    - value: 维度值 (如 today/week/month 或 分类名称)
    """
    if hot_words_analyzer is None:
        raise HTTPException(status_code=503, detail="热词分析服务未就绪")
    
    try:
        words = hot_words_analyzer.analyze_by_dimension(dimension, value, top_n)
        return {
            "success": True,
            "data": words,
            "message": f"按{dimension}={value}分析完成，获取{len(words)}个热词"
        }
    except Exception as e:
        logger.error(f"维度分析失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/hotwords/stats", tags=["热词分析"])
async def get_analysis_stats():
    """获取热词分析统计信息"""
    if hot_words_analyzer is None:
        raise HTTPException(status_code=503, detail="热词分析服务未就绪")
    
    try:
        stats = hot_words_analyzer.get_analysis_stats()
        return {
            "success": True,
            "data": stats,
            "message": "获取分析统计成功"
        }
    except Exception as e:
        logger.error(f"获取分析统计失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ==================== 用户聚类分析 API ====================

@app.get("/api/user/clustering/type/{user_id}", tags=["用户聚类"])
async def get_user_type(user_id: int):
    """
    获取单个用户的类型
    
    基于K-Means聚类算法，将用户分为：
    - 活跃创作者 ✍️
    - 深度阅读者 📚
    - 社交达人 💬
    - 视频爱好者 🎬
    - 潜水用户 👀
    """
    if user_clustering is None:
        raise HTTPException(status_code=503, detail="用户聚类服务未就绪")
    
    try:
        result = user_clustering.get_user_type(user_id)
        return {
            "success": True,
            "data": result,
            "message": "获取用户类型成功"
        }
    except Exception as e:
        logger.error(f"获取用户类型失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/user/clustering/all", tags=["用户聚类"])
async def get_all_user_types():
    """获取所有用户的类型"""
    if user_clustering is None:
        raise HTTPException(status_code=503, detail="用户聚类服务未就绪")
    
    try:
        results = user_clustering.get_all_user_types()
        return {
            "success": True,
            "data": results,
            "message": f"获取到{len(results)}个用户的类型"
        }
    except Exception as e:
        logger.error(f"获取用户类型失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/user/clustering/distribution", tags=["用户聚类"])
async def get_user_distribution():
    """
    获取用户类型分布统计
    
    返回各类型用户的数量和占比
    """
    if user_clustering is None:
        raise HTTPException(status_code=503, detail="用户聚类服务未就绪")
    
    try:
        result = user_clustering.get_cluster_distribution()
        return {
            "success": True,
            "data": result,
            "message": "获取用户分布成功"
        }
    except Exception as e:
        logger.error(f"获取用户分布失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ==================== 热度预测 API ====================

@app.get("/api/predict/article/{article_id}", tags=["热度预测"])
async def predict_article_trend(article_id: int):
    """
    预测文章热度趋势
    
    基于线性回归模型，预测文章未来7天和30天的浏览量
    """
    if trend_predictor is None:
        raise HTTPException(status_code=503, detail="热度预测服务未就绪")
    
    try:
        result = trend_predictor.predict_article_trend(article_id)
        return {
            "success": True,
            "data": result,
            "message": "文章热度预测成功"
        }
    except Exception as e:
        logger.error(f"文章热度预测失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/predict/video/{video_id}", tags=["热度预测"])
async def predict_video_trend(video_id: int):
    """
    预测视频热度趋势
    
    基于线性回归模型，预测视频未来7天和30天的播放量
    """
    if trend_predictor is None:
        raise HTTPException(status_code=503, detail="热度预测服务未就绪")
    
    try:
        result = trend_predictor.predict_video_trend(video_id)
        return {
            "success": True,
            "data": result,
            "message": "视频热度预测成功"
        }
    except Exception as e:
        logger.error(f"视频热度预测失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/predict/trending", tags=["热度预测"])
async def get_trending_content(
    content_type: str = Query(default="all", description="内容类型: all/article/video"),
    top_n: int = Query(default=10, ge=1, le=50)
):
    """
    获取热度上升的内容
    
    返回预测热度将上升的文章和视频
    """
    if trend_predictor is None:
        raise HTTPException(status_code=503, detail="热度预测服务未就绪")
    
    try:
        results = trend_predictor.get_trending_content(content_type, top_n)
        return {
            "success": True,
            "data": results,
            "message": f"获取到{len(results)}条上升趋势内容"
        }
    except Exception as e:
        logger.error(f"获取趋势内容失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/predict/platform-stats", tags=["热度预测"])
async def get_platform_stats():
    """
    获取平台整体趋势统计
    
    返回平台文章和视频的整体增长趋势
    """
    if trend_predictor is None:
        raise HTTPException(status_code=503, detail="热度预测服务未就绪")
    
    try:
        result = trend_predictor.get_platform_stats()
        return {
            "success": True,
            "data": result,
            "message": "获取平台统计成功"
        }
    except Exception as e:
        logger.error(f"获取平台统计失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host=SERVICE_CONFIG["host"],
        port=SERVICE_CONFIG["port"],
        reload=True
    )
