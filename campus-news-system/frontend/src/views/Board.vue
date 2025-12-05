<template>
  <div class="board-page">
    <!-- 顶部功能栏 -->
    <div class="board-header-bar">
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
          <div
            v-for="(article, index) in articles"
            :key="article.id"
            class="news-card"
            :class="{ 'is-pinned': article.isPinned }"
            @click="goToDetail(article.id)"
            :style="{ animationDelay: `${index * 0.05}s` }"
          >
            <!-- 封面图区域 -->
            <div class="card-cover" v-if="article.coverImage">
              <el-image :src="article.coverImage" fit="cover" loading="lazy">
                <template #placeholder>
                  <div class="image-placeholder">
                    <el-icon><Picture /></el-icon>
                  </div>
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
            :page-sizes="[12, 24, 36]"
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
const pageSize = ref(12) // Grid layout fits better with multiples of 3/4
const total = ref(0)
const sortBy = ref('date_desc')
const viewMode = ref('grid') // grid, list, card, masonry

const boardType = computed(() => route.params.type)

const boardTitle = computed(() => {
  const titles = {
    OFFICIAL: '官方新闻',
    CAMPUS: '全校新闻',
    COLLEGE: '学院新闻'
  }
  return titles[boardType.value] || '文章列表'
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
    articles.value = data.records
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

watch(boardType, () => {
  currentPage.value = 1
  fetchArticles()
})

onMounted(() => {
  fetchArticles()
})
</script>

<style scoped>
.board-page {
  max-width: 1400px;
  margin: 0 auto;
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* Header Bar */
.board-header-bar {
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border-radius: 16px;
  padding: 20px 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), 
              0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  border: 2px solid rgba(255, 255, 255, 0.5);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.board-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.board-icon.OFFICIAL { background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%); }
.board-icon.CAMPUS { background: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%); }
.board-icon.COLLEGE { background: linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%); }

.header-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.board-title {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  color: #2c3e50;
  letter-spacing: -0.5px;
}

.board-desc {
  font-size: 14px;
  color: #606266;
}

.sort-select {
  width: 160px;
}

/* Grid Layout */
.article-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
  padding-bottom: 40px;
}

/* News Card */
.news-card {
  background: rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border-radius: 16px;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.5);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  position: relative;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06), 
              0 0 0 1px rgba(255, 255, 255, 0.4) inset;
  animation: fadeInUp 0.6s ease-out both;
}

.news-card:hover {
  transform: translateY(-8px);
  background: rgba(255, 255, 255, 0.5);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.12), 
              0 0 0 1px rgba(255, 255, 255, 0.6) inset;
  border-color: rgba(255, 255, 255, 0.8);
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
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
  background: rgba(255, 87, 34, 0.9);
  color: white;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
  backdrop-filter: blur(4px);
  box-shadow: 0 2px 8px rgba(255, 87, 34, 0.4);
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
  padding: 20px;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border-radius: 16px;
  border: 2px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06), 
              0 0 0 1px rgba(255, 255, 255, 0.4) inset;
  width: fit-content;
  margin: 0 auto;
}

.header-right {
  display: flex;
  align-items: center;
}

.view-controls {
  display: flex;
  background: rgba(255, 255, 255, 0.4);
  padding: 4px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  gap: 2px;
}

.view-btn {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  cursor: pointer;
  color: #606266;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  overflow: hidden;
}

.view-btn:hover {
  background: rgba(255, 255, 255, 0.6);
  color: #409eff;
  transform: translateY(-1px);
}

.view-btn:active {
  transform: scale(0.92);
}

.view-btn.active {
  background: #fff;
  color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
  transform: scale(1.05);
}

.view-btn.active::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 50%;
  transform: translateX(-50%);
  width: 4px;
  height: 4px;
  background: #409eff;
  border-radius: 50%;
}

.header-divider {
  height: 24px;
  margin: 0 20px;
  border-color: rgba(0, 0, 0, 0.1);
}

.sort-select {
  width: 150px;
}

/* ================= Layout Modes ================= */

.article-container {
  width: 100%;
  padding-bottom: 40px;
}

/* 1. Grid Mode (Default) */
.mode-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
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
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.8);
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  position: relative;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  animation: fadeInUp 0.8s cubic-bezier(0.34, 1.56, 0.64, 1) both;
  transform-origin: center center;
}

.news-card:hover {
  transform: translateY(-8px) scale(1.01);
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.12);
  z-index: 2;
}

.news-card:active {
  transform: scale(0.98);
}

@keyframes fadeInUp {
  from { 
    opacity: 0; 
    transform: translateY(40px) scale(0.95); 
  }
  to { 
    opacity: 1; 
    transform: translateY(0) scale(1); 
  }
}

/* Responsive Design */
@media (max-width: 768px) {
  .board-header-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
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
}
</style>
