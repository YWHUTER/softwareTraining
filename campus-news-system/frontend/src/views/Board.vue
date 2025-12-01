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
                  <span><el-icon><User /></el-icon> {{ article.author?.realName }}</span>
                  <span v-if="article.college"><el-icon><School /></el-icon> {{ article.college?.name }}</span>
                  <span><el-icon><View /></el-icon> {{ article.viewCount }}</span>
                  <span><el-icon><ChatDotRound /></el-icon> {{ article.commentCount }}</span>
                  <span><el-icon><Clock /></el-icon> {{ formatTime(article.createdAt) }}</span>
                </div>
              </div>
              <div v-if="article.coverImage" class="article-cover">
                <el-image :src="article.coverImage" fit="cover" />
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

watch(boardType, () => {
  currentPage.value = 1
  fetchArticles()
})

onMounted(() => {
  fetchArticles()
})
</script>

<style scoped>
.board-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.board-page h2 {
  margin: 0;
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
}

.article-card {
  margin-bottom: 15px;
  cursor: pointer;
  transition: transform 0.3s;
}

.article-card:hover {
  transform: translateY(-2px);
}

.article-content {
  display: flex;
  gap: 20px;
}

.article-main {
  flex: 1;
}

.article-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.article-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.article-summary {
  color: #606266;
  margin-bottom: 10px;
  line-height: 1.6;
}

.article-meta {
  display: flex;
  gap: 15px;
  color: #909399;
  font-size: 14px;
}

.article-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.article-cover {
  width: 160px;
  height: 120px;
  flex-shrink: 0;
}

.article-cover .el-image {
  width: 100%;
  height: 100%;
  border-radius: 4px;
}
</style>
