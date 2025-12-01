<template>
  <div class="home">
    <!-- 顶部横幅 -->
    <div class="banner">
      <div class="banner-content">
        <h1 class="banner-title">校园新闻中心</h1>
        <p class="banner-subtitle">第一时间了解校园动态，掌握最新资讯</p>
      </div>
    </div>

    <!-- 内容区域 -->
    <el-row :gutter="24" class="content-row">
      <!-- 左侧文章列表 -->
      <el-col :xs="24" :sm="24" :md="24" :lg="17" :xl="18">
        <div class="articles-section">
          <!-- 分类筛选栏 -->
          <div class="filter-section">
            <div class="section-title">
              <el-icon :size="20"><Tickets /></el-icon>
              <h2>最新资讯</h2>
            </div>
            <div class="filter-controls">
              <el-radio-group v-model="currentBoard" @change="handleBoardChange" class="board-filters">
                <el-radio-button label="">
                  <el-icon><Grid /></el-icon>
                  <span>全部</span>
                </el-radio-button>
                <el-radio-button label="OFFICIAL">
                  <el-icon><Document /></el-icon>
                  <span>官方新闻</span>
                </el-radio-button>
                <el-radio-button label="CAMPUS">
                  <el-icon><School /></el-icon>
                  <span>全校新闻</span>
                </el-radio-button>
                <el-radio-button label="COLLEGE">
                  <el-icon><OfficeBuilding /></el-icon>
                  <span>学院新闻</span>
                </el-radio-button>
              </el-radio-group>
              <el-select v-model="sortBy" @change="handleSortChange" class="sort-select" placeholder="排序方式">
                <el-option label="按日期排序（最新）" value="date_desc">
                  <el-icon><Clock /></el-icon>
                  <span>按日期排序（最新）</span>
                </el-option>
                <el-option label="按日期排序（最早）" value="date_asc">
                  <el-icon><Clock /></el-icon>
                  <span>按日期排序（最早）</span>
                </el-option>
                <el-option label="按热度排序（最高）" value="views_desc">
                  <el-icon><TrendCharts /></el-icon>
                  <span>按热度排序（最高）</span>
                </el-option>
                <el-option label="按热度排序（最低）" value="views_asc">
                  <el-icon><TrendCharts /></el-icon>
                  <span>按热度排序（最低）</span>
                </el-option>
              </el-select>
            </div>
          </div>
          
          <!-- 文章列表 -->
          <div v-loading="loading" class="article-list" element-loading-text="加载中...">
            <el-empty v-if="articles.length === 0 && !loading" description="暂无文章" />
            
            <div
              v-for="(article, index) in articles"
              :key="article.id"
              class="article-card hover-lift"
              :class="{ 'pinned': article.isPinned }"
              @click="goToDetail(article.id)"
              :style="{ animationDelay: `${index * 0.05}s` }"
            >
              <!-- 置顶标识 -->
              <div class="pinned-badge" v-if="article.isPinned">
                <el-icon><Star /></el-icon>
                <span>置顶</span>
              </div>

              <div class="article-content">
                <!-- 左侧主要内容 -->
                <div class="article-main">
                  <!-- 文章头部信息 -->
                  <div class="article-header">
                    <el-tag 
                      :type="getBoardTypeTag(article.boardType)" 
                      size="small"
                      effect="plain"
                      class="board-tag"
                    >
                      {{ getBoardTypeName(article.boardType) }}
                    </el-tag>
                    <h3 class="article-title">{{ article.title }}</h3>
                  </div>
                  
                  <!-- 文章摘要 -->
                  <p class="article-summary">
                    {{ article.summary || article.content?.substring(0, 120) + '...' }}
                  </p>
                  
                  <!-- 文章元信息 -->
                  <div class="article-meta">
                    <div class="meta-item">
                      <el-avatar :size="24" class="author-avatar">
                        {{ article.author?.realName?.[0] }}
                      </el-avatar>
                      <span class="author-name">{{ article.author?.realName }}</span>
                    </div>
                    <span class="meta-divider">·</span>
                    <div class="meta-item" v-if="article.college">
                      <el-icon><School /></el-icon>
                      <span>{{ article.college?.name }}</span>
                    </div>
                    <span class="meta-divider" v-if="article.college">·</span>
                    <div class="meta-item">
                      <el-icon><Clock /></el-icon>
                      <span>{{ formatTime(article.createdAt) }}</span>
                    </div>
                    <div class="meta-stats">
                      <span class="stat-item">
                        <el-icon><View /></el-icon>
                        {{ article.viewCount }}
                      </span>
                      <span class="stat-item">
                        <el-icon><ChatDotRound /></el-icon>
                        {{ article.commentCount }}
                      </span>
                    </div>
                  </div>
                </div>
                
                <!-- 右侧封面图 -->
                <div v-if="article.coverImage" class="article-cover">
                  <el-image 
                    :src="article.coverImage" 
                    fit="cover"
                    lazy
                  >
                    <template #error>
                      <div class="image-error">
                        <el-icon><Picture /></el-icon>
                      </div>
                    </template>
                  </el-image>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 分页 -->
          <div class="pagination-wrapper" v-if="total > 0">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="total"
              :page-sizes="[10, 20, 30, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="fetchArticles"
              @size-change="fetchArticles"
              background
            />
          </div>
        </div>
      </el-col>
      
      <!-- 右侧边栏 -->
      <el-col :xs="24" :sm="24" :md="24" :lg="7" :xl="6">
        <div class="sidebar">
          <!-- 热门文章 -->
          <el-card class="sidebar-card hot-articles-card" shadow="hover">
            <template #header>
              <div class="card-title">
                <el-icon :size="18" color="#f56c6c"><TrendCharts /></el-icon>
                <span>热门 TOP10</span>
              </div>
            </template>
            <div class="hot-articles">
              <div 
                v-for="(article, index) in hotArticles" 
                :key="article.id" 
                class="hot-item" 
                @click="goToDetail(article.id)"
              >
                <div class="hot-rank" :class="getRankClass(index)">
                  {{ index + 1 }}
                </div>
                <div class="hot-content">
                  <h4 class="hot-title">{{ article.title }}</h4>
                  <div class="hot-meta">
                    <span class="hot-stat">
                      <el-icon><View /></el-icon>
                      {{ article.viewCount }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </el-card>

          <!-- 快捷入口 -->
          <el-card class="sidebar-card quick-links-card" shadow="hover">
            <template #header>
              <div class="card-title">
                <el-icon :size="18" color="#409eff"><Pointer /></el-icon>
                <span>快捷入口</span>
              </div>
            </template>
            <div class="quick-links">
              <div class="quick-link" @click="$router.push('/publish')">
                <div class="quick-icon" style="background: linear-gradient(135deg, #667eea, #764ba2);">
                  <el-icon :size="20"><Edit /></el-icon>
                </div>
                <span>发布文章</span>
              </div>
              <div class="quick-link" @click="$router.push('/profile')">
                <div class="quick-icon" style="background: linear-gradient(135deg, #f093fb, #f5576c);">
                  <el-icon :size="20"><User /></el-icon>
                </div>
                <span>个人中心</span>
              </div>
            </div>
          </el-card>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getArticleList } from '@/api/article'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const articles = ref([])
const hotArticles = ref([])
const currentBoard = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const sortBy = ref('date_desc') // 默认按日期降序

const fetchArticles = async () => {
  loading.value = true
  try {
    // 解析排序参数
    const [sortField, sortOrder] = sortBy.value.split('_')
    const data = await getArticleList({
      current: currentPage.value,
      size: pageSize.value,
      boardType: currentBoard.value || undefined,
      isApproved: 1,
      sortBy: sortField === 'views' ? 'views' : 'date',
      sortOrder: sortOrder,
      _t: Date.now() // 防止缓存
    })
    articles.value = data.records
    total.value = data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const fetchHotArticles = async () => {
  try {
    const data = await getArticleList({
      current: 1,
      size: 10,                // 热门 TOP10
      isApproved: 1,
      sortBy: 'views',         // 按浏览量排序
      sortOrder: 'desc',       // 降序（最高在前）
      _t: Date.now() // 防止缓存
    })
    // 确保最多只显示10个
    hotArticles.value = (data.records || []).slice(0, 10)
  } catch (error) {
    console.error(error)
  }
}

const handleBoardChange = () => {
  currentPage.value = 1
  fetchArticles()
}

const handleSortChange = () => {
  currentPage.value = 1
  fetchArticles()
}

const goToDetail = (id) => {
  router.push(`/article/${id}`)
}

const getBoardTypeName = (type) => {
  const types = {
    OFFICIAL: '官方新闻',
    CAMPUS: '全校新闻',
    COLLEGE: '学院新闻'
  }
  return types[type] || type
}

const getBoardTypeTag = (type) => {
  const tags = {
    OFFICIAL: 'danger',
    CAMPUS: 'primary',
    COLLEGE: 'success'
  }
  return tags[type] || ''
}

const getRankClass = (index) => {
  if (index === 0) return 'rank-1'
  if (index === 1) return 'rank-2'
  if (index === 2) return 'rank-3'
  return ''
}

const formatTime = (time) => {
  const now = new Date()
  const past = new Date(time)
  const diff = now - past
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return new Date(time).toLocaleDateString('zh-CN')
}

onMounted(() => {
  fetchArticles()
  fetchHotArticles()
})

// 监听路由变化，当返回首页时刷新数据
watch(() => route.path, (newPath) => {
  if (newPath === '/') {
    fetchArticles()
    fetchHotArticles()
  }
})
</script>

<style scoped>
.home {
  width: 100%;
}

/* 顶部横幅 */
.banner {
  background: url('@/assets/home-bg.jpg') center center / cover no-repeat;
  border-radius: 16px;
  padding: 60px 40px;
  margin-bottom: 30px;
  color: white;
  position: relative;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.banner::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.35);
}

.banner-content {
  position: relative;
  z-index: 1;
}

.banner-title {
  font-size: 42px;
  font-weight: 700;
  margin: 0 0 15px;
  letter-spacing: 1px;
}

.banner-subtitle {
  font-size: 18px;
  margin: 0;
  opacity: 0.95;
  font-weight: 300;
}

/* 内容区域 */
.content-row {
  width: 100%;
}

/* 文章区域 */
.articles-section {
  width: 100%;
}

/* 筛选栏 */
.filter-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-title h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #2c3e50;
}

.filter-controls {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.board-filters {
  display: flex;
  gap: 8px;
}

.sort-select {
  width: 180px;
}

.sort-select :deep(.el-input__inner) {
  font-weight: 500;
}

.sort-select :deep(.el-select-dropdown__item) {
  display: flex;
  align-items: center;
  gap: 8px;
}

.board-filters :deep(.el-radio-button__inner) {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 500;
}

/* 文章列表 */
.article-list {
  min-height: 400px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.article-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #e4e7ed;
  position: relative;
  overflow: hidden;
  animation: fadeInUp 0.5s ease-out both;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.article-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12);
  border-color: #2196f3;
}

.article-card.pinned {
  background: linear-gradient(135deg, #fff9e6 0%, #fff 100%);
  border-color: #ffd700;
}

.pinned-badge {
  position: absolute;
  top: 16px;
  right: 16px;
  background: linear-gradient(135deg, #ff9800 0%, #ff5722 100%);
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
  box-shadow: 0 2px 8px rgba(255, 152, 0, 0.4);
}

.article-content {
  display: flex;
  gap: 24px;
}

.article-main {
  flex: 1;
  min-width: 0;
}

.article-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.board-tag {
  border-radius: 6px;
  font-weight: 600;
}

.article-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #2c3e50;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.article-summary {
  margin: 12px 0;
  color: #606266;
  font-size: 15px;
  line-height: 1.7;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.article-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  color: #909399;
  font-size: 14px;
  margin-top: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.author-avatar {
  background: linear-gradient(135deg, #2196f3, #1976d2);
  color: white;
  font-weight: 600;
  font-size: 12px;
}

.author-name {
  font-weight: 500;
  color: #606266;
}

.meta-divider {
  color: #dcdfe6;
}

.meta-stats {
  margin-left: auto;
  display: flex;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
}

/* 封面图 */
.article-cover {
  width: 200px;
  height: 150px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
}

.article-cover .el-image {
  width: 100%;
  height: 100%;
}

.image-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 32px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding: 20px;
  background: white;
  border-radius: 12px;
}

/* 侧边栏 */
.sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sidebar-card {
  border-radius: 12px;
  border: none;
  transition: all 0.3s ease;
}

.sidebar-card:hover {
  transform: translateY(-2px);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 16px;
  color: #2c3e50;
}

/* 热门文章 */
.hot-articles {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hot-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fafafa;
}

.hot-item:hover {
  background: #f0f2f5;
  transform: translateX(4px);
}

.hot-rank {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e4e7ed;
  color: #606266;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}

.hot-rank.rank-1 {
  background: linear-gradient(135deg, #ffd700, #ff8c00);
  color: white;
  box-shadow: 0 2px 8px rgba(255, 215, 0, 0.4);
}

.hot-rank.rank-2 {
  background: linear-gradient(135deg, #c0c0c0, #a9a9a9);
  color: white;
}

.hot-rank.rank-3 {
  background: linear-gradient(135deg, #cd7f32, #b8860b);
  color: white;
}

.hot-content {
  flex: 1;
  min-width: 0;
}

.hot-title {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 500;
  color: #2c3e50;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.hot-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hot-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 12px;
}

/* 快捷入口 */
.quick-links {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quick-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.quick-link:hover {
  background: #f0f2f5;
  transform: translateX(4px);
}

.quick-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.quick-link span {
  font-weight: 500;
  color: #2c3e50;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .article-cover {
    width: 160px;
    height: 120px;
  }
}

@media (max-width: 992px) {
  .banner {
    padding: 40px 30px;
  }

  .banner-title {
    font-size: 32px;
  }

  .banner-subtitle {
    font-size: 16px;
  }

  .sidebar {
    margin-top: 20px;
  }
}

@media (max-width: 768px) {
  .banner {
    padding: 30px 20px;
    border-radius: 12px;
  }

  .banner-title {
    font-size: 26px;
  }

  .banner-subtitle {
    font-size: 14px;
  }

  .filter-section {
    flex-direction: column;
    align-items: stretch;
    padding: 16px;
  }

  .board-filters {
    flex-wrap: wrap;
  }

  .board-filters :deep(.el-radio-button__inner) {
    padding: 8px 16px;
  }

  .article-card {
    padding: 16px;
  }

  .article-content {
    flex-direction: column-reverse;
  }

  .article-cover {
    width: 100%;
    height: 180px;
  }

  .article-title {
    font-size: 18px;
  }

  .article-summary {
    font-size: 14px;
  }

  .meta-stats {
    margin-left: 0;
    margin-top: 8px;
    width: 100%;
  }
}
</style>
