import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import App from './App.vue'
import router from './router'
import './style.css'
import './styles/glassmorphism.css'
import { MotionPlugin } from '@vueuse/motion'
import Particles from '@tsparticles/vue3'
import { loadFull } from 'tsparticles'

const app = createApp(App)
const pinia = createPinia()

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.use(MotionPlugin)
app.use(Particles, {
  init: async engine => {
    try {
      await loadFull(engine)
    } catch (error) {
      console.warn('Failed to load particles:', error)
    }
  }
})

app.mount('#app')
