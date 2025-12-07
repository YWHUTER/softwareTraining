<template>
  <div class="ai-summary">
    <!-- 文章选择器 -->
    <ArticleSelector 
      v-model="showArticleSelector"
      @select="handleArticleSelect"
    />
    <!-- 页面头部 -->
    <div class="summary-header">
      <div class="header-gradient">
        <div class="header-content">
          <div class="header-left">
            <el-icon :size="40" class="header-icon"><Document /></el-icon>
            <div class="header-text">
              <h1>AI智能摘要</h1>
              <p>让AI帮您快速理解文章核心内容</p>
            </div>
          </div>
          <div class="header-stats">
            <div class="stat-item">
              <span class="stat-num">{{ summaryCount }}</span>
              <span class="stat-label">今日生成</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ savedTime }}分钟</span>
              <span class="stat-label">节省时间</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="summary-container">
      <!-- 输入区域 -->
      <el-card class="input-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="header-title">
              <el-icon><EditPen /></el-icon>
              输入文章内容
            </span>
            <el-button-group>
              <el-button size="small" @click="loadSample">加载示例</el-button>
              <el-button size="small" @click="clearInput">清空</el-button>
            </el-button-group>
          </div>
        </template>
        
        <div class="input-area">
          <el-input
            v-model="articleContent"
            type="textarea"
            :rows="12"
            placeholder="请粘贴或输入需要生成摘要的文章内容..."
            :maxlength="10000"
            show-word-limit
          />
          
          <!-- 选择文章按钮 -->
          <div class="article-select-area">
            <el-divider class="custom-divider">或</el-divider>
            <el-button 
              class="glass-button"
              @click="showArticleSelector = true"
            >
              <el-icon><Document /></el-icon>
              选择已发布文章
            </el-button>
            <span v-if="selectedArticle" class="selected-article">
              已选择：{{ selectedArticle.title }}
            </span>
          </div>
        </div>

        <!-- 摘要选项 -->
        <div class="summary-options">
          <div class="option-group">
            <span class="option-label">摘要长度：</span>
            <el-radio-group v-model="summaryLength">
              <el-radio-button :value="50">极简（50字）</el-radio-button>
              <el-radio-button :value="100">简短（100字）</el-radio-button>
              <el-radio-button :value="200">标准（200字）</el-radio-button>
              <el-radio-button :value="300">详细（300字）</el-radio-button>
            </el-radio-group>
          </div>
          
          <div class="option-group">
            <span class="option-label">生成风格：</span>
            <el-radio-group v-model="summaryStyle">
              <el-radio-button value="professional">专业</el-radio-button>
              <el-radio-button value="casual">通俗</el-radio-button>
              <el-radio-button value="academic">学术</el-radio-button>
            </el-radio-group>
          </div>

          <div class="option-group">
            <el-checkbox v-model="extractKeyPoints">提取关键要点</el-checkbox>
            <el-checkbox v-model="generateTitle">生成标题建议</el-checkbox>
            <el-checkbox v-model="extractKeywords">提取关键词</el-checkbox>
          </div>
        </div>

        <!-- 生成按钮 -->
        <div class="generate-button">
          <el-button 
            type="primary" 
            size="large" 
            @click="generateSummaryContent"
            :loading="generating"
            :disabled="!articleContent.trim()"
          >
            <el-icon><MagicStick /></el-icon>
            {{ generating ? '正在生成...' : '生成摘要' }}
          </el-button>
        </div>
      </el-card>

      <!-- 结果展示区域 -->
      <el-card v-if="summaryResult" class="result-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="header-title">
              <el-icon><Files /></el-icon>
              AI生成结果
            </span>
            <div class="result-actions">
              <el-button size="small" @click="copySummary">复制</el-button>
              <el-button size="small" @click="regenerate">重新生成</el-button>
            </div>
          </div>
        </template>

        <!-- 摘要内容 -->
        <div class="summary-content">
          <div class="content-section">
            <h3 class="section-title">📝 摘要内容</h3>
            <div class="summary-text">{{ summaryResult.summary }}</div>
            <div class="summary-meta">
              <el-tag size="small">字数：{{ summaryResult.summary.length }}</el-tag>
              <el-tag size="small" type="success">压缩率：{{ compressionRate }}%</el-tag>
            </div>
          </div>

          <!-- 关键要点 -->
          <div v-if="extractKeyPoints && summaryResult.keyPoints" class="content-section">
            <h3 class="section-title">🎯 关键要点</h3>
            <ul class="key-points">
              <li v-for="(point, index) in summaryResult.keyPoints" :key="index">
                <el-icon color="#409eff"><CircleCheck /></el-icon>
                {{ point }}
              </li>
            </ul>
          </div>

          <!-- 标题建议 -->
          <div v-if="generateTitle && summaryResult.titleSuggestions" class="content-section">
            <h3 class="section-title">💡 标题建议</h3>
            <div class="title-suggestions">
              <div 
                v-for="(title, index) in summaryResult.titleSuggestions" 
                :key="index"
                class="title-item"
                @click="selectTitle(title)"
              >
                <span class="title-num">{{ index + 1 }}</span>
                <span class="title-text">{{ title }}</span>
              </div>
            </div>
          </div>

          <!-- 关键词 -->
          <div v-if="extractKeywords && summaryResult.keywords" class="content-section">
            <h3 class="section-title">🔖 关键词</h3>
            <div class="keywords">
              <el-tag 
                v-for="keyword in summaryResult.keywords" 
                :key="keyword"
                class="keyword-tag"
                effect="plain"
              >
                {{ keyword }}
              </el-tag>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Document, EditPen, MagicStick, Files, CircleCheck
} from '@element-plus/icons-vue'
import { sendChatMessage } from '@/api/ai'
import { getArticleDetail } from '@/api/article'
import { htmlToText } from '@/utils/htmlParser'
import ArticleSelector from '@/components/ArticleSelector.vue'

// 数据状态
const articleContent = ref('')
const summaryLength = ref(200)
const summaryStyle = ref('professional')
const extractKeyPoints = ref(true)
const generateTitle = ref(true)
const extractKeywords = ref(true)
const generating = ref(false)
const summaryResult = ref(null)
const summaryCount = ref(0)
const savedTime = ref(0)
const showArticleSelector = ref(false)
const selectedArticle = ref(null)

// 计算属性
const compressionRate = computed(() => {
  if (!summaryResult.value || !articleContent.value) return 0
  return Math.round((1 - summaryResult.value.summary.length / articleContent.value.length) * 100)
})

// 方法
const generateSummaryContent = async () => {
  if (!articleContent.value.trim()) {
    ElMessage.warning('请输入文章内容')
    return
  }

  generating.value = true
  
  try {
    // 构建请求prompt
    let prompt = `请为以下文章生成一个${summaryLength.value}字左右的${getStyleText(summaryStyle.value)}摘要：\n\n${articleContent.value}`
    
    if (extractKeyPoints.value) {
      prompt += '\n\n另外，请提取3-5个关键要点，每个要点单独一行。'
    }
    
    if (generateTitle.value) {
      prompt += '\n\n请提供3个吸引人的标题建议，每个标题单独一行。'
    }
    
    if (extractKeywords.value) {
      prompt += '\n\n请提取5-8个关键词，用逗号分隔。'
    }

    // 调用通用聊天API
    const response = await sendChatMessage({
      question: prompt
    })

    // 解析AI返回的响应
    console.log('API响应:', response) // 调试输出
    
    // 响应拦截器已经返回了res.data，所以response就是{answer, sessionId, timestamp}
    let aiResponse = ''
    if (response && response.answer) {
      aiResponse = response.answer
    } else if (typeof response === 'string') {
      aiResponse = response
    } else {
      console.error('未知的响应格式:', response)
      throw new Error('响应格式错误')
    }
    
    summaryResult.value = parseAIResponse(aiResponse)
    
    summaryCount.value++
    savedTime.value += Math.round(articleContent.value.length / 200)
    
    ElMessage.success('摘要生成成功！')
  } catch (error) {
    console.error('生成摘要失败:', error)
    ElMessage.error('生成失败，请重试')
  } finally {
    generating.value = false
  }
}

// 解析AI响应的辅助函数
const parseAIResponse = (response) => {
  const result = {
    summary: '',
    keyPoints: [],
    titleSuggestions: [],
    keywords: [],
    qualityScore: 4
  }

  // 将响应分段
  const sections = response.split(/\n\n+/)
  
  // 第一段通常是摘要
  if (sections.length > 0) {
    result.summary = sections[0].replace(/^(摘要[:：]\s*)/i, '').trim()
  }

  // 查找关键要点部分
  const keyPointsSection = sections.find(s => s.includes('关键要点') || s.includes('要点'))
  if (keyPointsSection) {
    const lines = keyPointsSection.split('\n').slice(1) // 跳过标题行
    result.keyPoints = lines
      .filter(line => line.trim())
      .map(line => line.replace(/^[\d、\.\-\*]+\s*/, '').trim())
      .filter(point => point.length > 0)
  }

  // 查找标题建议部分
  const titlesSection = sections.find(s => s.includes('标题建议') || s.includes('标题'))
  if (titlesSection) {
    const lines = titlesSection.split('\n').slice(1)
    result.titleSuggestions = lines
      .filter(line => line.trim())
      .map(line => line.replace(/^[\d、\.\-\*]+\s*/, '').trim())
      .filter(title => title.length > 0)
      .slice(0, 3)
  }

  // 查找关键词部分
  const keywordsSection = sections.find(s => s.includes('关键词') || s.includes('关键字'))
  if (keywordsSection) {
    const keywordLine = keywordsSection.split('\n').find(line => !line.includes('关键'))
    if (keywordLine) {
      result.keywords = keywordLine
        .split(/[，,、\s]+/)
        .filter(k => k.trim() && k.length > 1)
        .slice(0, 8)
    }
  }

  // 计算质量评分
  if (result.summary.length >= summaryLength.value * 0.8) {
    result.qualityScore = 5
  } else if (result.summary.length >= summaryLength.value * 0.6) {
    result.qualityScore = 4
  } else {
    result.qualityScore = 3
  }

  return result
}

const getStyleText = (style) => {
  const styleMap = {
    professional: '专业风格的',
    casual: '通俗易懂的',
    academic: '学术性的'
  }
  return styleMap[style] || ''
}

const loadSample = () => {
  articleContent.value = `武汉理工大学近日举办了第十届"互联网+"大学生创新创业大赛校内选拔赛，吸引了来自全校各学院的300余支队伍参赛。本次大赛以"我敢闯，我会创"为主题，旨在激发大学生的创新精神、创业意识和创新创业能力。

比赛分为"青年红色筑梦之旅"、"高教主赛道"、"职教赛道"三个赛道，涵盖了现代农业、制造业、信息技术服务、文化创意服务、社会服务等多个领域。参赛项目展现了武理工学子的创新思维和实践能力，其中不乏具有市场前景和社会价值的优秀项目。

经过激烈角逐，最终有50个项目脱颖而出，将代表学校参加省级比赛。学校创新创业学院负责人表示，近年来学校高度重视创新创业教育，通过完善课程体系、搭建实践平台、提供孵化服务等举措，为学生创新创业提供全方位支持。`
  
  ElMessage.success('已加载示例文章')
}

const clearInput = () => {
  articleContent.value = ''
  summaryResult.value = null
  selectedArticle.value = null
}

// 处理文章选择
const handleArticleSelect = async (article) => {
  try {
    // 获取文章详情
    const detail = await getArticleDetail(article.id)
    
    // 处理HTML内容，转换为纯文本（保留换行以便更好地理解文章结构）
    const content = htmlToText(detail.content || detail.summary || '', true)
    
    // 填充文章内容
    articleContent.value = content
    selectedArticle.value = article
    
    ElMessage.success(`已选择文章：${article.title}`)
  } catch (error) {
    console.error('获取文章详情失败:', error)
    ElMessage.error('获取文章内容失败，请重试')
  }
}

const copySummary = () => {
  if (summaryResult.value) {
    navigator.clipboard.writeText(summaryResult.value.summary)
    ElMessage.success('摘要已复制到剪贴板')
  }
}

const regenerate = () => {
  generateSummaryContent()
}

const selectTitle = (title) => {
  navigator.clipboard.writeText(title)
  ElMessage.success('标题已复制')
}

onMounted(() => {
  // 检查是否从文章详情页跳转过来
  const analysisContent = sessionStorage.getItem('aiAnalysisContent')
  if (analysisContent) {
    try {
      const data = JSON.parse(analysisContent)
      articleContent.value = data.content
      selectedArticle.value = {
        title: data.title,
        id: data.articleId
      }
      // 清除临时存储
      sessionStorage.removeItem('aiAnalysisContent')
      // 自动开始生成摘要
      ElMessage.success(`已加载文章：${data.title}`)
      setTimeout(() => {
        generateSummaryContent()
      }, 500)
    } catch (error) {
      console.error('解析文章内容失败:', error)
    }
  }
  
  // 加载统计数据
  const stats = localStorage.getItem('summaryStats')
  if (stats) {
    const { count, time } = JSON.parse(stats)
    summaryCount.value = count || 0
    savedTime.value = time || 0
  }
})
</script>

<style scoped>
.ai-summary {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.summary-header {
  margin-bottom: 30px;
  border-radius: 20px;
  overflow: hidden;
  background: rgba(102, 126, 234, 0.08);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 2px solid rgba(102, 126, 234, 0.2);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.12),
              0 0 0 1px rgba(255, 255, 255, 0.4) inset;
}

.header-gradient {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.15), rgba(118, 75, 162, 0.15));
  padding: 40px;
  position: relative;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #2c3e50;
  position: relative;
  z-index: 1;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-icon {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  padding: 15px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.header-text h1 {
  margin: 0 0 8px;
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.header-text p {
  margin: 0;
  color: #606266;
  font-weight: 500;
}

.header-stats {
  display: flex;
  gap: 40px;
}

.article-select-area {
  margin-top: 20px;
  text-align: center;
  padding: 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border-radius: 12px;
  border: 2px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05),
              0 0 0 1px rgba(255, 255, 255, 0.8) inset;
}

/* 自定义分隔符样式 */
.custom-divider {
  margin: 15px 0;
}

.custom-divider :deep(.el-divider__text) {
  background: rgba(255, 255, 255, 0.95) !important;
  color: #606266 !important;
  font-weight: 600 !important;
  font-size: 14px !important;
  padding: 0 20px !important;
}

/* 玻璃按钮样式 */
.glass-button {
  background: rgba(102, 126, 234, 0.1) !important;
  backdrop-filter: blur(10px) saturate(150%) !important;
  -webkit-backdrop-filter: blur(10px) saturate(150%) !important;
  border: 2px solid rgba(102, 126, 234, 0.2) !important;
  color: #667eea !important;
  font-weight: 600 !important;
  padding: 10px 20px !important;
  height: auto !important;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1),
              0 0 0 1px rgba(255, 255, 255, 0.5) inset !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

.glass-button:hover {
  background: rgba(102, 126, 234, 0.15) !important;
  border-color: rgba(102, 126, 234, 0.3) !important;
  color: #667eea !important;
  transform: translateY(-2px) !important;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2),
              0 0 0 1px rgba(255, 255, 255, 0.6) inset !important;
}

.glass-button .el-icon {
  font-size: 16px !important;
  margin-right: 5px !important;
}

.selected-article {
  margin-left: 15px;
  color: #67c23a;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.stat-item {
  text-align: center;
}

.stat-num {
  display: block;
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 5px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.stat-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.summary-container {
  display: grid;
  gap: 20px;
}

.input-card, .result-card {
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05),
              0 0 0 1px rgba(255, 255, 255, 0.4) inset;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.input-card:hover, .result-card:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.5);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08),
              0 0 0 1px rgba(255, 255, 255, 0.6) inset;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.summary-options {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  background: rgba(245, 247, 250, 0.6);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  margin: 20px 0;
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.option-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.option-label {
  font-weight: 500;
  color: #606266;
  min-width: 80px;
}

.generate-button {
  text-align: center;
  padding: 20px 0;
}

.generate-button .el-button {
  min-width: 200px;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  color: white;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.generate-button .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.summary-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.content-section {
  padding: 20px;
  background: rgba(245, 247, 250, 0.5);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  transition: all 0.3s ease;
}

.content-section:hover {
  background: rgba(245, 247, 250, 0.7);
  transform: translateY(-1px);
}

.section-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.summary-text {
  line-height: 1.8;
  color: #606266;
  font-size: 15px;
}

.summary-meta {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.key-points {
  list-style: none;
  padding: 0;
  margin: 0;
}

.key-points li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  line-height: 1.6;
  color: #606266;
}

.title-suggestions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.title-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(5px);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
}

.title-item:hover {
  border-color: rgba(102, 126, 234, 0.5);
  background: rgba(255, 255, 255, 0.95);
  transform: translateX(5px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.title-num {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: #667eea;
  color: white;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
}

.title-text {
  flex: 1;
  color: #303133;
}

.keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.keyword-tag {
  padding: 6px 12px;
  font-size: 14px;
  background: rgba(102, 126, 234, 0.1) !important;
  border: 1px solid rgba(102, 126, 234, 0.2) !important;
  color: #667eea !important;
  font-weight: 500;
}

/* 输入区域优化 */
.input-area {
  margin-bottom: 20px;
}

.input-area :deep(.el-textarea__inner) {
  background: rgba(255, 255, 255, 0.7);
  border-color: rgba(220, 223, 230, 0.8);
  color: #2c3e50;
  font-size: 15px;
  font-weight: 500;
  line-height: 1.6;
}

.input-area :deep(.el-textarea__inner:focus) {
  background: rgba(255, 255, 255, 0.9);
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}
</style>
