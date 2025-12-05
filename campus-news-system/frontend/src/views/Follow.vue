<template>
  <div class="follow-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>关注动态</h1>
      <p>查看你关注的人发布的最新内容</p>
    </div>
    
    <el-row :gutter="20">
      <!-- 左侧：关注动态 -->
      <el-col :span="16">
        <!-- Tab 切换 -->
        <el-card class="content-card">
          <el-tabs v-model="activeTab" @tab-change="handleTabChange">
            <el-tab-pane label="关注动态" name="feed">
              <div v-if="feedLoading" class="loading-container">
                <el-skeleton :rows="5" animated />
              </div>
              <div v-else-if="feedList.length === 0" class="empty-container">
                <el-empty description="暂无动态，去关注一些用户吧~">
                  <el-button type="primary" @click="activeTab = 'recommend'">发现用户</el-button>
                </el-empty>
              </div>
              <div v-else class="feed-list">
                <div 
                  v-for="article in feedList" 
                  :key="article.id" 
                  class="feed-item"
                  @click="goToArticle(article.id)"
                >
                  <div class="feed-author">
                    <el-avatar :size="40" :src="article.author?.avatar">
                      {{ article.author?.realName?.[0] || 'U' }}
                    </el-avatar>
                    <div class="author-info">
                      <span class="author-name">{{ article.author?.realName }}</span>
                      <span class="publish-time">{{ formatTime(article.createdAt) }}</span>
                    </div>
                  </div>
                  <div class="feed-content">
                    <h3 class="feed-title">{{ article.title }}</h3>
                    <p class="feed-summary">{{ article.summary || article.content?.slice(0, 100) }}</p>
                    <img v-if="article.coverImage" :src="article.coverImage" class="feed-cover" />
                  </div>
                  <div class="feed-stats">
                    <span><el-icon><View /></el-icon> {{ article.viewCount }}</span>
                    <span><el-icon><ChatDotRound /></el-icon> {{ article.commentCount }}</span>
                    <el-tag size="small" :type="getBoardTagType(article.boardType)">
                      {{ getBoardTypeName(article.boardType) }}
                    </el-tag>
                  </div>
                </div>
                
                <!-- 分页 -->
                <div class="pagination-container">
                  <el-pagination
                    v-model:current-page="feedPage.current"
                    :page-size="feedPage.size"
                    :total="feedPage.total"
                    layout="prev, pager, next"
                    @current-change="loadFeed"
                  />
                </div>
              </div>
            </el-tab-pane>
            
            <el-tab-pane label="我的关注" name="following">
              <div v-if="followingLoading" class="loading-container">
                <el-skeleton :rows="3" animated />
              </div>
              <div v-else-if="followingList.length === 0" class="empty-container">
                <el-empty description="你还没有关注任何人" />
              </div>
              <div v-else class="user-list">
                <div v-for="user in followingList" :key="user.id" class="user-item">
                  <el-avatar :size="50" :src="user.avatar">
                    {{ user.realName?.[0] || 'U' }}
                  </el-avatar>
                  <div class="user-info">
                    <h4>{{ user.realName }}</h4>
                    <p>{{ user.email }}</p>
                    <div class="user-stats">
                      <span>关注 {{ user.followingCount || 0 }}</span>
                      <span>粉丝 {{ user.followerCount || 0 }}</span>
                    </div>
                  </div>
                  <el-button 
                    :type="user.isFollowed ? 'default' : 'primary'"
                    size="small"
                    @click.stop="handleToggleFollow(user)"
                  >
                    {{ user.isFollowed ? '已关注' : '关注' }}
                  </el-button>
                </div>
                
                <div class="pagination-container">
                  <el-pagination
                    v-model:current-page="followingPage.current"
                    :page-size="followingPage.size"
                    :total="followingPage.total"
                    layout="prev, pager, next"
                    @current-change="loadFollowing"
                  />
                </div>
              </div>
            </el-tab-pane>
            
            <el-tab-pane label="我的粉丝" name="followers">
              <div v-if="followersLoading" class="loading-container">
                <el-skeleton :rows="3" animated />
              </div>
              <div v-else-if="followersList.length === 0" class="empty-container">
                <el-empty description="暂时还没有粉丝" />
              </div>
              <div v-else class="user-list">
                <div v-for="user in followersList" :key="user.id" class="user-item">
                  <el-avatar :size="50" :src="user.avatar">
                    {{ user.realName?.[0] || 'U' }}
                  </el-avatar>
                  <div class="user-info">
                    <h4>{{ user.realName }}</h4>
                    <p>{{ user.email }}</p>
                    <div class="user-stats">
                      <span>关注 {{ user.followingCount || 0 }}</span>
                      <span>粉丝 {{ user.followerCount || 0 }}</span>
                    </div>
                  </div>
                  <el-button 
                    :type="user.isFollowed ? 'default' : 'primary'"
                    size="small"
                    @click.stop="handleToggleFollow(user)"
                  >
                    {{ user.isFollowed ? '已关注' : '回关' }}
                  </el-button>
                </div>
                
                <div class="pagination-container">
                  <el-pagination
                    v-model:current-page="followersPage.current"
                    :page-size="followersPage.size"
                    :total="followersPage.total"
                    layout="prev, pager, next"
                    @current-change="loadFollowers"
                  />
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
      
      <!-- 右侧：推荐关注 -->
      <el-col :span="8">
        <el-card class="recommend-card">
          <template #header>
            <div class="card-header">
              <span>🌟 推荐关注</span>
              <el-button text size="small" @click="loadRecommend">
                <el-icon><Refresh /></el-icon> 换一批
              </el-button>
            </div>
          </template>
          
          <div v-if="recommendLoading" class="loading-container">
            <el-skeleton :rows="3" animated />
          </div>
          <div v-else-if="recommendList.length === 0" class="empty-container">
            <el-empty description="暂无推荐" :image-size="60" />
          </div>
          <div v-else class="recommend-list">
            <div v-for="user in recommendList" :key="user.id" class="recommend-item">
              <el-avatar :size="45" :src="user.avatar">
                {{ user.realName?.[0] || 'U' }}
              </el-avatar>
              <div class="recommend-info">
                <h4>{{ user.realName }}</h4>
                <p>粉丝 {{ user.followerCount || 0 }}</p>
              </div>
              <el-button 
                type="primary" 
                size="small" 
                plain
                @click="handleToggleFollow(user)"
              >
                <el-icon><Plus /></el-icon> 关注
              </el-button>
            </div>
          </div>
        </el-card>
        
        <!-- 我的关注统计 -->
        <el-card class="stats-card">
          <div class="stats-item">
            <div class="stats-number">{{ userStore.user?.followingCount || 0 }}</div>
            <div class="stats-label">关注</div>
          </div>
          <div class="stats-divider"></div>
          <div class="stats-item">
            <div class="stats-number">{{ userStore.user?.followerCount || 0 }}</div>
            <div class="stats-label">粉丝</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { View, ChatDotRound, Refresh, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { 
  getFollowFeed, 
  getMyFollowing, 
  getMyFollowers, 
  getRecommendUsers,
  toggleFollow 
} from '@/api/follow'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('feed')

// 关注动态
const feedList = ref([])
const feedLoading = ref(false)
const feedPage = ref({ current: 1, size: 10, total: 0 })

// 我的关注
const followingList = ref([])
const followingLoading = ref(false)
const followingPage = ref({ current: 1, size: 10, total: 0 })

// 我的粉丝
const followersList = ref([])
const followersLoading = ref(false)
const followersPage = ref({ current: 1, size: 10, total: 0 })

// 推荐关注
const recommendList = ref([])
const recommendLoading = ref(false)

// 加载关注动态
const loadFeed = async () => {
  feedLoading.value = true
  try {
    const data = await getFollowFeed({
      current: feedPage.value.current,
      size: feedPage.value.size
    })
    feedList.value = data.records
    feedPage.value.total = data.total
  } catch (error) {
    console.error('加载动态失败:', error)
  } finally {
    feedLoading.value = false
  }
}

// 加载我的关注
const loadFollowing = async () => {
  followingLoading.value = true
  try {
    const data = await getMyFollowing({
      current: followingPage.value.current,
      size: followingPage.value.size
    })
    followingList.value = data.records
    followingPage.value.total = data.total
  } catch (error) {
    console.error('加载关注列表失败:', error)
  } finally {
    followingLoading.value = false
  }
}

// 加载我的粉丝
const loadFollowers = async () => {
  followersLoading.value = true
  try {
    const data = await getMyFollowers({
      current: followersPage.value.current,
      size: followersPage.value.size
    })
    followersList.value = data.records
    followersPage.value.total = data.total
  } catch (error) {
    console.error('加载粉丝列表失败:', error)
  } finally {
    followersLoading.value = false
  }
}

// 加载推荐用户
const loadRecommend = async () => {
  recommendLoading.value = true
  try {
    recommendList.value = await getRecommendUsers(5)
  } catch (error) {
    console.error('加载推荐失败:', error)
  } finally {
    recommendLoading.value = false
  }
}

// 关注/取消关注
const handleToggleFollow = async (user) => {
  try {
    const result = await toggleFollow(user.id)
    user.isFollowed = result.isFollowing
    ElMessage.success(result.message)
    
    // 如果在推荐列表中关注了，从列表移除
    if (result.isFollowing) {
      recommendList.value = recommendList.value.filter(u => u.id !== user.id)
    }
    
    // 刷新用户信息（等待完成）
    await userStore.fetchUserInfo()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// Tab 切换
const handleTabChange = (tab) => {
  if (tab === 'feed' && feedList.value.length === 0) {
    loadFeed()
  } else if (tab === 'following' && followingList.value.length === 0) {
    loadFollowing()
  } else if (tab === 'followers' && followersList.value.length === 0) {
    loadFollowers()
  }
}

// 跳转到文章详情
const goToArticle = (id) => {
  router.push(`/article/${id}`)
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  
  return date.toLocaleDateString()
}

// 获取板块类型名称
const getBoardTypeName = (type) => {
  const map = { OFFICIAL: '官方', CAMPUS: '全校', COLLEGE: '学院' }
  return map[type] || type
}

// 获取板块标签类型
const getBoardTagType = (type) => {
  const map = { OFFICIAL: 'danger', CAMPUS: 'success', COLLEGE: 'warning' }
  return map[type] || 'info'
}

onMounted(() => {
  loadFeed()
  loadRecommend()
})
</script>

<style scoped>
.follow-page {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
  padding: 60px 40px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.9) 0%, rgba(118, 75, 162, 0.9) 100%);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  color: white;
  box-shadow: 0 20px 40px rgba(102, 126, 234, 0.3);
  position: relative;
  overflow: hidden;
  animation: fadeInDown 0.6s ease-out;
}

.page-header::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(255,255,255,0.2) 0%, rgba(255,255,255,0) 70%);
  border-radius: 50%;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.page-header h1 {
  margin: 0 0 12px;
  font-size: 36px;
  font-weight: 800;
  position: relative;
  z-index: 1;
}

.page-header p {
  margin: 0;
  opacity: 0.95;
  font-size: 16px;
  position: relative;
  z-index: 1;
}

.content-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  animation: fadeInUp 0.6s ease-out 0.1s both;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.loading-container, .empty-container {
  padding: 40px 0;
}

/* 动态列表 */
.feed-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feed-item {
  padding: 20px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  animation: fadeInUp 0.4s ease-out both;
}

.feed-item:nth-child(1) { animation-delay: 0.05s; }
.feed-item:nth-child(2) { animation-delay: 0.1s; }
.feed-item:nth-child(3) { animation-delay: 0.15s; }
.feed-item:nth-child(4) { animation-delay: 0.2s; }
.feed-item:nth-child(5) { animation-delay: 0.25s; }

.feed-item:hover {
  box-shadow: 0 12px 32px rgba(102, 126, 234, 0.15);
  transform: translateY(-6px);
  background: rgba(255, 255, 255, 0.9);
  border-color: #a18cd1;
}

.feed-item:hover .feed-title {
  color: #667eea;
}

.feed-author {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.author-info {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-weight: 600;
  color: #303133;
}

.publish-time {
  font-size: 12px;
  color: #909399;
}

.feed-content {
  margin-bottom: 12px;
}

.feed-title {
  margin: 0 0 8px;
  font-size: 18px;
  color: #303133;
}

.feed-summary {
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.feed-cover {
  width: 100%;
  max-height: 200px;
  object-fit: cover;
  border-radius: 8px;
  margin-top: 12px;
}

.feed-stats {
  display: flex;
  align-items: center;
  gap: 16px;
  color: #909399;
  font-size: 14px;
}

.feed-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 用户列表 */
.user-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.user-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  background: rgba(255, 255, 255, 0.85);
  border-color: #a18cd1;
}

.user-info {
  flex: 1;
}

.user-info h4 {
  margin: 0 0 4px;
  font-size: 16px;
  color: #303133;
}

.user-info p {
  margin: 0 0 4px;
  font-size: 13px;
  color: #909399;
}

.user-stats {
  font-size: 12px;
  color: #909399;
}

.user-stats span {
  margin-right: 12px;
}

/* 推荐卡片 */
.recommend-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
  animation: fadeInUp 0.6s ease-out 0.2s both;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.recommend-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recommend-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.recommend-info {
  flex: 1;
}

.recommend-info h4 {
  margin: 0 0 4px;
  font-size: 15px;
  color: #303133;
}

.recommend-info p {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

/* 统计卡片 */
.stats-card {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30px 20px;
  animation: fadeInUp 0.6s ease-out 0.3s both;
}

.stats-item {
  text-align: center;
  flex: 1;
}

.stats-number {
  font-size: 32px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stats-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.stats-divider {
  width: 1px;
  height: 40px;
  background: #ebeef5;
  margin: 0 20px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
