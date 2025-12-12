<template>
  <div class="yt-container">
    <!-- 左侧侧边栏 -->
    <div class="yt-sidebar">
      <!-- 主要导航 -->
      <div class="sidebar-section">
        <div class="sidebar-item" :class="{ active: currentView === 'home' }" @click="switchView('home')">
          <el-icon :size="24"><HomeFilled /></el-icon>
          <span>首页</span>
        </div>
        <div class="sidebar-item" @click="showUploadDialog = true">
          <el-icon :size="24"><Upload /></el-icon>
          <span>上传视频</span>
        </div>
        <div class="sidebar-item">
          <el-icon :size="24"><Collection /></el-icon>
          <span>订阅</span>
        </div>
      </div>
      
      <div class="sidebar-divider"></div>

      <!-- 个人中心 -->
      <div class="sidebar-section">
        <div class="sidebar-header">
          <span>我</span>
          <el-icon><ArrowRight /></el-icon>
        </div>
        <div class="sidebar-item" :class="{ active: currentView === 'myVideos' }" @click="switchView('myVideos')">
          <el-icon :size="24"><VideoCamera /></el-icon>
          <span>我的视频</span>
        </div>
        <div class="sidebar-item" :class="{ active: currentView === 'history' }" @click="switchView('history')">
          <el-icon :size="24"><Clock /></el-icon>
          <span>历史记录</span>
        </div>
        <div class="sidebar-item">
          <el-icon :size="24"><Files /></el-icon>
          <span>播放列表</span>
        </div>
        <div class="sidebar-item" :class="{ active: currentView === 'watchLater' }" @click="switchView('watchLater')">
          <el-icon :size="24"><Timer /></el-icon>
          <span>稍后观看</span>
        </div>
        <div class="sidebar-item" :class="{ active: currentView === 'liked' }" @click="switchView('liked')">
          <el-icon :size="24"><Pointer /></el-icon>
          <span>点赞的视频</span>
        </div>
      </div>

      <div class="sidebar-divider"></div>

      <!-- 订阅列表 -->
      <div class="sidebar-section">
        <div class="sidebar-header">
          <span>订阅内容</span>
        </div>
        <div v-for="sub in subscriptions" :key="sub.id" class="sidebar-item subscription-item">
          <el-avatar :size="24" :style="{ background: sub.color }">{{ sub.name[0] }}</el-avatar>
          <span class="sub-name">{{ sub.name }}</span>
          <span v-if="sub.hasNew" class="new-dot"></span>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="yt-main">
      <!-- 搜索栏 -->
      <div class="search-bar-container">
        <div class="search-input-wrapper">
          <el-icon class="search-icon"><Search /></el-icon>
          <input 
            v-model="searchKeyword" 
            type="text" 
            placeholder="搜索视频..."
            class="search-input"
            @keyup.enter="handleSearch"
            @input="handleSearchInput"
          />
          <button v-if="searchKeyword" class="clear-btn" @click="clearSearch">
            <el-icon><Close /></el-icon>
          </button>
          <button class="search-btn" @click="handleSearch">
            <el-icon><Search /></el-icon>
          </button>
        </div>
        <!-- 搜索建议 -->
        <div v-if="showSuggestions && suggestions.length > 0" class="search-suggestions">
          <div 
            v-for="(suggestion, index) in suggestions" 
            :key="index"
            class="suggestion-item"
            @click="selectSuggestion(suggestion)"
          >
            <el-icon><Search /></el-icon>
            <span>{{ suggestion }}</span>
          </div>
        </div>
      </div>

      <!-- 搜索筛选器 -->
      <div v-if="isSearchMode" class="search-filters">
        <div class="filter-group">
          <span class="filter-label">时长:</span>
          <button 
            v-for="opt in durationOptions" 
            :key="opt.value"
            class="filter-btn"
            :class="{ active: searchFilters.duration === opt.value }"
            @click="setFilter('duration', opt.value)"
          >
            {{ opt.label }}
          </button>
        </div>
        <div class="filter-group">
          <span class="filter-label">上传日期:</span>
          <button 
            v-for="opt in uploadDateOptions" 
            :key="opt.value"
            class="filter-btn"
            :class="{ active: searchFilters.uploadDate === opt.value }"
            @click="setFilter('uploadDate', opt.value)"
          >
            {{ opt.label }}
          </button>
        </div>
        <div class="filter-group">
          <span class="filter-label">排序:</span>
          <button 
            v-for="opt in sortOptions" 
            :key="opt.value"
            class="filter-btn"
            :class="{ active: searchFilters.sortBy === opt.value }"
            @click="setFilter('sortBy', opt.value)"
          >
            {{ opt.label }}
          </button>
        </div>
      </div>

      <!-- 页面标题 -->
      <div v-if="currentView !== 'home' && !isSearchMode" class="page-title">
        <h2>{{ getViewTitle() }}</h2>
        <button v-if="currentView === 'history'" class="clear-history-btn" @click="handleClearHistory">
          清空历史
        </button>
      </div>
      <div v-if="isSearchMode" class="page-title">
        <h2>搜索结果: "{{ searchKeyword }}"</h2>
        <span class="result-count">共 {{ total }} 个结果</span>
      </div>
      
      <!-- 分类筛选栏 -->
      <div v-if="currentView === 'home'" class="yt-chips-bar">
        <div class="yt-chips-scroll">
          <button 
            class="yt-chip"
            :class="{ 'yt-chip-active': activeCategory === 'all' }"
            @click="selectCategory('all')"
          >
            全部
          </button>
          <button 
            v-for="cat in categories" 
            :key="cat.id"
            class="yt-chip"
            :class="{ 'yt-chip-active': activeCategory === cat.code }"
            @click="selectCategory(cat.code)"
          >
            {{ cat.name }}
          </button>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
        <p>加载中...</p>
      </div>

      <!-- 视频网格 -->
      <div v-else class="yt-grid">
        <div 
          v-for="video in videos" 
          :key="video.id" 
          class="yt-video-renderer"
          @click="handleVideoClick(video)"
        >
          <!-- 缩略图容器 -->
          <div class="yt-thumbnail">
            <a class="yt-thumbnail-link">
              <img :src="video.thumbnail || defaultThumbnail" :alt="video.title" class="yt-thumbnail-img" />
              <!-- 时长标签 -->
              <div class="yt-time-status">
                <span class="yt-time-text">{{ video.duration || '00:00' }}</span>
              </div>
              <!-- 悬停遮罩 -->
              <div class="yt-thumbnail-hover">
                <el-icon class="yt-play-icon"><VideoPlay /></el-icon>
              </div>
            </a>
          </div>
          
          <!-- 视频元数据 -->
          <div class="yt-meta">
            <a class="yt-avatar-link" @click.stop="goToChannel(video.authorId)">
              <div class="yt-avatar">
                {{ (video.channelName || video.author?.realName || '未知')[0] }}
              </div>
            </a>
            <div class="yt-details">
              <h3 class="yt-video-title">
                <a class="yt-title-link">{{ video.title }}</a>
              </h3>
              <div class="yt-channel-info">
                <a class="yt-channel-name" @click.stop="goToChannel(video.authorId)">{{ video.channelName || video.author?.realName || '未知频道' }}</a>
                <el-tooltip content="验证频道" placement="top" :show-after="500">
                  <el-icon class="verified-icon" :size="12"><CircleCheckFilled /></el-icon>
                </el-tooltip>
              </div>
              <div class="yt-video-meta-block">
                <span class="yt-view-count">{{ formatViews(video.viewCount) }}次观看</span>
                <span class="yt-dot">•</span>
                <span class="yt-publish-time">{{ formatTime(video.createdAt) }}</span>
              </div>
            </div>
            <!-- 更多操作按钮 -->
            <div class="yt-more-actions">
              <el-icon><MoreFilled /></el-icon>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && videos.length === 0" class="empty-state">
        <el-icon :size="64"><VideoCamera /></el-icon>
        <p>暂无视频</p>
      </div>

      <!-- 分页 -->
      <div v-if="total > pageSize" class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 上传视频对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传视频" width="600px">
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="视频文件" required>
          <el-upload
            ref="videoUploadRef"
            :auto-upload="false"
            :limit="1"
            accept="video/*"
            :on-change="handleVideoFileChange"
          >
            <el-button type="primary">选择视频</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 mp4, avi, mov, mkv 等格式，最大500MB</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="缩略图">
          <el-upload
            ref="thumbnailUploadRef"
            :auto-upload="false"
            :limit="1"
            accept="image/*"
            :on-change="handleThumbnailChange"
            list-type="picture"
          >
            <el-button>选择缩略图</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="uploadForm.title" placeholder="请输入视频标题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="uploadForm.description" type="textarea" :rows="3" placeholder="请输入视频描述" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="uploadForm.categoryId" placeholder="请选择分类">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="频道名称">
          <el-input v-model="uploadForm.channelName" placeholder="默认使用您的用户名" />
        </el-form-item>
      </el-form>
      <div v-if="uploadProgress > 0" class="upload-progress">
        <el-progress :percentage="uploadProgress" :status="uploadProgress === 100 ? 'success' : ''" />
      </div>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  VideoPlay, HomeFilled, VideoCamera, Collection, 
  Clock, Files, Timer, Pointer, ArrowRight, CircleCheckFilled, MoreFilled,
  Upload, Loading, Search, Close
} from '@element-plus/icons-vue'
import { getVideoList, getVideoCategories, uploadVideoComplete, getLikedVideos, getMyVideos, searchVideos, getSearchSuggestions, getWatchHistory, getWatchLaterList, clearHistory } from '@/api/video'

const router = useRouter()

const defaultThumbnail = 'https://picsum.photos/seed/default/640/360'

// 状态
const loading = ref(false)
const videos = ref([])
const categories = ref([])
const activeCategory = ref('all')
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const currentView = ref('home') // home, liked, myVideos, history, watchLater
const currentUser = ref(null)

// 上传
const showUploadDialog = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadForm = ref({
  title: '',
  description: '',
  categoryId: null,
  channelName: '',
  videoFile: null,
  thumbnailFile: null,
  duration: '',
  durationSeconds: 0
})

// 搜索相关
const searchKeyword = ref('')
const isSearchMode = ref(false)
const showSuggestions = ref(false)
const suggestions = ref([])
const searchFilters = ref({
  duration: null,
  uploadDate: null,
  sortBy: 'relevance'
})

const durationOptions = [
  { label: '全部', value: null },
  { label: '短视频(<4分钟)', value: 'short' },
  { label: '中等(4-20分钟)', value: 'medium' },
  { label: '长视频(>20分钟)', value: 'long' }
]

const uploadDateOptions = [
  { label: '全部', value: null },
  { label: '今天', value: 'today' },
  { label: '本周', value: 'week' },
  { label: '本月', value: 'month' },
  { label: '今年', value: 'year' }
]

const sortOptions = [
  { label: '相关性', value: 'relevance' },
  { label: '上传日期', value: 'date' },
  { label: '观看次数', value: 'views' },
  { label: '评分', value: 'rating' }
]

const subscriptions = ref([
  { id: 1, name: '武理官方', color: '#ef4444', hasNew: true },
  { id: 2, name: '校团委', color: '#f59e0b', hasNew: true },
  { id: 3, name: '教务处', color: '#10b981', hasNew: false },
  { id: 4, name: '图书馆', color: '#3b82f6', hasNew: false },
  { id: 5, name: '后勤保障', color: '#6366f1', hasNew: true },
  { id: 6, name: '学工部', color: '#8b5cf6', hasNew: false },
])

// 加载分类
const loadCategories = async () => {
  try {
    const res = await getVideoCategories()
    categories.value = res || []
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

// 加载视频列表
const loadVideos = async () => {
  loading.value = true
  try {
    let res
    const params = {
      current: currentPage.value,
      size: pageSize.value
    }
    
    if (isSearchMode.value) {
      // 搜索模式
      res = await searchVideos({
        keyword: searchKeyword.value,
        ...params,
        categoryCode: activeCategory.value !== 'all' ? activeCategory.value : null,
        duration: searchFilters.value.duration,
        uploadDate: searchFilters.value.uploadDate,
        sortBy: searchFilters.value.sortBy
      })
    } else if (currentView.value === 'liked') {
      res = await getLikedVideos(params)
    } else if (currentView.value === 'myVideos') {
      res = await getMyVideos(params)
    } else if (currentView.value === 'history') {
      res = await getWatchHistory(params)
    } else if (currentView.value === 'watchLater') {
      res = await getWatchLaterList(params)
    } else {
      if (activeCategory.value !== 'all') {
        params.categoryCode = activeCategory.value
      }
      res = await getVideoList(params)
    }
    
    videos.value = res.records || []
    total.value = res.total || res.records?.length || 0
  } catch (error) {
    console.error('加载视频失败:', error)
    ElMessage.error('加载视频失败')
  } finally {
    loading.value = false
  }
}

// 搜索相关方法
let searchTimeout = null
const handleSearchInput = async () => {
  if (searchTimeout) clearTimeout(searchTimeout)
  
  if (searchKeyword.value.length >= 2) {
    searchTimeout = setTimeout(async () => {
      try {
        const res = await getSearchSuggestions(searchKeyword.value)
        suggestions.value = res || []
        showSuggestions.value = true
      } catch (error) {
        console.error('获取搜索建议失败')
      }
    }, 300)
  } else {
    suggestions.value = []
    showSuggestions.value = false
  }
}

const handleSearch = () => {
  if (!searchKeyword.value.trim()) {
    clearSearch()
    return
  }
  isSearchMode.value = true
  showSuggestions.value = false
  currentPage.value = 1
  loadVideos()
}

const selectSuggestion = (suggestion) => {
  searchKeyword.value = suggestion
  handleSearch()
}

const clearSearch = () => {
  searchKeyword.value = ''
  isSearchMode.value = false
  showSuggestions.value = false
  suggestions.value = []
  searchFilters.value = { duration: null, uploadDate: null, sortBy: 'relevance' }
  currentPage.value = 1
  loadVideos()
}

const setFilter = (key, value) => {
  searchFilters.value[key] = value
  currentPage.value = 1
  loadVideos()
}

// 跳转到频道
const goToChannel = (userId) => {
  if (userId) {
    router.push(`/channel/${userId}`)
  }
}

// 获取视图标题
const getViewTitle = () => {
  switch (currentView.value) {
    case 'liked': return '点赞的视频'
    case 'myVideos': return '我的视频'
    case 'history': return '观看历史'
    case 'watchLater': return '稍后观看'
    default: return ''
  }
}

// 清空历史记录
const handleClearHistory = async () => {
  try {
    await clearHistory()
    ElMessage.success('历史记录已清空')
    loadVideos()
  } catch (error) {
    ElMessage.error('清空失败')
  }
}

// 切换视图
const switchView = (view) => {
  if (!currentUser.value && view !== 'home') {
    ElMessage.warning('请先登录')
    return
  }
  currentView.value = view
  currentPage.value = 1
  if (view === 'home') {
    activeCategory.value = 'all'
  }
  loadVideos()
}

// 加载当前用户
const loadCurrentUser = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    currentUser.value = JSON.parse(userStr)
  }
}

// 选择分类
const selectCategory = (code) => {
  currentView.value = 'home'
  activeCategory.value = code
  currentPage.value = 1
  loadVideos()
}

// 分页
const handlePageChange = (page) => {
  currentPage.value = page
  loadVideos()
}

// 点击视频 - 跳转到详情页
const handleVideoClick = (video) => {
  router.push(`/video/${video.id}`)
}

// 处理视频文件选择 - 提取视频时长
const handleVideoFileChange = (file) => {
  uploadForm.value.videoFile = file.raw
  
  // 创建临时video元素提取时长
  const video = document.createElement('video')
  video.preload = 'metadata'
  
  video.onloadedmetadata = () => {
    window.URL.revokeObjectURL(video.src)
    const seconds = Math.floor(video.duration)
    uploadForm.value.durationSeconds = seconds
    
    // 格式化为 MM:SS 或 HH:MM:SS
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    const secs = seconds % 60
    
    if (hours > 0) {
      uploadForm.value.duration = `${hours}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
    } else {
      uploadForm.value.duration = `${minutes}:${String(secs).padStart(2, '0')}`
    }
    console.log('视频时长:', uploadForm.value.duration, '秒数:', uploadForm.value.durationSeconds)
  }
  
  video.src = URL.createObjectURL(file.raw)
}

// 处理缩略图选择
const handleThumbnailChange = (file) => {
  uploadForm.value.thumbnailFile = file.raw
}

// 上传视频
const handleUpload = async () => {
  if (!uploadForm.value.videoFile) {
    ElMessage.warning('请选择视频文件')
    return
  }
  if (!uploadForm.value.title) {
    ElMessage.warning('请输入视频标题')
    return
  }
  
  uploading.value = true
  uploadProgress.value = 0
  
  try {
    await uploadVideoComplete({
      video: uploadForm.value.videoFile,
      thumbnail: uploadForm.value.thumbnailFile,
      title: uploadForm.value.title,
      description: uploadForm.value.description,
      categoryId: uploadForm.value.categoryId,
      channelName: uploadForm.value.channelName,
      duration: uploadForm.value.duration,
      durationSeconds: uploadForm.value.durationSeconds
    }, (progressEvent) => {
      uploadProgress.value = Math.round((progressEvent.loaded * 100) / progressEvent.total)
    })
    
    ElMessage.success('视频上传成功')
    showUploadDialog.value = false
    resetUploadForm()
    loadVideos()
  } catch (error) {
    ElMessage.error('上传失败: ' + (error.message || '未知错误'))
  } finally {
    uploading.value = false
  }
}

// 重置上传表单
const resetUploadForm = () => {
  uploadForm.value = {
    title: '',
    description: '',
    categoryId: null,
    channelName: '',
    videoFile: null,
    thumbnailFile: null,
    duration: '',
    durationSeconds: 0
  }
  uploadProgress.value = 0
}

const formatViews = (num) => {
  if (!num) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  }
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

onMounted(() => {
  loadCurrentUser()
  loadCategories()
  loadVideos()
})
</script>


<style scoped>
/* ========== 布局容器 ========== */
.yt-container {
  display: flex;
  background-color: #ffffff;
  min-height: calc(100vh - 60px);
}

/* ========== 侧边栏 ========== */
.yt-sidebar {
  width: 240px;
  flex-shrink: 0;
  padding: 12px 12px;
  overflow-y: auto;
  position: sticky;
  top: 0;
  height: calc(100vh - 60px);
  background-color: #ffffff;
}

.sidebar-section {
  padding: 8px 0;
}

.sidebar-divider {
  height: 1px;
  background-color: #e5e5e5;
  margin: 8px 12px;
}

.sidebar-header {
  padding: 6px 12px;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #0f0f0f;
  cursor: pointer;
  border-radius: 10px;
}

.sidebar-header:hover {
  background-color: #f2f2f2;
}

.sidebar-item {
  display: flex;
  align-items: center;
  padding: 0 12px;
  height: 40px;
  border-radius: 10px;
  cursor: pointer;
  color: #0f0f0f;
  margin-bottom: 2px;
}

.sidebar-item:hover {
  background-color: #f2f2f2;
}

.sidebar-item.active {
  background-color: #f2f2f2;
  font-weight: 500;
}

.sidebar-item .el-icon {
  margin-right: 24px;
  color: #0f0f0f;
}

.sidebar-item span {
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.subscription-item {
  gap: 12px;
}

.subscription-item .el-avatar {
  margin-right: 12px;
  font-size: 12px;
}

.subscription-item .sub-name {
  flex: 1;
}

.new-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background-color: #3ea6ff;
}

/* ========== 主内容区 ========== */
.yt-main {
  flex: 1;
  padding: 0 24px;
  overflow-x: hidden;
}

.page-title {
  padding: 24px 0 16px;
}

.page-title h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #0f0f0f;
}

/* ========== 分类标签栏 ========== */
.yt-chips-bar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.98);
  padding: 12px 0;
  margin-bottom: 24px;
}

.yt-chips-scroll {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
  padding-bottom: 4px;
}

.yt-chips-scroll::-webkit-scrollbar {
  display: none;
}

.yt-chip {
  flex-shrink: 0;
  height: 32px;
  padding: 0 12px;
  border: none;
  border-radius: 8px;
  background-color: #f2f2f2;
  color: #0f0f0f;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  transition: background-color 0.2s;
}

.yt-chip:hover {
  background-color: #e5e5e5;
}

.yt-chip-active {
  background-color: #0f0f0f;
  color: #ffffff;
}

.yt-chip-active:hover {
  background-color: #3f3f3f;
}

/* ========== 加载状态 ========== */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: #606060;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: #606060;
}

/* ========== 视频网格 ========== */
.yt-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 40px 16px;
  margin-bottom: 40px;
}

@media (max-width: 1400px) {
  .yt-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1100px) {
  .yt-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 700px) {
  .yt-grid {
    grid-template-columns: 1fr;
  }
}

.yt-video-renderer {
  cursor: pointer;
}

/* ========== 缩略图 ========== */
.yt-thumbnail {
  position: relative;
  margin-bottom: 12px;
}

.yt-thumbnail-link {
  display: block;
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  background-color: #cccccc;
  aspect-ratio: 16 / 9;
}

.yt-thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.2s ease;
}

.yt-video-renderer:hover .yt-thumbnail-img {
  transform: scale(1.02);
}

.yt-time-status {
  position: absolute;
  bottom: 8px;
  right: 8px;
  padding: 3px 4px;
  background-color: rgba(0, 0, 0, 0.8);
  border-radius: 4px;
}

.yt-time-text {
  color: #fff;
  font-size: 12px;
  font-weight: 500;
}

.yt-thumbnail-hover {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.1);
  opacity: 0;
  transition: opacity 0.2s;
}

.yt-video-renderer:hover .yt-thumbnail-hover {
  opacity: 1;
}

.yt-play-icon {
  font-size: 48px;
  color: #ffffff;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.5));
}

/* ========== 视频元数据 ========== */
.yt-meta {
  display: flex;
  gap: 12px;
  position: relative;
}

.yt-avatar-link {
  flex-shrink: 0;
  margin-top: 4px;
}

.yt-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff0000 0%, #cc0000 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 500;
  font-size: 14px;
}

.yt-details {
  flex: 1;
  min-width: 0;
  padding-right: 24px;
}

.yt-video-title {
  margin: 0 0 4px;
  line-height: 22px;
}

.yt-title-link {
  color: #0f0f0f;
  font-size: 16px;
  font-weight: 500;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.yt-channel-info {
  display: flex;
  align-items: center;
  margin-bottom: 2px;
}

.yt-channel-name {
  color: #606060;
  font-size: 14px;
  margin-right: 4px;
}

.yt-channel-name:hover {
  color: #0f0f0f;
}

.verified-icon {
  color: #606060;
}

.yt-video-meta-block {
  color: #606060;
  font-size: 14px;
}

.yt-dot {
  margin: 0 4px;
}

.yt-more-actions {
  position: absolute;
  top: 0;
  right: -10px;
  opacity: 0;
  transition: opacity 0.2s;
  cursor: pointer;
  padding: 8px;
}

.yt-video-renderer:hover .yt-more-actions {
  opacity: 1;
}

/* ========== 分页 ========== */
.pagination-container {
  display: flex;
  justify-content: center;
  padding: 20px 0 40px;
}

/* ========== 视频播放器 ========== */
.video-player-container {
  width: 100%;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}

.video-player {
  width: 100%;
  max-height: 70vh;
}

.video-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #e5e5e5;
}

.video-stats {
  color: #606060;
  font-size: 14px;
}

.video-stats .dot {
  margin: 0 8px;
}

.video-description {
  padding: 16px 0;
  color: #0f0f0f;
  white-space: pre-wrap;
}

/* ========== 上传进度 ========== */
.upload-progress {
  margin-top: 16px;
}

/* ========== 搜索栏 ========== */
.search-bar-container {
  position: relative;
  margin-bottom: 16px;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  background: #f2f2f2;
  border-radius: 40px;
  padding: 0 16px;
  height: 44px;
  transition: background 0.2s, box-shadow 0.2s;
}

.search-input-wrapper:focus-within {
  background: #ffffff;
  box-shadow: 0 1px 6px rgba(0,0,0,0.1);
  border: 1px solid #e5e5e5;
}

.search-icon {
  color: #606060;
  margin-right: 12px;
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 16px;
  color: #0f0f0f;
  outline: none;
}

.search-input::placeholder {
  color: #909090;
}

.clear-btn {
  background: none;
  border: none;
  color: #606060;
  cursor: pointer;
  padding: 4px;
  margin-right: 8px;
}

.search-btn {
  background: #f2f2f2;
  border: none;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #0f0f0f;
}

.search-btn:hover {
  background: #e5e5e5;
}

/* 搜索建议 */
.search-suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  margin-top: 4px;
  z-index: 100;
  overflow: hidden;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  color: #0f0f0f;
}

.suggestion-item:hover {
  background: #f2f2f2;
}

.suggestion-item .el-icon {
  color: #606060;
}

/* 搜索筛选器 */
.search-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #e5e5e5;
  margin-bottom: 16px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  color: #606060;
  font-size: 14px;
  margin-right: 4px;
}

.filter-btn {
  padding: 6px 12px;
  border: 1px solid #e5e5e5;
  border-radius: 16px;
  background: transparent;
  color: #0f0f0f;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-btn:hover {
  background: #f2f2f2;
}

.filter-btn.active {
  background: #0f0f0f;
  color: #ffffff;
  border-color: #0f0f0f;
}

.page-title {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding: 16px 0;
}

.page-title h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #0f0f0f;
}

.result-count {
  color: #606060;
  font-size: 14px;
}

.clear-history-btn {
  margin-left: auto;
  padding: 8px 16px;
  border: none;
  border-radius: 18px;
  background: #f2f2f2;
  color: #0f0f0f;
  font-size: 14px;
  cursor: pointer;
}

.clear-history-btn:hover {
  background: #e5e5e5;
}
</style>
