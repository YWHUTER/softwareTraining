<template>
  <div class="ai-assistant">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-section">
          <el-icon :size="32" class="ai-icon"><ChatDotRound /></el-icon>
          <div class="title-text">
            <h1>武理小助手</h1>
            <p>您的校园新闻智能问答助手</p>
          </div>
        </div>
        <div class="header-buttons">
          <el-button @click="$router.push('/ai-help')" :icon="QuestionFilled" round type="info">使用手册</el-button>
          <el-button @click="clearChat" :icon="Delete" round>清空对话</el-button>
        </div>
      </div>
    </div>

    <!-- 聊天区域 -->
    <div class="chat-container">
      <!-- 消息列表 -->
      <div class="messages-area" ref="messagesContainer">
        <!-- 欢迎消息 -->
        <div v-if="messages.length === 0" class="welcome-message">
          <el-icon :size="64" class="welcome-icon"><ChatLineRound /></el-icon>
          <h2>欢迎使用武理小助手</h2>
          <p>我可以帮助您了解校园新闻、解答系统使用问题、提供文章写作建议等。</p>
          <div class="quick-questions">
            <span class="label">快捷提问：</span>
            <el-button 
              v-for="q in quickQuestions" 
              :key="q" 
              size="small" 
              round
              @click="sendQuickQuestion(q)"
            >
              {{ q }}
            </el-button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div 
          v-for="(msg, index) in messages" 
          :key="index" 
          class="message-item"
          :class="msg.role"
        >
          <div class="message-avatar">
            <el-avatar v-if="msg.role === 'user'" :size="40" class="user-avatar">
              {{ userStore.user?.realName?.[0] || 'U' }}
            </el-avatar>
            <el-avatar v-else :size="40" class="ai-avatar">
              <el-icon><ChatDotRound /></el-icon>
            </el-avatar>
          </div>
          <div class="message-content">
            <div class="message-header">
              <span class="sender-name">{{ msg.role === 'user' ? '我' : '武理小助手' }}</span>
              <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
            </div>
            <div class="message-text" v-html="formatMessage(msg.content)"></div>
          </div>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="message-item assistant">
          <div class="message-avatar">
            <el-avatar :size="40" class="ai-avatar">
              <el-icon><ChatDotRound /></el-icon>
            </el-avatar>
          </div>
          <div class="message-content">
            <div class="message-header">
              <span class="sender-name">武理小助手</span>
            </div>
            <div class="loading-indicator">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="loading-text">正在思考...</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <div class="input-wrapper">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="1"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="输入您的问题..."
            @keydown.enter.exact.prevent="sendMessage"
            :disabled="loading"
            class="message-input"
          />
          <el-button 
            type="primary" 
            :icon="Promotion" 
            :loading="loading"
            :disabled="!inputMessage.trim() || loading"
            @click="sendMessage"
            class="send-btn"
            circle
          />
        </div>
        <div class="input-tips">
          <span>按 Enter 发送，Shift + Enter 换行</span>
          <span class="session-info" v-if="sessionId">会话ID: {{ sessionId.slice(0, 8) }}...</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { Delete, Promotion, QuestionFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { sendChatMessage } from '@/api/ai'

const userStore = useUserStore()
const messagesContainer = ref(null)

// 状态
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const sessionId = ref('')

// 快捷问题
const quickQuestions = [
  '搜索校园活动',
  '最热门的文章是什么？',
  '最新发布的文章',
  '帮我找讲座相关',
  '系统有多少篇文章？',
  '如何发布文章？'
]

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
      sessionId: sessionId.value || undefined
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
})
</script>

<style scoped>
.ai-assistant {
  max-width: 900px;
  margin: 0 auto;
  height: calc(100vh - 200px);
  display: flex;
  flex-direction: column;
}

/* 页面标题 */
.page-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 24px 30px;
  margin-bottom: 20px;
  color: white;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-buttons {
  display: flex;
  gap: 10px;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.ai-icon {
  background: rgba(255, 255, 255, 0.2);
  padding: 12px;
  border-radius: 12px;
}

.title-text h1 {
  margin: 0 0 4px;
  font-size: 24px;
  font-weight: 700;
}

.title-text p {
  margin: 0;
  opacity: 0.9;
  font-size: 14px;
}

/* 聊天容器 */
.chat-container {
  flex: 1;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 消息区域 */
.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  scroll-behavior: smooth;
}

/* 欢迎消息 */
.welcome-message {
  text-align: center;
  padding: 60px 20px;
  color: #606266;
}

.welcome-icon {
  color: #667eea;
  margin-bottom: 20px;
}

.welcome-message h2 {
  margin: 0 0 12px;
  color: #2c3e50;
  font-size: 24px;
}

.welcome-message p {
  margin: 0 0 24px;
  font-size: 15px;
  line-height: 1.6;
}

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  align-items: center;
}

.quick-questions .label {
  font-size: 14px;
  color: #909399;
}

/* 消息项 */
.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.user-avatar {
  background: linear-gradient(135deg, #2196f3, #1976d2);
  color: white;
  font-weight: 600;
}

.ai-avatar {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.message-content {
  max-width: 70%;
}

.message-item.user .message-content {
  text-align: right;
}

.message-header {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 6px;
}

.message-item.user .message-header {
  flex-direction: row-reverse;
}

.sender-name {
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
}

.message-time {
  font-size: 12px;
  color: #909399;
}

.message-text {
  background: #f5f7fa;
  padding: 14px 18px;
  border-radius: 16px;
  line-height: 1.7;
  font-size: 15px;
  color: #303133;
  text-align: left;
  word-break: break-word;
}

.message-item.user .message-text {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-radius: 16px 16px 4px 16px;
}

.message-item.assistant .message-text {
  border-radius: 16px 16px 16px 4px;
}

.message-text :deep(strong) {
  font-weight: 600;
}

.message-text :deep(.list-number) {
  color: #667eea;
  font-weight: 600;
}

.message-text :deep(.article-link) {
  display: inline-block;
  color: #667eea;
  text-decoration: none;
  padding: 4px 12px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
  border-radius: 16px;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.3s ease;
  margin-left: 4px;
}

.message-text :deep(.article-link:hover) {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.2), rgba(118, 75, 162, 0.2));
  transform: translateY(-1px);
}

.message-text :deep(.article-title) {
  color: #2c3e50;
}

.message-item.assistant .message-text :deep(.article-title) {
  color: #409eff;
}

/* 加载指示器 */
.loading-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 14px 18px;
  background: #f5f7fa;
  border-radius: 16px 16px 16px 4px;
}

.dot {
  width: 8px;
  height: 8px;
  background: #667eea;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }
.dot:nth-child(3) { animation-delay: 0; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); }
  40% { transform: scale(1); }
}

.loading-text {
  margin-left: 8px;
  color: #909399;
  font-size: 14px;
}

/* 输入区域 */
.input-area {
  padding: 20px 24px;
  border-top: 1px solid #e4e7ed;
  background: #fafafa;
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.message-input {
  flex: 1;
}

.message-input :deep(.el-textarea__inner) {
  border-radius: 20px;
  padding: 12px 20px;
  font-size: 15px;
  resize: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid #e4e7ed;
  transition: all 0.3s;
}

.message-input :deep(.el-textarea__inner:focus) {
  border-color: #667eea;
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.2);
}

.send-btn {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.send-btn:hover {
  transform: scale(1.05);
}

.send-btn:disabled {
  background: #c0c4cc;
  box-shadow: none;
}

.input-tips {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.session-info {
  font-family: monospace;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ai-assistant {
    height: calc(100vh - 160px);
  }
  
  .page-header {
    padding: 20px;
  }
  
  .title-text h1 {
    font-size: 20px;
  }
  
  .message-content {
    max-width: 85%;
  }
  
  .messages-area {
    padding: 16px;
  }
  
  .welcome-message {
    padding: 40px 16px;
  }
}
</style>
