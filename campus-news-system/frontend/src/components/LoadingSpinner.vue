<template>
  <div class="loading-spinner">
    <div class="spinner"></div>
    <div v-if="text" class="loading-text">{{ text }}</div>
  </div>
</template>

<script setup>
interface Props {
  text?: string
}

withDefaults(defineProps<Props>(), {
  text: '加载中'
})
</script>

<style scoped>
.loading-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(102, 126, 234, 0.3);
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.loading-text {
  color: #667eea;
  font-size: 14px;
  font-weight: 500;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>