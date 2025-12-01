<template>
  <div class="profile-page">
    <el-row :gutter="20">
      <!-- 左侧用户信息 -->
      <el-col :span="6">
        <el-card class="user-profile-card">
          <div class="user-card">
            <el-avatar :size="90" class="user-avatar">{{ userStore.user?.realName?.[0] }}</el-avatar>
            <h3>{{ userStore.user?.realName }}</h3>
            <p class="username">@{{ userStore.user?.username }}</p>
            <div class="role-tags">
              <el-tag v-for="role in userStore.user?.roles" :key="role.id" effect="dark" size="small">
                {{ getRoleName(role.roleName) }}
              </el-tag>
            </div>
            <!-- 关注数据 -->
            <div class="follow-stats">
              <div class="follow-item" @click="$router.push('/follow')">
                <span class="count">{{ userStore.user?.followingCount || 0 }}</span>
                <span class="label">关注</span>
              </div>
              <div class="divider"></div>
              <div class="follow-item" @click="$router.push('/follow')">
                <span class="count">{{ userStore.user?.followerCount || 0 }}</span>
                <span class="label">粉丝</span>
              </div>
            </div>
          </div>
        </el-card>
        
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <el-icon><User /></el-icon>
              <span>基本信息</span>
            </div>
          </template>
          <div class="user-info">
            <div class="info-item">
              <el-icon><Message /></el-icon>
              <span class="label">邮箱</span>
              <span class="value">{{ userStore.user?.email || '未设置' }}</span>
            </div>
            <div class="info-item">
              <el-icon><Phone /></el-icon>
              <span class="label">手机</span>
              <span class="value">{{ userStore.user?.phone || '未设置' }}</span>
            </div>
            <div class="info-item" v-if="userStore.user?.college">
              <el-icon><School /></el-icon>
              <span class="label">学院</span>
              <span class="value">{{ userStore.user?.college?.name }}</span>
            </div>
            <div class="info-item" v-if="userStore.user?.studentId">
              <el-icon><Postcard /></el-icon>
              <span class="label">学号</span>
              <span class="value">{{ userStore.user?.studentId }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <!-- 右侧内容区 -->
      <el-col :span="18">
        <!-- 数据统计卡片 -->
        <el-row :gutter="16" class="stats-row">
          <el-col :span="6">
            <el-card class="stat-card" shadow="hover">
              <div class="stat-content">
                <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
                  <el-icon :size="24"><Document /></el-icon>
                </div>
                <div class="stat-info">
                  <span class="stat-value">{{ stats.totalArticles }}</span>
                  <span class="stat-label">发布文章</span>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card" shadow="hover">
              <div class="stat-content">
                <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
                  <el-icon :size="24"><View /></el-icon>
                </div>
                <div class="stat-info">
                  <span class="stat-value">{{ stats.totalViews }}</span>
                  <span class="stat-label">总浏览量</span>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card" shadow="hover">
              <div class="stat-content">
                <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
                  <el-icon :size="24"><Star /></el-icon>
                </div>
                <div class="stat-info">
                  <span class="stat-value">{{ stats.totalLikes }}</span>
                  <span class="stat-label">获得点赞</span>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card" shadow="hover">
              <div class="stat-content">
                <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);">
                  <el-icon :size="24"><ChatDotRound /></el-icon>
                </div>
                <div class="stat-info">
                  <span class="stat-value">{{ stats.totalComments }}</span>
                  <span class="stat-label">收到评论</span>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
        
        <!-- 文章分布统计 -->
        <el-row :gutter="16" class="chart-row">
          <el-col :span="12">
            <el-card class="chart-card">
              <template #header>
                <div class="card-header">
                  <el-icon><PieChart /></el-icon>
                  <span>文章分布</span>
                </div>
              </template>
              <div class="distribution-list">
                <div class="distribution-item">
                  <div class="dist-info">
                    <span class="dist-dot" style="background-color: #f56c6c;"></span>
                    <span class="dist-name">官方新闻</span>
                  </div>
                  <div class="dist-bar-wrap">
                    <div class="dist-bar" :style="{ width: getDistPercent('OFFICIAL') + '%', backgroundColor: '#f56c6c' }"></div>
                  </div>
                  <span class="dist-count">{{ stats.officialCount }} 篇</span>
                </div>
                <div class="distribution-item">
                  <div class="dist-info">
                    <span class="dist-dot" style="background-color: #409eff;"></span>
                    <span class="dist-name">全校新闻</span>
                  </div>
                  <div class="dist-bar-wrap">
                    <div class="dist-bar" :style="{ width: getDistPercent('CAMPUS') + '%', backgroundColor: '#409eff' }"></div>
                  </div>
                  <span class="dist-count">{{ stats.campusCount }} 篇</span>
                </div>
                <div class="distribution-item">
                  <div class="dist-info">
                    <span class="dist-dot" style="background-color: #67c23a;"></span>
                    <span class="dist-name">学院新闻</span>
                  </div>
                  <div class="dist-bar-wrap">
                    <div class="dist-bar" :style="{ width: getDistPercent('COLLEGE') + '%', backgroundColor: '#67c23a' }"></div>
                  </div>
                  <span class="dist-count">{{ stats.collegeCount }} 篇</span>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card class="chart-card">
              <template #header>
                <div class="card-header">
                  <el-icon><TrendCharts /></el-icon>
                  <span>热门文章 TOP3</span>
                </div>
              </template>
              <div class="top-articles">
                <div v-for="(article, index) in topArticles" :key="article.id" class="top-article-item" @click="$router.push(`/article/${article.id}`)">
                  <span class="rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
                  <span class="title">{{ article.title }}</span>
                  <span class="views">
                    <el-icon><View /></el-icon>
                    {{ article.viewCount }}
                  </span>
                </div>
                <el-empty v-if="topArticles.length === 0" description="暂无数据" :image-size="60" />
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 我的文章列表 -->
        <el-card class="articles-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon><List /></el-icon>
                <span>我的文章</span>
              </div>
              <el-button type="primary" size="small" @click="$router.push('/publish')">
                <el-icon><Plus /></el-icon>
                发布文章
              </el-button>
            </div>
          </template>
          
          <div v-loading="loading">
            <el-empty v-if="articles.length === 0 && !loading" description="暂无文章" />
            
            <el-table :data="articles" stripe>
              <el-table-column prop="title" label="标题" />
              <el-table-column prop="boardType" label="板块" width="120">
                <template #default="{ row }">
                  <el-tag :type="getBoardTypeTag(row.boardType)" size="small">
                    {{ getBoardTypeName(row.boardType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="viewCount" label="浏览" width="80" />
              <el-table-column prop="likeCount" label="点赞" width="80" />
              <el-table-column prop="commentCount" label="评论" width="80" />
              <el-table-column prop="createdAt" label="发布时间" width="160">
                <template #default="{ row }">
                  {{ formatTime(row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150">
                <template #default="{ row }">
                  <el-button text size="small" @click="handleEdit(row)">编辑</el-button>
                  <el-button text size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            
            <el-pagination
              v-if="total > 0"
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="total"
              layout="total, prev, pager, next"
              @current-change="fetchArticles"
              style="margin-top: 20px; justify-content: center;"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getArticleList, deleteArticle } from '@/api/article'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Message, Phone, School, Postcard, Document, View, Star, ChatDotRound, PieChart, TrendCharts, List, Plus } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const articles = ref([])
const topArticles = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 统计数据
const stats = ref({
  totalArticles: 0,
  totalViews: 0,
  totalLikes: 0,
  totalComments: 0,
  officialCount: 0,
  campusCount: 0,
  collegeCount: 0
})

const fetchArticles = async () => {
  loading.value = true
  try {
    const data = await getArticleList({
      current: currentPage.value,
      size: pageSize.value,
      authorId: userStore.user?.id
    })
    articles.value = data.records
    total.value = data.total
    
    // 计算统计数据
    calculateStats(data.records, data.total)
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 获取所有文章用于统计
const fetchAllStats = async () => {
  try {
    const data = await getArticleList({
      current: 1,
      size: 100,
      authorId: userStore.user?.id
    })
    
    const allArticles = data.records || []
    
    // 使用实际获取到的文章数量
    const officialCount = allArticles.filter(a => a.boardType === 'OFFICIAL').length
    const campusCount = allArticles.filter(a => a.boardType === 'CAMPUS').length
    const collegeCount = allArticles.filter(a => a.boardType === 'COLLEGE').length
    
    stats.value.totalArticles = officialCount + campusCount + collegeCount
    stats.value.totalViews = allArticles.reduce((sum, a) => sum + (a.viewCount || 0), 0)
    stats.value.totalLikes = allArticles.reduce((sum, a) => sum + (a.likeCount || 0), 0)
    stats.value.totalComments = allArticles.reduce((sum, a) => sum + (a.commentCount || 0), 0)
    stats.value.officialCount = officialCount
    stats.value.campusCount = campusCount
    stats.value.collegeCount = collegeCount
    
    console.log('统计数据:', stats.value) // 调试日志
    
    // 热门文章 TOP3
    topArticles.value = [...allArticles]
      .sort((a, b) => (b.viewCount || 0) - (a.viewCount || 0))
      .slice(0, 3)
  } catch (error) {
    console.error(error)
  }
}

// 计算分布百分比
const getDistPercent = (type) => {
  const total = stats.value.totalArticles
  if (total === 0) return 0
  const count = type === 'OFFICIAL' ? stats.value.officialCount 
              : type === 'CAMPUS' ? stats.value.campusCount 
              : stats.value.collegeCount
  return Math.round((count / total) * 100)
}

const calculateStats = (records, totalCount) => {
  // 这个函数用于分页数据的快速统计，完整统计在 fetchAllStats 中
}

const handleEdit = (article) => {
  router.push({
    path: '/publish',
    query: { id: article.id }
  })
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这篇文章吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteArticle(id)
    ElMessage.success('删除成功')
    fetchArticles()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const getRoleName = (roleName) => {
  const names = {
    ADMIN: '管理员',
    TEACHER: '教师',
    STUDENT: '学生'
  }
  return names[roleName] || roleName
}

const getBoardTypeName = (type) => {
  const types = {
    OFFICIAL: '官方新闻',
    CAMPUS: '全校新闻',
    COLLEGE: '学院新闻'
  }
  return types[type] || type
}

const getBoardTypeTag = (type) => {
  const tags = {
    OFFICIAL: 'danger',
    CAMPUS: 'primary',
    COLLEGE: 'success'
  }
  return tags[type] || ''
}

const formatTime = (time) => {
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchArticles()
  fetchAllStats()
})
</script>

<style scoped>
.profile-page {
  padding: 20px;
}

/* 用户卡片 */
.user-profile-card {
  border-radius: 12px;
}

.user-card {
  text-align: center;
  padding: 20px 0;
}

.user-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 32px;
  font-weight: bold;
}

.user-card h3 {
  margin: 15px 0 5px;
  font-size: 20px;
  color: #303133;
}

.user-card .username {
  color: #909399;
  margin: 5px 0 15px;
}

.role-tags {
  margin-bottom: 20px;
}

/* 关注统计 */
.follow-stats {
  display: flex;
  justify-content: center;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}

.follow-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  padding: 0 20px;
}

.follow-item:hover .count {
  color: #409eff;
}

.follow-item .count {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.follow-item .label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.follow-stats .divider {
  width: 1px;
  height: 30px;
  background: #ebeef5;
}

/* 信息卡片 */
.info-card {
  margin-top: 20px;
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.user-info .info-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-info .info-item .el-icon {
  color: #909399;
}

.user-info .info-item .label {
  color: #909399;
  width: 50px;
}

.user-info .info-item .value {
  color: #303133;
}

/* 统计卡片 */
.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  border-radius: 12px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

/* 图表卡片 */
.chart-row {
  margin-bottom: 16px;
  display: flex;
}

.chart-row .el-col {
  display: flex;
}

.chart-card {
  border-radius: 12px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chart-card :deep(.el-card__body) {
  flex: 1;
}

/* 文章分布 */
.distribution-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.distribution-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.dist-info {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 90px;
}

.dist-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dist-name {
  font-size: 15px;
  color: #303133;
  font-weight: 500;
}

.dist-bar-wrap {
  flex: 1;
  height: 10px;
  background: #f0f2f5;
  border-radius: 5px;
  overflow: hidden;
}

.dist-bar {
  height: 100%;
  border-radius: 5px;
  transition: width 0.5s ease;
}

.dist-count {
  font-size: 15px;
  color: #606266;
  width: 55px;
  text-align: right;
  font-weight: 500;
}

/* 热门文章 */
.top-articles {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.top-article-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.3s;
}

.top-article-item:hover {
  background: #f5f7fa;
}

.top-article-item .rank {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: white;
  background: #909399;
}

.top-article-item .rank-1 {
  background: linear-gradient(135deg, #f5af19 0%, #f12711 100%);
}

.top-article-item .rank-2 {
  background: linear-gradient(135deg, #c0c0c0 0%, #8e8e8e 100%);
}

.top-article-item .rank-3 {
  background: linear-gradient(135deg, #cd7f32 0%, #8b5a2b 100%);
}

.top-article-item .title {
  flex: 1;
  font-size: 14px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.top-article-item .views {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

/* 文章列表卡片 */
.articles-card {
  border-radius: 12px;
}

.articles-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.articles-card .header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
</style>
