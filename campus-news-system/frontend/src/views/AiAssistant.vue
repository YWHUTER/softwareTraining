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
        <!-- 历史记录 -->
        <div class="sidebar-section">
          <div class="section-title">📜 历史记录</div>
          <div v-if="historyLoading" class="history-loading">
            <el-icon class="loading-icon"><Loading /></el-icon>
            <span>加载中...</span>
          </div>
          <div v-else-if="chatSessions.length === 0" class="history-empty">
            暂无历史对话
          </div>
          <div v-else class="history-list">
            <div 
              v-for="session in chatSessions.slice(0, 10)" 
              :key="session.id" 
              class="history-item"
              :class="{ active: currentSessionId === session.id }"
              @click="loadSession(session.id)"
            >
              <el-icon><ChatLineRound /></el-icon>
              <span class="history-title">{{ session.title }}</span>
              <el-icon 
                class="delete-btn" 
                @click.stop="deleteSession(session.id)"
              ><Delete /></el-icon>
            </div>
          </div>
        </div>

        <!-- 快捷功能 -->
        <div class="sidebar-section">
          <div class="section-title">💡 快捷功能</div>
          <div class="nav-item" @click="sendQuickQuestion('帮我搜索关于讲座的新闻')">
            <el-icon><Search /></el-icon>
            <span>智能搜索</span>
          </div>
          <div class="nav-item" @click="sendQuickQuestion('请帮我写一篇校园活动的新闻稿，包含标题、摘要和正文框架')">
            <el-icon><Edit /></el-icon>
            <span>写作辅助</span>
          </div>
          <div class="nav-item" @click="sendQuickQuestion('帮我看看浏览量最高的热门文章有哪些')">
            <el-icon><TrendCharts /></el-icon>
            <span>热门排行</span>
          </div>
          <div class="nav-item" @click="sendQuickQuestion('统计一下系统目前有多少文章、用户和总浏览量')">
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
      <!-- 展开侧边栏按钮 (当侧边栏折叠时显示) -->
      <button v-if="sidebarCollapsed" class="show-sidebar-btn" @click="sidebarCollapsed = false">
        <el-icon><Expand /></el-icon>
      </button>

      <!-- 顶部标题栏 -->
      <header class="main-header">
        <el-dropdown trigger="click" @command="handleModelChange" popper-class="model-dropdown-popper">
          <div class="model-selector">
            <img :src="whutLogo" alt="WHUT" class="whut-badge" />
            <span class="model-selector-text">WHUTGPT <span class="model-version">{{ modelOptions.find(m => m.value === currentModel)?.label }}</span></span>
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
                  <el-icon class="model-icon">
                    <img v-if="m.useImage" :src="m.image" class="model-logo-img" @error="handleImageError(m.value)" />
                    <component v-else :is="m.icon" />
                  </el-icon>
                  <div class="model-info">
                    <span class="model-name">{{ m.label }}</span>
                    <span class="model-desc">{{ m.desc }}</span>
                  </div>
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
          <h1 class="welcome-title">🎓 你好，我是WHUTGPT</h1>
          <p class="welcome-subtitle">你的校园新闻智能助手，可以搜索新闻、查看数据、辅助写作</p>
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
                <div class="message-content">
                  <span v-html="formatMessage(msg.content)"></span>
                  <span v-if="msg.streaming" class="typing-cursor">|</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 加载状态 - 仅在流式输出开始前显示 -->
          <div v-if="loading && !messages.some(m => m.streaming)" class="message-row assistant">
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
              placeholder="试试问我：最近有什么热门新闻？帮我写一篇活动稿..."
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
            💡 WHUTGPT可查询实时数据、搜索新闻、辅助写作、解答系统问题
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
  User, Setting, SwitchButton, Delete, Moon, Lightning, Coordinate
} from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import logoUrl from '@/assets/whut-logo.png'
import { useUserStore } from '@/stores/user'
import { 
  sendChatMessage, streamChat, 
  getChatSessions, getChatSessionDetail, createChatSession, 
  saveChatMessage, deleteChatSession 
} from '@/api/ai'
import { getArticleList } from '@/api/article'
import kimiLogo from '@/assets/icons/kimi.png'
import deepseekLogo from '@/assets/icons/deepseek.png'
import doubaoLogo from '@/assets/icons/doubao.png'
import whutLogo from '@/assets/whut-logo.png'

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

// 历史记录相关状态
const chatSessions = ref([])
const currentSessionId = ref(null)
const historyLoading = ref(false)

// 模型选择
const currentModel = ref('kimi')
const modelOptions = ref([
  { value: 'kimi', label: 'Kimi', desc: 'Moonshot AI', image: kimiLogo, icon: 'Moon', useImage: true },
  { value: 'deepseek', label: 'DeepSeek', desc: 'DeepSeek AI', image: deepseekLogo, icon: 'Lightning', useImage: true },
  { value: 'doubao', label: '豆包', desc: '字节跳动 AI', image: doubaoLogo, icon: 'Coordinate', useImage: true }
])

const handleImageError = (modelValue) => {
  const model = modelOptions.value.find(m => m.value === modelValue)
  if (model) {
    model.useImage = false
  }
}

const currentModelName = computed(() => {
  const model = modelOptions.value.find(m => m.value === currentModel.value)
  return model ? `${model.label} ${model.desc}` : 'WHUTGPT'
})

// 切换模型
const handleModelChange = (model) => {
  currentModel.value = model
  const modelInfo = modelOptions.value.find(m => m.value === model)
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

// 建议卡片 - 覆盖AI的主要能力场景
const suggestionCards = [
  { text: '🔥 热门文章排行', prompt: '帮我看看浏览量最高的热门文章有哪些？', icon: 'Document' },
  { text: '📊 系统数据统计', prompt: '统计一下系统目前有多少文章、用户和总浏览量', icon: 'DataAnalysis' },
  { text: '✍️ 帮我写新闻稿', prompt: '请帮我写一篇关于校园文化活动的新闻稿，包含标题、摘要和正文框架', icon: 'EditPen' },
  { text: '📖 系统使用指南', prompt: '请详细介绍一下这个系统怎么使用？如何发布文章、关注用户？', icon: 'Compass' },
  { text: '🆕 最新发布文章', prompt: '最近有什么新发布的文章？', icon: 'Search' },
  { text: '⭐ 粉丝排行榜', prompt: '平台上粉丝最多的用户是谁？给我看看排行榜', icon: 'TrendCharts' }
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

// 发送消息 - 使用流式输出实现打字机效果
const sendMessage = async () => {
  const question = inputMessage.value.trim()
  if (!question || loading.value) return

  // 如果是新对话，先创建会话
  let activeSessionId = currentSessionId.value
  if (!activeSessionId) {
    try {
      const session = await createChatSession({
        model: currentModel.value,
        firstMessage: question
      })
      activeSessionId = session.id
      currentSessionId.value = session.id
      // 刷新历史列表
      fetchChatSessions()
    } catch (error) {
      console.error('创建会话失败:', error)
    }
  }

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: question,
    timestamp: Date.now()
  })

  // 保存用户消息到数据库
  if (activeSessionId) {
    saveChatMessage(activeSessionId, { role: 'user', content: question }).catch(console.error)
  }
  
  inputMessage.value = ''
  loading.value = true
  
  // 预先添加一个空的AI回复消息，用于流式更新
  const aiMessageIndex = messages.value.length
  messages.value.push({
    role: 'assistant',
    content: '',
    timestamp: Date.now(),
    streaming: true  // 标记正在流式输出
  })
  
  scrollToBottom()

  try {
    // 🔥 使用流式API - 实现打字机效果
    await streamChat(
      {
        question,
        sessionId: sessionId.value || undefined,
        model: currentModel.value
      },
      // onMessage: 每收到一个字符/片段就更新消息
      (content) => {
        messages.value[aiMessageIndex].content += content
        scrollToBottom()
      },
      // onError: 发生错误
      (error) => {
        console.error('流式聊天错误:', error)
        messages.value[aiMessageIndex].content = '抱歉，AI服务暂时出现问题，请稍后重试。'
        messages.value[aiMessageIndex].streaming = false
        loading.value = false
      },
      // onComplete: 流式输出完成
      (newSessionId) => {
        if (newSessionId) {
          sessionId.value = newSessionId
        }
        messages.value[aiMessageIndex].streaming = false
        loading.value = false
        scrollToBottom()
        
        // 保存AI回复到数据库
        if (activeSessionId && messages.value[aiMessageIndex].content) {
          saveChatMessage(activeSessionId, { 
            role: 'assistant', 
            content: messages.value[aiMessageIndex].content 
          }).catch(console.error)
        }
      }
    )
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送失败，请稍后重试')
    
    // 更新错误消息
    messages.value[aiMessageIndex].content = '抱歉，我暂时无法回复。请稍后再试。'
    messages.value[aiMessageIndex].streaming = false
    loading.value = false
    scrollToBottom()
  }
}

// 发送快捷问题
const sendQuickQuestion = (question) => {
  inputMessage.value = question
  sendMessage()
}

// 清空对话（开始新对话）
const clearChat = () => {
  messages.value = []
  sessionId.value = ''
  currentSessionId.value = null
  ElMessage.success('已开始新对话')
}

// 获取历史会话列表
const fetchChatSessions = async () => {
  historyLoading.value = true
  try {
    const data = await getChatSessions()
    chatSessions.value = data || []
  } catch (error) {
    console.error('获取历史记录失败:', error)
  } finally {
    historyLoading.value = false
  }
}

// 加载历史会话
const loadSession = async (sessionId) => {
  if (currentSessionId.value === sessionId) return
  
  try {
    const session = await getChatSessionDetail(sessionId)
    if (session && session.messages) {
      currentSessionId.value = sessionId
      currentModel.value = session.model || 'kimi'
      // 转换消息格式
      messages.value = session.messages.map(msg => ({
        role: msg.role,
        content: msg.content,
        timestamp: new Date(msg.createdAt).getTime()
      }))
      scrollToBottom()
      ElMessage.success('已加载历史对话')
    }
  } catch (error) {
    console.error('加载会话失败:', error)
    ElMessage.error('加载失败')
  }
}

// 删除会话
const deleteSession = async (sessionId) => {
  try {
    await deleteChatSession(sessionId)
    chatSessions.value = chatSessions.value.filter(s => s.id !== sessionId)
    // 如果删除的是当前会话，清空界面
    if (currentSessionId.value === sessionId) {
      clearChat()
    }
    ElMessage.success('已删除')
  } catch (error) {
    console.error('删除会话失败:', error)
    ElMessage.error('删除失败')
  }
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

// 格式化消息（支持 Markdown 和文章链接）
const formatMessage = (content) => {
  if (!content) return ''
  
  let html = content
    // 转义 HTML 特殊字符 (除 Markdown 语法外)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  // 处理代码块 (```code```)
  html = html.replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
  
  // 处理行内代码 (`code`)
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>')
  
  // 处理标题 (### Title)
  html = html.replace(/^### (.*$)/gm, '<h3>$1</h3>')
  html = html.replace(/^## (.*$)/gm, '<h3>$1</h3>') // 二级标题也转为 h3 样式
  
  // 处理引用 (> quote)
  html = html.replace(/^> (.*$)/gm, '<blockquote>$1</blockquote>')
  
  // 处理无序列表 (- item)
  html = html.replace(/^\- (.*$)/gm, '<li>$1</li>')
  // 将相邻的 li 包裹在 ul 中 (简单处理)
  html = html.replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>')
  
  // 处理粗体 (**bold**)
  html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
  
  // 处理文章链接 (/article/123)
  html = html.replace(/\/article\/(\d+)/g, '<a href="/article/$1" class="article-link" onclick="event.stopPropagation()">🔗 查看文章</a>')
  
  // 处理文章标题 (《Title》)
  html = html.replace(/《(.*?)》/g, '<strong class="article-title">《$1》</strong>')
  
  // 处理换行 (非代码块内的换行)
  // 注意：这里简化处理，可能会影响代码块内的换行，实际应使用 markdown-it
  html = html.replace(/\n/g, '<br>')
  
  return html
}

onMounted(() => {
  scrollToBottom()
  fetchTodayStats()
  fetchChatSessions()
})
</script>

<style scoped>
/* ChatGPT 风格布局 */
.chatgpt-layout {
  display: flex;
  /* 减去顶部导航栏的高度，假设为60px-64px，这里预留一些余量 */
  height: calc(100vh - 64px);
  overflow: hidden;
  background: #ffffff;
  font-family: "Söhne", "ui-sans-serif", "system-ui", -apple-system, "Segoe UI", Roboto, Ubuntu, Cantarell, "Noto Sans", sans-serif, "Helvetica Neue", Arial, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji";
  border-radius: 8px; /* 如果是在容器中，圆角会好看点 */
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

/* 左侧边栏 */
.sidebar {
  width: 260px;
  background: #f9f9f9;
  display: flex;
  flex-direction: column;
  transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.4s ease;
  border-right: 1px solid rgba(0, 0, 0, 0.05);
  z-index: 100;
  flex-shrink: 0;
  overflow: hidden;
  white-space: nowrap;
}

.sidebar.collapsed {
  width: 0;
  border-right-color: transparent;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 12px;
  gap: 8px;
}

.new-chat-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  padding: 12px 16px;
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  color: #333;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
  white-space: nowrap; /* 防止文字换行 */
  overflow: hidden;
}

.new-chat-btn:hover {
  background: #f0f0f0;
  border-color: #d0d0d0;
}

.collapse-btn {
  padding: 8px;
  background: transparent;
  border: 1px solid transparent;
  color: #666;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
  flex-shrink: 0;
}

.collapse-btn:hover {
  background: #ececec;
  color: #333;
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.sidebar-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 12px;
  color: #999;
  padding: 8px 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* 历史记录样式 */
.history-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px;
  color: #888;
  font-size: 13px;
}

.history-empty {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 13px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.history-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
  color: #333;
  overflow: hidden;
}

.history-item:hover {
  background: #ececec;
}

.history-item.active {
  background: #e0e0e0;
  color: #000;
}

.history-item .el-icon {
  font-size: 16px;
  opacity: 0.7;
  flex-shrink: 0;
}

.history-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 渐变遮罩，让长标题看起来更柔和 */
.history-item::after {
  content: '';
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 40px;
  background: linear-gradient(to right, transparent, #f9f9f9);
  pointer-events: none;
  opacity: 0;
}

.history-item:hover::after {
  background: linear-gradient(to right, transparent, #ececec);
  opacity: 1;
}

.history-item.active::after {
  background: linear-gradient(to right, transparent, #e0e0e0);
  opacity: 1;
}

.history-item .delete-btn {
  position: absolute;
  right: 8px;
  opacity: 0;
  color: #666;
  transition: all 0.2s;
  z-index: 2;
  padding: 4px;
  border-radius: 4px;
}

.history-item:hover .delete-btn {
  opacity: 1;
  background: #ececec;
}

.history-item .delete-btn:hover {
  color: #f56c6c;
  background: #e0e0e0;
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
  white-space: nowrap;
  overflow: hidden;
}

.nav-item:hover {
  background: #ececec;
  color: #000;
}

.nav-item .el-icon {
  font-size: 18px;
  opacity: 0.8;
  flex-shrink: 0;
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
  flex-shrink: 0;
}

.topic-badge.hot {
  background: linear-gradient(135deg, #f56c6c, #e6a23c);
  color: #fff;
}

.stats-section {
  background: #ffffff;
  border-radius: 12px;
  padding: 16px;
  margin: 0 0 24px;
  border: 1px solid rgba(0,0,0,0.05);
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
}

.mini-stats {
  display: flex;
  justify-content: space-around;
  margin-top: 8px;
}

.mini-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-num {
  font-size: 18px;
  font-weight: 600;
  color: #10a37f;
  font-family: 'Inter', sans-serif;
}

.stat-text {
  font-size: 11px;
  color: #8e8ea0;
}

.input-box:focus-within {
  border-color: #10a37f;
  box-shadow: 0 0 0 2px rgba(16, 163, 127, 0.1), 0 4px 12px rgba(0, 0, 0, 0.05);
  border-top: 1px solid rgba(0,0,0,0.05);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  margin-top: 4px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  overflow: hidden;
}

.user-info:hover {
  background: #ececec;
}

/* 主内容区 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  overflow: hidden;
  position: relative;
}

.main-header {
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 20px;
  background: #ffffff;
  border-bottom: 1px solid rgba(0,0,0,0.05);
  flex-shrink: 0;
}

/* 模型选择器优化 */
.model-selector {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  border-radius: 12px;
  color: #202123;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.model-selector:hover {
  background: rgba(0, 0, 0, 0.05);
}

.whut-badge {
  width: 28px;
  height: 28px;
  object-fit: contain;
  border-radius: 6px;
}

.model-selector-text {
  display: flex;
  align-items: center;
  gap: 6px;
}

.model-version {
  font-size: 14px;
  font-weight: 400;
  color: #8e8ea0;
  background: rgba(0,0,0,0.05);
  padding: 2px 6px;
  border-radius: 4px;
}

.dropdown-icon {
  font-size: 16px;
  color: #8e8ea0;
  margin-left: 4px;
  transition: transform 0.2s;
}

.model-selector:hover .dropdown-icon {
  color: #565869;
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
  margin-bottom: 12px;
  animation: fadeInDown 0.6s ease-out;
}

.welcome-subtitle {
  font-size: 16px;
  color: #666;
  margin-bottom: 40px;
  animation: fadeInDown 0.6s ease-out 0.1s both;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.suggestion-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  max-width: 680px;
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
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  animation: fadeInUp 0.5s ease-out both;
}

.suggestion-card:nth-child(1) { animation-delay: 0.1s; }
.suggestion-card:nth-child(2) { animation-delay: 0.15s; }
.suggestion-card:nth-child(3) { animation-delay: 0.2s; }
.suggestion-card:nth-child(4) { animation-delay: 0.25s; }
.suggestion-card:nth-child(5) { animation-delay: 0.3s; }
.suggestion-card:nth-child(6) { animation-delay: 0.35s; }

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(15px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.suggestion-card:hover {
  background: #f0f0f0;
  border-color: #667eea;
  color: #000;
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.suggestion-card .el-icon {
  opacity: 0.7;
  transition: all 0.3s ease;
}

.suggestion-card:hover .el-icon {
  opacity: 1;
  color: #667eea;
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
  animation: messageSlideIn 0.4s ease-out;
}

@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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

/* Markdown 样式增强 */
.message-content :deep(pre) {
  background: #f6f8fa;
  padding: 12px 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
  border: 1px solid #e5e5e5;
}

.message-content :deep(code) {
  font-family: 'Menlo', 'Monaco', 'Consolas', 'Courier New', monospace;
  font-size: 13px;
  color: #d63384;
}

.message-content :deep(pre code) {
  color: #333;
  background: transparent;
  padding: 0;
}

.message-content :deep(blockquote) {
  margin: 8px 0;
  padding-left: 12px;
  border-left: 4px solid #e5e5e5;
  color: #666;
}

.message-content :deep(ul), .message-content :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.message-content :deep(li) {
  margin: 4px 0;
}

.message-content :deep(h3) {
  font-size: 16px;
  font-weight: 600;
  margin: 16px 0 8px;
  color: #1f1f1f;
}

/* 欢迎卡片悬停效果增强 */
.suggestion-card {
  /* ... existing styles ... */
  transform: translateY(0);
}

.suggestion-card:hover {
  background: #ffffff;
  border-color: #10a37f; /* 绿色边框 */
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
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

/* 打字光标 - 流式输出时显示 */
.typing-cursor {
  display: inline-block;
  color: #10a37f;
  font-weight: bold;
  animation: blink 1s step-end infinite;
  margin-left: 2px;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
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

/* 展开侧边栏按钮 */
.show-sidebar-btn {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  color: #666;
  transition: all 0.2s;
}

.show-sidebar-btn:hover {
  background: rgba(0,0,0,0.05);
  color: #333;
}

.show-sidebar-btn .el-icon {
  font-size: 20px;
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

<style>
/* 全局下拉菜单样式 */
.model-dropdown-popper.el-popper {
  border-radius: 12px;
  border: 1px solid rgba(0,0,0,0.08);
  box-shadow: 0 10px 30px rgba(0,0,0,0.1);
  padding: 6px;
}

.model-dropdown-popper .el-dropdown-menu {
  padding: 0;
}

.model-dropdown-popper .el-dropdown-menu__item {
  padding: 10px 12px;
  border-radius: 8px;
  margin-bottom: 2px;
}

.model-dropdown-popper .el-dropdown-menu__item:last-child {
  margin-bottom: 0;
}

.model-dropdown-popper .el-dropdown-menu__item:hover {
  background-color: #f5f5f5;
}

.model-dropdown-popper .el-dropdown-menu__item.is-active {
  background-color: rgba(16, 163, 127, 0.08);
  color: #10a37f;
}

.model-option {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 220px;
}

.model-icon {
  font-size: 20px;
  color: #333;
  display: flex;
  align-items: center;
  justify-content: center;
}

.model-logo-img {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

.model-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.model-name {
  font-weight: 600;
  font-size: 14px;
  color: #333;
}

.model-desc {
  font-size: 12px;
  color: #8e8ea0;
}

.check-icon {
  font-size: 16px;
  color: #10a37f;
}
</style>
