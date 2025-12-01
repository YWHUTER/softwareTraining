<template>
  <div class="about-page">
    <div class="page-header">
      <h1>关于我们</h1>
      <p>了解校园新闻发布系统</p>
    </div>
    
    <div class="content-section">
      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <el-icon><InfoFilled /></el-icon>
            <span>系统简介</span>
          </div>
        </template>
        <p>
          校园新闻发布系统是一个基于现代化技术栈构建的校园信息发布平台，
          旨在为广大师生提供一个便捷、高效的新闻资讯获取与发布渠道。
        </p>
        <p>
          本系统采用前后端分离架构，前端使用 Vue3 + Element Plus 构建，
          后端使用 Spring Boot + MyBatis-Plus 开发，数据存储采用 MySQL，
          并集成 Redis 缓存以提升系统性能。
        </p>
      </el-card>
      
      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <el-icon><Star /></el-icon>
            <span>核心功能</span>
          </div>
        </template>
        <el-row :gutter="20">
          <el-col :span="8">
            <div class="feature-item">
              <el-icon :size="40" color="#409eff"><Document /></el-icon>
              <h3>新闻发布</h3>
              <p>支持多种类型新闻发布，包括官方新闻、全校新闻、学院新闻</p>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="feature-item">
              <el-icon :size="40" color="#67c23a"><ChatDotRound /></el-icon>
              <h3>AI 助手</h3>
              <p>集成智能 AI 助手，为用户提供便捷的问答服务</p>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="feature-item">
              <el-icon :size="40" color="#e6a23c"><User /></el-icon>
              <h3>用户管理</h3>
              <p>完善的用户权限体系，支持多角色管理</p>
            </div>
          </el-col>
        </el-row>
      </el-card>
      
      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <el-icon><Cpu /></el-icon>
            <span>技术栈</span>
          </div>
        </template>
        <div class="tech-stack">
          <el-tag type="primary" size="large">Vue 3</el-tag>
          <el-tag type="primary" size="large">Element Plus</el-tag>
          <el-tag type="primary" size="large">Vite</el-tag>
          <el-tag type="success" size="large">Spring Boot</el-tag>
          <el-tag type="success" size="large">MyBatis-Plus</el-tag>
          <el-tag type="warning" size="large">MySQL</el-tag>
          <el-tag type="danger" size="large">Redis</el-tag>
          <el-tag type="info" size="large">JWT</el-tag>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { InfoFilled, Star, Document, ChatDotRound, User, Cpu } from '@element-plus/icons-vue'
</script>

<style scoped>
.about-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
  padding: 40px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  color: white;
}

.page-header h1 {
  margin: 0 0 10px;
  font-size: 32px;
}

.page-header p {
  margin: 0;
  opacity: 0.9;
}

.info-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
}

.info-card p {
  line-height: 1.8;
  color: #606266;
  margin-bottom: 12px;
}

.feature-item {
  text-align: center;
  padding: 20px;
}

.feature-item h3 {
  margin: 16px 0 8px;
  color: #303133;
}

.feature-item p {
  font-size: 14px;
  color: #909399;
}

.tech-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.tech-stack .el-tag {
  padding: 8px 16px;
}
</style>
