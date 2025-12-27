<template>
  <div class="hot-word-cloud">
    <div class="cloud-header">
      <div class="header-left">
        <div class="header-icon">
          <el-icon :size="20"><TrendCharts /></el-icon>
        </div>
        <h3>热门话题</h3>
        <span class="update-time" v-if="lastUpdate">{{ formatUpdateTime }}</span>
      </div>
      <div class="header-actions">
        <!-- 视图切换 -->
        <el-button-group size="small" class="view-toggle">
          <el-button 
            :type="viewMode === 'cloud' ? 'primary' : 'default'"
            @click="viewMode = 'cloud'"
          >
            <el-icon><Grid /></el-icon>
          </el-button>
          <el-button 
            :type="viewMode === 'list' ? 'primary' : 'default'"
            @click="viewMode = 'list'"
          >
            <el-icon><List /></el-icon>
          </el-button>
        </el-button-group>
        <button class="refresh-btn" @click="fetchHotWords" :disabled="loading">
          <el-icon :class="{ 'is-loading': loading }"><Refresh /></el-icon>
        </button>
      </div>
    </div>
    
    <!-- 分析维度选择 -->
    <div class="dimension-tabs">
      <span 
        v-for="dim in dimensions" 
        :key="dim.value"
        class="dimension-tab"
        :class="{ active: activeDimension === dim.value }"
        @click="switchDimension(dim.value)"
      >
        {{ dim.label }}
      </span>
    </div>
    
    <div class="cloud-body" v-loading="loading">
      <!-- 词云展示模式 -->
      <div class="word-cloud" v-if="viewMode === 'cloud' && words.length > 0">
        <TransitionGroup name="word-pop">
          <span
            v-for="(word, index) in words"
            :key="word.word"
            class="word-item"
            :class="[getTrendClass(word), getSizeClass(word), getSentimentClass(word)]"
            :style="getWordStyle(word, index)"
            @click="handleWordClick(word)"
            @mouseenter="showWordDetail(word, $event)"
            @mouseleave="hideWordDetail"
          >
            <span class="word-text">{{ word.word }}</span>
            <span class="trend-icon" v-if="word.trend === 'up'">🔥</span>
            <span class="trend-icon new" v-else-if="word.trend === 'new'">✨</span>
            <span class="sentiment-dot" :class="word.sentiment" v-if="showSentiment"></span>
          </span>
        </TransitionGroup>
      </div>
      
      <!-- 列表展示模式 -->
      <div class="word-list" v-else-if="viewMode === 'list' && words.length > 0">
        <div 
          v-for="(word, index) in words.slice(0, 20)" 
          :key="word.word"
          class="word-list-item"
          @click="handleWordClick(word)"
        >
          <span class="rank" :class="{ top3: index < 3 }">{{ index + 1 }}</span>
          <span class="word-name">{{ word.word }}</span>
          <span class="word-category">{{ word.category || '综合' }}</span>
          <div class="word-bar">
            <div class="bar-fill" :style="{ width: word.weight + '%' }"></div>
          </div>
          <span class="word-weight">{{ word.weight }}</span>
          <span class="trend-badge" :class="word.trend">
            <el-icon v-if="word.trend === 'up'"><Top /></el-icon>
            <el-icon v-else-if="word.trend === 'down'"><Bottom /></el-icon>
            <span v-else>-</span>
          </span>
        </div>
      </div>
      
      <!-- 空状态 -->
      <div class="empty-state" v-else-if="!loading">
        <el-icon :size="48"><TrendCharts /></el-icon>
        <p>暂无热词数据</p>
      </div>
    </div>
    
    <!-- 新兴话题区域 -->
    <div class="emerging-section" v-if="emergingTopics.length > 0">
      <div class="section-title">
        <el-icon><Promotion /></el-icon>
        <span>新兴话题</span>
        <el-tag size="small" type="danger">HOT</el-tag>
      </div>
      <div class="emerging-list">
        <div 
          v-for="topic in emergingTopics.slice(0, 5)" 
          :key="topic.word"
          class="emerging-item"
          @click="handleWordClick(topic)"
        >
          <span class="topic-word">{{ topic.word }}</span>
          <span class="growth-rate">+{{ (topic.growth_rate * 100).toFixed(0) }}%</span>
          <el-progress 
            :percentage="Math.min(topic.confidence * 100, 100)" 
            :show-text="false"
            :stroke-width="4"
            color="#22c55e"
          />
        </div>
      </div>
    </div>
    
    <!-- 底部趋势词 -->
    <div class="trending-section" v-if="trendingWords.length > 0">
      <div class="trending-title">
        <el-icon><Top /></el-icon>
        <span>上升最快</span>
      </div>
      <div class="trending-list">
        <span 
          v-for="word in trendingWords.slice(0, 5)" 
          :key="word.word"
          class="trending-item"
          @click="handleWordClick(word)"
        >
          {{ word.word }}
          <el-icon class="up-icon"><Top /></el-icon>
        </span>
      </div>
    </div>
    
    <!-- 情感分布统计 -->
    <div class="sentiment-stats" v-if="sentimentData.distribution">
      <div class="stats-title">情感分布</div>
      <div class="stats-bars">
        <div class="stat-bar positive">
          <span class="label">积极</span>
          <div class="bar">
            <div class="fill" :style="{ width: sentimentData.distribution.positive_ratio + '%' }"></div>
          </div>
          <span class="value">{{ sentimentData.distribution.positive_ratio }}%</span>
        </div>
        <div class="stat-bar neutral">
          <span class="label">中性</span>
          <div class="bar">
            <div class="fill" :style="{ width: sentimentData.distribution.neutral_ratio + '%' }"></div>
          </div>
          <span class="value">{{ sentimentData.distribution.neutral_ratio }}%</span>
        </div>
        <div class="stat-bar negative">
          <span class="label">消极</span>
          <div class="bar">
            <div class="fill" :style="{ width: sentimentData.distribution.negative_ratio + '%' }"></div>
          </div>
          <span class="value">{{ sentimentData.distribution.negative_ratio }}%</span>
        </div>
      </div>
    </div>
    
    <!-- 词汇详情悬浮框 -->
    <Teleport to="body">
      <div 
        class="word-detail-popup" 
        v-if="hoveredWord"
        :style="popupStyle"
      >
        <div class="popup-header">
          <span class="popup-word">{{ hoveredWord.word }}</span>
          <span class="popup-trend" :class="hoveredWord.trend">
            {{ getTrendLabel(hoveredWord.trend) }}
          </span>
        </div>
        <div class="popup-stats">
          <div class="stat-item">
            <span class="stat-label">热度权重</span>
            <span class="stat-value">{{ hoveredWord.weight }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">出现次数</span>
            <span class="stat-value">{{ hoveredWord.count || '-' }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">分类</span>
            <span class="stat-value">{{ hoveredWord.category || '综合' }}</span>
          </div>
          <div class="stat-item" v-if="hoveredWord.tfidf_score">
            <span class="stat-label">TF-IDF</span>
            <span class="stat-value">{{ hoveredWord.tfidf_score.toFixed(3) }}</span>
          </div>
        </div>
        <div class="popup-tip">点击查看相关内容</div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { TrendCharts, Refresh, Top, Bottom, Grid, List, Promotion } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()
const words = ref([])
const trendingWords = ref([])
const emergingTopics = ref([])
const sentimentData = ref({})
const loading = ref(false)
const lastUpdate = ref(null)
const viewMode = ref('cloud')
const activeDimension = ref('all')
const showSentiment = ref(false)
const hoveredWord = ref(null)
const popupStyle = reactive({ top: '0px', left: '0px' })

// 分析维度
const dimensions = [
  { label: '全部', value: 'all' },
  { label: '今日', value: 'today' },
  { label: '本周', value: 'week' },
  { label: '本月', value: 'month' }
]

// 颜色方案
const colorSchemes = [
  { bg: 'rgba(99, 102, 241, 0.15)', color: '#6366f1', glow: 'rgba(99, 102, 241, 0.4)' },
  { bg: 'rgba(236, 72, 153, 0.15)', color: '#ec4899', glow: 'rgba(236, 72, 153, 0.4)' },
  { bg: 'rgba(34, 197, 94, 0.15)', color: '#22c55e', glow: 'rgba(34, 197, 94, 0.4)' },
  { bg: 'rgba(249, 115, 22, 0.15)', color: '#f97316', glow: 'rgba(249, 115, 22, 0.4)' },
  { bg: 'rgba(14, 165, 233, 0.15)', color: '#0ea5e9', glow: 'rgba(14, 165, 233, 0.4)' },
  { bg: 'rgba(168, 85, 247, 0.15)', color: '#a855f7', glow: 'rgba(168, 85, 247, 0.4)' },
  { bg: 'rgba(20, 184, 166, 0.15)', color: '#14b8a6', glow: 'rgba(20, 184, 166, 0.4)' },
  { bg: 'rgba(245, 158, 11, 0.15)', color: '#f59e0b', glow: 'rgba(245, 158, 11, 0.4)' },
]

const formatUpdateTime = computed(() => {
  if (!lastUpdate.value) return ''
  const now = new Date()
  const diff = Math.floor((now - lastUpdate.value) / 1000 / 60)
  if (diff < 1) return '刚刚更新'
  if (diff < 60) return `${diff}分钟前`
  return `${Math.floor(diff / 60)}小时前`
})

const getTrendClass = (word) => {
  if (word.trend === 'up') return 'trend-up'
  if (word.trend === 'new') return 'trend-new'
  if (word.trend === 'down') return 'trend-down'
  return ''
}

const getSizeClass = (word) => {
  if (word.weight >= 80) return 'size-xl'
  if (word.weight >= 60) return 'size-lg'
  if (word.weight >= 40) return 'size-md'
  return 'size-sm'
}

const getSentimentClass = (word) => {
  if (!showSentiment.value) return ''
  return `sentiment-${word.sentiment || 'neutral'}`
}

const getWordStyle = (word, index) => {
  const colorIndex = index % colorSchemes.length
  const scheme = colorSchemes[colorIndex]
  
  return {
    '--word-bg': scheme.bg,
    '--word-color': scheme.color,
    '--word-glow': scheme.glow,
    animationDelay: `${index * 0.05}s`
  }
}

const getTrendLabel = (trend) => {
  const labels = {
    'up': '↑ 上升',
    'down': '↓ 下降',
    'stable': '→ 稳定',
    'new': '✨ 新词'
  }
  return labels[trend] || '→ 稳定'
}

const handleWordClick = (word) => {
  router.push({
    path: '/search',
    query: { keyword: word.word }
  })
}

const showWordDetail = (word, event) => {
  hoveredWord.value = word
  const rect = event.target.getBoundingClientRect()
  popupStyle.top = `${rect.bottom + 10}px`
  popupStyle.left = `${rect.left}px`
}

const hideWordDetail = () => {
  hoveredWord.value = null
}

const switchDimension = async (dimension) => {
  activeDimension.value = dimension
  await fetchHotWords()
}

const fetchHotWords = async () => {
  loading.value = true
  try {
    // 根据维度获取热词
    let days = 7
    if (activeDimension.value === 'today') days = 1
    else if (activeDimension.value === 'week') days = 7
    else if (activeDimension.value === 'month') days = 30
    
    // 尝试从算法服务获取
    const res = await request({
      url: '/analysis/hotwords',
      method: 'get',
      params: { top_n: 30, days },
      timeout: 5000
    })
    
    if (res && res.length > 0) {
      words.value = res
      trendingWords.value = res.filter(w => w.trend === 'up')
    } else {
      // 降级：使用标签数据
      await fetchFallbackTags()
    }
    
    // 获取新兴话题
    await fetchEmergingTopics()
    
    // 获取情感分布
    await fetchSentimentData()
    
    lastUpdate.value = new Date()
  } catch (error) {
    console.log('热词服务不可用，使用标签数据')
    await fetchFallbackTags()
  } finally {
    loading.value = false
  }
}

const fetchEmergingTopics = async () => {
  try {
    const res = await request({
      url: '/analysis/hotwords/emerging',
      method: 'get',
      params: { window_days: 3, threshold: 1.5 },
      timeout: 5000
    })
    
    if (res && res.length > 0) {
      emergingTopics.value = res
    }
  } catch (error) {
    console.log('新兴话题服务不可用')
    emergingTopics.value = []
  }
}

const fetchSentimentData = async () => {
  try {
    const res = await request({
      url: '/analysis/hotwords/sentiment',
      method: 'get',
      params: { top_n: 50 },
      timeout: 5000
    })
    
    if (res && res.distribution) {
      sentimentData.value = res
      showSentiment.value = true
    }
  } catch (error) {
    console.log('情感分析服务不可用')
    sentimentData.value = {}
    showSentiment.value = false
  }
}

const fetchFallbackTags = async () => {
  try {
    const res = await request({
      url: '/tag/hot',
      method: 'get',
      params: { limit: 30 }
    })
    
    if (res && res.length > 0) {
      words.value = res.map((tag, index) => ({
        word: tag.name,
        weight: Math.max(20, 100 - index * 3),
        count: tag.useCount || 0,
        trend: index < 5 ? 'up' : 'stable',
        category: '标签'
      }))
      trendingWords.value = words.value.slice(0, 5)
    }
    lastUpdate.value = new Date()
  } catch (error) {
    console.error('获取标签失败:', error)
  }
}

onMounted(() => {
  fetchHotWords()
})
</script>

<style scoped>
.hot-word-cloud {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  position: relative;
}

/* 背景装饰 */
.hot-word-cloud::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.1) 0%, transparent 70%);
  pointer-events: none;
}

.cloud-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  position: relative;
  z-index: 1;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}

.cloud-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
}

.update-time {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 10px;
}

.refresh-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.refresh-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.refresh-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.refresh-btn .is-loading {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.cloud-body {
  min-height: 180px;
  position: relative;
  z-index: 1;
}

.word-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  align-items: center;
}

.word-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 20px;
  background: var(--word-bg);
  color: var(--word-color);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  animation: wordPop 0.5s ease-out both;
  border: 1px solid transparent;
}

@keyframes wordPop {
  0% {
    opacity: 0;
    transform: scale(0.5) translateY(20px);
  }
  60% {
    transform: scale(1.1) translateY(-5px);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.word-item:hover {
  transform: translateY(-4px) scale(1.08);
  box-shadow: 0 8px 24px var(--word-glow);
  border-color: var(--word-color);
}

.word-text {
  font-weight: 500;
}

.trend-icon {
  font-size: 12px;
}

/* 尺寸类 */
.size-xl {
  font-size: 18px;
  padding: 10px 20px;
  font-weight: 700;
}

.size-lg {
  font-size: 15px;
  padding: 8px 16px;
  font-weight: 600;
}

.size-md {
  font-size: 13px;
  padding: 6px 14px;
}

.size-sm {
  font-size: 12px;
  padding: 5px 12px;
}

/* 趋势类 */
.trend-up {
  animation: wordPop 0.5s ease-out both, pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 var(--word-glow); }
  50% { box-shadow: 0 0 20px 4px var(--word-glow); }
}

.trend-new {
  border: 1px dashed var(--word-color);
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 150px;
  color: rgba(255, 255, 255, 0.4);
}

.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}

/* 趋势区域 */
.trending-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  position: relative;
  z-index: 1;
}

.trending-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 10px;
}

.trending-title .el-icon {
  color: #22c55e;
}

.trending-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.trending-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 12px;
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.trending-item:hover {
  background: rgba(34, 197, 94, 0.25);
  transform: translateY(-2px);
}

.up-icon {
  font-size: 10px;
  animation: bounce 1s ease infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}

/* 过渡动画 */
.word-pop-enter-active {
  transition: all 0.4s ease-out;
}

.word-pop-leave-active {
  transition: all 0.3s ease-in;
}

.word-pop-enter-from {
  opacity: 0;
  transform: scale(0.5);
}

.word-pop-leave-to {
  opacity: 0;
  transform: scale(0.8);
}

/* 亮色模式适配 */
:global(.light) .hot-word-cloud,
:global([data-theme="light"]) .hot-word-cloud {
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

:global(.light) .cloud-header h3,
:global([data-theme="light"]) .cloud-header h3 {
  color: #1e293b;
}

:global(.light) .update-time,
:global([data-theme="light"]) .update-time {
  color: #64748b;
  background: rgba(0, 0, 0, 0.05);
}

:global(.light) .refresh-btn,
:global([data-theme="light"]) .refresh-btn {
  background: rgba(0, 0, 0, 0.05);
  color: #64748b;
}

:global(.light) .trending-title,
:global([data-theme="light"]) .trending-title {
  color: #64748b;
}

:global(.light) .trending-section,
:global([data-theme="light"]) .trending-section {
  border-top-color: rgba(0, 0, 0, 0.1);
}

:global(.light) .empty-state,
:global([data-theme="light"]) .empty-state {
  color: #94a3b8;
}

/* 新增样式 */

/* 头部操作区 */
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.view-toggle {
  opacity: 0.8;
}

.view-toggle .el-button {
  padding: 6px 10px;
}

/* 维度选择标签 */
.dimension-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.dimension-tab {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: all 0.3s;
  background: rgba(255, 255, 255, 0.05);
}

.dimension-tab:hover {
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.1);
}

.dimension-tab.active {
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
}

/* 列表视图 */
.word-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.word-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.05);
  cursor: pointer;
  transition: all 0.3s;
}

.word-list-item:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateX(4px);
}

.word-list-item .rank {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.7);
}

.word-list-item .rank.top3 {
  background: linear-gradient(135deg, #f59e0b, #ef4444);
  color: #fff;
}

.word-list-item .word-name {
  flex: 1;
  font-weight: 500;
  color: #fff;
}

.word-list-item .word-category {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
}

.word-list-item .word-bar {
  width: 80px;
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
  overflow: hidden;
}

.word-list-item .bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
  border-radius: 3px;
  transition: width 0.5s ease;
}

.word-list-item .word-weight {
  width: 30px;
  text-align: right;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.word-list-item .trend-badge {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.trend-badge.up {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.trend-badge.down {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.trend-badge.stable {
  background: rgba(107, 114, 128, 0.2);
  color: #6b7280;
}

/* 新兴话题区域 */
.emerging-section {
  margin-top: 16px;
  padding: 12px;
  background: rgba(34, 197, 94, 0.1);
  border-radius: 12px;
  border: 1px solid rgba(34, 197, 94, 0.2);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #22c55e;
  margin-bottom: 12px;
}

.emerging-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.emerging-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.emerging-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.emerging-item .topic-word {
  flex: 1;
  font-size: 13px;
  color: #fff;
}

.emerging-item .growth-rate {
  font-size: 12px;
  font-weight: 600;
  color: #22c55e;
}

.emerging-item .el-progress {
  width: 60px;
}

/* 情感分布统计 */
.sentiment-stats {
  margin-top: 16px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
}

.stats-title {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 10px;
}

.stats-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-bar .label {
  width: 32px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
}

.stat-bar .bar {
  flex: 1;
  height: 8px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  overflow: hidden;
}

.stat-bar .fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.stat-bar.positive .fill {
  background: linear-gradient(90deg, #22c55e, #4ade80);
}

.stat-bar.neutral .fill {
  background: linear-gradient(90deg, #6b7280, #9ca3af);
}

.stat-bar.negative .fill {
  background: linear-gradient(90deg, #ef4444, #f87171);
}

.stat-bar .value {
  width: 36px;
  text-align: right;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
}

/* 情感标记点 */
.sentiment-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-left: 4px;
}

.sentiment-dot.positive {
  background: #22c55e;
}

.sentiment-dot.neutral {
  background: #6b7280;
}

.sentiment-dot.negative {
  background: #ef4444;
}

/* 词汇详情悬浮框 */
.word-detail-popup {
  position: fixed;
  z-index: 9999;
  min-width: 200px;
  padding: 12px;
  background: rgba(26, 26, 46, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(10px);
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.popup-word {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}

.popup-trend {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 8px;
}

.popup-trend.up {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.popup-trend.down {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.popup-trend.stable {
  background: rgba(107, 114, 128, 0.2);
  color: #9ca3af;
}

.popup-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-label {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.5);
}

.stat-value {
  font-size: 13px;
  font-weight: 500;
  color: #fff;
}

.popup-tip {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 10px;
  color: rgba(255, 255, 255, 0.4);
  text-align: center;
}

/* 下降趋势样式 */
.trend-down {
  opacity: 0.7;
}

/* 亮色模式适配 - 新增样式 */
:global(.light) .dimension-tabs,
:global([data-theme="light"]) .dimension-tabs {
  border-bottom-color: rgba(0, 0, 0, 0.1);
}

:global(.light) .dimension-tab,
:global([data-theme="light"]) .dimension-tab {
  color: #64748b;
  background: rgba(0, 0, 0, 0.05);
}

:global(.light) .dimension-tab:hover,
:global([data-theme="light"]) .dimension-tab:hover {
  color: #1e293b;
  background: rgba(0, 0, 0, 0.1);
}

:global(.light) .word-list-item,
:global([data-theme="light"]) .word-list-item {
  background: rgba(0, 0, 0, 0.03);
}

:global(.light) .word-list-item:hover,
:global([data-theme="light"]) .word-list-item:hover {
  background: rgba(0, 0, 0, 0.06);
}

:global(.light) .word-list-item .word-name,
:global([data-theme="light"]) .word-list-item .word-name {
  color: #1e293b;
}

:global(.light) .emerging-section,
:global([data-theme="light"]) .emerging-section {
  background: rgba(34, 197, 94, 0.05);
}

:global(.light) .emerging-item .topic-word,
:global([data-theme="light"]) .emerging-item .topic-word {
  color: #1e293b;
}

:global(.light) .sentiment-stats,
:global([data-theme="light"]) .sentiment-stats {
  background: rgba(0, 0, 0, 0.03);
}

:global(.light) .word-detail-popup,
:global([data-theme="light"]) .word-detail-popup {
  background: rgba(255, 255, 255, 0.95);
  border-color: rgba(0, 0, 0, 0.1);
}

:global(.light) .popup-word,
:global([data-theme="light"]) .popup-word {
  color: #1e293b;
}

:global(.light) .stat-value,
:global([data-theme="light"]) .stat-value {
  color: #1e293b;
}
</style>
