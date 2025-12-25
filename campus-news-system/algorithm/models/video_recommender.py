"""视频推荐系统"""
import pandas as pd
import numpy as np
from typing import List, Dict, Optional
from .data_loader import DataLoader
import logging
import time

logger = logging.getLogger(__name__)


class VideoRecommender:
    """视频推荐器 - 基于热度和用户偏好的混合推荐"""
    
    def __init__(self, data_loader: DataLoader, config: dict):
        self.data_loader = data_loader
        self.config = config
        self.videos_df = None
        self.hot_videos = []
        self.last_train_time = 0
        self.train_interval = 3600  # 1小时重新训练
    
    def train(self):
        """训练视频推荐模型"""
        logger.info("开始训练视频推荐模型...")
        start_time = time.time()
        
        # 加载视频数据
        self.videos_df = self.data_loader.get_videos()
        
        if self.videos_df.empty:
            logger.warning("没有视频数据，跳过训练")
            return
        
        # 计算热门视频
        self._compute_hot_videos()
        
        self.last_train_time = time.time()
        logger.info(f"视频推荐模型训练完成，耗时: {time.time() - start_time:.2f}秒")
    
    def _compute_hot_videos(self):
        """计算热门视频排行"""
        if self.videos_df is None or self.videos_df.empty:
            return
        
        df = self.videos_df.copy()
        # 热度分数 = 播放量*1 + 点赞数*3 + 评论数*5
        df['hot_score'] = (
            df['view_count'].fillna(0) * 1 +
            df['like_count'].fillna(0) * 3 +
            df['comment_count'].fillna(0) * 5
        )
        
        # 时间衰减因子(7天内的视频权重更高)
        df['created_at'] = pd.to_datetime(df['created_at'])
        now = pd.Timestamp.now()
        df['days_ago'] = (now - df['created_at']).dt.days
        df['time_decay'] = np.exp(-df['days_ago'] / 7)
        df['final_score'] = df['hot_score'] * df['time_decay']
        
        self.hot_videos = df.nlargest(100, 'final_score')[['id', 'final_score']].values.tolist()
    
    def _should_retrain(self) -> bool:
        """检查是否需要重新训练"""
        return time.time() - self.last_train_time > self.train_interval
    
    def recommend(self, user_id: Optional[int] = None, top_n: int = 10,
                  exclude_ids: List[int] = None, category_id: Optional[int] = None) -> List[Dict]:
        """视频推荐主入口"""
        if self._should_retrain():
            self.train()
        
        exclude_ids = exclude_ids or []
        
        logger.info(f"=== 视频推荐请求 === user_id={user_id}, top_n={top_n}, category_id={category_id}")
        logger.info(f"当前视频库数量: {len(self.videos_df) if self.videos_df is not None else 0}")
        logger.info(f"热门视频数量: {len(self.hot_videos)}")
        
        if user_id is None:
            # 未登录用户，返回热门推荐
            logger.info("推荐策略: 热门推荐（未登录用户）")
            results = self._get_hot_recommendations(top_n * 2, exclude_ids, category_id)
        else:
            # 获取用户历史
            user_history = self.data_loader.get_user_video_history(user_id)
            user_profile = self.data_loader.get_user_video_profile(user_id)
            
            logger.info(f"用户历史交互数: {len(user_history)}")
            logger.info(f"用户偏好分类: {user_profile.get('liked_categories', [])}")
            
            if len(user_history) < 3:
                # 新用户，主要使用热门推荐
                logger.info("推荐策略: 冷启动推荐（新用户，交互<3次）")
                results = self._get_cold_start_recommendations(user_id, top_n * 2, exclude_ids, category_id)
            else:
                # 老用户，使用混合推荐
                logger.info("推荐策略: 混合推荐（老用户）")
                results = self._get_hybrid_recommendations(user_id, user_history, top_n * 2, exclude_ids, category_id)
        
        logger.info(f"推荐结果数量: {len(results[:top_n])}")
        for i, r in enumerate(results[:top_n]):
            logger.info(f"  [{i+1}] video_id={r['video_id']}, score={r['score']:.2f}, reason={r['reason']}")
        
        return results[:top_n]
    
    def _get_hot_recommendations(self, top_n: int, exclude_ids: List[int], 
                                  category_id: Optional[int] = None) -> List[Dict]:
        """热门视频推荐"""
        exclude_set = set(exclude_ids)
        results = []
        
        for video_id, score in self.hot_videos:
            if video_id in exclude_set:
                continue
            
            # 如果指定了分类，过滤
            if category_id is not None and self.videos_df is not None:
                video_row = self.videos_df[self.videos_df['id'] == video_id]
                if not video_row.empty and video_row.iloc[0]['category_id'] != category_id:
                    continue
            
            results.append({
                "video_id": int(video_id),
                "score": float(score),
                "reason": "热门推荐"
            })
            if len(results) >= top_n:
                break
        
        return results
    
    def _get_cold_start_recommendations(self, user_id: int, top_n: int,
                                         exclude_ids: List[int], 
                                         category_id: Optional[int] = None) -> List[Dict]:
        """冷启动推荐(新用户)"""
        # 获取用户视频偏好
        user_profile = self.data_loader.get_user_video_profile(user_id)
        
        # 70%热门 + 30%基于用户偏好
        hot_count = int(top_n * 0.7)
        pref_count = top_n - hot_count
        
        results = self._get_hot_recommendations(hot_count, exclude_ids, category_id)
        used_ids = set(exclude_ids + [r['video_id'] for r in results])
        
        # 如果有用户偏好分类，基于分类推荐
        if user_profile.get('liked_categories'):
            pref_recs = self._recommend_by_categories(
                user_profile['liked_categories'], pref_count, list(used_ids)
            )
            results.extend(pref_recs)
        
        return results[:top_n]
    
    def _recommend_by_categories(self, categories: List[str], top_n: int,
                                  exclude_ids: List[int]) -> List[Dict]:
        """基于分类推荐"""
        if self.videos_df is None:
            return []
        
        exclude_set = set(exclude_ids)
        results = []
        
        for _, row in self.videos_df.iterrows():
            if row['id'] in exclude_set:
                continue
            
            category_name = row.get('category_name', '')
            if category_name in categories:
                results.append({
                    "video_id": int(row['id']),
                    "score": float(row.get('view_count', 0)),
                    "reason": f"分类偏好: {category_name}"
                })
        
        results.sort(key=lambda x: x['score'], reverse=True)
        return results[:top_n]
    
    def _get_hybrid_recommendations(self, user_id: int, user_history: List[int],
                                     top_n: int, exclude_ids: List[int],
                                     category_id: Optional[int] = None) -> List[Dict]:
        """混合推荐(老用户)"""
        all_exclude = list(set(exclude_ids + user_history))
        
        # 获取用户偏好分类
        user_profile = self.data_loader.get_user_video_profile(user_id)
        liked_categories = user_profile.get('liked_categories', [])
        
        # 60%基于分类偏好 + 40%热门
        pref_count = int(top_n * 0.6)
        hot_count = top_n - pref_count
        
        results = []
        
        # 基于分类偏好推荐
        if liked_categories:
            pref_recs = self._recommend_by_categories(liked_categories, pref_count, all_exclude)
            results.extend(pref_recs)
        
        # 补充热门推荐
        used_ids = all_exclude + [r['video_id'] for r in results]
        hot_recs = self._get_hot_recommendations(hot_count, used_ids, category_id)
        results.extend(hot_recs)
        
        return results[:top_n]
    
    def get_similar_videos(self, video_id: int, top_n: int = 10) -> List[Dict]:
        """获取相似视频(基于同分类)"""
        if self.videos_df is None or self.videos_df.empty:
            return []
        
        # 获取当前视频的分类
        video_row = self.videos_df[self.videos_df['id'] == video_id]
        if video_row.empty:
            return []
        
        category_id = video_row.iloc[0]['category_id']
        
        # 获取同分类的其他视频
        same_category = self.videos_df[
            (self.videos_df['category_id'] == category_id) & 
            (self.videos_df['id'] != video_id)
        ].copy()
        
        if same_category.empty:
            return []
        
        # 按热度排序
        same_category['score'] = (
            same_category['view_count'].fillna(0) * 1 +
            same_category['like_count'].fillna(0) * 3 +
            same_category['comment_count'].fillna(0) * 5
        )
        
        top_videos = same_category.nlargest(top_n, 'score')
        
        return [
            {
                "video_id": int(row['id']),
                "score": float(row['score']),
                "reason": f"同分类: {row.get('category_name', '未知')}"
            }
            for _, row in top_videos.iterrows()
        ]
