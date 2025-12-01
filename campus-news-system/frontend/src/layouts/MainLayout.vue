<template>
  <div class="main-layout" :style="{ backgroundImage: `url(${bgImage})` }">
    <el-container class="main-container">
      <!-- 顶部导航栏 -->
      <el-header class="header">
        <div class="header-content">
          <!-- Logo 区域 -->
          <div class="logo" @click="$router.push('/')">
            <img src="@/assets/whut-logo.png" alt="校徽" class="logo-img" />
            <div class="logo-text">
              <span class="logo-title">校园新闻</span>
              <span class="logo-subtitle">Campus News</span>
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
            <el-menu-item index="/board/OFFICIAL" class="menu-item">
              <el-icon><Document /></el-icon>
              <span>官方新闻</span>
            </el-menu-item>
            <el-menu-item index="/board/CAMPUS" class="menu-item">
              <el-icon><School /></el-icon>
              <span>全校新闻</span>
            </el-menu-item>
            <el-menu-item index="/board/COLLEGE" class="menu-item">
              <el-icon><OfficeBuilding /></el-icon>
              <span>学院新闻</span>
            </el-menu-item>
            <el-menu-item index="/ai-assistant" class="menu-item ai-menu-item">
              <el-icon><ChatDotRound /></el-icon>
              <span>AI 助手</span>
            </el-menu-item>
          </el-menu>

          <!-- 用户操作区 -->
          <div class="user-actions">
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

              <!-- 用户下拉菜单 -->
              <el-dropdown @command="handleUserCommand" trigger="click" class="user-dropdown">
                <div class="user-info">
                  <el-avatar :size="38" class="user-avatar">
                    {{ userStore.user?.realName?.[0] }}
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
        <div class="content-wrapper">
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
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import bgImage from '@/assets/main-bg.jpg'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

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
      router.push('/')
      break
  }
}
</script>

<style scoped>
/* 整体布局 */
.main-layout {
  min-height: 100vh;
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  background-color: #f5f7fa;
}

/* 顶部导航栏 */
.header {
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 0;
  height: 70px;
  position: sticky;
  top: 0;
  z-index: 1000;
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
  transition: all 0.3s ease;
}

.main-menu .menu-item:hover {
  background: #f5f7fa;
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
}

.publish-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(33, 150, 243, 0.4);
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
  background: rgba(245, 247, 250, 0.7);
  min-height: calc(100vh - 140px);
}

.content-wrapper {
  max-width: 1400px;
  margin: 0 auto;
  padding: 30px;
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
  background: #fff;
  border-top: 1px solid #e4e7ed;
  padding: 30px 20px;
  height: auto;
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
  font-weight: 500;
  font-size: 14px;
}

.beian {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.footer-links {
  display: flex;
  align-items: center;
  gap: 12px;
}

.footer-links a {
  color: #606266;
  font-size: 14px;
  transition: color 0.3s ease;
}

.footer-links a:hover {
  color: #2196f3;
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
</style>
