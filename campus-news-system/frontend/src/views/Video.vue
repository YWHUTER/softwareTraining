<template>
  <div class="yt-container">
    <!-- 左侧侧边栏 -->
    <div class="yt-sidebar">
      <!-- 主要导航 -->
      <div class="sidebar-section">
        <div class="sidebar-item active">
          <el-icon :size="24"><HomeFilled /></el-icon>
          <span>首页</span>
        </div>
        <div class="sidebar-item">
          <el-icon :size="24"><VideoCamera /></el-icon>
          <span>Shorts</span>
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
        <div class="sidebar-item">
          <el-icon :size="24"><Clock /></el-icon>
          <span>历史记录</span>
        </div>
        <div class="sidebar-item">
          <el-icon :size="24"><Files /></el-icon>
          <span>播放列表</span>
        </div>
        <div class="sidebar-item">
          <el-icon :size="24"><Timer /></el-icon>
          <span>稍后观看</span>
        </div>
        <div class="sidebar-item">
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
      <!-- 分类筛选栏 -->
      <div class="yt-chips-bar">
        <div class="yt-chips-scroll">
          <button 
            v-for="cat in categories" 
            :key="cat.id"
            class="yt-chip"
            :class="{ 'yt-chip-active': activeCategory === cat.id }"
            @click="activeCategory = cat.id"
          >
            {{ cat.name }}
          </button>
        </div>
      </div>

      <!-- 视频网格 -->
      <div class="yt-grid">
        <div 
          v-for="video in filteredVideos" 
          :key="video.id" 
          class="yt-video-renderer"
          @click="handleVideoClick(video)"
        >
          <!-- 缩略图容器 -->
          <div class="yt-thumbnail">
            <a class="yt-thumbnail-link">
              <img :src="video.thumbnail" :alt="video.title" class="yt-thumbnail-img" />
              <!-- 时长标签 -->
              <div class="yt-time-status">
                <span class="yt-time-text">{{ video.duration }}</span>
              </div>
              <!-- 悬停遮罩 -->
              <div class="yt-thumbnail-hover">
                <el-icon class="yt-play-icon"><VideoPlay /></el-icon>
              </div>
            </a>
          </div>
          
          <!-- 视频元数据 -->
          <div class="yt-meta">
            <a class="yt-avatar-link">
              <div class="yt-avatar">
                {{ video.channel[0] }}
              </div>
            </a>
            <div class="yt-details">
              <h3 class="yt-video-title">
                <a class="yt-title-link">{{ video.title }}</a>
              </h3>
              <div class="yt-channel-info">
                <a class="yt-channel-name">{{ video.channel }}</a>
                <el-tooltip content="验证频道" placement="top" :show-after="500">
                  <el-icon class="verified-icon" :size="12"><CircleCheckFilled /></el-icon>
                </el-tooltip>
              </div>
              <div class="yt-video-meta-block">
                <span class="yt-view-count">{{ formatViews(video.views) }}次观看</span>
                <span class="yt-dot">•</span>
                <span class="yt-publish-time">{{ video.uploadTime }}</span>
              </div>
            </div>
            <!-- 更多操作按钮 -->
            <div class="yt-more-actions">
              <el-icon><MoreFilled /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  VideoPlay, More, InfoFilled, HomeFilled, VideoCamera, Collection, 
  Clock, Files, Timer, Pointer, ArrowRight, CircleCheckFilled, MoreFilled
} from '@element-plus/icons-vue'

const activeCategory = ref('all')

const categories = ref([
  { id: 'all', name: '全部' },
  { id: 'activity', name: '校园活动' },
  { id: 'lecture', name: '讲座报告' },
  { id: 'interview', name: '人物访谈' },
  { id: 'campus', name: '校园风光' },
  { id: 'sports', name: '体育赛事' },
  { id: 'art', name: '文艺演出' },
  { id: 'tech', name: '科技创新' },
  { id: 'life', name: '校园生活' },
  { id: 'news', name: '新闻直击' },
  { id: 'documentary', name: '纪录片' },
  { id: 'vlog', name: 'Vlog' }
])

const subscriptions = ref([
  { id: 1, name: '武理官方', color: '#ef4444', hasNew: true },
  { id: 2, name: '校团委', color: '#f59e0b', hasNew: true },
  { id: 3, name: '教务处', color: '#10b981', hasNew: false },
  { id: 4, name: '图书馆', color: '#3b82f6', hasNew: false },
  { id: 5, name: '后勤保障', color: '#6366f1', hasNew: true },
  { id: 6, name: '学工部', color: '#8b5cf6', hasNew: false },
])

const videos = ref([
  {
    id: 1,
    title: '2024年武汉理工大学毕业典礼精彩回顾 - 青春不散场，梦想起航时',
    thumbnail: 'https://picsum.photos/seed/v1/640/360',
    channel: '武理官方',
    views: 128000,
    uploadTime: '2天前',
    duration: '15:32',
    category: 'activity'
  },
  {
    id: 2,
    title: '【院士讲坛】人工智能与未来科技发展：从大模型到通用人工智能',
    thumbnail: 'https://picsum.photos/seed/v2/640/360',
    channel: '学术报告厅',
    views: 45600,
    uploadTime: '1周前',
    duration: '1:23:45',
    category: 'lecture'
  },
  {
    id: 3,
    title: '对话杰出校友：从武理到世界500强，我的职场进阶之路',
    thumbnail: 'https://picsum.photos/seed/v3/640/360',
    channel: '校友会',
    views: 23400,
    uploadTime: '3天前',
    duration: '28:16',
    category: 'interview'
  },
  {
    id: 4,
    title: '4K航拍武理：四季校园风光大片，每一帧都是壁纸',
    thumbnail: 'https://picsum.photos/seed/v4/640/360',
    channel: '摄影协会',
    views: 89200,
    uploadTime: '2周前',
    duration: '8:45',
    category: 'campus'
  },
  {
    id: 5,
    title: '校运会开幕式全程回顾：青春飞扬 激情燃烧，看各学院方阵创意入场',
    thumbnail: 'https://picsum.photos/seed/v5/640/360',
    channel: '体育部',
    views: 67800,
    uploadTime: '1个月前',
    duration: '45:20',
    category: 'sports'
  },
  {
    id: 6,
    title: '新年音乐会：交响乐团专场演出，奏响新春华章',
    thumbnail: 'https://picsum.photos/seed/v6/640/360',
    channel: '艺术团',
    views: 34500,
    uploadTime: '3周前',
    duration: '1:56:30',
    category: 'art'
  },
  {
    id: 7,
    title: 'RoboMaster机器人大赛：武理战队夺冠之路纪录片',
    thumbnail: 'https://picsum.photos/seed/v7/640/360',
    channel: '科创中心',
    views: 56700,
    uploadTime: '5天前',
    duration: '22:18',
    category: 'tech'
  },
  {
    id: 8,
    title: 'Vlog：武理学子的一天 | 食堂测评 | 泡图书馆 | 社团活动',
    thumbnail: 'https://picsum.photos/seed/v8/640/360',
    channel: '学生媒体',
    views: 78900,
    uploadTime: '4天前',
    duration: '12:05',
    category: 'life'
  },
  {
    id: 9,
    title: '图书馆深夜：考研人的坚守与梦想，致每一个努力奋斗的你',
    thumbnail: 'https://picsum.photos/seed/v9/640/360',
    channel: '校报记者团',
    views: 45600,
    uploadTime: '1周前',
    duration: '18:32',
    category: 'life'
  },
  {
    id: 10,
    title: '校长开学典礼致辞：奋进新征程，做新时代的有为青年',
    thumbnail: 'https://picsum.photos/seed/v10/640/360',
    channel: '武理官方',
    views: 234000,
    uploadTime: '2个月前',
    duration: '25:40',
    category: 'activity'
  },
  {
    id: 11,
    title: '实验室探秘：走进材料科学前沿，感受科技的魅力',
    thumbnail: 'https://picsum.photos/seed/v11/640/360',
    channel: '科研处',
    views: 28900,
    uploadTime: '2周前',
    duration: '35:15',
    category: 'tech'
  },
  {
    id: 12,
    title: '足球联赛决赛：精彩进球集锦，绝杀时刻燃爆全场',
    thumbnail: 'https://picsum.photos/seed/v12/640/360',
    channel: '体育部',
    views: 41200,
    uploadTime: '6天前',
    duration: '10:28',
    category: 'sports'
  }
])

const filteredVideos = computed(() => {
  if (activeCategory.value === 'all') {
    return videos.value
  }
  return videos.value.filter(v => v.category === activeCategory.value)
})

const formatViews = (num) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  }
  return num.toLocaleString()
}

const handleVideoClick = (video) => {
  ElMessage.info({
    message: `视频播放功能开发中：${video.title}`,
    duration: 2000
  })
}
</script>

<style scoped>
/* ========== 布局容器 ========== */
.yt-container {
  display: flex;
  background-color: #ffffff;
  min-height: calc(100vh - 60px); /* 减去顶部导航高度 */
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

/* 侧边栏分组 */
.sidebar-section {
  padding: 8px 0;
}

.sidebar-divider {
  height: 1px;
  background-color: #e5e5e5;
  margin: 8px 12px;
}

/* 侧边栏标题 */
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

/* 侧边栏条目 */
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

/* 订阅列表样式 */
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
  padding-bottom: 4px; /* 防止滚动条遮挡 */
}

.yt-chips-scroll::-webkit-scrollbar {
  display: none;
}

.yt-chip {
  flex-shrink: 0;
  height: 32px;
  padding: 0 12px;
  border: none;
  border-radius: 8px; /* 圆角矩形 */
  background-color: #f2f2f2;
  color: #0f0f0f;
  font-family: 'Roboto', 'Arial', sans-serif;
  font-size: 14px;
  font-weight: 500;
  line-height: 32px;
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

/* ========== 视频网格 ========== */
.yt-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr); /* 默认3列，仿照截图 */
  gap: 40px 16px; /* 行间距加大 */
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

/* ========== 视频渲染器 ========== */
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
  transform: scale(1.02); /* 轻微放大 */
  border-radius: 0; /* YouTube悬停时圆角会变化，这里保持简单放大 */
}

.yt-thumbnail-link:hover {
  border-radius: 0; /* YouTube悬停时圆角变直角 */
  transition: border-radius 0.2s;
}

/* 时长标签 */
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

/* 悬停遮罩 */
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
  padding-right: 24px; /* 为更多按钮留空间 */
}

/* 标题 */
.yt-video-title {
  margin: 0 0 4px;
  line-height: 22px;
}

.yt-title-link {
  color: #0f0f0f;
  font-family: 'Roboto', 'Arial', sans-serif;
  font-size: 16px;
  font-weight: 500;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 频道信息 */
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

/* 观看次数和时间 */
.yt-video-meta-block {
  color: #606060;
  font-size: 14px;
}

.yt-dot {
  margin: 0 4px;
}

/* 更多操作按钮 - 只有hover时显示 */
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

/* ========== 暗黑模式适配 ========== */
.dark-mode .yt-container,
.dark-mode .yt-sidebar,
.dark-mode .yt-chips-bar {
  background-color: #0f0f0f;
}

.dark-mode .sidebar-header,
.dark-mode .sidebar-item {
  color: #f1f1f1;
}

.dark-mode .sidebar-item .el-icon {
  color: #f1f1f1;
}

.dark-mode .sidebar-header:hover,
.dark-mode .sidebar-item:hover,
.dark-mode .sidebar-item.active {
  background-color: #272727;
}

.dark-mode .sidebar-divider {
  background-color: #3f3f3f;
}

.dark-mode .yt-chip {
  background-color: #272727;
  color: #f1f1f1;
}

.dark-mode .yt-chip:hover {
  background-color: #3f3f3f;
}

.dark-mode .yt-chip-active {
  background-color: #f1f1f1;
  color: #0f0f0f;
}

.dark-mode .yt-title-link {
  color: #f1f1f1;
}

.dark-mode .yt-channel-name,
.dark-mode .yt-video-meta-block,
.dark-mode .verified-icon {
  color: #aaaaaa;
}

.dark-mode .yt-channel-name:hover {
  color: #f1f1f1;
}

.dark-mode .yt-thumbnail-link {
  background-color: #202020;
}
</style>
