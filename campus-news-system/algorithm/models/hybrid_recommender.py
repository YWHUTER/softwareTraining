"""混合推荐系统 - 融合多种推荐算法"""
import pandas as pd
import numpy as np
from typing import List, Dict, Tuple, Optional
from .content_based import ContentBasedRecommender
from .collaborative_filter import CollaborativeFilterRecommender
from .data_loader import DataLoader
import logging
import time

logger = logging.getLogger(__name__)

class HybridRecommender:
    def __init__(self, data_loader: DataLoader, config: dict):
        self.data_loader = data_loader
        self.config = config
        self.content_recommender = ContentBasedRecommender()
        self.cf_recommender = CollaborativeFilterRecommender()
        self.articles_df = None
        self.hot_articles = []
        self.last_train_time = 0
        self.train_interval = 3600  # 1小时重新训练一次
    
    def train(self):
        """训练所有推荐模型"""
        logger.info("开始训练推荐模型...")
        start_time = time.time()
        
        # 加载数据
        self.articles_df = self.data_loader.get_articles()
        interactions_df = self.data_loader.get_user_interactions()
        
        if self.articles_df.empty:
            logger.warning("没有文章数据，跳过训练")
            return
        
        # 训练内容推荐模型
        self.content_recommender.fit(self.articles_df)
        
        # 训练协同过滤模型
        if not interactions_df.empty:
            self.cf_recommender.fit(interactions_df)
        
        # 计算热门文章
        self._compute_hot_articles()
        
        self.last_train_time = time.time()
        logger.info(f"推荐模型训练完成，耗时: {time.time() - start_time:.2f}秒")
    
    def _compute_hot_articles(self):
        """计算热门文章排行"""
        if self.articles_df is None or self.articles_df.empty:
            return
        
        # 热度分数 = 浏览量*1 + 点赞数*3 + 评论数*5
        df = self.articles_df.copy()
        df['hot_score'] = (
            df['view_count'].fillna(0) * 1 +
            df['like_count'].fillna(0) * 3 +
            df['comment_count'].fillna(0) * 5
        )
        
        # 时间衰减因子(7天内的文章权重更高)
        df['created_at'] = pd.to_datetime(df['created_at'])
        now = pd.Timestamp.now()
        df['days_ago'] = (now - df['created_at']).dt.days
        df['time_decay'] = np.exp(-df['days_ago'] / 7)
        df['final_score'] = df['hot_score'] * df['time_decay']
        
        self.hot_articles = df.nlargest(100, 'final_score')[['id', 'final_score']].values.tolist()
    
    def _should_retrain(self) -> bool:
        """检查是否需要重新训练"""
        return time.time() - self.last_train_time > self.train_interval
    
    def recommend(self, user_id: Optional[int] = None, top_n: int = 10,
                  exclude_ids: List[int] = None, shuffle: bool = True) -> List[Dict]:
        """混合推荐主入口
        
        Args:
            user_id: 用户ID，None表示未登录
            top_n: 返回推荐数量
            exclude_ids: 需要排除的文章ID
            shuffle: 是否随机打乱结果（用于"换一批"功能）
        """
        if self._should_retrain():
            self.train()
        
        exclude_ids = exclude_ids or []
        
        if user_id is None:
            # 未登录用户，返回热门推荐
            results = self._get_hot_recommendations(top_n * 2, exclude_ids)
        else:
            # 获取用户历史
            user_history = self.data_loader.get_user_history(user_id)
            
            if len(user_history) < self.config.get('min_interactions', 3):
                # 新用户，主要使用热门推荐
                results = self._get_cold_start_recommendations(user_id, top_n * 2, exclude_ids)
            else:
                # 老用户，使用混合推荐
                results = self._get_hybrid_recommendations(user_id, user_history, top_n * 2, exclude_ids)
        
        # 如果需要随机打乱（换一批功能）
        if shuffle and len(results) > top_n:
            import random
            random.shuffle(results)
        
        return results[:top_n]
    
    def _get_hot_recommendations(self, top_n: int, exclude_ids: List[int]) -> List[Dict]:
        """热门推荐"""
        exclude_set = set(exclude_ids)
        results = []
        
        for article_id, score in self.hot_articles:
            if article_id not in exclude_set:
                results.append({
                    "article_id": int(article_id),
                    "score": float(score),
                    "reason": "热门推荐"
                })
                if len(results) >= top_n:
                    break
        
        return results
    
    def _get_cold_start_recommendations(self, user_id: int, top_n: int, 
                                        exclude_ids: List[int]) -> List[Dict]:
        """冷启动推荐(新用户)"""
        # 获取用户画像
        user_profile = self.data_loader.get_user_profile(user_id)
        
        # 70%热门 + 30%基于用户偏好的内容推荐
        hot_count = int(top_n * 0.7)
        content_count = top_n - hot_count
        
        results = self._get_hot_recommendations(hot_count, exclude_ids)
        used_ids = set(exclude_ids + [r['article_id'] for r in results])
        
        # 如果有用户偏好标签，基于标签推荐
        if user_profile.get('liked_tags'):
            content_recs = self._recommend_by_tags(user_profile['liked_tags'], 
                                                   content_count, list(used_ids))
            results.extend(content_recs)
        
        return results[:top_n]
    
    def _recommend_by_tags(self, tags: List[str], top_n: int, 
                          exclude_ids: List[int]) -> List[Dict]:
        """基于标签推荐"""
        if self.articles_df is None:
            return []
        
        exclude_set = set(exclude_ids)
        results = []
        
        for _, row in self.articles_df.iterrows():
            if row['id'] in exclude_set:
                continue
            
            article_tags = str(row['tags']).split(',') if row['tags'] else []
            # 计算标签匹配度
            match_count = len(set(tags) & set(article_tags))
            if match_count > 0:
                results.append({
                    "article_id": int(row['id']),
                    "score": float(match_count),
                    "reason": f"标签匹配: {', '.join(set(tags) & set(article_tags))}"
                })
        
        results.sort(key=lambda x: x['score'], reverse=True)
        return results[:top_n]
    
    def _get_hybrid_recommendations(self, user_id: int, user_history: List[int],
                                    top_n: int, exclude_ids: List[int]) -> List[Dict]:
        """混合推荐(老用户)"""
        content_weight = self.config.get('content_weight', 0.4)
        cf_weight = self.config.get('cf_weight', 0.4)
        hot_weight = self.config.get('hot_weight', 0.2)
        
        all_exclude = list(set(exclude_ids + user_history))
        
        # 获取各算法推荐结果
        content_recs = self.content_recommender.recommend_for_user(
            user_history, top_n * 2, all_exclude
        )
        cf_recs = self.cf_recommender.recommend_for_user(
            user_id, top_n * 2, all_exclude
        )
        hot_recs = [(aid, score) for aid, score in self.hot_articles 
                    if aid not in set(all_exclude)][:top_n]
        
        # 归一化分数
        content_scores = self._normalize_scores(content_recs)
        cf_scores = self._normalize_scores(cf_recs)
        hot_scores = self._normalize_scores(hot_recs)
        
        # 融合分数
        merged_scores = {}
        reasons = {}
        
        for aid, score in content_scores.items():
            merged_scores[aid] = merged_scores.get(aid, 0) + score * content_weight
            reasons[aid] = "内容相似"
        
        for aid, score in cf_scores.items():
            merged_scores[aid] = merged_scores.get(aid, 0) + score * cf_weight
            reasons[aid] = reasons.get(aid, "") + ("+" if aid in reasons else "") + "协同过滤"
        
        for aid, score in hot_scores.items():
            merged_scores[aid] = merged_scores.get(aid, 0) + score * hot_weight
            if aid not in reasons:
                reasons[aid] = "热门推荐"
        
        # 排序并返回
        sorted_items = sorted(merged_scores.items(), key=lambda x: x[1], reverse=True)
        
        results = []
        for aid, score in sorted_items[:top_n]:
            results.append({
                "article_id": int(aid),
                "score": float(score),
                "reason": reasons.get(aid, "综合推荐")
            })
        
        return results
    
    def _normalize_scores(self, scores: List[Tuple[int, float]]) -> Dict[int, float]:
        """归一化分数到0-1范围"""
        if not scores:
            return {}
        
        values = [s for _, s in scores]
        min_val, max_val = min(values), max(values)
        
        if max_val == min_val:
            return {aid: 1.0 for aid, _ in scores}
        
        return {aid: (score - min_val) / (max_val - min_val) 
                for aid, score in scores}
    
    def get_similar_articles(self, article_id: int, top_n: int = 10) -> List[Dict]:
        """获取相似文章"""
        similar = self.content_recommender.get_similar_articles(article_id, top_n)
        return [
            {"article_id": int(aid), "score": float(score), "reason": "内容相似"}
            for aid, score in similar
        ]
