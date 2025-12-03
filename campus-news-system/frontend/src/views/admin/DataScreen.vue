<template>
  <div class="data-screen">
    <!-- 顶部标题栏 -->
    <header class="screen-header">
      <div class="header-left">
        <el-button 
          type="primary" 
          :icon="Back" 
          @click="$router.back()" 
          circle 
          class="back-btn"
        />
      </div>
      <div class="header-center">
        <h1 class="title">
          <span class="title-icon">📊</span>
          校园新闻数据可视化大屏
        </h1>
        <div class="current-time">{{ currentTime }}</div>
      </div>
      <div class="header-right">
        <div class="status-badge">
          <span class="status-dot"></span>
          实时数据
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="screen-main">
      <!-- 左侧面板 -->
      <section class="panel panel-left">
        <!-- 核心指标 -->
        <div class="card metrics-card">
          <div class="card-title">📈 核心指标</div>
          <div class="metrics-grid">
            <div class="metric-item">
              <div class="metric-value counter" :data-target="statistics.userCount">
                {{ animatedStats.userCount }}
              </div>
              <div class="metric-label">用户总数</div>
              <div class="metric-icon user-icon">👥</div>
            </div>
            <div class="metric-item">
              <div class="metric-value counter">{{ animatedStats.articleCount }}</div>
              <div class="metric-label">文章总数</div>
              <div class="metric-icon article-icon">📰</div>
            </div>
            <div class="metric-item">
              <div class="metric-value counter">{{ animatedStats.commentCount }}</div>
              <div class="metric-label">评论总数</div>
              <div class="metric-icon comment-icon">💬</div>
            </div>
            <div class="metric-item">
              <div class="metric-value counter">{{ formatNumber(animatedStats.totalViews) }}</div>
              <div class="metric-label">总浏览量</div>
              <div class="metric-icon view-icon">👁️</div>
            </div>
          </div>
        </div>

        <!-- 文章分类分布 -->
        <div class="card">
          <div class="card-title">📊 文章分类分布</div>
          <div ref="categoryChartRef" class="chart-container"></div>
        </div>

        <!-- 用户活跃排行 -->
        <div class="card">
          <div class="card-title">🏆 活跃用户 TOP5</div>
          <div class="rank-list">
            <div 
              v-for="(user, index) in topUsers" 
              :key="user.id" 
              class="rank-item"
              :style="{ animationDelay: `${index * 0.1}s` }"
            >
              <span class="rank-num" :class="`rank-${index + 1}`">{{ index + 1 }}</span>
              <span class="rank-name">{{ user.realName || user.username }}</span>
              <span class="rank-value">{{ user.articleCount }} 篇</span>
              <div class="rank-bar" :style="{ width: `${(user.articleCount / maxArticleCount) * 100}%` }"></div>
            </div>
          </div>
        </div>
      </section>

      <!-- 中间面板 -->
      <section class="panel panel-center">
        <!-- 发布趋势大图 -->
        <div class="card card-large">
          <div class="card-title">📈 近30天文章发布趋势</div>
          <div ref="trendChartRef" class="chart-container-large"></div>
        </div>

        <!-- 底部数据流 -->
        <div class="card data-flow-card">
          <div class="card-title">🔥 实时热门文章</div>
          <div class="hot-articles-scroll">
            <div class="scroll-content">
              <div 
                v-for="article in hotArticles" 
                :key="article.id" 
                class="hot-article-item"
              >
                <span class="hot-title">{{ article.title }}</span>
                <span class="hot-views">
                  <el-icon><View /></el-icon>
                  {{ article.viewCount }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 右侧面板 -->
      <section class="panel panel-right">
        <!-- 今日数据 -->
        <div class="card today-card">
          <div class="card-title">📅 今日数据</div>
          <div class="today-stats">
            <div class="today-item">
              <div class="today-value">{{ todayStats.newArticles }}</div>
              <div class="today-label">新增文章</div>
            </div>
            <div class="today-item">
              <div class="today-value">{{ todayStats.newUsers }}</div>
              <div class="today-label">新增用户</div>
            </div>
            <div class="today-item">
              <div class="today-value">{{ todayStats.newComments }}</div>
              <div class="today-label">新增评论</div>
            </div>
          </div>
        </div>

        <!-- 审核状态 -->
        <div class="card">
          <div class="card-title">📋 文章审核状态</div>
          <div ref="statusChartRef" class="chart-container"></div>
        </div>

        <!-- 热门文章排行 -->
        <div class="card">
          <div class="card-title">🔥 浏览量排行 TOP5</div>
          <div ref="hotChartRef" class="chart-container"></div>
        </div>
      </section>
    </main>

    <!-- 底部装饰 -->
    <footer class="screen-footer">
      <div class="footer-line"></div>
      <div class="tech-info">武汉理工大学校园新闻发布系统 · 数据驱动决策</div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { Back, View } from '@element-plus/icons-vue'
import { getStatistics, getChartData } from '@/api/admin'
import { getArticleList } from '@/api/article'
import * as echarts from 'echarts'

// 当前时间
const currentTime = ref('')
let timeTimer = null

// 统计数据
const statistics = ref({
  userCount: 0,
  articleCount: 0,
  commentCount: 0,
  totalViews: 0
})

// 动画数字
const animatedStats = ref({
  userCount: 0,
  articleCount: 0,
  commentCount: 0,
  totalViews: 0
})

// 今日数据
const todayStats = ref({
  newArticles: 0,
  newUsers: 0,
  newComments: 0
})

// 热门文章
const hotArticles = ref([])
const topUsers = ref([])

// 图表引用
const categoryChartRef = ref(null)
const trendChartRef = ref(null)
const statusChartRef = ref(null)
const hotChartRef = ref(null)

let categoryChart = null
let trendChart = null
let statusChart = null
let hotChart = null

// 计算最大文章数
const maxArticleCount = computed(() => {
  if (topUsers.value.length === 0) return 1
  return Math.max(...topUsers.value.map(u => u.articleCount || 0), 1)
})

// 格式化数字
const formatNumber = (num) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num
}

// 更新时间
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 数字动画
const animateNumber = (key, target) => {
  const duration = 2000
  const steps = 60
  const increment = target / steps
  let current = 0
  
  const timer = setInterval(() => {
    current += increment
    if (current >= target) {
      animatedStats.value[key] = target
      clearInterval(timer)
    } else {
      animatedStats.value[key] = Math.floor(current)
    }
  }, duration / steps)
}

// 获取统计数据
const fetchStatistics = async () => {
  try {
    const data = await getStatistics()
    statistics.value = {
      userCount: data.userCount || 0,
      articleCount: data.articleCount || 0,
      commentCount: data.commentCount || 0,
      totalViews: data.totalViews || 0
    }
    
    // 启动数字动画
    Object.keys(statistics.value).forEach(key => {
      animateNumber(key, statistics.value[key])
    })
    
    // 模拟今日数据
    todayStats.value = {
      newArticles: Math.floor(Math.random() * 10) + 1,
      newUsers: Math.floor(Math.random() * 5) + 1,
      newComments: Math.floor(Math.random() * 20) + 5
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 获取热门文章
const fetchHotArticles = async () => {
  try {
    const data = await getArticleList({
      current: 1,
      size: 10,
      isApproved: 1,
      sortBy: 'view_count',
      sortOrder: 'desc'
    })
    hotArticles.value = data.records || []
  } catch (error) {
    console.error('获取热门文章失败:', error)
  }
}

// 获取活跃用户（模拟数据，实际应从后端获取）
const fetchTopUsers = async () => {
  try {
    const data = await getArticleList({
      current: 1,
      size: 100,
      isApproved: 1
    })
    
    // 统计每个用户的文章数
    const userMap = new Map()
    data.records?.forEach(article => {
      const author = article.author
      if (author) {
        const key = author.id
        if (!userMap.has(key)) {
          userMap.set(key, { ...author, articleCount: 0 })
        }
        userMap.get(key).articleCount++
      }
    })
    
    topUsers.value = Array.from(userMap.values())
      .sort((a, b) => b.articleCount - a.articleCount)
      .slice(0, 5)
  } catch (error) {
    console.error('获取活跃用户失败:', error)
  }
}

// 获取图表数据
const fetchChartData = async () => {
  try {
    const data = await getChartData()
    await nextTick()
    renderCategoryChart(data.categoryData || [])
    renderTrendChart(data.trendDates || [], data.trendCounts || [])
    renderStatusChart(data.statusData || [])
    renderHotChart(data.hotTitles || [], data.hotViews || [])
  } catch (error) {
    console.error('获取图表数据失败:', error)
  }
}

// 渲染分类饼图
const renderCategoryChart = (data) => {
  if (!categoryChartRef.value) return
  if (categoryChart) categoryChart.dispose()
  
  categoryChart = echarts.init(categoryChartRef.value)
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 篇 ({d}%)',
      backgroundColor: 'rgba(0,0,0,0.7)',
      borderColor: '#0ff',
      textStyle: { color: '#fff' }
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: { color: '#8ec6ff' }
    },
    color: ['#00d4ff', '#00ff88', '#ffaa00'],
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['35%', '50%'],
      itemStyle: {
        borderRadius: 8,
        borderColor: 'rgba(0,212,255,0.3)',
        borderWidth: 2
      },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold', color: '#fff' },
        itemStyle: { shadowBlur: 20, shadowColor: 'rgba(0,212,255,0.5)' }
      },
      data: data
    }]
  }
  
  categoryChart.setOption(option)
}

// 渲染趋势图
const renderTrendChart = (dates, counts) => {
  if (!trendChartRef.value) return
  if (trendChart) trendChart.dispose()
  
  trendChart = echarts.init(trendChartRef.value)
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0,0,0,0.7)',
      borderColor: '#0ff',
      textStyle: { color: '#fff' }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLine: { lineStyle: { color: 'rgba(0,212,255,0.3)' } },
      axisLabel: { color: '#8ec6ff', fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      axisLabel: { color: '#8ec6ff' },
      splitLine: { lineStyle: { color: 'rgba(0,212,255,0.1)' } }
    },
    series: [{
      name: '发布数量',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#00d4ff', width: 3, shadowColor: 'rgba(0,212,255,0.5)', shadowBlur: 10 },
      itemStyle: { color: '#00d4ff', borderColor: '#fff', borderWidth: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(0,212,255,0.4)' },
          { offset: 1, color: 'rgba(0,212,255,0.05)' }
        ])
      },
      data: counts
    }]
  }
  
  trendChart.setOption(option)
}

// 渲染审核状态图
const renderStatusChart = (data) => {
  if (!statusChartRef.value) return
  if (statusChart) statusChart.dispose()
  
  statusChart = echarts.init(statusChartRef.value)
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(0,0,0,0.7)',
      borderColor: '#0ff',
      textStyle: { color: '#fff' }
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: { color: '#8ec6ff' }
    },
    color: ['#00ff88', '#ffaa00', '#ff4757'],
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['35%', '50%'],
      itemStyle: { borderRadius: 8, borderColor: 'rgba(0,255,136,0.3)', borderWidth: 2 },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold', color: '#fff' }
      },
      data: data
    }]
  }
  
  statusChart.setOption(option)
}

// 渲染热门排行图
const renderHotChart = (titles, views) => {
  if (!hotChartRef.value) return
  if (hotChart) hotChart.dispose()
  
  hotChart = echarts.init(hotChartRef.value)
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(0,0,0,0.7)',
      borderColor: '#0ff',
      textStyle: { color: '#fff' }
    },
    grid: { left: '3%', right: '10%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: {
      type: 'value',
      axisLine: { show: false },
      axisLabel: { color: '#8ec6ff' },
      splitLine: { lineStyle: { color: 'rgba(0,212,255,0.1)' } }
    },
    yAxis: {
      type: 'category',
      data: titles.slice(0, 5).reverse(),
      axisLine: { lineStyle: { color: 'rgba(0,212,255,0.3)' } },
      axisLabel: { color: '#8ec6ff', width: 80, overflow: 'truncate', fontSize: 11 }
    },
    series: [{
      type: 'bar',
      barWidth: 16,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#ff4757' },
          { offset: 1, color: '#ff6b81' }
        ]),
        borderRadius: [0, 8, 8, 0]
      },
      data: views.slice(0, 5).reverse()
    }]
  }
  
  hotChart.setOption(option)
}

// 窗口调整
const handleResize = () => {
  categoryChart?.resize()
  trendChart?.resize()
  statusChart?.resize()
  hotChart?.resize()
}

onMounted(async () => {
  updateTime()
  timeTimer = setInterval(updateTime, 1000)
  
  await Promise.all([
    fetchStatistics(),
    fetchHotArticles(),
    fetchTopUsers(),
    fetchChartData()
  ])
  
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  clearInterval(timeTimer)
  window.removeEventListener('resize', handleResize)
  categoryChart?.dispose()
  trendChart?.dispose()
  statusChart?.dispose()
  hotChart?.dispose()
})
</script>

<style scoped>
.data-screen {
  min-height: 100vh;
  min-width: 1400px;
  background: linear-gradient(135deg, #0c1929 0%, #1a2a4a 50%, #0c1929 100%);
  color: #fff;
  overflow-x: auto;
  position: relative;
}

/* 背景动画 */
.data-screen::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 80%, rgba(0,212,255,0.1) 0%, transparent 40%),
    radial-gradient(circle at 80% 20%, rgba(0,255,136,0.1) 0%, transparent 40%);
  pointer-events: none;
}

/* 顶部 */
.screen-header {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30px;
  background: linear-gradient(180deg, rgba(0,20,40,0.9) 0%, transparent 100%);
  border-bottom: 1px solid rgba(0,212,255,0.2);
  position: relative;
  z-index: 10;
}

.back-btn {
  background: rgba(0,212,255,0.2) !important;
  border: 1px solid rgba(0,212,255,0.5) !important;
}

.header-center {
  text-align: center;
}

.title {
  font-size: 28px;
  font-weight: 600;
  margin: 0;
  background: linear-gradient(90deg, #00d4ff, #00ff88);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 30px rgba(0,212,255,0.5);
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon {
  font-size: 32px;
  -webkit-text-fill-color: initial;
}

.current-time {
  font-size: 14px;
  color: #8ec6ff;
  margin-top: 5px;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(0,255,136,0.1);
  border: 1px solid rgba(0,255,136,0.3);
  border-radius: 20px;
  font-size: 13px;
  color: #00ff88;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: #00ff88;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.2); }
}

/* 主内容 */
.screen-main {
  display: flex;
  gap: 20px;
  padding: 20px 30px;
  height: calc(100vh - 140px);
  position: relative;
  z-index: 10;
}

.panel {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.panel-left, .panel-right {
  width: 25%;
  min-width: 280px;
  flex-shrink: 0;
}

.panel-center {
  flex: 1;
}

/* 卡片 */
.card {
  background: rgba(0,20,40,0.6);
  border: 1px solid rgba(0,212,255,0.2);
  border-radius: 12px;
  padding: 15px;
  backdrop-filter: blur(10px);
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-large {
  flex: 2;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #00d4ff;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(0,212,255,0.2);
}

/* 核心指标 */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  flex: 1;
}

.metric-item {
  background: rgba(0,212,255,0.05);
  border: 1px solid rgba(0,212,255,0.15);
  border-radius: 10px;
  padding: 12px;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.metric-value {
  font-size: 24px;
  font-weight: 700;
  color: #00d4ff;
  text-shadow: 0 0 20px rgba(0,212,255,0.5);
}

.metric-label {
  font-size: 11px;
  color: #8ec6ff;
  margin-top: 4px;
}

.metric-icon {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 16px;
  opacity: 0.6;
}

/* 排行榜 */
.rank-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: rgba(0,212,255,0.05);
  border-radius: 6px;
  position: relative;
  overflow: hidden;
  animation: slideIn 0.5s ease-out both;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateX(-20px); }
  to { opacity: 1; transform: translateX(0); }
}

.rank-num {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  background: rgba(0,212,255,0.2);
  color: #8ec6ff;
}

.rank-1 { background: linear-gradient(135deg, #ffd700, #ffaa00); color: #000; }
.rank-2 { background: linear-gradient(135deg, #c0c0c0, #a0a0a0); color: #000; }
.rank-3 { background: linear-gradient(135deg, #cd7f32, #b87333); color: #fff; }

.rank-name {
  flex: 1;
  font-size: 13px;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-value {
  font-size: 12px;
  color: #00d4ff;
  font-weight: 600;
}

.rank-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  height: 2px;
  background: linear-gradient(90deg, #00d4ff, transparent);
}

/* 图表容器 */
.chart-container {
  flex: 1;
  min-height: 150px;
}

.chart-container-large {
  flex: 1;
  min-height: 250px;
}

/* 今日数据 */
.today-stats {
  display: flex;
  justify-content: space-around;
  flex: 1;
  align-items: center;
}

.today-item {
  text-align: center;
}

.today-value {
  font-size: 28px;
  font-weight: 700;
  color: #00ff88;
  text-shadow: 0 0 20px rgba(0,255,136,0.5);
}

.today-label {
  font-size: 12px;
  color: #8ec6ff;
  margin-top: 5px;
}

/* 热门文章滚动 */
.data-flow-card {
  flex: 0 0 auto;
  max-height: 120px;
}

.hot-articles-scroll {
  overflow: hidden;
  flex: 1;
}

.scroll-content {
  display: flex;
  gap: 30px;
  animation: scroll 20s linear infinite;
}

@keyframes scroll {
  0% { transform: translateX(0); }
  100% { transform: translateX(-50%); }
}

.hot-article-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  background: rgba(255,71,87,0.1);
  border: 1px solid rgba(255,71,87,0.3);
  border-radius: 20px;
  white-space: nowrap;
}

.hot-title {
  font-size: 13px;
  color: #fff;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hot-views {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #ff4757;
}

/* 底部 */
.screen-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.footer-line {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0,212,255,0.5), transparent);
}

.tech-info {
  font-size: 12px;
  color: #5a7a9a;
}
</style>
