"""视频推荐系统 - 增强版"""
import pandas as pd
import numpy as np
from typing import List, Dict, Optional
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from .data_loader import DataLoader
import logging
import time
import jieba

logger = logging.getLogger(__name__)


class VideoRecommender:
    """视频推荐器 - 多维度混合推荐
    
    推荐依据：
    1. Wilson Score - 综合播放量、点赞率、互动率
    2. 内容相似度 - 基于标题和描述的TF-IDF相似度
    3. 用户行为协同过滤 - 相似用户喜欢的视频
    4. 分类偏好 - 用户历史观看的分类
    5. 时效性 - 新视频加权
    6. 多样性优化 - 避免推荐过于集中
    """
    
    def __init__(self, data_loader: DataLoader, config: dict):
        self.data_loader = data_loader
        self.config = config
        self.videos_df = None
        self.hot_videos = []
        self.wilson_scores = {}
        self.content_similarity = None
        self.video_id_to_idx = {}
        self.idx_to_video_id = {}
        self.last_train_time = 0
        self.train_interval = 3600
    
    def train(self):
        """训练视频推荐模型"""
        logger.info("=" * 50)
        logger.info("开始训练视频推荐模型...")
        start_time = time.time()
        
        self.videos_df = self.data_loader.get_videos()
        
        if self.videos_df.empty:
            logger.warning("没有视频数据，跳过训练")
            return
        
        logger.info(f"加载视频数量: {len(self.videos_df)}")
        
        # 1. 计算Wilson Score
        self._compute_wilson_scores()
        
        # 2. 计算热门视频
        self._compute_hot_videos()
        
        # 3. 构建内容相似度矩阵
        self._build_content_similarity()
        
        self.last_train_time = time.time()
        logger.info(f"视频推荐模型训练完成，耗时: {time.time() - start_time:.2f}秒")
        logger.info("=" * 50)
    
    def _compute_wilson_scores(self):
        """计算Wilson Score（置信区间下界）
        
        Wilson Score考虑了样本量，避免少量高赞视频排名过高
        公式: (p + z²/2n - z*sqrt(p(1-p)/n + z²/4n²)) / (1 + z²/n)
        """
        logger.info("计算Wilson Score...")
        z = 1.96  # 95%置信度
        
        for _, row in self.videos_df.iterrows():
            video_id = row['id']
            views = max(row.get('view_count', 0) or 0, 1)
            likes = row.get('like_count', 0) or 0
            comments = row.get('comment_count', 0) or 0
            
            # 互动率 = (点赞 + 评论*2) / 播放量
            interactions = likes + comments * 2
            p = min(interactions / views, 1.0)
            n = views
            
            # Wilson Score
            if n > 0:
                wilson = (p + (z**2)/(2*n) - z*np.sqrt((p*(1-p) + (z**2)/(4*n))/n)) / (1 + (z**2)/n)
            else:
                wilson = 0
            
            self.wilson_scores[video_id] = max(wilson, 0)
    
    def _compute_hot_videos(self):
        """计算热门视频排行（综合多维度）"""
        logger.info("计算热门视频排行...")
        if self.videos_df is None or self.videos_df.empty:
            return
        
        df = self.videos_df.copy()
        
        # Wilson Score
        df['wilson_score'] = df['id'].map(self.wilson_scores).fillna(0)
        
        # 时间衰减（7天半衰期）
        df['created_at'] = pd.to_datetime(df['created_at'])
        now = pd.Timestamp.now()
        df['days_ago'] = (now - df['created_at']).dt.total_seconds() / 86400
        df['time_decay'] = np.power(0.5, df['days_ago'] / 7)
        
        # 播放量归一化
        max_views = df['view_count'].max() or 1
        df['view_norm'] = df['view_count'].fillna(0) / max_views
        
        # 综合得分 = Wilson*0.4 + 播放量归一化*0.3 + 时间衰减*0.3
        df['final_score'] = (
            df['wilson_score'] * 0.4 +
            df['view_norm'] * 0.3 +
            df['time_decay'] * 0.3
        ) * 100  # 放大到0-100
        
        self.hot_videos = df.nlargest(100, 'final_score')[['id', 'final_score']].values.tolist()
        logger.info(f"热门视频计算完成，共{len(self.hot_videos)}个")
    
    def _build_content_similarity(self):
        """构建内容相似度矩阵（基于TF-IDF）"""
        logger.info("构建内容相似度矩阵...")
        if self.videos_df is None or len(self.videos_df) < 2:
            return
        
        # 构建ID映射
        self.video_id_to_idx = {vid: idx for idx, vid in enumerate(self.videos_df['id'])}
        self.idx_to_video_id = {idx: vid for vid, idx in self.video_id_to_idx.items()}
        
        # 合并标题和描述作为文本特征
        texts = []
        for _, row in self.videos_df.iterrows():
            title = str(row.get('title', '') or '')
            desc = str(row.get('description', '') or '')
            category = str(row.get('category_name', '') or '')
            # 分词
            text = ' '.join(jieba.cut(f"{title} {title} {desc} {category}"))  # 标题权重加倍
            texts.append(text)
        
        # TF-IDF向量化
        try:
            vectorizer = TfidfVectorizer(max_features=1000, min_df=1)
            tfidf_matrix = vectorizer.fit_transform(texts)
            self.content_similarity = cosine_similarity(tfidf_matrix)
            logger.info(f"内容相似度矩阵构建完成，维度: {self.content_similarity.shape}")
        except Exception as e:
            logger.error(f"构建相似度矩阵失败: {e}")
            self.content_similarity = None

    
    def _should_retrain(self) -> bool:
        return time.time() - self.last_train_time > self.train_interval
    
    def recommend(self, user_id: Optional[int] = None, top_n: int = 10,
                  exclude_ids: List[int] = None, category_id: Optional[int] = None) -> List[Dict]:
        """视频推荐主入口"""
        if self._should_retrain():
            self.train()
        
        exclude_ids = exclude_ids or []
        
        logger.info(f"=== 视频推荐请求 === user_id={user_id}, top_n={top_n}")
        
        if user_id is None:
            logger.info("推荐策略: 热门推荐（未登录用户）")
            results = self._get_hot_recommendations(top_n * 2, exclude_ids, category_id)
        else:
            user_history = self.data_loader.get_user_video_history(user_id)
            user_profile = self.data_loader.get_user_video_profile(user_id)
            
            logger.info(f"用户历史交互数: {len(user_history)}")
            
            if len(user_history) < 3:
                logger.info("推荐策略: 冷启动推荐")
                results = self._get_cold_start_recommendations(user_id, top_n * 2, exclude_ids, category_id)
            else:
                logger.info("推荐策略: 个性化混合推荐")
                results = self._get_personalized_recommendations(user_id, user_history, user_profile, top_n * 2, exclude_ids, category_id)
        
        # 多样性优化
        results = self._diversify_results(results, top_n)
        
        logger.info(f"最终推荐数量: {len(results)}")
        return results
    
    def _get_hot_recommendations(self, top_n: int, exclude_ids: List[int], 
                                  category_id: Optional[int] = None) -> List[Dict]:
        """热门视频推荐"""
        exclude_set = set(exclude_ids)
        results = []
        
        for video_id, score in self.hot_videos:
            if video_id in exclude_set:
                continue
            
            if category_id is not None and self.videos_df is not None:
                video_row = self.videos_df[self.videos_df['id'] == video_id]
                if not video_row.empty and video_row.iloc[0]['category_id'] != category_id:
                    continue
            
            # 获取视频信息用于生成推荐理由
            reason = self._generate_hot_reason(video_id)
            
            results.append({
                "video_id": int(video_id),
                "score": float(score),
                "reason": reason
            })
            if len(results) >= top_n:
                break
        
        return results
    
    def _generate_hot_reason(self, video_id: int) -> str:
        """生成热门推荐理由"""
        if self.videos_df is None:
            return "热门推荐"
        
        video_row = self.videos_df[self.videos_df['id'] == video_id]
        if video_row.empty:
            return "热门推荐"
        
        row = video_row.iloc[0]
        views = row.get('view_count', 0) or 0
        likes = row.get('like_count', 0) or 0
        
        if views >= 100:
            return f"🔥 {views}次播放"
        elif likes >= 10:
            return f"👍 {likes}人点赞"
        else:
            return "热门推荐"
    
    def _get_cold_start_recommendations(self, user_id: int, top_n: int,
                                         exclude_ids: List[int], 
                                         category_id: Optional[int] = None) -> List[Dict]:
        """冷启动推荐"""
        user_profile = self.data_loader.get_user_video_profile(user_id)
        
        # 70%热门 + 30%分类偏好
        hot_count = int(top_n * 0.7)
        pref_count = top_n - hot_count
        
        results = self._get_hot_recommendations(hot_count, exclude_ids, category_id)
        used_ids = set(exclude_ids + [r['video_id'] for r in results])
        
        if user_profile.get('liked_categories'):
            pref_recs = self._recommend_by_categories(
                user_profile['liked_categories'], pref_count, list(used_ids)
            )
            results.extend(pref_recs)
        
        return results
    
    def _get_personalized_recommendations(self, user_id: int, user_history: List[int],
                                           user_profile: dict, top_n: int, 
                                           exclude_ids: List[int],
                                           category_id: Optional[int] = None) -> List[Dict]:
        """个性化混合推荐
        
        策略：
        - 40% 基于内容相似度（用户看过的视频的相似视频）
        - 30% 基于分类偏好
        - 30% 热门补充
        """
        all_exclude = list(set(exclude_ids + user_history))
        results = []
        
        # 1. 基于内容相似度推荐 (40%)
        content_count = int(top_n * 0.4)
        if self.content_similarity is not None and user_history:
            content_recs = self._recommend_by_content_similarity(
                user_history[-10:], content_count, all_exclude
            )
            results.extend(content_recs)
            logger.info(f"内容相似度推荐: {len(content_recs)}个")
        
        # 2. 基于分类偏好推荐 (30%)
        used_ids = all_exclude + [r['video_id'] for r in results]
        pref_count = int(top_n * 0.3)
        liked_categories = user_profile.get('liked_categories', [])
        if liked_categories:
            pref_recs = self._recommend_by_categories(liked_categories, pref_count, used_ids)
            results.extend(pref_recs)
            logger.info(f"分类偏好推荐: {len(pref_recs)}个")
        
        # 3. 热门补充 (30%)
        used_ids = all_exclude + [r['video_id'] for r in results]
        hot_count = top_n - len(results)
        if hot_count > 0:
            hot_recs = self._get_hot_recommendations(hot_count, used_ids, category_id)
            results.extend(hot_recs)
            logger.info(f"热门补充: {len(hot_recs)}个")
        
        return results
    
    def _recommend_by_content_similarity(self, watched_ids: List[int], top_n: int,
                                          exclude_ids: List[int]) -> List[Dict]:
        """基于内容相似度推荐"""
        if self.content_similarity is None:
            return []
        
        exclude_set = set(exclude_ids)
        candidate_scores = {}
        
        for watched_id in watched_ids:
            if watched_id not in self.video_id_to_idx:
                continue
            
            idx = self.video_id_to_idx[watched_id]
            similarities = self.content_similarity[idx]
            
            for other_idx, sim in enumerate(similarities):
                other_id = self.idx_to_video_id.get(other_idx)
                if other_id is None or other_id in exclude_set or other_id == watched_id:
                    continue
                
                if sim > 0.1:  # 相似度阈值
                    if other_id not in candidate_scores:
                        candidate_scores[other_id] = 0
                    candidate_scores[other_id] += sim
        
        # 排序并返回
        sorted_candidates = sorted(candidate_scores.items(), key=lambda x: x[1], reverse=True)
        
        results = []
        for video_id, score in sorted_candidates[:top_n]:
            results.append({
                "video_id": int(video_id),
                "score": float(score * 100),
                "reason": "📺 相似内容推荐"
            })
        
        return results
    
    def _recommend_by_categories(self, categories: List[str], top_n: int,
                                  exclude_ids: List[int]) -> List[Dict]:
        """基于分类推荐"""
        if self.videos_df is None:
            return []
        
        exclude_set = set(exclude_ids)
        results = []
        
        # 筛选指定分类的视频
        for _, row in self.videos_df.iterrows():
            if row['id'] in exclude_set:
                continue
            
            category_name = row.get('category_name', '')
            if category_name in categories:
                wilson = self.wilson_scores.get(row['id'], 0)
                results.append({
                    "video_id": int(row['id']),
                    "score": float(wilson * 100),
                    "reason": f"🏷️ {category_name}"
                })
        
        results.sort(key=lambda x: x['score'], reverse=True)
        return results[:top_n]
    
    def _diversify_results(self, results: List[Dict], top_n: int) -> List[Dict]:
        """多样性优化 - 避免同一分类过多"""
        if not results or self.videos_df is None:
            return results[:top_n]
        
        category_count = {}
        max_per_category = max(2, top_n // 3)  # 每个分类最多占1/3
        
        diversified = []
        remaining = []
        
        for item in results:
            video_id = item['video_id']
            video_row = self.videos_df[self.videos_df['id'] == video_id]
            
            if video_row.empty:
                diversified.append(item)
                continue
            
            category = video_row.iloc[0].get('category_name', 'unknown')
            current_count = category_count.get(category, 0)
            
            if current_count < max_per_category:
                diversified.append(item)
                category_count[category] = current_count + 1
            else:
                remaining.append(item)
        
        # 如果不够，从remaining补充
        while len(diversified) < top_n and remaining:
            diversified.append(remaining.pop(0))
        
        return diversified[:top_n]
    
    def get_similar_videos(self, video_id: int, top_n: int = 10) -> List[Dict]:
        """获取相似视频（基于内容相似度+同分类）"""
        results = []
        
        # 1. 基于内容相似度
        if self.content_similarity is not None and video_id in self.video_id_to_idx:
            idx = self.video_id_to_idx[video_id]
            similarities = self.content_similarity[idx]
            
            sim_scores = []
            for other_idx, sim in enumerate(similarities):
                other_id = self.idx_to_video_id.get(other_idx)
                if other_id and other_id != video_id and sim > 0.1:
                    sim_scores.append((other_id, sim))
            
            sim_scores.sort(key=lambda x: x[1], reverse=True)
            
            for vid, sim in sim_scores[:top_n]:
                results.append({
                    "video_id": int(vid),
                    "score": float(sim * 100),
                    "reason": "📺 相似内容"
                })
        
        # 2. 如果不够，用同分类补充
        if len(results) < top_n and self.videos_df is not None:
            video_row = self.videos_df[self.videos_df['id'] == video_id]
            if not video_row.empty:
                category_id = video_row.iloc[0]['category_id']
                category_name = video_row.iloc[0].get('category_name', '')
                
                used_ids = set([video_id] + [r['video_id'] for r in results])
                same_category = self.videos_df[
                    (self.videos_df['category_id'] == category_id) & 
                    (~self.videos_df['id'].isin(used_ids))
                ].copy()
                
                if not same_category.empty:
                    same_category['score'] = same_category['id'].map(self.wilson_scores).fillna(0)
                    top_same = same_category.nlargest(top_n - len(results), 'score')
                    
                    for _, row in top_same.iterrows():
                        results.append({
                            "video_id": int(row['id']),
                            "score": float(row['score'] * 100),
                            "reason": f"🏷️ 同分类: {category_name}"
                        })
        
        return results[:top_n]
