import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  // 主题模式: 'light' | 'dark'
  const savedMode = localStorage.getItem('theme-mode')
  // 如果之前是 system 模式，默认改为 light
  const mode = ref(savedMode === 'system' ? 'light' : (savedMode || 'light'))
  // 实际应用的主题
  const isDark = ref(false)

  // 更新实际主题
  const updateTheme = () => {
    isDark.value = mode.value === 'dark'
    
    // 更新 DOM
    if (isDark.value) {
      document.documentElement.classList.add('dark')
      document.documentElement.setAttribute('data-theme', 'dark')
    } else {
      document.documentElement.classList.remove('dark')
      document.documentElement.setAttribute('data-theme', 'light')
    }
  }

  // 切换主题（只在 light 和 dark 之间切换）
  const toggleTheme = () => {
    mode.value = mode.value === 'light' ? 'dark' : 'light'
  }

  // 设置主题模式
  const setMode = (newMode) => {
    mode.value = newMode
  }

  // 监听模式变化
  watch(mode, (newMode) => {
    localStorage.setItem('theme-mode', newMode)
    updateTheme()
  }, { immediate: true })

  return {
    mode,
    isDark,
    toggleTheme,
    setMode,
    updateTheme
  }
})