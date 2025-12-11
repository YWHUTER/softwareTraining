<template>
  <div class="home">
    <!-- 顶部轮播图 -->
    <div 
      class="banner-carousel-wrapper"
      v-motion
      :initial="{ opacity: 0, y: -50 }"
      :enter="{ opacity: 1, y: 0, transition: { type: 'spring', stiffness: 250, damping: 25 } }"
    >
      <el-carousel 
        :interval="5000" 
        arrow="hover" 
        :height="carouselHeight"
        indicator-position="bottom"
        class="banner-carousel"
      >
        <el-carousel-item v-for="item in carouselItems" :key="item.id">
          <div class="banner" :style="{ 
            backgroundImage: `url(${item.image})`,
            backgroundPosition: item.position || 'center center'
          }">
            <div class="banner-content">
              <h1 class="banner-title">{{ item.title }}</h1>
              <p class="banner-subtitle">{{ item.subtitle }}</p>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <!-- 统计数据栏 -->
    <div class="stats-row mb-8 grid grid-cols-2 md:grid-cols-4 gap-4" v-motion :initial="{ opacity: 0, y: 20 }" :enter="{ opacity: 1, y: 0, transition: { delay: 100 } }">
      <div class="glass-card p-4 rounded-xl flex items-center gap-4">
        <div class="p-3 rounded-lg bg-blue-50/80 text-blue-600">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <div>
          <div class="text-sm text-gray-600 font-medium">已发布文章</div>
          <div class="text-xl font-bold text-gray-800">
            <CountTo :startVal="0" :endVal="stats.articleCount" />
          </div>
        </div>
      </div>
      <div class="glass-card p-4 rounded-xl flex items-center gap-4">
        <div class="p-3 rounded-lg bg-green-50/80 text-green-600">
          <el-icon :size="24"><User /></el-icon>
        </div>
        <div>
          <div class="text-sm text-gray-600 font-medium">活跃用户</div>
          <div class="text-xl font-bold text-gray-800">
            <CountTo :startVal="0" :endVal="stats.userCount" />
          </div>
        </div>
      </div>
      <div class="glass-card p-4 rounded-xl flex items-center gap-4">
        <div class="p-3 rounded-lg bg-orange-50/80 text-orange-600">
          <el-icon :size="24"><View /></el-icon>
        </div>
        <div>
          <div class="text-sm text-gray-600 font-medium">总浏览量</div>
          <div class="text-xl font-bold text-gray-800">
            <CountTo :startVal="0" :endVal="stats.viewCount" />
          </div>
        </div>
      </div>
      <div class="glass-card p-4 rounded-xl flex items-center gap-4 hoverable" @click="goToComments">
        <div class="p-3 rounded-lg bg-purple-50/80 text-purple-600">
          <el-icon :size="24"><ChatDotRound /></el-icon>
        </div>
        <div>
          <div class="text-sm text-gray-600 font-medium">互动评论</div>
          <div class="text-xl font-bold text-gray-800">
            <CountTo :startVal="0" :endVal="stats.commentCount" />
          </div>
        </div>
      </div>
    </div>

    <!-- 内容区域 -->
    <el-row :gutter="24" class="content-row">
      <!-- 左侧文章列表 -->
      <el-col :xs="24" :sm="24" :md="24" :lg="17" :xl="18">
        <div class="articles-section">
          <!-- 分类筛选栏 -->
          <div 
            class="filter-section"
            v-motion
            :initial="{ opacity: 0, y: 20 }"
            :enter="{ opacity: 1, y: 0, transition: { delay: 200 } }"
          >
            <!-- 头部标题 -->
            <div class="filter-header">
              <div class="section-title">
                <div class="title-icon">
                  <el-icon :size="20"><Tickets /></el-icon>
                </div>
                <h2>最新资讯</h2>
              </div>
              <div class="result-count" v-if="total > 0">
                共找到 <span class="count-number">{{ total }}</span> 篇相关文章
              </div>
            </div>

            <!-- 筛选工具栏 -->
            <div class="filter-toolbar">
              <!-- 左侧：分类Tabs -->
              <div class="filters-left">
                <el-radio-group v-model="currentBoard" @change="handleBoardChange" class="custom-tabs">
                  <el-radio-button label="">
                    <div class="tab-content">
                      <el-icon><Grid /></el-icon>
                      <span>全部</span>
                    </div>
                  </el-radio-button>
                  <el-radio-button label="OFFICIAL">
                    <div class="tab-content">
                      <el-icon><Document /></el-icon>
                      <span>官方新闻</span>
                    </div>
                  </el-radio-button>
                  <el-radio-button label="CAMPUS">
                    <div class="tab-content">
                      <el-icon><School /></el-icon>
                      <span>全校新闻</span>
                    </div>
                  </el-radio-button>
                  <el-radio-button label="COLLEGE">
                    <div class="tab-content">
                      <el-icon><OfficeBuilding /></el-icon>
                      <span>学院新闻</span>
                    </div>
                  </el-radio-button>
                </el-radio-group>
              </div>

              <!-- 右侧：筛选器 -->
              <div class="filters-right">
                <div class="filter-item">
                  <el-date-picker
                    v-model="selectedDate"
                    type="date"
                    placeholder="按日期筛选"
                    clearable
                    value-format="YYYY-MM-DD"
                    @change="handleDateChange"
                    class="custom-input"
                  />
                </div>
                <div class="filter-item">
                  <el-select 
                    v-model="sortBy" 
                    @change="handleSortChange" 
                    class="custom-input sort-select" 
                    placeholder="排序方式"
                  >
                    <el-option label="最新发布" value="date_desc">
                      <div class="option-item">
                        <el-icon><Clock /></el-icon>
                        <span>最新发布</span>
                      </div>
                    </el-option>
                    <el-option label="最早发布" value="date_asc">
                      <div class="option-item">
                        <el-icon><Clock /></el-icon>
                        <span>最早发布</span>
                      </div>
                    </el-option>
                    <el-option label="最多浏览" value="views_desc">
                      <div class="option-item">
                        <el-icon><View /></el-icon>
                        <span>最多浏览</span>
                      </div>
                    </el-option>
                  </el-select>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 文章列表 -->
          <div class="article-list-container">
            <!-- 加载骨架屏 -->
            <div v-if="loading" class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <LoadingSkeleton v-for="i in 6" :key="i" type="card" />
            </div>
            
            <!-- 空状态 -->
            <el-empty v-else-if="articles.length === 0" description="暂无文章" />
            
            <!-- 文章卡片网格 -->
            <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div
                v-for="(article, index) in articles"
                :key="article.id"
                v-motion
                :initial="{ opacity: 0, y: 50 }"
                :enter="{ opacity: 1, y: 0, transition: { delay: index * 50 } }"
              >
                <ModernNewsCard
                  :title="article.title"
                  :summary="article.summary || article.content?.substring(0, 80) + '...'"
                  :image="article.coverImage"
                  :category="getBoardTypeName(article.boardType)"
                  :date="article.createdAt"
                  :views="article.viewCount"
                  :likes="article.likeCount || 0"
                  :comments="article.commentCount"
                  :author="article.author?.realName || article.author?.username"
                  :author-avatar="getValidAvatar(article.author?.avatar)"
                  @click="goToDetail(article.id)"
                />
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
          <el-card 
            class="sidebar-card hot-articles-card" 
            shadow="hover"
            v-motion
            :initial="{ opacity: 0, x: 50 }"
            :enter="{ opacity: 1, x: 0, transition: { delay: 300, type: 'spring' } }"
          >
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

          <!-- AI智能推荐 -->
          <div
            v-motion
            :initial="{ opacity: 0, x: 50 }"
            :enter="{ opacity: 1, x: 0, transition: { delay: 400, type: 'spring' } }"
          >
            <RecommendationCard 
              type="personalized" 
              :count="5" 
              title="智能算法推荐"
              :show-refresh="true"
              :show-more="false"
            />
          </div>

          <!-- 快捷入口 -->
          <el-card 
            class="sidebar-card quick-links-card" 
            shadow="hover"
            v-motion
            :initial="{ opacity: 0, x: 50 }"
            :enter="{ opacity: 1, x: 0, transition: { delay: 500, type: 'spring' } }"
          >
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
          
          <!-- 标签云 -->
          <div
            v-motion
            :initial="{ opacity: 0, x: 50 }"
            :enter="{ opacity: 1, x: 0, transition: { delay: 500, type: 'spring' } }"
          >
            <TagCloud class="sidebar-card" />
          </div>
        </div>
      </el-col>
    </el-row>
    
    <!-- 返回顶部按钮 -->
    <BackToTop />
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getArticleList, getPublicStats } from '@/api/article'
import TagCloud from '@/components/TagCloud.vue'
import BackToTop from '@/components/BackToTop.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import ModernNewsCard from '@/components/ModernNewsCard.vue'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'
import CountTo from '@/components/CountTo.vue'
import RecommendationCard from '@/components/RecommendationCard.vue'

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
const selectedDate = ref(null) // 按日期筛选

// 轮播图数据
const carouselItems = ref([
  {
    id: 1,
    title: '校园新闻中心',
    subtitle: '第一时间了解校园动态，掌握最新资讯',
    image: '/src/assets/carousel-1.jpg',
    position: 'center 45%'  // 白色建筑与黄叶，稍微向上调整展示建筑主体
  },
  {
    id: 2,
    title: '学术前沿',
    subtitle: '探索知识边界，引领学术潮流',
    image: '/src/assets/carousel-2.jpg',
    position: 'center 45%'  // 向上调整，优先显示马雕像
  },
  {
    id: 3,
    title: '校园生活',
    subtitle: '精彩活动不断，青春岁月绽放',
    image: '/src/assets/carousel-3.jpg',
    position: 'center 80%'  // 弧形建筑仰拍，显示建筑主体
  },
  {
    id: 4,
    title: '通知公告',
    subtitle: '重要信息及时发布，服务师生便捷高效',
    image: '/src/assets/carousel-4.jpg',
    position: 'center center'  // 居中显示，突出中间的大楼
  }
])

// 响应式轮播图高度
const windowWidth = ref(window.innerWidth)
const carouselHeight = computed(() => {
  if (windowWidth.value < 768) return '160px'
  if (windowWidth.value < 992) return '200px'
  return '240px'
})

// 监听窗口大小变化
const handleResize = () => {
  windowWidth.value = window.innerWidth
}

// 统计数据
const stats = ref({
  articleCount: 0,
  userCount: 0,
  viewCount: 0,
  commentCount: 0
})

const fetchStats = async () => {
  try {
    const data = await getPublicStats()
    if (data) {
      stats.value = data
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const fetchArticles = async () => {
  loading.value = true
  try {
    // 解析排序参数
    const [sortField, sortOrder] = sortBy.value.split('_')
    const selectedDateStr = selectedDate.value || undefined
    const data = await getArticleList({
      current: currentPage.value,
      size: pageSize.value,
      boardType: currentBoard.value || undefined,
      isApproved: 1,
      sortBy: sortField === 'views' ? 'views' : 'date',
      sortOrder: sortOrder,
      startDate: selectedDateStr,
      endDate: selectedDateStr,
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

const handleDateChange = () => {
  currentPage.value = 1
  fetchArticles()
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

const goToComments = () => {
  router.push('/comments')
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

// 验证头像URL是否有效
const getValidAvatar = (avatar) => {
  if (!avatar) return undefined
  // 只接受以 /api/file 开头的有效头像URL
  if (avatar.startsWith('/api/file/')) return avatar
  // 其他URL视为无效
  return undefined
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
  fetchStats()
  fetchArticles()
  fetchHotArticles()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
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

/* 轮播图容器 */
.banner-carousel-wrapper {
  margin-bottom: 40px;
}

.banner-carousel {
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
}

/* 轮播图指示器样式 */
.banner-carousel :deep(.el-carousel__indicators) {
  bottom: 20px;
}

.banner-carousel :deep(.el-carousel__indicator) {
  margin: 0 8px;
}

.banner-carousel :deep(.el-carousel__button) {
  width: 40px;
  height: 4px;
  border-radius: 2px;
  background-color: rgba(255, 255, 255, 0.5);
  transition: all 0.3s;
}

.banner-carousel :deep(.is-active .el-carousel__button) {
  background-color: #fff;
  width: 60px;
}

/* 轮播图切换箭头样式 */
.banner-carousel :deep(.el-carousel__arrow) {
  background-color: rgba(0, 0, 0, 0.3);
  color: #fff;
  font-size: 20px;
  width: 45px;
  height: 45px;
  border-radius: 50%;
  backdrop-filter: blur(10px);
  transition: all 0.3s;
}

.banner-carousel :deep(.el-carousel__arrow:hover) {
  background-color: rgba(0, 0, 0, 0.5);
  transform: scale(1.1);
}

/* 横幅样式 */
.banner {
  /* background-position 通过内联样式动态设置 */
  background-size: cover;
  background-repeat: no-repeat;
  height: 100%;
  padding: 80px 60px;
  color: #fff;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
}

.banner::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.25); /* 纯黑遮罩，还原图片本色 */
  z-index: 0;
}

.banner-content {
  position: relative;
  z-index: 1;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
}

.banner-title {
  font-size: 48px;
  font-weight: 800;
  margin: 0 0 20px;
  letter-spacing: -1px;
  color: #fff;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
}

.banner-subtitle {
  font-size: 20px;
  margin: 0;
  opacity: 0.95;
  font-weight: 500;
  color: #fff;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
}

/* 内容区域 */
.content-row {
  width: 100%;
}

/* 文章区域 */
.articles-section {
  width: 100%;
}

/* 筛选栏容器 */
.filter-section {
  background: rgba(255, 255, 255, 0.45);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border-radius: 20px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05), 
              0 0 0 1px rgba(255, 255, 255, 0.6) inset;
  display: flex;
  flex-direction: column;
  gap: 20px;
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.filter-section:hover {
  background: rgba(255, 255, 255, 0.6);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08), 
              0 0 0 1px rgba(255, 255, 255, 0.8) inset;
}

/* 头部样式 */
.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  padding-bottom: 16px;
}

.section-title {
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

.section-title h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #2c3e50;
  letter-spacing: 0.5px;
}

.result-count {
  font-size: 14px;
  color: #909399;
}

.count-number {
  color: #667eea;
  font-weight: 700;
  font-family: 'DIN Alternate', sans-serif;
  font-size: 16px;
  margin: 0 2px;
}

/* 工具栏样式 */
.filter-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

/* 左侧 Tabs 样式 */
.filters-left {
  flex: 1;
  min-width: 300px;
}

.custom-tabs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.custom-tabs :deep(.el-radio-button__inner) {
  border: none !important;
  border-radius: 8px !important;
  padding: 8px 16px !important;
  background: white !important;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05), 0 0 0 1px rgba(0,0,0,0.02) !important;
  color: #606266 !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  height: 36px;
  display: flex;
  align-items: center;
}

.custom-tabs :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-left: none !important;
  border-radius: 8px !important;
}

.custom-tabs :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 8px !important;
}

/* 选中状态 */
.custom-tabs :deep(.el-radio-button.is-active .el-radio-button__inner) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  color: white !important;
  box-shadow: 0 4px 12px rgba(118, 75, 162, 0.3) !important;
  transform: translateY(-1px);
}

/* 悬浮状态 */
.custom-tabs :deep(.el-radio-button:not(.is-active) .el-radio-button__inner:hover) {
  color: #667eea !important;
  background: #f8f9fa !important;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08), 0 0 0 1px rgba(0,0,0,0.02) !important;
}

.tab-content {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}

/* 右侧筛选器样式 */
.filters-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-item {
  position: relative;
}

.custom-input {
  width: 160px;
}

/* 输入框样式覆盖 */
.custom-input :deep(.el-input__wrapper) {
  background: white !important;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05) !important;
  border-radius: 8px !important;
  padding: 4px 12px !important;
  transition: all 0.3s ease;
}

.custom-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08) !important;
  transform: translateY(-1px);
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2), 0 2px 12px rgba(0, 0, 0, 0.08) !important;
}

.sort-select {
  width: 140px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 文章列表 */
.article-list {
  min-height: 400px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.article-card {
  background: rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border-radius: 16px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid rgba(255, 255, 255, 0.5);
  position: relative;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06), 
              0 0 0 1px rgba(255, 255, 255, 0.4) inset;
}

.article-card:hover {
  transform: translateY(-8px) scale(1.01);
  background: rgba(255, 255, 255, 0.5);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.12), 
              0 0 0 1px rgba(255, 255, 255, 0.6) inset;
  border-color: rgba(255, 255, 255, 0.8);
}

.article-card:hover .article-title {
  color: #667eea;
}

.article-card:hover .article-cover {
  transform: scale(1.05);
}

.article-card:active {
  transform: translateY(-2px) scale(1.005);
}

.article-card.pinned {
  background: linear-gradient(135deg, #fff9e6 0%, #fff 100%);
  border-color: #ffd700;
}

.pinned-badge {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 10;
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
  line-clamp: 2;
  -webkit-box-orient: vertical;
  transition: color 0.3s ease;
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
  line-clamp: 2;
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
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.article-cover :deep(.image-container) {
  width: 100%;
  height: 100%;
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
  gap: 24px;
}

.sidebar-card {
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border-radius: 16px;
  border: 2px solid rgba(255, 255, 255, 0.5);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06), 
              0 0 0 1px rgba(255, 255, 255, 0.4) inset;
}

.sidebar-card:hover {
  transform: translateY(-4px);
  background: rgba(255, 255, 255, 0.45);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1), 
              0 0 0 1px rgba(255, 255, 255, 0.6) inset;
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
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: #fafafa;
  border: 1px solid transparent;
}

.hot-item:hover {
  background: #f0f2f5;
  transform: translateX(6px);
  border-color: #667eea;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.hot-item:hover .hot-title {
  color: #667eea;
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
  line-clamp: 2;
  -webkit-box-orient: vertical;
  transition: color 0.3s ease;
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

  .banner-carousel {
    border-radius: 16px;
  }

  .banner-carousel :deep(.el-carousel) {
    height: 200px !important;
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
  }

  .banner-carousel {
    border-radius: 12px;
  }

  .banner-carousel :deep(.el-carousel) {
    height: 160px !important;
  }

  .banner-carousel :deep(.el-carousel__arrow) {
    width: 36px;
    height: 36px;
    font-size: 16px;
  }

  .banner-title {
    font-size: 26px;
  }

  .banner-subtitle {
    font-size: 14px;
  }

  .filter-section {
    padding: 16px;
  }

  .filter-toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .filters-left {
    width: 100%;
    min-width: 0;
  }

  .custom-tabs {
    width: 100%;
    overflow-x: auto;
    padding-bottom: 4px; /* 为滚动条留空间 */
  }

  .custom-tabs :deep(.el-radio-button) {
    flex-shrink: 0;
  }
  
  .filters-right {
    flex-direction: column;
    width: 100%;
    gap: 12px;
  }
  
  .filter-item {
    width: 100%;
  }
  
  .custom-input, .sort-select {
    width: 100% !important;
  }

  .article-list {
    min-height: auto;
  }
  
  /* 隐藏滚动条但保持功能 */
  .custom-tabs::-webkit-scrollbar {
    height: 0;
    width: 0;
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

/* 热门评论样式 */
.hot-comment {
  margin-top: 16px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #fff5f5 0%, #fef5f0 100%);
  border-radius: 8px;
  border-left: 3px solid #f56c6c;
}

.hot-comment-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.hot-comment-label {
  font-size: 12px;
  font-weight: 600;
  color: #f56c6c;
}

.hot-comment-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.comment-avatar {
  background: linear-gradient(135deg, #f56c6c, #e6a23c);
  color: white;
  font-size: 10px;
  font-weight: 600;
  flex-shrink: 0;
}

.comment-author {
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  flex-shrink: 0;
}

.comment-text {
  font-size: 13px;
  color: #606266;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 500px;
}

.comment-likes {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #f56c6c;
  flex-shrink: 0;
  margin-left: auto;
}

@media (max-width: 768px) {
  .hot-comment {
    padding: 10px 12px;
  }

  .comment-text {
    max-width: 200px;
  }
}

/* 骨架屏样式 */
.skeleton-card {
  pointer-events: none;
  cursor: default;
}

.skeleton-card .article-cover {
  background: #f2f3f5;
}

/* 玻璃态卡片通用样式 */
.glass-card {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.05);
  transition: all 0.3s ease;
}

.glass-card:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(31, 38, 135, 0.1);
}

.hoverable {
  cursor: pointer;
}

/* ========== 暗黑模式适配 ========== */
:global(.dark) .glass-card,
:global([data-theme="dark"]) .glass-card {
  background: var(--bg-card);
  border-color: var(--border-color);
}

:global(.dark) .glass-card:hover,
:global([data-theme="dark"]) .glass-card:hover {
  background: var(--bg-glass-hover);
}

:global(.dark) .section-title h2,
:global([data-theme="dark"]) .section-title h2 {
  color: var(--text-primary);
}

:global(.dark) .filter-section,
:global([data-theme="dark"]) .filter-section {
  background: var(--bg-card);
  border-color: var(--border-color);
}

:global(.dark) .sidebar-card,
:global([data-theme="dark"]) .sidebar-card {
  background: var(--bg-card);
  border-color: var(--border-color);
}

:global(.dark) .card-title span,
:global([data-theme="dark"]) .card-title span {
  color: var(--text-primary);
}

:global(.dark) .hot-item,
:global([data-theme="dark"]) .hot-item {
  background: transparent;
}

:global(.dark) .hot-item:hover,
:global([data-theme="dark"]) .hot-item:hover {
  background: var(--bg-tertiary);
}

:global(.dark) .hot-title,
:global([data-theme="dark"]) .hot-title {
  color: var(--text-primary);
}

:global(.dark) .hot-meta,
:global(.dark) .hot-stat,
:global([data-theme="dark"]) .hot-meta,
:global([data-theme="dark"]) .hot-stat {
  color: var(--text-tertiary);
}

:global(.dark) .quick-link span,
:global([data-theme="dark"]) .quick-link span {
  color: var(--text-secondary);
}

:global(.dark) .quick-link:hover span,
:global([data-theme="dark"]) .quick-link:hover span {
  color: var(--primary-color);
}

:global(.dark) .comment-author,
:global(.dark) .comment-text,
:global([data-theme="dark"]) .comment-author,
:global([data-theme="dark"]) .comment-text {
  color: var(--text-secondary);
}

:global(.dark) .skeleton-card .article-cover,
:global([data-theme="dark"]) .skeleton-card .article-cover {
  background: var(--bg-tertiary);
}

:global(.dark) .pagination-wrapper,
:global([data-theme="dark"]) .pagination-wrapper {
  background: var(--bg-card);
  border-color: var(--border-color);
}
</style>
