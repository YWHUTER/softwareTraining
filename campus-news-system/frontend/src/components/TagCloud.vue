<template>
  <div class="tag-cloud-container">
    <div class="tag-cloud-header">
      <h3 class="tag-cloud-title">
        <el-icon><PriceTag /></el-icon>
        热门标签
      </h3>
    </div>
    
    <div class="tag-cloud" v-loading="loading">
      <TransitionGroup name="tag-fade">
        <span
          v-for="tag in tags"
          :key="tag.id"
          class="tag-item"
          :class="getTagClass(tag)"
          :style="getTagStyle(tag)"
          @click="handleTagClick(tag)"
        >
          {{ tag.name }}
          <span class="tag-count">{{ tag.useCount }}</span>
        </span>
      </TransitionGroup>
      
      <el-empty v-if="!loading && tags.length === 0" description="暂无标签" :image-size="60" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { PriceTag } from '@element-plus/icons-vue'
import { getHotTags } from '@/api/tag'

const router = useRouter()
const tags = ref([])
const loading = ref(false)

// 计算最大和最小使用次数
const maxCount = computed(() => {
  if (tags.value.length === 0) return 1
  return Math.max(...tags.value.map(t => t.useCount || 0))
})

const minCount = computed(() => {
  if (tags.value.length === 0) return 0
  return Math.min(...tags.value.map(t => t.useCount || 0))
})

// 预定义颜色
const colors = [
  { bg: 'rgba(64, 158, 255, 0.1)', color: '#409eff', border: 'rgba(64, 158, 255, 0.3)' },
  { bg: 'rgba(103, 194, 58, 0.1)', color: '#67c23a', border: 'rgba(103, 194, 58, 0.3)' },
  { bg: 'rgba(230, 162, 60, 0.1)', color: '#e6a23c', border: 'rgba(230, 162, 60, 0.3)' },
  { bg: 'rgba(245, 108, 108, 0.1)', color: '#f56c6c', border: 'rgba(245, 108, 108, 0.3)' },
  { bg: 'rgba(144, 147, 153, 0.1)', color: '#909399', border: 'rgba(144, 147, 153, 0.3)' },
  { bg: 'rgba(102, 126, 234, 0.1)', color: '#667eea', border: 'rgba(102, 126, 234, 0.3)' },
  { bg: 'rgba(118, 75, 162, 0.1)', color: '#764ba2', border: 'rgba(118, 75, 162, 0.3)' },
  { bg: 'rgba(16, 185, 129, 0.1)', color: '#10b981', border: 'rgba(16, 185, 129, 0.3)' },
]

// 获取标签样式类
const getTagClass = (tag) => {
  const ratio = (tag.useCount - minCount.value) / (maxCount.value - minCount.value || 1)
  if (ratio > 0.7) return 'tag-hot'
  if (ratio > 0.4) return 'tag-warm'
  return 'tag-normal'
}

// 获取标签样式
const getTagStyle = (tag) => {
  const colorIndex = tag.id % colors.length
  const colorSet = colors[colorIndex]
  
  // 根据使用次数计算字体大小
  const ratio = (tag.useCount - minCount.value) / (maxCount.value - minCount.value || 1)
  const fontSize = 12 + ratio * 6 // 12px - 18px
  
  return {
    backgroundColor: colorSet.bg,
    color: colorSet.color,
    borderColor: colorSet.border,
    fontSize: `${fontSize}px`,
    animationDelay: `${(tag.id % 10) * 0.05}s`
  }
}

// 点击标签
const handleTagClick = (tag) => {
  router.push({
    path: '/search',
    query: { keyword: tag.name }
  })
}

// 获取热门标签
const fetchTags = async () => {
  loading.value = true
  try {
    const data = await getHotTags(30)
    tags.value = data || []
  } catch (error) {
    console.error('获取标签失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchTags()
})
</script>

<style scoped>
.tag-cloud-container {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.tag-cloud-header {
  margin-bottom: 16px;
}

.tag-cloud-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.tag-cloud-title .el-icon {
  color: #409eff;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  min-height: 100px;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 20px;
  border: 1px solid;
  cursor: pointer;
  transition: all 0.3s ease;
  animation: tagFloat 0.5s ease-out both;
}

@keyframes tagFloat {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.9);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.tag-item:hover {
  transform: translateY(-3px) scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.tag-count {
  font-size: 11px;
  opacity: 0.7;
  background: rgba(0, 0, 0, 0.1);
  padding: 1px 6px;
  border-radius: 10px;
}

/* 热门标签 */
.tag-hot {
  font-weight: 600;
}

.tag-hot::before {
  content: '🔥';
  font-size: 10px;
  margin-right: 2px;
}

/* 过渡动画 */
.tag-fade-enter-active,
.tag-fade-leave-active {
  transition: all 0.3s ease;
}

.tag-fade-enter-from,
.tag-fade-leave-to {
  opacity: 0;
  transform: scale(0.8);
}

/* 悬浮动画 */
.tag-item:nth-child(odd):hover {
  animation: wobble 0.5s ease;
}

@keyframes wobble {
  0%, 100% { transform: translateY(-3px) rotate(0); }
  25% { transform: translateY(-3px) rotate(-2deg); }
  75% { transform: translateY(-3px) rotate(2deg); }
}

/* ========== 暗黑模式适配 ========== */
:global(.dark) .tag-cloud-container,
:global([data-theme="dark"]) .tag-cloud-container {
  background: var(--bg-card);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
}

:global(.dark) .tag-cloud-title,
:global([data-theme="dark"]) .tag-cloud-title {
  color: var(--text-primary);
}

:global(.dark) .tag-cloud-title .el-icon,
:global([data-theme="dark"]) .tag-cloud-title .el-icon {
  color: var(--primary-color);
}

:global(.dark) .tag-item:hover,
:global([data-theme="dark"]) .tag-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

:global(.dark) .tag-count,
:global([data-theme="dark"]) .tag-count {
  background: rgba(255, 255, 255, 0.1);
}
</style>
