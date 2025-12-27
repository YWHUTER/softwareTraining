<template>
  <div class="dashboard">
    <!-- 页面标题和刷新按钮 -->
    <div class="dashboard-header">
      <h2>数据概览</h2>
      <div class="header-actions">
        <el-button 
          type="primary" 
          @click="$router.push('/data-screen')"
          class="screen-btn"
        >
          <el-icon><DataAnalysis /></el-icon>
          <span>数据大屏</span>
        </el-button>
        <el-button :icon="Refresh" :loading="loading" @click="refreshAll">刷新数据</el-button>
      </div>
    </div>
    
    <!-- 主要统计卡片 -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card user-card">
          <el-statistic title="用户总数" :value="statistics.userCount">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
            <template #suffix>人</template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card article-card">
          <el-statistic title="文章总数" :value="statistics.articleCount">
            <template #prefix>
              <el-icon><Document /></el-icon>
            </template>
            <template #suffix>篇</template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card comment-card">
          <el-statistic title="评论总数" :value="statistics.commentCount">
            <template #prefix>
              <el-icon><ChatDotRound /></el-icon>
            </template>
            <template #suffix>条</template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card view-card">
          <el-statistic title="总浏览量" :value="statistics.totalViews">
            <template #prefix>
              <el-icon><View /></el-icon>
            </template>
            <template #suffix>次</template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 次要统计 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card-mini clickable" @click="goToArticles">
          <div class="mini-stat">
            <span class="label">待审核文章</span>
            <span class="value pending">{{ statistics.pendingCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card-mini">
          <div class="mini-stat">
            <span class="label">学院数量</span>
            <span class="value">{{ statistics.collegeCount }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 文章分类饼图 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>📊 文章分类分布</span>
          </template>
          <div ref="categoryChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <!-- 近7天发布趋势 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>📈 近7天发布趋势</span>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 热门文章排行 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>🔥 热门文章 TOP5</span>
          </template>
          <div ref="hotChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <!-- 审核状态饼图 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>📋 文章审核状态</span>
          </template>
          <div ref="statusChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 文章表格区域 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>最新文章</span>
          </template>
          <el-table :data="recentArticles" stripe>
            <el-table-column prop="title" label="标题" show-overflow-tooltip />
            <el-table-column prop="author.realName" label="作者" width="100" />
            <el-table-column prop="viewCount" label="浏览" width="80" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>待审核文章</span>
          </template>
          <el-table :data="pendingArticles" stripe>
            <el-table-column prop="title" label="标题" show-overflow-tooltip />
            <el-table-column prop="author.realName" label="作者" width="100" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button text size="small" type="success" @click="handleApprove(row.id, 1)">通过</el-button>
                <el-button text size="small" type="danger" @click="handleApprove(row.id, 2)">拒绝</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, View, DataAnalysis } from '@element-plus/icons-vue'
import { getArticleList, approveArticle } from '@/api/article'
import { getStatistics, getChartData } from '@/api/admin'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const router = useRouter()

const statistics = ref({
  userCount: 0,
  articleCount: 0,
  commentCount: 0,
  collegeCount: 0,
  pendingCount: 0,
  totalViews: 0
})

const recentArticles = ref([])
const pendingArticles = ref([])
const loading = ref(false)

// 跳转到文章管理页面
const goToArticles = () => {
  router.push('/admin/articles')
}

// 图表 DOM 引用
const categoryChartRef = ref(null)
const trendChartRef = ref(null)
const hotChartRef = ref(null)
const statusChartRef = ref(null)

// 图表实例
let categoryChart = null
let trendChart = null
let hotChart = null
let statusChart = null

// 获取统计数据
const fetchStatistics = async () => {
  try {
    const data = await getStatistics()
    statistics.value = {
      userCount: data.userCount || 0,
      articleCount: data.articleCount || 0,
      commentCount: data.commentCount || 0,
      collegeCount: data.collegeCount || 0,
      pendingCount: data.pendingCount || 0,
      totalViews: data.totalViews || 0
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 获取图表数据并渲染
const fetchChartData = async () => {
  try {
    const data = await getChartData()
    await nextTick()
    renderCategoryChart(data.categoryData || [])
    renderTrendChart(data.trendDates || [], data.trendCounts || [])
    renderHotChart(data.hotTitles || [], data.hotViews || [])
    renderStatusChart(data.statusData || [])
  } catch (error) {
    console.error('获取图表数据失败:', error)
  }
}

// 渲染文章分类饼图
const renderCategoryChart = (data) => {
  if (!categoryChartRef.value) return
  
  if (categoryChart) {
    categoryChart.dispose()
  }
  categoryChart = echarts.init(categoryChartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 篇 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '10%',
      top: 'center'
    },
    color: ['#5470c6', '#91cc75', '#fac858'],
    series: [
      {
        name: '文章分类',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 18,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: data
      }
    ]
  }
  
  categoryChart.setOption(option)
}

// 渲染发布趋势折线图
const renderTrendChart = (dates, counts) => {
  if (!trendChartRef.value) return
  
  if (trendChart) {
    trendChart.dispose()
  }
  trendChart = echarts.init(trendChartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>发布: {c} 篇'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLine: { lineStyle: { color: '#ddd' } },
      axisLabel: { color: '#666' }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      axisLabel: { color: '#666' },
      splitLine: { lineStyle: { color: '#eee' } }
    },
    series: [
      {
        name: '发布数量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: {
          color: '#409eff',
          width: 3
        },
        itemStyle: {
          color: '#409eff'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ])
        },
        data: counts
      }
    ]
  }
  
  trendChart.setOption(option)
}

// 渲染热门文章排行柱状图
const renderHotChart = (titles, views) => {
  if (!hotChartRef.value) return
  
  if (hotChart) {
    hotChart.dispose()
  }
  hotChart = echarts.init(hotChartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: '{b}<br/>浏览量: {c}'
    },
    grid: {
      left: '3%',
      right: '8%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      axisLine: { show: false },
      axisLabel: { color: '#666' },
      splitLine: { lineStyle: { color: '#eee' } }
    },
    yAxis: {
      type: 'category',
      data: titles.reverse(),
      axisLine: { lineStyle: { color: '#ddd' } },
      axisLabel: { 
        color: '#666',
        width: 100,
        overflow: 'truncate'
      }
    },
    series: [
      {
        name: '浏览量',
        type: 'bar',
        barWidth: 20,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#f56c6c' },
            { offset: 1, color: '#f78989' }
          ]),
          borderRadius: [0, 10, 10, 0]
        },
        data: views.reverse()
      }
    ]
  }
  
  hotChart.setOption(option)
}

// 渲染审核状态饼图
const renderStatusChart = (data) => {
  if (!statusChartRef.value) return
  
  if (statusChart) {
    statusChart.dispose()
  }
  statusChart = echarts.init(statusChartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 篇 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '10%',
      top: 'center'
    },
    color: ['#67c23a', '#e6a23c', '#f56c6c'],
    series: [
      {
        name: '审核状态',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 18,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: data
      }
    ]
  }
  
  statusChart.setOption(option)
}

const fetchRecentArticles = async () => {
  try {
    const data = await getArticleList({
      current: 1,
      size: 5,
      isApproved: 1,
      sortBy: 'created_at',
      sortOrder: 'desc'
    })
    recentArticles.value = data.records
  } catch (error) {
    console.error(error)
  }
}

const fetchPendingArticles = async () => {
  try {
    const data = await getArticleList({
      current: 1,
      size: 5,
      isApproved: 0
    })
    pendingArticles.value = data.records
  } catch (error) {
    console.error(error)
  }
}

const handleApprove = async (id, isApproved) => {
  try {
    await approveArticle(id, isApproved)
    ElMessage.success(isApproved === 1 ? '审核通过' : '审核拒绝')
    // 刷新所有数据
    refreshAll()
  } catch (error) {
    console.error(error)
  }
}

// 刷新所有数据
const refreshAll = async () => {
  loading.value = true
  await Promise.all([
    fetchStatistics(),
    fetchChartData(),
    fetchRecentArticles(),
    fetchPendingArticles()
  ])
  loading.value = false
}

// 窗口大小改变时重新调整图表
const handleResize = () => {
  categoryChart?.resize()
  trendChart?.resize()
  hotChart?.resize()
  statusChart?.resize()
}

onMounted(() => {
  refreshAll()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  categoryChart?.dispose()
  trendChart?.dispose()
  hotChart?.dispose()
  statusChart?.dispose()
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  animation: slideDown 0.5s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-15px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dashboard-header h2 {
  margin: 0;
  font-size: 22px;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.screen-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

/* 统计卡片样式 */
.el-card {
  background: rgba(255, 255, 255, 0.35) !important;
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06), 
              0 0 0 1px rgba(255, 255, 255, 0.4) inset !important;
}

.dashboard-header h2 {
  margin: 0;
  font-size: 24px;
  color: #fff;
  text-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.stat-card {
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  animation: scaleIn 0.4s ease-out both;
  background: rgba(255, 255, 255, 0.35) !important;
}

.stat-card:nth-child(1) .stat-card { animation-delay: 0.05s; }
.stat-card:nth-child(2) .stat-card { animation-delay: 0.1s; }
.stat-card:nth-child(3) .stat-card { animation-delay: 0.15s; }
.stat-card:nth-child(4) .stat-card { animation-delay: 0.2s; }

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.stat-card:hover {
  transform: translateY(-6px) scale(1.02);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.12), 
              0 0 0 1px rgba(255, 255, 255, 0.6) inset !important;
  background: rgba(255, 255, 255, 0.5) !important;
  border-color: rgba(255, 255, 255, 0.8) !important;
}

.stat-card :deep(.el-statistic__head) {
  font-size: 14px;
  color: #606266;
  font-weight: 600;
}

.stat-card :deep(.el-statistic__content) {
  font-size: 28px;
  font-weight: 600;
}

.stat-card :deep(.el-icon) {
  font-size: 20px;
  margin-right: 4px;
}

/* 不同卡片的颜色 */
.user-card :deep(.el-statistic__content) {
  color: #409eff;
}

.article-card :deep(.el-statistic__content) {
  color: #67c23a;
}

.comment-card :deep(.el-statistic__content) {
  color: #e6a23c;
}

.view-card :deep(.el-statistic__content) {
  color: #f56c6c;
}

/* 小卡片 */
.stat-card-mini {
  border-radius: 8px;
}

.stat-card-mini.clickable {
  cursor: pointer;
  transition: all 0.3s ease;
}

.stat-card-mini.clickable:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.mini-stat {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.mini-stat .label {
  color: #606266;
  font-size: 14px;
}

.mini-stat .value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.mini-stat .value.pending {
  color: #e6a23c;
}

/* 表格卡片 */
.el-card :deep(.el-card__header) {
  padding: 16px 20px;
  font-weight: 600;
  color: #303133;
}

/* 图表卡片 */
.chart-card {
  border-radius: 12px;
}

.chart-container {
  width: 100%;
  height: 280px;
}

/* 表格透明背景 */
:deep(.el-table),
:deep(.el-table__expanded-cell) {
  background-color: transparent !important;
  --el-table-tr-bg-color: transparent !important;
}

:deep(.el-table th.el-table__cell),
:deep(.el-table tr) {
  background-color: transparent !important;
  background: transparent !important;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: rgba(255, 255, 255, 0.25);
}

:deep(.el-table--enable-row-hover .el-table__body tr:hover > td.el-table__cell) {
  background-color: rgba(255, 255, 255, 0.4) !important;
}

/* 表格文字颜色加深 */
:deep(.el-table) {
  color: #1a1a1a !important;
}

:deep(.el-table th.el-table__cell) {
  color: #000 !important;
  font-weight: 700 !important;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.5);
}

:deep(.el-table td.el-table__cell) {
  color: #1a1a1a !important;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.3);
}

:deep(.el-table .el-table__empty-text) {
  color: #333 !important;
}
</style>
