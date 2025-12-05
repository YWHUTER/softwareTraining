<template>
  <div class="admin-layout">
    <!-- 背景图 -->
    <div class="bg-image"></div>
    <!-- 遮罩层 -->
    <div class="bg-overlay"></div>
    
    <el-container class="layout-container">
      <el-aside width="220px" class="sidebar">
        <div class="logo">
          <img src="@/assets/whut-logo.png" alt="logo" class="logo-img" />
          <span>管理后台</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          @select="handleMenuSelect"
          class="glass-menu"
        >
          <el-menu-item index="/admin">
            <el-icon><Odometer /></el-icon>
            <span>数据概览</span>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/articles">
            <el-icon><Document /></el-icon>
            <span>文章管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/colleges">
            <el-icon><School /></el-icon>
            <span>学院管理</span>
          </el-menu-item>
          <el-menu-item index="/" @click="$router.push('/')">
            <el-icon><HomeFilled /></el-icon>
            <span>返回首页</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      
      <el-container>
        <el-header class="header">
          <div class="header-content">
            <h2>{{ pageTitle }}</h2>
            <div class="user-info">
              <el-avatar :size="32" class="user-avatar">{{ userStore.user?.realName?.[0] }}</el-avatar>
              <span>{{ userStore.user?.realName }}</span>
            </div>
          </div>
        </el-header>
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const pageTitle = computed(() => {
  const titles = {
    '/admin': '数据概览',
    '/admin/users': '用户管理',
    '/admin/articles': '文章管理',
    '/admin/colleges': '学院管理'
  }
  return titles[route.path] || '管理后台'
})

const handleMenuSelect = (index) => {
  if (index !== '/') {
    router.push(index)
  }
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

.bg-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: url('@/assets/login-bg.jpg') center center / cover no-repeat;
  z-index: 0;
}

.bg-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(10, 60, 120, 0.2);
  z-index: 1;
}

.layout-container {
  position: relative;
  z-index: 2;
  height: 100vh;
}

/* 侧边栏玻璃拟态 */
.sidebar {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border-right: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 20px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
  color: #fff;
  text-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.logo-img {
  height: 32px;
  width: auto;
}

/* 菜单样式 */
.glass-menu {
  background: transparent !important;
  border-right: none;
  padding: 10px;
}

:deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.9);
  margin: 8px 0;
  border-radius: 10px;
  height: 50px;
  line-height: 50px;
  border: 1px solid transparent;
}

:deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}

:deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  font-weight: bold;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

:deep(.el-icon) {
  font-size: 18px;
  margin-right: 8px;
}

/* 顶部导航栏玻璃拟态 */
.header {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  height: 64px;
  padding: 0 24px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.header-content h2 {
  margin: 0;
  font-size: 20px;
  color: #fff;
  text-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #fff;
  background: rgba(0, 0, 0, 0.2);
  padding: 6px 16px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
}

.user-info:hover {
  background: rgba(0, 0, 0, 0.3);
}

.user-avatar {
  border: 2px solid rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

/* 主内容区 */
.main-content {
  background: transparent;
  padding: 24px;
  height: calc(100vh - 64px);
  overflow-y: auto;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
}

::-webkit-scrollbar-track {
  background: transparent;
}
</style>
