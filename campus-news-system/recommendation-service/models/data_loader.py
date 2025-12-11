"""数据加载模块 - 从MySQL加载用户行为和文章数据"""
import pandas as pd
from sqlalchemy import create_engine, text
from typing import Optional, List, Dict
import logging

logger = logging.getLogger(__name__)

class DataLoader:
    def __init__(self, db_config: dict):
        self.engine = create_engine(
            f"mysql+pymysql://{db_config['user']}:{db_config['password']}@"
            f"{db_config['host']}:{db_config['port']}/{db_config['database']}",
            pool_recycle=3600
        )
    
    def get_user_interactions(self) -> pd.DataFrame:
        """获取用户交互数据(浏览、点赞、收藏、评论)"""
        query = """
        SELECT user_id, article_id, 'like' as action_type, 3 as weight, created_at
        FROM article_like
        UNION ALL
        SELECT user_id, article_id, 'favorite' as action_type, 5 as weight, created_at
        FROM article_favorite
        UNION ALL
        SELECT user_id, article_id, 'comment' as action_type, 4 as weight, created_at
        FROM comment WHERE article_id IS NOT NULL
        """
        try:
            return pd.read_sql(query, self.engine)
        except Exception as e:
            logger.error(f"获取用户交互数据失败: {e}")
            return pd.DataFrame()
    
    def get_articles(self) -> pd.DataFrame:
        """获取文章数据"""
        query = """
        SELECT a.id, a.title, a.content, a.summary, a.board_type, 
               a.view_count, a.like_count, a.comment_count, a.author_id,
               a.created_at, a.status,
               GROUP_CONCAT(t.name) as tags
        FROM article a
        LEFT JOIN article_tag at ON a.id = at.article_id
        LEFT JOIN tag t ON at.tag_id = t.id
        WHERE a.status = 1 AND a.is_approved = 1
        GROUP BY a.id
        """
        try:
            return pd.read_sql(query, self.engine)
        except Exception as e:
            logger.error(f"获取文章数据失败: {e}")
            return pd.DataFrame()
    
    def get_user_profile(self, user_id: int) -> Dict:
        """获取用户画像数据"""
        query = text("""
        SELECT u.id, u.username, 
               (SELECT GROUP_CONCAT(DISTINCT t.name)
                FROM article_like al
                JOIN article a ON al.article_id = a.id
                JOIN article_tag at ON a.id = at.article_id
                JOIN tag t ON at.tag_id = t.id
                WHERE al.user_id = :user_id) as liked_tags,
               (SELECT GROUP_CONCAT(DISTINCT a.board_type)
                FROM article_like al
                JOIN article a ON al.article_id = a.id
                WHERE al.user_id = :user_id) as preferred_boards
        FROM user u WHERE u.id = :user_id
        """)
        try:
            with self.engine.connect() as conn:
                result = conn.execute(query, {"user_id": user_id}).fetchone()
                if result:
                    return {
                        "id": result[0],
                        "username": result[1],
                        "liked_tags": result[2].split(",") if result[2] else [],
                        "preferred_boards": result[3].split(",") if result[3] else []
                    }
        except Exception as e:
            logger.error(f"获取用户画像失败: {e}")
        return {}
    
    def get_user_history(self, user_id: int, limit: int = 50) -> List[int]:
        """获取用户历史交互的文章ID"""
        query = text("""
        SELECT article_id FROM (
            SELECT article_id, MAX(created_at) as latest_time FROM (
                SELECT article_id, created_at FROM article_like WHERE user_id = :user_id
                UNION ALL
                SELECT article_id, created_at FROM article_favorite WHERE user_id = :user_id
                UNION ALL
                SELECT article_id, created_at FROM comment WHERE user_id = :user_id
            ) t GROUP BY article_id
        ) grouped ORDER BY latest_time DESC LIMIT :limit
        """)
        try:
            with self.engine.connect() as conn:
                result = conn.execute(query, {"user_id": user_id, "limit": limit})
                return [row[0] for row in result]
        except Exception as e:
            logger.error(f"获取用户历史失败: {e}")
            return []
