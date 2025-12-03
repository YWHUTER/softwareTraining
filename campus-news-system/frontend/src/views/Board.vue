<template>
  <div class="board-page">
    <el-card>
      <template #header>
        <div class="board-header">
          <h2>{{ boardTitle }}</h2>
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
      </template>
      
      <div class="board-content" v-loading="loading">
        <el-empty v-if="articles.length === 0 && !loading" description="暂无文章" />
        
        <div class="article-list">
          <el-card
            v-for="article in articles"
            :key="article.id"
            class="article-card"
            shadow="hover"
            @click="goToDetail(article.id)"
          >
            <div class="article-content">
              <div class="article-main">
                <div class="article-header">
                  <el-tag v-if="article.isPinned" type="danger" size="small">置顶</el-tag>
                  <span class="article-title">{{ article.title }}</span>
                </div>
                <div class="article-summary">{{ article.summary || article.content?.substring(0, 100) + '...' }}</div>
                <div class="article-meta">
                  <div class="author-info">
                    <el-avatar :size="22" :src="getValidAvatar(article.author?.avatar)" class="author-avatar" fit="cover">
                      {{ article.author?.realName?.[0] }}
                    </el-avatar>
                    <span class="author-name-text">{{ article.author?.realName }}</span>
                  </div>
                  <span v-if="article.college"><el-icon><School /></el-icon> {{ article.college?.name }}</span>
                  <span><el-icon><View /></el-icon> {{ article.viewCount }}</span>
                  <span><el-icon><ChatDotRound /></el-icon> {{ article.commentCount }}</span>
                </div>
              </div>
              <div class="article-right">
                <div v-if="article.coverImage" class="article-cover">
                  <el-image :src="article.coverImage" fit="cover" />
                </div>
                <div class="publish-time-badge">
                  <el-icon><Clock /></el-icon>
                  <span>{{ getRelativeTime(article.createdAt) }}</span>
                </div>
              </div>
            </div>
            
            <!-- 热门评论 -->
            <div v-if="article.hotComment" class="hot-comment" @click.stop>
              <div class="hot-comment-header">
                <el-icon color="#f56c6c"><ChatLineSquare /></el-icon>
                <span class="hot-comment-label">热评</span>
              </div>
              <div class="hot-comment-content">
                <el-avatar :size="20" class="comment-avatar" :src="getValidAvatar(article.hotComment.user?.avatar)" fit="cover">
                  {{ article.hotComment.user?.realName?.[0] }}
                </el-avatar>
                <span class="comment-author">{{ article.hotComment.user?.realName }}：</span>
                <span class="comment-text">{{ article.hotComment.content }}</span>
                <span class="comment-likes">
                  <el-icon><Star /></el-icon>
                  {{ article.hotComment.likeCount || 0 }}
                </span>
              </div>
            </div>
          </el-card>
        </div>
        
        <el-pagination
          v-if="total > 0"
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchArticles"
          style="margin-top: 20px; justify-content: center;"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArticleList } from '@/api/article'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const articles = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const sortBy = ref('date_desc') // 默认按日期降序

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
    // 解析排序参数
    const [sortField, sortOrder] = sortBy.value.split('_')
    const data = await getArticleList({
      current: currentPage.value,
      size: pageSize.value,
      boardType: boardType.value,
      isApproved: 1,
      sortBy: sortField === 'views' ? 'views' : 'date',
      sortOrder: sortOrder
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

const formatTime = (time) => {
  return new Date(time).toLocaleString('zh-CN')
}

// 获取相对时间
const getRelativeTime = (time) => {
  const now = new Date()
  const past = new Date(time)
  const diff = now - past
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  if (days < 365) return `${Math.floor(days / 30)}个月前`
  return `${Math.floor(days / 365)}年前`
}

// 验证头像URL是否有效
const getValidAvatar = (avatar) => {
  if (!avatar) return undefined
  // 只接受以 /api/file 开头的有效头像URL
  if (avatar.startsWith('/api/file/')) return avatar
  // 其他URL视为无效
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
  max-width: 1200px;
  margin: 0 auto;
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.board-page > .el-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  animation: slideUp 0.5s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.board-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.board-page h2 {
  margin: 0;
  font-size: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.sort-select {
  width: 180px;
}

.sort-select :deep(.el-select-dropdown__item) {
  display: flex;
  align-items: center;
  gap: 8px;
}

.article-list {
  min-height: 400px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.article-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 12px;
  border: none;
  overflow: hidden;
  position: relative;
  animation: fadeInUp 0.4s ease-out both;
}

.article-card:nth-child(1) { animation-delay: 0.05s; }
.article-card:nth-child(2) { animation-delay: 0.1s; }
.article-card:nth-child(3) { animation-delay: 0.15s; }
.article-card:nth-child(4) { animation-delay: 0.2s; }
.article-card:nth-child(5) { animation-delay: 0.25s; }

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

/* 交替背景色 */
.article-card:nth-child(odd) {
  background: linear-gradient(135deg, #f8f9ff 0%, #ffffff 100%);
}

.article-card:nth-child(even) {
  background: linear-gradient(135deg, #fff8f6 0%, #ffffff 100%);
}

.article-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.article-card:nth-child(even)::before {
  background: linear-gradient(180deg, #f093fb 0%, #f5576c 100%);
}

.article-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 30px rgba(102, 126, 234, 0.15);
}

.article-card:hover::before {
  opacity: 1;
}

.article-content {
  display: flex;
  gap: 20px;
  padding: 4px;
}

.article-main {
  flex: 1;
}

.article-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.article-title {
  font-size: 18px;
  font-weight: 700;
  color: #2c3e50;
  transition: color 0.3s ease;
}

.article-card:hover .article-title {
  color: #667eea;
}

.article-summary {
  color: #606266;
  margin-bottom: 12px;
  line-height: 1.7;
  font-size: 14px;
}

.article-meta {
  display: flex;
  gap: 16px;
  color: #909399;
  font-size: 13px;
  flex-wrap: wrap;
}

.article-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 20px;
  transition: all 0.2s ease;
}

.article-meta span:hover {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 4px;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 20px;
}

.author-info:hover {
  background: rgba(102, 126, 234, 0.1);
}

.author-avatar {
  flex-shrink: 0;
  width: 22px !important;
  height: 22px !important;
  min-width: 22px !important;
  min-height: 22px !important;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 10px;
  font-weight: 600;
  overflow: hidden;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
}

.author-avatar :deep(img) {
  width: 22px !important;
  height: 22px !important;
  min-width: 22px !important;
  min-height: 22px !important;
  object-fit: cover !important;
  display: block !important;
}

.author-name-text {
  font-size: 13px;
  color: #606266;
}

.article-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  flex-shrink: 0;
}

.article-cover {
  width: 160px;
  height: 110px;
  flex-shrink: 0;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.article-cover .el-image {
  width: 100%;
  height: 100%;
  transition: transform 0.3s ease;
}

.article-card:hover .article-cover .el-image {
  transform: scale(1.05);
}

.publish-time-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  background: #f5f7fa;
  color: #909399;
  border-radius: 6px;
  font-size: 12px;
  white-space: nowrap;
}

/* 热门评论样式 */
.hot-comment {
  margin-top: 12px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #fff5f5 0%, #fef5f0 100%);
  border-radius: 8px;
  border-left: 3px solid #f56c6c;
}

.hot-comment-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
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
  max-width: 400px;
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
</style>
