<template>
  <div class="channel-page">
    <!-- 频道头部 -->
    <div class="channel-header">
      <div class="channel-banner">
        <div class="banner-gradient"></div>
      </div>
      <div class="channel-info-section">
        <div class="channel-avatar-large">
          <img v-if="channelInfo?.user?.avatar" :src="getAvatarUrl(channelInfo.user.avatar)" />
          <span v-else>{{ (channelInfo?.user?.realName || '?')[0] }}</span>
        </div>
        <div class="channel-details">
          <h1 class="channel-name">{{ channelInfo?.user?.realName || '未知频道' }}</h1>
          <div class="channel-stats">
            <span>@{{ channelInfo?.user?.username }}</span>
            <span class="dot">•</span>
            <span>{{ formatNumber(channelInfo?.subscriberCount) }} 位订阅者</span>
            <span class="dot">•</span>
            <span>{{ channelInfo?.videoCount || 0 }} 个视频</span>
          </div>
          <p class="channel-description">{{ channelInfo?.user?.bio || '这个频道还没有简介' }}</p>
        </div>
        <div class="channel-actions">
          <button 
            v-if="!isOwnChannel"
            class="subscribe-btn" 
            :class="{ subscribed: isSubscribed }"
            @click="toggleSubscribe"
          >
            {{ isSubscribed ? '已订阅' : '订阅' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 频道导航 -->
    <div class="channel-tabs">
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'videos' }"
        @click="activeTab = 'videos'"
      >
        视频
      </button>
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'popular' }"
        @click="activeTab = 'popular'"
      >
        热门
      </button>
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'about' }"
        @click="activeTab = 'about'"
      >
        简介
      </button>
    </div>

    <!-- 内容区域 -->
    <div class="channel-content">
      <!-- 视频列表 -->
      <div v-if="activeTab !== 'about'" class="videos-section">
        <div class="sort-bar">
          <span class="sort-label">排序方式:</span>
          <button 
            class="sort-btn" 
            :class="{ active: sortBy === 'date' }"
            @click="changeSortBy('date')"
          >
            最新发布
          </button>
          <button 
            class="sort-btn" 
            :class="{ active: sortBy === 'popular' }"
            @click="changeSortBy('popular')"
          >
            最多观看
          </button>
        </div>

        <div v-if="loading" class="loading-container">
          <el-icon class="is-loading" :size="40"><Loading /></el-icon>
        </div>

        <div v-else-if="videos.length === 0" class="empty-state">
          <el-icon :size="64"><VideoCamera /></el-icon>
          <p>该频道还没有上传视频</p>
        </div>

        <div v-else class="video-grid">
          <div 
            v-for="video in videos" 
            :key="video.id" 
            class="video-card"
            @click="goToVideo(video.id)"
          >
            <div class="video-thumbnail">
              <img :src="video.thumbnail || defaultThumbnail" :alt="video.title" />
              <span class="duration">{{ video.duration || '00:00' }}</span>
            </div>
            <div class="video-info">
              <h3 class="video-title">{{ video.title }}</h3>
              <div class="video-meta">
                <span>{{ formatViews(video.viewCount) }}次观看</span>
                <span class="dot">•</span>
                <span>{{ formatTime(video.createdAt) }}</span>
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

      <!-- 简介 -->
      <div v-else class="about-section">
        <div class="about-card">
          <h3>频道简介</h3>
          <p>{{ channelInfo?.user?.bio || '这个频道还没有简介' }}</p>
        </div>
        <div class="stats-card">
          <h3>统计信息</h3>
          <div class="stat-item">
            <span class="stat-label">总播放量</span>
            <span class="stat-value">{{ formatNumber(channelInfo?.totalViews) }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">视频数量</span>
            <span class="stat-value">{{ channelInfo?.videoCount || 0 }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">订阅者</span>
            <span class="stat-value">{{ formatNumber(channelInfo?.subscriberCount) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, VideoCamera } from '@element-plus/icons-vue'
import { getChannelInfo, getChannelVideos } from '@/api/video'
import { toggleFollow, checkFollowStatus } from '@/api/user'

const route = useRoute()
const router = useRouter()

const defaultThumbnail = 'https://picsum.photos/seed/default/320/180'

// 状态
const channelInfo = ref(null)
const videos = ref([])
const loading = ref(false)
const activeTab = ref('videos')
const sortBy = ref('date')
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const isSubscribed = ref(false)
const currentUser = ref(null)

// 是否是自己的频道
const isOwnChannel = computed(() => {
  if (!currentUser.value || !channelInfo.value?.user) return false
  return currentUser.value.id === channelInfo.value.user.id
})

// 加载频道信息
const loadChannelInfo = async () => {
  try {
    const res = await getChannelInfo(route.params.userId)
    channelInfo.value = res
    loadSubscribeStatus()
  } catch (error) {
    ElMessage.error('加载频道信息失败')
  }
}

// 加载视频列表
const loadVideos = async () => {
  loading.value = true
  try {
    const res = await getChannelVideos(route.params.userId, {
      current: currentPage.value,
      size: pageSize.value,
      sortBy: activeTab.value === 'popular' ? 'popular' : sortBy.value
    })
    videos.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('加载视频失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载订阅状态
const loadSubscribeStatus = async () => {
  if (!currentUser.value || !channelInfo.value?.user?.id) return
  if (currentUser.value.id === channelInfo.value.user.id) return
  
  try {
    const res = await checkFollowStatus(channelInfo.value.user.id)
    isSubscribed.value = res
  } catch (error) {
    console.error('加载订阅状态失败')
  }
}

// 订阅/取消订阅
const toggleSubscribe = async () => {
  if (!currentUser.value) {
    ElMessage.warning('请先登录')
    return
  }
  
  try {
    const res = await toggleFollow(channelInfo.value.user.id)
    isSubscribed.value = res.isFollowing
    channelInfo.value.subscriberCount += res.isFollowing ? 1 : -1
    ElMessage.success(res.message)
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 切换排序
const changeSortBy = (sort) => {
  sortBy.value = sort
  currentPage.value = 1
  loadVideos()
}

// 跳转视频
const goToVideo = (id) => {
  router.push(`/video/${id}`)
}

// 加载当前用户
const loadCurrentUser = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    currentUser.value = JSON.parse(userStr)
  }
}

// 工具函数
const getAvatarUrl = (url) => {
  if (!url) return ''
  return url.startsWith('http') ? url : `http://localhost:8080${url}`
}

const formatNumber = (num) => {
  if (!num) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return num.toLocaleString()
}

const formatViews = (num) => {
  if (!num) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return num.toLocaleString()
}

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / 86400000)
  if (days < 30) return `${days}天前`
  if (days < 365) return `${Math.floor(days / 30)}个月前`
  return `${Math.floor(days / 365)}年前`
}

// 监听tab变化
watch(activeTab, () => {
  if (activeTab.value !== 'about') {
    currentPage.value = 1
    loadVideos()
  }
})

// 监听路由变化
watch(() => route.params.userId, () => {
  if (route.params.userId) {
    loadChannelInfo()
    loadVideos()
  }
})

onMounted(() => {
  loadCurrentUser()
  loadChannelInfo()
  loadVideos()
})
</script>


<style scoped>
.channel-page {
  background: #ffffff;
  min-height: calc(100vh - 60px);
}

/* 频道头部 */
.channel-header {
  position: relative;
}

.channel-banner {
  height: 200px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
}

.banner-gradient {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 80px;
  background: linear-gradient(transparent, rgba(255,255,255,0.8));
}

.channel-info-section {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  transform: translateY(-40px);
}

.channel-avatar-large {
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff0000, #cc0000);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 4px solid #ffffff;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  flex-shrink: 0;
}

.channel-avatar-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.channel-avatar-large span {
  color: #fff;
  font-size: 48px;
  font-weight: 600;
}

.channel-details {
  flex: 1;
  padding-top: 50px;
}

.channel-name {
  font-size: 24px;
  font-weight: 600;
  color: #0f0f0f;
  margin: 0 0 8px;
}

.channel-stats {
  color: #606060;
  font-size: 14px;
  margin-bottom: 8px;
}

.channel-stats .dot {
  margin: 0 6px;
}

.channel-description {
  color: #606060;
  font-size: 14px;
  margin: 0;
  max-width: 600px;
}

.channel-actions {
  padding-top: 50px;
}

.subscribe-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 20px;
  background: #0f0f0f;
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.subscribe-btn:hover {
  background: #272727;
}

.subscribe-btn.subscribed {
  background: #f2f2f2;
  color: #0f0f0f;
}

/* 频道导航 */
.channel-tabs {
  display: flex;
  gap: 8px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  border-bottom: 1px solid #e5e5e5;
}

.tab-btn {
  padding: 16px 24px;
  border: none;
  background: transparent;
  color: #606060;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  position: relative;
}

.tab-btn:hover {
  color: #0f0f0f;
}

.tab-btn.active {
  color: #0f0f0f;
}

.tab-btn.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: #0f0f0f;
}

/* 内容区域 */
.channel-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

/* 排序栏 */
.sort-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.sort-label {
  color: #606060;
  font-size: 14px;
}

.sort-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 16px;
  background: #f2f2f2;
  color: #0f0f0f;
  font-size: 14px;
  cursor: pointer;
}

.sort-btn:hover {
  background: #e5e5e5;
}

.sort-btn.active {
  background: #0f0f0f;
  color: #ffffff;
}

/* 视频网格 */
.video-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 1200px) {
  .video-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 900px) {
  .video-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 600px) {
  .video-grid { grid-template-columns: 1fr; }
}

.video-card {
  cursor: pointer;
}

.video-thumbnail {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  aspect-ratio: 16 / 9;
  background: #e5e5e5;
  margin-bottom: 12px;
}

.video-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.2s;
}

.video-card:hover .video-thumbnail img {
  transform: scale(1.05);
}

.video-thumbnail .duration {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0,0,0,0.8);
  color: #fff;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
}

.video-info {
  padding: 0 4px;
}

.video-title {
  font-size: 14px;
  font-weight: 500;
  color: #0f0f0f;
  margin: 0 0 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.video-meta {
  color: #606060;
  font-size: 12px;
}

.video-meta .dot {
  margin: 0 4px;
}

/* 简介区域 */
.about-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.about-card, .stats-card {
  background: #f9f9f9;
  border-radius: 12px;
  padding: 24px;
}

.about-card h3, .stats-card h3 {
  font-size: 16px;
  font-weight: 600;
  color: #0f0f0f;
  margin: 0 0 16px;
}

.about-card p {
  color: #606060;
  line-height: 1.6;
  margin: 0;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #e5e5e5;
}

.stat-item:last-child {
  border-bottom: none;
}

.stat-label {
  color: #606060;
}

.stat-value {
  font-weight: 500;
  color: #0f0f0f;
}

/* 加载和空状态 */
.loading-container, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: #606060;
}

/* 分页 */
.pagination-container {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .channel-info-section {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  
  .channel-avatar-large {
    width: 120px;
    height: 120px;
  }
  
  .channel-details {
    padding-top: 16px;
  }
  
  .channel-actions {
    padding-top: 16px;
  }
  
  .about-section {
    grid-template-columns: 1fr;
  }
}
</style>
