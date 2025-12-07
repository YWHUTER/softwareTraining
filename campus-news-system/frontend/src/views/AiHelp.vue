<template>
  <div class="ai-help">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-section">
          <el-icon :size="32" class="help-icon"><QuestionFilled /></el-icon>
          <div class="title-text">
            <h1>武理小助手使用指南</h1>
            <p>学会提问，让AI更好地为您服务</p>
          </div>
        </div>
        <el-button type="primary" @click="$router.push('/ai-assistant')" round>
          <el-icon><ChatDotRound /></el-icon>
          开始对话
        </el-button>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="help-content">
      <!-- 功能介绍 -->
      <el-card class="help-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon :size="20" color="#409eff"><InfoFilled /></el-icon>
            <span>功能介绍</span>
          </div>
        </template>
        <p class="intro-text">
          武理小助手是校园新闻发布系统的智能问答助手，它可以帮助您：
        </p>
        <div class="feature-list">
          <div class="feature-item">
            <el-icon color="#67c23a"><Search /></el-icon>
            <span><strong>搜索新闻</strong> - 根据关键词快速找到相关文章</span>
          </div>
          <div class="feature-item">
            <el-icon color="#e6a23c"><TrendCharts /></el-icon>
            <span><strong>查看热门</strong> - 了解最热门、最新的新闻资讯</span>
          </div>
          <div class="feature-item">
            <el-icon color="#409eff"><DataAnalysis /></el-icon>
            <span><strong>数据统计</strong> - 查询系统文章数量、用户数据等</span>
          </div>
          <div class="feature-item">
            <el-icon color="#f56c6c"><Service /></el-icon>
            <span><strong>使用帮助</strong> - 解答系统功能使用问题</span>
          </div>
        </div>
      </el-card>

      <!-- 搜索功能 -->
      <el-card class="help-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon :size="20" color="#67c23a"><Search /></el-icon>
            <span>🔍 如何搜索新闻</span>
          </div>
        </template>
        <p class="section-desc">当您想查找某个话题的新闻时，可以使用以下方式提问：</p>
        <div class="example-list">
          <div class="example-item" @click="copyText('搜索校园活动')">
            <code>搜索校园活动</code>
            <el-tag size="small" type="success">推荐</el-tag>
          </div>
          <div class="example-item" @click="copyText('帮我找讲座相关的新闻')">
            <code>帮我找讲座相关的新闻</code>
          </div>
          <div class="example-item" @click="copyText('有没有关于竞赛的文章')">
            <code>有没有关于竞赛的文章</code>
          </div>
          <div class="example-item" @click="copyText('想看招聘信息')">
            <code>想看招聘信息</code>
          </div>
          <div class="example-item" @click="copyText('查找学术报告')">
            <code>查找学术报告</code>
          </div>
        </div>
        <el-alert type="info" :closable="false" class="tip-alert">
          <template #title>
            💡 提示：点击示例可复制，关键词越精准，搜索结果越准确
          </template>
        </el-alert>
      </el-card>

      <!-- 热门查询 -->
      <el-card class="help-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon :size="20" color="#e6a23c"><TrendCharts /></el-icon>
            <span>🔥 如何查看热门/最新</span>
          </div>
        </template>
        <p class="section-desc">想了解热门或最新的新闻动态：</p>
        <div class="example-list">
          <div class="example-item" @click="copyText('最热门的文章是什么')">
            <code>最热门的文章是什么</code>
            <el-tag size="small" type="warning">热门</el-tag>
          </div>
          <div class="example-item" @click="copyText('最新发布的文章')">
            <code>最新发布的文章</code>
            <el-tag size="small" type="primary">最新</el-tag>
          </div>
          <div class="example-item" @click="copyText('有哪些置顶文章')">
            <code>有哪些置顶文章</code>
          </div>
          <div class="example-item" @click="copyText('最新的官方新闻')">
            <code>最新的官方新闻</code>
          </div>
        </div>
      </el-card>

      <!-- 数据统计 -->
      <el-card class="help-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon :size="20" color="#409eff"><DataAnalysis /></el-icon>
            <span>📊 如何查询数据统计</span>
          </div>
        </template>
        <p class="section-desc">想了解系统的统计数据：</p>
        <div class="example-list">
          <div class="example-item" @click="copyText('系统有多少篇文章')">
            <code>系统有多少篇文章</code>
          </div>
          <div class="example-item" @click="copyText('谁的粉丝最多')">
            <code>谁的粉丝最多</code>
          </div>
          <div class="example-item" @click="copyText('有多少注册用户')">
            <code>有多少注册用户</code>
          </div>
        </div>
      </el-card>

      <!-- 使用帮助 -->
      <el-card class="help-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon :size="20" color="#f56c6c"><Service /></el-icon>
            <span>❓ 如何询问使用帮助</span>
          </div>
        </template>
        <p class="section-desc">对系统功能有疑问时：</p>
        <div class="example-list">
          <div class="example-item" @click="copyText('如何发布文章')">
            <code>如何发布文章</code>
          </div>
          <div class="example-item" @click="copyText('怎么关注别人')">
            <code>怎么关注别人</code>
          </div>
          <div class="example-item" @click="copyText('如何收藏文章')">
            <code>如何收藏文章</code>
          </div>
          <div class="example-item" @click="copyText('系统有哪些功能')">
            <code>系统有哪些功能</code>
          </div>
        </div>
      </el-card>

      <!-- 注意事项 -->
      <el-card class="help-card notice-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon :size="20" color="#909399"><Warning /></el-icon>
            <span>📝 注意事项</span>
          </div>
        </template>
        <ul class="notice-list">
          <li>搜索关键词长度需在 <strong>2-50个字符</strong> 之间</li>
          <li>搜索结果最多显示 <strong>5条</strong> 最相关的文章</li>
          <li>点击搜索结果中的 <strong>"查看文章"</strong> 链接可直接跳转</li>
          <li>如果搜索无结果，尝试使用 <strong>更简短或不同</strong> 的关键词</li>
          <li>武理小助手也可以回答 <strong>与系统无关</strong> 的一般性问题</li>
        </ul>
      </el-card>

      <!-- 开始使用按钮 -->
      <div class="start-button">
        <el-button type="primary" size="large" @click="$router.push('/ai-assistant')" round>
          <el-icon><ChatDotRound /></el-icon>
          开始与武理小助手对话
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { 
  QuestionFilled, 
  InfoFilled, 
  Search, 
  TrendCharts, 
  DataAnalysis, 
  Service,
  Warning,
  ChatDotRound 
} from '@element-plus/icons-vue'

const copyText = (text) => {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success(`已复制: ${text}`)
  }).catch(() => {
    ElMessage.info(`示例: ${text}`)
  })
}
</script>

<style scoped>
.ai-help {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 30px;
  border-radius: 20px;
  background: rgba(102, 126, 234, 0.08);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 2px solid rgba(102, 126, 234, 0.2);
  padding: 40px;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.12),
              0 0 0 1px rgba(255, 255, 255, 0.4) inset;
  position: relative;
  overflow: hidden;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 1;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.help-icon {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  padding: 15px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.title-text h1 {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.title-text p {
  margin: 0;
  color: #606266;
  font-size: 16px;
  font-weight: 500;
}

.help-content {
  display: grid;
  gap: 24px;
}

.help-card {
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05),
              0 0 0 1px rgba(255, 255, 255, 0.4) inset;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.help-card:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.5);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08),
              0 0 0 1px rgba(255, 255, 255, 0.6) inset;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.intro-text {
  margin: 0 0 20px;
  font-size: 15px;
  color: #606266;
  line-height: 1.6;
}

.section-desc {
  margin: 0 0 16px;
  color: #606266;
  font-size: 15px;
}

.feature-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  transition: all 0.3s ease;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: translateX(5px);
}

.feature-item span {
  font-size: 14px;
  color: #606266;
}

.example-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.example-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

.example-item:hover {
  background: white;
  border-color: #667eea;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.1);
}

.example-item code {
  font-family: inherit;
  font-size: 14px;
  color: #303133;
}

.tip-alert {
  margin-top: 16px;
}

.notice-list {
  margin: 0;
  padding-left: 20px;
  color: #606266;
  line-height: 1.8;
}

.notice-list li {
  margin-bottom: 8px;
}

.start-button {
  text-align: center;
  margin-top: 20px;
}

.start-button .el-button {
  padding: 12px 30px;
  font-size: 16px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.start-button .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .page-header {
    padding: 24px;
  }
  
  .header-content {
    flex-direction: column;
    gap: 20px;
    text-align: center;
  }
  
  .title-section {
    flex-direction: column;
    gap: 15px;
  }
  
  .help-card {
    padding: 15px;
  }
  
  .feature-list {
    grid-template-columns: 1fr;
  }
}
</style>
