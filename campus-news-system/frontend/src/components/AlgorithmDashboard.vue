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
/**
 * 算法分析仪表板组件
 * 
 * 功能说明：
 * 1. 用户画像展示 - 基于K-Means聚类的用户类型分析
 * 2. 用户分布统计 - 展示各类型用户的占比
 * 3. 平台趋势统计 - 基于线性回归的热度预测
 * 4. 热度上升内容 - 展示增长率最高的内容
 * 
 * 技术要点：
 * - 并行请求优化加载速度
 * - 错误处理和降级方案
 * - 数据格式化和可视化
 */
import { ref, onMounted, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, PieChart, TrendCharts, Top, Right, Loading, WarningFilled, Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'

// ==================== 路由和状态 ====================
const router = useRouter()
const loading = ref(false)
const error = ref(null)
const lastUpdateTime = ref(null)
const autoRefreshTimer = ref(null)

// ==================== 数据状态 ====================
const userType = ref(null)
const distribution = ref({})
const platformStats = ref(null)
const trendingContent = ref([])
const serviceStatus = ref('unknown') // unknown, online, offline

// ==================== 计算属性 ====================
const formattedUpdateTime = computed(() => {
  if (!lastUpdateTime.value) return ''
  const now = new Date()
  const diff = Math.floor((now - lastUpdateTime.value) / 1000 / 60)
  if (diff < 1) return '刚刚更新'
  if (diff < 60) return `${diff}分钟前更新`
  return `${Math.floor(diff / 60)}小时前更新`
})

const hasData = computed(() => {
  return userType.value || distribution.value.distribution || platformStats.value || trendingContent.value.length > 0
})

// ==================== 工具方法 ====================

/**
 * 格式化数字显示
 * @param {number} num - 原始数字
 * @returns {string} 格式化后的字符串
 */
const formatNumber = (num) => {
  if (!num) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num.toString()
}

/**
 * 格式化百分比
 * @param {number} value - 百分比值
 * @returns {string} 格式化后的百分比
 */
const formatPercent = (value) => {
  if (!value) return '0%'
  return value.toFixed(1) + '%'
}

/**
 * 跳转到内容详情页
 * @param {Object} item - 内容项
 */
const goToContent = (item) => {
  if (item.type === 'article') {
    router.push(`/article/${item.id}`)
  } else {
    router.push(`/video/${item.id}`)
  }
}

/**
 * 获取趋势图标
 * @param {string} trend - 趋势类型
 * @returns {string} 图标
 */
const getTrendIcon = (trend) => {
  const icons = {
    'rising': '📈',
    'stable': '📊',
    'declining': '📉'
  }
  return icons[trend] || '📊'
}

// ==================== 数据加载方法 ====================

/**
 * 检查算法服务状态
 */
const checkServiceStatus = async () => {
  try {
    const res = await request({
      url: '/analysis/health',
      method: 'get',
      timeout: 3000
    })
    serviceStatus.value = res ? 'online' : 'offline'
  } catch (e) {
    serviceStatus.value = 'offline'
    console.log('算法服务状态检查失败:', e.message)
  }
}

/**
 * 加载用户类型数据
 */
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
        console.log('用户类型加载成功:', res.name)
      }
    }
  } catch (e) {
    console.log('获取用户类型失败:', e.message)
  }
}

/**
 * 加载用户分布数据
 */
const loadDistribution = async () => {
  try {
    const res = await request({
      url: '/analysis/user/distribution',
      method: 'get',
      timeout: 5000
    })
    if (res) {
      distribution.value = res
      console.log('用户分布加载成功，总用户数:', res.total)
    }
  } catch (e) {
    console.log('获取用户分布失败:', e.message)
  }
}

/**
 * 加载平台统计数据
 */
const loadPlatformStats = async () => {
  try {
    const res = await request({
      url: '/analysis/predict/platform',
      method: 'get',
      timeout: 5000
    })
    if (res) {
      platformStats.value = res
      console.log('平台统计加载成功，趋势:', res.platform_trend)
    }
  } catch (e) {
    console.log('获取平台统计失败:', e.message)
  }
}

/**
 * 加载热度上升内容
 */
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
      console.log('趋势内容加载成功，数量:', res.length)
    }
  } catch (e) {
    console.log('获取趋势内容失败:', e.message)
  }
}

/**
 * 加载所有数据（并行请求）
 */
const loadData = async () => {
  loading.value = true
  error.value = null
  
  try {
    // 先检查服务状态
    await checkServiceStatus()
    
    if (serviceStatus.value === 'offline') {
      error.value = '算法服务未启动，请先启动算法服务'
      return
    }
    
    // 并行加载所有数据
    await Promise.all([
      loadUserType(),
      loadDistribution(),
      loadPlatformStats(),
      loadTrendingContent()
    ])
    
    lastUpdateTime.value = new Date()
    
    if (!hasData.value) {
      error.value = '暂无分析数据'
    }
  } catch (e) {
    error.value = '加载数据失败，请确保算法服务已启动'
    console.error('数据加载失败:', e)
  } finally {
    loading.value = false
  }
}

/**
 * 手动刷新数据
 */
const refreshData = () => {
  console.log('手动刷新数据')
  loadData()
}

/**
 * 启动自动刷新
 * @param {number} interval - 刷新间隔（毫秒）
 */
const startAutoRefresh = (interval = 60000) => {
  stopAutoRefresh()
  autoRefreshTimer.value = setInterval(() => {
    console.log('自动刷新数据')
    loadData()
  }, interval)
}

/**
 * 停止自动刷新
 */
const stopAutoRefresh = () => {
  if (autoRefreshTimer.value) {
    clearInterval(autoRefreshTimer.value)
    autoRefreshTimer.value = null
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadData()
  // 启动自动刷新（每5分钟）
  startAutoRefresh(300000)
})

onUnmounted(() => {
  stopAutoRefresh()
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
