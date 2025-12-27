"""用户分类算法 - 基于K-Means聚类"""
import pandas as pd
import numpy as np
from typing import List, Dict, Optional
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
from .data_loader import DataLoader
import logging
import time

logger = logging.getLogger(__name__)


class UserClusteringAnalyzer:
    """用户聚类分析器 - 将用户分为不同类型"""
    
    def __init__(self, data_loader: DataLoader, n_clusters: int = 5):
        self.data_loader = data_loader
        self.n_clusters = n_clusters
        self.model = None
        self.scaler = StandardScaler()
        self.user_features = None
        self.cluster_labels = None
        self.last_train_time = 0
        self.cache_duration = 3600  # 1小时缓存
        
        # 用户类型定义
        self.cluster_names = {
            0: {'name': '活跃创作者', 'icon': '✍️', 'color': '#f59e0b', 
                'desc': '经常发布内容，互动频繁'},
            1: {'name': '深度阅读者', 'icon': '📚', 'color': '#3b82f6',
                'desc': '阅读量大，偏好长文'},
            2: {'name': '社交达人', 'icon': '💬', 'color': '#ec4899',
                'desc': '评论互动多，喜欢社交'},
            3: {'name': '视频爱好者', 'icon': '🎬', 'color': '#8b5cf6',
                'desc': '偏好视频内容'},
            4: {'name': '潜水用户', 'icon': '👀', 'color': '#6b7280',
                'desc': '浏览为主，较少互动'},
        }
    
    def train(self):
        """训练聚类模型"""
        logger.info("开始训练用户聚类模型...")
        start_time = time.time()
        
        try:
            # 获取用户行为数据
            features_df = self._extract_user_features()
            
            if features_df.empty or len(features_df) < self.n_clusters:
                logger.warning("用户数据不足，无法训练聚类模型")
                return
            
            # 标准化特征
            feature_cols = ['article_count', 'video_count', 'comment_count', 
                          'like_count', 'view_count', 'avg_read_time']
            X = features_df[feature_cols].fillna(0)
            X_scaled = self.scaler.fit_transform(X)
            
            # K-Means聚类
            self.model = KMeans(n_clusters=self.n_clusters, random_state=42, n_init=10)
            self.cluster_labels = self.model.fit_predict(X_scaled)
            
            # 保存结果
            features_df['cluster'] = self.cluster_labels
            self.user_features = features_df
            
            # 分析每个簇的特征，重新映射到有意义的标签
            self._analyze_clusters()
            
            self.last_train_time = time.time()
            logger.info(f"用户聚类模型训练完成，耗时{time.time()-start_time:.2f}秒")
            
        except Exception as e:
            logger.error(f"用户聚类训练失败: {e}")
    
    def _extract_user_features(self) -> pd.DataFrame:
        """提取用户特征"""
        users_df = self.data_loader.get_users()
        articles_df = self.data_loader.get_articles()
        videos_df = self.data_loader.get_videos()
        
        if users_df.empty:
            return pd.DataFrame()
        
        features = []
        for _, user in users_df.iterrows():
            user_id = user['id']
            
            # 统计发布数量
            article_count = len(articles_df[articles_df['author_id'] == user_id]) if not articles_df.empty else 0
            video_count = len(videos_df[videos_df['author_id'] == user_id]) if not videos_df.empty else 0
            
            # 模拟其他特征（实际应从数据库获取）
            comment_count = np.random.poisson(5)  # 评论数
            like_count = np.random.poisson(10)    # 点赞数
            view_count = np.random.poisson(50)    # 浏览数
            avg_read_time = np.random.exponential(3)  # 平均阅读时长
            
            features.append({
                'user_id': user_id,
                'username': user.get('username', ''),
                'article_count': article_count,
                'video_count': video_count,
                'comment_count': comment_count,
                'like_count': like_count,
                'view_count': view_count,
                'avg_read_time': avg_read_time
            })
        
        return pd.DataFrame(features)
    
    def _analyze_clusters(self):
        """分析聚类结果，映射到有意义的用户类型"""
        if self.user_features is None:
            return
        
        # 计算每个簇的特征均值
        cluster_stats = self.user_features.groupby('cluster').agg({
            'article_count': 'mean',
            'video_count': 'mean',
            'comment_count': 'mean',
            'like_count': 'mean',
            'view_count': 'mean'
        })
        
        # 根据特征重新分配标签
        cluster_mapping = {}
        for cluster_id in range(self.n_clusters):
            if cluster_id not in cluster_stats.index:
                continue
            stats = cluster_stats.loc[cluster_id]
            
            # 判断用户类型
            if stats['article_count'] > cluster_stats['article_count'].median():
                cluster_mapping[cluster_id] = 0  # 活跃创作者
            elif stats['video_count'] > cluster_stats['video_count'].median():
                cluster_mapping[cluster_id] = 3  # 视频爱好者
            elif stats['comment_count'] > cluster_stats['comment_count'].median():
                cluster_mapping[cluster_id] = 2  # 社交达人
            elif stats['view_count'] > cluster_stats['view_count'].median():
                cluster_mapping[cluster_id] = 1  # 深度阅读者
            else:
                cluster_mapping[cluster_id] = 4  # 潜水用户
        
        # 应用映射
        self.user_features['user_type'] = self.user_features['cluster'].map(
            lambda x: cluster_mapping.get(x, 4)
        )
    
    def get_user_type(self, user_id: int) -> Dict:
        """获取单个用户的类型"""
        if self.user_features is None:
            self.train()
        
        if self.user_features is None:
            return {'type': 4, **self.cluster_names[4]}
        
        user_row = self.user_features[self.user_features['user_id'] == user_id]
        if user_row.empty:
            return {'type': 4, **self.cluster_names[4]}
        
        user_type = int(user_row['user_type'].iloc[0])
        return {
            'type': user_type,
            'user_id': user_id,
            **self.cluster_names.get(user_type, self.cluster_names[4])
        }
    
    def get_all_user_types(self) -> List[Dict]:
        """获取所有用户的类型分布"""
        if self.user_features is None:
            self.train()
        
        if self.user_features is None:
            return []
        
        results = []
        for _, row in self.user_features.iterrows():
            user_type = int(row.get('user_type', 4))
            results.append({
                'user_id': int(row['user_id']),
                'username': row.get('username', ''),
                'type': user_type,
                **self.cluster_names.get(user_type, self.cluster_names[4])
            })
        
        return results
    
    def get_cluster_distribution(self) -> Dict:
        """获取用户类型分布统计"""
        if self.user_features is None:
            self.train()
        
        if self.user_features is None:
            return {'total': 0, 'distribution': []}
        
        distribution = []
        type_counts = self.user_features['user_type'].value_counts()
        total = len(self.user_features)
        
        for type_id, info in self.cluster_names.items():
            count = int(type_counts.get(type_id, 0))
            distribution.append({
                'type': type_id,
                'name': info['name'],
                'icon': info['icon'],
                'color': info['color'],
                'count': count,
                'percentage': round(count / total * 100, 1) if total > 0 else 0
            })
        
        return {
            'total': total,
            'distribution': sorted(distribution, key=lambda x: x['count'], reverse=True)
        }
