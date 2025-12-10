<template>
  <div class="comments-page">
    <!-- 顶部控制栏 -->
    <div 
      class="control-panel"
      v-motion
      :initial="{ opacity: 0, y: -20 }"
      :enter="{ opacity: 1, y: 0, transition: { type: 'spring', stiffness: 250, damping: 25 } }"
    >
      <div class="panel-header">
        <div class="title-section">
          <div class="icon-box">
            <el-icon :size="24"><ChatLineRound /></el-icon>
          </div>
          <div class="title-text">
            <h2>互动评论</h2>
            <span class="subtitle">管理您的所有互动记录</span>
          </div>
        </div>
        <div class="stats-badge">
          共 <span class="count">{{ total }}</span> 条记录
        </div>
      </div>

      <div class="panel-filters">
        <div class="filter-group">
          <el-radio-group v-model="type" @change="handleTypeChange" class="custom-tabs">
            <el-radio-button label="received">
              <div class="tab-content">
                <el-icon><Message /></el-icon>
                <span>收到的评论</span>
              </div>
            </el-radio-button>
            <el-radio-button label="sent">
              <div class="tab-content">
                <el-icon><Promotion /></el-icon>
                <span>我发出的评论</span>
              </div>
            </el-radio-button>
          </el-radio-group>
        </div>

        <div class="filter-group">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            unlink-panels
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            @change="handleDateChange"
            class="custom-date-picker"
            :prefix-icon="Calendar"
          />
        </div>
      </div>
    </div>

    <!-- 评论列表区域 -->
    <div class="list-container">
      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="3" animated class="skeleton-item" v-for="i in 3" :key="i" />
      </div>

      <el-empty 
        v-else-if="comments.length === 0" 
        description="暂无互动记录" 
        class="glass-empty"
      >
        <template #image>
          <el-icon :size="60" class="empty-icon"><ChatDotSquare /></el-icon>
        </template>
      </el-empty>

      <div v-else class="comment-grid">
        <div
          v-for="(item, index) in comments"
          :key="item.id"
          class="comment-card"
          v-motion
          :initial="{ opacity: 0, y: 30 }"
          :enter="{ opacity: 1, y: 0, transition: { delay: index * 50 } }"
        >
          <!-- 卡片头部：用户信息 -->
          <div class="card-header">
            <div class="user-info">
              <el-avatar 
                :size="40" 
                class="user-avatar"
                :class="{ 'sent-avatar': type === 'sent' }"
              >
                {{ getUserName(displayCounterparty(item)).charAt(0).toUpperCase() }}
              </el-avatar>
              <div class="user-meta">
                <div class="user-name">
                  {{ getUserName(displayCounterparty(item)) }}
                  <el-tag size="small" :type="type === 'received' ? 'success' : 'primary'" effect="plain" round class="role-tag">
                    {{ type === 'received' ? '来自' : '发送给' }}
                  </el-tag>
                </div>
                <div class="time-meta">
                  <el-icon><Timer /></el-icon>
                  {{ formatTime(item.createdAt) }}
                </div>
              </div>
            </div>
          </div>

          <!-- 卡片内容：评论正文 -->
          <div class="card-content">
            <div class="quote-icon">
              <el-icon><ChatLineSquare /></el-icon>
            </div>
            <div class="text">{{ item.content }}</div>
          </div>

          <!-- 卡片底部：来源文章 -->
          <div class="card-footer" @click="goArticle(item.article?.id)">
            <div class="article-link">
              <div class="link-icon">
                <el-icon><Document /></el-icon>
              </div>
              <span class="article-title">{{ item.article?.title || '未知文章' }}</span>
              <el-icon class="arrow-icon"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          layout="total, prev, pager, next, jumper"
          :total="total"
          @current-change="fetchComments"
          @size-change="fetchComments"
          background
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCommentHistory } from '@/api/comment'
import { 
  ChatLineRound, Message, Promotion, Timer, 
  Document, ArrowRight, ChatDotSquare, ChatLineSquare, Calendar
} from '@element-plus/icons-vue'

const router = useRouter()

const loading = ref(false)
const comments = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12) // 改为12，适应网格布局
const type = ref('received')
const dateRange = ref([])

const fetchComments = async () => {
  loading.value = true
  try {
    const [start, end] = dateRange.value || []
    const data = await getCommentHistory({
      current: currentPage.value,
      size: pageSize.value,
      type: type.value,
      startDate: start,
      endDate: end,
      _t: Date.now()
    })
    comments.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleTypeChange = () => {
  currentPage.value = 1
  fetchComments()
}

const handleDateChange = () => {
  currentPage.value = 1
  fetchComments()
}

const goArticle = (id) => {
  if (!id) return
  router.push(`/article/${id}`)
}

const getUserName = (user) => {
  if (!user) return '未知用户'
  return user.realName || user.username || '未知用户'
}

const displayCounterparty = (row) => {
  return type.value === 'received' ? row.user : row.replyToUser || row.user
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  fetchComments()
})
</script>

<style scoped>
.comments-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: 80vh;
}

/* 顶部控制面板 */
.control-panel {
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border-radius: 24px;
  padding: 24px;
  margin-bottom: 24px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.icon-box {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.title-text h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #2c3e50;
  line-height: 1.2;
}

.subtitle {
  font-size: 14px;
  color: #606266;
  opacity: 0.8;
}

.stats-badge {
  background: rgba(255, 255, 255, 0.6);
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.count {
  color: #667eea;
  font-weight: 700;
  font-size: 16px;
}

.panel-filters {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

/* 自定义 Tabs 样式 */
.custom-tabs :deep(.el-radio-button__inner) {
  border: none !important;
  background: transparent !important;
  padding: 10px 24px !important;
  border-radius: 12px !important;
  color: #606266;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: none !important;
}

.custom-tabs :deep(.el-radio-button.is-active .el-radio-button__inner) {
  background: white !important;
  color: #667eea !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05) !important;
}

.tab-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 自定义日期选择器 */
.custom-date-picker :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.5) !important;
  border: none !important;
  border-radius: 12px !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05) !important;
  padding: 8px 16px !important;
}

.custom-date-picker :deep(.el-input__wrapper:hover) {
  background: white !important;
}

/* 列表区域 */
.comment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

.comment-card {
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(12px) saturate(150%);
  -webkit-backdrop-filter: blur(12px) saturate(150%);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.02);
}

.comment-card:hover {
  transform: translateY(-4px);
  background: rgba(255, 255, 255, 0.65);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08), 
              0 0 0 1px rgba(255, 255, 255, 0.6) inset;
}

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.user-info {
  display: flex;
  gap: 12px;
  align-items: center;
}

.user-avatar {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #5e6d82;
  font-weight: 700;
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.sent-avatar {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-name {
  font-weight: 600;
  color: #2c3e50;
  font-size: 15px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.role-tag {
  height: 20px;
  padding: 0 6px;
  font-size: 10px;
}

.time-meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 卡片内容 */
.card-content {
  background: rgba(255, 255, 255, 0.4);
  border-radius: 12px;
  padding: 12px 16px;
  position: relative;
  flex: 1;
}

.quote-icon {
  position: absolute;
  top: -10px;
  left: 12px;
  background: white;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #667eea;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.text {
  font-size: 14px;
  color: #4a5568;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

/* 卡片底部 */
.card-footer {
  border-top: 1px solid rgba(0, 0, 0, 0.03);
  padding-top: 12px;
}

.article-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.3);
  cursor: pointer;
  transition: all 0.3s ease;
}

.article-link:hover {
  background: rgba(102, 126, 234, 0.1);
}

.link-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #ecf5ff;
  color: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.article-title {
  flex: 1;
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow-icon {
  color: #909399;
  transition: transform 0.3s;
}

.article-link:hover .arrow-icon {
  transform: translateX(4px);
  color: #667eea;
}

.article-link:hover .article-title {
  color: #667eea;
}

/* 空状态 */
.glass-empty {
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 60px 0;
}

.empty-icon {
  color: #909399;
  opacity: 0.5;
}

/* 分页 */
.pagination-wrapper {
  margin-top: 30px;
  display: flex;
  justify-content: center;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
  padding: 16px;
  border-radius: 16px;
  width: fit-content;
  margin-left: auto;
  margin-right: auto;
}

/* 骨架屏 */
.skeleton-item {
  background: rgba(255, 255, 255, 0.3);
  padding: 20px;
  border-radius: 16px;
  margin-bottom: 16px;
}

/* 响应式 */
@media (max-width: 768px) {
  .comments-page {
    padding: 12px;
  }

  .control-panel {
    padding: 16px;
    gap: 16px;
  }
  
  .panel-filters {
    flex-direction: column;
    align-items: stretch;
  }

  .custom-tabs {
    width: 100%;
  }

  .custom-tabs :deep(.el-radio-button) {
    flex: 1;
    display: flex;
  }

  .custom-tabs :deep(.el-radio-button__inner) {
    width: 100%;
    justify-content: center;
  }

  .custom-date-picker {
    width: 100% !important;
  }

  .comment-grid {
    grid-template-columns: 1fr;
  }
}
</style>

