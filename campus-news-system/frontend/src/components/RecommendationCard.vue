<template>
  <div class="recommendation-section glass-card" :class="{ 'is-refreshing': isRefreshing }">
    <!-- 头部 -->
    <div class="section-header">
      <div class="title-wrapper">
        <div class="title-icon" :class="{ 'icon-spin': isRefreshing }">
          <el-icon :size="18"><MagicStick /></el-icon>
        </div>
        <h3 class="section-title gradient-text">{{ title }}</h3>
      </div>
      <el-button 
        v-if="showRefresh" 
        class="refresh-btn"
        :class="{ 'is-spinning': isRefreshing }"
        :disabled="isRefreshing"
        @click="refreshWithAnimation"
      >
        <el-icon class="refresh-icon"><Refresh /></el-icon>
        <span>{{ isRefreshing ? '刷新中' : '换一批' }}</span>
      </el-button>
    </div>
    
    <!-- 加载状态 -->
    <div v-if="loading && !isRefreshing" class="loading-container">
      <div v-for="i in 3" :key="i" class="skeleton-item">
        <el-skeleton animated>
          <template #template>
            <div class="skeleton-content">
              <el-skeleton-item variant="image" class="skeleton-cover" />
              <div class="skeleton-text">
                <el-skeleton-item variant="h3" style="width: 80%" />
                <el-skeleton-item variant="text" style="width: 60%" />
              </div>
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>
    
    <!-- 空状态 -->
    <div v-else-if="articles.length === 0 && !isRefreshing" class="empty-container">
      <div class="empty-icon">
        <el-icon :size="48"><Document /></el-icon>
      </div>
      <p class="empty-text">暂无推荐内容</p>
      <el-button type="primary" size="small" @click="refreshWithAnimation">刷新试试</el-button>
    </div>
    
    <!-- 推荐列表 -->
    <transition-group 
      v-else
      name="list" 
      tag="div" 
      class="recommendation-list"
      :class="{ 'fade-out': isRefreshing }"
    >
      <div 
        v-for="(article, index) in articles" 
        :key="article.id"
        class="recommendation-item"
        :style="{ animationDelay: `${index * 0.08}s` }"
        @click="goToArticle(article.id)"
      >
        <div class="item-rank" :class="getRankClass(index)">
          {{ index + 1 }}
        </div>
        <div class="item-cover" v-if="article.coverImage">
          <img :src="article.coverImage" :alt="article.title" loading="lazy" />
          <div class="cover-overlay"></div>
        </div>
        <div class="item-content">
          <h4 class="item-title">{{ article.title }}</h4>
          <p class="item-summary" v-if="article.summary">
            {{ article.summary.substring(0, 50) }}{{ article.summary.length > 50 ? '...' : '' }}
          </p>
          <div class="item-meta">
            <span class="meta-item">
              <el-icon><View /></el-icon>
              {{ formatNumber(article.viewCount || 0) }}
            </span>
            <span class="meta-item">
              <el-icon><Star /></el-icon>
              {{ formatNumber(article.likeCount || 0) }}
            </span>
            <span class="meta-item" v-if="article.author">
              <el-icon><User /></el-icon>
              {{ article.author.realName || article.author.username }}
            </span>
          </div>
        </div>
        <div class="item-arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>
    </transition-group>
    
    <!-- 查看更多 - 点击换一批新的推荐 -->
    <div v-if="articles.length >= 5 && showMore" class="view-more">
      <el-button text class="more-btn" @click="refreshWithAnimation">
        换一批推荐
        <el-icon class="refresh-icon"><Refresh /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { MagicStick, Refresh, View, Star, User, ArrowRight, Document } from '@element-plus/icons-vue'
import { 
  getPersonalizedRecommendations, 
  getSimilarArticles, 
  getHotRecommendations 
} from '@/api/recommendation'

const props = defineProps({
  type: {
    type: String,
    default: 'personalized',
    validator: (val) => ['personalized', 'similar', 'hot'].includes(val)
  },
  articleId: {
    type: Number,
    default: null
  },
  count: {
    type: Number,
    default: 6
  },
  title: {
    type: String,
    default: '智能推荐'
  },
  showRefresh: {
    type: Boolean,
    default: true
  },
  showMore: {
    type: Boolean,
    default: false
  },
  excludeIds: {
    type: Array,
    default: () => []
  }
})

const router = useRouter()
const loading = ref(false)
const articles = ref([])
const isRefreshing = ref(false)

const fetchRecommendations = async () => {
  loading.value = true
  try {
    let res
    switch (props.type) {
      case 'similar':
        if (props.articleId) {
          res = await getSimilarArticles(props.articleId, props.count)
        }
        break
      case 'hot':
        res = await getHotRecommendations(props.count)
        break
      case 'personalized':
      default:
        res = await getPersonalizedRecommendations(props.count, props.excludeIds)
        break
    }
    
    // request.js已经返回res.data，所以res直接就是文章数组
    let data = []
    if (res && Array.isArray(res)) {
      data = res
    } else if (res?.data) {
      // 兼容其他格式
      data = res.data
    }
    // 限制最多显示5条
    articles.value = data.slice(0, 5)
  } catch (error) {
    console.error('获取推荐失败:', error)
  } finally {
    loading.value = false
  }
}

const refresh = () => {
  fetchRecommendations()
}

// 带动画的刷新
const refreshWithAnimation = async () => {
  if (isRefreshing.value) return
  
  isRefreshing.value = true
  
  // 等待淡出动画
  await new Promise(resolve => setTimeout(resolve, 300))
  
  // 清空列表
  articles.value = []
  
  // 获取新数据
  await fetchRecommendations()
  
  // 等待淡入动画完成
  await new Promise(resolve => setTimeout(resolve, 100))
  
  isRefreshing.value = false
}

const goToArticle = (id) => {
  router.push(`/article/${id}`)
}

const getRankClass = (index) => {
  if (index === 0) return 'rank-1'
  if (index === 1) return 'rank-2'
  if (index === 2) return 'rank-3'
  return ''
}

const formatNumber = (num) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num
}

watch(() => props.articleId, (newVal) => {
  if (props.type === 'similar' && newVal) {
    fetchRecommendations()
  }
})

onMounted(() => {
  fetchRecommendations()
})

defineExpose({ refresh })
</script>

<style scoped>
.recommendation-section {
  background: rgba(255, 255, 255, 0.45);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border-radius: 16px;
  padding: 20px;
  border: 2px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06),
              0 0 0 1px rgba(255, 255, 255, 0.4) inset;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.recommendation-section:hover {
  background: rgba(255, 255, 255, 0.55);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1),
              0 0 0 1px rgba(255, 255, 255, 0.6) inset;
}

/* 头部样式 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.title-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.refresh-btn {
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 8px;
  padding: 8px 16px;
  color: #667eea;
  font-weight: 500;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.refresh-btn:hover {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-color: transparent;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

/* 加载骨架屏 */
.loading-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-item {
  padding: 12px;
  background: rgba(255, 255, 255, 0.4);
  border-radius: 12px;
}

.skeleton-content {
  display: flex;
  gap: 12px;
}

.skeleton-cover {
  width: 80px;
  height: 60px;
  border-radius: 8px;
}

.skeleton-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 空状态 */
.empty-container {
  padding: 40px 20px;
  text-align: center;
}

.empty-icon {
  color: #c0c4cc;
  margin-bottom: 16px;
}

.empty-text {
  color: #909399;
  font-size: 14px;
  margin: 0 0 16px 0;
}

/* 推荐列表 */
.recommendation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recommendation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
}

.recommendation-item:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: translateX(6px);
  border-color: rgba(102, 126, 234, 0.3);
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.15);
}

.recommendation-item:hover .item-title {
  color: #667eea;
}

.recommendation-item:hover .item-arrow {
  opacity: 1;
  transform: translateX(0);
}

/* 排名标识 */
.item-rank {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e4e7ed;
  color: #606266;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.item-rank.rank-1 {
  background: linear-gradient(135deg, #ffd700, #ff8c00);
  color: white;
  box-shadow: 0 2px 8px rgba(255, 215, 0, 0.4);
}

.item-rank.rank-2 {
  background: linear-gradient(135deg, #c0c0c0, #a9a9a9);
  color: white;
}

.item-rank.rank-3 {
  background: linear-gradient(135deg, #cd7f32, #b8860b);
  color: white;
}

/* 封面图 */
.item-cover {
  flex-shrink: 0;
  width: 80px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

.item-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.recommendation-item:hover .item-cover img {
  transform: scale(1.1);
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.recommendation-item:hover .cover-overlay {
  opacity: 1;
}

/* 内容区域 */
.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 6px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.3s ease;
}

.item-summary {
  font-size: 12px;
  color: #909399;
  margin: 0 0 8px 0;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.meta-item .el-icon {
  font-size: 13px;
}

/* 箭头 */
.item-arrow {
  flex-shrink: 0;
  color: #667eea;
  opacity: 0;
  transform: translateX(-10px);
  transition: all 0.3s ease;
}

/* 查看更多 */
.view-more {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  text-align: center;
}

.more-btn {
  color: #667eea;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: all 0.3s ease;
}

.more-btn:hover {
  color: #764ba2;
  transform: translateX(4px);
}

/* 刷新动画 */
.is-refreshing .recommendation-list {
  pointer-events: none;
}

/* 图标旋转动画 */
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.icon-spin {
  animation: spin 1s linear infinite;
}

.refresh-icon {
  transition: transform 0.3s ease;
}

.is-spinning .refresh-icon {
  animation: spin 1s linear infinite;
}

/* 列表淡出效果 */
.fade-out {
  opacity: 0.3;
  transform: scale(0.98);
  transition: all 0.3s ease;
}

/* 列表项过渡动画 */
.list-enter-active {
  animation: slideIn 0.4s ease-out forwards;
}

.list-leave-active {
  animation: slideOut 0.3s ease-in forwards;
}

.list-move {
  transition: transform 0.4s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes slideOut {
  from {
    opacity: 1;
    transform: translateX(0);
  }
  to {
    opacity: 0;
    transform: translateX(20px);
  }
}

/* 推荐项入场动画 */
.recommendation-item {
  animation: fadeInUp 0.4s ease-out backwards;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(15px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .recommendation-section {
    padding: 16px;
  }
  
  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .item-cover {
    width: 60px;
    height: 45px;
  }
  
  .item-rank {
    width: 24px;
    height: 24px;
    font-size: 12px;
  }
}
</style>
