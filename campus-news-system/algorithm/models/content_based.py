"""基于内容的推荐算法 - 使用TF-IDF和余弦相似度"""
import numpy as np
import pandas as pd
import jieba
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from typing import List, Dict, Tuple
import logging

logger = logging.getLogger(__name__)

class ContentBasedRecommender:
    def __init__(self):
        self.tfidf_vectorizer = TfidfVectorizer(
            max_features=5000,
            stop_words=self._get_stopwords()
        )
        self.article_vectors = None
        self.articles_df = None
        self.article_id_to_idx = {}
        self.idx_to_article_id = {}
    
    def _get_stopwords(self) -> List[str]:
        """获取中文停用词"""
        return ['的', '了', '在', '是', '我', '有', '和', '就', '不', '人', '都', 
                '一', '一个', '上', '也', '很', '到', '说', '要', '去', '你', '会',
                '着', '没有', '看', '好', '自己', '这', '那', '什么', '他', '她']
    
    def _tokenize(self, text: str) -> str:
        """中文分词"""
        if not text:
            return ""
        words = jieba.cut(text)
        return " ".join(words)
    
    def fit(self, articles_df: pd.DataFrame):
        """训练模型 - 构建文章特征向量"""
        if articles_df.empty:
            logger.warning("文章数据为空，跳过训练")
            return
        
        self.articles_df = articles_df.copy()
        
        # 构建文章索引映射
        for idx, article_id in enumerate(articles_df['id'].values):
            self.article_id_to_idx[article_id] = idx
            self.idx_to_article_id[idx] = article_id
        
        # 合并标题、摘要、标签作为文章特征
        self.articles_df['combined_text'] = (
            self.articles_df['title'].fillna('') + ' ' +
            self.articles_df['summary'].fillna('') + ' ' +
            self.articles_df['tags'].fillna('') + ' ' +
            self.articles_df['board_type'].fillna('')
        )
        
        # 分词处理
        self.articles_df['tokenized'] = self.articles_df['combined_text'].apply(self._tokenize)
        
        # TF-IDF向量化
        self.article_vectors = self.tfidf_vectorizer.fit_transform(
            self.articles_df['tokenized'].values
        )
        
        logger.info(f"内容推荐模型训练完成，文章数: {len(articles_df)}")
    
    def get_similar_articles(self, article_id: int, top_n: int = 10) -> List[Tuple[int, float]]:
        """获取与指定文章相似的文章"""
        if article_id not in self.article_id_to_idx:
            return []
        
        idx = self.article_id_to_idx[article_id]
        article_vector = self.article_vectors[idx]
        
        # 计算余弦相似度
        similarities = cosine_similarity(article_vector, self.article_vectors).flatten()
        
        # 排序并排除自身
        similar_indices = similarities.argsort()[::-1][1:top_n+1]
        
        results = []
        for i in similar_indices:
            article_id = self.idx_to_article_id[i]
            score = float(similarities[i])
            results.append((article_id, score))
        
        return results
    
    def recommend_for_user(self, user_history: List[int], top_n: int = 10, 
                          exclude_ids: List[int] = None) -> List[Tuple[int, float]]:
        """基于用户历史推荐文章"""
        if not user_history or self.article_vectors is None:
            return []
        
        exclude_ids = exclude_ids or []
        exclude_set = set(user_history + exclude_ids)
        
        # 获取用户历史文章的向量
        valid_history = [aid for aid in user_history if aid in self.article_id_to_idx]
        if not valid_history:
            return []
        
        history_indices = [self.article_id_to_idx[aid] for aid in valid_history]
        
        # 计算用户兴趣向量(历史文章向量的平均)
        user_vector = np.mean(self.article_vectors[history_indices].toarray(), axis=0).reshape(1, -1)
        
        # 计算与所有文章的相似度
        similarities = cosine_similarity(user_vector, self.article_vectors).flatten()
        
        # 排序并过滤
        results = []
        for idx in similarities.argsort()[::-1]:
            article_id = self.idx_to_article_id[idx]
            if article_id not in exclude_set:
                results.append((article_id, float(similarities[idx])))
                if len(results) >= top_n:
                    break
        
        return results
