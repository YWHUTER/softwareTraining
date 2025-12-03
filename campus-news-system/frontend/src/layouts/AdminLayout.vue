<template>
  <div class="admin-layout" :style="{ backgroundImage: `url(${bgImage})` }">
    <el-container>
      <el-aside width="200px" class="sidebar">
        <div class="logo">
          <el-icon :size="28"><Setting /></el-icon>
          <span>管理后台</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          @select="handleMenuSelect"
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
              <el-avatar :size="32">{{ userStore.user?.realName?.[0] }}</el-avatar>
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
import bgImage from '@/assets/main-bg.jpg'

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
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
}

.sidebar {
  background: linear-gradient(180deg, #304156 0%, #2a3a4e 100%);
  color: #fff;
  animation: slideInLeft 0.4s ease-out;
}

@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #3a4a5f;
  transition: all 0.3s ease;
}

.logo:hover {
  background: rgba(255, 255, 255, 0.05);
}

.el-menu {
  border: none;
  background: transparent;
}

:deep(.el-menu-item) {
  color: #fff;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  margin: 4px 8px;
  border-radius: 8px;
}

:deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.1);
  transform: translateX(4px);
}

:deep(.el-menu-item.is-active) {
  color: #409eff;
  background: rgba(64, 158, 255, 0.2);
}

.header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.header-content h2 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.main-content {
  background: rgba(240, 242, 245, 0.7);
  min-height: calc(100vh - 60px);
}
</style>
