<template>
  <div class="yt-container">
    <!-- 左侧侧边栏 -->
    <div class="yt-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <!-- 主要导航 -->
      <div class="sidebar-section">
        <div class="sidebar-item" :class="{ active: currentView === 'home' }" @click="switchView('home')">
          <el-icon :size="24"><HomeFilled /></el-icon>
          <span>首页</span>
        </div>
        <div class="sidebar-item" :class="{ active: currentView === 'shorts' }" @click="switchView('shorts')">
          <el-icon :size="24"><Film /></el-icon>
          <span>Shorts</span>
        </div>
        <div class="sidebar-item" @click="showUploadDialog = true">
          <el-icon :size="24"><Upload /></el-icon>
          <span>上传视频</span>
        </div>
        <div class="sidebar-item" :class="{ active: currentView === 'subscriptions' }" @click="switchView('subscriptions')">
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
        <div v-for="sub in subscriptions" :key="sub.id" class="sidebar-item subscription-item" @click="goToChannel(sub.userId)">
          <el-avatar :size="24" :src="sub.avatar" :style="{ background: sub.color }">{{ sub.name[0] }}</el-avatar>
          <span class="sub-name">{{ sub.name }}</span>
          <span v-if="sub.hasNew" class="new-dot"></span>
        </div>
        <div v-if="subscriptions.length === 0" class="sidebar-empty">
          <span>暂无订阅</span>
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
            @focus="showSuggestions = suggestions.length > 0"
            @blur="hideSuggestionsDelayed"
          />
          <button v-if="searchKeyword" class="clear-btn" @click="clearSearch">
            <el-icon><Close /></el-icon>
          </button>
          <button class="search-btn" @click="handleSearch">
            <el-icon><Search /></el-icon>
          </button>
          <button class="voice-btn" title="语音搜索">
            <el-icon><Microphone /></el-icon>
          </button>
        </div>
        <!-- 搜索建议 -->
        <div v-if="showSuggestions && suggestions.length > 0" class="search-suggestions">
          <div 
            v-for="(suggestion, index) in suggestions" 
            :key="index"
            class="suggestion-item"
            @mousedown="selectSuggestion(suggestion)"
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
      <div v-if="currentView !== 'home' && currentView !== 'shorts' && !isSearchMode" class="page-title">
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
        <button class="chips-scroll-btn left" @click="scrollChips('left')" v-show="canScrollLeft">
          <el-icon><ArrowLeft /></el-icon>
        </button>
        <div class="yt-chips-scroll" ref="chipsContainer">
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
        <button class="chips-scroll-btn right" @click="scrollChips('right')" v-show="canScrollRight">
          <el-icon><ArrowRight /></el-icon>
        </button>
      </div>

      <!-- Shorts 区域 -->
      <div v-if="currentView === 'home' && shorts.length > 0 && !isSearchMode" class="shorts-section">
        <div class="section-header">
          <el-icon class="shorts-icon"><Film /></el-icon>
          <h3>Shorts</h3>
        </div>
        <div class="shorts-grid">
          <div 
            v-for="short in shorts.slice(0, 6)" 
            :key="short.id" 
            class="short-card"
            @click="handleVideoClick(short)"
          >
            <div class="short-thumbnail">
              <img :src="short.thumbnail || defaultThumbnail" :alt="short.title" />
              <div class="short-overlay">
                <el-icon class="play-icon"><VideoPlay /></el-icon>
              </div>
            </div>
            <div class="short-info">
              <h4>{{ short.title }}</h4>
              <span>{{ formatViews(short.viewCount) }}次观看</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Shorts 视图 -->
      <div v-if="currentView === 'shorts'" class="shorts-view">
        <div class="shorts-feed">
          <div 
            v-for="short in videos" 
            :key="short.id" 
            class="short-item"
            @click="handleVideoClick(short)"
          >
            <div class="short-player">
              <img :src="short.thumbnail || defaultThumbnail" :alt="short.title" />
              <div class="short-actions">
                <button class="short-action-btn">
                  <el-icon><Pointer /></el-icon>
                  <span>{{ formatViews(short.likeCount) }}</span>
                </button>
                <button class="short-action-btn">
                  <el-icon><ChatDotRound /></el-icon>
                  <span>{{ short.commentCount || 0 }}</span>
                </button>
                <button class="short-action-btn">
                  <el-icon><Share /></el-icon>
                  <span>分享</span>
                </button>
              </div>
            </div>
            <div class="short-meta">
              <div class="short-channel">
                <div class="channel-avatar-small">{{ (short.channelName || '?')[0] }}</div>
                <span>@{{ short.channelName || short.author?.realName }}</span>
              </div>
              <h4>{{ short.title }}</h4>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <!-- 视频网格 -->
      <div v-else-if="currentView !== 'shorts'" class="yt-grid">
        <div 
          v-for="video in videos" 
          :key="video.id" 
          class="yt-video-renderer"
          @click="handleVideoClick(video)"
          @mouseenter="startPreview(video)"
          @mouseleave="stopPreview(video)"
        >
          <!-- 缩略图容器 -->
          <div class="yt-thumbnail">
            <a class="yt-thumbnail-link">
              <!-- 预览视频 -->
              <video 
                v-if="video.isPreviewPlaying && video.videoUrl"
                :src="video.videoUrl"
                class="preview-video"
                muted
                autoplay
                loop
                @loadeddata="onPreviewLoaded(video)"
              ></video>
              <img 
                v-show="!video.isPreviewPlaying"
                :src="video.thumbnail || defaultThumbnail" 
                :alt="video.title" 
                class="yt-thumbnail-img" 
              />
              <!-- 时长标签 -->
              <div class="yt-time-status" v-show="!video.isPreviewPlaying">
                <span class="yt-time-text">{{ video.duration || '00:00' }}</span>
              </div>
              <!-- 进度条 (如果有观看进度) -->
              <div v-if="video.watchProgress > 0" class="watch-progress-bar">
                <div class="progress-fill" :style="{ width: video.watchProgress + '%' }"></div>
              </div>
              <!-- 悬停遮罩 -->
              <div class="yt-thumbnail-hover" v-show="!video.isPreviewPlaying">
                <el-icon class="yt-play-icon"><VideoPlay /></el-icon>
              </div>
              <!-- 稍后观看按钮 -->
              <button class="watch-later-btn" @click.stop="quickWatchLater(video)" title="稍后观看">
                <el-icon><Clock /></el-icon>
              </button>
              <!-- 添加到队列按钮 -->
              <button class="add-queue-btn" @click.stop="addToQueue(video)" title="添加到队列">
                <el-icon><List /></el-icon>
              </button>
            </a>
          </div>
          
          <!-- 视频元数据 -->
          <div class="yt-meta">
            <a class="yt-avatar-link" @click.stop="goToChannel(video.authorId)">
              <div class="yt-avatar" :style="{ background: getAvatarColor(video.authorId) }">
                <img v-if="video.author?.avatar" :src="getAvatarUrl(video.author.avatar)" />
                <span v-else>{{ (video.channelName || video.author?.realName || '未知')[0] }}</span>
              </div>
            </a>
            <div class="yt-details">
              <h3 class="yt-video-title">
                <a class="yt-title-link">{{ video.title }}</a>
              </h3>
              <div class="yt-channel-info">
                <a class="yt-channel-name" @click.stop="goToChannel(video.authorId)">{{ video.channelName || video.author?.realName || '未知频道' }}</a>
                <el-tooltip content="已验证" placement="top" :show-after="500">
                  <el-icon v-if="video.author?.verified" class="verified-icon" :size="14"><CircleCheckFilled /></el-icon>
                </el-tooltip>
              </div>
              <div class="yt-video-meta-block">
                <span class="yt-view-count">{{ formatViews(video.viewCount) }}次观看</span>
                <span class="yt-dot">•</span>
                <span class="yt-publish-time">{{ formatTime(video.createdAt) }}</span>
              </div>
            </div>
            <!-- 更多操作按钮 -->
            <el-dropdown trigger="click" @click.stop>
              <div class="yt-more-actions">
                <el-icon><MoreFilled /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="quickWatchLater(video)">
                    <el-icon><Clock /></el-icon> 稍后观看
                  </el-dropdown-item>
                  <el-dropdown-item @click="addToQueue(video)">
                    <el-icon><List /></el-icon> 添加到队列
                  </el-dropdown-item>
                  <el-dropdown-item @click="shareVideo(video)">
                    <el-icon><Share /></el-icon> 分享
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="notInterested(video)">
                    <el-icon><CircleClose /></el-icon> 不感兴趣
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && videos.length === 0 && currentView !== 'shorts'" class="empty-state">
        <el-icon :size="64"><VideoCamera /></el-icon>
        <p>{{ getEmptyMessage() }}</p>
        <button v-if="currentView === 'myVideos'" class="upload-btn" @click="showUploadDialog = true">
          上传视频
        </button>
      </div>

      <!-- 分页 -->
      <div v-if="total > pageSize && currentView !== 'shorts'" class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 迷你播放器 -->
    <transition name="mini-player">
      <div v-if="miniPlayer.show" class="mini-player" :class="{ expanded: miniPlayer.expanded }">
        <div class="mini-player-header" @click="miniPlayer.expanded = !miniPlayer.expanded">
          <span>{{ miniPlayer.video?.title }}</span>
          <div class="mini-player-controls">
            <button @click.stop="toggleMiniPlayerPlay">
              <el-icon v-if="miniPlayer.playing"><VideoPause /></el-icon>
              <el-icon v-else><VideoPlay /></el-icon>
            </button>
            <button @click.stop="closeMiniPlayer">
              <el-icon><Close /></el-icon>
            </button>
          </div>
        </div>
        <div class="mini-player-content" v-show="miniPlayer.expanded">
          <video 
            ref="miniPlayerVideo"
            :src="miniPlayer.video?.videoUrl"
            class="mini-video"
            @timeupdate="onMiniPlayerTimeUpdate"
          ></video>
          <div class="mini-progress">
            <div class="mini-progress-fill" :style="{ width: miniPlayer.progress + '%' }"></div>
          </div>
        </div>
      </div>
    </transition>

    <!-- 上传视频对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传视频" width="700px" class="upload-dialog">
      <div class="upload-steps">
        <div class="step" :class="{ active: uploadStep === 1, done: uploadStep > 1 }">
          <span class="step-num">1</span>
          <span>选择文件</span>
        </div>
        <div class="step-line"></div>
        <div class="step" :class="{ active: uploadStep === 2, done: uploadStep > 2 }">
          <span class="step-num">2</span>
          <span>填写信息</span>
        </div>
        <div class="step-line"></div>
        <div class="step" :class="{ active: uploadStep === 3 }">
          <span class="step-num">3</span>
          <span>上传完成</span>
        </div>
      </div>

      <!-- 步骤1: 选择文件 -->
      <div v-if="uploadStep === 1" class="upload-step-content">
        <div class="upload-dropzone" 
          @dragover.prevent="dragOver = true" 
          @dragleave="dragOver = false"
          @drop.prevent="handleDrop"
          :class="{ 'drag-over': dragOver }"
        >
          <el-icon :size="80" class="upload-icon"><Upload /></el-icon>
          <p class="upload-text">拖放视频文件到此处上传</p>
          <p class="upload-hint">或者</p>
          <el-upload
            ref="videoUploadRef"
            :auto-upload="false"
            :show-file-list="false"
            accept="video/*"
            :on-change="handleVideoFileChange"
          >
            <el-button type="primary" size="large">选择文件</el-button>
          </el-upload>
          <p class="upload-tip">支持 MP4, AVI, MOV, MKV 等格式，最大 500MB</p>
        </div>
      </div>

      <!-- 步骤2: 填写信息 -->
      <div v-if="uploadStep === 2" class="upload-step-content">
        <div class="upload-form-layout">
          <div class="upload-preview">
            <video v-if="videoPreviewUrl" :src="videoPreviewUrl" controls class="preview-video-player"></video>
            <div v-else class="preview-placeholder">
              <el-icon :size="48"><VideoCamera /></el-icon>
            </div>
            <div class="video-file-info">
              <span>{{ uploadForm.videoFile?.name }}</span>
              <span>{{ formatFileSize(uploadForm.videoFile?.size) }}</span>
            </div>
          </div>
          <el-form :model="uploadForm" label-position="top" class="upload-form">
            <el-form-item label="标题" required>
              <el-input 
                v-model="uploadForm.title" 
                placeholder="给视频起个吸引人的标题"
                maxlength="100"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="描述">
              <el-input 
                v-model="uploadForm.description" 
                type="textarea" 
                :rows="4" 
                placeholder="介绍一下你的视频内容"
                maxlength="5000"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="缩略图">
              <div class="thumbnail-options">
                <div 
                  v-for="(thumb, index) in generatedThumbnails" 
                  :key="index"
                  class="thumbnail-option"
                  :class="{ selected: selectedThumbnail === index }"
                  @click="selectedThumbnail = index"
                >
                  <img :src="thumb" />
                </div>
                <el-upload
                  :auto-upload="false"
                  :show-file-list="false"
                  accept="image/*"
                  :on-change="handleThumbnailChange"
                >
                  <div class="thumbnail-option upload-thumb">
                    <el-icon><Plus /></el-icon>
                    <span>上传</span>
                  </div>
                </el-upload>
              </div>
            </el-form-item>
            <div class="form-row">
              <el-form-item label="分类">
                <el-select v-model="uploadForm.categoryId" placeholder="选择分类">
                  <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="频道名称">
                <el-input v-model="uploadForm.channelName" placeholder="默认使用您的用户名" />
              </el-form-item>
            </div>
          </el-form>
        </div>
      </div>

      <!-- 步骤3: 上传中/完成 -->
      <div v-if="uploadStep === 3" class="upload-step-content">
        <div v-if="uploading" class="uploading-state">
          <div class="upload-progress-ring">
            <svg viewBox="0 0 100 100">
              <circle class="progress-bg" cx="50" cy="50" r="45" />
              <circle class="progress-fill" cx="50" cy="50" r="45" :style="{ strokeDashoffset: 283 - (283 * uploadProgress / 100) }" />
            </svg>
            <span class="progress-text">{{ uploadProgress }}%</span>
          </div>
          <p>正在上传视频...</p>
          <p class="upload-speed">{{ uploadSpeed }}</p>
        </div>
        <div v-else class="upload-success">
          <el-icon :size="64" class="success-icon"><CircleCheckFilled /></el-icon>
          <h3>上传成功！</h3>
          <p>您的视频已上传，正在等待审核</p>
          <div class="success-actions">
            <el-button @click="goToMyVideos">查看我的视频</el-button>
            <el-button type="primary" @click="uploadAnother">继续上传</el-button>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button v-if="uploadStep > 1 && uploadStep < 3" @click="uploadStep--">上一步</el-button>
          <el-button v-if="uploadStep === 1" @click="showUploadDialog = false">取消</el-button>
          <el-button v-if="uploadStep === 2" type="primary" :loading="uploading" @click="handleUpload">
            开始上传
          </el-button>
          <el-button v-if="uploadStep === 3 && !uploading" type="primary" @click="showUploadDialog = false">
            完成
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>


<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  VideoPlay, VideoPause, HomeFilled, VideoCamera, Collection, 
  Clock, Files, Timer, Pointer, ArrowRight, ArrowLeft, CircleCheckFilled, MoreFilled,
  Upload, Loading, Search, Close, Film, Share, ChatDotRound, List, CircleClose, Plus, Microphone
} from '@element-plus/icons-vue'
import { 
  getVideoList, getVideoCategories, uploadVideoComplete, getLikedVideos, 
  getMyVideos, searchVideos, getSearchSuggestions, getWatchHistory, 
  getWatchLaterList, clearHistory, toggleWatchLater 
} from '@/api/video'

const router = useRouter()

const defaultThumbnail = 'https://picsum.photos/seed/default/640/360'

// 状态
const loading = ref(false)
const videos = ref([])
const shorts = ref([])
const categories = ref([])
const activeCategory = ref('all')
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const currentView = ref('home')
const currentUser = ref(null)
const sidebarCollapsed = ref(false)

// 分类滚动
const chipsContainer = ref(null)
const canScrollLeft = ref(false)
const canScrollRight = ref(true)

// 上传
const showUploadDialog = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadSpeed = ref('')
const uploadStep = ref(1)
const dragOver = ref(false)
const videoPreviewUrl = ref('')
const generatedThumbnails = ref([])
const selectedThumbnail = ref(0)
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

// 迷你播放器
const miniPlayer = ref({
  show: false,
  expanded: true,
  video: null,
  playing: false,
  progress: 0
})
const miniPlayerVideo = ref(null)

// 预览相关
let previewTimeout = null

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

const subscriptions = ref([])

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
    } else if (currentView.value === 'shorts') {
      params.maxDuration = 60
      res = await getVideoList(params)
    } else {
      if (activeCategory.value !== 'all') {
        params.categoryCode = activeCategory.value
      }
      res = await getVideoList(params)
    }
    
    videos.value = (res.records || []).map(v => ({ ...v, isPreviewPlaying: false }))
    total.value = res.total || res.records?.length || 0
    
    // 加载 Shorts (首页时)
    if (currentView.value === 'home' && !isSearchMode.value) {
      loadShorts()
    }
  } catch (error) {
    console.error('加载视频失败:', error)
    ElMessage.error('加载视频失败')
  } finally {
    loading.value = false
  }
}

// 加载 Shorts
const loadShorts = async () => {
  try {
    const res = await getVideoList({ current: 1, size: 6, maxDuration: 60 })
    shorts.value = res.records || []
  } catch (error) {
    console.error('加载Shorts失败')
  }
}

// 视频预览
const startPreview = (video) => {
  if (!video.videoUrl) return
  previewTimeout = setTimeout(() => {
    video.isPreviewPlaying = true
  }, 500)
}

const stopPreview = (video) => {
  if (previewTimeout) {
    clearTimeout(previewTimeout)
    previewTimeout = null
  }
  video.isPreviewPlaying = false
}

const onPreviewLoaded = (video) => {
  // 预览加载完成
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

const hideSuggestionsDelayed = () => {
  setTimeout(() => {
    showSuggestions.value = false
  }, 200)
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

// 分类滚动
const scrollChips = (direction) => {
  if (!chipsContainer.value) return
  const scrollAmount = 200
  if (direction === 'left') {
    chipsContainer.value.scrollLeft -= scrollAmount
  } else {
    chipsContainer.value.scrollLeft += scrollAmount
  }
  updateScrollButtons()
}

const updateScrollButtons = () => {
  if (!chipsContainer.value) return
  canScrollLeft.value = chipsContainer.value.scrollLeft > 0
  canScrollRight.value = chipsContainer.value.scrollLeft < chipsContainer.value.scrollWidth - chipsContainer.value.clientWidth
}

// 跳转到频道
const goToChannel = (userId) => {
  if (userId) {
    router.push(`/channel/${userId}`)
  }
}

// 获取视图标题
const getViewTitle = () => {
  const titles = {
    liked: '点赞的视频',
    myVideos: '我的视频',
    history: '观看历史',
    watchLater: '稍后观看',
    subscriptions: '订阅内容',
    shorts: 'Shorts'
  }
  return titles[currentView.value] || ''
}

// 获取空状态消息
const getEmptyMessage = () => {
  const messages = {
    liked: '还没有点赞的视频',
    myVideos: '还没有上传视频',
    history: '暂无观看历史',
    watchLater: '稍后观看列表为空',
    subscriptions: '还没有订阅的频道',
    home: '暂无视频'
  }
  return messages[currentView.value] || '暂无视频'
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
  if (!currentUser.value && view !== 'home' && view !== 'shorts') {
    ElMessage.warning('请先登录')
    return
  }
  currentView.value = view
  currentPage.value = 1
  isSearchMode.value = false
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
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 点击视频
const handleVideoClick = (video) => {
  router.push(`/video/${video.id}`)
}

// 快速添加稍后观看
const quickWatchLater = async (video) => {
  if (!currentUser.value) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await toggleWatchLater(video.id)
    ElMessage.success(res ? '已添加到稍后观看' : '已从稍后观看移除')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 添加到队列
const addToQueue = (video) => {
  ElMessage.success('已添加到播放队列')
}

// 分享视频
const shareVideo = (video) => {
  const url = `${window.location.origin}/video/${video.id}`
  navigator.clipboard.writeText(url)
  ElMessage.success('链接已复制')
}

// 不感兴趣
const notInterested = (video) => {
  videos.value = videos.value.filter(v => v.id !== video.id)
  ElMessage.success('已标记为不感兴趣')
}

// 迷你播放器
const toggleMiniPlayerPlay = () => {
  if (!miniPlayerVideo.value) return
  if (miniPlayer.value.playing) {
    miniPlayerVideo.value.pause()
  } else {
    miniPlayerVideo.value.play()
  }
  miniPlayer.value.playing = !miniPlayer.value.playing
}

const closeMiniPlayer = () => {
  miniPlayer.value.show = false
  miniPlayer.value.video = null
  if (miniPlayerVideo.value) {
    miniPlayerVideo.value.pause()
  }
}

const onMiniPlayerTimeUpdate = () => {
  if (!miniPlayerVideo.value) return
  const duration = miniPlayerVideo.value.duration
  const currentTime = miniPlayerVideo.value.currentTime
  miniPlayer.value.progress = (currentTime / duration) * 100
}

// 上传相关
const handleDrop = (e) => {
  dragOver.value = false
  const files = e.dataTransfer.files
  if (files.length > 0 && files[0].type.startsWith('video/')) {
    processVideoFile(files[0])
  }
}

const handleVideoFileChange = (file) => {
  processVideoFile(file.raw)
}

const processVideoFile = (file) => {
  uploadForm.value.videoFile = file
  uploadForm.value.title = file.name.replace(/\.[^/.]+$/, '')
  
  // 创建预览URL
  videoPreviewUrl.value = URL.createObjectURL(file)
  
  // 提取视频时长和生成缩略图
  const video = document.createElement('video')
  video.preload = 'metadata'
  video.onloadedmetadata = () => {
    const seconds = Math.floor(video.duration)
    uploadForm.value.durationSeconds = seconds
    
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    const secs = seconds % 60
    
    if (hours > 0) {
      uploadForm.value.duration = `${hours}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
    } else {
      uploadForm.value.duration = `${minutes}:${String(secs).padStart(2, '0')}`
    }
    
    // 生成缩略图
    generateThumbnails(video)
  }
  video.src = URL.createObjectURL(file)
  
  uploadStep.value = 2
}

const generateThumbnails = (video) => {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  canvas.width = 320
  canvas.height = 180
  
  const times = [0.1, 0.3, 0.5].map(t => video.duration * t)
  generatedThumbnails.value = []
  
  let index = 0
  const captureFrame = () => {
    if (index >= times.length) return
    video.currentTime = times[index]
  }
  
  video.onseeked = () => {
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
    generatedThumbnails.value.push(canvas.toDataURL('image/jpeg'))
    index++
    if (index < times.length) {
      video.currentTime = times[index]
    }
  }
  
  captureFrame()
}

const handleThumbnailChange = (file) => {
  uploadForm.value.thumbnailFile = file.raw
  const url = URL.createObjectURL(file.raw)
  generatedThumbnails.value.push(url)
  selectedThumbnail.value = generatedThumbnails.value.length - 1
}

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
  uploadStep.value = 3
  uploadProgress.value = 0
  
  try {
    // 获取选中的缩略图
    let thumbnailFile = uploadForm.value.thumbnailFile
    if (!thumbnailFile && generatedThumbnails.value[selectedThumbnail.value]) {
      // 将 base64 转为 File
      const dataUrl = generatedThumbnails.value[selectedThumbnail.value]
      const res = await fetch(dataUrl)
      const blob = await res.blob()
      thumbnailFile = new File([blob], 'thumbnail.jpg', { type: 'image/jpeg' })
    }
    
    await uploadVideoComplete({
      video: uploadForm.value.videoFile,
      thumbnail: thumbnailFile,
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
  } catch (error) {
    ElMessage.error('上传失败: ' + (error.message || '未知错误'))
    uploadStep.value = 2
  } finally {
    uploading.value = false
  }
}

const goToMyVideos = () => {
  showUploadDialog.value = false
  switchView('myVideos')
}

const uploadAnother = () => {
  resetUploadForm()
  uploadStep.value = 1
}

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
  videoPreviewUrl.value = ''
  generatedThumbnails.value = []
  selectedThumbnail.value = 0
}

// 工具函数
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

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const getAvatarUrl = (url) => {
  if (!url) return ''
  return url.startsWith('http') ? url : `http://localhost:8080${url}`
}

const getAvatarColor = (id) => {
  const colors = ['#ff0000', '#ff4500', '#ff6347', '#ff7f50', '#ffa500', '#3b82f6', '#8b5cf6', '#10b981', '#f59e0b']
  return colors[(id || 0) % colors.length]
}

onMounted(() => {
  loadCurrentUser()
  loadCategories()
  loadVideos()
  
  // 监听分类滚动
  nextTick(() => {
    if (chipsContainer.value) {
      chipsContainer.value.addEventListener('scroll', updateScrollButtons)
      updateScrollButtons()
    }
  })
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
  width: 280px;
  flex-shrink: 0;
  padding: 16px;
  overflow-y: auto;
  position: sticky;
  top: 0;
  height: calc(100vh - 60px);
  background-color: #ffffff;
  transition: width 0.2s;
}

.yt-sidebar.collapsed {
  width: 72px;
}

.yt-sidebar.collapsed .sidebar-item span,
.yt-sidebar.collapsed .sidebar-header,
.yt-sidebar.collapsed .sub-name {
  display: none;
}

.sidebar-section {
  padding: 8px 0;
}

.sidebar-divider {
  height: 1px;
  background-color: rgba(0, 0, 0, 0.1);
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
  padding: 0 16px;
  height: 44px;
  border-radius: 10px;
  cursor: pointer;
  color: #0f0f0f;
  margin-bottom: 4px;
  transition: background-color 0.2s;
  font-size: 15px;
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

.sidebar-empty {
  padding: 12px;
  color: #606060;
  font-size: 13px;
}

/* ========== 主内容区 ========== */
.yt-main {
  flex: 1;
  padding: 0 32px;
  overflow-x: hidden;
  max-width: calc(100% - 280px);
}

/* ========== 搜索栏 ========== */
.search-bar-container {
  position: relative;
  max-width: 640px;
  margin: 16px auto;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  background: #f2f2f2;
  border-radius: 40px;
  padding: 0 4px 0 16px;
  height: 44px;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.search-input-wrapper:focus-within {
  background: #ffffff;
  box-shadow: 0 1px 6px rgba(0,0,0,0.1);
  border-color: #065fd4;
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

.clear-btn, .search-btn, .voice-btn {
  background: none;
  border: none;
  color: #606060;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.clear-btn:hover, .search-btn:hover, .voice-btn:hover {
  background: #e5e5e5;
}

.search-btn {
  background: #f2f2f2;
  width: 36px;
  height: 36px;
  margin-left: 4px;
}

.voice-btn {
  margin-left: 8px;
}

.search-suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #ffffff;
  border-radius: 12px;
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
  transition: background 0.15s;
}

.suggestion-item:hover {
  background: #f2f2f2;
}

.suggestion-item .el-icon {
  color: #606060;
}

/* ========== 搜索筛选器 ========== */
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

/* ========== 页面标题 ========== */
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
  transition: background 0.2s;
}

.clear-history-btn:hover {
  background: #e5e5e5;
}

/* ========== 分类标签栏 ========== */
.yt-chips-bar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.98);
  padding: 12px 0;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.chips-scroll-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  z-index: 1;
}

.chips-scroll-btn:hover {
  background: #f2f2f2;
}

.yt-chips-scroll {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
  padding: 4px 0;
  scroll-behavior: smooth;
}

.yt-chips-scroll::-webkit-scrollbar {
  display: none;
}

.yt-chip {
  flex-shrink: 0;
  height: 36px;
  padding: 0 16px;
  border: none;
  border-radius: 8px;
  background-color: #f2f2f2;
  color: #0f0f0f;
  font-size: 15px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s;
}

.yt-chip:hover {
  background-color: #e5e5e5;
}

.yt-chip-active {
  background-color: #0f0f0f;
  color: #ffffff;
}

.yt-chip-active:hover {
  background-color: #272727;
}

/* ========== Shorts 区域 ========== */
.shorts-section {
  margin-bottom: 32px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #0f0f0f;
}

.shorts-icon {
  color: #ff0000;
  font-size: 24px;
}

.shorts-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 20px;
}

@media (max-width: 1600px) {
  .shorts-grid { grid-template-columns: repeat(5, 1fr); }
}

@media (max-width: 1400px) {
  .shorts-grid { grid-template-columns: repeat(4, 1fr); }
}

@media (max-width: 1100px) {
  .shorts-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 800px) {
  .shorts-grid { grid-template-columns: repeat(2, 1fr); }
}

.short-card {
  cursor: pointer;
}

.short-thumbnail {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  aspect-ratio: 9 / 16;
  background: #000;
}

.short-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.short-card:hover .short-thumbnail img {
  transform: scale(1.05);
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

.short-overlay .play-icon {
  font-size: 48px;
  color: #fff;
}

.short-info {
  padding: 8px 4px;
}

.short-info h4 {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 500;
  color: #0f0f0f;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.short-info span {
  font-size: 12px;
  color: #606060;
}

/* Shorts 视图 */
.shorts-view {
  max-width: 400px;
  margin: 0 auto;
}

.shorts-feed {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.short-item {
  cursor: pointer;
}

.short-player {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  aspect-ratio: 9 / 16;
  background: #000;
}

.short-player img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.short-actions {
  position: absolute;
  right: 12px;
  bottom: 80px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.short-action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: #fff;
  cursor: pointer;
}

.short-action-btn .el-icon {
  font-size: 28px;
}

.short-action-btn span {
  font-size: 12px;
}

.short-meta {
  padding: 12px 4px;
}

.short-channel {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.channel-avatar-small {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #ff0000;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.short-meta h4 {
  margin: 0;
  font-size: 14px;
  color: #0f0f0f;
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

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 3px solid #f2f2f2;
  border-top-color: #ff0000;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: #606060;
}

.upload-btn {
  margin-top: 16px;
  padding: 10px 24px;
  border: none;
  border-radius: 20px;
  background: #065fd4;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}

/* ========== 视频网格 ========== */
.yt-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 32px 20px;
  margin-bottom: 40px;
}

@media (max-width: 1800px) {
  .yt-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 1400px) {
  .yt-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 1100px) {
  .yt-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 700px) {
  .yt-grid { grid-template-columns: 1fr; }
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
  background-color: #000;
  aspect-ratio: 16 / 9;
}

.yt-thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.preview-video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
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

.watch-progress-bar {
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

.yt-thumbnail-hover {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.2);
  opacity: 0;
  transition: opacity 0.2s;
}

.yt-video-renderer:hover .yt-thumbnail-hover {
  opacity: 1;
}

.yt-play-icon {
  font-size: 56px;
  color: #ffffff;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.5));
}

.watch-later-btn, .add-queue-btn {
  position: absolute;
  top: 8px;
  background: rgba(0,0,0,0.8);
  border: none;
  color: #fff;
  width: 32px;
  height: 32px;
  border-radius: 4px;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.watch-later-btn {
  right: 8px;
}

.add-queue-btn {
  right: 48px;
}

.yt-video-renderer:hover .watch-later-btn,
.yt-video-renderer:hover .add-queue-btn {
  opacity: 1;
}

.watch-later-btn:hover, .add-queue-btn:hover {
  background: rgba(0,0,0,0.95);
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
  overflow: hidden;
}

.yt-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  transition: color 0.2s;
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
  border-radius: 50%;
}

.yt-video-renderer:hover .yt-more-actions {
  opacity: 1;
}

.yt-more-actions:hover {
  background: #f2f2f2;
}

/* ========== 分页 ========== */
.pagination-container {
  display: flex;
  justify-content: center;
  padding: 20px 0 40px;
}

/* ========== 迷你播放器 ========== */
.mini-player {
  position: fixed;
  bottom: 24px;
  right: 24px;
  width: 400px;
  background: #0f0f0f;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0,0,0,0.3);
  z-index: 1000;
}

.mini-player-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #272727;
  cursor: pointer;
}

.mini-player-header span {
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.mini-player-controls {
  display: flex;
  gap: 8px;
}

.mini-player-controls button {
  background: none;
  border: none;
  color: #fff;
  cursor: pointer;
  padding: 4px;
}

.mini-player-content {
  position: relative;
}

.mini-video {
  width: 100%;
  aspect-ratio: 16 / 9;
  display: block;
}

.mini-progress {
  height: 3px;
  background: rgba(255,255,255,0.3);
}

.mini-progress-fill {
  height: 100%;
  background: #ff0000;
  transition: width 0.1s;
}

.mini-player-enter-active,
.mini-player-leave-active {
  transition: all 0.3s ease;
}

.mini-player-enter-from,
.mini-player-leave-to {
  opacity: 0;
  transform: translateY(100px);
}

/* ========== 上传对话框 ========== */
.upload-dialog :deep(.el-dialog__body) {
  padding: 0 24px 24px;
}

.upload-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 0;
  margin-bottom: 24px;
  border-bottom: 1px solid #e5e5e5;
}

.step {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909090;
}

.step.active {
  color: #0f0f0f;
}

.step.done {
  color: #10b981;
}

.step-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #f2f2f2;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
}

.step.active .step-num {
  background: #0f0f0f;
  color: #fff;
}

.step.done .step-num {
  background: #10b981;
  color: #fff;
}

.step-line {
  width: 60px;
  height: 2px;
  background: #e5e5e5;
  margin: 0 16px;
}

.upload-step-content {
  min-height: 300px;
}

.upload-dropzone {
  border: 2px dashed #e5e5e5;
  border-radius: 12px;
  padding: 60px 40px;
  text-align: center;
  transition: all 0.2s;
}

.upload-dropzone.drag-over {
  border-color: #065fd4;
  background: rgba(6, 95, 212, 0.05);
}

.upload-icon {
  color: #909090;
  margin-bottom: 16px;
}

.upload-text {
  font-size: 18px;
  color: #0f0f0f;
  margin: 0 0 8px;
}

.upload-hint {
  color: #909090;
  margin: 16px 0;
}

.upload-tip {
  color: #909090;
  font-size: 12px;
  margin-top: 16px;
}

.upload-form-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
}

.upload-preview {
  background: #f9f9f9;
  border-radius: 12px;
  overflow: hidden;
}

.preview-video-player {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #000;
}

.preview-placeholder {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #e5e5e5;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909090;
}

.video-file-info {
  padding: 12px;
  font-size: 12px;
  color: #606060;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.upload-form {
  flex: 1;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.thumbnail-options {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.thumbnail-option {
  width: 120px;
  height: 68px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.2s;
}

.thumbnail-option.selected {
  border-color: #065fd4;
}

.thumbnail-option img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-thumb {
  background: #f2f2f2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #606060;
  font-size: 12px;
}

/* ========== 上传进度 ========== */
.uploading-state, .upload-success {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  text-align: center;
}

.upload-progress-ring {
  position: relative;
  width: 120px;
  height: 120px;
  margin-bottom: 24px;
}

.upload-progress-ring svg {
  transform: rotate(-90deg);
}

.upload-progress-ring .progress-bg {
  fill: none;
  stroke: #f2f2f2;
  stroke-width: 8;
}

.upload-progress-ring .progress-fill {
  fill: none;
  stroke: #065fd4;
  stroke-width: 8;
  stroke-linecap: round;
  stroke-dasharray: 283;
  transition: stroke-dashoffset 0.3s;
}

.progress-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 600;
  color: #0f0f0f;
}

.upload-speed {
  color: #606060;
  font-size: 14px;
}

.success-icon {
  color: #10b981;
  margin-bottom: 16px;
}

.upload-success h3 {
  margin: 0 0 8px;
  font-size: 20px;
  color: #0f0f0f;
}

.upload-success p {
  color: #606060;
  margin: 0 0 24px;
}

.success-actions {
  display: flex;
  gap: 12px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* ========== 响应式 ========== */
@media (max-width: 900px) {
  .yt-sidebar {
    display: none;
  }
  
  .upload-form-layout {
    grid-template-columns: 1fr;
  }
  
  .mini-player {
    width: calc(100% - 32px);
    left: 16px;
    right: 16px;
  }
}
</style>
