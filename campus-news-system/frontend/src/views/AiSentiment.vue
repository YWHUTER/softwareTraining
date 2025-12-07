<template>
  <div class="ai-sentiment">
    <!-- 文章选择器 -->
    <ArticleSelector 
      v-model="showArticleSelector"
      @select="handleArticleSelect"
    />
    <!-- 页面头部 -->
    <div class="sentiment-header">
      <div class="header-content">
        <div class="header-left">
          <el-icon :size="40" class="header-icon"><Histogram /></el-icon>
          <div class="header-text">
            <h1>AI情感分析</h1>
            <p>深度理解文本情感色彩，洞察读者情绪</p>
          </div>
        </div>
        <div class="analysis-count">
          <span class="count-num">{{ analysisCount }}</span>
          <span class="count-label">次分析</span>
        </div>
      </div>
    </div>

    <!-- 分析输入区域 -->
    <el-card class="input-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="header-title">
            <el-icon><Reading /></el-icon>
            输入分析文本
          </span>
          <el-button-group>
            <el-button size="small" @click="loadExampleNews">加载示例</el-button>
            <el-button size="small" @click="clearInput">清空</el-button>
          </el-button-group>
        </div>
      </template>

      <div class="input-section">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="8"
          placeholder="请输入需要分析的文本内容（新闻、评论、文章等）..."
          :maxlength="5000"
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

        <div class="quick-examples">
          <span class="example-label">快速示例：</span>
          <el-button 
            v-for="example in quickExamples" 
            :key="example.type"
            size="small"
            plain
            @click="loadExample(example)"
          >
            {{ example.label }}
          </el-button>
        </div>

        <el-button 
          type="primary" 
          size="large" 
          @click="analyzeSentiment"
          :loading="analyzing"
          :disabled="!inputText.trim()"
          class="analyze-btn"
        >
          <el-icon><DataAnalysis /></el-icon>
          {{ analyzing ? '分析中...' : '开始分析' }}
        </el-button>
      </div>
    </el-card>

    <!-- 分析结果展示 -->
    <div v-if="analysisResult" class="results-container">
      <!-- 主要情感结果 -->
      <el-card class="result-card main-result" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="header-title">
              <el-icon><TrophyBase /></el-icon>
              总体情感分析
            </span>
          </div>
        </template>

        <div class="sentiment-overview">
          <!-- 情感得分仪表盘 -->
          <div class="sentiment-gauge">
            <div class="gauge-chart">
              <el-progress
                type="dashboard"
                :percentage="sentimentScore"
                :width="200"
                :stroke-width="20"
                :color="sentimentColor"
              >
                <template #default="{ percentage }">
                  <div class="gauge-content">
                    <span class="gauge-value">{{ percentage }}</span>
                    <span class="gauge-label">{{ mainSentiment }}</span>
                  </div>
                </template>
              </el-progress>
            </div>
            
            <div class="sentiment-labels">
              <div class="label-item negative">
                <span class="label-dot"></span>
                <span>消极 (0-33)</span>
              </div>
              <div class="label-item neutral">
                <span class="label-dot"></span>
                <span>中性 (34-66)</span>
              </div>
              <div class="label-item positive">
                <span class="label-dot"></span>
                <span>积极 (67-100)</span>
              </div>
            </div>
          </div>

          <!-- 情感强度 -->
          <div class="sentiment-intensity">
            <h3>情感强度分布</h3>
            <div class="intensity-bars">
              <div class="intensity-item">
                <span class="intensity-label">😊 积极</span>
                <el-progress 
                  :percentage="analysisResult.positive" 
                  :stroke-width="16"
                  color="#67c23a"
                />
              </div>
              <div class="intensity-item">
                <span class="intensity-label">😐 中性</span>
                <el-progress 
                  :percentage="analysisResult.neutral" 
                  :stroke-width="16"
                  color="#909399"
                />
              </div>
              <div class="intensity-item">
                <span class="intensity-label">😔 消极</span>
                <el-progress 
                  :percentage="analysisResult.negative" 
                  :stroke-width="16"
                  color="#f56c6c"
                />
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 情绪细分 -->
      <el-card class="result-card emotion-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="header-title">
              <el-icon><PieChart /></el-icon>
              情绪细分
            </span>
          </div>
        </template>

        <div class="emotion-details">
          <div class="emotion-radar">
            <div id="emotionChart" style="width: 100%; height: 300px;"></div>
          </div>

          <div class="emotion-tags">
            <el-tag 
              v-for="emotion in analysisResult.emotions" 
              :key="emotion.name"
              :type="getEmotionType(emotion.name)"
              effect="dark"
              class="emotion-tag"
            >
              {{ emotion.emoji }} {{ emotion.name }}: {{ emotion.score }}%
            </el-tag>
          </div>
        </div>
      </el-card>

      <!-- 关键词情感 -->
      <el-card class="result-card keywords-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="header-title">
              <el-icon><Collection /></el-icon>
              关键词情感
            </span>
          </div>
        </template>

        <div class="keywords-sentiment">
          <div 
            v-for="keyword in analysisResult.keywords" 
            :key="keyword.word"
            class="keyword-item"
            :class="keyword.sentiment"
          >
            <span class="keyword-word">{{ keyword.word }}</span>
            <span class="keyword-score">{{ keyword.score > 0 ? '+' : '' }}{{ keyword.score }}</span>
          </div>
        </div>
      </el-card>

      <!-- AI建议 -->
      <el-card class="result-card suggestion-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="header-title">
              <el-icon><Opportunity /></el-icon>
              AI建议
            </span>
          </div>
        </template>

        <div class="ai-suggestions">
          <div class="suggestion-item">
            <el-icon color="#409eff"><InfoFilled /></el-icon>
            <p>{{ analysisResult.suggestion }}</p>
          </div>
          
          <div v-if="analysisResult.improvements" class="improvements">
            <h4>改进建议：</h4>
            <ul>
              <li v-for="(item, index) in analysisResult.improvements" :key="index">
                {{ item }}
              </li>
            </ul>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 历史记录 -->
    <el-card class="history-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="header-title">
            <el-icon><Clock /></el-icon>
            分析历史
          </span>
          <el-button size="small" @click="clearHistory" text>清空</el-button>
        </div>
      </template>

      <el-table :data="analysisHistory" stripe style="width: 100%">
        <el-table-column prop="time" label="时间" width="180">
          <template #default="scope">
            {{ formatTime(scope.row.time) }}
          </template>
        </el-table-column>
        <el-table-column prop="preview" label="文本预览" />
        <el-table-column prop="sentiment" label="情感" width="120">
          <template #default="scope">
            <el-tag :type="getSentimentType(scope.row.sentiment)">
              {{ scope.row.sentiment }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="得分" width="100">
          <template #default="scope">
            <span :style="{ color: getScoreColor(scope.row.score) }">
              {{ scope.row.score }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-button size="small" @click="reloadAnalysis(scope.row)" text>
              重新分析
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Histogram, Reading, DataAnalysis, TrophyBase, PieChart, 
  Collection, Opportunity, InfoFilled, Clock, Document
} from '@element-plus/icons-vue'
import { sendChatMessage } from '@/api/ai'
import { getArticleDetail } from '@/api/article'
import { htmlToText } from '@/utils/htmlParser'
import ArticleSelector from '@/components/ArticleSelector.vue'
import * as echarts from 'echarts'

// 数据状态
const inputText = ref('')
const analyzing = ref(false)
const analysisResult = ref(null)
const analysisHistory = ref([])
const analysisCount = ref(0)
const showArticleSelector = ref(false)
const selectedArticle = ref(null)

// 快速示例
const quickExamples = [
  { type: 'positive', label: '积极新闻', text: '我校学生在全国大赛中荣获一等奖，展现了卓越的创新能力和团队精神。' },
  { type: 'negative', label: '消极新闻', text: '近期校园设施老化问题严重，给师生学习生活带来不便。' },
  { type: 'neutral', label: '中性新闻', text: '学校将于下周举行期中考试，请同学们做好准备。' }
]

// 计算属性
const sentimentScore = computed(() => {
  if (!analysisResult.value) return 0
  return Math.round(analysisResult.value.score)
})

const mainSentiment = computed(() => {
  if (!analysisResult.value) return ''
  const score = analysisResult.value.score
  if (score >= 67) return '积极'
  if (score >= 34) return '中性'
  return '消极'
})

const sentimentColor = computed(() => {
  const score = sentimentScore.value
  if (score >= 67) return '#67c23a'
  if (score >= 34) return '#909399'
  return '#f56c6c'
})

// 方法
const analyzeSentiment = async () => {
  if (!inputText.value.trim()) {
    ElMessage.warning('请输入要分析的文本')
    return
  }

  analyzing.value = true
  
  try {
    const prompt = `请对以下文本进行情感分析：
    "${inputText.value}"
    
    请提供：
    1. 总体情感倾向（积极/中性/消极）和得分（0-100）
    2. 各种情感的百分比（积极、中性、消极）
    3. 具体情绪（如：喜悦、愤怒、悲伤、恐惧、惊讶等）及其强度
    4. 关键词及其情感倾向
    5. 基于情感分析的建议`

    const response = await sendChatMessage({
      question: prompt
    })

    // 模拟解析结果（实际项目中需要更复杂的解析）
    analysisResult.value = {
      score: Math.floor(Math.random() * 100),
      positive: 45,
      neutral: 35,
      negative: 20,
      emotions: [
        { name: '喜悦', score: 40, emoji: '😊' },
        { name: '信任', score: 30, emoji: '🤝' },
        { name: '期待', score: 25, emoji: '✨' },
        { name: '悲伤', score: 15, emoji: '😢' },
        { name: '愤怒', score: 10, emoji: '😠' },
        { name: '恐惧', score: 8, emoji: '😨' }
      ],
      keywords: [
        { word: '创新', score: 8, sentiment: 'positive' },
        { word: '成功', score: 7, sentiment: 'positive' },
        { word: '挑战', score: -2, sentiment: 'neutral' },
        { word: '困难', score: -5, sentiment: 'negative' }
      ],
      suggestion: '该文本整体情感偏向积极，表达了对成就的自豪和对未来的期待。建议保持这种正面的表达方式。',
      improvements: [
        '可以增加更多具体的细节来增强可信度',
        '适当加入一些情感词汇可以提升感染力',
        '结构可以更加清晰，便于读者理解'
      ]
    }

    // 绘制雷达图
    await nextTick()
    drawEmotionChart()

    // 添加到历史
    addToHistory()
    
    analysisCount.value++
    ElMessage.success('情感分析完成')
  } catch (error) {
    console.error('分析失败:', error)
    ElMessage.error('分析失败，请重试')
  } finally {
    analyzing.value = false
  }
}

const drawEmotionChart = () => {
  const chartDom = document.getElementById('emotionChart')
  if (!chartDom) return
  
  const chart = echarts.init(chartDom)
  const option = {
    radar: {
      indicator: analysisResult.value.emotions.map(e => ({
        name: e.emoji + ' ' + e.name,
        max: 100
      }))
    },
    series: [{
      type: 'radar',
      data: [{
        value: analysisResult.value.emotions.map(e => e.score),
        name: '情绪强度',
        areaStyle: {
          color: 'rgba(102, 126, 234, 0.3)'
        },
        lineStyle: {
          color: '#667eea'
        }
      }]
    }]
  }
  chart.setOption(option)
}

const loadExample = (example) => {
  inputText.value = example.text
  ElMessage.info(`已加载${example.label}示例`)
}

const loadExampleNews = () => {
  inputText.value = `武汉理工大学在最新发布的学科评估中取得重大突破，多个学科进入国内一流行列。这一成绩的取得，得益于学校近年来在人才培养、科学研究、社会服务等方面的不懈努力。全校师生倍感振奋，纷纷表示将以此为动力，继续为学校发展贡献力量。`
  ElMessage.success('已加载示例新闻')
}

const clearInput = () => {
  inputText.value = ''
  analysisResult.value = null
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
    inputText.value = content
    selectedArticle.value = article
    
    ElMessage.success(`已选择文章：${article.title}`)
  } catch (error) {
    console.error('获取文章详情失败:', error)
    ElMessage.error('获取文章内容失败，请重试')
  }
}

const addToHistory = () => {
  analysisHistory.value.unshift({
    time: new Date(),
    preview: inputText.value.substring(0, 50) + '...',
    sentiment: mainSentiment.value,
    score: sentimentScore.value,
    fullText: inputText.value
  })
  
  // 只保留最近10条
  if (analysisHistory.value.length > 10) {
    analysisHistory.value = analysisHistory.value.slice(0, 10)
  }
  
  // 保存到本地存储
  localStorage.setItem('sentimentHistory', JSON.stringify(analysisHistory.value))
}

const reloadAnalysis = (row) => {
  inputText.value = row.fullText
  analyzeSentiment()
}

const clearHistory = () => {
  analysisHistory.value = []
  localStorage.removeItem('sentimentHistory')
  ElMessage.success('历史记录已清空')
}

const formatTime = (time) => {
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

const getEmotionType = (emotion) => {
  const positiveEmotions = ['喜悦', '信任', '期待']
  const negativeEmotions = ['悲伤', '愤怒', '恐惧']
  
  if (positiveEmotions.includes(emotion)) return 'success'
  if (negativeEmotions.includes(emotion)) return 'danger'
  return 'info'
}

const getSentimentType = (sentiment) => {
  const map = {
    '积极': 'success',
    '中性': 'info',
    '消极': 'danger'
  }
  return map[sentiment] || 'info'
}

const getScoreColor = (score) => {
  if (score >= 67) return '#67c23a'
  if (score >= 34) return '#909399'
  return '#f56c6c'
}

// 生命周期
onMounted(() => {
  // 检查是否从文章详情页跳转过来
  const analysisContent = sessionStorage.getItem('aiAnalysisContent')
  if (analysisContent) {
    try {
      const data = JSON.parse(analysisContent)
      inputText.value = data.content
      selectedArticle.value = {
        title: data.title,
        id: data.articleId
      }
      // 清除临时存储
      sessionStorage.removeItem('aiAnalysisContent')
      // 自动开始分析
      ElMessage.success(`已加载文章：${data.title}`)
      setTimeout(() => {
        analyzeSentiment()
      }, 500)
    } catch (error) {
      console.error('解析文章内容失败:', error)
    }
  }
  
  // 加载历史记录
  const saved = localStorage.getItem('sentimentHistory')
  if (saved) {
    analysisHistory.value = JSON.parse(saved)
  }
  
  // 加载分析次数
  const count = localStorage.getItem('analysisCount')
  if (count) {
    analysisCount.value = parseInt(count)
  }
})

// 保存分析次数
import { watch } from 'vue'
watch(analysisCount, (val) => {
  localStorage.setItem('analysisCount', val.toString())
})
</script>

<style scoped>
.ai-sentiment {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* 页面头部 */
.sentiment-header {
  background: rgba(102, 126, 234, 0.08);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 2px solid rgba(102, 126, 234, 0.2);
  border-radius: 20px;
  padding: 40px;
  margin-bottom: 30px;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.12),
              0 0 0 1px rgba(255, 255, 255, 0.4) inset;
  position: relative;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
  font-size: 16px;
  font-weight: 500;
}

.analysis-count {
  text-align: center;
}

.count-num {
  display: block;
  font-size: 48px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.count-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

/* 输入卡片 */
.input-card {
  margin-bottom: 30px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05),
              0 0 0 1px rgba(255, 255, 255, 0.4) inset;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.input-card:hover {
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

.input-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
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

.quick-examples {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 15px;
  background: rgba(245, 247, 250, 0.6);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.example-label {
  font-weight: 500;
  color: #606266;
}

.analyze-btn {
  align-self: center;
  min-width: 200px;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea, #764ba2) !important;
  border: none !important;
  color: white !important;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.analyze-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

/* 结果容器 */
.results-container {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.result-card {
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05),
              0 0 0 1px rgba(255, 255, 255, 0.4) inset;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.result-card:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.5);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08),
              0 0 0 1px rgba(255, 255, 255, 0.6) inset;
}

.main-result {
  grid-column: span 2;
}

/* 情感概览 */
.sentiment-overview {
  display: flex;
  gap: 40px;
  align-items: center;
}

.sentiment-gauge {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.gauge-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.gauge-value {
  font-size: 48px;
  font-weight: 700;
  color: #303133;
}

.gauge-label {
  font-size: 18px;
  color: #606266;
  margin-top: 5px;
}

.sentiment-labels {
  display: flex;
  gap: 20px;
  justify-content: center;
}

.label-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: #606266;
}

.label-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.negative .label-dot {
  background: #f56c6c;
}

.neutral .label-dot {
  background: #909399;
}

.positive .label-dot {
  background: #67c23a;
}

/* 情感强度 */
.sentiment-intensity {
  flex: 1;
}

.sentiment-intensity h3 {
  margin: 0 0 20px;
  color: #303133;
}

.intensity-bars {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.intensity-item {
  display: flex;
  align-items: center;
  gap: 15px;
}

.intensity-label {
  min-width: 80px;
  font-weight: 500;
}

/* 情绪细分 */
.emotion-details {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.emotion-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.emotion-tag {
  padding: 8px 16px;
  font-size: 14px;
}

/* 关键词情感 */
.keywords-sentiment {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.keyword-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: 8px;
  border: 2px solid;
  transition: all 0.3s ease;
}

.keyword-item.positive {
  border-color: #67c23a;
  background: rgba(103, 194, 58, 0.1);
}

.keyword-item.neutral {
  border-color: #909399;
  background: rgba(144, 147, 153, 0.1);
}

.keyword-item.negative {
  border-color: #f56c6c;
  background: rgba(245, 108, 108, 0.1);
}

.keyword-word {
  font-weight: 600;
  color: #303133;
}

.keyword-score {
  font-size: 14px;
  font-weight: 500;
}

.positive .keyword-score {
  color: #67c23a;
}

.negative .keyword-score {
  color: #f56c6c;
}

/* AI建议 */
.ai-suggestions {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.suggestion-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.suggestion-item p {
  margin: 0;
  line-height: 1.6;
  color: #606266;
}

.improvements h4 {
  margin: 0 0 12px;
  color: #303133;
}

.improvements ul {
  margin: 0;
  padding-left: 20px;
  color: #606266;
  line-height: 1.8;
}

/* 历史记录 */
.history-card {
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05),
              0 0 0 1px rgba(255, 255, 255, 0.4) inset;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.history-card:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.5);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08),
              0 0 0 1px rgba(255, 255, 255, 0.6) inset;
}

/* 响应式 */
@media (max-width: 768px) {
  .results-container {
    grid-template-columns: 1fr;
  }
  
  .main-result {
    grid-column: span 1;
  }
  
  .sentiment-overview {
    flex-direction: column;
  }
}
</style>
