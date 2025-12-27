<template>
  <div class="analytics-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          <span class="title-icon">📊</span>
          智能分析中心
        </h1>
        <p class="page-subtitle">基于机器学习算法的用户画像与趋势预测</p>
      </div>
      <div class="header-decoration"></div>
    </div>

    <!-- 顶部统计卡片 -->
    <div class="stats-row">
      <div class="stat-card user-card" v-loading="loading.userType">
        <div class="card-glow"></div>
        <div class="stat-card-inner">
          <div class="user-type-display" v-if="userType">
            <div class="type-avatar">
              <span class="avatar-icon">{{ userType.icon }}</span>
              <div class="avatar-ring"></div>
            </div>
            <div class="type-content">
              <span class="type-label">🎯 我的用户画像</span>
              <h3 class="type-name">{{ userType.name }}</h3>
              <p class="type-desc">{{ userType.desc }}</p>
            </div>
          </div>
          <div class="empty-state" v-else>
            <el-icon><User /></el-icon>
            <span>暂无画像数据</span>
          </div>
        </div>
        <div class="card-badge">
          <span class="badge-dot"></span>
          K-Means 聚类
        </div>
      </div>

      <div class="stat-card metric-card article-metric">
        <div class="card-glow article-glow"></div>
        <div class="metric-icon">
          <el-icon><Document /></el-icon>
          <div class="icon-pulse"></div>
        </div>
        <div class="metric-content">
          <span class="metric-label">📖 文章总浏览</span>
          <div class="metric-value">{{ formatNumber(platformStats?.total_article_views || 0) }}</div>
          <div class="metric-growth positive" v-if="platformStats">
            <el-icon><Top /></el-icon>
            <span>{{ platformStats.article_growth_rate }}% 增长</span>
          </div>
        </div>
      </div>

      <div class="stat-card metric-card video-metric">
        <div class="card-glow video-glow"></div>
        <div class="metric-icon">
          <el-icon><VideoCamera /></el-icon>
          <div class="icon-pulse"></div>
        </div>
        <div class="metric-content">
          <span class="metric-label">🎬 视频总播放</span>
          <div class="metric-value">{{ formatNumber(platformStats?.total_video_views || 0) }}</div>
          <div class="metric-growth positive" v-if="platformStats">
            <el-icon><Top /></el-icon>
            <span>{{ platformStats.video_growth_rate }}% 增长</span>
          </div>
        </div>
      </div>

      <div class="stat-card predict-card">
        <div class="card-glow predict-glow"></div>
        <div class="predict-header">
          <div class="predict-icon">
            <el-icon><Aim /></el-icon>
          </div>
          <span>🔮 下周预测</span>
        </div>
        <div class="predict-grid">
          <div class="predict-item">
            <span class="predict-num">{{ formatNumber(platformStats?.predicted_article_views_next_week || 0) }}</span>
            <span class="predict-label">文章浏览</span>
          </div>
          <div class="predict-divider">
            <span class="divider-dot"></span>
          </div>
          <div class="predict-item">
            <span class="predict-num">{{ formatNumber(platformStats?.predicted_video_views_next_week || 0) }}</span>
            <span class="predict-label">视频播放</span>
          </div>
        </div>
        <div class="predict-badge">线性回归</div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 左侧：用户分布 -->
      <div class="content-card distribution-card" v-loading="loading.distribution">
        <div class="card-header">
          <div class="header-title">
            <div class="title-icon-wrap">
              <el-icon><PieChart /></el-icon>
            </div>
            <span>👥 平台用户分布</span>
          </div>
          <div class="header-badge">
            <span class="badge-count">{{ distribution.total || 0 }}</span>
            <span class="badge-text">用户</span>
          </div>
        </div>
        <div class="distribution-list" v-if="distribution.distribution?.length">
          <div 
            v-for="(item, index) in distribution.distribution" 
            :key="item.type"
            class="dist-item"
            :style="{ animationDelay: index * 0.1 + 's' }"
          >
            <div class="dist-info">
              <span class="dist-icon">{{ item.icon }}</span>
              <div class="dist-text">
                <span class="dist-name">{{ item.name }}</span>
                <span class="dist-count">{{ item.count }}人</span>
              </div>
            </div>
            <div class="dist-bar-wrap">
              <div 
                class="dist-bar" 
                :style="{ width: item.percentage + '%', background: `linear-gradient(90deg, ${item.color}, ${item.color}dd)` }"
              >
                <div class="bar-shine"></div>
              </div>
            </div>
            <span class="dist-percent">{{ item.percentage }}%</span>
          </div>
        </div>
        <el-empty v-else description="暂无数据" :image-size="60" />
      </div>

      <!-- 右侧：热度上升 -->
      <div class="content-card trending-card" v-loading="loading.trending">
        <div class="card-header">
          <div class="header-title">
            <div class="title-icon-wrap trending-icon">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <span>🔥 热度上升内容</span>
          </div>
          <el-tag size="small" effect="dark" type="warning">线性回归</el-tag>
        </div>
        <div class="trending-list" v-if="trendingContent.length">
          <div 
            v-for="(item, index) in trendingContent" 
            :key="`${item.type}-${item.id}`"
            class="trending-item"
            :style="{ animationDelay: index * 0.08 + 's' }"
            @click="goToContent(item)"
          >
            <div class="trending-rank">{{ index + 1 }}</div>
            <div class="trending-type" :class="item.type">
              {{ item.type === 'article' ? '📄' : '🎬' }}
            </div>
            <div class="trending-info">
              <span class="trending-title">{{ item.title }}</span>
              <div class="trending-stats">
                <span class="stat-current">当前 {{ item.current_views }}</span>
                <el-icon class="arrow"><Right /></el-icon>
                <span class="stat-predict">预测 {{ item.predicted_views_7d }}</span>
              </div>
            </div>
            <div class="trending-growth" :style="{ background: `linear-gradient(135deg, ${item.trend_color}, ${item.trend_color}cc)` }">
              <el-icon><Top /></el-icon>
              {{ item.growth_rate }}%
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无上升内容" :image-size="60" />
      </div>
    </div>

    <!-- 算法说明 -->
    <div class="algo-section">
      <div class="algo-header">
        <div class="algo-title">
          <span class="algo-icon">🧠</span>
          <span>核心算法技术</span>
        </div>
        <div class="algo-subtitle">本系统采用多种机器学习算法实现智能分析</div>
      </div>
      <div class="algo-grid">
        <div class="algo-item kmeans-item">
          <div class="algo-badge kmeans">
            <span>K</span>
            <div class="badge-glow"></div>
          </div>
          <div class="algo-content">
            <h4>K-Means 聚类算法</h4>
            <p>基于用户行为特征将用户智能分为5类：活跃创作者、深度阅读者、社交达人、视频爱好者、潜水用户</p>
            <div class="algo-tags">
              <span class="algo-tag">无监督学习</span>
              <span class="algo-tag">用户画像</span>
            </div>
          </div>
        </div>
        <div class="algo-item regression-item">
          <div class="algo-badge regression">
            <span>R</span>
            <div class="badge-glow"></div>
          </div>
          <div class="algo-content">
            <h4>线性回归预测</h4>
            <p>基于内容特征和历史数据，预测文章/视频未来7天和30天的浏览量趋势变化</p>
            <div class="algo-tags">
              <span class="algo-tag">趋势预测</span>
              <span class="algo-tag">时序分析</span>
            </div>
          </div>
        </div>
        <div class="algo-item tfidf-item">
          <div class="algo-badge tfidf">
            <span>T</span>
            <div class="badge-glow"></div>
          </div>
          <div class="algo-content">
            <h4>TF-IDF 热词提取</h4>
            <p>使用词频-逆文档频率算法从新闻标题中提取热门关键词，发现热点话题</p>
            <div class="algo-tags">
              <span class="algo-tag">文本挖掘</span>
              <span class="algo-tag">热词分析</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { 
  User, PieChart, TrendCharts, Document, VideoCamera,
  Top, Right, Aim, InfoFilled
} from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()

const loading = ref({
  userType: false,
  distribution: false,
  platform: false,
  trending: false
})

const userType = ref(null)
const distribution = ref({})
const platformStats = ref(null)
const trendingContent = ref([])

const formatNumber = (num) => {
  if (!num) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num.toString()
}

const goToContent = (item) => {
  if (item.type === 'article') {
    router.push(`/article/${item.id}`)
  } else {
    router.push(`/video/${item.id}`)
  }
}

const loadUserType = async () => {
  loading.value.userType = true
  try {
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    if (user.id) {
      const res = await request({
        url: `/analysis/user/type/${user.id}`,
        method: 'get',
        timeout: 8000
      })
      userType.value = res
    }
  } catch (e) {
    console.log('获取用户类型失败')
  } finally {
    loading.value.userType = false
  }
}

const loadDistribution = async () => {
  loading.value.distribution = true
  try {
    const res = await request({
      url: '/analysis/user/distribution',
      method: 'get',
      timeout: 8000
    })
    distribution.value = res || {}
  } catch (e) {
    console.log('获取用户分布失败')
  } finally {
    loading.value.distribution = false
  }
}

const loadPlatformStats = async () => {
  loading.value.platform = true
  try {
    const res = await request({
      url: '/analysis/predict/platform',
      method: 'get',
      timeout: 8000
    })
    platformStats.value = res
  } catch (e) {
    console.log('获取平台统计失败')
  } finally {
    loading.value.platform = false
  }
}

const loadTrendingContent = async () => {
  loading.value.trending = true
  try {
    const res = await request({
      url: '/analysis/predict/trending',
      method: 'get',
      params: { top_n: 6 },
      timeout: 8000
    })
    trendingContent.value = res || []
  } catch (e) {
    console.log('获取趋势内容失败')
  } finally {
    loading.value.trending = false
  }
}

onMounted(() => {
  loadUserType()
  loadDistribution()
  loadPlatformStats()
  loadTrendingContent()
})
</script>

<style scoped>
.analytics-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  background: linear-gradient(180deg, #f0f4ff 0%, #faf5ff 50%, #fff5f5 100%);
  min-height: 100vh;
}

/* 页面标题 */
.page-header {
  text-align: center;
  margin-bottom: 32px;
  position: relative;
}

.header-content {
  position: relative;
  z-index: 1;
}

.page-title {
  font-size: 32px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.title-icon {
  font-size: 36px;
  -webkit-text-fill-color: initial;
}

.page-subtitle {
  font-size: 14px;
  color: #8b5cf6;
  margin: 0;
  font-weight: 500;
}

.header-decoration {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.1) 0%, transparent 70%);
  border-radius: 50%;
  z-index: 0;
}

/* 顶部统计卡片行 */
.stats-row {
  display: grid;
  grid-template-columns: 1.5fr 1fr 1fr 1.3fr;
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.12);
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(102, 126, 234, 0.2);
}

.card-glow {
  position: absolute;
  top: -50%;
  right: -50%;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, rgba(102, 126, 234, 0.15) 0%, transparent 60%);
  pointer-events: none;
}

/* 用户画像卡片 */
.user-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  color: white;
  border: none;
}

.user-card .card-glow {
  background: radial-gradient(circle, rgba(255, 255, 255, 0.2) 0%, transparent 60%);
}

.card-badge {
  position: absolute;
  top: 16px;
  right: 16px;
  background: rgba(255, 255, 255, 0.25);
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  backdrop-filter: blur(10px);
}

.badge-dot {
  width: 6px;
  height: 6px;
  background: #4ade80;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.2); }
}

.user-type-display {
  display: flex;
  align-items: center;
  gap: 20px;
}

.type-avatar {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  position: relative;
}

.avatar-icon {
  font-size: 36px;
  z-index: 1;
}

.avatar-ring {
  position: absolute;
  inset: -4px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 24px;
  animation: ring-pulse 3s infinite;
}

@keyframes ring-pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.05); opacity: 0.5; }
}

.type-content {
  flex: 1;
}

.type-label {
  font-size: 12px;
  opacity: 0.9;
  font-weight: 500;
}

.type-name {
  font-size: 24px;
  font-weight: 800;
  margin: 6px 0;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.type-desc {
  font-size: 13px;
  opacity: 0.9;
  margin: 0;
  line-height: 1.5;
}

.empty-state {
  display: flex;
  align-items: center;
  gap: 10px;
  opacity: 0.8;
  font-size: 14px;
}

/* 指标卡片 */
.metric-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.article-metric .card-glow {
  background: radial-gradient(circle, rgba(102, 126, 234, 0.2) 0%, transparent 60%);
}

.video-metric .card-glow {
  background: radial-gradient(circle, rgba(240, 147, 251, 0.2) 0%, transparent 60%);
}

.metric-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  color: white;
  position: relative;
  flex-shrink: 0;
}

.article-metric .metric-icon {
  background: linear-gradient(135deg, #667eea, #764ba2);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
}

.video-metric .metric-icon {
  background: linear-gradient(135deg, #f093fb, #f5576c);
  box-shadow: 0 8px 20px rgba(240, 147, 251, 0.4);
}

.icon-pulse {
  position: absolute;
  inset: 0;
  border-radius: 16px;
  background: inherit;
  animation: icon-pulse 2s infinite;
  z-index: -1;
}

@keyframes icon-pulse {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.15); opacity: 0; }
}

.metric-content {
  flex: 1;
}

.metric-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.metric-value {
  font-size: 32px;
  font-weight: 800;
  background: linear-gradient(135deg, #1e293b, #475569);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1.2;
}

.metric-growth {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  margin-top: 4px;
  padding: 4px 10px;
  border-radius: 20px;
  background: linear-gradient(135deg, #dcfce7, #bbf7d0);
  color: #16a34a;
}

/* 预测卡片 */
.predict-card {
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 50%, #ddd6fe 100%);
  border: none;
}

.predict-card .card-glow {
  background: radial-gradient(circle, rgba(99, 102, 241, 0.2) 0%, transparent 60%);
}

.predict-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 700;
  color: #4f46e5;
  margin-bottom: 16px;
}

.predict-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}

.predict-grid {
  display: flex;
  align-items: center;
}

.predict-item {
  flex: 1;
  text-align: center;
}

.predict-num {
  display: block;
  font-size: 28px;
  font-weight: 800;
  background: linear-gradient(135deg, #4338ca, #6366f1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.predict-label {
  font-size: 12px;
  color: #6366f1;
  font-weight: 500;
}

.predict-divider {
  width: 2px;
  height: 50px;
  background: linear-gradient(180deg, transparent, rgba(99, 102, 241, 0.3), transparent);
  display: flex;
  align-items: center;
  justify-content: center;
}

.divider-dot {
  width: 6px;
  height: 6px;
  background: #6366f1;
  border-radius: 50%;
}

.predict-badge {
  position: absolute;
  bottom: 12px;
  right: 16px;
  font-size: 10px;
  color: #6366f1;
  font-weight: 600;
  opacity: 0.7;
}

/* 主内容区 */
.main-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.content-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

.content-card:hover {
  box-shadow: 0 12px 40px rgba(102, 126, 234, 0.15);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.title-icon-wrap {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.title-icon-wrap.trending-icon {
  background: linear-gradient(135deg, #f59e0b, #ef4444);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
}

.header-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: linear-gradient(135deg, #f0f4ff, #e0e7ff);
  border-radius: 20px;
}

.badge-count {
  font-size: 16px;
  font-weight: 800;
  color: #4f46e5;
}

.badge-text {
  font-size: 12px;
  color: #6366f1;
}

/* 用户分布 */
.distribution-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dist-item {
  display: flex;
  align-items: center;
  gap: 14px;
  animation: slideIn 0.5s ease forwards;
  opacity: 0;
  transform: translateX(-20px);
}

@keyframes slideIn {
  to { opacity: 1; transform: translateX(0); }
}

.dist-info {
  width: 130px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.dist-icon {
  font-size: 24px;
}

.dist-text {
  display: flex;
  flex-direction: column;
}

.dist-name {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.dist-count {
  font-size: 11px;
  color: #94a3b8;
}

.dist-bar-wrap {
  flex: 1;
  height: 12px;
  background: linear-gradient(90deg, #f1f5f9, #e2e8f0);
  border-radius: 6px;
  overflow: hidden;
}

.dist-bar {
  height: 100%;
  border-radius: 6px;
  transition: width 0.8s ease;
  position: relative;
  overflow: hidden;
}

.bar-shine {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  animation: shine 2s infinite;
}

@keyframes shine {
  to { left: 100%; }
}

.dist-percent {
  width: 50px;
  text-align: right;
  font-size: 14px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* 热度上升 */
.trending-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.trending-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: linear-gradient(135deg, #fafafa, #f5f5f5);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  animation: slideIn 0.5s ease forwards;
  opacity: 0;
  transform: translateX(-20px);
  border: 1px solid transparent;
}

.trending-item:hover {
  background: linear-gradient(135deg, #fff7ed, #fef3c7);
  transform: translateX(6px);
  border-color: #fbbf24;
  box-shadow: 0 4px 16px rgba(251, 191, 36, 0.2);
}

.trending-rank {
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #f59e0b, #d97706);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 800;
  color: white;
  box-shadow: 0 4px 10px rgba(245, 158, 11, 0.3);
}

.trending-item:nth-child(1) .trending-rank {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  box-shadow: 0 4px 10px rgba(239, 68, 68, 0.3);
}

.trending-item:nth-child(2) .trending-rank {
  background: linear-gradient(135deg, #f97316, #ea580c);
  box-shadow: 0 4px 10px rgba(249, 115, 22, 0.3);
}

.trending-item:nth-child(3) .trending-rank {
  background: linear-gradient(135deg, #eab308, #ca8a04);
  box-shadow: 0 4px 10px rgba(234, 179, 8, 0.3);
}

.trending-type {
  font-size: 20px;
}

.trending-info {
  flex: 1;
  min-width: 0;
}

.trending-title {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.trending-stats {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  font-size: 12px;
}

.stat-current {
  color: #94a3b8;
}

.arrow {
  font-size: 10px;
  color: #cbd5e1;
}

.stat-predict {
  color: #1e293b;
  font-weight: 700;
}

.trending-growth {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  color: white;
  display: flex;
  align-items: center;
  gap: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 算法说明 */
.algo-section {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 28px;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.algo-header {
  text-align: center;
  margin-bottom: 24px;
}

.algo-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 800;
  color: #1e293b;
}

.algo-icon {
  font-size: 28px;
}

.algo-subtitle {
  font-size: 13px;
  color: #64748b;
  margin-top: 6px;
}

.algo-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.algo-item {
  display: flex;
  gap: 16px;
  padding: 20px;
  border-radius: 16px;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.kmeans-item {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
}

.kmeans-item:hover {
  border-color: #f59e0b;
  box-shadow: 0 8px 24px rgba(245, 158, 11, 0.2);
}

.regression-item {
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
}

.regression-item:hover {
  border-color: #3b82f6;
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.2);
}

.tfidf-item {
  background: linear-gradient(135deg, #dcfce7, #bbf7d0);
}

.tfidf-item:hover {
  border-color: #22c55e;
  box-shadow: 0 8px 24px rgba(34, 197, 94, 0.2);
}

.algo-badge {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 800;
  color: white;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
}

.algo-badge.kmeans {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  box-shadow: 0 6px 16px rgba(245, 158, 11, 0.4);
}

.algo-badge.regression {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.4);
}

.algo-badge.tfidf {
  background: linear-gradient(135deg, #22c55e, #16a34a);
  box-shadow: 0 6px 16px rgba(34, 197, 94, 0.4);
}

.badge-glow {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.3), transparent);
}

.algo-content h4 {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.algo-content p {
  margin: 0 0 12px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.6;
}

.algo-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.algo-tag {
  padding: 4px 10px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 12px;
  font-size: 10px;
  font-weight: 600;
  color: #475569;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .analytics-page {
    padding: 16px;
  }
  
  .page-title {
    font-size: 24px;
  }
  
  .stats-row {
    grid-template-columns: 1fr;
  }
  
  .main-content {
    grid-template-columns: 1fr;
  }
  
  .algo-grid {
    grid-template-columns: 1fr;
  }
}
</style>
