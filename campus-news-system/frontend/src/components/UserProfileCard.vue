<template>
  <div class="user-profile-analysis">
    <!-- 阅读等级卡片 -->
    <div class="profile-card reading-level-card">
      <div class="card-header">
        <div class="header-icon">
          <el-icon :size="20"><Trophy /></el-icon>
        </div>
        <h3>阅读等级</h3>
      </div>
      <div class="level-content" v-if="!loading">
        <div class="level-badge" :class="getLevelClass(profile.reading_level?.level)">
          <span class="level-name">{{ profile.reading_level?.level || '新手' }}</span>
          <span class="level-score">{{ profile.reading_level?.score || 0 }}分</span>
        </div>
        <p class="level-desc">{{ profile.reading_level?.description || '暂无阅读记录' }}</p>
        <div class="level-metrics">
          <div class="metric-item">
            <span class="metric-label">点赞率</span>
            <el-progress 
              :percentage="profile.reading_level?.metrics?.like_rate || 0" 
              :stroke-width="6"
              :color="'#667eea'"
            />
          </div>
          <div class="metric-item">
            <span class="metric-label">收藏率</span>
            <el-progress 
              :percentage="profile.reading_level?.metrics?.favorite_rate || 0" 
              :stroke-width="6"
              :color="'#f093fb'"
            />
          </div>
          <div class="metric-item">
            <span class="metric-label">评论率</span>
            <el-progress 
              :percentage="profile.reading_level?.metrics?.comment_rate || 0" 
              :stroke-width="6"
              :color="'#43e97b'"
            />
          </div>
        </div>
      </div>
      <el-skeleton v-else :rows="4" animated />
    </div>

    <!-- 用户类型标签 -->
    <div class="profile-card user-type-card">
      <div class="card-header">
        <div class="header-icon">
          <el-icon :size="20"><User /></el-icon>
        </div>
        <h3>用户标签</h3>
      </div>
      <div class="type-tags" v-if="!loading">
        <el-tag 
          v-for="(type, index) in profile.user_type" 
          :key="index"
          :type="getTagType(index)"
          effect="dark"
          round
          class="user-tag"
        >
          {{ type }}
        </el-tag>
        <el-tag v-if="!profile.user_type?.length" type="info" effect="plain">
          暂无标签
        </el-tag>
      </div>
      <el-skeleton v-else :rows="1" animated />
    </div>

    <!-- 兴趣标签云 -->
    <div class="profile-card interest-card">
      <div class="card-header">
        <div class="header-icon">
          <el-icon :size="20"><PriceTag /></el-icon>
        </div>
        <h3>兴趣偏好</h3>
      </div>
      <div class="interest-tags" v-if="!loading">
        <span 
          v-for="(tag, index) in (profile.interest_tags || []).slice(0, 12)" 
          :key="index"
          class="interest-tag"
          :style="getTagStyle(tag.weight)"
        >
          {{ tag.tag }}
        </span>
        <span v-if="!profile.interest_tags?.length" class="empty-text">
          暂无兴趣数据，多浏览文章后生成
        </span>
      </div>
      <el-skeleton v-else :rows="2" animated />
    </div>

    <!-- 分类偏好 -->
    <div class="profile-card category-card">
      <div class="card-header">
        <div class="header-icon">
          <el-icon :size="20"><PieChart /></el-icon>
        </div>
        <h3>分类偏好</h3>
      </div>
      <div class="category-list" v-if="!loading">
        <div 
          v-for="cat in profile.category_preference" 
          :key="cat.category"
          class="category-item"
        >
          <div class="cat-info">
            <span class="cat-dot" :style="{ background: getCategoryColor(cat.category) }"></span>
            <span class="cat-name">{{ cat.name }}</span>
          </div>
          <div class="cat-bar-wrap">
            <div 
              class="cat-bar" 
              :style="{ 
                width: cat.percentage + '%', 
                background: getCategoryColor(cat.category) 
              }"
            ></div>
          </div>
          <span class="cat-percent">{{ cat.percentage }}%</span>
        </div>
        <div v-if="!profile.category_preference?.length" class="empty-text">
          暂无分类偏好数据
        </div>
      </div>
      <el-skeleton v-else :rows="3" animated />
    </div>

    <!-- 活跃时间 -->
    <div class="profile-card activity-card">
      <div class="card-header">
        <div class="header-icon">
          <el-icon :size="20"><Clock /></el-icon>
        </div>
        <h3>活跃时间</h3>
      </div>
      <div class="activity-content" v-if="!loading">
        <div class="activity-section">
          <span class="section-label">高峰时段</span>
          <div class="peak-hours">
            <span 
              v-for="hour in profile.activity_pattern?.peak_hours" 
              :key="hour"
              class="hour-badge"
            >
              {{ hour }}:00
            </span>
            <span v-if="!profile.activity_pattern?.peak_hours?.length" class="empty-inline">
              暂无数据
            </span>
          </div>
        </div>
        <div class="activity-section">
          <span class="section-label">活跃日</span>
          <div class="active-days">
            <span 
              v-for="day in profile.activity_pattern?.active_days" 
              :key="day"
              class="day-badge"
            >
              {{ day }}
            </span>
            <span v-if="!profile.activity_pattern?.active_days?.length" class="empty-inline">
              暂无数据
            </span>
          </div>
        </div>
      </div>
      <el-skeleton v-else :rows="2" animated />
    </div>

    <!-- 行为统计 -->
    <div class="profile-card behavior-card">
      <div class="card-header">
        <div class="header-icon">
          <el-icon :size="20"><DataAnalysis /></el-icon>
        </div>
        <h3>行为统计</h3>
      </div>
      <div class="behavior-stats" v-if="!loading">
        <div class="stat-item">
          <div class="stat-icon like-icon">
            <el-icon><Star /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ profile.behavior_stats?.like_count || 0 }}</span>
            <span class="stat-label">点赞</span>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon favorite-icon">
            <el-icon><Collection /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ profile.behavior_stats?.favorite_count || 0 }}</span>
            <span class="stat-label">收藏</span>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon comment-icon">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ profile.behavior_stats?.comment_count || 0 }}</span>
            <span class="stat-label">评论</span>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon article-icon">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ profile.behavior_stats?.unique_articles || 0 }}</span>
            <span class="stat-label">阅读</span>
          </div>
        </div>
      </div>
      <el-skeleton v-else :rows="1" animated />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { getUserProfile } from '@/api/userProfile'
import { 
  Trophy, User, PriceTag, PieChart, Clock, DataAnalysis,
  Star, Collection, ChatDotRound, Document
} from '@element-plus/icons-vue'

const props = defineProps({
  userId: {
    type: Number,
    required: true
  }
})

const loading = ref(true)
const profile = ref({})

const fetchProfile = async () => {
  if (!props.userId) return
  
  loading.value = true
  try {
    const res = await getUserProfile(props.userId)
    // Python 服务返回格式: {success: true, data: {...}, message: "..."}
    if (res?.success && res?.data) {
      profile.value = res.data
    }
  } catch (error) {
    console.error('获取用户画像失败:', error)
  } finally {
    loading.value = false
  }
}

const getLevelClass = (level) => {
  const classes = {
    '资深读者': 'level-expert',
    '活跃读者': 'level-active',
    '普通读者': 'level-normal',
    '轻度读者': 'level-light',
    '新手': 'level-newbie'
  }
  return classes[level] || 'level-newbie'
}

const getTagType = (index) => {
  const types = ['', 'success', 'warning', 'danger', 'info']
  return types[index % types.length]
}

const getTagStyle = (weight) => {
  const size = 12 + weight * 6
  const opacity = 0.6 + weight * 0.4
  return {
    fontSize: `${size}px`,
    opacity: opacity,
    fontWeight: weight > 0.7 ? '600' : '400'
  }
}

const getCategoryColor = (category) => {
  const colors = {
    'OFFICIAL': '#f56c6c',
    'CAMPUS': '#409eff',
    'COLLEGE': '#67c23a'
  }
  return colors[category] || '#909399'
}

watch(() => props.userId, () => {
  fetchProfile()
})

onMounted(() => {
  fetchProfile()
})
</script>

<style scoped>
.user-profile-analysis {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.profile-card {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06),
              0 0 0 1px rgba(255, 255, 255, 0.4) inset;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.profile-card:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.65);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1),
              0 0 0 1px rgba(255, 255, 255, 0.6) inset;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.header-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

/* 阅读等级 */
.level-content {
  text-align: center;
}

.level-badge {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 32px;
  border-radius: 16px;
  margin-bottom: 12px;
}

.level-badge.level-expert {
  background: linear-gradient(135deg, #ffd700, #ff8c00);
  color: white;
}

.level-badge.level-active {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.level-badge.level-normal {
  background: linear-gradient(135deg, #4facfe, #00f2fe);
  color: white;
}

.level-badge.level-light {
  background: linear-gradient(135deg, #a8edea, #fed6e3);
  color: #606266;
}

.level-badge.level-newbie {
  background: linear-gradient(135deg, #e0e0e0, #c0c0c0);
  color: #606266;
}

.level-name {
  font-size: 18px;
  font-weight: 700;
}

.level-score {
  font-size: 13px;
  opacity: 0.9;
  margin-top: 4px;
}

.level-desc {
  color: #909399;
  font-size: 13px;
  margin: 0 0 16px 0;
}

.level-metrics {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.metric-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.metric-label {
  width: 50px;
  font-size: 12px;
  color: #909399;
  text-align: right;
}

.metric-item :deep(.el-progress) {
  flex: 1;
}

/* 用户标签 */
.type-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.user-tag {
  font-size: 13px;
  padding: 6px 14px;
}

/* 兴趣标签 */
.interest-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.interest-tag {
  padding: 4px 12px;
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
  border-radius: 20px;
  transition: all 0.3s ease;
  cursor: default;
}

.interest-tag:hover {
  background: rgba(102, 126, 234, 0.2);
  transform: scale(1.05);
}

/* 分类偏好 */
.category-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cat-info {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 80px;
}

.cat-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.cat-name {
  font-size: 13px;
  color: #606266;
}

.cat-bar-wrap {
  flex: 1;
  height: 8px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 4px;
  overflow: hidden;
}

.cat-bar {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.cat-percent {
  width: 45px;
  text-align: right;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

/* 活跃时间 */
.activity-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.activity-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-label {
  width: 60px;
  font-size: 13px;
  color: #909399;
}

.peak-hours, .active-days {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hour-badge, .day-badge {
  padding: 4px 12px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.day-badge {
  background: linear-gradient(135deg, #43e97b, #38f9d7);
  color: #2c3e50;
}

/* 行为统计 */
.behavior-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 10px 8px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 10px;
  transition: all 0.3s ease;
}

.stat-item:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.8);
}

.stat-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
}

.like-icon { background: linear-gradient(135deg, #f093fb, #f5576c); }
.favorite-icon { background: linear-gradient(135deg, #ffd700, #ff8c00); }
.comment-icon { background: linear-gradient(135deg, #43e97b, #38f9d7); }
.article-icon { background: linear-gradient(135deg, #4facfe, #00f2fe); }

.stat-info {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 18px;
  font-weight: 700;
  color: #2c3e50;
}

.stat-label {
  font-size: 11px;
  color: #909399;
}

.empty-text, .empty-inline {
  color: #c0c4cc;
  font-size: 13px;
}


</style>
