"""推荐服务主入口 - FastAPI应用"""
from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
import logging
import uvicorn

from config import DATABASE_CONFIG, REDIS_CONFIG, RECOMMENDATION_CONFIG, SERVICE_CONFIG
from models import DataLoader, HybridRecommender

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
    global data_loader, recommender
    
    logger.info("正在初始化推荐系统...")
    try:
        data_loader = DataLoader(DATABASE_CONFIG)
        recommender = HybridRecommender(data_loader, RECOMMENDATION_CONFIG)
        recommender.train()
        logger.info("推荐系统初始化完成")
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

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host=SERVICE_CONFIG["host"],
        port=SERVICE_CONFIG["port"],
        reload=True
    )
