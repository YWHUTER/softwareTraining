<template>
  <div class="search-page">
    <!-- 搜索横幅 -->
    <div class="search-banner">
      <div class="banner-content">
        <h1 class="banner-title">
          <el-icon><Search /></el-icon>
          新闻搜索
        </h1>
        <p class="banner-subtitle">输入关键词，快速找到您想要的新闻资讯</p>
        
        <!-- 搜索框 -->
        <div class="search-box">
          <el-input
            v-model="keyword"
            placeholder="请输入搜索关键词，如：校园活动、讲座、竞赛..."
            size="large"
            clearable
            @keyup.enter="handleSearch"
            class="search-input"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button 
            type="primary" 
            size="large" 
            @click="handleSearch"
            :loading="loading"
            class="search-btn"
          >
            搜索
          </el-button>
        </div>
        
        <!-- 热门搜索 -->
        <div class="hot-keywords" v-if="!hasSearched">
          <span class="hot-label">热门搜索：</span>
          <el-tag 
            v-for="tag in hotKeywords" 
            :key="tag"
            @click="quickSearch(tag)"
            class="hot-tag"
            effect="plain"
          >
            {{ tag }}
          </el-tag>
        </div>

        <!-- 搜索历史 -->
        <div class="search-history" v-if="!hasSearched && searchHistory.length > 0">
          <div class="history-header">
            <span class="history-label">搜索历史</span>
            <el-button link type="info" @click="clearHistory" size="small" class="clear-btn">
              <el-icon><Delete /></el-icon> 清空
            </el-button>
          </div>
          <div class="history-tags">
            <el-tag 
              v-for="tag in searchHistory" 
              :key="tag"
              @click="quickSearch(tag)"
              closable
              @close="deleteHistoryItem(tag)"
              class="history-tag"
              type="info"
              effect="plain"
            >
              {{ tag }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 搜索结果区域 -->
    <div class="search-results" v-if="hasSearched">
      <!-- 结果统计 -->
      <div class="results-header">
        <div class="results-info">
          <span class="keyword-highlight">"{{ searchedKeyword }}"</span>
          <span class="results-count">
            共找到 <strong>{{ total }}</strong> 条相关结果
          </span>
        </div>
        <div class="filter-options">
          <el-select v-model="sortBy" @change="handleSortChange" placeholder="排序方式" class="sort-select">
            <el-option label="相关度优先" value="relevance">
              <el-icon><Aim /></el-icon>
              <span>相关度优先</span>
            </el-option>
            <el-option label="最新发布" value="date_desc">
              <el-icon><Clock /></el-icon>
              <span>最新发布</span>
            </el-option>
            <el-option label="最早发布" value="date_asc">
              <el-icon><Clock /></el-icon>
              <span>最早发布</span>
            </el-option>
            <el-option label="最多浏览" value="views_desc">
              <el-icon><View /></el-icon>
              <span>最多浏览</span>
            </el-option>
          </el-select>
          <el-select v-model="boardType" @change="handleBoardChange" placeholder="文章类型" clearable class="board-select">
            <el-option label="全部类型" value="">
              <el-icon><Grid /></el-icon>
              <span>全部类型</span>
            </el-option>
            <el-option label="官方新闻" value="OFFICIAL">
              <el-icon><Document /></el-icon>
              <span>官方新闻</span>
            </el-option>
            <el-option label="全校新闻" value="CAMPUS">
              <el-icon><School /></el-icon>
              <span>全校新闻</span>
            </el-option>
            <el-option label="学院新闻" value="COLLEGE">
              <el-icon><OfficeBuilding /></el-icon>
              <span>学院新闻</span>
            </el-option>
          </el-select>
        </div>
      </div>

      <!-- 结果列表 -->
      <div class="article-list">
        <!-- 骨架屏加载状态 -->
        <div v-if="loading" class="skeleton-list">
          <el-card v-for="i in 5" :key="i" class="article-card skeleton-card">
            <div class="skeleton-content">
              <div class="skeleton-main">
                <el-skeleton animated>
                  <template #template>
                    <el-skeleton-item variant="text" style="width: 60px; margin-bottom: 10px" />
                    <el-skeleton-item variant="h3" style="width: 60%; margin-bottom: 15px" />
                    <el-skeleton-item variant="p" style="width: 100%; margin-bottom: 5px" />
                    <el-skeleton-item variant="p" style="width: 80%; margin-bottom: 15px" />
                    <div style="display: flex; align-items: center; gap: 10px">
                      <el-skeleton-item variant="circle" style="width: 24px; height: 24px" />
                      <el-skeleton-item variant="text" style="width: 100px" />
                    </div>
                  </template>
                </el-skeleton>
              </div>
              <el-skeleton-item variant="image" style="width: 200px; height: 150px; border-radius: 8px" />
            </div>
          </el-card>
        </div>

        <template v-else>
          <el-empty v-if="articles.length === 0" description="未找到相关结果，请尝试其他关键词">
            <template #image>
              <el-icon :size="80" color="#c0c4cc"><Search /></el-icon>
            </template>
          </el-empty>
          
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
                <h3 class="article-title" v-html="highlightKeyword(article.title)"></h3>
              </div>
              
              <!-- 文章摘要 -->
              <p class="article-summary" v-html="highlightKeyword(article.summary || article.content?.substring(0, 120) + '...')">
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
      </template>
    </div>
      
      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchResults"
          @size-change="fetchResults"
          background
        />
      </div>
    </div>

    <!-- 未搜索时的提示 -->
    <div class="search-tips" v-if="!hasSearched">
      <el-card class="tips-card" shadow="hover">
        <template #header>
          <div class="tips-header">
            <el-icon :size="20" color="#409eff"><InfoFilled /></el-icon>
            <span>搜索小技巧</span>
          </div>
        </template>
        <ul class="tips-list">
          <li>输入关键词后按 <kbd>Enter</kbd> 键或点击搜索按钮开始搜索</li>
          <li>支持标题和内容的模糊匹配搜索</li>
          <li>可以使用筛选功能缩小搜索范围</li>
          <li>尝试使用不同的关键词获得更好的搜索结果</li>
        </ul>
      </el-card>
    </div>
    <el-backtop :right="40" :bottom="40" />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getArticleList } from '@/api/article'
import { Search, InfoFilled } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const keyword = ref('')
const searchedKeyword = ref('')
const loading = ref(false)
const hasSearched = ref(false)
const articles = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const sortBy = ref('relevance')
const boardType = ref('')
const searchHistory = ref([])

// 热门搜索关键词
const hotKeywords = ['校园活动', '讲座', '竞赛', '招聘', '学术', '通知']

// 从URL参数初始化搜索
onMounted(() => {
  loadHistory()
  if (route.query.keyword) {
    keyword.value = route.query.keyword
    handleSearch()
  }
})

// 监听路由变化
watch(() => route.query.keyword, (newKeyword) => {
  if (newKeyword && newKeyword !== searchedKeyword.value) {
    keyword.value = newKeyword
    handleSearch()
  }
})

const handleSearch = async () => {
  if (!keyword.value.trim()) {
    return
  }
  
  searchedKeyword.value = keyword.value.trim()
  saveHistory(searchedKeyword.value)
  currentPage.value = 1
  hasSearched.value = true
  
  // 更新URL
  router.replace({
    path: '/search',
    query: { keyword: searchedKeyword.value }
  })
  
  await fetchResults()
}

const quickSearch = (tag) => {
  keyword.value = tag
  handleSearch()
}

const fetchResults = async () => {
  if (!searchedKeyword.value) return
  
  loading.value = true
  try {
    // 解析排序参数
    let sortField = 'date'
    let sortOrder = 'desc'
    
    if (sortBy.value === 'date_desc') {
      sortField = 'date'
      sortOrder = 'desc'
    } else if (sortBy.value === 'date_asc') {
      sortField = 'date'
      sortOrder = 'asc'
    } else if (sortBy.value === 'views_desc') {
      sortField = 'views'
      sortOrder = 'desc'
    }
    // relevance 使用默认排序
    
    const data = await getArticleList({
      current: currentPage.value,
      size: pageSize.value,
      keyword: searchedKeyword.value,
      boardType: boardType.value || undefined,
      isApproved: 1,
      sortBy: sortField,
      sortOrder: sortOrder,
      _t: Date.now()
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
  fetchResults()
}

const handleBoardChange = () => {
  currentPage.value = 1
  fetchResults()
}

const loadHistory = () => {
  try {
    const history = localStorage.getItem('searchHistory')
    if (history) {
      searchHistory.value = JSON.parse(history)
    }
  } catch (e) {
    console.error('Failed to load search history', e)
  }
}

const saveHistory = (kw) => {
  if (!kw) return
  // 移除已存在的相同关键词
  const index = searchHistory.value.indexOf(kw)
  if (index > -1) {
    searchHistory.value.splice(index, 1)
  }
  // 添加到头部
  searchHistory.value.unshift(kw)
  // 限制数量
  if (searchHistory.value.length > 10) {
    searchHistory.value.pop()
  }
  localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value))
}

const clearHistory = () => {
  searchHistory.value = []
  localStorage.removeItem('searchHistory')
}

const deleteHistoryItem = (tag) => {
  const index = searchHistory.value.indexOf(tag)
  if (index > -1) {
    searchHistory.value.splice(index, 1)
    localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value))
  }
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

const highlightKeyword = (text) => {
  if (!text || !searchedKeyword.value) return text
  const regex = new RegExp(`(${searchedKeyword.value})`, 'gi')
  return text.replace(regex, '<mark class="highlight">$1</mark>')
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
</script>

<style scoped>
.search-page {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
}

/* 搜索横幅 */
.search-banner {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.95) 0%, rgba(118, 75, 162, 0.95) 100%);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  padding: 70px 60px;
  margin-bottom: 40px;
  color: white;
  box-shadow: 0 20px 50px rgba(102, 126, 234, 0.4);
  position: relative;
  overflow: hidden;
  animation: fadeInDown 0.6s ease-out;
}

.search-banner::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -20%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0) 70%);
  border-radius: 50%;
  animation: float 15s infinite ease-in-out;
}

.search-banner::after {
  content: '';
  position: absolute;
  bottom: -40%;
  right: -15%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0) 70%);
  border-radius: 50%;
  animation: float 20s infinite ease-in-out reverse;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(30px, -30px); }
}

.banner-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.banner-title {
  margin: 0 0 16px;
  font-size: 42px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  letter-spacing: -1px;
  animation: fadeInDown 0.6s ease-out;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.banner-subtitle {
  margin: 0 0 40px;
  opacity: 0.95;
  font-size: 18px;
  font-weight: 300;
}

/* 搜索框 */
.search-box {
  display: flex;
  gap: 16px;
  max-width: 800px;
  margin: 0 auto 30px;
}

.search-input {
  flex: 1;
}

.search-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 2px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.search-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.6);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.15);
}

.search-input :deep(.el-input__wrapper.is-focus) {
  border-color: white;
  box-shadow: 0 8px 32px rgba(255, 255, 255, 0.3);
}

.search-btn {
  padding: 0 50px;
  font-weight: 700;
  font-size: 16px;
  background: white;
  color: #667eea;
  border: none;
  box-shadow: 0 6px 20px rgba(255, 255, 255, 0.3);
  transition: all 0.3s ease;
}

.search-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(255, 255, 255, 0.4);
  background: rgba(255, 255, 255, 0.95);
}

/* 热门搜索 */
.hot-keywords {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
}

.hot-label {
  font-size: 14px;
  opacity: 0.9;
}

.hot-tag {
  cursor: pointer;
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
  color: white;
  transition: all 0.3s ease;
}

.hot-tag:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
}

/* 搜索历史 */
.search-history {
  margin-top: 24px;
  animation: fadeInDown 0.6s ease-out;
}

.history-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 12px;
  color: rgba(255, 255, 255, 0.9);
}

.history-label {
  font-size: 14px;
  opacity: 0.9;
}

.clear-btn {
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
}

.clear-btn:hover {
  color: white;
}

.history-tags {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
}

.history-tag {
  cursor: pointer;
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.9);
  transition: all 0.3s ease;
}

.history-tag:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
  color: white;
}

.history-tag :deep(.el-tag__close) {
  color: rgba(255, 255, 255, 0.6);
}

.history-tag :deep(.el-tag__close:hover) {
  color: white;
  background-color: rgba(255, 255, 255, 0.2);
}

/* 搜索结果 */
.search-results {
  width: 100%;
}

.results-header {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 24px 30px;
  margin-bottom: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.5);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.results-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.keyword-highlight {
  font-size: 18px;
  font-weight: 600;
  color: #667eea;
}

.results-count {
  color: #606266;
  font-size: 14px;
}

.results-count strong {
  color: #667eea;
  font-weight: 600;
}

.filter-options {
  display: flex;
  gap: 12px;
}

.sort-select, .board-select {
  width: 140px;
}

/* 文章列表 */
.article-list {
  min-height: 400px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.article-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(255, 255, 255, 0.5);
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
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
  transform: translateY(-6px);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 16px 40px rgba(102, 126, 234, 0.15);
  border-color: #a18cd1;
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
  line-clamp: 2;
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
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 关键词高亮 */
:deep(.highlight) {
  background: linear-gradient(135deg, #fff3cd, #ffe69c);
  color: #856404;
  padding: 2px 4px;
  border-radius: 4px;
  font-weight: 600;
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
  background: linear-gradient(135deg, #667eea, #764ba2);
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

/* 骨架屏样式 */
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.skeleton-content {
  display: flex;
  gap: 24px;
}

.skeleton-main {
  flex: 1;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  width: fit-content;
  margin-left: auto;
  margin-right: auto;
}

/* 搜索提示 */
.search-tips {
  max-width: 600px;
  margin: 0 auto;
}

.tips-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  animation: fadeInUp 0.6s ease-out;
}

.tips-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 16px;
  color: #2c3e50;
}

.tips-list {
  margin: 0;
  padding-left: 20px;
  color: #606266;
  line-height: 2;
}

.tips-list kbd {
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 12px;
  color: #606266;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .search-banner {
    padding: 30px 20px;
  }

  .banner-title {
    font-size: 28px;
  }

  .search-box {
    flex-direction: column;
  }

  .search-btn {
    width: 100%;
  }

  .results-header {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-options {
    flex-direction: column;
  }

  .sort-select, .board-select {
    width: 100%;
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

  .meta-stats {
    margin-left: 0;
    margin-top: 8px;
    width: 100%;
  }
}
</style>
