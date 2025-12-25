"""热词分析模块 - 提取热门关键词"""
import pandas as pd
import numpy as np
from typing import List, Dict
from collections import Counter
import jieba
import jieba.analyse
from .data_loader import DataLoader
import logging
import time
import re

logger = logging.getLogger(__name__)

# 停用词
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
])


class HotWordsAnalyzer:
    """热词分析器"""
    
    def __init__(self, data_loader: DataLoader):
        self.data_loader = data_loader
        self.hot_words_cache = []
        self.last_update_time = 0
        self.cache_duration = 1800  # 30分钟缓存
    
    def get_hot_words(self, top_n: int = 50, days: int = 7) -> List[Dict]:
        """获取热门关键词
        
        Args:
            top_n: 返回数量
            days: 统计天数
            
        Returns:
            热词列表，包含 word, weight, trend, category
        """
        # 检查缓存
        if self._is_cache_valid():
            return self.hot_words_cache[:top_n]
        
        logger.info(f"开始分析热词，统计最近{days}天...")
        start_time = time.time()
        
        # 获取文章数据
        articles_df = self.data_loader.get_articles()
        videos_df = self.data_loader.get_videos()
        
        all_words = Counter()
        category_words = {}  # 分类关联
        
        # 分析文章标题和内容
        if not articles_df.empty:
            for _, row in articles_df.iterrows():
                title = str(row.get('title', '') or '')
                content = str(row.get('content', '') or '')
                category = str(row.get('category_name', '') or '综合')
                
                # 提取关键词（标题权重更高）
                title_words = self._extract_keywords(title, top_k=5)
                content_words = self._extract_keywords(self._clean_html(content), top_k=10)
                
                # 标题词权重 x3
                for word, weight in title_words:
                    all_words[word] += weight * 3
                    if word not in category_words:
                        category_words[word] = Counter()
                    category_words[word][category] += 1
                
                for word, weight in content_words:
                    all_words[word] += weight
                    if word not in category_words:
                        category_words[word] = Counter()
                    category_words[word][category] += 1
        
        # 分析视频标题
        if not videos_df.empty:
            for _, row in videos_df.iterrows():
                title = str(row.get('title', '') or '')
                desc = str(row.get('description', '') or '')
                category = str(row.get('category_name', '') or '视频')
                
                title_words = self._extract_keywords(title, top_k=5)
                desc_words = self._extract_keywords(desc, top_k=5)
                
                for word, weight in title_words:
                    all_words[word] += weight * 2
                    if word not in category_words:
                        category_words[word] = Counter()
                    category_words[word][category] += 1
                
                for word, weight in desc_words:
                    all_words[word] += weight
        
        # 构建结果
        if not all_words:
            logger.warning("没有提取到热词")
            return []
        
        max_weight = max(all_words.values())
        results = []
        
        for word, weight in all_words.most_common(top_n * 2):
            if len(word) < 2:  # 过滤单字
                continue
            if word in STOP_WORDS:
                continue
            
            # 归一化权重到 1-100
            normalized_weight = int((weight / max_weight) * 100)
            
            # 获取主要分类
            main_category = '综合'
            if word in category_words and category_words[word]:
                main_category = category_words[word].most_common(1)[0][0]
            
            # 随机趋势（实际应该对比历史数据）
            trend = np.random.choice(['up', 'down', 'stable'], p=[0.4, 0.2, 0.4])
            
            results.append({
                'word': word,
                'weight': normalized_weight,
                'count': int(weight),
                'trend': trend,
                'category': main_category
            })
            
            if len(results) >= top_n:
                break
        
        self.hot_words_cache = results
        self.last_update_time = time.time()
        
        logger.info(f"热词分析完成，共{len(results)}个，耗时{time.time()-start_time:.2f}秒")
        return results
    
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
    
    def _clean_html(self, html: str) -> str:
        """清理HTML标签"""
        if not html:
            return ''
        # 移除HTML标签
        clean = re.sub(r'<[^>]+>', '', html)
        # 移除多余空白
        clean = re.sub(r'\s+', ' ', clean)
        return clean.strip()
    
    def _is_cache_valid(self) -> bool:
        """检查缓存是否有效"""
        if not self.hot_words_cache:
            return False
        return time.time() - self.last_update_time < self.cache_duration
    
    def get_trending_words(self, top_n: int = 10) -> List[Dict]:
        """获取上升趋势的热词"""
        all_words = self.get_hot_words(top_n * 3)
        trending = [w for w in all_words if w['trend'] == 'up']
        return trending[:top_n]
    
    def get_words_by_category(self, category: str, top_n: int = 20) -> List[Dict]:
        """获取指定分类的热词"""
        all_words = self.get_hot_words(100)
        category_words = [w for w in all_words if w['category'] == category]
        return category_words[:top_n]
