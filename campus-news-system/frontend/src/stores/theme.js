import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  // 主题模式: 'light' | 'dark' | 'system'
  const mode = ref(localStorage.getItem('theme-mode') || 'system')
  // 实际应用的主题
  const isDark = ref(false)

  // 获取系统主题偏好
  const getSystemTheme = () => {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }

  // 更新实际主题
  const updateTheme = () => {
    const actualTheme = mode.value === 'system' ? getSystemTheme() : mode.value
    isDark.value = actualTheme === 'dark'
    
    // 更新 DOM
    if (isDark.value) {
      document.documentElement.classList.add('dark')
      document.documentElement.setAttribute('data-theme', 'dark')
    } else {
      document.documentElement.classList.remove('dark')
      document.documentElement.setAttribute('data-theme', 'light')
    }
  }

  // 切换主题
  const toggleTheme = () => {
    if (mode.value === 'light') {
      mode.value = 'dark'
    } else if (mode.value === 'dark') {
      mode.value = 'system'
    } else {
      mode.value = 'light'
    }
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

  // 监听系统主题变化
  if (typeof window !== 'undefined') {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
      if (mode.value === 'system') {
        updateTheme()
      }
    })
  }

  return {
    mode,
    isDark,
    toggleTheme,
    setMode,
    updateTheme
  }
})
