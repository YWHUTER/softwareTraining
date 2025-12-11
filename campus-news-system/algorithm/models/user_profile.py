"""用户画像分析模块 - 分析用户阅读偏好和行为特征"""
import pandas as pd
import numpy as np
from collections import Counter, defaultdict
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any
import logging
import jieba
import jieba.analyse

logger = logging.getLogger(__name__)


class UserProfileAnalyzer:
    """用户画像分析器"""
    
    def __init__(self, data_loader):
        self.data_loader = data_loader
        self.user_profiles = {}  # 缓存用户画像
        
    def analyze_user(self, user_id: int) -> Dict[str, Any]:
        """
        分析单个用户的完整画像
        
        返回:
        - basic_info: 基本信息
        - interest_tags: 兴趣标签 (权重排序)
        - category_preference: 分类偏好
        - activity_pattern: 活跃时间模式
        - behavior_stats: 行为统计
        - reading_level: 阅读深度等级
        - user_type: 用户类型标签
        """
        try:
            # 获取用户交互数据
            interactions = self._get_user_interactions(user_id)
            if interactions.empty:
                return self._empty_profile(user_id)
            
            # 获取用户交互的文章详情
            article_ids = interactions['article_id'].unique().tolist()
            articles = self._get_articles_by_ids(article_ids)
            
            profile = {
                "user_id": user_id,
                "basic_info": self._get_basic_info(user_id),
                "interest_tags": self._analyze_interest_tags(interactions, articles),
                "category_preference": self._analyze_category_preference(interactions, articles),
                "activity_pattern": self._analyze_activity_pattern(interactions),
                "behavior_stats": self._analyze_behavior_stats(interactions),
                "reading_level": self._calculate_reading_level(interactions, articles),
                "user_type": self._determine_user_type(interactions),
                "generated_at": datetime.now().isoformat()
            }
            
            # 缓存画像
            self.user_profiles[user_id] = profile
            return profile
            
        except Exception as e:
            logger.error(f"分析用户画像失败 user_id={user_id}: {e}")
            return self._empty_profile(user_id)
    
    def _get_user_interactions(self, user_id: int) -> pd.DataFrame:
        """获取用户的所有交互记录"""
        all_interactions = self.data_loader.get_user_interactions()
        if all_interactions.empty:
            return pd.DataFrame()
        return all_interactions[all_interactions['user_id'] == user_id]
    
    def _get_articles_by_ids(self, article_ids: List[int]) -> pd.DataFrame:
        """获取指定文章的详情"""
        all_articles = self.data_loader.get_articles()
        if all_articles.empty:
            return pd.DataFrame()
        return all_articles[all_articles['id'].isin(article_ids)]
    
    def _get_basic_info(self, user_id: int) -> Dict:
        """获取用户基本信息"""
        return self.data_loader.get_user_profile(user_id)
    
    def _analyze_interest_tags(self, interactions: pd.DataFrame, articles: pd.DataFrame) -> List[Dict]:
        """
        分析用户兴趣标签
        基于用户交互的文章标签和内容关键词
        """
        tag_weights = defaultdict(float)
        
        # 权重配置
        action_weights = {'like': 3, 'favorite': 5, 'comment': 4, 'view': 1}
        
        for _, row in interactions.iterrows():
            article_id = row['article_id']
            action_type = row.get('action_type', 'view')
            weight = action_weights.get(action_type, 1)
            
            # 获取文章标签
            article = articles[articles['id'] == article_id]
            if not article.empty:
                tags = article.iloc[0].get('tags', '')
                if tags and pd.notna(tags):
                    for tag in str(tags).split(','):
                        tag = tag.strip()
                        if tag:
                            tag_weights[tag] += weight
                
                # 从标题提取关键词
                title = article.iloc[0].get('title', '')
                if title:
                    keywords = jieba.analyse.extract_tags(title, topK=3, withWeight=True)
                    for kw, kw_weight in keywords:
                        tag_weights[kw] += weight * kw_weight * 0.5
        
        # 排序并返回前20个标签
        sorted_tags = sorted(tag_weights.items(), key=lambda x: x[1], reverse=True)[:20]
        
        # 归一化权重
        max_weight = sorted_tags[0][1] if sorted_tags else 1
        return [
            {"tag": tag, "weight": round(w / max_weight, 3), "raw_score": round(w, 2)}
            for tag, w in sorted_tags
        ]
    
    def _analyze_category_preference(self, interactions: pd.DataFrame, articles: pd.DataFrame) -> List[Dict]:
        """分析用户的分类偏好"""
        category_counts = defaultdict(int)
        
        for _, row in interactions.iterrows():
            article_id = row['article_id']
            article = articles[articles['id'] == article_id]
            if not article.empty:
                board_type = article.iloc[0].get('board_type', 'UNKNOWN')
                if board_type and pd.notna(board_type):
                    category_counts[board_type] += 1
        
        total = sum(category_counts.values()) or 1
        
        # 分类名称映射
        category_names = {
            'OFFICIAL': '官方新闻',
            'CAMPUS': '全校新闻', 
            'COLLEGE': '学院新闻'
        }
        
        return [
            {
                "category": cat,
                "name": category_names.get(cat, cat),
                "count": count,
                "percentage": round(count / total * 100, 1)
            }
            for cat, count in sorted(category_counts.items(), key=lambda x: x[1], reverse=True)
        ]
    
    def _analyze_activity_pattern(self, interactions: pd.DataFrame) -> Dict:
        """分析用户活跃时间模式"""
        if interactions.empty or 'created_at' not in interactions.columns:
            return {"hourly": [], "daily": [], "peak_hours": [], "active_days": []}
        
        # 转换时间
        interactions['created_at'] = pd.to_datetime(interactions['created_at'])
        interactions['hour'] = interactions['created_at'].dt.hour
        interactions['weekday'] = interactions['created_at'].dt.dayofweek
        
        # 按小时统计
        hourly = interactions['hour'].value_counts().sort_index()
        hourly_data = [{"hour": int(h), "count": int(c)} for h, c in hourly.items()]
        
        # 按星期统计
        weekday_names = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
        daily = interactions['weekday'].value_counts().sort_index()
        daily_data = [
            {"day": int(d), "name": weekday_names[d], "count": int(c)} 
            for d, c in daily.items()
        ]
        
        # 找出高峰时段 (活跃度前3的小时)
        peak_hours = hourly.nlargest(3).index.tolist()
        
        # 找出活跃日 (活跃度前3的星期)
        active_days = [weekday_names[d] for d in daily.nlargest(3).index.tolist()]
        
        return {
            "hourly": hourly_data,
            "daily": daily_data,
            "peak_hours": [int(h) for h in peak_hours],
            "active_days": active_days
        }
    
    def _analyze_behavior_stats(self, interactions: pd.DataFrame) -> Dict:
        """分析用户行为统计"""
        if interactions.empty:
            return {
                "total_interactions": 0,
                "like_count": 0,
                "favorite_count": 0,
                "comment_count": 0,
                "unique_articles": 0,
                "avg_daily_interactions": 0
            }
        
        action_counts = interactions['action_type'].value_counts().to_dict() if 'action_type' in interactions.columns else {}
        
        # 计算日均互动
        if 'created_at' in interactions.columns:
            interactions['created_at'] = pd.to_datetime(interactions['created_at'])
            date_range = (interactions['created_at'].max() - interactions['created_at'].min()).days + 1
            avg_daily = len(interactions) / max(date_range, 1)
        else:
            avg_daily = 0
        
        return {
            "total_interactions": len(interactions),
            "like_count": action_counts.get('like', 0),
            "favorite_count": action_counts.get('favorite', 0),
            "comment_count": action_counts.get('comment', 0),
            "unique_articles": interactions['article_id'].nunique(),
            "avg_daily_interactions": round(avg_daily, 2)
        }
    
    def _calculate_reading_level(self, interactions: pd.DataFrame, articles: pd.DataFrame) -> Dict:
        """
        计算用户阅读深度等级
        基于收藏率、评论率等指标
        """
        if interactions.empty:
            return {"level": "新手", "score": 0, "description": "暂无阅读记录"}
        
        total = len(interactions)
        action_counts = interactions['action_type'].value_counts().to_dict() if 'action_type' in interactions.columns else {}
        
        # 计算各项指标
        like_rate = action_counts.get('like', 0) / max(total, 1)
        favorite_rate = action_counts.get('favorite', 0) / max(total, 1)
        comment_rate = action_counts.get('comment', 0) / max(total, 1)
        
        # 综合评分 (0-100)
        score = min(100, int(
            like_rate * 20 +
            favorite_rate * 40 +
            comment_rate * 40 +
            min(total / 10, 20)  # 活跃度加分
        ))
        
        # 等级划分
        if score >= 80:
            level, desc = "资深读者", "深度参与，经常收藏和评论"
        elif score >= 60:
            level, desc = "活跃读者", "积极互动，有明确的阅读偏好"
        elif score >= 40:
            level, desc = "普通读者", "有一定的阅读习惯"
        elif score >= 20:
            level, desc = "轻度读者", "偶尔浏览，互动较少"
        else:
            level, desc = "新手", "刚开始使用，建议多探索"
        
        return {
            "level": level,
            "score": score,
            "description": desc,
            "metrics": {
                "like_rate": round(like_rate * 100, 1),
                "favorite_rate": round(favorite_rate * 100, 1),
                "comment_rate": round(comment_rate * 100, 1)
            }
        }
    
    def _determine_user_type(self, interactions: pd.DataFrame) -> List[str]:
        """
        确定用户类型标签
        """
        if interactions.empty:
            return ["新用户"]
        
        types = []
        action_counts = interactions['action_type'].value_counts().to_dict() if 'action_type' in interactions.columns else {}
        total = len(interactions)
        
        # 收藏达人
        if action_counts.get('favorite', 0) >= 10:
            types.append("收藏达人")
        
        # 评论活跃
        if action_counts.get('comment', 0) >= 5:
            types.append("评论活跃")
        
        # 点赞狂魔
        if action_counts.get('like', 0) >= 20:
            types.append("点赞狂魔")
        
        # 活跃用户
        if total >= 50:
            types.append("活跃用户")
        elif total >= 20:
            types.append("普通用户")
        else:
            types.append("新用户")
        
        # 检查活跃时间模式
        if 'created_at' in interactions.columns:
            interactions['created_at'] = pd.to_datetime(interactions['created_at'])
            hours = interactions['created_at'].dt.hour
            
            # 夜猫子 (22点-6点活跃)
            night_count = ((hours >= 22) | (hours <= 6)).sum()
            if night_count / max(total, 1) > 0.3:
                types.append("夜猫子")
            
            # 早起鸟 (6点-9点活跃)
            morning_count = ((hours >= 6) & (hours <= 9)).sum()
            if morning_count / max(total, 1) > 0.3:
                types.append("早起鸟")
        
        return types if types else ["普通用户"]
    
    def _empty_profile(self, user_id: int) -> Dict:
        """返回空的用户画像"""
        return {
            "user_id": user_id,
            "basic_info": {},
            "interest_tags": [],
            "category_preference": [],
            "activity_pattern": {"hourly": [], "daily": [], "peak_hours": [], "active_days": []},
            "behavior_stats": {
                "total_interactions": 0,
                "like_count": 0,
                "favorite_count": 0,
                "comment_count": 0,
                "unique_articles": 0,
                "avg_daily_interactions": 0
            },
            "reading_level": {"level": "新手", "score": 0, "description": "暂无阅读记录"},
            "user_type": ["新用户"],
            "generated_at": datetime.now().isoformat()
        }
    
    def get_similar_users(self, user_id: int, top_n: int = 5) -> List[Dict]:
        """
        找出与指定用户兴趣相似的用户
        基于兴趣标签的余弦相似度
        """
        try:
            # 获取目标用户画像
            target_profile = self.analyze_user(user_id)
            target_tags = {t['tag']: t['weight'] for t in target_profile.get('interest_tags', [])}
            
            if not target_tags:
                return []
            
            # 获取所有用户
            all_interactions = self.data_loader.get_user_interactions()
            if all_interactions.empty:
                return []
            
            all_user_ids = all_interactions['user_id'].unique()
            
            similarities = []
            for uid in all_user_ids:
                if uid == user_id:
                    continue
                
                other_profile = self.analyze_user(uid)
                other_tags = {t['tag']: t['weight'] for t in other_profile.get('interest_tags', [])}
                
                if not other_tags:
                    continue
                
                # 计算余弦相似度
                common_tags = set(target_tags.keys()) & set(other_tags.keys())
                if not common_tags:
                    continue
                
                dot_product = sum(target_tags[t] * other_tags[t] for t in common_tags)
                norm1 = np.sqrt(sum(v**2 for v in target_tags.values()))
                norm2 = np.sqrt(sum(v**2 for v in other_tags.values()))
                
                similarity = dot_product / (norm1 * norm2) if norm1 * norm2 > 0 else 0
                
                similarities.append({
                    "user_id": int(uid),
                    "similarity": round(similarity, 3),
                    "common_interests": list(common_tags)[:5]
                })
            
            # 返回最相似的用户
            return sorted(similarities, key=lambda x: x['similarity'], reverse=True)[:top_n]
            
        except Exception as e:
            logger.error(f"查找相似用户失败: {e}")
            return []
