<template>
  <div class="board-page">
    <!-- 顶部功能栏 -->
    <div 
      class="board-header-bar"
      v-motion
      :initial="{ opacity: 0, y: -20 }"
      :enter="{ opacity: 1, y: 0, transition: { type: 'spring', stiffness: 250, damping: 25 } }"
    >
      <div class="header-left">
        <div class="board-icon" :class="boardType">
          <el-icon v-if="boardType === 'OFFICIAL'"><Document /></el-icon>
          <el-icon v-else-if="boardType === 'CAMPUS'"><School /></el-icon>
          <el-icon v-else><OfficeBuilding /></el-icon>
        </div>
        <div class="header-info">
          <h2 class="board-title">{{ boardTitle }}</h2>
          <span class="board-desc">共 {{ total }} 篇文章</span>
        </div>
      </div>
      
      <div class="header-right">
        <div class="view-controls">
          <el-tooltip content="网格视图" placement="top">
            <div class="view-btn" :class="{ active: viewMode === 'grid' }" @click="viewMode = 'grid'">
              <el-icon><Grid /></el-icon>
            </div>
          </el-tooltip>
          <el-tooltip content="列表视图" placement="top">
            <div class="view-btn" :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'">
              <el-icon><List /></el-icon>
            </div>
          </el-tooltip>
          <el-tooltip content="大图视图" placement="top">
            <div class="view-btn" :class="{ active: viewMode === 'card' }" @click="viewMode = 'card'">
              <el-icon><Files /></el-icon>
            </div>
          </el-tooltip>
          <el-tooltip content="瀑布流" placement="top">
            <div class="view-btn" :class="{ active: viewMode === 'masonry' }" @click="viewMode = 'masonry'">
              <el-icon><Menu /></el-icon>
            </div>
          </el-tooltip>
        </div>
        <el-divider direction="vertical" class="header-divider" />
        <el-select v-model="sortBy" @change="handleSortChange" class="sort-select" placeholder="排序方式" size="large">
          <template #prefix><el-icon><Sort /></el-icon></template>
          <el-option label="最新发布" value="date_desc" />
          <el-option label="最早发布" value="date_asc" />
          <el-option label="最多浏览" value="views_desc" />
          <el-option label="最少浏览" value="views_asc" />
        </el-select>
      </div>
    </div>
    
    <!-- 内容区域 -->
    <div class="board-content">
      <!-- 加载骨架屏 -->
      <div v-if="loading" class="article-container mode-grid">
        <div v-for="i in 8" :key="i" class="news-card skeleton-card">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="image" style="width: 100%; height: 180px;" />
              <div style="padding: 20px;">
                <el-skeleton-item variant="h3" style="width: 60%; margin-bottom: 16px;" />
                <el-skeleton-item variant="text" style="width: 100%; margin-bottom: 8px;" />
                <el-skeleton-item variant="text" style="width: 80%; margin-bottom: 16px;" />
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <div style="display: flex; gap: 8px; align-items: center;">
                    <el-skeleton-item variant="circle" style="width: 24px; height: 24px;" />
                    <el-skeleton-item variant="text" style="width: 60px;" />
                  </div>
                  <el-skeleton-item variant="text" style="width: 40px;" />
                </div>
              </div>
            </template>
          </el-skeleton>
        </div>
      </div>

      <template v-else>
        <el-empty v-if="articles.length === 0" description="暂无文章" />
        
        <div v-else class="article-container" :class="`mode-${viewMode}`" :key="viewMode">
          <!-- 网格模式下的头条大图 -->
          <div 
            v-if="viewMode === 'grid' && currentPage === 1 && articles.length > 0" 
            class="hero-section"
            v-motion
            :initial="{ opacity: 0, y: 20 }"
            :enter="{ opacity: 1, y: 0, transition: { type: 'spring', stiffness: 200, damping: 20 } }"
          >
            <div 
              class="hero-card" 
              :class="{ 'no-cover': !isValidCover(articles[0]) }"
              @click="goToDetail(articles[0].id)"
            >
              <div class="hero-cover" v-if="isValidCover(articles[0])">
                <el-image 
                  :src="articles[0].coverImage" 
                  fit="cover" 
                  loading="lazy" 
                  class="hero-image"
                  @error="handleImageError(articles[0])"
                >
                  <template #placeholder>
                    <div class="image-placeholder hero-placeholder">
                      <el-icon class="is-loading"><Loading /></el-icon>
                    </div>
                  </template>
                  <template #error>
                    <div class="image-error" v-show="false"></div>
                  </template>
                </el-image>
                <div class="hero-overlay"></div>
                <div class="pinned-badge" v-if="articles[0].isPinned">
                  <el-icon><Top /></el-icon> 置顶推荐
                </div>
              </div>
              <div class="hero-content">
                <div class="hero-meta">
                  <el-tag effect="dark" :type="getBoardTypeTag(articles[0].boardType)" round size="small">
                    {{ boardTitle }}
                  </el-tag>
                  <span class="publish-time">{{ getRelativeTime(articles[0].createdAt) }}</span>
                  <!-- 无封面图时的置顶标记 -->
                  <div class="pinned-mark-inline" v-if="!isValidCover(articles[0]) && articles[0].isPinned">
                    <el-icon><Top /></el-icon> 置顶
                  </div>
                </div>
                <h2 class="hero-title">{{ articles[0].title }}</h2>
                <p class="hero-summary">{{ articles[0].summary || articles[0].content?.replace(/<[^>]+>/g, '').substring(0, 120) + '...' }}</p>
                <div class="hero-footer">
                  <div class="author-info">
                    <el-avatar :size="32" :src="getValidAvatar(articles[0].author?.avatar)" class="author-avatar">
                      {{ articles[0].author?.realName?.[0] }}
                    </el-avatar>
                    <span class="author-name">{{ articles[0].author?.realName }}</span>
                  </div>
                  <div class="hero-stats">
                    <span class="stat-item"><el-icon><View /></el-icon> {{ formatCount(articles[0].viewCount) }}</span>
                    <span class="stat-item"><el-icon><ChatDotRound /></el-icon> {{ articles[0].commentCount }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 文章列表（排除第一篇如果是Grid模式且在第一页） -->
          <div
            v-for="(article, index) in displayArticles"
            :key="article.id"
            class="news-card"
            :class="{ 'is-pinned': article.isPinned }"
            @click="goToDetail(article.id)"
            v-motion
            :initial="{ opacity: 0, y: 50 }"
            :enter="{ opacity: 1, y: 0, transition: { delay: index * 50, type: 'spring', stiffness: 250, damping: 25 } }"
          >
            <!-- 封面图区域 -->
            <div class="card-cover" v-if="isValidCover(article)">
              <el-image 
                :src="article.coverImage" 
                fit="cover" 
                loading="lazy"
                @error="handleImageError(article)"
              >
                <template #placeholder>
                  <div class="image-placeholder">
                    <el-icon class="is-loading"><Loading /></el-icon>
                  </div>
                </template>
                <template #error>
                  <div class="image-error" v-show="false"></div>
                </template>
              </el-image>
              <div class="card-overlay"></div>
              <div class="pinned-badge" v-if="article.isPinned">
                <el-icon><Top /></el-icon> 置顶
              </div>
            </div>
            <!-- 无封面图时的置顶标记 -->
            <div class="pinned-mark" v-else-if="article.isPinned">
              <el-icon><Top /></el-icon> 置顶
            </div>

            <div class="card-body">
              <h3 class="news-title" :title="article.title">{{ article.title }}</h3>
              <p class="news-summary">
                {{ article.summary || article.content?.replace(/<[^>]+>/g, '').substring(0, 80) + '...' }}
              </p>
              
              <div class="card-footer">
                <div class="author-row">
                  <el-avatar :size="24" :src="getValidAvatar(article.author?.avatar)" class="author-avatar">
                    {{ article.author?.realName?.[0] }}
                  </el-avatar>
                  <span class="author-name">{{ article.author?.realName }}</span>
                  <span class="divider">•</span>
                  <span class="publish-time">{{ getRelativeTime(article.createdAt) }}</span>
                </div>
                
                <div class="stats-row">
                  <span class="stat-item">
                    <el-icon><View /></el-icon> {{ formatCount(article.viewCount) }}
                  </span>
                  <span class="stat-item">
                    <el-icon><ChatDotRound /></el-icon> {{ article.commentCount }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 分页 -->
        <div class="pagination-container" v-if="total > 0">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[13, 25, 37]"
            layout="total, prev, pager, next, jumper"
            @current-change="fetchArticles"
            @size-change="fetchArticles"
            background
          />
        </div>
      </template>
    </div>

    <!-- 回到顶部 -->
    <el-backtop :right="40" :bottom="40" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArticleList } from '@/api/article'
import { Document, School, OfficeBuilding, Sort, Picture, Top, View, ChatDotRound, Grid, List, Files, Menu } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const articles = ref([])
const currentPage = ref(1)
const pageSize = ref(13) // Grid layout: 1 hero + 12 cards = 13
const total = ref(0)
const sortBy = ref('date_desc')
const viewMode = ref('grid') // grid, list, card, masonry

const boardType = computed(() => route.params.type)

// 根据板块类型设置默认视图模式
const getDefaultViewMode = (type) => {
  const viewModes = {
    OFFICIAL: 'grid',    // 官方新闻使用网格视图
    CAMPUS: 'list',      // 全校新闻使用列表视图
    COLLEGE: 'masonry'   // 学院新闻使用瀑布流视图
  }
  return viewModes[type] || 'grid'
}

const boardTitle = computed(() => {
  const titles = {
    OFFICIAL: '官方新闻',
    CAMPUS: '全校新闻',
    COLLEGE: '学院新闻'
  }
  return titles[boardType.value] || '文章列表'
})

// 计算显示的普通卡片文章（如果第一篇是Hero，则排除第一篇）
const displayArticles = computed(() => {
  if (viewMode.value === 'grid' && currentPage.value === 1 && articles.value.length > 0) {
    return articles.value.slice(1)
  }
  return articles.value
})

const fetchArticles = async () => {
  loading.value = true
  try {
    const [sortField, sortOrder] = sortBy.value.split('_')
    const data = await getArticleList({
      current: currentPage.value,
      size: pageSize.value,
      boardType: boardType.value,
      isApproved: 1,
      sortBy: sortField === 'views' ? 'views' : 'date',
      sortOrder: sortOrder,
      _t: Date.now() // Add timestamp to prevent caching
    })
    articles.value = (data.records || []).map(item => ({
      ...item,
      imageLoadError: false // 初始化图片加载状态
    }))
    total.value = data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSortChange = () => {
  currentPage.value = 1
  fetchArticles()
}

const goToDetail = (id) => {
  router.push(`/article/${id}`)
}

// 图片处理相关
const isValidCover = (article) => {
  if (!article.coverImage) return false
  if (article.imageLoadError) return false
  if (article.coverImage === 'null' || article.coverImage === 'undefined') return false
  return true
}

const handleImageError = (article) => {
  article.imageLoadError = true
}

const getRelativeTime = (time) => {
  const now = new Date()
  const past = new Date(time)
  const diff = (now - past) / 1000
  
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  if (diff < 604800) return Math.floor(diff / 86400) + '天前'
  return past.toLocaleDateString('zh-CN')
}

const formatCount = (num) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  return num
}

const getValidAvatar = (avatar) => {
  if (!avatar) return undefined
  if (avatar.startsWith('/api/file/')) return avatar
  return undefined
}

const getBoardTypeTag = (type) => {
  const tags = {
    OFFICIAL: 'danger',
    CAMPUS: 'primary',
    COLLEGE: 'success'
  }
  return tags[type] || ''
}

watch(boardType, (newType) => {
  currentPage.value = 1
  viewMode.value = getDefaultViewMode(newType)
  fetchArticles()
})

onMounted(() => {
  viewMode.value = getDefaultViewMode(boardType.value)
  fetchArticles()
})
</script>

<style scoped>
.board-page {
  max-width: 1400px;
  margin: 0 auto;
}

/* Header Bar */
.board-header-bar {
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border-radius: 20px;
  padding: 24px 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.05), 
              0 0 0 1px rgba(255, 255, 255, 0.6) inset;
  border: 1px solid rgba(255, 255, 255, 0.6);
  position: relative;
  z-index: 10;
  transition: all 0.3s ease;
}

.board-header-bar:hover {
  background: rgba(255, 255, 255, 0.55);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.08), 
              0 0 0 1px rgba(255, 255, 255, 0.8) inset;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.board-icon {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: white;
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
  transform: rotate(-5deg);
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.board-header-bar:hover .board-icon {
  transform: rotate(0deg) scale(1.05);
}

.board-icon.OFFICIAL { background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%); }
.board-icon.CAMPUS { background: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%); }
.board-icon.COLLEGE { background: linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%); }

.header-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.board-title {
  margin: 0;
  font-size: 28px;
  font-weight: 800;
  color: #2c3e50;
  letter-spacing: -0.5px;
  background: linear-gradient(90deg, #2c3e50, #4a5568);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.board-desc {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

/* View Controls */
.header-right {
  display: flex;
  align-items: center;
}

.view-controls {
  display: flex;
  background: rgba(235, 240, 245, 0.5);
  padding: 4px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  gap: 4px;
}

.view-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  cursor: pointer;
  color: #606266;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.view-btn:hover {
  background: rgba(255, 255, 255, 0.8);
  color: #409eff;
}

.view-btn.active {
  background: #fff;
  color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.header-divider {
  height: 24px;
  margin: 0 24px;
  border-color: rgba(0, 0, 0, 0.1);
}

.sort-select {
  width: 160px;
}

/* ================= Layout Modes ================= */

.article-container {
  width: 100%;
  padding-bottom: 60px;
}

/* 1. Grid Mode (Default) */
.mode-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  grid-auto-rows: min-content; /* 关键：防止行高被拉伸 */
}

/* Hero Section */
.hero-section {
  grid-column: 1 / -1; /* 跨越所有列 */
  margin-bottom: 12px;
}

.hero-card {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  height: 360px;
}

.hero-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.12);
}

.hero-card.no-cover {
  grid-template-columns: 1fr;
  background: rgba(255, 255, 255, 0.45);
  border-color: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px) saturate(150%);
}

.hero-card.no-cover .hero-content {
  padding: 40px 60px;
  background: transparent;
}

.hero-card.no-cover .hero-content::before {
  display: none;
}

.hero-cover {
  position: relative;
  height: 100%;
  overflow: hidden;
}

.hero-image {
  width: 100%;
  height: 100%;
  transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.hero-card:hover .hero-image {
  transform: scale(1.05);
}

.hero-content {
  padding: 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
}

.hero-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 100%;
  background: linear-gradient(135deg, rgba(255,255,255,0.4) 0%, rgba(255,255,255,0.1) 100%);
  pointer-events: none;
}

.hero-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  position: relative;
}

.hero-title {
  font-size: 32px;
  font-weight: 800;
  color: #1a202c;
  line-height: 1.3;
  margin: 0 0 16px;
  position: relative;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hero-summary {
  font-size: 16px;
  color: #4a5568;
  line-height: 1.6;
  margin: 0 0 24px;
  flex: 1;
  position: relative;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hero-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  padding-top: 20px;
  border-top: 1px solid rgba(0,0,0,0.05);
}

.author-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hero-stats {
  display: flex;
  gap: 16px;
  color: #718096;
}

/* 2. List Mode */
.mode-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mode-list .news-card {
  flex-direction: row;
  height: 180px;
  align-items: stretch;
}

.mode-list .card-cover {
  width: 280px;
  height: 100%;
  flex-shrink: 0;
}

.mode-list .card-body {
  padding: 16px 24px;
}

.mode-list .news-summary {
  -webkit-line-clamp: 3;
  line-clamp: 3;
  margin-bottom: 12px;
}

/* 3. Card Mode (Large) */
.mode-card {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(450px, 1fr));
  gap: 30px;
}

.mode-card .card-cover {
  height: 280px;
}

.mode-card .news-title {
  font-size: 22px;
}

/* 4. Masonry Mode */
.mode-masonry {
  column-count: 4;
  column-gap: 24px;
}

.mode-masonry .news-card {
  break-inside: avoid;
  margin-bottom: 24px;
  display: inline-block;
  width: 100%;
}

/* News Card Base Styles */
.news-card {
  background: rgba(255, 255, 255, 0.45);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.6);
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  position: relative;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  transform-origin: center center;
}

.news-card:hover {
  transform: translateY(-8px) scale(1.01);
  background: rgba(255, 255, 255, 0.65);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.12);
  z-index: 2;
  border-color: rgba(255, 255, 255, 0.9);
}

.news-card:active {
  transform: scale(0.98);
}

/* Cover Image */
.card-cover {
  height: 180px;
  position: relative;
  overflow: hidden;
}

.card-cover .el-image {
  width: 100%;
  height: 100%;
  transition: transform 0.6s ease;
}

.news-card:hover .card-cover .el-image {
  transform: scale(1.08);
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 32px;
}

.pinned-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: linear-gradient(135deg, #ff9800 0%, #ff5722 100%);
  color: white;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
  backdrop-filter: blur(4px);
  box-shadow: 0 4px 12px rgba(255, 87, 34, 0.3);
  z-index: 2;
}

.pinned-mark {
  position: absolute;
  top: 16px;
  right: 16px;
  color: #ff5722;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 87, 34, 0.1);
  padding: 4px 8px;
  border-radius: 6px;
}

.pinned-mark-inline {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #ff5722;
  font-size: 12px;
  font-weight: 600;
  background: rgba(255, 87, 34, 0.1);
  padding: 4px 8px;
  border-radius: 6px;
  margin-left: 8px;
}

/* Card Body */
.card-body {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.news-title {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 700;
  color: #2c3e50;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.3s ease;
}

.news-card:hover .news-title {
  color: #667eea;
}

.news-summary {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 20px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

/* Footer */
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.author-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #909399;
}

.author-avatar {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  font-weight: 600;
  font-size: 10px;
}

.author-name {
  font-weight: 500;
  color: #606266;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.divider {
  color: #e0e0e0;
}

.stats-row {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #999;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* Pagination */
.pagination-container {
  display: flex;
  justify-content: center;
  padding: 24px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
  width: fit-content;
  margin: 0 auto;
}

/* Responsive Design */
@media (max-width: 1024px) {
  .hero-card {
    height: auto;
    grid-template-columns: 1fr;
  }
  
  .hero-cover {
    height: 240px;
  }
  
  .hero-content {
    padding: 24px;
  }
}

@media (max-width: 768px) {
  .board-header-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
    padding: 20px;
  }
  
  .header-right {
    width: 100%;
    justify-content: space-between;
  }
  
  .sort-select {
    width: 140px;
  }
  
  .header-divider {
    margin: 0 10px;
  }
  
  .mode-grid, .mode-card {
    grid-template-columns: 1fr;
  }
  
  .mode-list .news-card {
    flex-direction: column;
    height: auto;
  }
  
  .mode-list .card-cover {
    width: 100%;
    height: 180px;
  }
  
  .mode-masonry {
    column-count: 1;
  }
  
  .hero-title {
    font-size: 24px;
  }
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
}

.image-placeholder .el-icon {
  font-size: 24px;
}
</style>
