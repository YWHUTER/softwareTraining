<template>
  <div class="marketplace-page">
    <!-- 顶部区域 -->
    <div 
      class="marketplace-header"
      v-motion
      :initial="{ opacity: 0, y: -20 }"
      :enter="{ opacity: 1, y: 0, transition: { type: 'spring', stiffness: 250, damping: 25 } }"
    >
      <div class="header-content">
        <div class="header-left">
          <div class="marketplace-icon">
            <el-icon :size="32"><Shop /></el-icon>
          </div>
          <div class="header-info">
            <h1 class="page-title">校园集市</h1>
            <p class="page-desc">共 {{ total }} 条动态 · 分享日常、交流互助</p>
          </div>
        </div>
        <div class="header-right">
          <el-button type="primary" round @click="handlePublishClick" class="publish-btn">
            <el-icon><EditPen /></el-icon>
            发布动态
          </el-button>
        </div>
      </div>
      
      <!-- 分类标签 -->
      <div class="category-tabs">
        <div 
          class="tab-item" 
          :class="{ active: activeCategory === 'all' }"
          @click="selectCategory('all')"
        >
          <el-icon><Grid /></el-icon>
          全部
        </div>
        <div 
          v-for="cat in categories" 
          :key="cat.value"
          class="tab-item"
          :class="{ active: activeCategory === cat.value }"
          @click="selectCategory(cat.value)"
        >
          <el-icon><component :is="cat.icon" /></el-icon>
          {{ cat.label }}
        </div>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="marketplace-content">
      <!-- 左侧帖子列表 -->
      <div class="posts-section">
        <!-- 排序栏 -->
        <div 
          class="sort-bar"
          v-motion
          :initial="{ opacity: 0, x: -20 }"
          :enter="{ opacity: 1, x: 0, transition: { delay: 100 } }"
        >
          <div class="sort-tabs">
            <span 
              class="sort-tab" 
              :class="{ active: sortBy === 'latest' }"
              @click="changeSortBy('latest')"
            >
              <el-icon><Clock /></el-icon>
              最新
            </span>
            <span 
              class="sort-tab" 
              :class="{ active: sortBy === 'hot' }"
              @click="changeSortBy('hot')"
            >
              <el-icon><TrendCharts /></el-icon>
              热门
            </span>
          </div>
          <div class="sort-right">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索动态..."
              size="small"
              clearable
              style="width: 180px;"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="loading-state">
          <div v-for="i in 5" :key="i" class="post-skeleton">
            <el-skeleton animated>
              <template #template>
                <div class="skeleton-header">
                  <el-skeleton-item variant="circle" style="width: 40px; height: 40px;" />
                  <div class="skeleton-info">
                    <el-skeleton-item variant="text" style="width: 100px;" />
                    <el-skeleton-item variant="text" style="width: 60px;" />
                  </div>
                </div>
                <el-skeleton-item variant="h3" style="width: 80%; margin: 12px 0;" />
                <el-skeleton-item variant="text" style="width: 100%;" />
                <el-skeleton-item variant="text" style="width: 90%;" />
              </template>
            </el-skeleton>
          </div>
        </div>

        <!-- 帖子列表 -->
        <div v-else-if="posts.length > 0" class="posts-list">
          <div 
            v-for="(post, index) in posts" 
            :key="post.id" 
            class="post-card"
            :class="{ 'has-image': post.coverImage }"
            @click="goToDetail(post.id)"
            v-motion
            :initial="{ opacity: 0, y: 30 }"
            :enter="{ opacity: 1, y: 0, transition: { delay: index * 50, type: 'spring', stiffness: 250, damping: 25 } }"
          >
            <!-- 帖子头部 -->
            <div class="post-header">
              <div class="author-info" @click.stop="goToProfile(post.author?.id)">
                <el-avatar :size="42" :src="getAvatarUrl(post.author?.avatar)" class="author-avatar">
                  {{ post.author?.realName?.[0] || '?' }}
                </el-avatar>
                <div class="author-detail">
                  <span class="author-name">{{ post.author?.realName || '匿名用户' }}</span>
                  <div class="post-meta">
                    <span class="post-time">{{ formatTime(post.createdAt) }}</span>
                    <el-tag v-if="post.category" size="small" round class="category-tag">
                      {{ getCategoryLabel(post.category) }}
                    </el-tag>
                  </div>
                </div>
              </div>
              <el-dropdown trigger="click" @click.stop>
                <div class="more-btn">
                  <el-icon><MoreFilled /></el-icon>
                </div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handleShare(post)">
                      <el-icon><Share /></el-icon> 分享
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleFavorite(post)">
                      <el-icon><Star /></el-icon> 收藏
                    </el-dropdown-item>
                    <el-dropdown-item v-if="isOwnPost(post)" divided @click="handleDeletePost(post)">
                      <el-icon><Delete /></el-icon> 删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>

            <!-- 帖子内容 -->
            <div class="post-body">
              <h3 class="post-title">{{ post.title }}</h3>
              <p class="post-content">{{ getPostSummary(post) }}</p>
              
              <!-- 图片展示 -->
              <div v-if="post.coverImage" class="post-images">
                <el-image 
                  :src="getImageUrl(post.coverImage)" 
                  fit="cover"
                  class="post-image"
                  :preview-src-list="[getImageUrl(post.coverImage)]"
                  @click.stop
                >
                  <template #placeholder>
                    <div class="image-loading">
                      <el-icon class="is-loading"><Loading /></el-icon>
                    </div>
                  </template>
                </el-image>
              </div>
            </div>

            <!-- 帖子底部 -->
            <div class="post-footer">
              <div class="action-item" :class="{ active: post.isLiked }" @click.stop="handleLike(post)">
                <el-icon>
                  <StarFilled v-if="post.isLiked" />
                  <Star v-else />
                </el-icon>
                <span>{{ formatCount(post.likeCount) }}</span>
              </div>
              <div class="action-item">
                <el-icon><ChatDotRound /></el-icon>
                <span>{{ formatCount(post.commentCount) }}</span>
              </div>
              <div class="action-item">
                <el-icon><View /></el-icon>
                <span>{{ formatCount(post.viewCount) }}</span>
              </div>
              <div class="action-item share-btn" @click.stop="handleShare(post)">
                <el-icon><Share /></el-icon>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-state" v-motion :initial="{ opacity: 0, scale: 0.9 }" :enter="{ opacity: 1, scale: 1 }">
          <div class="empty-icon">
            <el-icon :size="80"><Document /></el-icon>
          </div>
          <h3>暂无动态</h3>
          <p>快来发布第一条动态吧~</p>
          <el-button type="primary" round @click="handlePublishClick">
            <el-icon><EditPen /></el-icon>
            发布动态
          </el-button>
        </div>

        <!-- 分页 -->
        <div v-if="total > pageSize" class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="loadPosts"
            background
          />
        </div>
      </div>

      <!-- 右侧边栏 -->
      <div class="sidebar">
        <!-- 热门话题 -->
        <div 
          class="sidebar-card"
          v-motion
          :initial="{ opacity: 0, x: 20 }"
          :enter="{ opacity: 1, x: 0, transition: { delay: 200 } }"
        >
          <h3 class="card-title">
            <el-icon><TrendCharts /></el-icon>
            热门话题
          </h3>
          <div class="hot-topics">
            <div 
              v-for="(topic, index) in hotTopics" 
              :key="topic.id"
              class="topic-item"
              @click="searchByTag(topic.name)"
              v-motion
              :initial="{ opacity: 0, x: 10 }"
              :enter="{ opacity: 1, x: 0, transition: { delay: 250 + index * 50 } }"
            >
              <span class="topic-rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
              <span class="topic-name"># {{ topic.name }}</span>
              <span class="topic-count">{{ topic.useCount }}条</span>
            </div>
            <div v-if="hotTopics.length === 0" class="empty-tip">暂无热门话题</div>
          </div>
        </div>

        <!-- 活跃用户 -->
        <div 
          class="sidebar-card"
          v-motion
          :initial="{ opacity: 0, x: 20 }"
          :enter="{ opacity: 1, x: 0, transition: { delay: 300 } }"
        >
          <h3 class="card-title">
            <el-icon><User /></el-icon>
            活跃用户
          </h3>
          <div class="active-users">
            <div 
              v-for="(user, index) in activeUsers" 
              :key="user.id" 
              class="user-item"
              @click="goToProfile(user.id)"
              v-motion
              :initial="{ opacity: 0, x: 10 }"
              :enter="{ opacity: 1, x: 0, transition: { delay: 350 + index * 50 } }"
            >
              <el-avatar :size="40" :src="getAvatarUrl(user.avatar)" class="user-avatar">
                {{ user.realName?.[0] }}
              </el-avatar>
              <div class="user-info">
                <span class="user-name">{{ user.realName }}</span>
                <span class="user-posts">{{ user.postCount }}条动态</span>
              </div>
            </div>
            <div v-if="activeUsers.length === 0" class="empty-tip">暂无活跃用户</div>
          </div>
        </div>

        <!-- 发布提示卡片 -->
        <div 
          class="sidebar-card publish-tip-card"
          v-motion
          :initial="{ opacity: 0, x: 20 }"
          :enter="{ opacity: 1, x: 0, transition: { delay: 400 } }"
        >
          <div class="tip-content">
            <el-icon :size="32"><EditPen /></el-icon>
            <p>有什么想分享的？</p>
            <el-button type="primary" round size="small" @click="handlePublishClick">
              发布动态
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 发布对话框 -->
    <el-dialog 
      v-model="showPublishDialog" 
      title="发布动态" 
      width="600px"
      class="publish-dialog"
      :close-on-click-modal="false"
    >
      <el-form :model="publishForm" label-position="top">
        <el-form-item label="标题" required>
          <el-input 
            v-model="publishForm.title" 
            placeholder="给你的动态起个标题"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="内容" required>
          <el-input 
            v-model="publishForm.content" 
            type="textarea" 
            :rows="6"
            placeholder="分享你的校园生活..."
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="分类">
          <el-radio-group v-model="publishForm.category" class="category-radio-group">
            <el-radio-button 
              v-for="cat in categories" 
              :key="cat.value" 
              :value="cat.value"
            >
              <el-icon><component :is="cat.icon" /></el-icon>
              {{ cat.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="图片（可选）">
          <el-upload
            class="image-uploader"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :before-upload="beforeUpload"
            :on-progress="handleUploadProgress"
            accept="image/*"
          >
            <div v-if="publishForm.coverImage" class="uploaded-image">
              <el-image :src="getImageUrl(publishForm.coverImage)" fit="cover" />
              <div class="image-mask" @click.stop="removeImage">
                <el-icon><Delete /></el-icon>
                <span>删除</span>
              </div>
            </div>
            <div v-else-if="uploading" class="upload-progress">
              <el-progress type="circle" :percentage="uploadProgress" :width="80" />
            </div>
            <div v-else class="upload-placeholder">
              <el-icon :size="32"><Picture /></el-icon>
              <span>点击或拖拽上传图片</span>
              <span class="upload-tip">支持 JPG、PNG，最大 5MB</span>
            </div>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showPublishDialog = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="handlePublish">
          发布
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Shop, EditPen, ChatDotRound, View, Star, StarFilled, Document,
  TrendCharts, User, Delete, MoreFilled, Share, Picture, Loading,
  Clock, Search, Grid, Coffee, ShoppingCart, Service, Flag, 
  Ticket, Reading, Football
} from '@element-plus/icons-vue'
import { getArticleList, createArticle, toggleLike, toggleFavorite, deleteArticle } from '@/api/article'

const router = useRouter()

// 分类配置
const categories = [
  { value: 'daily', label: '日常', icon: Coffee },
  { value: 'trade', label: '交易', icon: ShoppingCart },
  { value: 'help', label: '互助', icon: Service },
  { value: 'activity', label: '组队', icon: Flag },
  { value: 'lost', label: '失物', icon: Ticket },
  { value: 'study', label: '学习', icon: Reading },
  { value: 'sports', label: '运动', icon: Football }
]

// 状态
const loading = ref(false)
const posts = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const activeCategory = ref('all')
const sortBy = ref('latest')
const searchKeyword = ref('')
const currentUser = ref(null)

// 发布相关
const showPublishDialog = ref(false)
const publishing = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const publishForm = ref({
  title: '',
  content: '',
  category: 'daily',
  coverImage: ''
})

// 侧边栏数据
const hotTopics = ref([])
const activeUsers = ref([])

// 上传配置
const uploadUrl = '/api/file/upload'
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`
}))

// 加载帖子
const loadPosts = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      boardType: 'MARKETPLACE',
      isApproved: 1,
      sortBy: sortBy.value === 'hot' ? 'views' : 'date',
      sortOrder: 'desc'
    }
    
    if (activeCategory.value !== 'all') {
      params.category = activeCategory.value
    }
    
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    
    const res = await getArticleList(params)
    posts.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('加载帖子失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载热门话题（从标签API获取）
const loadHotTopics = async () => {
  try {
    // 使用静态数据，实际项目中应该从API获取
    hotTopics.value = [
      { id: 1, name: '期末复习', useCount: 128 },
      { id: 2, name: '二手书籍', useCount: 96 },
      { id: 3, name: '校园美食', useCount: 85 },
      { id: 4, name: '社团招新', useCount: 72 },
      { id: 5, name: '考研交流', useCount: 68 }
    ]
  } catch (error) {
    console.error('加载热门话题失败:', error)
  }
}

// 加载活跃用户
const loadActiveUsers = async () => {
  try {
    // 使用静态数据，实际项目中应该从API获取
    activeUsers.value = [
      { id: 1, realName: '小明同学', avatar: '', postCount: 32 },
      { id: 2, realName: '学霸君', avatar: '', postCount: 28 },
      { id: 3, realName: '运动达人', avatar: '', postCount: 24 }
    ]
  } catch (error) {
    console.error('加载活跃用户失败:', error)
  }
}

// 选择分类
const selectCategory = (cat) => {
  activeCategory.value = cat
  currentPage.value = 1
  loadPosts()
}

// 切换排序
const changeSortBy = (sort) => {
  sortBy.value = sort
  currentPage.value = 1
  loadPosts()
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadPosts()
}

// 发布点击
const handlePublishClick = () => {
  if (!currentUser.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  showPublishDialog.value = true
}

// 发布帖子
const handlePublish = async () => {
  if (!publishForm.value.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!publishForm.value.content.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  
  publishing.value = true
  try {
    await createArticle({
      title: publishForm.value.title,
      content: publishForm.value.content,
      summary: publishForm.value.content.substring(0, 200),
      coverImage: publishForm.value.coverImage,
      boardType: 'MARKETPLACE',
      category: publishForm.value.category
    })
    
    ElMessage.success('发布成功，等待审核')
    showPublishDialog.value = false
    publishForm.value = { title: '', content: '', category: 'daily', coverImage: '' }
    loadPosts()
  } catch (error) {
    ElMessage.error(error.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

// 点赞
const handleLike = async (post) => {
  if (!currentUser.value) {
    ElMessage.warning('请先登录')
    return
  }
  
  try {
    const res = await toggleLike(post.id)
    post.isLiked = res
    post.likeCount = res ? (post.likeCount || 0) + 1 : Math.max(0, (post.likeCount || 1) - 1)
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 收藏
const handleFavorite = async (post) => {
  if (!currentUser.value) {
    ElMessage.warning('请先登录')
    return
  }
  
  try {
    const res = await toggleFavorite(post.id)
    ElMessage.success(res ? '收藏成功' : '取消收藏')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 分享
const handleShare = (post) => {
  const url = `${window.location.origin}/article/${post.id}`
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('链接已复制到剪贴板')
  }).catch(() => {
    ElMessage.info(`分享链接: ${url}`)
  })
}

// 删除帖子
const handleDeletePost = async (post) => {
  try {
    await ElMessageBox.confirm('确定要删除这条动态吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteArticle(post.id)
    ElMessage.success('删除成功')
    loadPosts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 是否是自己的帖子
const isOwnPost = (post) => {
  return currentUser.value && post.author?.id === currentUser.value.id
}

// 跳转详情
const goToDetail = (id) => {
  router.push(`/article/${id}`)
}

// 跳转用户主页
const goToProfile = (userId) => {
  if (userId) {
    router.push(`/profile?userId=${userId}`)
  }
}

// 按标签搜索
const searchByTag = (tag) => {
  searchKeyword.value = tag
  handleSearch()
}

// 工具函数
const getAvatarUrl = (url) => {
  if (!url) return ''
  return url.startsWith('http') ? url : `http://localhost:8080${url}`
}

const getImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/api')) return url
  return `http://localhost:8080${url}`
}

const formatTime = (time) => {
  if (!time) return ''
  const now = new Date()
  const past = new Date(time)
  const diff = (now - past) / 1000
  
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  if (diff < 604800) return Math.floor(diff / 86400) + '天前'
  return past.toLocaleDateString('zh-CN')
}

const formatCount = (num) => {
  if (!num) return 0
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num
}

const getPostSummary = (post) => {
  const content = post.summary || post.content || ''
  const text = content.replace(/<[^>]+>/g, '')
  return text.length > 150 ? text.substring(0, 150) + '...' : text
}

const getCategoryLabel = (value) => {
  const cat = categories.find(c => c.value === value)
  return cat ? cat.label : value
}

// 上传相关
const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  uploading.value = true
  uploadProgress.value = 0
  return true
}

const handleUploadProgress = (event) => {
  uploadProgress.value = Math.round(event.percent)
}

const handleUploadSuccess = (response) => {
  uploading.value = false
  if (response.code === 200) {
    publishForm.value.coverImage = response.data
    ElMessage.success('上传成功')
  } else {
    ElMessage.error('上传失败')
  }
}

const removeImage = () => {
  publishForm.value.coverImage = ''
}

// 加载当前用户
const loadCurrentUser = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    currentUser.value = JSON.parse(userStr)
  }
}

onMounted(() => {
  loadCurrentUser()
  loadPosts()
  loadHotTopics()
  loadActiveUsers()
})
</script>


<style scoped>
.marketplace-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

/* 头部区域 */
.marketplace-header {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border-radius: 20px;
  padding: 24px 28px;
  margin-bottom: 24px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.marketplace-header:hover {
  background: rgba(255, 255, 255, 0.6);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.marketplace-icon {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #a855f7 0%, #7c3aed 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 8px 24px rgba(168, 85, 247, 0.35);
  transform: rotate(-5deg);
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.marketplace-header:hover .marketplace-icon {
  transform: rotate(0deg) scale(1.05);
}

.page-title {
  font-size: 26px;
  font-weight: 800;
  color: #1f2937;
  margin: 0 0 4px;
  background: linear-gradient(90deg, #a855f7, #7c3aed);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.page-desc {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.publish-btn {
  background: linear-gradient(135deg, #a855f7 0%, #7c3aed 100%) !important;
  border: none !important;
  box-shadow: 0 4px 16px rgba(168, 85, 247, 0.4);
  transition: all 0.3s ease;
}

.publish-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(168, 85, 247, 0.5);
}

/* 分类标签 */
.category-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 20px;
  background: rgba(243, 244, 246, 0.8);
  color: #4b5563;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 1px solid transparent;
}

.tab-item:hover {
  background: rgba(168, 85, 247, 0.1);
  color: #a855f7;
  border-color: rgba(168, 85, 247, 0.2);
  transform: translateY(-2px);
}

.tab-item.active {
  background: linear-gradient(135deg, #a855f7 0%, #7c3aed 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(168, 85, 247, 0.3);
}

/* 内容区域 */
.marketplace-content {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 24px;
}

/* 帖子区域 */
.posts-section {
  min-width: 0;
}

.sort-bar {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(20px);
  border-radius: 14px;
  padding: 12px 20px;
  margin-bottom: 16px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sort-tabs {
  display: flex;
  gap: 20px;
}

.sort-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: all 0.2s;
}

.sort-tab:hover {
  color: #a855f7;
  background: rgba(168, 85, 247, 0.1);
}

.sort-tab.active {
  color: #a855f7;
  font-weight: 600;
  background: rgba(168, 85, 247, 0.15);
}

/* 帖子卡片 */
.posts-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-card {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(20px) saturate(150%);
  border-radius: 18px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  overflow: hidden;
}

.post-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #a855f7, #7c3aed, #ec4899);
  opacity: 0;
  transition: opacity 0.3s;
}

.post-card:hover {
  transform: translateY(-4px) scale(1.01);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.12);
  background: rgba(255, 255, 255, 0.7);
}

.post-card:hover::before {
  opacity: 1;
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.author-avatar {
  background: linear-gradient(135deg, #a855f7, #7c3aed);
  color: white;
  font-weight: 600;
  transition: transform 0.3s;
}

.author-info:hover .author-avatar {
  transform: scale(1.1);
}

.author-detail {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.author-name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  transition: color 0.2s;
}

.author-info:hover .author-name {
  color: #a855f7;
}

.post-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.post-time {
  font-size: 12px;
  color: #9ca3af;
}

.category-tag {
  background: rgba(168, 85, 247, 0.1) !important;
  color: #a855f7 !important;
  border: none !important;
  font-size: 11px;
}

.more-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.2s;
}

.more-btn:hover {
  background: rgba(0, 0, 0, 0.05);
  color: #6b7280;
}

.post-body {
  margin-bottom: 16px;
}

.post-title {
  font-size: 17px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px;
  line-height: 1.4;
  transition: color 0.2s;
}

.post-card:hover .post-title {
  color: #7c3aed;
}

.post-content {
  font-size: 14px;
  color: #4b5563;
  line-height: 1.7;
  margin: 0;
}

.post-images {
  margin-top: 12px;
  border-radius: 12px;
  overflow: hidden;
}

.post-image {
  width: 100%;
  max-height: 320px;
  border-radius: 12px;
  transition: transform 0.3s;
}

.post-card:hover .post-image {
  transform: scale(1.02);
}

.image-loading {
  width: 100%;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  color: #9ca3af;
}

.post-footer {
  display: flex;
  gap: 20px;
  padding-top: 14px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.action-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 8px;
  transition: all 0.2s;
}

.action-item:hover {
  color: #a855f7;
  background: rgba(168, 85, 247, 0.1);
}

.action-item.active {
  color: #f59e0b;
}

.action-item.active .el-icon {
  animation: like-bounce 0.4s ease;
}

@keyframes like-bounce {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.3); }
}

.share-btn {
  margin-left: auto;
}

/* 侧边栏 */
.sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sidebar-card {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(20px) saturate(150%);
  border-radius: 18px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  transition: all 0.3s ease;
}

.sidebar-card:hover {
  background: rgba(255, 255, 255, 0.65);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 16px;
}

.card-title .el-icon {
  color: #a855f7;
}

/* 热门话题 */
.hot-topics {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.topic-item {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 10px 12px;
  border-radius: 10px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.topic-item:hover {
  background: rgba(168, 85, 247, 0.1);
  transform: translateX(4px);
}

.topic-rank {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  background: #e5e7eb;
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.topic-rank.top {
  background: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.4);
}

.topic-name {
  flex: 1;
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

.topic-count {
  font-size: 12px;
  color: #9ca3af;
}

/* 活跃用户 */
.active-users {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.user-item:hover {
  background: rgba(168, 85, 247, 0.1);
  transform: translateX(4px);
}

.user-avatar {
  background: linear-gradient(135deg, #a855f7, #7c3aed);
  color: white;
  font-weight: 600;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.user-posts {
  font-size: 12px;
  color: #9ca3af;
}

.empty-tip {
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
  padding: 20px 0;
}

/* 发布提示卡片 */
.publish-tip-card {
  background: linear-gradient(135deg, rgba(168, 85, 247, 0.1) 0%, rgba(124, 58, 237, 0.1) 100%) !important;
  border-color: rgba(168, 85, 247, 0.2) !important;
}

.tip-content {
  text-align: center;
  padding: 10px 0;
}

.tip-content .el-icon {
  color: #a855f7;
  margin-bottom: 8px;
}

.tip-content p {
  color: #6b7280;
  margin: 0 0 12px;
  font-size: 14px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(20px);
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.empty-icon {
  width: 120px;
  height: 120px;
  margin: 0 auto 20px;
  background: rgba(168, 85, 247, 0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #a855f7;
}

.empty-state h3 {
  font-size: 20px;
  color: #1f2937;
  margin: 0 0 8px;
}

.empty-state p {
  color: #6b7280;
  margin: 0 0 24px;
}

/* 加载骨架 */
.loading-state {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-skeleton {
  background: rgba(255, 255, 255, 0.5);
  border-radius: 18px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.skeleton-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.skeleton-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* 分页 */
.pagination-container {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}

/* 发布对话框 */
:deep(.publish-dialog) {
  border-radius: 16px;
}

:deep(.publish-dialog .el-dialog__header) {
  border-bottom: 1px solid #f3f4f6;
  padding-bottom: 16px;
}

.category-radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.category-radio-group .el-radio-button {
  --el-radio-button-checked-bg-color: linear-gradient(135deg, #a855f7 0%, #7c3aed 100%);
}

.image-uploader {
  width: 100%;
}

.upload-placeholder {
  width: 100%;
  height: 160px;
  border: 2px dashed #d1d5db;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-placeholder:hover {
  border-color: #a855f7;
  color: #a855f7;
  background: rgba(168, 85, 247, 0.05);
}

.upload-placeholder .el-icon {
  color: #a855f7;
}

.upload-tip {
  font-size: 12px;
  color: #9ca3af;
}

.upload-progress {
  width: 100%;
  height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px dashed #a855f7;
  border-radius: 12px;
  background: rgba(168, 85, 247, 0.05);
}

.uploaded-image {
  width: 100%;
  height: 200px;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
}

.uploaded-image .el-image {
  width: 100%;
  height: 100%;
}

.image-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s;
  cursor: pointer;
  color: white;
}

.uploaded-image:hover .image-mask {
  opacity: 1;
}

/* 响应式 */
@media (max-width: 1024px) {
  .marketplace-content {
    grid-template-columns: 1fr;
  }
  
  .sidebar {
    display: none;
  }
}

@media (max-width: 600px) {
  .marketplace-page {
    padding: 16px;
  }
  
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .category-tabs {
    overflow-x: auto;
    flex-wrap: nowrap;
    padding-bottom: 8px;
    -webkit-overflow-scrolling: touch;
  }
  
  .tab-item {
    flex-shrink: 0;
  }
  
  .sort-bar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .sort-right {
    width: 100%;
  }
  
  .sort-right .el-input {
    width: 100% !important;
  }
}
</style>
