<template>
  <div class="chatgpt-layout">
    <!-- 左侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <button class="new-chat-btn" @click="clearChat">
          <el-icon><Plus /></el-icon>
          <span v-if="!sidebarCollapsed">新对话</span>
        </button>
        <button class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">
          <el-icon><Fold v-if="!sidebarCollapsed" /><Expand v-else /></el-icon>
        </button>
      </div>
      
      <div class="sidebar-content" v-show="!sidebarCollapsed">
        <!-- 快捷功能 -->
        <div class="sidebar-section">
          <div class="section-title">快捷功能</div>
          <div class="nav-item" @click="sendQuickQuestion('帮我搜索最近的校园活动')">
            <el-icon><Search /></el-icon>
            <span>智能搜索</span>
          </div>
          <div class="nav-item" @click="sendQuickQuestion('帮我写一篇关于校园文化的文章摘要')">
            <el-icon><Edit /></el-icon>
            <span>写作辅助</span>
          </div>
          <div class="nav-item" @click="sendQuickQuestion('分析一下最近的热门话题')">
            <el-icon><TrendCharts /></el-icon>
            <span>热点分析</span>
          </div>
          <div class="nav-item" @click="sendQuickQuestion('系统数据统计')">
            <el-icon><DataAnalysis /></el-icon>
            <span>数据统计</span>
          </div>
        </div>

        <!-- 热门话题 -->
        <div class="sidebar-section">
          <div class="section-title">热门话题</div>
          <div 
            v-for="(topic, index) in hotTopics" 
            :key="index" 
            class="nav-item"
            @click="sendQuickQuestion(`搜索关于${topic}的新闻`)"
          >
            <span class="topic-badge" :class="{ hot: index < 3 }">{{ index + 1 }}</span>
            <span>{{ topic }}</span>
          </div>
        </div>

        <!-- 今日数据 -->
        <div class="sidebar-section stats-section">
          <div class="section-title">今日数据</div>
          <div class="mini-stats">
            <div class="mini-stat">
              <span class="stat-num">{{ todayStats.articles }}</span>
              <span class="stat-text">文章</span>
            </div>
            <div class="mini-stat">
              <span class="stat-num">{{ todayStats.comments }}</span>
              <span class="stat-text">评论</span>
            </div>
            <div class="mini-stat">
              <span class="stat-num">{{ todayStats.views }}</span>
              <span class="stat-text">浏览</span>
            </div>
          </div>
        </div>
      </div>

      <div class="sidebar-footer" v-show="!sidebarCollapsed">
        <div class="nav-item" @click="$router.push('/ai-help')">
          <el-icon><QuestionFilled /></el-icon>
          <span>使用帮助</span>
        </div>
        <el-dropdown trigger="click" placement="top-start" v-if="userStore.user" @command="handleUserCommand">
          <div class="user-info">
            <el-avatar :size="32" :src="userStore.user?.avatar" class="user-avatar-small">
              {{ userStore.user?.realName?.[0] || userStore.user?.username?.[0] || 'U' }}
            </el-avatar>
            <span class="user-name">{{ userStore.user?.realName || userStore.user?.username }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>
                <span>个人中心</span>
              </el-dropdown-item>
              <el-dropdown-item command="settings">
                <el-icon><Setting /></el-icon>
                <span>账号设置</span>
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>
                <span>退出登录</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </aside>

    <!-- 主聊天区域 -->
    <main class="main-content">
      <!-- 顶部标题栏 -->
      <header class="main-header">
        <el-dropdown trigger="click" @command="handleModelChange">
          <div class="model-selector">
            <el-icon><ChatDotRound /></el-icon>
            <span>{{ currentModelName }}</span>
            <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item 
                v-for="m in modelOptions" 
                :key="m.value" 
                :command="m.value"
                :class="{ 'is-active': currentModel === m.value }"
              >
                <div class="model-option">
                  <span class="model-name">{{ m.label }}</span>
                  <span class="model-desc">{{ m.desc }}</span>
                  <el-icon v-if="currentModel === m.value" class="check-icon"><Check /></el-icon>
                </div>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <!-- 聊天消息区域 -->
      <div class="chat-area" ref="messagesContainer">
        <!-- 欢迎界面 -->
        <div v-if="messages.length === 0" class="welcome-screen">
          <h1 class="welcome-title">有什么可以帮忙的？</h1>
          <div class="suggestion-grid">
            <div 
              v-for="(item, index) in suggestionCards" 
              :key="index" 
              class="suggestion-card"
              @click="sendQuickQuestion(item.prompt)"
            >
              <el-icon :size="20"><component :is="item.icon" /></el-icon>
              <span>{{ item.text }}</span>
            </div>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="messages-wrapper" v-else>
          <div 
            v-for="(msg, index) in messages" 
            :key="index" 
            class="message-row"
            :class="msg.role"
          >
            <div class="message-container">
              <div class="avatar-wrapper">
                <el-avatar v-if="msg.role === 'user'" :size="36" :src="userStore.user?.avatar" class="avatar user">
                  {{ userStore.user?.realName?.[0] || userStore.user?.username?.[0] || 'U' }}
                </el-avatar>
                <el-avatar v-else :size="36" :src="logoUrl" class="avatar assistant" />
              </div>
              <div class="message-body">
                <div class="message-sender">{{ msg.role === 'user' ? '你' : 'WHUTGPT' }}</div>
                <div class="message-content" v-html="formatMessage(msg.content)"></div>
              </div>
            </div>
          </div>

          <!-- 加载状态 -->
          <div v-if="loading" class="message-row assistant">
            <div class="message-container">
              <div class="avatar-wrapper">
                <el-avatar :size="36" :src="logoUrl" class="avatar assistant" />
              </div>
              <div class="message-body">
                <div class="message-sender">WHUTGPT</div>
                <div class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部输入区域 -->
      <div class="input-section">
        <div class="input-container">
          <div class="input-box">
            <button class="attach-btn">
              <el-icon><Plus /></el-icon>
            </button>
            <textarea 
              v-model="inputMessage"
              placeholder="询问任何问题"
              rows="1"
              @keydown.enter.exact.prevent="sendMessage"
              @input="autoResize"
              ref="textareaRef"
              :disabled="loading"
            ></textarea>
            <button 
              class="send-btn" 
              :class="{ active: inputMessage.trim() && !loading }"
              @click="sendMessage"
              :disabled="!inputMessage.trim() || loading"
            >
              <el-icon v-if="!loading"><Top /></el-icon>
              <el-icon v-else class="loading-icon"><Loading /></el-icon>
            </button>
          </div>
          <div class="input-hint">
            WHUTGPT可搜索校园新闻、解答问题、辅助写作
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, computed } from 'vue'
import { 
  QuestionFilled, Search, Edit, TrendCharts, DataAnalysis, 
  Plus, Fold, Expand, ArrowDown, Top, Loading,
  Document, Compass, EditPen, ChatLineRound, ChatDotRound, Check,
  User, Setting, SwitchButton
} from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import logoUrl from '@/assets/whut-logo.png'
import { useUserStore } from '@/stores/user'
import { sendChatMessage } from '@/api/ai'
import { getArticleList } from '@/api/article'

const userStore = useUserStore()
const router = useRouter()
const messagesContainer = ref(null)
const textareaRef = ref(null)

// 状态
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const sessionId = ref('')
const sidebarCollapsed = ref(false)

// 模型选择
const currentModel = ref('kimi')
const modelOptions = [
  { value: 'kimi', label: 'Kimi', desc: 'Moonshot AI' },
  { value: 'deepseek', label: 'DeepSeek', desc: 'DeepSeek AI' },
  { value: 'doubao', label: '豆包', desc: '字节跳动 AI' }
]
const currentModelName = computed(() => {
  const model = modelOptions.find(m => m.value === currentModel.value)
  return model ? `WHUTGPT · ${model.label}` : 'WHUTGPT'
})

// 切换模型
const handleModelChange = (model) => {
  currentModel.value = model
  const modelInfo = modelOptions.find(m => m.value === model)
  ElMessage.success(`已切换到 ${modelInfo?.label || model} 模型`)
}

// 用户菜单操作
const handleUserCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/profile')
      break
    case 'logout':
      userStore.logout()
      router.push('/login')
      ElMessage.success('已退出登录')
      break
  }
}

// 今日数据统计
const todayStats = ref({
  articles: 0,
  comments: 0,
  users: 0,
  views: 0
})

// 热门话题
const hotTopics = ref([
  '校园活动',
  '讲座信息',
  '比赛通知',
  '就业招聘',
  '社团纳新'
])

// 建议卡片
const suggestionCards = [
  { text: '搜索最新校园活动', prompt: '搜索最新的校园活动', icon: 'Search' },
  { text: '查看热门文章', prompt: '最热门的文章是什么？', icon: 'Document' },
  { text: '帮我写文章摘要', prompt: '帮我写一篇关于校园文化的文章摘要', icon: 'EditPen' },
  { text: '了解系统功能', prompt: '系统有哪些主要功能？', icon: 'Compass' }
]

// 自动调整输入框高度
const autoResize = () => {
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
    textareaRef.value.style.height = Math.min(textareaRef.value.scrollHeight, 200) + 'px'
  }
}

// 获取今日数据
const fetchTodayStats = async () => {
  try {
    const result = await getArticleList({ current: 1, size: 100, sortBy: 'date', sortOrder: 'desc' })
    const articles = result.records || []
    const today = new Date().toDateString()
    
    // 统计今日文章
    const todayArticles = articles.filter(a => new Date(a.createdAt).toDateString() === today)
    todayStats.value.articles = todayArticles.length
    
    // 计算总浏览
    todayStats.value.views = articles.reduce((sum, a) => sum + (a.viewCount || 0), 0)
    
    // 模拟评论和用户数据
    todayStats.value.comments = Math.floor(Math.random() * 20) + 5
    todayStats.value.users = Math.floor(Math.random() * 50) + 10
    
    // 从文章标题提取热门话题
    const keywords = ['活动', '讲座', '比赛', '招聘', '通知', '纳新', '考试', '培训']
    const topicCounts = {}
    articles.forEach(a => {
      keywords.forEach(k => {
        if (a.title?.includes(k)) {
          topicCounts[k] = (topicCounts[k] || 0) + 1
        }
      })
    })
    
    // 按出现次数排序
    const sortedTopics = Object.entries(topicCounts)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 5)
      .map(([k]) => k)
    
    if (sortedTopics.length > 0) {
      hotTopics.value = sortedTopics
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 发送消息
const sendMessage = async () => {
  const question = inputMessage.value.trim()
  if (!question || loading.value) return

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: question,
    timestamp: Date.now()
  })
  
  inputMessage.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const response = await sendChatMessage({
      question,
      sessionId: sessionId.value || undefined,
      model: currentModel.value
    })
    
    // 保存会话ID
    if (response.sessionId) {
      sessionId.value = response.sessionId
    }
    
    // 添加AI回复
    messages.value.push({
      role: 'assistant',
      content: response.answer,
      timestamp: response.timestamp || Date.now()
    })
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送失败，请稍后重试')
    
    // 添加错误提示消息
    messages.value.push({
      role: 'assistant',
      content: '抱歉，我暂时无法回复。请稍后再试。',
      timestamp: Date.now()
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 发送快捷问题
const sendQuickQuestion = (question) => {
  inputMessage.value = question
  sendMessage()
}

// 清空对话
const clearChat = () => {
  messages.value = []
  sessionId.value = ''
  ElMessage.success('对话已清空')
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 格式化时间
const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 格式化消息（支持简单的 Markdown 和文章链接）
const formatMessage = (content) => {
  if (!content) return ''
  
  return content
    // 转义 HTML
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    // 粗体
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    // 将文章链接转换为可点击的链接
    .replace(/\/article\/(\d+)/g, '<a href="/article/$1" class="article-link" onclick="event.stopPropagation()">🔗 查看文章</a>')
    // 将《标题》格式的文章标题加粗
    .replace(/《(.*?)》/g, '<strong class="article-title">《$1》</strong>')
    // 换行
    .replace(/\n/g, '<br>')
    // 列表项
    .replace(/^(\d+)\.\s/gm, '<span class="list-number">$1.</span> ')
    // emoji 表情保持原样
}

onMounted(() => {
  scrollToBottom()
  fetchTodayStats()
})
</script>

<style scoped>
/* ChatGPT 风格布局 */
.chatgpt-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: #ffffff;
}

/* 左侧边栏 */
.sidebar {
  width: 260px;
  background: #f9f9f9;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  border-right: 1px solid #e5e5e5;
}

.sidebar.collapsed {
  width: 60px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  gap: 8px;
}

.new-chat-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 16px;
  background: transparent;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  color: #333;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.new-chat-btn:hover {
  background: #ececec;
}

.sidebar.collapsed .new-chat-btn {
  padding: 12px;
}

.sidebar.collapsed .new-chat-btn span {
  display: none;
}

.collapse-btn {
  padding: 10px;
  background: transparent;
  border: none;
  color: #666;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

.collapse-btn:hover {
  background: #ececec;
  color: #333;
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px;
}

.sidebar-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 12px;
  color: #888;
  padding: 8px 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  color: #333;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
}

.nav-item:hover {
  background: #ececec;
  color: #000;
}

.nav-item .el-icon {
  font-size: 18px;
  opacity: 0.8;
}

.topic-badge {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  background: #e5e5e5;
  border-radius: 4px;
  color: #666;
}

.topic-badge.hot {
  background: linear-gradient(135deg, #f56c6c, #e6a23c);
  color: #fff;
}

/* 今日数据 */
.stats-section {
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  margin: 0 0 24px;
  border: 1px solid #e5e5e5;
}

.mini-stats {
  display: flex;
  justify-content: space-around;
}

.mini-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-num {
  font-size: 20px;
  font-weight: 600;
  color: #10a37f;
}

.stat-text {
  font-size: 11px;
  color: #888;
}

/* 侧边栏底部 */
.sidebar-footer {
  padding: 12px;
  border-top: 1px solid #e5e5e5;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  margin-top: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.user-info:hover {
  background: #ececec;
}

.user-avatar-small {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 14px;
  flex-shrink: 0;
}

.user-name {
  color: #333;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

/* 用户下拉菜单 */
:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
}

:deep(.el-dropdown-menu__item .el-icon) {
  font-size: 16px;
}

/* 主内容区 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  overflow: hidden;
}

.main-header {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 20px;
  border-bottom: 1px solid #e5e5e5;
}

.model-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #f5f5f5;
  border-radius: 8px;
  color: #333;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.model-selector:hover {
  background: #ececec;
}

.dropdown-icon {
  font-size: 14px;
  opacity: 0.6;
}

/* 模型选择下拉菜单 */
.model-option {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 180px;
  padding: 4px 0;
}

.model-name {
  font-weight: 500;
  color: #333;
}

.model-desc {
  font-size: 12px;
  color: #999;
  flex: 1;
}

.check-icon {
  color: #10a37f;
  font-size: 16px;
}

:deep(.el-dropdown-menu__item.is-active) {
  background-color: #f0f9f6;
}

:deep(.el-dropdown-menu__item:hover) {
  background-color: #f5f5f5;
}

/* 聊天区域 */
.chat-area {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

/* 欢迎界面 */
.welcome-screen {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.welcome-title {
  font-size: 32px;
  font-weight: 600;
  color: #333;
  margin-bottom: 40px;
}

.suggestion-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  max-width: 600px;
  width: 100%;
}

.suggestion-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: #f9f9f9;
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  color: #333;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.suggestion-card:hover {
  background: #f0f0f0;
  border-color: #d0d0d0;
  color: #000;
}

.suggestion-card .el-icon {
  opacity: 0.7;
}

/* 消息列表 */
.messages-wrapper {
  max-width: 768px;
  width: 100%;
  margin: 0 auto;
  padding: 20px;
}

.message-row {
  padding: 24px 0;
}

.message-row:not(:last-child) {
  border-bottom: 1px solid #f0f0f0;
}

.message-container {
  display: flex;
  gap: 16px;
}

.avatar-wrapper {
  flex-shrink: 0;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.avatar.user {
  background: linear-gradient(135deg, #5436DA, #764ba2);
  color: #fff;
}

.avatar.assistant {
  background: #fff;
  border: 1px solid #e5e5e5;
}

.message-body {
  flex: 1;
  min-width: 0;
}

.message-sender {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.message-content {
  color: #333;
  font-size: 15px;
  line-height: 1.7;
  word-break: break-word;
}

.message-content :deep(strong) {
  font-weight: 600;
  color: #000;
}

.message-content :deep(.list-number) {
  color: #10a37f;
  font-weight: 600;
}

.message-content :deep(.article-link) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #10a37f;
  text-decoration: none;
  padding: 4px 12px;
  background: rgba(16, 163, 127, 0.1);
  border-radius: 6px;
  font-size: 13px;
  transition: all 0.2s;
  margin: 4px 4px 4px 0;
}

.message-content :deep(.article-link:hover) {
  background: rgba(16, 163, 127, 0.2);
}

.message-content :deep(.article-title) {
  color: #10a37f;
}

/* 打字动画 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #10a37f;
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out both;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }
.typing-indicator span:nth-child(3) { animation-delay: 0; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

/* 输入区域 */
.input-section {
  padding: 20px;
  background: linear-gradient(180deg, transparent, #ffffff 20%);
}

.input-container {
  max-width: 768px;
  margin: 0 auto;
}

.input-box {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  background: #f4f4f4;
  border: 1px solid #e5e5e5;
  border-radius: 24px;
  padding: 8px 12px;
  transition: all 0.2s;
}

.input-box:focus-within {
  border-color: #c0c0c0;
  background: #fff;
}

.attach-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  color: #666;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s;
}

.attach-btn:hover {
  background: #e5e5e5;
  color: #333;
}

.input-box textarea {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #333;
  font-size: 15px;
  line-height: 1.5;
  resize: none;
  max-height: 200px;
  padding: 8px 0;
}

.input-box textarea::placeholder {
  color: #999;
}

.input-box .send-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e5e5e5;
  border: none;
  border-radius: 50%;
  color: #999;
  cursor: not-allowed;
  transition: all 0.2s;
}

.input-box .send-btn.active {
  background: #10a37f;
  color: #fff;
  cursor: pointer;
}

.input-box .send-btn.active:hover {
  opacity: 0.9;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.input-hint {
  text-align: center;
  font-size: 12px;
  color: #999;
  margin-top: 12px;
}

/* 滚动条样式 */
.chat-area::-webkit-scrollbar,
.sidebar-content::-webkit-scrollbar {
  width: 6px;
}

.chat-area::-webkit-scrollbar-track,
.sidebar-content::-webkit-scrollbar-track {
  background: transparent;
}

.chat-area::-webkit-scrollbar-thumb,
.sidebar-content::-webkit-scrollbar-thumb {
  background: #d0d0d0;
  border-radius: 3px;
}

.chat-area::-webkit-scrollbar-thumb:hover,
.sidebar-content::-webkit-scrollbar-thumb:hover {
  background: #b0b0b0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    height: 100vh;
    z-index: 1000;
    transform: translateX(-100%);
  }

  .sidebar:not(.collapsed) {
    transform: translateX(0);
  }

  .welcome-title {
    font-size: 24px;
  }

  .suggestion-grid {
    grid-template-columns: 1fr;
  }

  .messages-wrapper {
    padding: 16px;
  }

  .input-container {
    padding: 0 12px;
  }
}
</style>
