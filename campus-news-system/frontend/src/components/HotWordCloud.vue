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
      <button class="refresh-btn" @click="fetchHotWords" :disabled="loading">
        <el-icon :class="{ 'is-loading': loading }"><Refresh /></el-icon>
      </button>
    </div>
    
    <div class="cloud-body" v-loading="loading">
      <!-- 词云展示 -->
      <div class="word-cloud" v-if="words.length > 0">
        <TransitionGroup name="word-pop">
          <span
            v-for="(word, index) in words"
            :key="word.word"
            class="word-item"
            :class="[getTrendClass(word), getSizeClass(word)]"
            :style="getWordStyle(word, index)"
            @click="handleWordClick(word)"
          >
            <span class="word-text">{{ word.word }}</span>
            <span class="trend-icon" v-if="word.trend === 'up'">🔥</span>
            <span class="trend-icon new" v-else-if="word.trend === 'new'">✨</span>
          </span>
        </TransitionGroup>
      </div>
      
      <!-- 空状态 -->
      <div class="empty-state" v-else-if="!loading">
        <el-icon :size="48"><TrendCharts /></el-icon>
        <p>暂无热词数据</p>
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
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { TrendCharts, Refresh, Top } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()
const words = ref([])
const trendingWords = ref([])
const loading = ref(false)
const lastUpdate = ref(null)

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
  return ''
}

const getSizeClass = (word) => {
  if (word.weight >= 80) return 'size-xl'
  if (word.weight >= 60) return 'size-lg'
  if (word.weight >= 40) return 'size-md'
  return 'size-sm'
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

const handleWordClick = (word) => {
  router.push({
    path: '/search',
    query: { keyword: word.word }
  })
}

const fetchHotWords = async () => {
  loading.value = true
  try {
    // 尝试从算法服务获取
    const res = await request({
      url: '/analysis/hotwords',
      method: 'get',
      params: { top_n: 30 },
      timeout: 5000
    })
    
    if (res && res.length > 0) {
      words.value = res
      trendingWords.value = res.filter(w => w.trend === 'up')
    } else {
      // 降级：使用标签数据
      await fetchFallbackTags()
    }
    lastUpdate.value = new Date()
  } catch (error) {
    console.log('热词服务不可用，使用标签数据')
    await fetchFallbackTags()
  } finally {
    loading.value = false
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
</style>
