<template>
  <div class="image-container">
    <!-- 正常图片显示 -->
    <el-image 
      v-if="!hasError"
      :src="src" 
      :fit="fit"
      :lazy="lazy"
      @error="handleError"
      @load="handleLoad"
      class="main-image"
    >
      <!-- 加载中状态 -->
      <template #placeholder>
        <div class="image-loading">
          <el-icon class="loading-icon" :size="32">
            <Loading />
          </el-icon>
        </div>
      </template>
    </el-image>
    
    <!-- 错误状态 -->
    <div v-else class="image-error-container">
      <!-- 默认图片背景 -->
      <div class="error-background">
        <svg class="error-pattern" viewBox="0 0 400 300">
          <!-- 山脉轮廓 -->
          <path 
            d="M0 200 L100 120 L200 180 L300 100 L400 150 L400 300 L0 300 Z" 
            fill="url(#mountainGradient)" 
            opacity="0.3"
          />
          <path 
            d="M0 250 L150 170 L250 210 L400 140 L400 300 L0 300 Z" 
            fill="url(#mountainGradient)" 
            opacity="0.2"
          />
          
          <!-- 太阳/月亮 -->
          <circle cx="320" cy="60" r="25" fill="url(#sunGradient)" opacity="0.4" />
          
          <defs>
            <linearGradient id="mountainGradient" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" style="stop-color:#667eea;stop-opacity:1" />
              <stop offset="100%" style="stop-color:#764ba2;stop-opacity:1" />
            </linearGradient>
            <linearGradient id="sunGradient" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" style="stop-color:#ffd700;stop-opacity:1" />
              <stop offset="100%" style="stop-color:#ff8c00;stop-opacity:1" />
            </linearGradient>
          </defs>
        </svg>
      </div>
      
      <!-- 错误信息 -->
      <div class="error-content">
        <el-icon class="error-icon" :size="32">
          <PictureFilled />
        </el-icon>
        <p class="error-text">图片加载失败</p>
        
        <!-- 重试按钮 -->
        <el-button 
          v-if="showRetry"
          class="retry-button"
          size="small"
          round
          @click="retry"
        >
          <el-icon class="retry-icon">
            <Refresh />
          </el-icon>
          重试
        </el-button>
        
        <!-- 错误提示 -->
        <p v-if="retryCount >= maxRetries" class="max-retry-text">
          已达最大重试次数
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Loading, PictureFilled, Refresh } from '@element-plus/icons-vue'

const props = defineProps({
  src: {
    type: String,
    required: true
  },
  fit: {
    type: String,
    default: 'cover'
  },
  lazy: {
    type: Boolean,
    default: true
  },
  showRetry: {
    type: Boolean,
    default: true
  },
  maxRetries: {
    type: Number,
    default: 3
  }
})

const emit = defineEmits(['error', 'load'])

const hasError = ref(false)
const retryCount = ref(0)
const currentSrc = ref(props.src)

// 监听 src 变化，重置状态
watch(() => props.src, (newSrc) => {
  currentSrc.value = newSrc
  hasError.value = false
  retryCount.value = 0
})

// 处理图片加载错误
const handleError = () => {
  hasError.value = true
  emit('error')
}

// 处理图片加载成功
const handleLoad = () => {
  hasError.value = false
  retryCount.value = 0
  emit('load')
}

// 重试加载
const retry = () => {
  if (retryCount.value >= props.maxRetries) {
    return
  }
  
  retryCount.value++
  hasError.value = false
  
  // 添加时间戳强制重新加载
  const separator = currentSrc.value.includes('?') ? '&' : '?'
  currentSrc.value = props.src + separator + 't=' + Date.now()
}
</script>

<style scoped>
.image-container {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
  border-radius: inherit;
}

.main-image {
  width: 100%;
  height: 100%;
}

.image-loading {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(
    135deg,
    #f5f7fa 0%,
    #f0f2f5 50%,
    #f5f7fa 100%
  );
  animation: shimmer 2s ease-in-out infinite;
}

@keyframes shimmer {
  0% { background-position: -200% center; }
  100% { background-position: 200% center; }
}

.loading-icon {
  animation: spin 1.5s linear infinite;
  color: #909399;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.image-error-container {
  width: 100%;
  height: 100%;
  position: relative;
  background: linear-gradient(135deg, #f5f7fa, #e6e9ed);
  display: flex;
  align-items: center;
  justify-content: center;
}

.error-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  opacity: 0.5;
}

.error-pattern {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.error-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  text-align: center;
}

.error-icon {
  color: #c0c4cc;
  margin-bottom: 4px;
}

.error-text {
  margin: 0;
  font-size: 14px;
  color: #909399;
  font-weight: 500;
}

.retry-button {
  margin-top: 8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border: none;
  transition: all 0.3s ease;
}

.retry-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.retry-icon {
  margin-right: 4px;
  animation: rotate-on-hover 0.5s ease;
}

.retry-button:hover .retry-icon {
  animation: rotate 0.5s ease;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.max-retry-text {
  margin: 4px 0 0;
  font-size: 12px;
  color: #f56c6c;
}

/* 小尺寸优化 */
@media (max-width: 200px), (max-height: 150px) {
  .error-content {
    padding: 8px;
  }
  
  .error-icon {
    font-size: 24px !important;
  }
  
  .error-text {
    font-size: 12px;
  }
  
  .retry-button {
    padding: 4px 8px;
    font-size: 12px;
  }
}
</style>
