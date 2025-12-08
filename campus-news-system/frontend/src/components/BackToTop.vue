<template>
  <Transition name="back-to-top">
    <div
      v-show="visible"
      class="back-to-top"
      @click="scrollToTop"
      v-motion
      :initial="{ opacity: 0, scale: 0.8 }"
      :enter="{ opacity: 1, scale: 1 }"
      :leave="{ opacity: 0, scale: 0.8 }"
    >
      <el-tooltip content="返回顶部" placement="left" :hide-after="1000">
        <div class="back-to-top-button">
          <el-icon :size="28" class="arrow-icon">
            <ArrowUpBold />
          </el-icon>
        </div>
      </el-tooltip>
      
      <!-- 进度环 -->
      <svg class="progress-ring" width="64" height="64">
        <circle
          class="progress-ring-bg"
          stroke="rgba(255, 255, 255, 0.3)"
          stroke-width="3"
          fill="none"
          r="30"
          cx="32"
          cy="32"
        />
        <circle
          class="progress-ring-circle"
          :stroke-dasharray="`${circumference} ${circumference}`"
          :stroke-dashoffset="strokeDashoffset"
          stroke="url(#gradient)"
          stroke-width="3"
          fill="none"
          r="30"
          cx="32"
          cy="32"
        />
        <defs>
          <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" style="stop-color:#95de64;stop-opacity:1" />
            <stop offset="100%" style="stop-color:#52c41a;stop-opacity:1" />
          </linearGradient>
        </defs>
      </svg>
    </div>
  </Transition>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { ArrowUpBold } from '@element-plus/icons-vue'

const visible = ref(false)
const scrollProgress = ref(0)

// 圆环周长
const radius = 30  // 更新半径以匹配新的SVG尺寸
const circumference = radius * 2 * Math.PI

// 计算进度环的偏移量
const strokeDashoffset = computed(() => {
  return circumference - (scrollProgress.value / 100) * circumference
})

// 处理滚动事件
const handleScroll = () => {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const docHeight = document.documentElement.scrollHeight - window.innerHeight
  
  // 计算滚动进度
  scrollProgress.value = Math.min((scrollTop / docHeight) * 100, 100)
  
  // 显示/隐藏按钮（滚动超过300px显示）
  visible.value = scrollTop > 300
}

// 返回顶部
const scrollToTop = () => {
  // 平滑滚动到顶部
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
  handleScroll() // 初始化状态
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.back-to-top {
  position: fixed;
  bottom: 100px;  /* 提高位置，避开底部导航 */
  right: 60px;    /* 稍微向内移动 */
  z-index: 999;
  cursor: pointer;
}

.back-to-top-button {
  width: 56px;     /* 增大尺寸，更醒目 */
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #52c41a, #73d13d);  /* 自然绿色渐变 */
  backdrop-filter: blur(12px);
  box-shadow: 0 10px 40px rgba(82, 196, 26, 0.3),  /* 绿色阴影 */
              0 0 0 3px rgba(255, 255, 255, 0.3);   /* 白色外边框，增强对比 */
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  position: absolute;
  top: 4px;
  left: 4px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  animation: pulse 2s infinite;  /* 添加脉冲动画提醒用户 */
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 10px 40px rgba(82, 196, 26, 0.3),
                0 0 0 3px rgba(255, 255, 255, 0.3);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 15px 50px rgba(82, 196, 26, 0.4),
                0 0 0 5px rgba(255, 255, 255, 0.2);
  }
}

.back-to-top:hover .back-to-top-button {
  transform: translateY(-5px) scale(1.1);  /* 悬停时放大 */
  box-shadow: 0 15px 50px rgba(82, 196, 26, 0.5),
              0 0 0 4px rgba(255, 255, 255, 0.4);
  background: linear-gradient(135deg, #73d13d, #52c41a);  /* 反转渐变 */
  animation: none;  /* 悬停时停止脉冲 */
}

.back-to-top:active .back-to-top-button {
  transform: translateY(-1px) scale(0.95);
  animation: none;
}

/* 点击时的箭头动画 */
.back-to-top:active .arrow-icon {
  animation: arrowBounce 0.5s ease;
}

@keyframes arrowBounce {
  0% { transform: translateY(0); }
  40% { transform: translateY(-8px); }
  100% { transform: translateY(0); }
}

.progress-ring {
  position: absolute;
  top: 0;
  left: 0;
  transform: rotate(-90deg);
  pointer-events: none;
}

.progress-ring-circle {
  transition: stroke-dashoffset 0.15s ease;
}

/* 增强按钮在不同背景下的可见性 */
.back-to-top-button::before {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  z-index: -1;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.back-to-top:hover .back-to-top-button::before {
  opacity: 1;
}

/* 过渡动画 */
.back-to-top-enter-active,
.back-to-top-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.back-to-top-enter-from {
  opacity: 0;
  transform: translateY(20px) scale(0.8);
}

.back-to-top-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.8);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .back-to-top {
    bottom: 70px;  /* 移动端也保持一定高度 */
    right: 20px;
  }
  
  .back-to-top-button {
    width: 48px;
    height: 48px;
  }
  
  .back-to-top-button .el-icon {
    font-size: 20px !important;
  }
  
  .progress-ring {
    width: 56px;
    height: 56px;
  }
  
  .progress-ring circle {
    r: 26;
    cx: 28;
    cy: 28;
  }
  
  /* 移动端减弱脉冲动画，避免太过抢眼 */
  @keyframes pulse {
    0%, 100% {
      transform: scale(1);
    }
    50% {
      transform: scale(1.02);
    }
  }
}
</style>
