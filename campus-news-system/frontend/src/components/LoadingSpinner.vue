<template>
  <div class="loading-spinner" :class="{ 'fullscreen': fullscreen }">
    <div class="spinner-container">
      <div v-if="type === 'circle'" class="spinner circle"></div>
      <div v-else-if="type === 'dots'" class="spinner dots">
        <div class="dot"></div>
        <div class="dot"></div>
        <div class="dot"></div>
      </div>
      <div v-else-if="type === 'pulse'" class="spinner pulse"></div>
      <div v-else-if="type === 'wave'" class="spinner wave">
        <div class="wave-bar"></div>
        <div class="wave-bar"></div>
        <div class="wave-bar"></div>
        <div class="wave-bar"></div>
        <div class="wave-bar"></div>
      </div>
      <div v-else-if="type === 'bars'" class="spinner bars">
        <div class="bar"></div>
        <div class="bar"></div>
        <div class="bar"></div>
      </div>
    </div>
    <div v-if="text" class="loading-text">{{ text }}</div>
  </div>
</template>

<script setup>
interface Props {
  type?: 'circle' | 'dots' | 'pulse' | 'wave' | 'bars'
  text?: string
  fullscreen?: boolean
}

withDefaults(defineProps<Props>(), {
  type: 'circle',
  text: '加载中',
  fullscreen: false
})
</script>

<style scoped>
.loading-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.loading-spinner.fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  justify-content: center;
  z-index: 9999;
}

.spinner-container {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Circle Spinner */
.spinner.circle {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(102, 126, 234, 0.3);
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* Dots Spinner */
.spinner.dots {
  display: flex;
  gap: 4px;
}

.dots .dot {
  width: 8px;
  height: 8px;
  background: #667eea;
  border-radius: 50%;
  animation: dots-bounce 1.4s ease-in-out infinite both;
}

.dots .dot:nth-child(1) { animation-delay: -0.32s; }
.dots .dot:nth-child(2) { animation-delay: -0.16s; }

/* Pulse Spinner */
.spinner.pulse {
  width: 40px;
  height: 40px;
  background: #667eea;
  border-radius: 50%;
  animation: pulse-scale 1s ease-in-out infinite;
}

/* Wave Spinner */
.spinner.wave {
  display: flex;
  align-items: flex-end;
  gap: 2px;
}

.wave .wave-bar {
  width: 4px;
  height: 20px;
  background: linear-gradient(45deg, #667eea, #764ba2);
  border-radius: 2px;
  animation: wave-bounce 1.2s ease-in-out infinite;
}

.wave .wave-bar:nth-child(1) { animation-delay: 0s; }
.wave .wave-bar:nth-child(2) { animation-delay: 0.1s; }
.wave .wave-bar:nth-child(3) { animation-delay: 0.2s; }
.wave .wave-bar:nth-child(4) { animation-delay: 0.3s; }
.wave .wave-bar:nth-child(5) { animation-delay: 0.4s; }

/* Bars Spinner */
.spinner.bars {
  display: flex;
  gap: 4px;
}

.bars .bar {
  width: 6px;
  height: 30px;
  background: linear-gradient(180deg, #667eea, #764ba2);
  border-radius: 3px;
  animation: bars-stretch 1s ease-in-out infinite;
}

.bars .bar:nth-child(1) { animation-delay: 0s; }
.bars .bar:nth-child(2) { animation-delay: 0.15s; }
.bars .bar:nth-child(3) { animation-delay: 0.3s; }

.loading-text {
  color: #667eea;
  font-size: 14px;
  font-weight: 500;
}

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

@keyframes bars-stretch {
  0%, 40%, 100% {
    transform: scaleY(0.4);
  }
  20% {
    transform: scaleY(1);
  }
}
</style>