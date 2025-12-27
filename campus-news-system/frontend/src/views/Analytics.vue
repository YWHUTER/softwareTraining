<template>
  <div class="analytics-page">
    <div class="page-header">
      <h1>
        <el-icon><DataAnalysis /></el-icon>
        智能算法分析
      </h1>
      <p>基于机器学习的用户画像与热度预测</p>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：用户分析 -->
      <el-col :xs="24" :lg="12">
        <!-- 我的用户类型 -->
        <el-card class="analysis-card user-type-card" v-loading="loading.userType">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="20"><User /></el-icon>
                <span>我的用户画像</span>
              </div>
              <el-tag effect="plain" size="small">K-Means 聚类算法</el-tag>
            </div>
          </template>
          
          <div class="type-display" v-if="userType">
            <div class="type-icon-wrapper" :style="{ background: userType.color }">
              <span class="type-emoji">{{ userType.icon }}</span>
            </div>
            <div class="type-info">
              <h2>{{ userType.name }}</h2>
              <p>{{ userType.desc }}</p>
            </div>
          </div>
          <el-empty v-else description="暂无数据" />
        </el-card>

        <!-- 用户类型分布 -->
        <el-card class="analysis-card" v-loading="loading.distribution">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="20"><PieChart /></el-icon>
                <span>平台用户分布</span>
              </div>
              <span class="total-count">共 {{ distribution.total || 0 }} 用户</span>
            </div>
          </template>
          
          <div class="distribution-chart" v-if="distribution.distribution?.length">
            <div 
              v-for="item in distribution.distribution" 
              :key="item.type"
              class="dist-row"
            >
              <div class="dist-label">
                <span class="dist-icon">{{ item.icon }}</span>
                <span class="dist-name">{{ item.name }}</span>
              </div>
              <div class="dist-bar-container">
                <div 
                  class="dist-bar" 
                  :style="{ 
                    width: item.percentage + '%', 
                    background: `linear-gradient(90deg, ${item.color}, ${item.color}88)`
                  }"
                >
                  <span class="bar-label" v-if="item.percentage > 15">{{ item.count }}人</span>
                </div>
              </div>
              <span class="dist-percent">{{ item.percentage }}%</span>
            </div>
          </div>
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>

      <!-- 右侧：热度预测 -->
      <el-col :xs="24" :lg="12">
        <!-- 平台趋势 -->
        <el-card class="analysis-card platform-card" v-loading="loading.platform">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="20"><TrendCharts /></el-icon>
                <span>平台热度趋势</span>
              </div>
              <el-tag 
                :type="platformStats?.platform_trend === 'rising' ? 'success' : 'info'" 
                effect="dark"
                size="small"
              >
                {{ platformStats?.platform_trend === 'rising' ? '📈 上升中' : '📊 平稳' }}
              </el-tag>
            </div>
          </template>
          
          <div class="platform-stats" v-if="platformStats">
            <div class="stats-grid">
              <div class="stat-box">
                <div class="stat-icon article">
                  <el-icon><Document /></el-icon>
                </div>
                <div class="stat-content">
                  <span class="stat-label">文章总浏览</span>
                  <span class="stat-value">{{ formatNumber(platformStats.total_article_views) }}</span>
                  <span class="stat-growth positive">
                    <el-icon><Top /></el-icon>
                    {{ platformStats.article_growth_rate }}%
                  </span>
                </div>
              </div>
              <div class="stat-box">
                <div class="stat-icon video">
                  <el-icon><VideoCamera /></el-icon>
                </div>
                <div class="stat-content">
                  <span class="stat-label">视频总播放</span>
                  <span class="stat-value">{{ formatNumber(platformStats.total_video_views) }}</span>
                  <span class="stat-growth positive">
                    <el-icon><Top /></el-icon>
                    {{ platformStats.video_growth_rate }}%
                  </span>
                </div>
              </div>
            </div>
            
            <div class="prediction-section">
              <h4>
                <el-icon><Aim /></el-icon>
                下周预测 (线性回归)
              </h4>
              <div class="prediction-grid">
                <div class="prediction-item">
                  <span class="pred-label">预测文章浏览</span>
                  <span class="pred-value">{{ formatNumber(platformStats.predicted_article_views_next_week) }}</span>
                </div>
                <div class="prediction-item">
                  <span class="pred-label">预测视频播放</span>
                  <span class="pred-value">{{ formatNumber(platformStats.predicted_video_views_next_week) }}</span>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无数据" />
        </el-card>

        <!-- 热度上升内容 -->
        <el-card class="analysis-card trending-card" v-loading="loading.trending">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="20"><Sunrise /></el-icon>
                <span>热度上升内容</span>
              </div>
              <el-tag effect="plain" size="small">线性回归预测</el-tag>
            </div>
          </template>
          
          <div class="trending-list" v-if="trendingContent.length">
            <div 
              v-for="item in trendingContent" 
              :key="`${item.type}-${item.id}`"
              class="trending-item"
              @click="goToContent(item)"
            >
              <el-tag 
                :type="item.type === 'article' ? '' : 'success'" 
                size="small"
                effect="dark"
              >
                {{ item.type === 'article' ? '📄 文章' : '🎬 视频' }}
              </el-tag>
              <span class="trending-title">{{ item.title }}</span>
              <div class="trending-prediction">
                <span class="current">{{ item.current_views }}</span>
                <el-icon><Right /></el-icon>
                <span class="predicted">{{ item.predicted_views_7d }}</span>
                <span class="growth-badge" :style="{ background: item.trend_color }">
                  +{{ item.growth_rate }}%
                </span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无上升趋势内容" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 算法说明 -->
    <el-card class="algorithm-info-card">
      <template #header>
        <div class="card-header">
          <el-icon :size="20"><InfoFilled /></el-icon>
          <span>算法说明</span>
        </div>
      </template>
      <div class="algorithm-list">
        <div class="algorithm-item">
          <div class="algo-icon kmeans">K</div>
          <div class="algo-info">
            <h4>K-Means 聚类算法</h4>
            <p>基于用户行为特征（发布数、评论数、浏览量等）将用户分为5类：活跃创作者、深度阅读者、社交达人、视频爱好者、潜水用户</p>
          </div>
        </div>
        <div class="algorithm-item">
          <div class="algo-icon regression">R</div>
          <div class="algo-info">
            <h4>线性回归预测</h4>
            <p>基于内容特征（标题长度、发布时间、历史数据等）预测文章/视频未来7天和30天的浏览量趋势</p>
          </div>
        </div>
        <div class="algorithm-item">
          <div class="algo-icon tfidf">T</div>
          <div class="algo-info">
            <h4>TF-IDF 热词提取</h4>
            <p>使用词频-逆文档频率算法从文章和视频标题中提取热门关键词，展示在首页热词云</p>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { 
  DataAnalysis, User, PieChart, TrendCharts, Document, VideoCamera,
  Top, Right, Sunrise, Aim, InfoFilled
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
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-header h1 {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-size: 28px;
  color: #303133;
  margin: 0 0 8px;
}

.page-header p {
  color: #909399;
  margin: 0;
}

.analysis-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.total-count {
  font-size: 12px;
  color: #909399;
}

/* 用户类型卡片 */
.user-type-card :deep(.el-card__body) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 0 0 12px 12px;
}

.type-display {
  display: flex;
  align-items: center;
  gap: 20px;
  color: white;
}

.type-icon-wrapper {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,0.2) !important;
}

.type-emoji {
  font-size: 40px;
}

.type-info h2 {
  margin: 0 0 8px;
  font-size: 24px;
}

.type-info p {
  margin: 0;
  opacity: 0.9;
}

/* 分布图表 */
.distribution-chart {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dist-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dist-label {
  width: 110px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.dist-icon {
  font-size: 20px;
}

.dist-name {
  font-size: 13px;
  color: #606266;
}

.dist-bar-container {
  flex: 1;
  height: 24px;
  background: #f5f7fa;
  border-radius: 12px;
  overflow: hidden;
}

.dist-bar {
  height: 100%;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 8px;
  transition: width 0.5s ease;
}

.bar-label {
  font-size: 11px;
  color: white;
  font-weight: 600;
}

.dist-percent {
  width: 50px;
  text-align: right;
  font-weight: 600;
  color: #303133;
}

/* 平台统计 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 12px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-icon.article {
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.stat-icon.video {
  background: linear-gradient(135deg, #f093fb, #f5576c);
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.stat-growth {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
}

.stat-growth.positive {
  color: #22c55e;
}

.prediction-section {
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  border-radius: 12px;
  padding: 16px;
}

.prediction-section h4 {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 12px;
  font-size: 14px;
  color: #4f46e5;
}

.prediction-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.prediction-item {
  text-align: center;
}

.pred-label {
  display: block;
  font-size: 12px;
  color: #6366f1;
  margin-bottom: 4px;
}

.pred-value {
  font-size: 20px;
  font-weight: 700;
  color: #4f46e5;
}

/* 趋势列表 */
.trending-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.trending-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.trending-item:hover {
  background: #f0f2f5;
  transform: translateX(4px);
}

.trending-title {
  flex: 1;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trending-prediction {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.trending-prediction .current {
  color: #909399;
}

.trending-prediction .predicted {
  color: #303133;
  font-weight: 600;
}

.growth-badge {
  padding: 2px 6px;
  border-radius: 4px;
  color: white;
  font-size: 11px;
  font-weight: 600;
}

/* 算法说明 */
.algorithm-info-card {
  margin-top: 20px;
}

.algorithm-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

@media (max-width: 992px) {
  .algorithm-list {
    grid-template-columns: 1fr;
  }
}

.algorithm-item {
  display: flex;
  gap: 12px;
}

.algo-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  color: white;
  flex-shrink: 0;
}

.algo-icon.kmeans {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.algo-icon.regression {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
}

.algo-icon.tfidf {
  background: linear-gradient(135deg, #22c55e, #16a34a);
}

.algo-info h4 {
  margin: 0 0 4px;
  font-size: 14px;
  color: #303133;
}

.algo-info p {
  margin: 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
</style>
