<template>
  <div class="video-card" @click="$emit('click', video)" @mouseenter="startPreview" @mouseleave="stopPreview">
    <!-- 缩略图 -->
    <div class="thumbnail-container">
      <video 
        v-if="isPreviewPlaying && video.videoUrl"
        ref="previewVideo"
        :src="video.videoUrl"
        class="preview-video"
        muted
        autoplay
        loop
      ></video>
      <img 
        v-show="!isPreviewPlaying"
        :src="video.thumbnail || defaultThumbnail" 
        :alt="video.title" 
        class="thumbnail-img" 
      />
      <!-- 时长 -->
      <div class="duration-badge" v-show="!isPreviewPlaying">
        {{ video.duration || '00:00' }}
      </div>
      <!-- 观看进度 -->
      <div v-if="video.watchProgress > 0" class="progress-bar">
        <div class="progress-fill" :style="{ width: video.watchProgress + '%' }"></div>
      </div>
      <!-- 悬停遮罩 -->
      <div class="hover-overlay" v-show="!isPreviewPlaying">
        <el-icon class="play-icon"><VideoPlay /></el-icon>
      </div>
      <!-- 快捷操作 -->
      <div class="quick-actions">
        <button class="quick-btn" @click.stop="$emit('watchLater', video)" title="稍后观看">
          <el-icon><Clock /></el-icon>
        </button>
        <button class="quick-btn" @click.stop="$emit('addQueue', video)" title="添加到队列">
          <el-icon><List /></el-icon>
        </button>
      </div>
    </div>
    
    <!-- 视频信息 -->
    <div class="video-info">
      <div class="avatar" @click.stop="$emit('channelClick', video.authorId)" :style="{ background: avatarColor }">
        <img v-if="video.author?.avatar" :src="getAvatarUrl(video.author.avatar)" />
        <span v-else>{{ channelInitial }}</span>
      </div>
      <div class="details">
        <h3 class="title">{{ video.title }}</h3>
        <div class="channel-name" @click.stop="$emit('channelClick', video.authorId)">
          {{ video.channelName || video.author?.realName || '未知频道' }}
        </div>
        <div class="meta">
          <span>{{ formatViews(video.viewCount) }}次观看</span>
          <span class="dot">•</span>
          <span>{{ formatTime(video.createdAt) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>


<script setup>
import { ref, computed } from 'vue'
import { VideoPlay, Clock, List } from '@element-plus/icons-vue'

const props = defineProps({
  video: {
    type: Object,
    required: true
  }
})

defineEmits(['click', 'watchLater', 'addQueue', 'channelClick'])

const defaultThumbnail = 'https://picsum.photos/seed/default/640/360'
const isPreviewPlaying = ref(false)
const previewVideo = ref(null)
let previewTimeout = null

const channelInitial = computed(() => {
  return (props.video.channelName || props.video.author?.realName || '?')[0]
})

const avatarColor = computed(() => {
  const colors = ['#ff0000', '#ff4500', '#ff6347', '#3b82f6', '#8b5cf6', '#10b981', '#f59e0b']
  return colors[(props.video.authorId || 0) % colors.length]
})

const startPreview = () => {
  if (!props.video.videoUrl) return
  previewTimeout = setTimeout(() => {
    isPreviewPlaying.value = true
  }, 500)
}

const stopPreview = () => {
  if (previewTimeout) {
    clearTimeout(previewTimeout)
    previewTimeout = null
  }
  isPreviewPlaying.value = false
}

const getAvatarUrl = (url) => {
  if (!url) return ''
  return url.startsWith('http') ? url : `http://localhost:8080${url}`
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
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  if (days < 365) return `${Math.floor(days / 30)}个月前`
  return `${Math.floor(days / 365)}年前`
}
</script>


<style scoped>
.video-card {
  cursor: pointer;
}

.thumbnail-container {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  background: #000;
  aspect-ratio: 16 / 9;
  margin-bottom: 12px;
}

.thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.video-card:hover .thumbnail-img {
  transform: scale(1.02);
}

.preview-video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.duration-badge {
  position: absolute;
  bottom: 8px;
  right: 8px;
  padding: 3px 4px;
  background: rgba(0, 0, 0, 0.8);
  border-radius: 4px;
  color: #fff;
  font-size: 12px;
  font-weight: 500;
}

.progress-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: rgba(255,255,255,0.3);
}

.progress-fill {
  height: 100%;
  background: #ff0000;
}

.hover-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.2);
  opacity: 0;
  transition: opacity 0.2s;
}

.video-card:hover .hover-overlay {
  opacity: 1;
}

.play-icon {
  font-size: 56px;
  color: #fff;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.5));
}

.quick-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.video-card:hover .quick-actions {
  opacity: 1;
}

.quick-btn {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  border: none;
  background: rgba(0,0,0,0.8);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.quick-btn:hover {
  background: rgba(0,0,0,0.95);
}

.video-info {
  display: flex;
  gap: 12px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
  margin-top: 4px;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar span {
  color: #fff;
  font-weight: 500;
  font-size: 14px;
}

.details {
  flex: 1;
  min-width: 0;
}

.title {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 500;
  color: #0f0f0f;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.channel-name {
  color: #606060;
  font-size: 14px;
  margin-bottom: 2px;
  transition: color 0.2s;
}

.channel-name:hover {
  color: #0f0f0f;
}

.meta {
  color: #606060;
  font-size: 14px;
}

.dot {
  margin: 0 4px;
}
</style>
