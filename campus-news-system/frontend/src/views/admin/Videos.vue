<template>
  <div class="videos-page">
    <el-card class="glass-card">
      <template #header>
        <div class="header">
          <div class="header-left">
            <el-icon class="header-icon"><VideoCamera /></el-icon>
            <span class="header-title">视频管理</span>
            <el-tag type="info" size="small" class="total-tag">共 {{ total }} 个视频</el-tag>
          </div>
          <div class="filters">
            <el-select v-model="categoryId" placeholder="视频分类" clearable @change="fetchVideos" class="filter-select">
              <el-option 
                v-for="cat in categories" 
                :key="cat.id" 
                :label="cat.name" 
                :value="cat.id" 
              />
            </el-select>
            <el-select v-model="isApproved" placeholder="审核状态" clearable @change="fetchVideos" class="filter-select">
              <el-option label="待审核" :value="0">
                <el-icon class="option-icon" style="color: #e6a23c;"><Clock /></el-icon>
                <span>待审核</span>
              </el-option>
              <el-option label="已通过" :value="1">
                <el-icon class="option-icon" style="color: #67c23a;"><CircleCheck /></el-icon>
                <span>已通过</span>
              </el-option>
              <el-option label="已拒绝" :value="2">
                <el-icon class="option-icon" style="color: #f56c6c;"><CircleClose /></el-icon>
                <span>已拒绝</span>
              </el-option>
            </el-select>
            <el-input
              v-model="searchKeyword"
              placeholder="搜索视频标题..."
              class="search-input"
              clearable
              @change="fetchVideos"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
        </div>
      </template>
      
      <!-- 待审核提示 -->
      <el-alert 
        v-if="pendingCount > 0"
        :title="`有 ${pendingCount} 个视频待审核`"
        type="warning"
        show-icon
        :closable="false"
        class="pending-alert"
      />
      
      <el-table :data="videos" v-loading="loading" stripe class="video-table">
        <el-table-column label="视频信息" min-width="320">
          <template #default="{ row }">
            <div class="video-info">
              <div class="thumbnail-wrapper" @click="handleViewDetail(row.id)">
                <el-image 
                  :src="row.thumbnail || '/default-video.png'" 
                  fit="cover"
                  class="video-thumbnail"
                >
                  <template #error>
                    <div class="thumbnail-placeholder">
                      <el-icon><VideoCamera /></el-icon>
                    </div>
                  </template>
                </el-image>
                <div class="duration-badge">{{ formatDuration(row.durationSeconds) }}</div>
                <div class="play-overlay">
                  <el-icon><VideoPlay /></el-icon>
                </div>
              </div>
              <div class="video-meta">
                <div class="video-title" @click="handleViewDetail(row.id)">{{ row.title }}</div>
                <div class="video-author">
                  <el-icon><User /></el-icon>
                  {{ row.author?.realName || row.channelName || '未知作者' }}
                </div>
                <div class="video-stats">
                  <span><el-icon><View /></el-icon> {{ row.viewCount || 0 }}</span>
                  <span><el-icon><Star /></el-icon> {{ row.likeCount || 0 }}</span>
                  <span><el-icon><ChatDotRound /></el-icon> {{ row.commentCount || 0 }}</span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">
              {{ row.categoryName || '未分类' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getApproveTag(row.isApproved)" size="default" effect="dark" class="status-tag">
              <el-icon class="status-icon">
                <Clock v-if="row.isApproved === 0" />
                <CircleCheck v-else-if="row.isApproved === 1" />
                <CircleClose v-else />
              </el-icon>
              {{ getApproveName(row.isApproved) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上传时间" width="160" align="center">
          <template #default="{ row }">
            <div class="time-info">
              <div>{{ formatDate(row.createdAt) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button 
                size="small" 
                type="primary"
                @click="handleViewDetail(row.id)" 
                class="action-btn view-btn"
              >
                <el-icon><VideoPlay /></el-icon>
                预览
              </el-button>
              <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, row)">
                <el-button size="small" class="action-btn more-btn">
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="approve" v-if="row.isApproved !== 1">
                      <el-icon style="color: #67c23a;"><CircleCheck /></el-icon>
                      通过审核
                    </el-dropdown-item>
                    <el-dropdown-item command="reject" v-if="row.isApproved !== 2">
                      <el-icon style="color: #e6a23c;"><Warning /></el-icon>
                      拒绝发布
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided>
                      <el-icon style="color: #f56c6c;"><Delete /></el-icon>
                      删除视频
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchVideos"
          @size-change="fetchVideos"
          background
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getVideoList, getVideoCategories, approveVideo, deleteVideo } from '@/api/video'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  VideoCamera, VideoPlay, User, View, Star, ChatDotRound,
  Clock, CircleCheck, CircleClose, Search, MoreFilled, Warning, Delete
} from '@element-plus/icons-vue'

const loading = ref(false)
const videos = ref([])
const categories = ref([])
const searchKeyword = ref('')
const categoryId = ref(null)
const isApproved = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const pendingCount = ref(0)

const fetchCategories = async () => {
  try {
    const data = await getVideoCategories()
    categories.value = data
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

const fetchVideos = async () => {
  loading.value = true
  try {
    const data = await getVideoList({
      current: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      categoryId: categoryId.value || undefined,
      isApproved: isApproved.value,
      showAll: true // 管理后台显示所有状态的视频
    })
    videos.value = data.records
    total.value = data.total
    
    // 获取待审核数量
    const pendingData = await getVideoList({ current: 1, size: 1, isApproved: 0, showAll: true })
    pendingCount.value = pendingData.total
  } catch (error) {
    console.error('获取视频列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleCommand = (command, row) => {
  switch (command) {
    case 'approve':
      handleApprove(row.id, 1)
      break
    case 'reject':
      handleApprove(row.id, 2)
      break
    case 'delete':
      handleDelete(row.id)
      break
  }
}

const handleApprove = async (id, status) => {
  const statusText = status === 1 ? '通过' : '拒绝'
  const statusType = status === 1 ? 'success' : 'warning'
  try {
    await ElMessageBox.confirm(
      `确定要${statusText}这个视频吗？${status === 1 ? '通过后将在前台展示。' : '拒绝后用户将收到通知。'}`, 
      '审核确认', 
      {
        confirmButtonText: `确定${statusText}`,
        cancelButtonText: '取消',
        type: statusType
      }
    )
    await approveVideo(id, status)
    ElMessage.success(`已${statusText}`)
    fetchVideos()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('审核失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个视频吗？此操作不可恢复！', 
      '删除确认', 
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error',
        confirmButtonClass: 'el-button--danger'
      }
    )
    await deleteVideo(id)
    ElMessage.success('删除成功')
    fetchVideos()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleViewDetail = (id) => {
  window.open(`/video/${id}`, '_blank')
}

const formatDuration = (seconds) => {
  if (!seconds) return '00:00'
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getApproveName = (status) => {
  const names = { 0: '待审核', 1: '已通过', 2: '已拒绝' }
  return names[status] ?? '待审核'
}

const getApproveTag = (status) => {
  const tags = { 0: 'warning', 1: 'success', 2: 'danger' }
  return tags[status] ?? 'warning'
}

onMounted(() => {
  fetchCategories()
  fetchVideos()
})
</script>

<style scoped>
.videos-page {
  padding: 20px;
}

/* 玻璃卡片 */
.glass-card {
  background: rgba(255, 255, 255, 0.85) !important;
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.6) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  border-radius: 16px;
}

/* 头部样式 */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  font-size: 24px;
  color: #409eff;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.total-tag {
  margin-left: 8px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-select {
  width: 130px;
}

.search-input {
  width: 200px;
}

.option-icon {
  margin-right: 6px;
  vertical-align: middle;
}

/* 待审核提示 */
.pending-alert {
  margin-bottom: 16px;
  border-radius: 8px;
}

/* 表格样式 */
.video-table {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.el-table) {
  background: transparent !important;
  --el-table-tr-bg-color: transparent;
}

:deep(.el-table th.el-table__cell) {
  background: rgba(245, 247, 250, 0.8) !important;
  font-weight: 600;
  color: #606266;
}

:deep(.el-table tr) {
  background: transparent !important;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: rgba(250, 250, 252, 0.6);
}

:deep(.el-table--enable-row-hover .el-table__body tr:hover > td.el-table__cell) {
  background: rgba(64, 158, 255, 0.08) !important;
}

/* 视频信息样式 */
.video-info {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding: 8px 0;
}

.thumbnail-wrapper {
  position: relative;
  width: 140px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.video-thumbnail {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnail-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 28px;
}

.duration-badge {
  position: absolute;
  bottom: 4px;
  right: 4px;
  background: rgba(0, 0, 0, 0.75);
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.play-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  color: white;
  font-size: 32px;
}

.thumbnail-wrapper:hover .play-overlay {
  opacity: 1;
}

.video-meta {
  flex: 1;
  min-width: 0;
}

.video-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
  cursor: pointer;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
  transition: color 0.3s;
}

.video-title:hover {
  color: #409eff;
}

.video-author {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.video-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.video-stats span {
  display: flex;
  align-items: center;
  gap: 3px;
}

/* 状态标签 */
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 20px;
}

.status-icon {
  font-size: 14px;
}

/* 时间信息 */
.time-info {
  font-size: 13px;
  color: #606266;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.action-btn {
  border-radius: 8px;
}

.view-btn {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%) !important;
  border: none !important;
}

.view-btn:hover {
  background: linear-gradient(135deg, #66b1ff 0%, #409eff 100%) !important;
  transform: translateY(-1px);
}

.more-btn {
  background: rgba(144, 147, 153, 0.1) !important;
  border: 1px solid rgba(144, 147, 153, 0.2) !important;
  color: #606266 !important;
}

.more-btn:hover {
  background: rgba(144, 147, 153, 0.2) !important;
}

/* 分页 */
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
}
</style>
