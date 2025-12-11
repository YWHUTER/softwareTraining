"""协同过滤推荐算法 - 基于用户行为的矩阵分解"""
import numpy as np
import pandas as pd
from sklearn.decomposition import TruncatedSVD
from scipy.sparse import csr_matrix
from typing import List, Dict, Tuple
import logging

logger = logging.getLogger(__name__)

class CollaborativeFilterRecommender:
    def __init__(self, n_factors: int = 50):
        self.n_factors = n_factors
        self.svd = TruncatedSVD(n_components=n_factors, random_state=42)
        self.user_factors = None
        self.item_factors = None
        self.user_id_to_idx = {}
        self.idx_to_user_id = {}
        self.article_id_to_idx = {}
        self.idx_to_article_id = {}
        self.user_item_matrix = None
        self.global_mean = 0
    
    def fit(self, interactions_df: pd.DataFrame):
        """训练协同过滤模型"""
        if interactions_df.empty:
            logger.warning("交互数据为空，跳过训练")
            return
        
        # 聚合用户-文章交互分数
        user_item_scores = interactions_df.groupby(['user_id', 'article_id'])['weight'].sum().reset_index()
        
        # 构建用户和文章索引
        unique_users = user_item_scores['user_id'].unique()
        unique_articles = user_item_scores['article_id'].unique()
        
        for idx, user_id in enumerate(unique_users):
            self.user_id_to_idx[user_id] = idx
            self.idx_to_user_id[idx] = user_id
        
        for idx, article_id in enumerate(unique_articles):
            self.article_id_to_idx[article_id] = idx
            self.idx_to_article_id[idx] = article_id
        
        # 构建稀疏用户-物品矩阵
        rows = [self.user_id_to_idx[uid] for uid in user_item_scores['user_id']]
        cols = [self.article_id_to_idx[aid] for aid in user_item_scores['article_id']]
        data = user_item_scores['weight'].values
        
        n_users = len(unique_users)
        n_items = len(unique_articles)
        
        self.user_item_matrix = csr_matrix((data, (rows, cols)), shape=(n_users, n_items))
        self.global_mean = np.mean(data)
        
        # SVD分解
        if n_users > self.n_factors and n_items > self.n_factors:
            self.user_factors = self.svd.fit_transform(self.user_item_matrix)
            self.item_factors = self.svd.components_.T
        else:
            # 数据量太小，使用简单的矩阵
            self.user_factors = self.user_item_matrix.toarray()
            self.item_factors = np.eye(n_items)
        
        logger.info(f"协同过滤模型训练完成，用户数: {n_users}, 文章数: {n_items}")
    
    def predict_score(self, user_id: int, article_id: int) -> float:
        """预测用户对文章的评分"""
        if user_id not in self.user_id_to_idx or article_id not in self.article_id_to_idx:
            return self.global_mean
        
        user_idx = self.user_id_to_idx[user_id]
        article_idx = self.article_id_to_idx[article_id]
        
        score = np.dot(self.user_factors[user_idx], self.item_factors[article_idx])
        return float(max(0, score))
    
    def recommend_for_user(self, user_id: int, top_n: int = 10, 
                          exclude_ids: List[int] = None) -> List[Tuple[int, float]]:
        """为用户推荐文章"""
        if user_id not in self.user_id_to_idx or self.user_factors is None:
            return []
        
        exclude_ids = exclude_ids or []
        exclude_set = set(exclude_ids)
        
        user_idx = self.user_id_to_idx[user_id]
        
        # 计算用户对所有文章的预测分数
        scores = np.dot(self.user_factors[user_idx], self.item_factors.T)
        
        # 获取用户已交互的文章
        interacted = set(self.user_item_matrix[user_idx].nonzero()[1])
        
        # 排序并过滤
        results = []
        for idx in scores.argsort()[::-1]:
            if idx not in interacted:
                article_id = self.idx_to_article_id[idx]
                if article_id not in exclude_set:
                    results.append((article_id, float(scores[idx])))
                    if len(results) >= top_n:
                        break
        
        return results
    
    def get_similar_users(self, user_id: int, top_n: int = 10) -> List[Tuple[int, float]]:
        """获取相似用户"""
        if user_id not in self.user_id_to_idx or self.user_factors is None:
            return []
        
        user_idx = self.user_id_to_idx[user_id]
        user_vector = self.user_factors[user_idx].reshape(1, -1)
        
        # 计算与其他用户的相似度
        similarities = np.dot(self.user_factors, user_vector.T).flatten()
        similarities[user_idx] = -1  # 排除自己
        
        results = []
        for idx in similarities.argsort()[::-1][:top_n]:
            if similarities[idx] > 0:
                results.append((self.idx_to_user_id[idx], float(similarities[idx])))
        
        return results
