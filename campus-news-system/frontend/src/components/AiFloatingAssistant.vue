0<template>
  <div class="ai-floating-assistant">
    <!-- 悬浮按钮 -->
    <div 
      class="floating-btn"
      :class="{ 'is-open': isOpen }"
      @click="toggleChat"
    >
      <div class="btn-inner">
        <img v-if="!isOpen" src="@/assets/airobot.png" alt="AI" class="robot-icon" />
        <el-icon v-else :size="24"><Close /></el-icon>
      </div>
      <div class="btn-pulse"></div>
      <div class="btn-tooltip" v-if="!isOpen">AI 助手</div>
    </div>

    <!-- 聊天窗口 -->
    <transition name="chat-popup">
      <div 
        v-if="isOpen" 
        class="chat-window"
        :style="chatWindowStyle"
        ref="chatWindow"
      >
        <!-- 缩放手柄 -->
        <div class="resize-handle resize-n" @mousedown="startResize($event, 'n')"></div>
        <div class="resize-handle resize-s" @mousedown="startResize($event, 's')"></div>
        <div class="resize-handle resize-e" @mousedown="startResize($event, 'e')"></div>
        <div class="resize-handle resize-w" @mousedown="startResize($event, 'w')"></div>
        <div class="resize-handle resize-ne" @mousedown="startResize($event, 'ne')"></div>
        <div class="resize-handle resize-nw" @mousedown="startResize($event, 'nw')"></div>
        <div class="resize-handle resize-se" @mousedown="startResize($event, 'se')">
          <div class="resize-icon">⤡</div>
        </div>
        <div class="resize-handle resize-sw" @mousedown="startResize($event, 'sw')"></div>
        <!-- 头部 -->
        <div class="chat-header">
          <div class="header-left">
            <div class="ai-avatar">
              <img src="@/assets/airobot.png" alt="AI" class="header-robot-icon" />
            </div>
            <div class="header-info">
              <span class="header-title">WHUTGPT</span>
              <span class="header-status">
                <span class="status-dot"></span>
                在线
              </span>
            </div>
          </div>
          <div class="header-actions">
            <el-tooltip content="清空对话" placement="top">
              <el-button circle size="small" @click="clearChat">
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="最小化" placement="top">
              <el-button circle size="small" @click="isOpen = false">
                <el-icon><Minus /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="chat-messages" ref="messagesContainer">
          <!-- 欢迎消息 -->
          <div v-if="messages.length === 0" class="welcome-message">
            <div class="welcome-icon">
              <img src="@/assets/airobot.png" alt="AI" class="welcome-robot-icon" />
            </div>
            <h3>你好！我是 WHUTGPT</h3>
            <p>武汉理工大学校园新闻系统 AI 助手</p>
            <div class="quick-actions">
              <el-button 
                v-for="action in quickActions" 
                :key="action.text"
                size="small" 
                round
                @click="sendQuickMessage(action.text)"
              >
                {{ action.label }}
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
              <img v-if="msg.role === 'assistant'" src="@/assets/airobot.png" alt="AI" class="msg-robot-icon" />
              <el-icon v-else :size="18"><User /></el-icon>
            </div>
            <div class="message-content">
              <div class="message-bubble" v-html="formatMessage(msg.content)"></div>
              <div class="message-time">{{ formatTime(msg.time) }}</div>
            </div>
          </div>

          <!-- 加载中 -->
          <div v-if="isLoading" class="message-item assistant">
            <div class="message-avatar">
              <img src="@/assets/airobot.png" alt="AI" class="msg-robot-icon" />
            </div>
            <div class="message-content">
              <div class="message-bubble typing">
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="chat-input">
          <el-input
            v-model="inputText"
            placeholder="输入你的问题..."
            :disabled="isLoading"
            @keyup.enter="sendMessage"
          >
            <template #suffix>
              <el-button 
                type="primary" 
                circle 
                size="small"
                :disabled="!inputText.trim() || isLoading"
                @click="sendMessage"
              >
                <el-icon><Promotion /></el-icon>
              </el-button>
            </template>
          </el-input>
        </div>
        
        <!-- 底部缩放提示条 -->
        <div class="resize-bar" @mousedown="startResize($event, 'se')">
          <div class="resize-bar-dots">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted, computed } from 'vue'
import { Close, Delete, Minus, User, Promotion } from '@element-plus/icons-vue'
import { sendChatMessage } from '@/api/ai'
import { ElMessage } from 'element-plus'

const isOpen = ref(false)
const inputText = ref('')
const messages = ref([])
const isLoading = ref(false)
const messagesContainer = ref(null)
const sessionId = ref(null)
const chatWindow = ref(null)

// 窗口尺寸状态
const windowWidth = ref(380)
const windowHeight = ref(520)
const minWidth = 300
const maxWidth = 600
const minHeight = 400
const maxHeight = 800

// 缩放相关状态
const isResizing = ref(false)
const resizeDirection = ref('')
const startX = ref(0)
const startY = ref(0)
const startWidth = ref(0)
const startHeight = ref(0)

// 计算窗口样式
const chatWindowStyle = computed(() => ({
  width: `${windowWidth.value}px`,
  height: `${windowHeight.value}px`
}))

// 开始缩放
const startResize = (e, direction) => {
  e.preventDefault()
  isResizing.value = true
  resizeDirection.value = direction
  startX.value = e.clientX
  startY.value = e.clientY
  startWidth.value = windowWidth.value
  startHeight.value = windowHeight.value
  
  document.addEventListener('mousemove', handleResize)
  document.addEventListener('mouseup', stopResize)
  document.body.style.cursor = getCursorStyle(direction)
  document.body.style.userSelect = 'none'
}

// 获取光标样式
const getCursorStyle = (direction) => {
  const cursors = {
    'n': 'ns-resize',
    's': 'ns-resize',
    'e': 'ew-resize',
    'w': 'ew-resize',
    'ne': 'nesw-resize',
    'nw': 'nwse-resize',
    'se': 'nwse-resize',
    'sw': 'nesw-resize'
  }
  return cursors[direction] || 'default'
}

// 处理缩放
const handleResize = (e) => {
  if (!isResizing.value) return
  
  const deltaX = e.clientX - startX.value
  const deltaY = e.clientY - startY.value
  const dir = resizeDirection.value
  
  let newWidth = startWidth.value
  let newHeight = startHeight.value
  
  // 根据方向计算新尺寸
  if (dir.includes('e')) {
    newWidth = startWidth.value + deltaX
  }
  if (dir.includes('w')) {
    newWidth = startWidth.value - deltaX
  }
  if (dir.includes('s')) {
    newHeight = startHeight.value + deltaY
  }
  if (dir.includes('n')) {
    newHeight = startHeight.value - deltaY
  }
  
  // 限制尺寸范围
  windowWidth.value = Math.min(maxWidth, Math.max(minWidth, newWidth))
  windowHeight.value = Math.min(maxHeight, Math.max(minHeight, newHeight))
}

// 停止缩放
const stopResize = () => {
  isResizing.value = false
  resizeDirection.value = ''
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

// 组件卸载时清理事件监听
onUnmounted(() => {
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
})

// 快捷操作
const quickActions = [
  { label: '📰 今日热点', text: '今天有什么热门新闻？' },
  { label: '🎬 推荐视频', text: '给我推荐一些热门视频' },
  { label: '❓ 使用帮助', text: '这个系统有哪些功能？' }
]

// 切换聊天窗口
const toggleChat = () => {
  isOpen.value = !isOpen.value
}

// 发送消息
const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || isLoading.value) return

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: text,
    time: new Date()
  })
  inputText.value = ''
  scrollToBottom()

  // 调用 AI 接口
  isLoading.value = true
  try {
    const response = await sendChatMessage({
      question: text,
      sessionId: sessionId.value
    })
    
    // 保存会话ID
    if (response.sessionId) {
      sessionId.value = response.sessionId
    }

    // 添加 AI 回复
    messages.value.push({
      role: 'assistant',
      content: response.answer || response.content || '抱歉，我暂时无法回答这个问题。',
      time: new Date()
    })
  } catch (error) {
    console.error('AI 请求失败:', error)
    messages.value.push({
      role: 'assistant',
      content: '抱歉，服务暂时不可用，请稍后再试。',
      time: new Date()
    })
    ElMessage.error('AI 服务请求失败')
  } finally {
    isLoading.value = false
    scrollToBottom()
  }
}

// 发送快捷消息
const sendQuickMessage = (text) => {
  inputText.value = text
  sendMessage()
}

// 清空对话
const clearChat = () => {
  messages.value = []
  sessionId.value = null
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

// 格式化消息（丰富排版）
const formatMessage = (content) => {
  if (!content) return ''
  // 转义 HTML
  let text = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  
  // 处理代码块 ```code``` (先处理，避免内部被其他规则影响)
  text = text.replace(/```(\w*)\n?([\s\S]*?)```/g, (match, lang, code) => {
    return `<pre class="code-block"><code>${code.trim()}</code></pre>`
  })
  
  // 处理行内代码 `code`
  text = text.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
  
  // 处理粗体 **text**
  text = text.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  
  // 处理斜体 *text*
  text = text.replace(/\*([^*]+)\*/g, '<em>$1</em>')
  
  // 处理标题 ### 
  text = text.replace(/^### (.+)$/gm, '<h4 class="msg-h4">$1</h4>')
  text = text.replace(/^## (.+)$/gm, '<h3 class="msg-h3">$1</h3>')
  text = text.replace(/^# (.+)$/gm, '<h2 class="msg-h2">$1</h2>')
  
  // 处理无序列表 - item
  text = text.replace(/^[-•] (.+)$/gm, '<li class="msg-li">$1</li>')
  text = text.replace(/(<li class="msg-li">.*<\/li>\n?)+/g, '<ul class="msg-ul">$&</ul>')
  
  // 处理有序列表 1. item
  text = text.replace(/^\d+\. (.+)$/gm, '<li class="msg-oli">$1</li>')
  text = text.replace(/(<li class="msg-oli">.*<\/li>\n?)+/g, '<ol class="msg-ol">$&</ol>')
  
  // 处理引用 > text
  text = text.replace(/^&gt; (.+)$/gm, '<blockquote class="msg-quote">$1</blockquote>')
  
  // 处理分隔线 ---
  text = text.replace(/^---$/gm, '<hr class="msg-hr">')
  
  // 处理换行（在其他处理之后）
  text = text.replace(/\n/g, '<br>')
  
  // 清理多余的 <br>
  text = text.replace(/<br><br>/g, '</p><p class="msg-p">')
  text = `<p class="msg-p">${text}</p>`
  text = text.replace(/<p class="msg-p"><\/p>/g, '')
  text = text.replace(/<p class="msg-p"><br>/g, '<p class="msg-p">')
  
  return text
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>


<style scoped>
.ai-floating-assistant {
  position: fixed;
  right: 24px;
  top: 35%;
  z-index: 9999;
}

/* 悬浮按钮 */
.floating-btn {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.floating-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 30px rgba(102, 126, 234, 0.5);
}

.floating-btn.is-open {
  background: linear-gradient(135deg, #f56c6c 0%, #e74c3c 100%);
  box-shadow: 0 4px 20px rgba(245, 108, 108, 0.4);
}

.btn-inner {
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
}

.robot-icon {
  width: 50px;
  height: 50px;
  object-fit: contain;
}

.header-robot-icon {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.welcome-robot-icon {
  width: 72px;
  height: 72px;
  object-fit: contain;
}

.msg-robot-icon {
  width: 26px;
  height: 26px;
  object-fit: contain;
}

.btn-pulse {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  animation: pulse 2s infinite;
  z-index: 1;
}

.floating-btn.is-open .btn-pulse {
  display: none;
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 0.5;
  }
  50% {
    transform: scale(1.3);
    opacity: 0;
  }
  100% {
    transform: scale(1);
    opacity: 0;
  }
}

.btn-tooltip {
  position: absolute;
  right: 70px;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 13px;
  white-space: nowrap;
  opacity: 0;
  transform: translateX(10px);
  transition: all 0.3s ease;
  pointer-events: none;
}

.floating-btn:hover .btn-tooltip {
  opacity: 1;
  transform: translateX(0);
}

/* 聊天窗口 */
.chat-window {
  position: absolute;
  right: 0;
  top: 70px;
  width: 380px;
  height: 520px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  box-shadow: 0 10px 50px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.5);
}

/* 缩放手柄样式 */
.resize-handle {
  position: absolute;
  z-index: 10;
}

.resize-n {
  top: 0;
  left: 10px;
  right: 10px;
  height: 6px;
  cursor: ns-resize;
}

.resize-s {
  bottom: 0;
  left: 10px;
  right: 10px;
  height: 6px;
  cursor: ns-resize;
}

.resize-e {
  right: 0;
  top: 10px;
  bottom: 10px;
  width: 6px;
  cursor: ew-resize;
}

.resize-w {
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 6px;
  cursor: ew-resize;
}

.resize-ne {
  top: 0;
  right: 0;
  width: 12px;
  height: 12px;
  cursor: nesw-resize;
}

.resize-nw {
  top: 0;
  left: 0;
  width: 12px;
  height: 12px;
  cursor: nwse-resize;
}

.resize-se {
  bottom: 0;
  right: 0;
  width: 20px;
  height: 20px;
  cursor: nwse-resize;
  display: flex;
  align-items: center;
  justify-content: center;
}

.resize-icon {
  font-size: 14px;
  color: #909399;
  transform: rotate(90deg);
  pointer-events: none;
}

.resize-se:hover .resize-icon {
  color: #667eea;
}

.resize-sw {
  bottom: 0;
  left: 0;
  width: 12px;
  height: 12px;
  cursor: nesw-resize;
}

/* 悬停时显示缩放提示 */
.resize-handle:hover {
  background: rgba(102, 126, 234, 0.1);
}

/* 聊天窗口动画 */
.chat-popup-enter-active,
.chat-popup-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.chat-popup-enter-from,
.chat-popup-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}

/* 头部 */
.chat-header {
  padding: 16px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.header-title {
  color: white;
  font-weight: 600;
  font-size: 15px;
}

.header-status {
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4ade80;
  animation: blink 2s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.header-actions {
  display: flex;
  gap: 8px;
}

.header-actions .el-button {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
}

.header-actions .el-button:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 消息列表 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 欢迎消息 */
.welcome-message {
  text-align: center;
  padding: 30px 20px;
}

.welcome-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 16px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #667eea;
}

.welcome-message h3 {
  margin: 0 0 8px;
  color: #2c3e50;
  font-size: 18px;
}

.welcome-message p {
  margin: 0 0 20px;
  color: #909399;
  font-size: 14px;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.quick-actions .el-button {
  font-size: 12px;
}

/* 消息项 */
.message-item {
  display: flex;
  gap: 10px;
  max-width: 85%;
}

.message-item.user {
  flex-direction: row-reverse;
  align-self: flex-end;
}

.message-item.assistant {
  align-self: flex-start;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message-item.assistant .message-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.message-item.user .message-avatar {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.message-item.assistant .message-bubble {
  background: #f5f7fa;
  color: #2c3e50;
  border-bottom-left-radius: 4px;
}

.message-item.user .message-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-right-radius: 4px;
}

.message-bubble :deep(p) {
  margin: 0 0 8px;
}

.message-bubble :deep(p:last-child) {
  margin-bottom: 0;
}

.message-bubble :deep(code) {
  background: rgba(0, 0, 0, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

.message-bubble :deep(pre) {
  background: rgba(0, 0, 0, 0.05);
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
}

/* 丰富的消息排版样式 */
.message-bubble :deep(.msg-p) {
  margin: 0 0 10px;
  line-height: 1.7;
}

.message-bubble :deep(.msg-p:last-child) {
  margin-bottom: 0;
}

.message-bubble :deep(.msg-h2) {
  font-size: 16px;
  font-weight: 600;
  margin: 12px 0 8px;
  color: inherit;
}

.message-bubble :deep(.msg-h3) {
  font-size: 15px;
  font-weight: 600;
  margin: 10px 0 6px;
  color: inherit;
}

.message-bubble :deep(.msg-h4) {
  font-size: 14px;
  font-weight: 600;
  margin: 8px 0 4px;
  color: inherit;
}

.message-bubble :deep(.msg-ul),
.message-bubble :deep(.msg-ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.message-bubble :deep(.msg-li),
.message-bubble :deep(.msg-oli) {
  margin: 4px 0;
  line-height: 1.6;
}

.message-bubble :deep(.msg-quote) {
  margin: 8px 0;
  padding: 8px 12px;
  border-left: 3px solid rgba(102, 126, 234, 0.5);
  background: rgba(102, 126, 234, 0.08);
  border-radius: 0 8px 8px 0;
  font-style: italic;
}

.message-bubble :deep(.msg-hr) {
  margin: 12px 0;
  border: none;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
}

.message-bubble :deep(.code-block) {
  background: rgba(0, 0, 0, 0.06);
  padding: 12px 14px;
  border-radius: 8px;
  margin: 8px 0;
  overflow-x: auto;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.5;
}

.message-bubble :deep(.inline-code) {
  background: rgba(102, 126, 234, 0.15);
  color: #667eea;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
}

.message-item.user .message-bubble :deep(.inline-code) {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.message-bubble :deep(strong) {
  font-weight: 600;
}

.message-bubble :deep(em) {
  font-style: italic;
}

.message-time {
  font-size: 11px;
  color: #909399;
  padding: 0 4px;
}

.message-item.user .message-time {
  text-align: right;
}

/* 打字动画 */
.message-bubble.typing {
  display: flex;
  gap: 4px;
  padding: 16px 20px;
}

.message-bubble.typing .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #909399;
  animation: typing 1.4s infinite;
}

.message-bubble.typing .dot:nth-child(2) {
  animation-delay: 0.2s;
}

.message-bubble.typing .dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-8px);
    opacity: 1;
  }
}

/* 输入区域 */
.chat-input {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  background: white;
}

.chat-input :deep(.el-input__wrapper) {
  border-radius: 24px;
  padding: 8px 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.chat-input :deep(.el-input__suffix) {
  padding-right: 4px;
}

/* 底部缩放提示条 */
.resize-bar {
  height: 16px;
  background: linear-gradient(to top, rgba(102, 126, 234, 0.05), transparent);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: nwse-resize;
  border-bottom-left-radius: 20px;
  border-bottom-right-radius: 20px;
  transition: background 0.2s;
}

.resize-bar:hover {
  background: linear-gradient(to top, rgba(102, 126, 234, 0.15), transparent);
}

.resize-bar-dots {
  display: flex;
  gap: 3px;
}

.resize-bar-dots span {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #c0c4cc;
  transition: background 0.2s;
}

.resize-bar:hover .resize-bar-dots span {
  background: #667eea;
}

/* 暗黑模式适配 */
:root.dark .chat-window,
[data-theme="dark"] .chat-window {
  background: rgba(30, 41, 59, 0.95);
  border-color: rgba(51, 65, 85, 0.5);
}

:root.dark .welcome-message h3,
[data-theme="dark"] .welcome-message h3 {
  color: #f1f5f9;
}

:root.dark .welcome-message p,
[data-theme="dark"] .welcome-message p {
  color: #94a3b8;
}

:root.dark .message-item.assistant .message-bubble,
[data-theme="dark"] .message-item.assistant .message-bubble {
  background: #334155;
  color: #f1f5f9;
}

:root.dark .chat-input,
[data-theme="dark"] .chat-input {
  background: #1e293b;
  border-top-color: #334155;
}

:root.dark .chat-input :deep(.el-input__wrapper),
[data-theme="dark"] .chat-input :deep(.el-input__wrapper) {
  background: #334155;
  box-shadow: none;
}

:root.dark .chat-input :deep(.el-input__inner),
[data-theme="dark"] .chat-input :deep(.el-input__inner) {
  color: #f1f5f9;
}

/* 响应式 */
@media (max-width: 480px) {
  .chat-window {
    width: calc(100vw - 48px);
    height: 70vh;
    right: -12px;
  }
}
</style>
