<template>
  <div class="channel-page">
    <!-- 频道头部 -->
    <div class="channel-header">
      <div class="channel-banner" :style="{ background: getBannerGradient() }">
        <div class="banner-pattern"></div>
        <div class="banner-gradient"></div>
      </div>
      <div class="channel-info-section">
        <div class="channel-avatar-large" :style="{ background: getAvatarGradient() }">
          <img v-if="channelInfo?.user?.avatar" :src="getAvatarUrl(channelInfo.user.avatar)" />
          <span v-else>{{ (channelInfo?.user?.realName || '?')[0] }}</span>
        </div>
        <div class="channel-details">
          <div class="channel-name-row">
            <h1 class="channel-name">{{ channelInfo?.user?.realName || '未知频道' }}</h1>
            <el-icon v-if="channelInfo?.verified" class="verified-badge"><CircleCheckFilled /></el-icon>
          </div>
          <div class="channel-handle">@{{ channelInfo?.user?.username }}</div>
          <div class="channel-stats">
            <span>{{ formatNumber(channelInfo?.subscriberCount) }} 位订阅者</span>
            <span class="dot">•</span>
            <span>{{ channelInfo?.videoCount || 0 }} 个视频</span>
            <span class="dot">•</span>
            <span>{{ formatNumber(channelInfo?.totalViews) }} 次观看</span>
          </div>
          <p class="channel-description">
            {{ truncateDescription(channelInfo?.user?.bio) }}
            <button v-if="channelInfo?.user?.bio?.length > 100" class="more-btn" @click="activeTab = 'about'">...更多</button>
          </p>
          <div class="channel-links" v-if="channelInfo?.links?.length">
            <a v-for="link in channelInfo.links.slice(0, 3)" :key="link.url" :href="link.url" target="_blank" class="channel-link">
              {{ link.title }}
            </a>
          </div>
        </div>
        <div class="channel-actions">
          <button 
            v-if="!isOwnChannel"
            class="subscribe-btn" 
            :class="{ subscribed: isSubscribed }"
            @click="toggleSubscribe"
          >
            <el-icon v-if="isSubscribed"><Bell /></el-icon>
            {{ isSubscribed ? '已订阅' : '订阅' }}
          </button>
          <button v-if="isOwnChannel" class="customize-btn" @click="customizeChannel">
            自定义频道
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
import { Loading, VideoCamera, CircleCheckFilled, Bell } from '@element-plus/icons-vue'
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

const getBannerGradient = () => {
  // 简洁的白色背景，无 banner
  return '#ffffff'
}

const getAvatarGradient = () => {
  // 柔和的颜色，类似 YouTube 的头像背景
  const colors = [
    '#5c6bc0', // 靛蓝
    '#26a69a', // 青色
    '#7e57c2', // 紫色
    '#42a5f5', // 蓝色
    '#66bb6a', // 绿色
    '#ef5350', // 红色
    '#ffa726', // 橙色
    '#78909c'  // 蓝灰
  ]
  const id = channelInfo.value?.user?.id || 0
  return colors[id % colors.length]
}

const truncateDescription = (text) => {
  if (!text) return '这个频道还没有简介'
  if (text.length <= 100) return text
  return text.substring(0, 100)
}

const customizeChannel = () => {
  ElMessage.info('频道自定义功能开发中')
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

/* 频道头部 - YouTube 风格 */
.channel-header {
  position: relative;
  background: #ffffff;
}

.channel-banner {
  height: 0;
  display: none;
}

.banner-pattern {
  display: none;
}

.banner-gradient {
  display: none;
}

.channel-info-section {
  display: flex;
  align-items: center;
  gap: 24px;
  max-width: 1284px;
  margin: 0 auto;
  padding: 16px 24px 24px;
  transform: translateY(0);
}

.channel-avatar-large {
  width: 128px;
  height: 128px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: none;
  box-shadow: none;
  flex-shrink: 0;
  transition: none;
}

.channel-avatar-large:hover {
  transform: none;
}

.channel-avatar-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.channel-avatar-large span {
  color: #fff;
  font-size: 48px;
  font-weight: 500;
}

.channel-details {
  flex: 1;
  padding-top: 0;
}

.channel-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}

.channel-name {
  font-size: 24px;
  font-weight: 400;
  color: #0f0f0f;
  margin: 0;
  letter-spacing: -0.5px;
}

.verified-badge {
  color: #606060;
  font-size: 16px;
}

.channel-handle {
  color: #606060;
  font-size: 14px;
  margin-bottom: 2px;
}

.channel-stats {
  color: #606060;
  font-size: 14px;
  margin-bottom: 8px;
}

.channel-stats .dot {
  margin: 0 4px;
}

.channel-description {
  color: #606060;
  font-size: 14px;
  margin: 0;
  max-width: 600px;
  line-height: 1.4;
}

.more-btn {
  background: none;
  border: none;
  color: #606060;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
}

.more-btn:hover {
  color: #0f0f0f;
}

.channel-links {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.channel-link {
  color: #065fd4;
  font-size: 14px;
  text-decoration: none;
}

.channel-link:hover {
  text-decoration: underline;
}

.channel-actions {
  padding-top: 0;
  align-self: center;
}

.subscribe-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border: none;
  border-radius: 18px;
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
  color: #606060;
}

.subscribe-btn.subscribed:hover {
  background: #e5e5e5;
}

.customize-btn {
  padding: 10px 16px;
  border: none;
  border-radius: 18px;
  background: #f2f2f2;
  color: #0f0f0f;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.customize-btn:hover {
  background: #e5e5e5;
}

/* 频道导航 - YouTube 风格 */
.channel-tabs {
  display: flex;
  gap: 0;
  max-width: 1284px;
  margin: 0 auto;
  padding: 0 24px;
  border-bottom: 1px solid rgba(0,0,0,0.1);
}

.tab-btn {
  padding: 12px 32px;
  border: none;
  background: transparent;
  color: #606060;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
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
  height: 2px;
  background: #0f0f0f;
}

/* 内容区域 */
.channel-content {
  max-width: 1284px;
  margin: 0 auto;
  padding: 16px 24px;
}

/* 排序栏 - YouTube 风格 */
.sort-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0,0,0,0.1);
}

.sort-label {
  color: #606060;
  font-size: 14px;
  margin-right: 8px;
}

.sort-btn {
  padding: 6px 12px;
  border: 1px solid #d3d3d3;
  border-radius: 8px;
  background: #ffffff;
  color: #0f0f0f;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.sort-btn:hover {
  background: #f2f2f2;
}

.sort-btn.active {
  background: #0f0f0f;
  color: #ffffff;
  border-color: #0f0f0f;
}

/* 视频网格 - YouTube 风格 */
.video-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px 16px;
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
  border-radius: 8px;
  overflow: hidden;
  aspect-ratio: 16 / 9;
  background: #f2f2f2;
  margin-bottom: 8px;
}

.video-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: none;
}

.video-card:hover .video-thumbnail img {
  transform: none;
}

.video-thumbnail .duration {
  position: absolute;
  bottom: 4px;
  right: 4px;
  background: rgba(0,0,0,0.8);
  color: #fff;
  font-size: 12px;
  padding: 2px 4px;
  border-radius: 4px;
  font-weight: 500;
}

.video-info {
  padding: 0;
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
  line-height: 1.4;
}

.video-meta {
  color: #606060;
  font-size: 12px;
}

.video-meta .dot {
  margin: 0 4px;
}

/* 简介区域 - YouTube 风格 */
.about-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.about-card, .stats-card {
  background: #ffffff;
  border: 1px solid rgba(0,0,0,0.1);
  border-radius: 12px;
  padding: 20px;
}

.about-card h3, .stats-card h3 {
  font-size: 16px;
  font-weight: 500;
  color: #0f0f0f;
  margin: 0 0 12px;
}

.about-card p {
  color: #606060;
  line-height: 1.5;
  margin: 0;
  font-size: 14px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid rgba(0,0,0,0.05);
}

.stat-item:last-child {
  border-bottom: none;
}

.stat-label {
  color: #606060;
  font-size: 14px;
}

.stat-value {
  font-weight: 500;
  color: #0f0f0f;
  font-size: 14px;
}

/* 加载和空状态 */
.loading-container, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: #909090;
}

.empty-state p {
  margin-top: 16px;
  font-size: 14px;
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
    width: 80px;
    height: 80px;
  }
  
  .channel-avatar-large span {
    font-size: 32px;
  }
  
  .channel-details {
    padding-top: 12px;
  }
  
  .channel-actions {
    padding-top: 12px;
  }
  
  .about-section {
    grid-template-columns: 1fr;
  }
  
  .channel-name {
    font-size: 20px;
  }
  
  .channel-tabs {
    padding: 0 16px;
  }
  
  .tab-btn {
    padding: 12px 16px;
  }
}
</style>
