<template>
  <router-view v-slot="{ Component }">
    <transition name="fade-slide" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<script setup>
import { onMounted } from 'vue'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()

onMounted(() => {
  // 初始化主题
  themeStore.updateTheme()
})
</script>

<style>
/* ========== CSS 变量定义 ========== */
:root {
  /* 亮色主题变量 */
  --bg-primary: #ffffff;
  --bg-secondary: #f5f7fa;
  --bg-tertiary: #e4e7ed;
  --bg-glass: rgba(255, 255, 255, 0.85);
  --bg-glass-hover: rgba(255, 255, 255, 0.95);
  --bg-card: rgba(255, 255, 255, 0.6);
  --bg-overlay: rgba(245, 247, 250, 0.4);
  
  --text-primary: #2c3e50;
  --text-secondary: #606266;
  --text-tertiary: #909399;
  --text-inverse: #ffffff;
  
  --border-color: rgba(255, 255, 255, 0.3);
  --border-light: rgba(0, 0, 0, 0.06);
  --shadow-color: rgba(0, 0, 0, 0.1);
  --shadow-hover: rgba(0, 0, 0, 0.15);
  
  --primary-color: #667eea;
  --primary-light: rgba(102, 126, 234, 0.1);
  --primary-gradient: linear-gradient(135deg, #667eea, #764ba2);
  
  --success-color: #10b981;
  --warning-color: #f59e0b;
  --danger-color: #f56c6c;
  --info-color: #2196f3;
  
  /* 过渡时间 */
  --transition-fast: 0.2s;
  --transition-normal: 0.3s;
  --transition-slow: 0.5s;
}

/* 暗黑主题变量 */
:root.dark,
[data-theme="dark"] {
  --bg-primary: #0f172a;
  --bg-secondary: #1e293b;
  --bg-tertiary: #334155;
  --bg-glass: rgba(30, 41, 59, 0.9);
  --bg-glass-hover: rgba(30, 41, 59, 0.95);
  --bg-card: rgba(30, 41, 59, 0.8);
  --bg-overlay: rgba(15, 23, 42, 0.6);
  
  --text-primary: #f1f5f9;
  --text-secondary: #94a3b8;
  --text-tertiary: #64748b;
  --text-inverse: #0f172a;
  
  --border-color: rgba(51, 65, 85, 0.5);
  --border-light: rgba(255, 255, 255, 0.06);
  --shadow-color: rgba(0, 0, 0, 0.3);
  --shadow-hover: rgba(0, 0, 0, 0.4);
  
  --primary-color: #818cf8;
  --primary-light: rgba(129, 140, 248, 0.15);
  --primary-gradient: linear-gradient(135deg, #818cf8, #a78bfa);
}

/* 基础重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  background: var(--bg-secondary);
  background-attachment: fixed;
  min-height: 100vh;
  color: var(--text-primary);
  transition: background-color var(--transition-normal), color var(--transition-normal);
}

#app {
  min-height: 100vh;
}

/* 页面切换动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 淡入动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 滑入动画 */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-up-enter-from {
  opacity: 0;
  transform: translateY(30px);
}

.slide-up-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

/* 缩放动画 */
.scale-enter-active,
.scale-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.scale-enter-from,
.scale-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

/* 全局按钮悬停效果 */
.el-button {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

.el-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.el-button:active:not(:disabled) {
  transform: translateY(0);
}

/* 全局卡片悬停效果 */
.el-card {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  background: var(--bg-card) !important;
  border-color: var(--border-color) !important;
}

.el-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px var(--shadow-hover) !important;
}

/* 暗黑模式 Element Plus 组件适配 */
.dark .el-card,
[data-theme="dark"] .el-card {
  background: var(--bg-card) !important;
  border-color: var(--border-color) !important;
  color: var(--text-primary);
}

.dark .el-card__header,
[data-theme="dark"] .el-card__header {
  border-bottom-color: var(--border-color) !important;
}

.dark .el-input__wrapper,
[data-theme="dark"] .el-input__wrapper {
  background-color: var(--bg-tertiary) !important;
  box-shadow: 0 0 0 1px var(--border-color) inset !important;
}

.dark .el-input__inner,
[data-theme="dark"] .el-input__inner {
  color: var(--text-primary) !important;
}

.dark .el-input__inner::placeholder,
[data-theme="dark"] .el-input__inner::placeholder {
  color: var(--text-tertiary) !important;
}

.dark .el-menu,
[data-theme="dark"] .el-menu {
  background-color: transparent !important;
}

.dark .el-menu-item,
[data-theme="dark"] .el-menu-item {
  color: var(--text-secondary) !important;
}

.dark .el-menu-item:hover,
[data-theme="dark"] .el-menu-item:hover {
  background-color: var(--primary-light) !important;
  color: var(--primary-color) !important;
}

.dark .el-menu-item.is-active,
[data-theme="dark"] .el-menu-item.is-active {
  color: var(--primary-color) !important;
  background: var(--primary-light) !important;
}

.dark .el-dropdown-menu,
[data-theme="dark"] .el-dropdown-menu {
  background-color: var(--bg-glass) !important;
  border-color: var(--border-color) !important;
}

.dark .el-dropdown-menu__item,
[data-theme="dark"] .el-dropdown-menu__item {
  color: var(--text-secondary) !important;
}

.dark .el-dropdown-menu__item:hover,
[data-theme="dark"] .el-dropdown-menu__item:hover {
  background-color: var(--primary-light) !important;
  color: var(--primary-color) !important;
}

.dark .el-pagination,
[data-theme="dark"] .el-pagination {
  --el-pagination-bg-color: var(--bg-tertiary);
  --el-pagination-text-color: var(--text-secondary);
  --el-pagination-button-bg-color: var(--bg-tertiary);
}

.dark .el-pagination button,
.dark .el-pagination .el-pager li,
[data-theme="dark"] .el-pagination button,
[data-theme="dark"] .el-pagination .el-pager li {
  background-color: var(--bg-tertiary) !important;
  color: var(--text-secondary) !important;
}

.dark .el-pagination .el-pager li.is-active,
[data-theme="dark"] .el-pagination .el-pager li.is-active {
  background-color: var(--primary-color) !important;
  color: var(--text-inverse) !important;
}

.dark .el-radio-button__inner,
[data-theme="dark"] .el-radio-button__inner {
  background-color: var(--bg-tertiary) !important;
  border-color: var(--border-color) !important;
  color: var(--text-secondary) !important;
}

.dark .el-radio-button__original-radio:checked + .el-radio-button__inner,
[data-theme="dark"] .el-radio-button__original-radio:checked + .el-radio-button__inner {
  background-color: var(--primary-color) !important;
  border-color: var(--primary-color) !important;
  color: var(--text-inverse) !important;
}

.dark .el-select .el-input__wrapper,
[data-theme="dark"] .el-select .el-input__wrapper {
  background-color: var(--bg-tertiary) !important;
}

.dark .el-popper.is-light,
[data-theme="dark"] .el-popper.is-light {
  background: var(--bg-glass) !important;
  border-color: var(--border-color) !important;
}

.dark .el-select-dropdown__item,
[data-theme="dark"] .el-select-dropdown__item {
  color: var(--text-secondary) !important;
}

.dark .el-select-dropdown__item.hover,
.dark .el-select-dropdown__item:hover,
[data-theme="dark"] .el-select-dropdown__item.hover,
[data-theme="dark"] .el-select-dropdown__item:hover {
  background-color: var(--primary-light) !important;
}

.dark .el-tag,
[data-theme="dark"] .el-tag {
  --el-tag-bg-color: var(--primary-light);
  --el-tag-border-color: var(--border-color);
  --el-tag-text-color: var(--text-secondary);
}

/* 输入框聚焦动画 */
.el-input__wrapper,
.el-textarea__inner {
  transition: all 0.3s ease !important;
}

.el-input__wrapper:focus-within,
.el-textarea__inner:focus {
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2) !important;
}

/* 平滑滚动 */
html {
  scroll-behavior: smooth;
}

/* 选中文字样式 */
::selection {
  background: var(--primary-light);
  color: var(--text-primary);
}

/* 滚动条暗黑模式 */
.dark ::-webkit-scrollbar,
[data-theme="dark"] ::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.dark ::-webkit-scrollbar-track,
[data-theme="dark"] ::-webkit-scrollbar-track {
  background: var(--bg-secondary);
}

.dark ::-webkit-scrollbar-thumb,
[data-theme="dark"] ::-webkit-scrollbar-thumb {
  background: var(--bg-tertiary);
  border-radius: 4px;
}

.dark ::-webkit-scrollbar-thumb:hover,
[data-theme="dark"] ::-webkit-scrollbar-thumb:hover {
  background: var(--text-tertiary);
}

/* 加载动画 */
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
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

/* 骨架屏加载效果 */
.skeleton {
  background: linear-gradient(90deg, var(--bg-tertiary) 25%, var(--bg-secondary) 50%, var(--bg-tertiary) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
}

/* 渐入动画类 */
.animate-fade-in {
  animation: fadeInUp 0.5s ease forwards;
}

.animate-fade-in-delay-1 { animation-delay: 0.1s; opacity: 0; }
.animate-fade-in-delay-2 { animation-delay: 0.2s; opacity: 0; }
.animate-fade-in-delay-3 { animation-delay: 0.3s; opacity: 0; }
.animate-fade-in-delay-4 { animation-delay: 0.4s; opacity: 0; }
</style>
