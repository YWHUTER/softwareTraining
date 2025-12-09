<template>
  <div 
    class="group relative bg-white/60 backdrop-blur-md border border-white/40 rounded-xl overflow-hidden transition-all duration-300 hover:shadow-2xl hover:-translate-y-2 cursor-pointer h-full flex flex-col"
    @mousemove="handleMouseMove"
    @mouseleave="handleMouseLeave"
    ref="cardRef"
    @click="$emit('click')"
  >
    <!-- 图片区域 -->
    <div class="relative h-48 overflow-hidden flex-shrink-0">
      <div class="absolute inset-0 bg-gray-200 animate-pulse" v-if="loading"></div>
      <img 
        v-else
        :src="image || 'https://picsum.photos/400/300'" 
        class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110"
        alt="News Cover"
        @error="handleImageError"
      />
      <!-- 渐变遮罩 -->
      <div class="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
      
      <!-- 分类标签 -->
      <div class="absolute top-4 left-4 z-10">
        <span class="px-3 py-1 text-xs font-semibold text-white bg-blue-600/90 backdrop-blur-sm rounded-full shadow-lg">
          {{ category }}
        </span>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="p-5 flex-1 flex flex-col">
      <div class="flex items-center gap-2 text-xs text-gray-500 mb-3">
        <el-icon><Calendar /></el-icon>
        <span>{{ formatDate(date) }}</span>
        <span class="w-1 h-1 bg-gray-300 rounded-full"></span>
        <el-icon><View /></el-icon>
        <span>{{ views }} 阅读</span>
      </div>

      <h3 class="text-lg font-bold text-gray-800 mb-2 line-clamp-2 group-hover:text-blue-600 transition-colors">
        {{ title }}
      </h3>

      <p class="text-sm text-gray-600 line-clamp-2 mb-4 flex-1">
        {{ summary }}
      </p>

      <div class="flex items-center justify-between mt-auto pt-4 border-t border-gray-50">
        <div class="flex items-center gap-2">
          <el-avatar :size="24" :src="authorAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
          <span class="text-xs text-gray-600 truncate max-w-[80px]">{{ author }}</span>
        </div>
        
        <div class="flex items-center gap-3 text-gray-400">
          <div class="flex items-center gap-1 hover:text-red-500 transition-colors">
            <el-icon><Star /></el-icon>
            <span class="text-xs">{{ likes }}</span>
          </div>
          <div class="flex items-center gap-1 hover:text-blue-500 transition-colors">
            <el-icon><ChatDotRound /></el-icon>
            <span class="text-xs">{{ comments }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Calendar, View, Star, ChatDotRound } from '@element-plus/icons-vue'

const props = defineProps({
  title: String,
  summary: String,
  image: String,
  category: {
    type: String,
    default: '校园新闻'
  },
  date: [String, Number, Date],
  views: {
    type: Number,
    default: 0
  },
  likes: {
    type: Number,
    default: 0
  },
  comments: {
    type: Number,
    default: 0
  },
  author: String,
  authorAvatar: String,
  loading: Boolean
})

defineEmits(['click'])

const cardRef = ref(null)

// 简单的3D视差效果
const handleMouseMove = (e) => {
  if (!cardRef.value) return
  
  const card = cardRef.value
  const rect = card.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  
  const centerX = rect.width / 2
  const centerY = rect.height / 2
  
  const rotateX = ((y - centerY) / centerY) * -2 // 减小旋转角度以免晕眩
  const rotateY = ((x - centerX) / centerX) * 2
  
  card.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateY(-4px)`
}

const handleMouseLeave = () => {
  if (!cardRef.value) return
  cardRef.value.style.transform = ''
}

const handleImageError = (e) => {
  e.target.src = 'https://picsum.photos/400/300'
}

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getMonth() + 1}-${d.getDate()}`
}
</script>