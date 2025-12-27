"""用户分类算法 - 基于K-Means聚类"""
import pandas as pd
import numpy as np
from typing import List, Dict, Optional
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import silhouette_score
from .data_loader import DataLoader
import logging
import time
import sys
import os

# 添加父目录到路径以支持utils导入
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
try:
    from utils.cache_manager import cached
    from utils.performance_monitor import monitor_performance
except ImportError:
    # 如果导入失败，提供空装饰器
    def cached(ttl=3600, prefix="default"):
        def decorator(func):
            return func
        return decorator
    
    def monitor_performance(name=None):
        def decorator(func):
            return func
        return decorator

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
        self.silhouette_score = 0.0
        
        # 新增：特征重要性和权重管理
        self.feature_importance = {}
        self.feature_weights = {}
        self.feature_selection_threshold = 0.01  # 特征选择阈值
        
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
    
    @monitor_performance("user_clustering.train")
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
            
            # 标准化特征 - 包含所有新增特征维度
            feature_cols = ['article_count', 'video_count', 'comment_count', 
                          'like_count', 'favorite_count', 'view_count', 'avg_read_time', 
                          'activity_score', 'content_diversity', 'social_score',
                          'device_diversity', 'active_hours_pattern', 'reading_depth',
                          'content_preference_strength', 'interaction_quality', 
                          'temporal_consistency', 'influence_score']
            X = features_df[feature_cols].fillna(0)
            
            # 特征重要性评估和选择
            selected_features = self._evaluate_and_select_features(X, feature_cols)
            X_selected = X[selected_features]
            
            # 特征标准化和权重调整
            X_scaled = self._apply_feature_weights_and_scaling(X_selected, selected_features)
            
            # 自动选择最优聚类数
            best_k = self._find_optimal_clusters(X_scaled)
            if best_k != self.n_clusters:
                logger.info(f"调整聚类数从 {self.n_clusters} 到 {best_k}")
                self.n_clusters = best_k
            
            # K-Means聚类
            self.model = KMeans(n_clusters=self.n_clusters, random_state=42, n_init=10)
            self.cluster_labels = self.model.fit_predict(X_scaled)
            
            # 计算聚类质量
            if len(set(self.cluster_labels)) > 1:
                self.silhouette_score = silhouette_score(X_scaled, self.cluster_labels)
                logger.info(f"聚类质量评分 (Silhouette Score): {self.silhouette_score:.3f}")
            
            # 保存结果
            features_df['cluster'] = self.cluster_labels
            self.user_features = features_df
            
            # 分析每个簇的特征，重新映射到有意义的标签
            self._analyze_clusters()
            
            self.last_train_time = time.time()
            logger.info(f"用户聚类模型训练完成，耗时{time.time()-start_time:.2f}秒")
            
        except Exception as e:
            logger.error(f"用户聚类训练失败: {e}")
    
    def _find_optimal_clusters(self, X_scaled: np.ndarray) -> int:
        """使用多种指标找到最优聚类数（增强版）"""
        if len(X_scaled) < 10:  # 数据太少，使用默认值
            return min(self.n_clusters, len(X_scaled) - 1)
        
        max_k = min(10, len(X_scaled) - 1)
        
        # 多种评估指标
        silhouette_scores = []
        calinski_harabasz_scores = []
        davies_bouldin_scores = []
        inertias = []
        stability_scores = []
        
        from sklearn.metrics import calinski_harabasz_score, davies_bouldin_score
        
        logger.info("开始多指标聚类数优化...")
        
        for k in range(2, max_k + 1):
            # 多次运行评估稳定性
            k_silhouette_scores = []
            k_inertias = []
            
            for run in range(3):  # 运行3次评估稳定性
                kmeans = KMeans(n_clusters=k, random_state=42+run, n_init=10)
                labels = kmeans.fit_predict(X_scaled)
                
                # 轮廓系数
                sil_score = silhouette_score(X_scaled, labels)
                k_silhouette_scores.append(sil_score)
                k_inertias.append(kmeans.inertia_)
            
            # 使用第一次运行的结果计算其他指标
            kmeans = KMeans(n_clusters=k, random_state=42, n_init=10)
            labels = kmeans.fit_predict(X_scaled)
            
            # 收集各种评估指标
            avg_silhouette = np.mean(k_silhouette_scores)
            silhouette_scores.append(avg_silhouette)
            
            calinski_score = calinski_harabasz_score(X_scaled, labels)
            calinski_harabasz_scores.append(calinski_score)
            
            davies_bouldin = davies_bouldin_score(X_scaled, labels)
            davies_bouldin_scores.append(davies_bouldin)
            
            avg_inertia = np.mean(k_inertias)
            inertias.append(avg_inertia)
            
            # 稳定性评分（标准差越小越稳定）
            stability = 1.0 / (1.0 + np.std(k_silhouette_scores))
            stability_scores.append(stability)
            
            logger.debug(f"k={k}: 轮廓系数={avg_silhouette:.3f}, "
                        f"Calinski-Harabasz={calinski_score:.1f}, "
                        f"Davies-Bouldin={davies_bouldin:.3f}, "
                        f"稳定性={stability:.3f}")
        
        # 综合评分选择最优k
        best_k = self._calculate_optimal_k_score(
            silhouette_scores, calinski_harabasz_scores, 
            davies_bouldin_scores, inertias, stability_scores
        )
        
        logger.info(f"最优聚类数选择: k={best_k}")
        return best_k
    
    def _calculate_optimal_k_score(self, silhouette_scores: List[float], 
                                 calinski_scores: List[float],
                                 davies_bouldin_scores: List[float],
                                 inertias: List[float],
                                 stability_scores: List[float]) -> int:
        """计算综合评分选择最优k"""
        
        # 归一化各个指标到0-1范围
        def normalize_scores(scores, reverse=False):
            min_score, max_score = min(scores), max(scores)
            if max_score == min_score:
                return [0.5] * len(scores)
            normalized = [(s - min_score) / (max_score - min_score) for s in scores]
            return [1 - n for n in normalized] if reverse else normalized
        
        # 归一化各指标（Davies-Bouldin越小越好，需要反转）
        norm_silhouette = normalize_scores(silhouette_scores)
        norm_calinski = normalize_scores(calinski_scores)
        norm_davies_bouldin = normalize_scores(davies_bouldin_scores, reverse=True)
        norm_stability = normalize_scores(stability_scores)
        
        # 肘部法则评分（惯性下降率）
        elbow_scores = self._calculate_elbow_scores(inertias)
        
        # 综合评分（各指标权重）
        weights = {
            'silhouette': 0.3,      # 轮廓系数权重
            'calinski': 0.2,        # Calinski-Harabasz权重
            'davies_bouldin': 0.2,  # Davies-Bouldin权重
            'stability': 0.2,       # 稳定性权重
            'elbow': 0.1           # 肘部法则权重
        }
        
        composite_scores = []
        for i in range(len(silhouette_scores)):
            score = (weights['silhouette'] * norm_silhouette[i] +
                    weights['calinski'] * norm_calinski[i] +
                    weights['davies_bouldin'] * norm_davies_bouldin[i] +
                    weights['stability'] * norm_stability[i] +
                    weights['elbow'] * elbow_scores[i])
            composite_scores.append(score)
        
        # 选择综合评分最高的k
        best_idx = composite_scores.index(max(composite_scores))
        best_k = best_idx + 2  # k从2开始
        
        logger.info(f"综合评分: {[f'{s:.3f}' for s in composite_scores]}")
        
        return best_k
    
    def _calculate_elbow_scores(self, inertias: List[float]) -> List[float]:
        """计算肘部法则评分"""
        if len(inertias) < 2:
            return [0.5] * len(inertias)
        
        # 计算惯性下降率
        decreases = []
        for i in range(1, len(inertias)):
            decrease = (inertias[i-1] - inertias[i]) / inertias[i-1]
            decreases.append(decrease)
        
        # 找到下降率变化最大的点（肘部）
        if len(decreases) < 2:
            return [0.5] * len(inertias)
        
        elbow_scores = [0.0]  # k=2的评分
        for i in range(1, len(decreases)):
            # 下降率的变化（二阶导数）
            change = decreases[i-1] - decreases[i]
            elbow_scores.append(max(0, change))
        
        # 归一化肘部评分
        if max(elbow_scores) > 0:
            max_score = max(elbow_scores)
            elbow_scores = [s / max_score for s in elbow_scores]
        
        return elbow_scores
    
    @monitor_performance("user_clustering.extract_features")
    def _extract_user_features(self) -> pd.DataFrame:
        """提取用户特征（增强版）- 多维度特征提取"""
        users_df = self.data_loader.get_users()
        articles_df = self.data_loader.get_articles()
        videos_df = self.data_loader.get_videos()
        interactions_df = self.data_loader.get_user_interactions()
        
        if users_df.empty:
            return pd.DataFrame()
        
        features = []
        for _, user in users_df.iterrows():
            user_id = user['id']
            
            # 基础统计
            article_count = len(articles_df[articles_df['author_id'] == user_id]) if not articles_df.empty else 0
            video_count = len(videos_df[videos_df['author_id'] == user_id]) if not videos_df.empty else 0
            
            # 用户交互统计
            user_interactions = interactions_df[interactions_df['user_id'] == user_id] if not interactions_df.empty else pd.DataFrame()
            comment_count = len(user_interactions[user_interactions['action_type'] == 'comment'])
            like_count = len(user_interactions[user_interactions['action_type'] == 'like'])
            favorite_count = len(user_interactions[user_interactions['action_type'] == 'favorite'])
            
            # 计算活跃度评分
            activity_score = self._calculate_activity_score(article_count, video_count, comment_count, like_count)
            
            # 计算内容多样性
            content_diversity = self._calculate_content_diversity(user_interactions, articles_df)
            
            # 计算社交评分
            social_score = self._calculate_social_score(comment_count, like_count, favorite_count)
            
            # 新增特征维度
            # 1. 设备类型多样性
            device_diversity = self._calculate_device_diversity(user_id)
            
            # 2. 活跃时段模式
            active_hours_pattern = self._calculate_active_hours_pattern(user_interactions)
            
            # 3. 阅读深度评分
            reading_depth = self._calculate_reading_depth(user_interactions, articles_df)
            
            # 4. 内容偏好强度
            content_preference_strength = self._calculate_content_preference_strength(user_interactions, articles_df)
            
            # 5. 互动质量评分
            interaction_quality = self._calculate_interaction_quality(user_interactions)
            
            # 6. 时间一致性评分
            temporal_consistency = self._calculate_temporal_consistency(user_interactions)
            
            # 7. 影响力评分
            influence_score = self._calculate_influence_score(user_id, articles_df, videos_df)
            
            # 模拟其他特征（基于真实数据模式）
            view_count = max(0, int(np.random.poisson(50) + activity_score * 10))
            avg_read_time = max(0.5, np.random.exponential(3) + reading_depth)
            
            features.append({
                'user_id': user_id,
                'username': user.get('username', ''),
                'article_count': article_count,
                'video_count': video_count,
                'comment_count': comment_count,
                'like_count': like_count,
                'favorite_count': favorite_count,
                'view_count': view_count,
                'avg_read_time': avg_read_time,
                'activity_score': activity_score,
                'content_diversity': content_diversity,
                'social_score': social_score,
                # 新增特征维度
                'device_diversity': device_diversity,
                'active_hours_pattern': active_hours_pattern,
                'reading_depth': reading_depth,
                'content_preference_strength': content_preference_strength,
                'interaction_quality': interaction_quality,
                'temporal_consistency': temporal_consistency,
                'influence_score': influence_score
            })
        
        return pd.DataFrame(features)
    
    def _calculate_activity_score(self, article_count: int, video_count: int, 
                                comment_count: int, like_count: int) -> float:
        """计算用户活跃度评分"""
        # 权重：发布内容 > 评论 > 点赞
        score = (article_count * 5 + video_count * 4 + 
                comment_count * 2 + like_count * 1)
        return min(score / 10.0, 10.0)  # 归一化到0-10
    
    def _calculate_content_diversity(self, user_interactions: pd.DataFrame, 
                                   articles_df: pd.DataFrame) -> float:
        """计算用户内容多样性"""
        if user_interactions.empty or articles_df.empty:
            return 0.0
        
        # 获取用户交互的文章类型
        interacted_articles = user_interactions[user_interactions['article_id'].notna()]
        if interacted_articles.empty:
            return 0.0
        
        article_ids = interacted_articles['article_id'].unique()
        board_types = articles_df[articles_df['id'].isin(article_ids)]['board_type'].unique()
        
        # 多样性 = 涉及的板块数量 / 总板块数量
        total_board_types = articles_df['board_type'].nunique()
        diversity = len(board_types) / max(total_board_types, 1)
        return min(diversity, 1.0)
    
    def _calculate_social_score(self, comment_count: int, like_count: int, 
                              favorite_count: int) -> float:
        """计算社交评分"""
        # 评论权重最高，收藏次之，点赞最低
        score = comment_count * 3 + favorite_count * 2 + like_count * 1
        return min(score / 5.0, 10.0)  # 归一化到0-10
    
    def _calculate_device_diversity(self, user_id: int) -> float:
        """计算设备类型多样性"""
        # 模拟设备类型数据（实际应从用户会话日志获取）
        device_types = ['mobile', 'desktop', 'tablet']
        # 基于用户ID生成一致的随机设备使用模式
        np.random.seed(user_id)
        used_devices = np.random.choice(device_types, size=np.random.randint(1, 4), replace=False)
        diversity = len(used_devices) / len(device_types)
        return diversity
    
    def _calculate_active_hours_pattern(self, user_interactions: pd.DataFrame) -> float:
        """计算活跃时段模式评分"""
        if user_interactions.empty:
            return 0.0
        
        # 模拟时间分布（实际应从interaction时间戳计算）
        # 计算活跃时段的集中度，越分散评分越高
        hours = np.random.choice(24, size=len(user_interactions), replace=True)
        unique_hours = len(np.unique(hours))
        # 归一化：活跃时段越多样化，评分越高
        pattern_score = unique_hours / 24.0
        return min(pattern_score, 1.0)
    
    def _calculate_reading_depth(self, user_interactions: pd.DataFrame, 
                               articles_df: pd.DataFrame) -> float:
        """计算阅读深度评分"""
        if user_interactions.empty or articles_df.empty:
            return 0.0
        
        # 基于用户交互类型计算阅读深度
        read_actions = user_interactions[user_interactions['action_type'].isin(['view', 'like', 'favorite'])]
        if read_actions.empty:
            return 0.0
        
        # 收藏和点赞表示深度阅读
        deep_read_count = len(user_interactions[user_interactions['action_type'].isin(['favorite', 'comment'])])
        total_read_count = len(read_actions)
        
        depth_ratio = deep_read_count / max(total_read_count, 1)
        return min(depth_ratio * 10, 10.0)
    
    def _calculate_content_preference_strength(self, user_interactions: pd.DataFrame, 
                                             articles_df: pd.DataFrame) -> float:
        """计算内容偏好强度"""
        if user_interactions.empty or articles_df.empty:
            return 0.0
        
        # 计算用户在不同内容类型上的分布集中度
        interacted_articles = user_interactions[user_interactions['article_id'].notna()]
        if interacted_articles.empty:
            return 0.0
        
        article_ids = interacted_articles['article_id'].unique()
        board_types = articles_df[articles_df['id'].isin(article_ids)]['board_type'].value_counts()
        
        if len(board_types) == 0:
            return 0.0
        
        # 使用基尼系数衡量偏好集中度
        total = board_types.sum()
        proportions = board_types / total
        gini = 1 - sum(proportions ** 2)
        
        # 转换为偏好强度（1-gini表示集中度）
        preference_strength = (1 - gini) * 10
        return min(preference_strength, 10.0)
    
    def _calculate_interaction_quality(self, user_interactions: pd.DataFrame) -> float:
        """计算互动质量评分"""
        if user_interactions.empty:
            return 0.0
        
        # 基于互动类型的质量权重
        quality_weights = {
            'comment': 5,  # 评论质量最高
            'favorite': 3,  # 收藏次之
            'like': 1,      # 点赞最低
            'view': 0.5     # 浏览最低
        }
        
        total_quality = 0
        total_interactions = len(user_interactions)
        
        for action_type, weight in quality_weights.items():
            count = len(user_interactions[user_interactions['action_type'] == action_type])
            total_quality += count * weight
        
        # 归一化质量评分
        avg_quality = total_quality / max(total_interactions, 1)
        return min(avg_quality, 10.0)
    
    def _calculate_temporal_consistency(self, user_interactions: pd.DataFrame) -> float:
        """计算时间一致性评分"""
        if user_interactions.empty:
            return 0.0
        
        # 模拟时间一致性计算（实际应基于真实时间戳）
        # 一致性高表示用户有规律的使用习惯
        interaction_count = len(user_interactions)
        
        # 基于交互频率计算一致性
        if interaction_count < 5:
            return 2.0  # 交互太少，一致性较低
        elif interaction_count < 20:
            return 5.0  # 中等一致性
        else:
            return 8.0  # 高一致性
    
    def _calculate_influence_score(self, user_id: int, articles_df: pd.DataFrame, 
                                 videos_df: pd.DataFrame) -> float:
        """计算用户影响力评分"""
        # 基于用户创作内容的影响力
        user_articles = articles_df[articles_df['author_id'] == user_id] if not articles_df.empty else pd.DataFrame()
        user_videos = videos_df[videos_df['author_id'] == user_id] if not videos_df.empty else pd.DataFrame()
        
        # 计算总浏览量和互动量
        total_article_views = user_articles['view_count'].sum() if not user_articles.empty and 'view_count' in user_articles.columns else 0
        total_video_views = user_videos['view_count'].sum() if not user_videos.empty and 'view_count' in user_videos.columns else 0
        
        total_views = total_article_views + total_video_views
        
        # 基于内容数量和浏览量计算影响力
        content_count = len(user_articles) + len(user_videos)
        if content_count == 0:
            return 0.0
        
        avg_views_per_content = total_views / content_count
        influence = min(avg_views_per_content / 100.0, 10.0)  # 归一化到0-10
        
        return influence
    
    def _analyze_clusters(self):
        """分析聚类结果，智能映射到有意义的用户类型（增强版）"""
        if self.user_features is None:
            return
        
        # 计算每个簇的特征均值和标准差
        feature_cols_for_analysis = [
            'article_count', 'video_count', 'comment_count', 'like_count', 
            'activity_score', 'social_score', 'reading_depth', 'influence_score',
            'content_diversity', 'interaction_quality', 'temporal_consistency'
        ]
        
        cluster_stats = self.user_features.groupby('cluster').agg({
            col: ['mean', 'std', 'count'] for col in feature_cols_for_analysis
        })
        
        # 扁平化列名
        cluster_stats.columns = ['_'.join(col).strip() for col in cluster_stats.columns.values]
        
        # 智能标签映射和置信度计算
        cluster_mapping = {}
        cluster_confidence = {}
        cluster_explanations = {}
        
        logger.info("开始智能用户类型映射...")
        
        for cluster_id in range(self.n_clusters):
            if cluster_id not in cluster_stats.index:
                continue
                
            # 获取该簇的特征统计
            stats = cluster_stats.loc[cluster_id]
            
            # 计算各种用户类型的匹配度
            type_scores = self._calculate_user_type_scores(stats)
            
            # 选择匹配度最高的类型
            best_type = max(type_scores.items(), key=lambda x: x[1])
            user_type_id = best_type[0]
            confidence = best_type[1]
            
            cluster_mapping[cluster_id] = user_type_id
            cluster_confidence[cluster_id] = confidence
            
            # 生成可解释的说明
            explanation = self._generate_cluster_explanation(stats, user_type_id)
            cluster_explanations[cluster_id] = explanation
            
            logger.info(f"簇{cluster_id} -> {self.cluster_names[user_type_id]['name']} "
                       f"(置信度: {confidence:.2f}) - {explanation}")
        
        # 应用映射
        self.user_features['user_type'] = self.user_features['cluster'].map(
            lambda x: cluster_mapping.get(x, 4)
        )
        
        # 保存映射信息用于解释
        self.cluster_mapping_info = {
            'mapping': cluster_mapping,
            'confidence': cluster_confidence,
            'explanations': cluster_explanations
        }
    
    def _calculate_user_type_scores(self, stats: pd.Series) -> Dict[int, float]:
        """计算各用户类型的匹配度评分"""
        
        # 提取关键特征值
        activity = stats.get('activity_score_mean', 0)
        social = stats.get('social_score_mean', 0)
        article_count = stats.get('article_count_mean', 0)
        video_count = stats.get('video_count_mean', 0)
        reading_depth = stats.get('reading_depth_mean', 0)
        influence = stats.get('influence_score_mean', 0)
        interaction_quality = stats.get('interaction_quality_mean', 0)
        content_diversity = stats.get('content_diversity_mean', 0)
        
        type_scores = {}
        
        # 0: 活跃创作者 - 高活跃度 + 高创作量 + 高影响力
        creator_score = (
            min(activity / 8.0, 1.0) * 0.4 +           # 活跃度权重40%
            min((article_count + video_count) / 5.0, 1.0) * 0.3 +  # 创作量权重30%
            min(influence / 5.0, 1.0) * 0.2 +          # 影响力权重20%
            min(interaction_quality / 8.0, 1.0) * 0.1  # 互动质量权重10%
        )
        type_scores[0] = creator_score
        
        # 1: 深度阅读者 - 高阅读深度 + 中等活跃度 + 高内容多样性
        reader_score = (
            min(reading_depth / 8.0, 1.0) * 0.4 +      # 阅读深度权重40%
            min(content_diversity, 1.0) * 0.3 +        # 内容多样性权重30%
            min(activity / 6.0, 1.0) * 0.2 +           # 适中活跃度权重20%
            (1.0 - min(video_count / 3.0, 1.0)) * 0.1  # 偏好文章权重10%
        )
        type_scores[1] = reader_score
        
        # 2: 社交达人 - 高社交评分 + 高互动质量 + 中等活跃度
        social_score = (
            min(social / 8.0, 1.0) * 0.4 +             # 社交评分权重40%
            min(interaction_quality / 8.0, 1.0) * 0.3 + # 互动质量权重30%
            min(activity / 6.0, 1.0) * 0.2 +           # 活跃度权重20%
            min(stats.get('comment_count_mean', 0) / 10.0, 1.0) * 0.1  # 评论数权重10%
        )
        type_scores[2] = social_score
        
        # 3: 视频爱好者 - 高视频偏好 + 中等活跃度
        video_lover_score = (
            min(video_count / max(article_count + 1, 1), 1.0) * 0.4 +  # 视频偏好权重40%
            min(video_count / 3.0, 1.0) * 0.3 +        # 视频数量权重30%
            min(activity / 6.0, 1.0) * 0.2 +           # 活跃度权重20%
            min(interaction_quality / 6.0, 1.0) * 0.1  # 互动质量权重10%
        )
        type_scores[3] = video_lover_score
        
        # 4: 潜水用户 - 低活跃度 + 低社交评分 + 低创作量
        lurker_score = (
            (1.0 - min(activity / 4.0, 1.0)) * 0.4 +   # 低活跃度权重40%
            (1.0 - min(social / 4.0, 1.0)) * 0.3 +     # 低社交权重30%
            (1.0 - min((article_count + video_count) / 2.0, 1.0)) * 0.2 +  # 低创作权重20%
            (1.0 - min(interaction_quality / 4.0, 1.0)) * 0.1  # 低互动质量权重10%
        )
        type_scores[4] = lurker_score
        
        return type_scores
    
    def _generate_cluster_explanation(self, stats: pd.Series, user_type_id: int) -> str:
        """生成聚类结果的可解释说明"""
        
        activity = stats.get('activity_score_mean', 0)
        social = stats.get('social_score_mean', 0)
        article_count = stats.get('article_count_mean', 0)
        video_count = stats.get('video_count_mean', 0)
        reading_depth = stats.get('reading_depth_mean', 0)
        influence = stats.get('influence_score_mean', 0)
        
        explanations = {
            0: f"平均活跃度{activity:.1f}，创作{article_count:.1f}篇文章和{video_count:.1f}个视频，影响力{influence:.1f}",
            1: f"平均阅读深度{reading_depth:.1f}，活跃度{activity:.1f}，偏好文章内容",
            2: f"平均社交评分{social:.1f}，活跃度{activity:.1f}，互动频繁",
            3: f"平均创作{video_count:.1f}个视频，活跃度{activity:.1f}，偏好视频内容",
            4: f"平均活跃度{activity:.1f}，社交评分{social:.1f}，以浏览为主"
        }
        
        return explanations.get(user_type_id, "特征不明显")
    
    def get_cluster_analysis_info(self) -> Dict:
        """获取聚类分析的详细信息"""
        if not hasattr(self, 'cluster_mapping_info'):
            return {}
        
        return self.cluster_mapping_info.copy()
    
    @cached(ttl=1800, prefix="user_clustering")
    @monitor_performance("user_clustering.get_user_type")
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
        user_stats = {
            'activity_score': float(user_row['activity_score'].iloc[0]),
            'social_score': float(user_row['social_score'].iloc[0]),
            'content_diversity': float(user_row['content_diversity'].iloc[0])
        }
        
        return {
            'type': user_type,
            'user_id': user_id,
            'stats': user_stats,
            **self.cluster_names.get(user_type, self.cluster_names[4])
        }
    
    @cached(ttl=3600, prefix="user_clustering")
    @monitor_performance("user_clustering.get_all_user_types")
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
                'activity_score': float(row.get('activity_score', 0)),
                'social_score': float(row.get('social_score', 0)),
                **self.cluster_names.get(user_type, self.cluster_names[4])
            })
        
        return results
    
    @cached(ttl=3600, prefix="user_clustering")
    @monitor_performance("user_clustering.get_cluster_distribution")
    def get_cluster_distribution(self) -> Dict:
        """获取用户类型分布统计"""
        if self.user_features is None:
            self.train()
        
        if self.user_features is None:
            return {'total': 0, 'distribution': [], 'quality_metrics': {}}
        
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
        
        # 添加质量指标
        quality_metrics = {
            'silhouette_score': round(self.silhouette_score, 3),
            'cluster_count': self.n_clusters,
            'last_train_time': self.last_train_time,
            'data_freshness': 'good' if time.time() - self.last_train_time < 3600 else 'stale'
        }
        
        return {
            'total': total,
            'distribution': sorted(distribution, key=lambda x: x['count'], reverse=True),
            'quality_metrics': quality_metrics
        }
    def _evaluate_and_select_features(self, X: pd.DataFrame, feature_cols: List[str]) -> List[str]:
        """评估特征重要性并选择有效特征"""
        from sklearn.feature_selection import VarianceThreshold, SelectKBest, f_classif
        from sklearn.ensemble import RandomForestClassifier
        
        # 1. 方差阈值过滤（移除低方差特征）
        variance_selector = VarianceThreshold(threshold=self.feature_selection_threshold)
        X_variance_filtered = variance_selector.fit_transform(X)
        selected_by_variance = [feature_cols[i] for i in range(len(feature_cols)) 
                               if variance_selector.get_support()[i]]
        
        logger.info(f"方差过滤后保留特征: {len(selected_by_variance)}/{len(feature_cols)}")
        
        # 2. 如果特征数量仍然很多，使用随机森林评估特征重要性
        if len(selected_by_variance) > 10:
            try:
                # 使用K-means预聚类作为目标变量
                from sklearn.cluster import KMeans
                temp_kmeans = KMeans(n_clusters=min(5, len(X)), random_state=42)
                temp_labels = temp_kmeans.fit_predict(X[selected_by_variance])
                
                # 训练随机森林评估特征重要性
                rf = RandomForestClassifier(n_estimators=100, random_state=42)
                rf.fit(X[selected_by_variance], temp_labels)
                
                # 记录特征重要性
                feature_importance = dict(zip(selected_by_variance, rf.feature_importances_))
                self.feature_importance = feature_importance
                
                # 选择重要性较高的特征
                sorted_features = sorted(feature_importance.items(), key=lambda x: x[1], reverse=True)
                selected_features = [f[0] for f in sorted_features[:12]]  # 保留前12个重要特征
                
                logger.info(f"特征重要性排序: {sorted_features[:5]}")
                
            except Exception as e:
                logger.warning(f"特征重要性评估失败，使用方差过滤结果: {e}")
                selected_features = selected_by_variance
        else:
            selected_features = selected_by_variance
        
        return selected_features
    
    def _apply_feature_weights_and_scaling(self, X: pd.DataFrame, feature_cols: List[str]) -> np.ndarray:
        """应用特征权重并进行标准化"""
        # 定义特征权重（基于业务理解）
        default_weights = {
            'activity_score': 1.5,      # 活跃度权重较高
            'social_score': 1.3,        # 社交评分权重较高
            'influence_score': 1.4,     # 影响力权重较高
            'content_diversity': 1.2,   # 内容多样性权重较高
            'reading_depth': 1.1,       # 阅读深度权重中等
            'interaction_quality': 1.2, # 互动质量权重较高
            'temporal_consistency': 0.9, # 时间一致性权重较低
            'device_diversity': 0.8,    # 设备多样性权重较低
            'active_hours_pattern': 0.8, # 活跃时段权重较低
            'content_preference_strength': 1.0  # 内容偏好权重中等
        }
        
        # 应用权重
        X_weighted = X.copy()
        for col in feature_cols:
            weight = default_weights.get(col, 1.0)
            X_weighted[col] = X_weighted[col] * weight
            self.feature_weights[col] = weight
        
        # 标准化
        X_scaled = self.scaler.fit_transform(X_weighted)
        
        logger.info(f"应用特征权重: {self.feature_weights}")
        
        return X_scaled
    
    def get_feature_importance(self) -> Dict[str, float]:
        """获取特征重要性评分"""
        return self.feature_importance.copy()
    
    def get_feature_weights(self) -> Dict[str, float]:
        """获取特征权重配置"""
        return self.feature_weights.copy()
    
    def update_feature_weights(self, new_weights: Dict[str, float]) -> None:
        """更新特征权重配置"""
        self.feature_weights.update(new_weights)
        logger.info(f"更新特征权重: {new_weights}")
        
        # 权重更新后需要重新训练
        if self.user_features is not None:
            logger.info("特征权重已更新，建议重新训练模型")