<template>
  <el-dialog
    v-model="visible"
    title="选择文章"
    width="800px"
    :before-close="handleClose"
  >
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索文章标题..."
        clearable
        @input="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      
      <el-select v-model="selectedBoard" placeholder="选择版块" @change="fetchArticles">
        <el-option label="全部" value="" />
        <el-option label="官方新闻" value="OFFICIAL" />
        <el-option label="校园新闻" value="CAMPUS" />
        <el-option label="学院新闻" value="COLLEGE" />
      </el-select>
    </div>

    <!-- 文章列表 -->
    <div class="article-list" v-loading="loading">
      <div v-if="filteredArticles.length === 0" class="empty-tip">
        <el-empty description="暂无文章" />
      </div>
      
      <div
        v-for="article in filteredArticles"
        :key="article.id"
        class="article-item"
        @click="selectArticle(article)"
      >
        <div class="article-header">
          <h3 class="article-title">{{ article.title }}</h3>
          <el-tag size="small" :type="getBoardType(article.boardType)">
            {{ getBoardName(article.boardType) }}
          </el-tag>
        </div>
        
        <p class="article-summary">{{ article.summary || '暂无摘要' }}</p>
        
        <div class="article-meta">
          <span class="meta-item">
            <el-icon><User /></el-icon>
            {{ article.authorName || '匿名' }}
          </span>
          <span class="meta-item">
            <el-icon><View /></el-icon>
            {{ article.viewCount || 0 }}
          </span>
          <span class="meta-item">
            <el-icon><Clock /></el-icon>
            {{ formatTime(article.createdAt) }}
          </span>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[5, 10, 20]"
        layout="total, sizes, prev, pager, next"
        @current-change="fetchArticles"
        @size-change="fetchArticles"
      />
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, User, View, Clock } from '@element-plus/icons-vue'
import { getArticleList } from '@/api/article'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'select'])

// 状态
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const articles = ref([])
const searchKeyword = ref('')
const selectedBoard = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 过滤后的文章列表
const filteredArticles = computed(() => {
  if (!searchKeyword.value) return articles.value
  
  return articles.value.filter(article =>
    article.title.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
    (article.summary && article.summary.toLowerCase().includes(searchKeyword.value.toLowerCase()))
  )
})

// 获取文章列表
const fetchArticles = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      boardType: selectedBoard.value || undefined
    }
    
    const result = await getArticleList(params)
    articles.value = result.records || []
    total.value = result.total || 0
  } catch (error) {
    console.error('获取文章列表失败:', error)
    ElMessage.error('获取文章列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索处理
const handleSearch = () => {
  // 本地过滤，不需要重新请求
}

// 选择文章
const selectArticle = (article) => {
  emit('select', article)
  handleClose()
}

// 关闭对话框
const handleClose = () => {
  visible.value = false
  searchKeyword.value = ''
  currentPage.value = 1
}

// 获取版块名称
const getBoardName = (boardType) => {
  const boardMap = {
    OFFICIAL: '官方新闻',
    CAMPUS: '校园新闻',
    COLLEGE: '学院新闻'
  }
  return boardMap[boardType] || '未知'
}

// 获取版块类型
const getBoardType = (boardType) => {
  const typeMap = {
    OFFICIAL: 'danger',
    CAMPUS: 'warning',
    COLLEGE: 'success'
  }
  return typeMap[boardType] || 'info'
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleDateString('zh-CN')
}

// 监听对话框打开
watch(visible, (val) => {
  if (val) {
    fetchArticles()
  }
})
</script>

<style scoped>
/* 对话框整体样式优化 */
:deep(.el-dialog) {
  border-radius: 16px !important;
  overflow: hidden;
}

:deep(.el-dialog__header) {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
  border-bottom: 1px solid rgba(102, 126, 234, 0.2);
  padding: 20px;
}

:deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

:deep(.el-dialog__body) {
  background: rgba(255, 255, 255, 0.95);
  padding: 25px;
}

.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.search-bar .el-input {
  flex: 1;
}

/* 搜索框玻璃特效 */
.search-bar :deep(.el-input__inner) {
  background: rgba(255, 255, 255, 0.7);
  border: 2px solid rgba(220, 223, 230, 0.8);
  font-weight: 500;
}

.search-bar :deep(.el-input__inner:focus) {
  background: rgba(255, 255, 255, 0.9);
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

/* 选择框玻璃特效 */
.search-bar :deep(.el-select .el-input__inner) {
  background: rgba(255, 255, 255, 0.7);
  border: 2px solid rgba(220, 223, 230, 0.8);
  font-weight: 500;
}

.search-bar :deep(.el-select .el-input__inner:focus) {
  background: rgba(255, 255, 255, 0.9);
  border-color: #667eea;
}

.article-list {
  max-height: 400px;
  overflow-y: auto;
  padding: 10px 0;
}

.article-item {
  padding: 15px;
  margin-bottom: 10px;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(10px) saturate(150%);
  -webkit-backdrop-filter: blur(10px) saturate(150%);
  border: 2px solid rgba(255, 255, 255, 0.6);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04),
              0 0 0 1px rgba(255, 255, 255, 0.3) inset;
}

.article-item:hover {
  border-color: rgba(102, 126, 234, 0.3);
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.2),
              0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  transform: translateY(-2px);
}

.article-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.article-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.article-summary {
  margin: 10px 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2; /* 标准属性 */
  -webkit-box-orient: vertical;
}

.article-meta {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #909399;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.empty-tip {
  padding: 40px 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}
</style>
