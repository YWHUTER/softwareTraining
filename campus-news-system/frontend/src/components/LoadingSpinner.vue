<template>
  <div v-if="fullscreen" class="loading-overlay">
    <div class="loading-content">
      <div :class="['loading-spinner', `loading-${type}`]">
        <div v-if="type === 'circle'" class="circle-spinner"></div>
        <div v-else-if="type === 'dots'" class="dots-spinner">
          <div class="dot"></div>
          <div class="dot"></div>
          <div class="dot"></div>
        </div>
        <div v-else-if="type === 'pulse'" class="pulse-spinner"></div>
        <div v-else-if="type === 'orbit'" class="orbit-spinner">
          <div class="orbit"></div>
          <div class="orbit"></div>
          <div class="orbit"></div>
        </div>
        <div v-else-if="type === 'wave'" class="wave-spinner">
          <div class="wave-bar"></div>
          <div class="wave-bar"></div>
          <div class="wave-bar"></div>
          <div class="wave-bar"></div>
          <div class="wave-bar"></div>
        </div>
      </div>
      <div v-if="text" class="loading-text">{{ text }}</div>
      <div v-if="showProgress" class="loading-progress">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progress + '%' }"></div>
        </div>
        <div class="progress-text">{{ progress }}%</div>
      </div>
    </div>
  </div>
  <div v-else :class="['loading-spinner', `loading-${type}`]">
    <div v-if="type === 'circle'" class="circle-spinner"></div>
    <div v-else-if="type === 'dots'" class="dots-spinner">
      <div class="dot"></div>
      <div class="dot"></div>
      <div class="dot"></div>
    </div>
    <div v-else-if="type === 'pulse'" class="pulse-spinner"></div>
    <div v-else-if="type === 'orbit'" class="orbit-spinner">
      <div class="orbit"></div>
      <div class="orbit"></div>
      <div class="orbit"></div>
    </div>
    <div v-else-if="type === 'wave'" class="wave-spinner">
      <div class="wave-bar"></div>
      <div class="wave-bar"></div>
      <div class="wave-bar"></div>
      <div class="wave-bar"></div>
      <div class="wave-bar"></div>
    </div>
    <div v-if="text" class="loading-text">{{ text }}</div>
    <div v-if="showProgress" class="loading-progress">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: progress + '%' }"></div>
      </div>
      <div class="progress-text">{{ progress }}%</div>
    </div>
  </div>
</template>

<script setup>
interface Props {
  type?: 'circle' | 'dots' | 'pulse' | 'orbit' | 'wave'
  text?: string
  fullscreen?: boolean
  showProgress?: boolean
  progress?: number
}

withDefaults(defineProps<Props>(), {
  type: 'circle',
  text: '加载中',
  fullscreen: false,
  showProgress: false,
  progress: 0
})
</script>

<style scoped>
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.loading-content {
  text-align: center;
  color: white;
}

.loading-spinner {
  display: inline-block;
  margin: 10px;
}

/* Circle Spinner */
.circle-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(102, 126, 234, 0.3);
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* Dots Spinner */
.dots-spinner {
  display: flex;
  gap: 4px;
}

.dots-spinner .dot {
  width: 8px;
  height: 8px;
  background: #667eea;
  border-radius: 50%;
  animation: dots-bounce 1.4s ease-in-out infinite both;
}

.dots-spinner .dot:nth-child(1) { animation-delay: -0.32s; }
.dots-spinner .dot:nth-child(2) { animation-delay: -0.16s; }

/* Pulse Spinner */
.pulse-spinner {
  width: 40px;
  height: 40px;
  background: #667eea;
  border-radius: 50%;
  animation: pulse-scale 1s ease-in-out infinite;
}

/* Orbit Spinner */
.orbit-spinner {
  position: relative;
  width: 40px;
  height: 40px;
}

.orbit-spinner .orbit {
  position: absolute;
  border: 2px solid transparent;
  border-top: 2px solid #667eea;
  border-radius: 50%;
  animation: spin 2s linear infinite;
}

.orbit-spinner .orbit:nth-child(1) {
  width: 40px;
  height: 40px;
  animation-delay: 0s;
}

.orbit-spinner .orbit:nth-child(2) {
  width: 30px;
  height: 30px;
  top: 5px;
  left: 5px;
  animation-delay: -0.5s;
}

.orbit-spinner .orbit:nth-child(3) {
  width: 20px;
  height: 20px;
  top: 10px;
  left: 10px;
  animation-delay: -1s;
}

/* Wave Spinner */
.wave-spinner {
  display: flex;
  gap: 2px;
  align-items: end;
}

.wave-spinner .wave-bar {
  width: 4px;
  height: 20px;
  background: #667eea;
  animation: wave-bounce 1.2s ease-in-out infinite;
}

.wave-spinner .wave-bar:nth-child(1) { animation-delay: -1.2s; }
.wave-spinner .wave-bar:nth-child(2) { animation-delay: -1.1s; }
.wave-spinner .wave-bar:nth-child(3) { animation-delay: -1.0s; }
.wave-spinner .wave-bar:nth-child(4) { animation-delay: -0.9s; }
.wave-spinner .wave-bar:nth-child(5) { animation-delay: -0.8s; }

/* Loading Text */
.loading-text {
  margin-top: 16px;
  font-size: 14px;
  color: #667eea;
  font-weight: 500;
}

/* Progress Bar */
.loading-progress {
  margin-top: 16px;
  width: 200px;
}

.progress-bar {
  width: 100%;
  height: 4px;
  background: rgba(102, 126, 234, 0.2);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #667eea, #764ba2);
  border-radius: 2px;
  transition: width 0.3s ease;
}

.progress-text {
  margin-top: 8px;
  font-size: 12px;
  color: #667eea;
}

/* Animations */
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@keyframes dots-bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

@keyframes pulse-scale {
  0% {
    transform: scale(0);
    opacity: 1;
  }
  100% {
    transform: scale(1);
    opacity: 0;
  }
}

@keyframes wave-bounce {
  0%, 40%, 100% {
    transform: scaleY(0.4);
  }
  20% {
    transform: scaleY(1);
  }
}
</style>