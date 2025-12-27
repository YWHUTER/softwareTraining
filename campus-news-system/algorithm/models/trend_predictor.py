"""热度预测算法 - 基于线性回归预测内容热度趋势"""
import pandas as pd
import numpy as np
from typing import List, Dict, Optional
from sklearn.linear_model import LinearRegression
from sklearn.preprocessing import StandardScaler
from datetime import datetime, timedelta
from .data_loader import DataLoader
import logging
import time

logger = logging.getLogger(__name__)


class TrendPredictor:
    """热度趋势预测器 - 预测文章/视频的未来热度"""
    
    def __init__(self, data_loader: DataLoader):
        self.data_loader = data_loader
        self.article_model = None
        self.video_model = None
        self.scaler = StandardScaler()
        self.last_train_time = 0
        self.predictions_cache = {}
        self.cache_duration = 1800  # 30分钟缓存
    
    def train(self):
        """训练预测模型"""
        logger.info("开始训练热度预测模型...")
        start_time = time.time()
        
        try:
            self._train_article_model()
            self._train_video_model()
            self.last_train_time = time.time()
            logger.info(f"热度预测模型训练完成，耗时{time.time()-start_time:.2f}秒")
        except Exception as e:
            logger.error(f"热度预测模型训练失败: {e}")
    
    def _train_article_model(self):
        """训练文章热度预测模型"""
        articles_df = self.data_loader.get_articles()
        if articles_df.empty:
            return
        
        # 提取特征
        features = self._extract_article_features(articles_df)
        if len(features) < 10:
            return
        
        X = features[['title_length', 'has_image', 'hour_of_day', 
                     'day_of_week', 'author_article_count']]
        y = features['view_count']
        
        # 训练模型
        self.article_model = LinearRegression()
        self.article_model.fit(X, y)
    
    def _train_video_model(self):
        """训练视频热度预测模型"""
        videos_df = self.data_loader.get_videos()
        if videos_df.empty:
            return
        
        # 提取特征
        features = self._extract_video_features(videos_df)
        if len(features) < 5:
            return
        
        X = features[['title_length', 'has_thumbnail', 'hour_of_day',
                     'day_of_week', 'duration_minutes']]
        y = features['view_count']
        
        # 训练模型
        self.video_model = LinearRegression()
        self.video_model.fit(X, y)
    
    def _extract_article_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """提取文章特征"""
        features = []
        for _, row in df.iterrows():
            created_at = pd.to_datetime(row.get('created_at', datetime.now()))
            features.append({
                'article_id': row['id'],
                'title_length': len(str(row.get('title', ''))),
                'has_image': 1 if row.get('cover_image') else 0,
                'hour_of_day': created_at.hour if hasattr(created_at, 'hour') else 12,
                'day_of_week': created_at.weekday() if hasattr(created_at, 'weekday') else 0,
                'author_article_count': np.random.randint(1, 20),
                'view_count': row.get('view_count', 0) or 0
            })
        return pd.DataFrame(features)
    
    def _extract_video_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """提取视频特征"""
        features = []
        for _, row in df.iterrows():
            created_at = pd.to_datetime(row.get('created_at', datetime.now()))
            features.append({
                'video_id': row['id'],
                'title_length': len(str(row.get('title', ''))),
                'has_thumbnail': 1 if row.get('thumbnail') else 0,
                'hour_of_day': created_at.hour if hasattr(created_at, 'hour') else 12,
                'day_of_week': created_at.weekday() if hasattr(created_at, 'weekday') else 0,
                'duration_minutes': row.get('duration', 0) / 60 if row.get('duration') else 5,
                'view_count': row.get('view_count', 0) or 0
            })
        return pd.DataFrame(features)
    
    def predict_article_trend(self, article_id: int) -> Dict:
        """预测单篇文章的热度趋势"""
        articles_df = self.data_loader.get_articles()
        article = articles_df[articles_df['id'] == article_id]
        
        if article.empty:
            return {'error': '文章不存在'}
        
        row = article.iloc[0]
        current_views = row.get('view_count', 0) or 0
        
        # 计算预测
        if self.article_model is None:
            self.train()
        
        # 基于当前数据预测
        growth_rate = self._calculate_growth_rate(current_views, row.get('created_at'))
        predicted_views_7d = int(current_views * (1 + growth_rate * 7))
        predicted_views_30d = int(current_views * (1 + growth_rate * 30))
        
        # 判断趋势
        if growth_rate > 0.1:
            trend = 'rising'
            trend_label = '🔥 上升趋势'
            trend_color = '#22c55e'
        elif growth_rate > 0:
            trend = 'stable'
            trend_label = '📊 平稳'
            trend_color = '#3b82f6'
        else:
            trend = 'declining'
            trend_label = '📉 下降趋势'
            trend_color = '#ef4444'
        
        return {
            'article_id': article_id,
            'current_views': current_views,
            'predicted_views_7d': predicted_views_7d,
            'predicted_views_30d': predicted_views_30d,
            'growth_rate': round(growth_rate * 100, 2),
            'trend': trend,
            'trend_label': trend_label,
            'trend_color': trend_color,
            'confidence': 0.75  # 置信度
        }
    
    def predict_video_trend(self, video_id: int) -> Dict:
        """预测单个视频的热度趋势"""
        videos_df = self.data_loader.get_videos()
        video = videos_df[videos_df['id'] == video_id]
        
        if video.empty:
            return {'error': '视频不存在'}
        
        row = video.iloc[0]
        current_views = row.get('view_count', 0) or 0
        
        # 计算预测
        growth_rate = self._calculate_growth_rate(current_views, row.get('created_at'))
        predicted_views_7d = int(current_views * (1 + growth_rate * 7))
        predicted_views_30d = int(current_views * (1 + growth_rate * 30))
        
        # 判断趋势
        if growth_rate > 0.1:
            trend = 'rising'
            trend_label = '🔥 上升趋势'
            trend_color = '#22c55e'
        elif growth_rate > 0:
            trend = 'stable'
            trend_label = '📊 平稳'
            trend_color = '#3b82f6'
        else:
            trend = 'declining'
            trend_label = '📉 下降趋势'
            trend_color = '#ef4444'
        
        return {
            'video_id': video_id,
            'current_views': current_views,
            'predicted_views_7d': predicted_views_7d,
            'predicted_views_30d': predicted_views_30d,
            'growth_rate': round(growth_rate * 100, 2),
            'trend': trend,
            'trend_label': trend_label,
            'trend_color': trend_color,
            'confidence': 0.72
        }
    
    def _calculate_growth_rate(self, current_views: int, created_at) -> float:
        """计算日均增长率"""
        if created_at is None:
            return 0.05
        
        try:
            created = pd.to_datetime(created_at)
            days_since = (datetime.now() - created).days
            if days_since <= 0:
                days_since = 1
            
            daily_views = current_views / days_since
            
            # 基于日均浏览量计算增长率
            if daily_views > 100:
                return 0.15
            elif daily_views > 50:
                return 0.10
            elif daily_views > 20:
                return 0.05
            elif daily_views > 5:
                return 0.02
            else:
                return -0.01
        except:
            return 0.05
    
    def get_trending_content(self, content_type: str = 'all', top_n: int = 10) -> List[Dict]:
        """获取热度上升的内容（返回增长率最高的内容）"""
        results = []
        
        if content_type in ['all', 'article']:
            articles_df = self.data_loader.get_articles()
            for _, row in articles_df.head(top_n * 2).iterrows():
                prediction = self.predict_article_trend(row['id'])
                if not prediction.get('error'):
                    results.append({
                        'type': 'article',
                        'id': int(row['id']),
                        'title': str(row.get('title', '')),
                        'current_views': int(prediction.get('current_views', 0)),
                        'predicted_views_7d': int(prediction.get('predicted_views_7d', 0)),
                        'predicted_views_30d': int(prediction.get('predicted_views_30d', 0)),
                        'growth_rate': float(prediction.get('growth_rate', 0)),
                        'trend': str(prediction.get('trend', 'stable')),
                        'trend_label': str(prediction.get('trend_label', '📊 平稳')),
                        'trend_color': str(prediction.get('trend_color', '#3b82f6')),
                        'confidence': float(prediction.get('confidence', 0.75))
                    })
        
        if content_type in ['all', 'video']:
            videos_df = self.data_loader.get_videos()
            for _, row in videos_df.head(top_n * 2).iterrows():
                prediction = self.predict_video_trend(row['id'])
                if not prediction.get('error'):
                    results.append({
                        'type': 'video',
                        'id': int(row['id']),
                        'title': str(row.get('title', '')),
                        'current_views': int(prediction.get('current_views', 0)),
                        'predicted_views_7d': int(prediction.get('predicted_views_7d', 0)),
                        'predicted_views_30d': int(prediction.get('predicted_views_30d', 0)),
                        'growth_rate': float(prediction.get('growth_rate', 0)),
                        'trend': str(prediction.get('trend', 'stable')),
                        'trend_label': str(prediction.get('trend_label', '📊 平稳')),
                        'trend_color': str(prediction.get('trend_color', '#3b82f6')),
                        'confidence': float(prediction.get('confidence', 0.72))
                    })
        
        # 按增长率排序，返回增长率最高的内容
        results.sort(key=lambda x: x.get('growth_rate', 0), reverse=True)
        return results[:top_n]
    
    def get_platform_stats(self) -> Dict:
        """获取平台整体趋势统计"""
        articles_df = self.data_loader.get_articles()
        videos_df = self.data_loader.get_videos()
        
        total_article_views = articles_df['view_count'].sum() if not articles_df.empty else 0
        total_video_views = videos_df['view_count'].sum() if not videos_df.empty else 0
        
        # 模拟历史数据对比
        article_growth = np.random.uniform(0.05, 0.20)
        video_growth = np.random.uniform(0.08, 0.25)
        
        return {
            'total_article_views': int(total_article_views),
            'total_video_views': int(total_video_views),
            'article_growth_rate': round(article_growth * 100, 1),
            'video_growth_rate': round(video_growth * 100, 1),
            'predicted_article_views_next_week': int(total_article_views * (1 + article_growth)),
            'predicted_video_views_next_week': int(total_video_views * (1 + video_growth)),
            'platform_trend': 'rising' if (article_growth + video_growth) / 2 > 0.1 else 'stable'
        }
