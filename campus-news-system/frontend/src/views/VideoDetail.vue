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
            <div class="like-dislike-group">
              <button 
                class="action-btn like-btn" 
                :class="{ active: video?.isLiked }"
                @click="handleLike"
              >
                <el-icon><Pointer /></el-icon>
                <span>{{ video?.likeCount || 0 }}</span>
              </button>
              <button 
                class="action-btn dislike-btn" 
                :class="{ active: isDisliked }"
                @click="handleDislike"
              >
                <el-icon style="transform: rotate(180deg)"><Pointer /></el-icon>
              </button>
            </div>
            <button class="action-btn" @click="handleShare">
              <el-icon><Share /></el-icon>
              <span>分享</span>
            </button>
            <button class="action-btn" @click="handleDownload">
              <el-icon><Download /></el-icon>
              <span>下载</span>
            </button>
            <button 
              class="action-btn" 
              :class="{ active: isInWatchLater }"
              @click="handleWatchLater"
            >
              <el-icon><Clock /></el-icon>
              <span>{{ isInWatchLater ? '已保存' : '保存' }}</span>
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
          <div class="channel-avatar" @click="goToChannel(video?.authorId)" style="cursor: pointer;">
            <img v-if="video?.author?.avatar" :src="getAvatarUrl(video.author.avatar)" />
            <span v-else>{{ (video?.channelName || video?.author?.realName || '?')[0] }}</span>
          </div>
          <div class="channel-details">
            <div class="channel-name" @click="goToChannel(video?.authorId)" style="cursor: pointer;">
              {{ video?.channelName || video?.author?.realName || '未知频道' }}
            </div>
            <div class="subscriber-count">{{ video?.author?.followerCount || 0 }} 位订阅者</div>
          </div>
          <button 
            v-if="!isOwnVideo"
            class="subscribe-btn" 
            :class="{ subscribed: isSubscribed }" 
            @click="toggleSubscribe"
          >
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
          <span class="comments-count">{{ totalComments }} 条评论</span>
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
                <button class="like-btn" :class="{ active: comment.isLiked }" @click="handleCommentLike(comment)">
                  <el-icon><Pointer /></el-icon>
                  <span>{{ comment.likeCount || 0 }}</span>
                </button>
                <button class="reply-btn" @click="startReply(comment)">回复</button>
              </div>
              <!-- 回复输入框 -->
              <div v-if="replyingTo?.id === comment.id" class="reply-input-wrapper">
                <input 
                  v-model="replyContent" 
                  type="text" 
                  :placeholder="'回复 @' + (comment.user?.realName || '用户')"
                  @keyup.enter="submitReply"
                />
                <div class="reply-actions">
                  <button class="cancel-btn" @click="cancelReply">取消</button>
                  <button class="submit-btn" :disabled="!replyContent.trim()" @click="submitReply">回复</button>
                </div>
              </div>
              <!-- 显示回复列表 -->
              <div v-if="comment.replies?.length" class="replies-list">
                <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                  <div class="reply-avatar">
                    <img v-if="reply.user?.avatar" :src="getAvatarUrl(reply.user.avatar)" />
                    <span v-else>{{ (reply.user?.realName || '?')[0] }}</span>
                  </div>
                  <div class="reply-content">
                    <div class="reply-header">
                      <span class="reply-author">{{ reply.user?.realName || '匿名用户' }}</span>
                      <span v-if="reply.replyToUser" class="reply-to">
                        回复 <span class="reply-to-name">@{{ reply.replyToUser.realName }}</span>
                      </span>
                      <span class="reply-time">{{ formatTime(reply.createdAt) }}</span>
                    </div>
                    <p class="reply-text">{{ reply.content }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-if="commentsLoading" class="comments-loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            加载中...
          </div>
          <div v-else-if="comments.length === 0" class="no-comments">
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
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Pointer, Share, Download, MoreFilled, Loading, Clock } from '@element-plus/icons-vue'
import { getVideoDetail, toggleVideoLike, getVideoComments, createVideoComment, toggleCommentLike, getRelatedVideos, toggleDislike, toggleWatchLater, saveWatchProgress, getWatchProgress, addWatchHistory } from '@/api/video'
import { toggleFollow, checkFollowStatus } from '@/api/user'

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

const totalComments = ref(0)
const commentsLoading = ref(false)
const replyingTo = ref(null) // 正在回复的评论
const replyContent = ref('')
const isDisliked = ref(false)
const isInWatchLater = ref(false)
const savedProgress = ref(0)

// 计算属性 - 前端不再排序，由后端处理
const sortedComments = computed(() => comments.value)

// 是否是自己的视频
const isOwnVideo = computed(() => {
  if (!currentUser.value || !video.value) return false
  return currentUser.value.id === video.value.authorId
})

// 加载视频详情
const loadVideo = async () => {
  try {
    currentVideoId = route.params.id // 保存当前视频ID
    const res = await getVideoDetail(route.params.id)
    video.value = res
    // 加载订阅状态
    loadSubscribeStatus()
    // 加载观看进度
    loadWatchProgress()
    // 添加到观看历史
    addToHistory()
  } catch (error) {
    ElMessage.error('加载视频失败')
  }
}

// 添加到观看历史
const addToHistory = async () => {
  if (!currentUser.value || !currentVideoId) return
  try {
    await addWatchHistory(currentVideoId)
  } catch (error) {
    // 静默处理
  }
}

// 加载观看进度
const loadWatchProgress = async () => {
  if (!currentUser.value || !currentVideoId) return
  try {
    const progress = await getWatchProgress(currentVideoId)
    savedProgress.value = progress || 0
    // 如果有保存的进度，跳转到该位置
    if (savedProgress.value > 0 && videoPlayer.value) {
      setTimeout(() => {
        if (videoPlayer.value) {
          videoPlayer.value.currentTime = savedProgress.value
        }
      }, 500)
    }
  } catch (error) {
    // 静默处理
  }
}

// 加载推荐视频（使用相关视频推荐API）
const loadRecommended = async () => {
  try {
    const res = await getRelatedVideos(route.params.id, 15)
    recommendedVideos.value = res || []
  } catch (error) {
    console.error('加载推荐视频失败:', error)
  }
}

// 加载评论
const loadComments = async () => {
  commentsLoading.value = true
  try {
    const res = await getVideoComments(route.params.id, {
      current: 1,
      size: 50,
      sortBy: sortBy.value
    })
    comments.value = res.records || []
    totalComments.value = res.total || 0
  } catch (error) {
    console.error('加载评论失败:', error)
  } finally {
    commentsLoading.value = false
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

// 订阅（关注视频作者）
const toggleSubscribe = async () => {
  if (!currentUser.value) {
    ElMessage.warning('请先登录')
    return
  }
  if (!video.value?.authorId) return
  
  try {
    const res = await toggleFollow(video.value.authorId)
    isSubscribed.value = res.isFollowing
    // 更新粉丝数
    if (video.value.author) {
      video.value.author.followerCount += res.isFollowing ? 1 : -1
    }
    ElMessage.success(res.message)
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 加载订阅状态
const loadSubscribeStatus = async () => {
  if (!currentUser.value || !video.value?.authorId) return
  // 不能关注自己
  if (currentUser.value.id === video.value.authorId) return
  
  try {
    const res = await checkFollowStatus(video.value.authorId)
    isSubscribed.value = res
  } catch (error) {
    console.error('加载订阅状态失败')
  }
}

// 提交评论
const submitComment = async () => {
  if (!newComment.value.trim()) return
  if (!currentUser.value) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await createVideoComment({
      videoId: Number(route.params.id),
      content: newComment.value.trim(),
      parentId: null
    })
    ElMessage.success('评论成功')
    newComment.value = ''
    commentFocused.value = false
    loadComments()
  } catch (error) {
    ElMessage.error('评论失败')
  }
}

// 点赞评论
const handleCommentLike = async (comment) => {
  if (!currentUser.value) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await toggleCommentLike(comment.id)
    comment.isLiked = res
    comment.likeCount += res ? 1 : -1
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 开始回复评论
const startReply = (comment) => {
  if (!currentUser.value) {
    ElMessage.warning('请先登录')
    return
  }
  replyingTo.value = comment
  replyContent.value = ''
}

// 取消回复
const cancelReply = () => {
  replyingTo.value = null
  replyContent.value = ''
}

// 提交回复
const submitReply = async () => {
  if (!replyContent.value.trim() || !replyingTo.value) return
  
  try {
    await createVideoComment({
      videoId: Number(route.params.id),
      content: replyContent.value.trim(),
      parentId: replyingTo.value.id
    })
    ElMessage.success('回复成功')
    cancelReply()
    loadComments()
  } catch (error) {
    ElMessage.error('回复失败')
  }
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

// 跳转频道
const goToChannel = (userId) => {
  if (userId) {
    router.push(`/channel/${userId}`)
  }
}

// 视频事件 - 保存观看进度
let progressSaveTimeout = null
let lastSavedTime = 0
let currentVideoId = null // 保存当前视频ID，避免路由变化时丢失

const onTimeUpdate = () => {
  if (!currentUser.value || !videoPlayer.value || !currentVideoId) return
  
  const currentTime = Math.floor(videoPlayer.value.currentTime)
  
  // 每30秒保存一次进度，且进度变化超过5秒才保存
  if (progressSaveTimeout) return
  if (Math.abs(currentTime - lastSavedTime) < 5) return
  
  const videoIdToSave = currentVideoId
  progressSaveTimeout = setTimeout(async () => {
    try {
      if (videoPlayer.value && currentTime > 0 && videoIdToSave) {
        await saveWatchProgress(videoIdToSave, currentTime)
        lastSavedTime = currentTime
      }
    } catch (error) {
      // 静默处理错误，不影响用户体验
    }
    progressSaveTimeout = null
  }, 30000)
}

const onLoadedMetadata = () => {
  // 如果有保存的进度，跳转到该位置
  if (savedProgress.value > 0 && videoPlayer.value) {
    videoPlayer.value.currentTime = savedProgress.value
    lastSavedTime = savedProgress.value
  }
}

// 页面离开时保存进度
const saveProgressOnLeave = async (videoId) => {
  if (!currentUser.value || !videoPlayer.value) return
  const idToSave = videoId || currentVideoId
  if (!idToSave) return
  
  try {
    const currentTime = Math.floor(videoPlayer.value.currentTime)
    if (currentTime > 0 && currentTime !== lastSavedTime) {
      await saveWatchProgress(idToSave, currentTime)
    }
  } catch (error) {
    // 静默处理
  }
}

// 不喜欢
const handleDislike = async () => {
  if (!currentUser.value) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await toggleDislike(video.value.id)
    isDisliked.value = res
    // 如果点了不喜欢，取消点赞
    if (res && video.value.isLiked) {
      video.value.isLiked = false
      video.value.likeCount -= 1
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 稍后观看
const handleWatchLater = async () => {
  if (!currentUser.value) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await toggleWatchLater(video.value.id)
    isInWatchLater.value = res
    ElMessage.success(res ? '已添加到稍后观看' : '已从稍后观看移除')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

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
watch(() => route.params.id, async (newId, oldId) => {
  // 切换视频时保存旧视频的进度
  if (oldId && newId !== oldId) {
    await saveProgressOnLeave(oldId)
    lastSavedTime = 0
    savedProgress.value = 0
  }
  if (newId) {
    loadVideo()
    loadRecommended()
    loadComments()
  }
})

// 监听排序变化
watch(sortBy, () => {
  loadComments()
})

onMounted(() => {
  loadCurrentUser()
  loadVideo()
  loadRecommended()
  loadComments()
})

// 页面离开时保存进度
onBeforeUnmount(() => {
  saveProgressOnLeave()
  // 清理定时器
  if (progressSaveTimeout) {
    clearTimeout(progressSaveTimeout)
  }
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

/* 点赞/不喜欢按钮组 */
.like-dislike-group {
  display: flex;
  background: #f2f2f2;
  border-radius: 18px;
  overflow: hidden;
}

.like-dislike-group .action-btn {
  border-radius: 0;
}

.like-dislike-group .like-btn {
  border-radius: 18px 0 0 18px;
  border-right: 1px solid #e5e5e5;
}

.like-dislike-group .dislike-btn {
  border-radius: 0 18px 18px 0;
  padding: 10px 14px;
}

.like-dislike-group .action-btn.active {
  background: #0f0f0f;
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

.like-btn.active {
  color: #065fd4;
}

.comments-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: #606060;
}

/* 回复输入框 */
.reply-input-wrapper {
  margin-top: 12px;
  padding-left: 52px;
}

.reply-input-wrapper input {
  width: 100%;
  background: transparent;
  border: none;
  border-bottom: 1px solid #e5e5e5;
  padding: 8px 0;
  color: #0f0f0f;
  font-size: 14px;
  outline: none;
}

.reply-input-wrapper input:focus {
  border-bottom-color: #0f0f0f;
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

/* 回复列表 */
.replies-list {
  margin-top: 12px;
  padding-left: 52px;
}

.reply-item {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.reply-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #909090;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.reply-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.reply-avatar span {
  color: #fff;
  font-size: 10px;
}

.reply-content {
  flex: 1;
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
  flex-wrap: wrap;
}

.reply-author {
  font-size: 12px;
  font-weight: 500;
  color: #0f0f0f;
}

.reply-to {
  font-size: 12px;
  color: #606060;
}

.reply-to-name {
  color: #065fd4;
}

.reply-time {
  font-size: 11px;
  color: #909090;
}

.reply-text {
  font-size: 13px;
  color: #0f0f0f;
  margin: 0;
  line-height: 1.4;
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
