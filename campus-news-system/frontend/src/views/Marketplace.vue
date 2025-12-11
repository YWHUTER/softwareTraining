<template>
  <div class="marketplace-page">
    <div class="page-header">
      <div class="header-icon">
        <el-icon :size="48"><Shop /></el-icon>
      </div>
      <h1 class="page-title">校园集市</h1>
      <p class="page-subtitle">买卖互助，便捷校园生活</p>
    </div>
    
    <div class="coming-soon">
      <div class="coming-soon-content">
        <el-icon :size="120" class="coming-icon"><ShoppingCart /></el-icon>
        <h2>功能开发中</h2>
        <p>校园集市板块正在紧张开发中，敬请期待...</p>
        <div class="features-preview">
          <div class="feature-item">
            <el-icon><Goods /></el-icon>
            <span>二手物品交易</span>
          </div>
          <div class="feature-item">
            <el-icon><Service /></el-icon>
            <span>技能服务互助</span>
          </div>
          <div class="feature-item">
            <el-icon><Ticket /></el-icon>
            <span>闲置资源共享</span>
          </div>
        </div>
        <el-button type="primary" round size="large" @click="$router.push('/')">
          <el-icon><Back /></el-icon>
          返回首页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Shop, ShoppingCart, Goods, Service, Ticket, Back } from '@element-plus/icons-vue'
</script>

<style scoped>
.marketplace-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 60px;
}

.header-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, #a855f7 0%, #7c3aed 100%);
  border-radius: 24px;
  color: white;
  margin-bottom: 24px;
  box-shadow: 0 12px 40px rgba(168, 85, 247, 0.3);
}

.page-title {
  font-size: 36px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 12px;
}

.page-subtitle {
  font-size: 18px;
  color: #6b7280;
  margin: 0;
}

.coming-soon {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border-radius: 24px;
  padding: 80px 40px;
  text-align: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.coming-icon {
  color: #a855f7;
  margin-bottom: 24px;
  opacity: 0.8;
}

.coming-soon h2 {
  font-size: 28px;
  color: #1f2937;
  margin: 0 0 16px;
}

.coming-soon p {
  font-size: 16px;
  color: #6b7280;
  margin: 0 0 40px;
}

.features-preview {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-bottom: 40px;
  flex-wrap: wrap;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: rgba(168, 85, 247, 0.1);
  border-radius: 12px;
  color: #a855f7;
  font-weight: 500;
}

.feature-item .el-icon {
  font-size: 20px;
}

/* 暗黑模式适配 */
:deep(.dark-mode) .page-title {
  color: var(--text-primary);
}

:deep(.dark-mode) .page-subtitle,
:deep(.dark-mode) .coming-soon p {
  color: var(--text-secondary);
}

:deep(.dark-mode) .coming-soon {
  background: var(--bg-glass);
  border-color: var(--border-color);
}

:deep(.dark-mode) .coming-soon h2 {
  color: var(--text-primary);
}
</style>
