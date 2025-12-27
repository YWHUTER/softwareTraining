"""热词分析模块 - 提取热门关键词（增强版）"""
import pandas as pd
import numpy as np
from typing import List, Dict, Optional, Tuple
from collections import Counter, defaultdict
import jieba
import jieba.analyse
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.cluster import KMeans
from .data_loader import DataLoader
import logging
import time
import re
from datetime import datetime, timedelta
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

# 扩展停用词库
STOP_WORDS = set([
    '的', '了', '是', '在', '我', '有', '和', '就', '不', '人', '都', '一', '一个',
    '上', '也', '很', '到', '说', '要', '去', '你', '会', '着', '没有', '看', '好',
    '自己', '这', '那', '他', '她', '它', '们', '这个', '那个', '什么', '怎么',
    '可以', '没', '把', '让', '被', '给', '从', '向', '对', '与', '为', '以',
    '及', '等', '但', '而', '或', '如', '若', '虽', '因', '所以', '因为',
    '如果', '只', '还', '又', '再', '已', '已经', '正在', '将', '将要',
    '能', '能够', '应该', '必须', '可能', '需要', '想', '想要', '希望',
    '知道', '觉得', '认为', '感觉', '发现', '看到', '听到', '告诉',
    '问', '回答', '表示', '进行', '开始', '结束', '继续', '完成',
    '使用', '通过', '根据', '按照', '关于', '对于', '由于', '为了',
    '其', '其中', '之', '之一', '之后', '之前', '以后', '以前',
    '现在', '今天', '明天', '昨天', '时候', '时间', '地方', '方面',
    '问题', '情况', '方式', '方法', '过程', '结果', '原因', '目的',
    '内容', '部分', '方向', '位置', '数量', '程度', '范围', '条件',
    '学校', '学生', '老师', '同学', '大学', '学院', '专业', '课程',
    '工作', '生活', '学习', '发展', '建设', '服务', '管理', '活动',
    # 新增领域专用停用词
    '新闻', '报道', '消息', '通知', '公告', '信息', '资讯', '最新',
    '重要', '相关', '主要', '基本', '一般', '特别', '具体', '详细',
    '各种', '多种', '不同', '相同', '类似', '其他', '另外', '此外',
    '首先', '然后', '最后', '总之', '因此', '所以', '但是', '不过',
    '虽然', '尽管', '无论', '不管', '只要', '只有', '除了', '包括'
])

# 领域专用词典
DOMAIN_WORDS = {
    'technology': ['人工智能', '机器学习', '深度学习', '大数据', '云计算', '区块链', '物联网'],
    'education': ['教育', '教学', '培训', '考试', '学术', '研究', '论文', '实验'],
    'campus': ['校园', '宿舍', '食堂', '图书馆', '实验室', '社团', '活动', '比赛'],
    'life': ['生活', '娱乐', '运动', '健康', '美食', '旅游', '购物', '交友']
}


class HotWordsAnalyzer:
    """热词分析器（增强版）- 支持TF-IDF和多维度分析"""
    
    def __init__(self, data_loader: DataLoader):
        self.data_loader = data_loader
        self.hot_words_cache = {}
        self.last_update_time = 0
        self.cache_duration = 1800  # 30分钟缓存
        
        # TF-IDF相关
        self.tfidf_vectorizer = None
        self.tfidf_matrix = None
        self.feature_names = []
        
        # 趋势分析相关
        self.historical_data = defaultdict(list)
        self.trend_cache = {}
        
        # 初始化jieba
        self._init_jieba()
    
    def _init_jieba(self):
        """初始化jieba分词器"""
        # 添加领域专用词典
        for category, words in DOMAIN_WORDS.items():
            for word in words:
                jieba.add_word(word)
        
        logger.info("jieba分词器初始化完成，已加载领域专用词典")
    
    @cached(ttl=1800, prefix="hot_words")
    @monitor_performance("hot_words.get_hot_words")
    def get_hot_words(self, top_n: int = 50, days: int = 7) -> List[Dict]:
        """获取热门关键词（TF-IDF增强版）
        
        Args:
            top_n: 返回数量
            days: 统计天数
            
        Returns:
            热词列表，包含 word, weight, count, trend, category, tfidf_score
        """
        logger.info(f"开始TF-IDF热词分析，统计最近{days}天...")
        start_time = time.time()
        
        # 获取文本数据
        texts, metadata = self._collect_texts(days)
        
        if not texts:
            logger.warning("没有找到文本数据")
            return []
        
        # 使用TF-IDF提取关键词
        tfidf_keywords = self._extract_keywords_tfidf(texts, top_n * 2)
        
        # 使用传统方法作为补充
        traditional_keywords = self._extract_keywords_traditional(texts, metadata, top_n)
        
        # 合并和排序结果
        combined_keywords = self._combine_keyword_results(
            tfidf_keywords, traditional_keywords, top_n
        )
        
        # 计算趋势
        keywords_with_trend = self._calculate_trends(combined_keywords, days)
        
        # 分类标注
        final_keywords = self._categorize_keywords(keywords_with_trend, metadata)
        
        logger.info(f"热词分析完成，耗时{time.time()-start_time:.2f}秒，提取{len(final_keywords)}个关键词")
        
        return final_keywords[:top_n]
    
    def _collect_texts(self, days: int) -> Tuple[List[str], List[Dict]]:
        """收集文本数据和元数据"""
        texts = []
        metadata = []
        
        # 获取数据
        articles_df = self.data_loader.get_articles()
        videos_df = self.data_loader.get_videos()
        
        # 处理文章
        if not articles_df.empty:
            for _, row in articles_df.iterrows():
                title = str(row.get('title', '') or '')
                content = str(row.get('content', '') or '')
                
                # 合并标题和内容，标题重复3次增加权重
                full_text = f"{title} {title} {title} {self._clean_html(content)}"
                texts.append(full_text)
                
                metadata.append({
                    'type': 'article',
                    'category': str(row.get('category_name', '') or '综合'),
                    'board_type': str(row.get('board_type', '') or ''),
                    'view_count': int(row.get('view_count', 0) or 0),
                    'created_time': row.get('created_time', datetime.now())
                })
        
        # 处理视频
        if not videos_df.empty:
            for _, row in videos_df.iterrows():
                title = str(row.get('title', '') or '')
                desc = str(row.get('description', '') or '')
                
                # 合并标题和描述，标题重复2次
                full_text = f"{title} {title} {desc}"
                texts.append(full_text)
                
                metadata.append({
                    'type': 'video',
                    'category': str(row.get('category_name', '') or '视频'),
                    'board_type': 'video',
                    'view_count': int(row.get('view_count', 0) or 0),
                    'created_time': row.get('created_time', datetime.now())
                })
        
        return texts, metadata
    
    def _extract_keywords_tfidf(self, texts: List[str], top_n: int) -> List[Dict]:
        """使用TF-IDF提取关键词"""
        try:
            # 自定义分词函数
            def chinese_tokenizer(text):
                words = jieba.cut(text)
                return [word for word in words if len(word) > 1 and word not in STOP_WORDS]
            
            # 初始化TF-IDF向量化器
            self.tfidf_vectorizer = TfidfVectorizer(
                tokenizer=chinese_tokenizer,
                max_features=1000,
                min_df=1,  # 至少出现在1个文档中（降低阈值）
                max_df=0.95,  # 最多出现在95%的文档中（提高阈值）
                ngram_range=(1, 2),  # 支持1-2元组
                lowercase=False
            )
            
            # 计算TF-IDF矩阵
            self.tfidf_matrix = self.tfidf_vectorizer.fit_transform(texts)
            self.feature_names = self.tfidf_vectorizer.get_feature_names_out()
            
            # 计算每个词的平均TF-IDF分数
            mean_scores = np.mean(self.tfidf_matrix.toarray(), axis=0)
            
            # 获取top关键词
            top_indices = np.argsort(mean_scores)[::-1][:top_n]
            
            keywords = []
            for idx in top_indices:
                word = self.feature_names[idx]
                tfidf_score = mean_scores[idx]
                
                # 计算词频
                word_count = sum(1 for text in texts if word in text)
                
                keywords.append({
                    'word': word,
                    'tfidf_score': float(tfidf_score),
                    'count': word_count,
                    'weight': int(tfidf_score * 100),  # 转换为0-100权重
                    'source': 'tfidf'
                })
            
            logger.info(f"TF-IDF提取了{len(keywords)}个关键词")
            return keywords
            
        except Exception as e:
            logger.error(f"TF-IDF关键词提取失败: {e}")
            return []
    
    def _extract_keywords_traditional(self, texts: List[str], metadata: List[Dict], top_n: int) -> List[Dict]:
        """使用传统方法提取关键词作为补充"""
        all_words = Counter()
        
        for i, text in enumerate(texts):
            # 使用jieba.analyse提取关键词
            keywords = jieba.analyse.extract_tags(text, topK=10, withWeight=True)
            
            # 根据内容类型和热度调整权重
            view_weight = min(metadata[i]['view_count'] / 100.0, 2.0) + 1.0
            type_weight = 1.5 if metadata[i]['type'] == 'article' else 1.2
            
            for word, weight in keywords:
                if len(word) > 1 and word not in STOP_WORDS:
                    adjusted_weight = weight * view_weight * type_weight
                    all_words[word] += adjusted_weight
        
        # 转换为标准格式
        keywords = []
        for word, weight in all_words.most_common(top_n):
            keywords.append({
                'word': word,
                'weight': int(weight * 10),
                'count': sum(1 for text in texts if word in text),
                'tfidf_score': 0.0,
                'source': 'traditional'
            })
        
        return keywords
    
    def _combine_keyword_results(self, tfidf_keywords: List[Dict], 
                               traditional_keywords: List[Dict], top_n: int) -> List[Dict]:
        """合并TF-IDF和传统方法的结果"""
        word_scores = {}
        
        # 合并TF-IDF结果（权重70%）
        for kw in tfidf_keywords:
            word = kw['word']
            word_scores[word] = {
                'word': word,
                'tfidf_score': kw['tfidf_score'],
                'count': kw['count'],
                'weight': kw['weight'] * 0.7,
                'combined_score': kw['weight'] * 0.7
            }
        
        # 合并传统方法结果（权重30%）
        for kw in traditional_keywords:
            word = kw['word']
            if word in word_scores:
                word_scores[word]['weight'] += kw['weight'] * 0.3
                word_scores[word]['combined_score'] += kw['weight'] * 0.3
                word_scores[word]['count'] = max(word_scores[word]['count'], kw['count'])
            else:
                word_scores[word] = {
                    'word': word,
                    'tfidf_score': 0.0,
                    'count': kw['count'],
                    'weight': kw['weight'] * 0.3,
                    'combined_score': kw['weight'] * 0.3
                }
        
        # 按综合评分排序
        sorted_keywords = sorted(word_scores.values(), 
                               key=lambda x: x['combined_score'], reverse=True)
        
        return sorted_keywords[:top_n]
    
    def _extract_keywords(self, text: str, top_k: int = 10) -> List[tuple]:
        """使用TF-IDF提取关键词"""
        if not text or len(text) < 2:
            return []
        
        try:
            keywords = jieba.analyse.extract_tags(text, topK=top_k, withWeight=True)
            # 过滤停用词和短词
            return [(w, s) for w, s in keywords if w not in STOP_WORDS and len(w) >= 2]
        except Exception as e:
            logger.error(f"关键词提取失败: {e}")
            return []
    
    def _is_cache_valid(self) -> bool:
        """检查缓存是否有效"""
        if not self.hot_words_cache:
            return False
        return time.time() - self.last_update_time < self.cache_duration
    
    def _calculate_trends(self, keywords: List[Dict], days: int) -> List[Dict]:
        """计算关键词趋势"""
        # 简化的趋势计算（实际应基于历史数据）
        for kw in keywords:
            word = kw['word']
            
            # 基于权重和计数估算趋势
            if kw['weight'] > 80:
                trend = 'up'
                trend_label = '上升'
                trend_color = '#22c55e'
            elif kw['weight'] > 50:
                trend = 'stable'
                trend_label = '稳定'
                trend_color = '#3b82f6'
            else:
                trend = 'down'
                trend_label = '下降'
                trend_color = '#ef4444'
            
            kw.update({
                'trend': trend,
                'trend_label': trend_label,
                'trend_color': trend_color
            })
        
        return keywords
    
    def _categorize_keywords(self, keywords: List[Dict], metadata: List[Dict]) -> List[Dict]:
        """为关键词分配分类"""
        # 统计各分类的内容数量
        category_counts = Counter()
        for meta in metadata:
            category_counts[meta['category']] += 1
        
        for kw in keywords:
            word = kw['word']
            
            # 基于领域词典分类
            category = '综合'
            for domain, domain_words in DOMAIN_WORDS.items():
                if word in domain_words:
                    category = domain
                    break
            
            # 如果没有匹配到领域词典，使用最常见的分类
            if category == '综合' and category_counts:
                category = category_counts.most_common(1)[0][0]
            
            kw['category'] = category
        
        return keywords
    
    @monitor_performance("hot_words.analyze_by_dimension")
    def analyze_by_dimension(self, dimension: str, value: str, top_n: int = 20) -> List[Dict]:
        """按维度分析热词
        
        Args:
            dimension: 分析维度 ('time', 'category', 'user_group')
            value: 维度值
            top_n: 返回数量
        """
        logger.info(f"开始按{dimension}={value}分析热词")
        
        if dimension == 'time':
            return self._analyze_by_time(value, top_n)
        elif dimension == 'category':
            return self._analyze_by_category(value, top_n)
        elif dimension == 'user_group':
            return self._analyze_by_user_group(value, top_n)
        else:
            logger.warning(f"不支持的分析维度: {dimension}")
            return []
    
    def _analyze_by_time(self, time_range: str, top_n: int) -> List[Dict]:
        """按时间段分析热词"""
        # 解析时间范围
        if time_range == 'today':
            days = 1
        elif time_range == 'week':
            days = 7
        elif time_range == 'month':
            days = 30
        else:
            days = 7
        
        return self.get_hot_words(top_n, days)
    
    def _analyze_by_category(self, category: str, top_n: int) -> List[Dict]:
        """按分类分析热词"""
        texts, metadata = self._collect_texts(7)
        
        # 过滤指定分类的文本
        filtered_texts = []
        filtered_metadata = []
        
        for i, meta in enumerate(metadata):
            if meta['category'] == category or meta['board_type'] == category:
                filtered_texts.append(texts[i])
                filtered_metadata.append(meta)
        
        if not filtered_texts:
            return []
        
        # 使用过滤后的文本提取关键词
        tfidf_keywords = self._extract_keywords_tfidf(filtered_texts, top_n * 2)
        traditional_keywords = self._extract_keywords_traditional(filtered_texts, filtered_metadata, top_n)
        
        combined_keywords = self._combine_keyword_results(tfidf_keywords, traditional_keywords, top_n)
        keywords_with_trend = self._calculate_trends(combined_keywords, 7)
        final_keywords = self._categorize_keywords(keywords_with_trend, filtered_metadata)
        
        return final_keywords[:top_n]
    
    def _analyze_by_user_group(self, user_group: str, top_n: int) -> List[Dict]:
        """按用户群体分析热词"""
        # 简化实现，实际应根据用户群体过滤内容
        return self.get_hot_words(top_n, 7)
    
    @cached(ttl=3600, prefix="hot_words_trending")
    def get_trending_words(self, top_n: int = 10) -> List[Dict]:
        """获取上升趋势的热词"""
        all_words = self.get_hot_words(top_n * 3, 7)
        trending_words = [w for w in all_words if w.get('trend') == 'up']
        return trending_words[:top_n]
    
    def get_words_by_category(self, category: str, top_n: int = 20) -> List[Dict]:
        """获取指定分类的热词"""
        return self.analyze_by_dimension('category', category, top_n)
    
    def generate_wordcloud_data(self, style: str = 'default', top_n: int = 100) -> Dict:
        """生成词云可视化数据
        
        Args:
            style: 可视化样式 ('default', 'colorful', 'minimal')
            top_n: 词汇数量
        """
        words = self.get_hot_words(top_n, 7)
        
        # 样式配置
        style_configs = {
            'default': {
                'colors': ['#3b82f6', '#ef4444', '#10b981', '#f59e0b', '#8b5cf6'],
                'font_family': 'Arial, sans-serif',
                'background': '#ffffff'
            },
            'colorful': {
                'colors': ['#ff6b6b', '#4ecdc4', '#45b7d1', '#96ceb4', '#feca57', '#ff9ff3'],
                'font_family': 'Arial, sans-serif',
                'background': '#f8f9fa'
            },
            'minimal': {
                'colors': ['#2c3e50', '#34495e', '#7f8c8d'],
                'font_family': 'Arial, sans-serif',
                'background': '#ffffff'
            }
        }
        
        config = style_configs.get(style, style_configs['default'])
        
        # 构建词云数据
        wordcloud_data = {
            'words': [
                {
                    'text': w['word'],
                    'size': max(10, min(w['weight'], 100)),
                    'color': config['colors'][i % len(config['colors'])],
                    'weight': w['weight'],
                    'category': w.get('category', '综合'),
                    'trend': w.get('trend', 'stable')
                }
                for i, w in enumerate(words)
            ],
            'config': config,
            'total_words': len(words),
            'max_weight': max([w['weight'] for w in words]) if words else 0,
            'categories': list(set([w.get('category', '综合') for w in words]))
        }
        
        return wordcloud_data
    
    def _clean_html(self, text: str) -> str:
        """清理HTML标签"""
        if not text:
            return ""
        
        # 移除HTML标签
        clean_text = re.sub(r'<[^>]+>', '', text)
        # 移除多余空白
        clean_text = re.sub(r'\s+', ' ', clean_text).strip()
        
        return clean_text
    
    def get_analysis_stats(self) -> Dict:
        """获取分析统计信息"""
        return {
            'last_update_time': self.last_update_time,
            'cache_duration': self.cache_duration,
            'tfidf_features': len(self.feature_names) if self.feature_names else 0,
            'domain_words_count': sum(len(words) for words in DOMAIN_WORDS.values()),
            'stop_words_count': len(STOP_WORDS)
        }
    
    # ==================== 高级热词分析功能 ====================
    
    @monitor_performance("hot_words.detect_emerging_topics")
    def detect_emerging_topics(self, window_days: int = 3, threshold: float = 1.5) -> List[Dict]:
        """检测新兴话题 - 识别短期内快速增长的关键词
        
        Args:
            window_days: 检测窗口天数
            threshold: 增长阈值倍数
            
        Returns:
            新兴话题列表，包含增长率和置信度
        """
        logger.info(f"开始检测新兴话题，窗口{window_days}天，阈值{threshold}倍")
        
        # 获取当前周期和历史周期的热词
        current_words = self.get_hot_words(100, window_days)
        historical_words = self.get_hot_words(100, window_days * 3)
        
        # 构建历史词频字典
        historical_freq = {w['word']: w['weight'] for w in historical_words}
        
        emerging_topics = []
        for word_data in current_words:
            word = word_data['word']
            current_weight = word_data['weight']
            historical_weight = historical_freq.get(word, 1)  # 避免除零
            
            # 计算增长率
            growth_rate = current_weight / max(historical_weight, 1)
            
            if growth_rate >= threshold:
                # 计算置信度（基于出现次数和增长率）
                confidence = min(1.0, (word_data['count'] / 10) * (growth_rate / threshold) * 0.5)
                
                emerging_topics.append({
                    'word': word,
                    'current_weight': current_weight,
                    'historical_weight': historical_weight,
                    'growth_rate': round(growth_rate, 2),
                    'confidence': round(confidence, 2),
                    'category': word_data.get('category', '综合'),
                    'emergence_level': self._classify_emergence_level(growth_rate),
                    'recommendation': self._generate_topic_recommendation(word, growth_rate)
                })
        
        # 按增长率排序
        emerging_topics.sort(key=lambda x: x['growth_rate'], reverse=True)
        
        logger.info(f"检测到{len(emerging_topics)}个新兴话题")
        return emerging_topics[:20]
    
    def _classify_emergence_level(self, growth_rate: float) -> str:
        """分类新兴话题的级别"""
        if growth_rate >= 5.0:
            return 'explosive'  # 爆发式增长
        elif growth_rate >= 3.0:
            return 'rapid'      # 快速增长
        elif growth_rate >= 2.0:
            return 'moderate'   # 中等增长
        else:
            return 'slight'     # 轻微增长
    
    def _generate_topic_recommendation(self, word: str, growth_rate: float) -> str:
        """生成话题推荐建议"""
        if growth_rate >= 5.0:
            return f"'{word}'正在爆发式增长，建议立即关注并创作相关内容"
        elif growth_rate >= 3.0:
            return f"'{word}'增长迅速，是当前热点话题，建议及时跟进"
        elif growth_rate >= 2.0:
            return f"'{word}'呈上升趋势，可考虑创作相关内容"
        else:
            return f"'{word}'有增长潜力，可持续关注"
    
    @monitor_performance("hot_words.analyze_keyword_correlation")
    def analyze_keyword_correlation(self, top_n: int = 30) -> Dict:
        """分析关键词之间的相关性
        
        Returns:
            关键词相关性矩阵和共现关系
        """
        logger.info("开始分析关键词相关性")
        
        texts, metadata = self._collect_texts(7)
        
        if not texts:
            return {'correlations': [], 'co_occurrences': []}
        
        # 获取热门关键词
        hot_words = self.get_hot_words(top_n, 7)
        keywords = [w['word'] for w in hot_words]
        
        # 构建共现矩阵
        co_occurrence_matrix = np.zeros((len(keywords), len(keywords)))
        
        for text in texts:
            # 检查每对关键词是否共现
            present_keywords = [kw for kw in keywords if kw in text]
            for i, kw1 in enumerate(present_keywords):
                for j, kw2 in enumerate(present_keywords):
                    if i != j:
                        idx1 = keywords.index(kw1)
                        idx2 = keywords.index(kw2)
                        co_occurrence_matrix[idx1][idx2] += 1
        
        # 提取显著的共现关系
        co_occurrences = []
        for i in range(len(keywords)):
            for j in range(i + 1, len(keywords)):
                count = co_occurrence_matrix[i][j]
                if count >= 2:  # 至少共现2次
                    co_occurrences.append({
                        'word1': keywords[i],
                        'word2': keywords[j],
                        'count': int(count),
                        'strength': round(count / len(texts), 3)
                    })
        
        # 按共现次数排序
        co_occurrences.sort(key=lambda x: x['count'], reverse=True)
        
        # 计算关键词聚类
        keyword_clusters = self._cluster_keywords(keywords, co_occurrence_matrix)
        
        logger.info(f"分析完成，发现{len(co_occurrences)}对共现关系")
        
        return {
            'keywords': keywords,
            'co_occurrences': co_occurrences[:50],
            'clusters': keyword_clusters,
            'matrix_size': len(keywords)
        }
    
    def _cluster_keywords(self, keywords: List[str], co_occurrence_matrix: np.ndarray) -> List[Dict]:
        """对关键词进行聚类"""
        if len(keywords) < 3:
            return []
        
        try:
            # 使用K-Means对关键词聚类
            n_clusters = min(5, len(keywords) // 3)
            if n_clusters < 2:
                return []
            
            kmeans = KMeans(n_clusters=n_clusters, random_state=42, n_init=10)
            labels = kmeans.fit_predict(co_occurrence_matrix)
            
            # 组织聚类结果
            clusters = defaultdict(list)
            for i, label in enumerate(labels):
                clusters[label].append(keywords[i])
            
            # 为每个聚类生成主题标签
            cluster_results = []
            for cluster_id, words in clusters.items():
                # 使用第一个词作为主题代表
                theme = words[0] if words else '未知'
                cluster_results.append({
                    'cluster_id': int(cluster_id),
                    'theme': theme,
                    'keywords': words,
                    'size': len(words)
                })
            
            return cluster_results
            
        except Exception as e:
            logger.error(f"关键词聚类失败: {e}")
            return []
    
    @monitor_performance("hot_words.analyze_sentiment_distribution")
    def analyze_sentiment_distribution(self, top_n: int = 50) -> Dict:
        """分析热词的情感分布
        
        Returns:
            热词情感分析结果
        """
        logger.info("开始分析热词情感分布")
        
        hot_words = self.get_hot_words(top_n, 7)
        
        # 简化的情感词典
        positive_words = {'好', '优秀', '精彩', '成功', '进步', '创新', '突破', '荣誉', 
                         '喜悦', '幸福', '美好', '优质', '卓越', '领先', '先进'}
        negative_words = {'问题', '困难', '失败', '危机', '风险', '挑战', '压力', '担忧',
                         '困扰', '障碍', '缺陷', '不足', '落后', '损失', '危害'}
        
        sentiment_results = {
            'positive': [],
            'negative': [],
            'neutral': []
        }
        
        for word_data in hot_words:
            word = word_data['word']
            
            # 判断情感倾向
            if any(pw in word for pw in positive_words) or word in positive_words:
                sentiment = 'positive'
                sentiment_label = '积极'
                sentiment_color = '#22c55e'
            elif any(nw in word for nw in negative_words) or word in negative_words:
                sentiment = 'negative'
                sentiment_label = '消极'
                sentiment_color = '#ef4444'
            else:
                sentiment = 'neutral'
                sentiment_label = '中性'
                sentiment_color = '#6b7280'
            
            word_data['sentiment'] = sentiment
            word_data['sentiment_label'] = sentiment_label
            word_data['sentiment_color'] = sentiment_color
            
            sentiment_results[sentiment].append(word_data)
        
        # 计算情感分布统计
        total = len(hot_words)
        distribution = {
            'positive_count': len(sentiment_results['positive']),
            'negative_count': len(sentiment_results['negative']),
            'neutral_count': len(sentiment_results['neutral']),
            'positive_ratio': round(len(sentiment_results['positive']) / max(total, 1) * 100, 1),
            'negative_ratio': round(len(sentiment_results['negative']) / max(total, 1) * 100, 1),
            'neutral_ratio': round(len(sentiment_results['neutral']) / max(total, 1) * 100, 1)
        }
        
        logger.info(f"情感分析完成: 积极{distribution['positive_count']}个, "
                   f"消极{distribution['negative_count']}个, 中性{distribution['neutral_count']}个")
        
        return {
            'words_by_sentiment': sentiment_results,
            'distribution': distribution,
            'total_analyzed': total
        }
    
    @monitor_performance("hot_words.generate_time_series")
    def generate_time_series(self, keyword: str, days: int = 30) -> Dict:
        """生成关键词的时间序列数据
        
        Args:
            keyword: 要分析的关键词
            days: 分析天数
            
        Returns:
            时间序列数据，用于趋势图表
        """
        logger.info(f"生成关键词'{keyword}'的时间序列数据，周期{days}天")
        
        # 模拟时间序列数据（实际应从数据库查询）
        time_series = []
        base_value = np.random.randint(10, 50)
        
        for i in range(days):
            date = datetime.now() - timedelta(days=days - i - 1)
            
            # 添加趋势和随机波动
            trend = i * 0.5  # 上升趋势
            seasonal = 10 * np.sin(2 * np.pi * i / 7)  # 周期性波动
            noise = np.random.normal(0, 5)  # 随机噪声
            
            value = max(0, base_value + trend + seasonal + noise)
            
            time_series.append({
                'date': date.strftime('%Y-%m-%d'),
                'value': round(value, 1),
                'day_of_week': date.strftime('%A')
            })
        
        # 计算统计指标
        values = [ts['value'] for ts in time_series]
        stats = {
            'mean': round(np.mean(values), 2),
            'std': round(np.std(values), 2),
            'min': round(min(values), 2),
            'max': round(max(values), 2),
            'trend': 'up' if values[-1] > values[0] else 'down',
            'growth_rate': round((values[-1] - values[0]) / max(values[0], 1) * 100, 1)
        }
        
        return {
            'keyword': keyword,
            'time_series': time_series,
            'stats': stats,
            'period_days': days
        }
    
    @monitor_performance("hot_words.compare_periods")
    def compare_periods(self, period1_days: int = 7, period2_days: int = 7, 
                       offset_days: int = 7, top_n: int = 30) -> Dict:
        """对比两个时间段的热词变化
        
        Args:
            period1_days: 第一个周期天数（当前周期）
            period2_days: 第二个周期天数（对比周期）
            offset_days: 对比周期的偏移天数
            top_n: 返回数量
            
        Returns:
            两个周期的热词对比结果
        """
        logger.info(f"对比热词变化: 当前{period1_days}天 vs {offset_days}天前的{period2_days}天")
        
        # 获取当前周期热词
        current_words = self.get_hot_words(top_n * 2, period1_days)
        current_dict = {w['word']: w for w in current_words}
        
        # 模拟历史周期热词（实际应从历史数据获取）
        # 这里简化处理，基于当前数据生成模拟历史数据
        historical_words = []
        for w in current_words:
            historical_weight = w['weight'] * np.random.uniform(0.5, 1.5)
            historical_words.append({
                'word': w['word'],
                'weight': int(historical_weight),
                'count': int(w['count'] * np.random.uniform(0.5, 1.5)),
                'category': w.get('category', '综合')
            })
        
        historical_dict = {w['word']: w for w in historical_words}
        
        # 分析变化
        comparison_results = {
            'new_words': [],      # 新出现的词
            'disappeared': [],    # 消失的词
            'rising': [],         # 上升的词
            'falling': [],        # 下降的词
            'stable': []          # 稳定的词
        }
        
        all_words = set(current_dict.keys()) | set(historical_dict.keys())
        
        for word in all_words:
            current_data = current_dict.get(word)
            historical_data = historical_dict.get(word)
            
            if current_data and not historical_data:
                comparison_results['new_words'].append({
                    'word': word,
                    'current_weight': current_data['weight'],
                    'change_type': 'new',
                    'category': current_data.get('category', '综合')
                })
            elif historical_data and not current_data:
                comparison_results['disappeared'].append({
                    'word': word,
                    'historical_weight': historical_data['weight'],
                    'change_type': 'disappeared',
                    'category': historical_data.get('category', '综合')
                })
            elif current_data and historical_data:
                change = current_data['weight'] - historical_data['weight']
                change_rate = change / max(historical_data['weight'], 1) * 100
                
                result = {
                    'word': word,
                    'current_weight': current_data['weight'],
                    'historical_weight': historical_data['weight'],
                    'change': change,
                    'change_rate': round(change_rate, 1),
                    'category': current_data.get('category', '综合')
                }
                
                if change_rate > 20:
                    result['change_type'] = 'rising'
                    comparison_results['rising'].append(result)
                elif change_rate < -20:
                    result['change_type'] = 'falling'
                    comparison_results['falling'].append(result)
                else:
                    result['change_type'] = 'stable'
                    comparison_results['stable'].append(result)
        
        # 排序
        comparison_results['rising'].sort(key=lambda x: x['change_rate'], reverse=True)
        comparison_results['falling'].sort(key=lambda x: x['change_rate'])
        
        # 生成摘要
        summary = {
            'new_count': len(comparison_results['new_words']),
            'disappeared_count': len(comparison_results['disappeared']),
            'rising_count': len(comparison_results['rising']),
            'falling_count': len(comparison_results['falling']),
            'stable_count': len(comparison_results['stable']),
            'period1': f'最近{period1_days}天',
            'period2': f'{offset_days}天前的{period2_days}天'
        }
        
        logger.info(f"周期对比完成: 新增{summary['new_count']}个, "
                   f"上升{summary['rising_count']}个, 下降{summary['falling_count']}个")
        
        return {
            'comparison': comparison_results,
            'summary': summary
        }
    
    @monitor_performance("hot_words.extract_topic_phrases")
    def extract_topic_phrases(self, top_n: int = 20) -> List[Dict]:
        """提取主题短语（2-3词组合）
        
        Returns:
            主题短语列表
        """
        logger.info("开始提取主题短语")
        
        texts, metadata = self._collect_texts(7)
        
        if not texts:
            return []
        
        # 提取n-gram短语
        phrase_counter = Counter()
        
        for text in texts:
            # 分词
            words = list(jieba.cut(text))
            words = [w for w in words if len(w) > 1 and w not in STOP_WORDS]
            
            # 提取2-gram
            for i in range(len(words) - 1):
                phrase = f"{words[i]}{words[i+1]}"
                if len(phrase) >= 4:  # 至少4个字符
                    phrase_counter[phrase] += 1
            
            # 提取3-gram
            for i in range(len(words) - 2):
                phrase = f"{words[i]}{words[i+1]}{words[i+2]}"
                if len(phrase) >= 6:  # 至少6个字符
                    phrase_counter[phrase] += 1
        
        # 过滤低频短语
        min_count = 2
        filtered_phrases = [(p, c) for p, c in phrase_counter.items() if c >= min_count]
        
        # 按频率排序
        filtered_phrases.sort(key=lambda x: x[1], reverse=True)
        
        # 构建结果
        results = []
        for phrase, count in filtered_phrases[:top_n]:
            results.append({
                'phrase': phrase,
                'count': count,
                'length': len(phrase),
                'weight': min(100, count * 10)
            })
        
        logger.info(f"提取了{len(results)}个主题短语")
        return results
    
    def update_stop_words(self, words_to_add: List[str] = None, 
                         words_to_remove: List[str] = None) -> Dict:
        """动态更新停用词库
        
        Args:
            words_to_add: 要添加的停用词
            words_to_remove: 要移除的停用词
            
        Returns:
            更新结果
        """
        global STOP_WORDS
        
        added = []
        removed = []
        
        if words_to_add:
            for word in words_to_add:
                if word not in STOP_WORDS:
                    STOP_WORDS.add(word)
                    added.append(word)
        
        if words_to_remove:
            for word in words_to_remove:
                if word in STOP_WORDS:
                    STOP_WORDS.discard(word)
                    removed.append(word)
        
        logger.info(f"停用词库更新: 添加{len(added)}个, 移除{len(removed)}个")
        
        return {
            'added': added,
            'removed': removed,
            'total_stop_words': len(STOP_WORDS)
        }
    
    def add_domain_words(self, domain: str, words: List[str]) -> Dict:
        """添加领域专用词
        
        Args:
            domain: 领域名称
            words: 词汇列表
            
        Returns:
            添加结果
        """
        global DOMAIN_WORDS
        
        if domain not in DOMAIN_WORDS:
            DOMAIN_WORDS[domain] = []
        
        added = []
        for word in words:
            if word not in DOMAIN_WORDS[domain]:
                DOMAIN_WORDS[domain].append(word)
                jieba.add_word(word)  # 同时添加到jieba词典
                added.append(word)
        
        logger.info(f"领域'{domain}'添加了{len(added)}个专用词")
        
        return {
            'domain': domain,
            'added': added,
            'total_domain_words': len(DOMAIN_WORDS[domain])
        }
    
    def get_comprehensive_analysis(self, days: int = 7, top_n: int = 50) -> Dict:
        """获取综合热词分析报告
        
        Returns:
            包含多维度分析的综合报告
        """
        logger.info(f"生成综合热词分析报告，周期{days}天")
        
        # 基础热词
        hot_words = self.get_hot_words(top_n, days)
        
        # 新兴话题
        emerging_topics = self.detect_emerging_topics(min(days, 3))
        
        # 关键词相关性
        correlation = self.analyze_keyword_correlation(min(top_n, 30))
        
        # 情感分布
        sentiment = self.analyze_sentiment_distribution(top_n)
        
        # 主题短语
        phrases = self.extract_topic_phrases(20)
        
        # 周期对比
        comparison = self.compare_periods(days, days, days, top_n)
        
        # 词云数据
        wordcloud = self.generate_wordcloud_data('colorful', top_n)
        
        # 统计摘要
        summary = {
            'total_hot_words': len(hot_words),
            'emerging_topics_count': len(emerging_topics),
            'keyword_clusters': len(correlation.get('clusters', [])),
            'sentiment_positive_ratio': sentiment['distribution']['positive_ratio'],
            'top_phrases_count': len(phrases),
            'analysis_period_days': days,
            'generated_at': datetime.now().isoformat()
        }
        
        return {
            'hot_words': hot_words,
            'emerging_topics': emerging_topics,
            'correlation': correlation,
            'sentiment': sentiment,
            'phrases': phrases,
            'comparison': comparison,
            'wordcloud': wordcloud,
            'summary': summary
        }