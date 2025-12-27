<template>
  <div class="algorithm-dashboard">
    <!-- 用户类型卡片 -->
    <div class="user-type-card" v-if="userType">
      <div class="card-header">
        <el-icon :size="20"><User /></el-icon>
        <span>我的用户画像</span>
        <el-tag size="small" effect="plain">K-Means聚类</el-tag>
      </div>
      <div class="type-content">
        <div class="type-icon" :style="{ background: userType.color }">
          {{ userType.icon }}
        </div>
        <div class="type-info">
          <h3>{{ userType.name }}</h3>
          <p>{{ userType.desc }}</p>
        </div>
      </div>
    </div>

    <!-- 用户分布统计 -->
    <div class="distribution-card" v-if="distribution.distribution">
      <div class="card-header">
        <el-icon :size="20"><PieChart /></el-icon>
        <span>用户类型分布</span>
        <span class="total">共 {{ distribution.total }} 用户</span>
      </div>
      <div class="distribution-list">
        <div 
          v-for="item in distribution.distribution" 
          :key="item.type"
          class="dist-item"
        >
          <div class="dist-label">
            <span class="dist-icon">{{ item.icon }}</span>
            <span>{{ item.name }}</span>
          </div>
          <div class="dist-bar-wrapper">
            <div 
              class="dist-bar" 
              :style="{ width: item.percentage + '%', background: item.color }"
            ></div>
          </div>
          <span class="dist-value">{{ item.percentage }}%</span>
        </div>
      </div>
    </div>

    <!-- 平台趋势统计 -->
    <div class="platform-stats-card" v-if="platformStats">
      <div class="card-header">
        <el-icon :size="20"><TrendCharts /></el-icon>
        <span>平台热度趋势</span>
        <el-tag size="small" :type="platformStats.platform_trend === 'rising' ? 'success' : 'info'">
          {{ platformStats.platform_trend === 'rising' ? '📈 上升' : '📊 平稳' }}
        </el-tag>
      </div>
      <div class="stats-grid">
        <div class="stat-item">
          <div class="stat-label">文章总浏览</div>
          <div class="stat-value">{{ formatNumber(platformStats.total_article_views) }}</div>
          <div class="stat-growth positive">
            +{{ platformStats.article_growth_rate }}%
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-label">视频总播放</div>
          <div class="stat-value">{{ formatNumber(platformStats.total_video_views) }}</div>
          <div class="stat-growth positive">
            +{{ platformStats.video_growth_rate }}%
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-label">预测文章周浏览</div>
          <div class="stat-value">{{ formatNumber(platformStats.predicted_article_views_next_week) }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">预测视频周播放</div>
          <div class="stat-value">{{ formatNumber(platformStats.predicted_video_views_next_week) }}</div>
        </div>
      </div>
    </div>

    <!-- 热度上升内容 -->
    <div class="trending-card" v-if="trendingContent.length > 0">
      <div class="card-header">
        <el-icon :size="20"><Top /></el-icon>
        <span>热度上升内容</span>
        <el-tag size="small" effect="plain">线性回归预测</el-tag>
      </div>
      <div class="trending-list">
        <div 
          v-for="item in trendingContent" 
          :key="`${item.type}-${item.id}`"
          class="trending-item"
          @click="goToContent(item)"
        >
          <el-tag :type="item.type === 'article' ? 'primary' : 'success'" size="small">
            {{ item.type === 'article' ? '文章' : '视频' }}
          </el-tag>
          <span class="trending-title">{{ item.title }}</span>
          <div class="trending-stats">
            <span class="current">{{ item.current_views }}次</span>
            <el-icon class="arrow"><Right /></el-icon>
            <span class="predicted">{{ item.predicted_views_7d }}次</span>
            <span class="growth" :style="{ color: item.trend_color }">
              +{{ item.growth_rate }}%
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p>正在加载算法分析数据...</p>
    </div>

    <!-- 错误状态 -->
    <div v-if="error" class="error-state">
      <el-icon :size="48"><WarningFilled /></el-icon>
      <p>{{ error }}</p>
      <el-button @click="loadData">重试</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, PieChart, TrendCharts, Top, Right, Loading, WarningFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const error = ref(null)

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
  try {
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    if (user.id) {
      const res = await request({
        url: `/analysis/user/type/${user.id}`,
        method: 'get',
        timeout: 5000
      })
      if (res) {
        userType.value = res
      }
    }
  } catch (e) {
    console.log('获取用户类型失败:', e)
  }
}

const loadDistribution = async () => {
  try {
    const res = await request({
      url: '/analysis/user/distribution',
      method: 'get',
      timeout: 5000
    })
    if (res) {
      distribution.value = res
    }
  } catch (e) {
    console.log('获取用户分布失败:', e)
  }
}

const loadPlatformStats = async () => {
  try {
    const res = await request({
      url: '/analysis/predict/platform',
      method: 'get',
      timeout: 5000
    })
    if (res) {
      platformStats.value = res
    }
  } catch (e) {
    console.log('获取平台统计失败:', e)
  }
}

const loadTrendingContent = async () => {
  try {
    const res = await request({
      url: '/analysis/predict/trending',
      method: 'get',
      params: { top_n: 5 },
      timeout: 5000
    })
    if (res && Array.isArray(res)) {
      trendingContent.value = res
    }
  } catch (e) {
    console.log('获取趋势内容失败:', e)
  }
}

const loadData = async () => {
  loading.value = true
  error.value = null
  
  try {
    await Promise.all([
      loadUserType(),
      loadDistribution(),
      loadPlatformStats(),
      loadTrendingContent()
    ])
  } catch (e) {
    error.value = '加载数据失败，请确保算法服务已启动'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.algorithm-dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-weight: 600;
  color: #303133;
}

.card-header .total {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
  font-weight: normal;
}

/* 用户类型卡片 */
.user-type-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 20px;
  color: white;
}

.user-type-card .card-header {
  color: white;
}

.user-type-card .card-header .el-tag {
  background: rgba(255,255,255,0.2);
  border: none;
  color: white;
}

.type-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.type-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  background: rgba(255,255,255,0.2);
}

.type-info h3 {
  margin: 0 0 4px;
  font-size: 20px;
}

.type-info p {
  margin: 0;
  opacity: 0.9;
  font-size: 14px;
}

/* 分布卡片 */
.distribution-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.distribution-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dist-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dist-label {
  width: 100px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.dist-icon {
  font-size: 16px;
}

.dist-bar-wrapper {
  flex: 1;
  height: 8px;
  background: #f0f2f5;
  border-radius: 4px;
  overflow: hidden;
}

.dist-bar {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.dist-value {
  width: 50px;
  text-align: right;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

/* 平台统计卡片 */
.platform-stats-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.stat-item {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 16px;
  text-align: center;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.stat-growth {
  font-size: 12px;
  margin-top: 4px;
}

.stat-growth.positive {
  color: #22c55e;
}

/* 趋势内容卡片 */
.trending-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

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
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trending-stats {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.trending-stats .current {
  color: #909399;
}

.trending-stats .arrow {
  color: #c0c4cc;
}

.trending-stats .predicted {
  color: #303133;
  font-weight: 600;
}

.trending-stats .growth {
  font-weight: 600;
}

/* 加载和错误状态 */
.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #909399;
}

.loading-state .is-loading {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.error-state .el-icon {
  color: #f56c6c;
  margin-bottom: 12px;
}
</style>
