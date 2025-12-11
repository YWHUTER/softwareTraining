<template>
  <div class="main-layout" :class="{ 'dark-mode': isDark }" :style="{ backgroundImage: `url(${bgImage})` }">
    <!-- 粒子背景 -->
    <ParticleBackground />
    
    <el-container class="main-container" style="position: relative; z-index: 1;">
      <!-- 顶部导航栏 -->
      <el-header class="header">
        <div class="header-content">
          <!-- Logo 区域 -->
          <div class="logo" @click="$router.push('/')">
            <img src="@/assets/whut-logo.png" alt="校徽" class="logo-img" />
            <div class="logo-text">
              <span class="logo-title">校园新闻</span>
              <span class="logo-subtitle">WHUT News</span>
            </div>
          </div>

          <!-- 主导航菜单 -->
          <el-menu
            :default-active="activeMenu"
            mode="horizontal"
            :ellipsis="false"
            @select="handleMenuSelect"
            class="main-menu"
          >
            <el-menu-item index="/" class="menu-item">
              <el-icon><House /></el-icon>
              <span>首页</span>
            </el-menu-item>
            <el-sub-menu index="news" class="menu-item news-menu-item">
              <template #title>
                <el-icon><Reading /></el-icon>
                <span>武理新闻</span>
              </template>
              <el-menu-item index="/board/OFFICIAL">
                <el-icon><Document /></el-icon>
                <span>官方新闻</span>
              </el-menu-item>
              <el-menu-item index="/board/CAMPUS">
                <el-icon><School /></el-icon>
                <span>全校新闻</span>
              </el-menu-item>
              <el-menu-item index="/board/COLLEGE">
                <el-icon><OfficeBuilding /></el-icon>
                <span>学院新闻</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item index="/video" class="menu-item video-menu-item">
              <el-icon><VideoCamera /></el-icon>
              <span>武理视界</span>
            </el-menu-item>
            <el-menu-item index="/marketplace" class="menu-item marketplace-menu-item">
              <el-icon><Shop /></el-icon>
              <span>校园集市</span>
            </el-menu-item>
            <el-menu-item index="/follow" class="menu-item follow-menu-item">
              <el-icon><Star /></el-icon>
              <span>关注</span>
            </el-menu-item>
            <el-menu-item index="/search" class="menu-item search-menu-item">
              <el-icon><Search /></el-icon>
              <span>搜索</span>
            </el-menu-item>
            <el-sub-menu index="ai" class="menu-item ai-menu-item">
              <template #title>
                <el-icon><Cpu /></el-icon>
                <span>AI功能</span>
              </template>
              <el-menu-item index="/ai-assistant">
                <el-icon><ChatDotRound /></el-icon>
                <span>智能对话</span>
              </el-menu-item>
              <el-menu-item index="/ai-summary">
                <el-icon><Document /></el-icon>
                <span>智能摘要</span>
              </el-menu-item>
              <el-menu-item index="/ai-sentiment">
                <el-icon><Histogram /></el-icon>
                <span>情感分析</span>
              </el-menu-item>
              <el-menu-item index="/ai-help">
                <el-icon><QuestionFilled /></el-icon>
                <span>使用帮助</span>
              </el-menu-item>
            </el-sub-menu>
          </el-menu>

          <!-- 用户操作区 -->
          <div class="user-actions">
            <!-- 主题切换按钮 -->
            <el-tooltip 
              :content="themeTooltip" 
              placement="bottom"
              :show-after="500"
            >
              <el-button 
                circle 
                class="theme-toggle-btn"
                @click="toggleTheme"
              >
                <el-icon :size="18">
                  <Sunny v-if="isDark" />
                  <Moon v-else-if="themeMode === 'light'" />
                  <Monitor v-else />
                </el-icon>
              </el-button>
            </el-tooltip>
            
            <template v-if="userStore.isLogin">
              <!-- 发布按钮 -->
              <el-button 
                type="primary" 
                @click="$router.push('/publish')"
                class="publish-btn"
                round
              >
                <el-icon><Edit /></el-icon>
                <span>发布文章</span>
              </el-button>

              <!-- 通知图标 -->
              <el-popover
                placement="bottom"
                :width="360"
                trigger="click"
                @show="fetchNotifications"
              >
                <template #reference>
                  <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notification-badge">
                    <el-button circle class="notification-btn">
                      <el-icon :size="20"><Bell /></el-icon>
                    </el-button>
                  </el-badge>
                </template>
                <div class="notification-panel">
                  <div class="notification-header">
                    <span class="notification-title">消息通知</span>
                    <el-button v-if="unreadCount > 0" link type="primary" @click="handleMarkAllRead">
                      全部已读
                    </el-button>
                  </div>
                  <div class="notification-list" v-loading="notificationLoading">
                    <div 
                      v-for="item in notifications" 
                      :key="item.id" 
                      class="notification-item"
                      :class="{ 'is-unread': item.isRead === 0 }"
                      @click="handleNotificationClick(item)"
                    >
                      <el-avatar :size="36" :src="item.fromUser?.avatar" class="notification-avatar">
                        {{ item.fromUser?.realName?.[0] }}
                      </el-avatar>
                      <div class="notification-content">
                        <p class="notification-text">{{ item.content }}</p>
                        <span class="notification-time">{{ formatTime(item.createdAt) }}</span>
                      </div>
                      <div v-if="item.isRead === 0" class="unread-dot"></div>
                    </div>
                    <el-empty v-if="notifications.length === 0 && !notificationLoading" description="暂无通知" :image-size="60" />
                  </div>
                </div>
              </el-popover>

              <!-- 用户下拉菜单 -->
              <el-dropdown @command="handleUserCommand" trigger="click" class="user-dropdown">
                <div class="user-info">
                  <el-avatar :size="38" class="user-avatar" :src="userStore.user?.avatar">
                    {{ !userStore.user?.avatar ? userStore.user?.realName?.[0] : '' }}
                  </el-avatar>
                  <span class="user-name">{{ userStore.user?.realName }}</span>
                  <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
                </div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="profile">
                      <el-icon><User /></el-icon>
                      <span>个人中心</span>
                    </el-dropdown-item>
                    <el-dropdown-item command="admin" v-if="userStore.isAdmin">
                      <el-icon><Setting /></el-icon>
                      <span>管理后台</span>
                    </el-dropdown-item>
                    <el-dropdown-item command="logout" divided>
                      <el-icon><SwitchButton /></el-icon>
                      <span>退出登录</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            
            <!-- 未登录状态 -->
            <template v-else>
              <el-button @click="$router.push('/login')" round>登录</el-button>
              <el-button type="primary" @click="$router.push('/register')" round>注册</el-button>
            </template>
          </div>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="main-content">
        <div class="content-wrapper" :class="{ 'video-layout': isVideoPage }">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </el-main>

      <!-- 底部 -->
      <el-footer class="footer">
        <div class="footer-content">
          <div class="footer-info">
            <p class="copyright">© 2025 校园新闻发布系统. All Rights Reserved.</p>
            <p class="beian">基于 Vue3 + Spring Boot 构建</p>
          </div>
          <div class="footer-links">
            <router-link to="/about">关于我们</router-link>
            <span class="divider">|</span>
            <router-link to="/contact">联系方式</router-link>
            <span class="divider">|</span>
            <router-link to="/privacy">隐私政策</router-link>
          </div>
        </div>
      </el-footer>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { storeToRefs } from 'pinia'
import { ElMessage, ElNotification } from 'element-plus'
import { Search, Bell, Cpu, Histogram, QuestionFilled, Sunny, Moon, Monitor } from '@element-plus/icons-vue'
import { getNotifications, getUnreadCount, markAsRead, markAllAsRead } from '@/api/notification'
import bgImage from '@/assets/main-bg.jpg'
import notificationWS from '@/utils/websocket'
import ParticleBackground from '@/components/ParticleBackground.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 主题相关
const themeStore = useThemeStore()
const { isDark, mode: themeMode } = storeToRefs(themeStore)

const themeTooltip = computed(() => {
  if (themeMode.value === 'light') return '当前：浅色模式（点击切换到深色）'
  if (themeMode.value === 'dark') return '当前：深色模式（点击切换到跟随系统）'
  return '当前：跟随系统（点击切换到浅色）'
})

const isVideoPage = computed(() => route.path === '/video')

const toggleTheme = () => {
  themeStore.toggleTheme()
}

const activeMenu = computed(() => route.path)

// 通知相关
const notifications = ref([])
const unreadCount = ref(0)
const notificationLoading = ref(false)
let notificationTimer = null

const handleMenuSelect = (index) => {
  router.push(index)
}

const handleUserCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'admin':
      router.push('/admin')
      break
    case 'logout':
      userStore.logout()
      ElMessage.success('退出成功')
      router.push('/login')
      break
  }
}

// 获取通知列表
const fetchNotifications = async () => {
  if (!userStore.isLogin) return
  notificationLoading.value = true
  try {
    const result = await getNotifications({ current: 1, size: 10 })
    notifications.value = result.records || []
  } catch (error) {
    console.error('获取通知失败:', error)
  } finally {
    notificationLoading.value = false
  }
}

// 获取未读数量
const fetchUnreadCount = async () => {
  if (!userStore.isLogin) return
  try {
    const result = await getUnreadCount()
    unreadCount.value = result.count || 0
  } catch (error) {
    console.error('获取未读数量失败:', error)
  }
}

// 点击通知
const handleNotificationClick = async (item) => {
  // 标记已读
  if (item.isRead === 0) {
    await markAsRead(item.id)
    item.isRead = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
  // 跳转到文章
  if (item.articleId) {
    router.push(`/article/${item.articleId}`)
  }
}

// 全部已读
const handleMarkAllRead = async () => {
  try {
    await markAllAsRead()
    notifications.value.forEach(n => n.isRead = 1)
    unreadCount.value = 0
    ElMessage.success('已全部标记为已读')
  } catch (error) {
    console.error(error)
  }
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = (now - date) / 1000
  
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  if (diff < 604800) return Math.floor(diff / 86400) + '天前'
  return date.toLocaleDateString('zh-CN')
}

// 🔔 WebSocket 实时通知处理
const setupWebSocket = () => {
  if (!userStore.isLogin) return
  
  // 连接WebSocket
  notificationWS.connect()
  
  // 监听实时通知
  notificationWS.on('LIKE', (data) => {
    showRealtimeNotification(data, 'success', '👍')
  })
  
  notificationWS.on('COMMENT', (data) => {
    showRealtimeNotification(data, 'info', '💬')
  })
  
  notificationWS.on('FOLLOW', (data) => {
    showRealtimeNotification(data, 'warning', '⭐')
  })
  
  notificationWS.on('FAVORITE', (data) => {
    showRealtimeNotification(data, 'success', '❤️')
  })
  
  notificationWS.on('SYSTEM', (data) => {
    showRealtimeNotification(data, 'info', '📢')
  })
  
  // 收到任何消息都刷新未读数量和通知列表
  notificationWS.on('message', (data) => {
    // 跳过连接成功消息
    if (data.type === 'CONNECTED') return
    fetchUnreadCount()
    fetchNotifications()  // 同时刷新通知列表
  })
}

// 显示实时通知弹窗
const showRealtimeNotification = (data, type, icon) => {
  // 增加未读数
  unreadCount.value++
  
  // 显示桌面通知弹窗
  ElNotification({
    title: `${icon} ${data.title || '新消息'}`,
    message: data.content,
    type: type,
    duration: 5000,
    position: 'top-right',
    onClick: () => {
      // 点击通知跳转
      if (data.articleId) {
        router.push(`/article/${data.articleId}`)
      } else if (data.fromUserId) {
        router.push(`/profile/${data.fromUserId}`)
      }
    }
  })
}

// 监听登录状态变化
watch(() => userStore.isLogin, (isLogin) => {
  if (isLogin) {
    setupWebSocket()
  } else {
    notificationWS.disconnect()
  }
})

// 定期刷新未读数量
onMounted(() => {
  fetchUnreadCount()
  notificationTimer = setInterval(fetchUnreadCount, 60000) // 每分钟刷新
  
  // 🔔 初始化WebSocket连接
  setupWebSocket()
})

onUnmounted(() => {
  if (notificationTimer) {
    clearInterval(notificationTimer)
  }
  // 断开WebSocket
  notificationWS.disconnect()
})
</script>

<style scoped>
/* 全局下拉菜单样式优化 */
:deep(.el-popper.is-light) {
  background: rgba(255, 255, 255, 0.85) !important;
  backdrop-filter: blur(12px) !important;
  -webkit-backdrop-filter: blur(12px) !important;
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12) !important;
  border-radius: 12px !important;
  padding: 8px !important;
}

:deep(.el-popper.is-light .el-popper__arrow::before) {
  background: rgba(255, 255, 255, 0.85) !important;
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
}

:deep(.el-menu--popup) {
  background: transparent !important;
  padding: 0 !important;
  min-width: 160px;
}

:deep(.el-menu--popup .el-menu-item) {
  height: 48px;
  line-height: 48px;
  margin: 4px 0;
  border-radius: 8px;
  color: #606266;
  background: transparent;
  transition: all 0.3s ease;
}

:deep(.el-menu--popup .el-menu-item:hover) {
  background: rgba(102, 126, 234, 0.1) !important;
  color: #667eea !important;
  transform: translateX(4px);
}

:deep(.el-menu--popup .el-menu-item .el-icon) {
  margin-right: 10px;
  font-size: 18px;
  vertical-align: middle;
  color: #909399;
  transition: color 0.3s ease;
}

:deep(.el-menu--popup .el-menu-item:hover .el-icon) {
  color: #667eea;
}

:deep(.el-dropdown-menu) {
  background: transparent !important;
  padding: 0 !important;
  border: none !important;
}

:deep(.el-dropdown-menu__item) {
  padding: 10px 20px;
  margin: 4px 0;
  border-radius: 8px;
  color: #606266;
  font-size: 14px;
  transition: all 0.3s ease;
}

:deep(.el-dropdown-menu__item:hover) {
  background: rgba(102, 126, 234, 0.1) !important;
  color: #667eea !important;
}

:deep(.el-dropdown-menu__item .el-icon) {
  margin-right: 8px;
  font-size: 16px;
}

:deep(.el-dropdown-menu__item--divided) {
  border-top-color: rgba(0, 0, 0, 0.06);
  margin-top: 8px;
}
/* 整体布局 */
.main-layout {
  min-height: 100vh;
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  position: relative;
}

.main-layout::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(245, 247, 250, 0.4); /* 降低遮罩浓度，让背景图清晰可见 */
  backdrop-filter: blur(3px); /* 降低模糊度 */
  pointer-events: none;
  z-index: 0;
}

/* 顶部导航栏 */
.header {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  padding: 0;
  height: 70px;
  position: sticky;
  top: 0;
  z-index: 1000;
  transition: all 0.3s ease;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 30px;
  gap: 30px;
}

/* Logo 样式 */
.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.logo:hover {
  transform: translateY(-2px);
}

.logo-img {
  height: 48px;
  width: auto;
}

.logo-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.logo-title {
  font-size: 20px;
  font-weight: 700;
  color: #2c3e50;
  line-height: 1;
}

.logo-subtitle {
  font-size: 11px;
  color: #909399;
  letter-spacing: 0.5px;
  line-height: 1;
}

/* 主菜单样式 */
.main-menu {
  flex: 1;
  border: none;
  background: transparent;
}

.main-menu .menu-item {
  font-weight: 500;
  font-size: 15px;
  margin: 0 5px;
  border-radius: 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.main-menu .menu-item::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  width: 0;
  height: 2px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  transition: all 0.3s ease;
  transform: translateX(-50%);
}

.main-menu .menu-item:hover {
  background: #f5f7fa;
  transform: translateY(-2px);
}

.main-menu .menu-item:hover::before {
  width: 60%;
}

.main-menu .is-active {
  color: #2196f3;
  background: linear-gradient(135deg, rgba(33, 150, 243, 0.1) 0%, rgba(25, 118, 210, 0.1) 100%);
  border-bottom: 2px solid #2196f3;
}

.main-menu .el-icon {
  margin-right: 6px;
}

/* AI 助手菜单项特殊样式 */
.main-menu .news-menu-item {
  background: linear-gradient(135deg, rgba(33, 150, 243, 0.1) 0%, rgba(3, 169, 244, 0.1) 100%);
  color: #2196f3;
}

.main-menu .news-menu-item:hover {
  background: linear-gradient(135deg, rgba(33, 150, 243, 0.2) 0%, rgba(3, 169, 244, 0.2) 100%);
}

.main-menu .news-menu-item.is-active {
  color: #0288d1;
  background: linear-gradient(135deg, rgba(33, 150, 243, 0.15) 0%, rgba(3, 169, 244, 0.15) 100%);
  border-bottom-color: #0288d1;
}

.main-menu .ai-menu-item {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  color: #667eea;
}

.main-menu .ai-menu-item:hover {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.2) 0%, rgba(118, 75, 162, 0.2) 100%);
}

.main-menu .ai-menu-item.is-active {
  color: #764ba2;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.15) 0%, rgba(118, 75, 162, 0.15) 100%);
  border-bottom-color: #764ba2;
}

/* 视频板块菜单项特殊样式 */
.main-menu .video-menu-item {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.1) 0%, rgba(220, 38, 38, 0.1) 100%);
  color: #ef4444;
}

.main-menu .video-menu-item:hover {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.2) 0%, rgba(220, 38, 38, 0.2) 100%);
}

.main-menu .video-menu-item.is-active {
  color: #dc2626;
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.15) 0%, rgba(220, 38, 38, 0.15) 100%);
  border-bottom-color: #ef4444;
}

/* 校园集市菜单项特殊样式 */
.main-menu .marketplace-menu-item {
  background: linear-gradient(135deg, rgba(168, 85, 247, 0.1) 0%, rgba(139, 92, 246, 0.1) 100%);
  color: #a855f7;
}

.main-menu .marketplace-menu-item:hover {
  background: linear-gradient(135deg, rgba(168, 85, 247, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
}

.main-menu .marketplace-menu-item.is-active {
  color: #7c3aed;
  background: linear-gradient(135deg, rgba(168, 85, 247, 0.15) 0%, rgba(139, 92, 246, 0.15) 100%);
  border-bottom-color: #a855f7;
}

/* 关注菜单项特殊样式 */
.main-menu .follow-menu-item {
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.1) 0%, rgba(255, 152, 0, 0.1) 100%);
  color: #f59e0b;
}

.main-menu .follow-menu-item:hover {
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.2) 0%, rgba(255, 152, 0, 0.2) 100%);
}

.main-menu .follow-menu-item.is-active {
  color: #d97706;
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.15) 0%, rgba(255, 152, 0, 0.15) 100%);
  border-bottom-color: #f59e0b;
}

/* 搜索菜单项特殊样式 */
.main-menu .search-menu-item {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.1) 0%, rgba(5, 150, 105, 0.1) 100%);
  color: #10b981;
}

.main-menu .search-menu-item:hover {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.2) 0%, rgba(5, 150, 105, 0.2) 100%);
}

.main-menu .search-menu-item.is-active {
  color: #059669;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.15) 0%, rgba(5, 150, 105, 0.15) 100%);
  border-bottom-color: #10b981;
}

/* 用户操作区 */
.user-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.publish-btn {
  font-weight: 600;
  padding: 12px 24px;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.publish-btn::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  transition: width 0.6s ease, height 0.6s ease;
}

.publish-btn:hover {
  transform: translateY(-3px) scale(1.02);
  box-shadow: 0 8px 20px rgba(33, 150, 243, 0.4);
}

.publish-btn:hover::after {
  width: 300px;
  height: 300px;
}

.publish-btn:active {
  transform: translateY(-1px) scale(1);
}

/* 通知组件 */
.notification-badge {
  margin-right: 8px;
}

.notification-btn {
  border: none;
  background: #f5f7fa;
  transition: all 0.3s ease;
}

.notification-btn:hover {
  background: #e4e7ed;
  transform: scale(1.05);
}

.notification-panel {
  margin: -12px;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.notification-title {
  font-weight: 600;
  font-size: 15px;
  color: #333;
}

.notification-list {
  max-height: 400px;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: background 0.2s ease;
  position: relative;
}

.notification-item:hover {
  background: #f5f7fa;
}

.notification-item.is-unread {
  background: #ecf5ff;
}

.notification-item.is-unread:hover {
  background: #d9ecff;
}

.notification-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-text {
  margin: 0 0 4px;
  font-size: 14px;
  color: #333;
  line-height: 1.5;
}

.notification-time {
  font-size: 12px;
  color: #909399;
}

.unread-dot {
  width: 8px;
  height: 8px;
  background: #f56c6c;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 6px;
}

.user-dropdown {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: 24px;
  transition: all 0.3s ease;
}

.user-info:hover {
  background: #f5f7fa;
}

.user-avatar {
  background: linear-gradient(135deg, #2196f3 0%, #1976d2 100%);
  color: white;
  font-weight: 600;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.user-info:hover .user-avatar {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.3);
}

.user-name {
  font-weight: 500;
  color: #2c3e50;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-icon {
  color: #909399;
  transition: transform 0.3s ease;
}

.user-dropdown:hover .dropdown-icon {
  transform: rotate(180deg);
}

/* 主内容区 */
.main-content {
  padding: 0;
  background: transparent;
  min-height: calc(100vh - 140px);
}

.content-wrapper {
  max-width: 1400px;
  margin: 0 auto;
  padding: 30px;
}

.content-wrapper.video-layout {
  max-width: none;
  margin: 0;
  padding: 0;
}

/* 页面过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 底部样式 */
.footer {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-top: 1px solid rgba(255, 255, 255, 0.3);
  padding: 30px 20px;
  height: auto;
  margin-top: auto;
}

.footer-content {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.footer-info {
  text-align: left;
}

.copyright {
  margin: 0 0 5px;
  color: #2c3e50;
  font-weight: 600;
  font-size: 14px;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.8);
}

.beian {
  margin: 0;
  color: #606266;
  font-size: 13px;
  font-weight: 500;
}

.footer-links {
  display: flex;
  align-items: center;
  gap: 12px;
}

.footer-links a {
  color: #4a5568;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.footer-links a:hover {
  color: #2196f3;
  text-shadow: 0 0 1px rgba(33, 150, 243, 0.3);
}

.footer-links .divider {
  color: #dcdfe6;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .header-content {
    padding: 0 20px;
    gap: 20px;
  }

  .logo-text {
    display: none;
  }

  .main-menu .menu-item span {
    display: none;
  }

  .content-wrapper {
    padding: 20px;
  }
}

@media (max-width: 768px) {
  .header {
    height: 60px;
  }

  .logo-img {
    height: 40px;
  }

  .main-menu {
    display: none;
  }

  .user-name {
    display: none;
  }

  .publish-btn span {
    display: none;
  }

  .footer-content {
    flex-direction: column;
    gap: 15px;
  }

  .footer-info {
    text-align: center;
  }
}

/* ========== 主题切换按钮样式 ========== */
.theme-toggle-btn {
  border: none;
  background: var(--bg-glass);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  color: var(--text-secondary);
}

.theme-toggle-btn:hover {
  background: var(--bg-glass-hover);
  transform: rotate(15deg) scale(1.1);
  color: var(--primary-color);
}

.theme-toggle-btn:active {
  transform: rotate(0deg) scale(0.95);
}

/* ========== 暗黑模式适配 ========== */
.main-layout.dark-mode::before {
  background: rgba(15, 23, 42, 0.7);
}

.dark-mode .header {
  background: var(--bg-glass);
  border-bottom-color: var(--border-color);
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.2);
}

.dark-mode .logo-title {
  color: var(--text-primary);
}

.dark-mode .logo-subtitle {
  color: var(--text-tertiary);
}

.dark-mode .main-menu .menu-item {
  color: var(--text-secondary);
}

.dark-mode .main-menu .menu-item:hover {
  background: var(--primary-light);
  color: var(--primary-color);
}

.dark-mode .main-menu .is-active {
  color: var(--primary-color);
  background: var(--primary-light);
  border-bottom-color: var(--primary-color);
}

.dark-mode .main-menu .news-menu-item {
  background: rgba(33, 150, 243, 0.15);
  color: #4fc3f7;
}

.dark-mode .main-menu .ai-menu-item {
  background: var(--primary-light);
  color: var(--primary-color);
}

.dark-mode .main-menu .video-menu-item {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
}

.dark-mode .main-menu .marketplace-menu-item {
  background: rgba(168, 85, 247, 0.15);
  color: #c084fc;
}

.dark-mode .main-menu .follow-menu-item {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
}

.dark-mode .main-menu .search-menu-item {
  background: rgba(16, 185, 129, 0.15);
  color: #34d399;
}

.dark-mode .notification-btn {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.dark-mode .notification-btn:hover {
  background: var(--primary-light);
  color: var(--primary-color);
}

.dark-mode .user-info:hover {
  background: var(--bg-tertiary);
}

.dark-mode .user-name {
  color: var(--text-primary);
}

.dark-mode .footer {
  background: var(--bg-glass);
  border-top-color: var(--border-color);
}

.dark-mode .copyright {
  color: var(--text-primary);
  text-shadow: none;
}

.dark-mode .beian {
  color: var(--text-secondary);
}

.dark-mode .footer-links a {
  color: var(--text-secondary);
}

.dark-mode .footer-links a:hover {
  color: var(--primary-color);
}

.dark-mode .footer-links .divider {
  color: var(--border-color);
}

/* 暗黑模式通知面板 */
.dark-mode .notification-panel {
  background: var(--bg-secondary);
}

.dark-mode .notification-header {
  background: var(--bg-tertiary);
  border-bottom-color: var(--border-color);
}

.dark-mode .notification-title {
  color: var(--text-primary);
}

.dark-mode .notification-item:hover {
  background: var(--bg-tertiary);
}

.dark-mode .notification-item.is-unread {
  background: var(--primary-light);
}

.dark-mode .notification-text {
  color: var(--text-primary);
}

.dark-mode .notification-time {
  color: var(--text-tertiary);
}

/* 暗黑模式下拉菜单 */
.dark-mode :deep(.el-popper.is-light) {
  background: var(--bg-glass) !important;
  border-color: var(--border-color) !important;
}

.dark-mode :deep(.el-popper.is-light .el-popper__arrow::before) {
  background: var(--bg-glass) !important;
  border-color: var(--border-color) !important;
}

.dark-mode :deep(.el-menu--popup .el-menu-item) {
  color: var(--text-secondary);
}

.dark-mode :deep(.el-menu--popup .el-menu-item:hover) {
  background: var(--primary-light) !important;
  color: var(--primary-color) !important;
}

.dark-mode :deep(.el-menu--popup .el-menu-item .el-icon) {
  color: var(--text-tertiary);
}

.dark-mode :deep(.el-dropdown-menu__item) {
  color: var(--text-secondary);
}

.dark-mode :deep(.el-dropdown-menu__item:hover) {
  background: var(--primary-light) !important;
  color: var(--primary-color) !important;
}
</style>
