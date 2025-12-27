<template>
  <transition name="notification" appear>
    <div 
      v-if="visible"
      :class="[
        'modern-notification',
        `notification-${type}`,
        { 'notification-closable': closable }
      ]"
      @click="handleClick"
    >
      <div class="notification-icon">
        <i :class="iconClass"></i>
      </div>
      <div class="notification-content">
        <div v-if="title" class="notification-title">{{ title }}</div>
        <div class="notification-message">{{ message }}</div>
      </div>
      <div v-if="closable" class="notification-close" @click.stop="close">
        <i class="el-icon-close"></i>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

interface Props {
  type?: 'success' | 'warning' | 'error' | 'info'
  title?: string
  message: string
  duration?: number
  closable?: boolean
  onClick?: () => void
}

const props = withDefaults(defineProps<Props>(), {
  type: 'info',
  title: '',
  duration: 4500,
  closable: true
})

const emit = defineEmits<{
  close: []
}>()

const visible = ref(true)

const iconClass = computed(() => {
  const icons = {
    success: 'el-icon-success',
    warning: 'el-icon-warning',
    error: 'el-icon-error',
    info: 'el-icon-info'
  }
  return icons[props.type]
})

const close = () => {
  visible.value = false
  emit('close')
}

const handleClick = () => {
  if (props.onClick) {
    props.onClick()
  }
}

onMounted(() => {
  if (props.duration > 0) {
    setTimeout(() => {
      close()
    }, props.duration)
  }
})
</script>

<style scoped>
.modern-notification {
  position: relative;
  display: flex;
  align-items: flex-start;
  padding: 16px 20px;
  margin-bottom: 16px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  min-width: 300px;
  max-width: 400px;
}

.modern-notification:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.notification-icon {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  margin-right: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 16px;
}

.notification-success .notification-icon {
  background: rgba(103, 194, 58, 0.1);
  color: #67c23a;
}

.notification-warning .notification-icon {
  background: rgba(230, 162, 60, 0.1);
  color: #e6a23c;
}

.notification-error .notification-icon {
  background: rgba(245, 108, 108, 0.1);
  color: #f56c6c;
}

.notification-info .notification-icon {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 4px;
  line-height: 1.4;
}

.notification-message {
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
  word-wrap: break-word;
}

.notification-close {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  margin-left: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #909399;
  cursor: pointer;
  transition: all 0.3s ease;
}

.notification-close:hover {
  background: rgba(0, 0, 0, 0.1);
  color: #606266;
}

/* Border colors for different types */
.notification-success {
  border-left: 4px solid #67c23a;
}

.notification-warning {
  border-left: 4px solid #e6a23c;
}

.notification-error {
  border-left: 4px solid #f56c6c;
}

.notification-info {
  border-left: 4px solid #667eea;
}

/* Transition animations */
.notification-enter-active,
.notification-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.notification-enter-from {
  opacity: 0;
  transform: translateX(100%) scale(0.8);
}

.notification-leave-to {
  opacity: 0;
  transform: translateX(100%) scale(0.8);
}

/* Dark mode support */
@media (prefers-color-scheme: dark) {
  .modern-notification {
    background: rgba(0, 0, 0, 0.8);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .notification-title {
    color: #e5e7eb;
  }
  
  .notification-message {
    color: #9ca3af;
  }
}
</style>