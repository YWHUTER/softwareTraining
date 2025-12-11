<template>
  <div class="video-page">
    <!-- 主内容区 -->
    <div class="video-main">
      <!-- 视频播放器 -->
      <div class="video-player-wrapper">
        <video
          ref="videoPlayer"
          :src="video?.videoUrl"
          controls
          autoplay
          class="video-player"
          @timeupdate="onTimeUpdate"
          @loadedmetadata="onLoadedMetadata"
        ></video>
      </div>

      <!-- 视频信息 -->
      <div class="video-info">
        <h1 class="video-title">{{ video?.title }}</h1>
        
        <div class="video-meta-actions">
          <div class="video-meta">
            <span class="view-count">{{ formatViews(video?.viewCount) }}次观看</span>
            <span class="dot">•</span>
            <span class="upload-date">{{ formatDate(video?.createdAt) }}</span>
          </div>
          
          <div class="video-actions">
            <button 
              class="action-btn" 
              :class="{ active: video?.isLiked }"
              @click="handleLike"
            >
              <el-icon><Pointer /></el-icon>
              <span>{{ video?.likeCount || 0 }}</span>
            </button>
            <button class="action-btn" @click="handleShare">
              <el-icon><Share /></el-icon>
              <span>分享</span>
            </button>
            <button class="action-btn" @click="handleDownload">
              <el-icon><Download /></el-icon>
              <span>下载</span>
            </button>
            <button class="action-btn">
              <el-icon><MoreFilled /></el-icon>
            </button>
          </div>
        </div>
      </div>

      <!-- 频道信息和描述 -->
      <div class="channel-section">
        <div class="channel-info">
          <div class="channel-avatar">
            <img v-if="video?.author?.avatar" :src="getAvatarUrl(video.author.avatar)" />
            <span v-else>{{ (video?.channelName || video?.author?.realName || '?')[0] }}</span>
          </div>
          <div class="channel-details">
            <div class="channel-name">{{ video?.channelName || video?.author?.realName || '未知频道' }}</div>
            <div class="subscriber-count">{{ video?.author?.followerCount || 0 }} 位订阅者</div>
          </div>
          <button class="subscribe-btn" :class="{ subscribed: isSubscribed }" @click="toggleSubscribe">
            {{ isSubscribed ? '已订阅' : '订阅' }}
          </button>
        </div>
        
        <div class="description-box" :class="{ expanded: descExpanded }">
          <div class="description-content">
            <p>{{ video?.description || '暂无简介' }}</p>
          </div>
          <button v-if="video?.description?.length > 100" class="expand-btn" @click="descExpanded = !descExpanded">
            {{ descExpanded ? '收起' : '展开' }}
          </button>
        </div>
      </div>

      <!-- 评论区 -->
      <div class="comments-section">
        <div class="comments-header">
          <span class="comments-count">{{ comments.length }} 条评论</span>
          <div class="sort-options">
            <button :class="{ active: sortBy === 'hot' }" @click="sortBy = 'hot'">按热度</button>
            <button :class="{ active: sortBy === 'new' }" @click="sortBy = 'new'">按时间</button>
          </div>
        </div>

        <!-- 评论输入 -->
        <div class="comment-input-wrapper">
          <div class="user-avatar">
            <img v-if="currentUser?.avatar" :src="getAvatarUrl(currentUser.avatar)" />
            <span v-else>{{ (currentUser?.realName || '?')[0] }}</span>
          </div>
          <div class="comment-input-box">
            <input 
              v-model="newComment" 
              type="text" 
              placeholder="添加评论..."
              @focus="commentFocused = true"
            />
            <div v-if="commentFocused" class="comment-actions">
              <button class="cancel-btn" @click="cancelComment">取消</button>
              <button class="submit-btn" :disabled="!newComment.trim()" @click="submitComment">评论</button>
            </div>
          </div>
        </div>

        <!-- 评论列表 -->
        <div class="comments-list">
          <div v-for="comment in sortedComments" :key="comment.id" class="comment-item">
            <div class="comment-avatar">
              <img v-if="comment.user?.avatar" :src="getAvatarUrl(comment.user.avatar)" />
              <span v-else>{{ (comment.user?.realName || '?')[0] }}</span>
            </div>
            <div class="comment-content">
              <div class="comment-header">
                <span class="comment-author">{{ comment.user?.realName || '匿名用户' }}</span>
                <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
              </div>
              <p class="comment-text">{{ comment.content }}</p>
              <div class="comment-footer">
                <button class="like-btn">
                  <el-icon><Pointer /></el-icon>
                  <span>{{ comment.likeCount || 0 }}</span>
                </button>
                <button class="reply-btn">回复</button>
              </div>
            </div>
          </div>
          <div v-if="comments.length === 0" class="no-comments">
            暂无评论，来发表第一条评论吧
          </div>
        </div>
      </div>
    </div>

    <!-- 推荐视频侧边栏 -->
    <div class="video-sidebar">
      <div class="sidebar-header">
        <span>推荐视频</span>
      </div>
      <div class="recommended-list">
        <div 
          v-for="item in recommendedVideos" 
          :key="item.id" 
          class="recommended-item"
          @click="goToVideo(item.id)"
        >
          <div class="recommended-thumbnail">
            <img :src="item.thumbnail || defaultThumbnail" :alt="item.title" />
            <span class="duration">{{ item.duration || '00:00' }}</span>
          </div>
          <div class="recommended-info">
            <h4 class="recommended-title">{{ item.title }}</h4>
            <p class="recommended-channel">{{ item.channelName || item.author?.realName }}</p>
            <p class="recommended-meta">
              {{ formatViews(item.viewCount) }}次观看 • {{ formatTime(item.createdAt) }}
            </p>
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
import { Pointer, Share, Download, MoreFilled } from '@element-plus/icons-vue'
import { getVideoDetail, getVideoList, toggleVideoLike } from '@/api/video'

const route = useRoute()
const router = useRouter()

const defaultThumbnail = 'https://picsum.photos/seed/default/320/180'

// 状态
const video = ref(null)
const videoPlayer = ref(null)
const recommendedVideos = ref([])
const comments = ref([])
const newComment = ref('')
const commentFocused = ref(false)
const descExpanded = ref(false)
const isSubscribed = ref(false)
const sortBy = ref('hot')
const currentUser = ref(null)

// 计算属性
const sortedComments = computed(() => {
  const list = [...comments.value]
  if (sortBy.value === 'hot') {
    return list.sort((a, b) => (b.likeCount || 0) - (a.likeCount || 0))
  }
  return list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
})

// 加载视频详情
const loadVideo = async () => {
  try {
    const res = await getVideoDetail(route.params.id)
    video.value = res
  } catch (error) {
    ElMessage.error('加载视频失败')
  }
}

// 加载推荐视频
const loadRecommended = async () => {
  try {
    const res = await getVideoList({ current: 1, size: 10 })
    recommendedVideos.value = (res.records || []).filter(v => v.id !== Number(route.params.id))
  } catch (error) {
    console.error('加载推荐视频失败')
  }
}

// 获取当前用户
const loadCurrentUser = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    currentUser.value = JSON.parse(userStr)
  }
}

// 点赞
const handleLike = async () => {
  if (!video.value) return
  try {
    const res = await toggleVideoLike(video.value.id)
    video.value.isLiked = res
    video.value.likeCount += res ? 1 : -1
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 分享
const handleShare = () => {
  navigator.clipboard.writeText(window.location.href)
  ElMessage.success('链接已复制到剪贴板')
}

// 下载
const handleDownload = () => {
  if (video.value?.videoUrl) {
    window.open(video.value.videoUrl, '_blank')
  }
}

// 订阅
const toggleSubscribe = () => {
  isSubscribed.value = !isSubscribed.value
  ElMessage.success(isSubscribed.value ? '订阅成功' : '已取消订阅')
}

// 提交评论
const submitComment = () => {
  if (!newComment.value.trim()) return
  ElMessage.info('评论功能开发中')
  newComment.value = ''
  commentFocused.value = false
}

// 取消评论
const cancelComment = () => {
  newComment.value = ''
  commentFocused.value = false
}

// 跳转视频
const goToVideo = (id) => {
  router.push(`/video/${id}`)
}

// 视频事件
const onTimeUpdate = () => {}
const onLoadedMetadata = () => {}

// 工具函数
const getAvatarUrl = (url) => {
  if (!url) return ''
  return url.startsWith('http') ? url : `http://localhost:8080${url}`
}

const formatViews = (num) => {
  if (!num) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return num.toLocaleString()
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
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
  return date.toLocaleDateString('zh-CN')
}

// 监听路由变化
watch(() => route.params.id, () => {
  if (route.params.id) {
    loadVideo()
    loadRecommended()
  }
})

onMounted(() => {
  loadCurrentUser()
  loadVideo()
  loadRecommended()
})
</script>


<style scoped>
.video-page {
  display: flex;
  max-width: 1800px;
  margin: 0 auto;
  padding: 24px;
  gap: 24px;
  background: #ffffff;
  min-height: calc(100vh - 60px);
}

/* 主内容区 */
.video-main {
  flex: 1;
  max-width: 1280px;
}

/* 视频播放器 */
.video-player-wrapper {
  width: 100%;
  background: #000;
  border-radius: 12px;
  overflow: hidden;
  aspect-ratio: 16 / 9;
}

.video-player {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

/* 视频信息 */
.video-info {
  padding: 12px 0;
}

.video-title {
  font-size: 20px;
  font-weight: 600;
  color: #0f0f0f;
  margin: 0 0 12px;
  line-height: 1.4;
}

.video-meta-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.video-meta {
  color: #606060;
  font-size: 14px;
}

.video-meta .dot {
  margin: 0 4px;
}

.video-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border: none;
  border-radius: 18px;
  background: #f2f2f2;
  color: #0f0f0f;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.action-btn:hover {
  background: #e5e5e5;
}

.action-btn.active {
  background: #065fd4;
  color: #ffffff;
}

/* 频道区域 */
.channel-section {
  background: #f2f2f2;
  border-radius: 12px;
  padding: 16px;
  margin: 16px 0;
}

.channel-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.channel-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff0000, #cc0000);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.channel-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.channel-avatar span {
  color: #fff;
  font-weight: 600;
}

.channel-details {
  flex: 1;
}

.channel-name {
  font-size: 16px;
  font-weight: 500;
  color: #0f0f0f;
}

.subscriber-count {
  font-size: 12px;
  color: #606060;
}

.subscribe-btn {
  padding: 10px 16px;
  border: none;
  border-radius: 18px;
  background: #0f0f0f;
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.subscribe-btn:hover {
  background: #272727;
}

.subscribe-btn.subscribed {
  background: #f2f2f2;
  color: #0f0f0f;
  border: none;
}

.description-box {
  color: #0f0f0f;
  font-size: 14px;
  line-height: 1.6;
}

.description-content {
  max-height: 60px;
  overflow: hidden;
}

.description-box.expanded .description-content {
  max-height: none;
}

.expand-btn {
  background: none;
  border: none;
  color: #606060;
  cursor: pointer;
  padding: 8px 0;
  font-size: 14px;
  font-weight: 500;
}

.expand-btn:hover {
  color: #0f0f0f;
}

/* 评论区 */
.comments-section {
  margin-top: 24px;
}

.comments-header {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 24px;
}

.comments-count {
  font-size: 16px;
  font-weight: 500;
  color: #0f0f0f;
}

.sort-options {
  display: flex;
  gap: 8px;
}

.sort-options button {
  background: none;
  border: none;
  color: #606060;
  font-size: 14px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 16px;
}

.sort-options button:hover {
  background: #f2f2f2;
}

.sort-options button.active {
  background: #0f0f0f;
  color: #ffffff;
}

/* 评论输入 */
.comment-input-wrapper {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #3ea6ff;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-avatar span {
  color: #fff;
  font-weight: 600;
}

.comment-input-box {
  flex: 1;
}

.comment-input-box input {
  width: 100%;
  background: transparent;
  border: none;
  border-bottom: 1px solid #e5e5e5;
  padding: 8px 0;
  color: #0f0f0f;
  font-size: 14px;
  outline: none;
}

.comment-input-box input::placeholder {
  color: #909090;
}

.comment-input-box input:focus {
  border-bottom-color: #0f0f0f;
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.cancel-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 18px;
  background: transparent;
  color: #0f0f0f;
  cursor: pointer;
}

.cancel-btn:hover {
  background: #f2f2f2;
}

.submit-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 18px;
  background: #065fd4;
  color: #ffffff;
  cursor: pointer;
  font-weight: 500;
}

.submit-btn:hover {
  background: #0056b8;
}

.submit-btn:disabled {
  background: #f2f2f2;
  color: #909090;
  cursor: not-allowed;
}

/* 评论列表 */
.comments-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  display: flex;
  gap: 12px;
}

.comment-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #606060;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.comment-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.comment-avatar span {
  color: #fff;
  font-size: 14px;
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.comment-author {
  font-size: 13px;
  font-weight: 500;
  color: #0f0f0f;
}

.comment-time {
  font-size: 12px;
  color: #606060;
}

.comment-text {
  color: #0f0f0f;
  font-size: 14px;
  line-height: 1.5;
  margin: 0 0 8px;
}

.comment-footer {
  display: flex;
  gap: 8px;
}

.like-btn, .reply-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: #606060;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 12px;
}

.like-btn:hover, .reply-btn:hover {
  background: #f2f2f2;
}

.no-comments {
  text-align: center;
  color: #606060;
  padding: 40px;
}

/* 推荐视频侧边栏 */
.video-sidebar {
  width: 400px;
  flex-shrink: 0;
}

.sidebar-header {
  font-size: 16px;
  font-weight: 500;
  color: #0f0f0f;
  margin-bottom: 16px;
}

.recommended-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.recommended-item {
  display: flex;
  gap: 8px;
  cursor: pointer;
  padding: 4px;
  border-radius: 8px;
  transition: background 0.2s;
}

.recommended-item:hover {
  background: #f2f2f2;
}

.recommended-thumbnail {
  width: 168px;
  height: 94px;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  flex-shrink: 0;
  background: #e5e5e5;
}

.recommended-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.recommended-thumbnail .duration {
  position: absolute;
  bottom: 4px;
  right: 4px;
  background: rgba(0, 0, 0, 0.8);
  color: #fff;
  font-size: 12px;
  padding: 2px 4px;
  border-radius: 4px;
}

.recommended-info {
  flex: 1;
  min-width: 0;
}

.recommended-title {
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

.recommended-channel {
  font-size: 12px;
  color: #606060;
  margin: 0 0 2px;
}

.recommended-meta {
  font-size: 12px;
  color: #606060;
  margin: 0;
}

/* 响应式 */
@media (max-width: 1200px) {
  .video-page {
    flex-direction: column;
  }
  
  .video-sidebar {
    width: 100%;
  }
  
  .recommended-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 16px;
  }
}

@media (max-width: 768px) {
  .video-page {
    padding: 0;
  }
  
  .video-player-wrapper {
    border-radius: 0;
  }
  
  .video-info, .channel-section, .comments-section {
    padding-left: 12px;
    padding-right: 12px;
  }
  
  .video-meta-actions {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .video-actions {
    width: 100%;
    justify-content: space-around;
  }
  
  .action-btn span {
    display: none;
  }
}
</style>
