<template>
  <div class="shorts-page">
    <!-- 返回导航 -->
    <div class="shorts-header">
      <button class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <div class="shorts-logo">
        <el-icon class="shorts-icon"><Film /></el-icon>
        <span>Shorts</span>
      </div>
    </div>

    <!-- Shorts 内容 -->
    <div class="shorts-container">
      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
        <p>加载中...</p>
      </div>

      <div v-else-if="videos.length === 0" class="empty-state">
        <el-icon :size="64"><VideoCamera /></el-icon>
        <p>暂无短视频</p>
      </div>

      <div v-else class="shorts-feed">
        <div 
          v-for="(video, index) in videos" 
          :key="video.id" 
          class="short-card"
          :class="{ active: currentIndex === index }"
          @click="playVideo(video)"
        >
          <div class="short-thumbnail">
            <img :src="video.thumbnail || defaultThumbnail" :alt="video.title" />
            <div class="short-duration">{{ video.duration || '0:00' }}</div>
            <div class="short-overlay">
              <el-icon class="play-icon"><VideoPlay /></el-icon>
            </div>
          </div>
          <div class="short-info">
            <h3 class="short-title">{{ video.title }}</h3>
            <div class="short-meta">
              <span>{{ formatViews(video.viewCount) }}次观看</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > pageSize" class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="loadVideos"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Film, Loading, VideoCamera, VideoPlay } from '@element-plus/icons-vue'
import { getVideoList } from '@/api/video'

const router = useRouter()
const defaultThumbnail = 'https://picsum.photos/seed/default/270/480'

// 状态
const loading = ref(false)
const videos = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const currentIndex = ref(-1)

// 加载短视频列表
const loadVideos = async () => {
  loading.value = true
  try {
    const res = await getVideoList({
      current: currentPage.value,
      size: pageSize.value,
      maxDuration: 60 // 短视频：60秒以内
    })
    videos.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('加载短视频失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 播放视频
const playVideo = (video) => {
  router.push(`/video/${video.id}`)
}

// 返回
const goBack = () => {
  router.push('/video')
}

// 格式化播放量
const formatViews = (num) => {
  if (!num) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return num.toLocaleString()
}

onMounted(() => {
  loadVideos()
})
</script>

<style scoped>
.shorts-page {
  min-height: calc(100vh - 60px);
  background: #ffffff;
}

.shorts-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  border-bottom: 1px solid rgba(0,0,0,0.1);
  position: sticky;
  top: 0;
  background: #ffffff;
  z-index: 100;
}

.back-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.back-btn:hover {
  background: #f2f2f2;
}

.back-btn .el-icon {
  font-size: 24px;
  color: #0f0f0f;
}

.shorts-logo {
  display: flex;
  align-items: center;
  gap: 8px;
}

.shorts-icon {
  font-size: 28px;
  color: #ff0000;
}

.shorts-logo span {
  font-size: 20px;
  font-weight: 600;
  color: #0f0f0f;
}

.shorts-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: #606060;
}

.empty-state p,
.loading-state p {
  margin-top: 16px;
  font-size: 14px;
}

.shorts-feed {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

@media (max-width: 1200px) {
  .shorts-feed { grid-template-columns: repeat(4, 1fr); }
}

@media (max-width: 900px) {
  .shorts-feed { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 600px) {
  .shorts-feed { grid-template-columns: repeat(2, 1fr); }
}

.short-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.short-card:hover {
  transform: translateY(-4px);
}

.short-thumbnail {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  aspect-ratio: 9 / 16;
  background: #f2f2f2;
}

.short-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.short-duration {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0,0,0,0.8);
  color: #fff;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.short-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.short-card:hover .short-overlay {
  opacity: 1;
}

.play-icon {
  font-size: 48px;
  color: #ffffff;
}

.short-info {
  padding: 8px 4px;
}

.short-title {
  font-size: 14px;
  font-weight: 500;
  color: #0f0f0f;
  margin: 0 0 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.3;
}

.short-meta {
  font-size: 12px;
  color: #606060;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}
</style>
