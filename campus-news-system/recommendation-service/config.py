"""配置文件"""
import os
from dotenv import load_dotenv

load_dotenv()

# 数据库配置
DATABASE_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", 3306)),
    "user": os.getenv("DB_USER", "root"),
    "password": os.getenv("DB_PASSWORD", "123456"),
    "database": os.getenv("DB_NAME", "campus_news")
}

# Redis配置
REDIS_CONFIG = {
    "host": os.getenv("REDIS_HOST", "localhost"),
    "port": int(os.getenv("REDIS_PORT", 6379)),
    "db": int(os.getenv("REDIS_DB", 0)),
    "password": os.getenv("REDIS_PASSWORD", None)
}

# 推荐系统配置
RECOMMENDATION_CONFIG = {
    "default_count": 10,  # 默认推荐数量
    "cache_ttl": 3600,    # 缓存过期时间(秒)
    "min_interactions": 3, # 最小交互次数才启用协同过滤
    "content_weight": 0.4, # 内容推荐权重
    "cf_weight": 0.4,      # 协同过滤权重
    "hot_weight": 0.2      # 热门推荐权重
}

# 服务配置
SERVICE_CONFIG = {
    "host": "0.0.0.0",
    "port": 5000
}
