"""通过API接口获取数据并生成可视化图表"""
import requests
import matplotlib.pyplot as plt
import seaborn as sns
import pandas as pd
import numpy as np
import json

# 设置中文字体
plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei', 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False

# API基础URL
BASE_URL = "http://localhost:8000"

def fetch_api_data(endpoint):
    """获取API数据"""
    try:
        response = requests.get(f"{BASE_URL}{endpoint}")
        if response.status_code == 200:
            return response.json()
        else:
            print(f"API请求失败: {endpoint}, 状态码: {response.status_code}")
            return None
    except Exception as e:
        print(f"API请求异常: {e}")
        return None

def generate_hotwords_chart_from_api():
    """从API获取热词数据并生成图4-2"""
    print("正在从API获取热词数据...")
    
    # 获取热词数据
    hotwords_data = fetch_api_data("/api/hotwords?top_n=10&days=7")
    
    if not hotwords_data or not hotwords_data.get('success'):
        print("无法获取热词数据，使用示例数据")
        # 使用示例数据
        words_data = [
            {'word': '创新创业', 'weight': 85, 'tfidf_score': 0.85},
            {'word': '大学生', 'weight': 72, 'tfidf_score': 0.72},
            {'word': '比赛', 'weight': 68, 'tfidf_score': 0.68},
            {'word': '获奖', 'weight': 61, 'tfidf_score': 0.61},
            {'word': '成绩', 'weight': 45, 'tfidf_score': 0.45}
        ]
    else:
        words_data = hotwords_data['data'][:10]
    
    # 创建图表
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 6))
    
    # 提取数据
    words = [w['word'] for w in words_data]
    weights = [w['weight'] for w in words_data]
    
    # 子图1: 热词权重柱状图
    bars = ax1.barh(words, weights, color='skyblue', alpha=0.8)
    ax1.set_xlabel('TF-IDF权重分数')
    ax1.set_title('TF-IDF热词提取效果示例')
    ax1.grid(axis='x', alpha=0.3)
    
    # 添加数值标签
    for i, (bar, weight) in enumerate(zip(bars, weights)):
        ax1.text(weight + 1, i, f'{weight}', va='center', fontsize=10)
    
    # 子图2: 权重分布散点图
    ax2.scatter(range(len(words)), weights, s=100, c='orange', alpha=0.7)
    ax2.plot(range(len(words)), weights, 'o-', color='orange', alpha=0.5)
    ax2.set_xticks(range(len(words)))
    ax2.set_xticklabels(words, rotation=45, ha='right')
    ax2.set_ylabel('权重分数')
    ax2.set_title('热词权重分布')
    ax2.grid(True, alpha=0.3)
    
    plt.tight_layout()
    plt.savefig('API_图4-2_TF-IDF热词提取效果示例.png', dpi=300, bbox_inches='tight')
    plt.show()
    
    print("图4-2生成成功!")

def generate_similarity_chart_from_api():
    """从API获取相关性数据并生成图4-3"""
    print("正在从API获取关键词相关性数据...")
    
    # 获取关键词相关性数据
    correlation_data = fetch_api_data("/api/hotwords/correlation?top_n=5")
    
    if not correlation_data or not correlation_data.get('success'):
        print("无法获取相关性数据，使用示例数据")
        # 生成示例相似度数据
        articles = ['创新创业大赛', '学生科技项目', '创业团队成果', '学术研究进展', '技术创新应用']
        similarity_data = [
            {'article1': '创新创业大赛', 'article2': '学生科技项目', 'similarity': 0.75},
            {'article1': '创新创业大赛', 'article2': '创业团队成果', 'similarity': 0.68},
            {'article1': '创新创业大赛', 'article2': '学术研究进展', 'similarity': 0.42},
            {'article1': '创新创业大赛', 'article2': '技术创新应用', 'similarity': 0.31},
        ]
    else:
        # 处理API返回的共现数据
        co_occurrences = correlation_data['data'].get('co_occurrences', [])
        articles = list(set([co['word1'] for co in co_occurrences] + [co['word2'] for co in co_occurrences]))[:5]
        similarity_data = [
            {'article1': co['word1'], 'article2': co['word2'], 'similarity': co['strength']}
            for co in co_occurrences[:10]
        ]
    
    # 创建图表
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(16, 6))
    
    # 子图1: 相似度矩阵热力图
    if len(articles) >= 3:
        # 构建相似度矩阵
        n = len(articles)
        similarity_matrix = np.eye(n)
        
        for sim in similarity_data:
            if sim['article1'] in articles and sim['article2'] in articles:
                i = articles.index(sim['article1'])
                j = articles.index(sim['article2'])
                similarity_matrix[i][j] = sim['similarity']
                similarity_matrix[j][i] = sim['similarity']
        
        sns.heatmap(similarity_matrix,
                   xticklabels=[f'内容{i+1}' for i in range(n)],
                   yticklabels=[f'内容{i+1}' for i in range(n)],
                   annot=True,
                   fmt='.2f',
                   cmap='YlOrRd',
                   ax=ax1,
                   cbar_kws={'label': '相似度分数'})
        ax1.set_title('TF-IDF内容相似度矩阵')
    
    # 子图2: 推荐结果示例
    threshold = 0.3
    if similarity_data:
        # 选择相似度数据作为推荐示例
        recommendations = [(s['article2'], s['similarity']) for s in similarity_data if s['similarity'] >= threshold][:6]
        
        if recommendations:
            rec_articles, rec_scores = zip(*recommendations)
            colors = ['green' if score >= threshold else 'red' for score in rec_scores]
            
            bars = ax2.barh(range(len(rec_articles)), rec_scores, color=colors, alpha=0.7)
            ax2.set_yticks(range(len(rec_articles)))
            ax2.set_yticklabels([f'推荐{i+1}' for i in range(len(rec_articles))])
            ax2.axvline(x=threshold, color='red', linestyle='--', alpha=0.8, label=f'阈值 ({threshold})')
            ax2.set_xlabel('相似度分数')
            ax2.set_title('内容相似度推荐结果')
            ax2.legend()
            ax2.grid(axis='x', alpha=0.3)
            
            # 添加数值标签
            for bar, score in zip(bars, rec_scores):
                ax2.text(score + 0.01, bar.get_y() + bar.get_height()/2,
                        f'{score:.2f}', va='center', fontsize=10)
    
    plt.tight_layout()
    plt.savefig('API_图4-3_TF-IDF内容相似度计算效果示例.png', dpi=300, bbox_inches='tight')
    plt.show()
    
    print("图4-3生成成功!")

def generate_comprehensive_dashboard():
    """生成综合仪表板"""
    print("正在生成综合分析仪表板...")
    
    # 获取多个API数据
    hotwords_data = fetch_api_data("/api/hotwords?top_n=15")
    sentiment_data = fetch_api_data("/api/hotwords/sentiment?top_n=50")
    emerging_data = fetch_api_data("/api/hotwords/emerging")
    phrases_data = fetch_api_data("/api/hotwords/phrases?top_n=10")
    
    fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(16, 12))
    
    # 1. 热词权重分布
    if hotwords_data and hotwords_data.get('success'):
        words_data = hotwords_data['data'][:10]
        words = [w['word'] for w in words_data]
        weights = [w['weight'] for w in words_data]
        
        ax1.barh(words, weights, color='skyblue', alpha=0.8)
        ax1.set_xlabel('权重分数')
        ax1.set_title('热门关键词分布')
        ax1.grid(axis='x', alpha=0.3)
    
    # 2. 情感分布
    if sentiment_data and sentiment_data.get('success'):
        distribution = sentiment_data['data']['distribution']
        labels = ['积极', '中性', '消极']
        sizes = [distribution['positive_ratio'], distribution['neutral_ratio'], distribution['negative_ratio']]
        colors = ['#22c55e', '#6b7280', '#ef4444']
        
        ax2.pie(sizes, labels=labels, colors=colors, autopct='%1.1f%%', startangle=90)
        ax2.set_title('热词情感分布')
    
    # 3. 新兴话题
    if emerging_data and emerging_data.get('success'):
        topics = emerging_data['data'][:8]
        if topics:
            topic_words = [t['word'] for t in topics]
            growth_rates = [t['growth_rate'] for t in topics]
            
            ax3.barh(topic_words, growth_rates, color='orange', alpha=0.7)
            ax3.set_xlabel('增长率')
            ax3.set_title('新兴话题趋势')
            ax3.grid(axis='x', alpha=0.3)
    
    # 4. 主题短语
    if phrases_data and phrases_data.get('success'):
        phrases = phrases_data['data'][:8]
        if phrases:
            phrase_texts = [p['phrase'] for p in phrases]
            phrase_counts = [p['count'] for p in phrases]
            
            ax4.barh(phrase_texts, phrase_counts, color='purple', alpha=0.7)
            ax4.set_xlabel('出现次数')
            ax4.set_title('主题短语统计')
            ax4.grid(axis='x', alpha=0.3)
    
    plt.tight_layout()
    plt.savefig('API_综合文本挖掘分析仪表板.png', dpi=300, bbox_inches='tight')
    plt.show()
    
    print("综合仪表板生成成功!")

def check_api_status():
    """检查API服务状态"""
    try:
        response = requests.get(f"{BASE_URL}/health")
        if response.status_code == 200:
            print("✅ API服务正常运行")
            return True
        else:
            print("❌ API服务异常")
            return False
    except Exception as e:
        print(f"❌ 无法连接到API服务: {e}")
        print("请确保算法服务正在运行 (python main.py)")
        return False

if __name__ == "__main__":
    print("=== 基于API的文本挖掘可视化工具 ===\n")
    
    # 检查API状态
    if not check_api_status():
        print("\n请先启动算法服务:")
        print("cd softwareTraining/campus-news-system/algorithm")
        print("python main.py")
        exit(1)
    
    print("\n开始生成可视化图表...")
    
    # 生成图4-2
    print("\n1. 生成图4-2: TF-IDF热词提取效果示例")
    generate_hotwords_chart_from_api()
    
    # 生成图4-3
    print("\n2. 生成图4-3: TF-IDF内容相似度计算效果示例")
    generate_similarity_chart_from_api()
    
    # 生成综合仪表板
    print("\n3. 生成综合分析仪表板")
    generate_comprehensive_dashboard()
    
    print("\n✅ 所有图表生成完成！")
    print("\n生成的文件:")
    print("- API_图4-2_TF-IDF热词提取效果示例.png")
    print("- API_图4-3_TF-IDF内容相似度计算效果示例.png")
    print("- API_综合文本挖掘分析仪表板.png")