import request from '@/utils/request'

/**
 * AI 助手 API 服务
 * 封装所有 AI 相关的接口调用
 */

/**
 * 发送聊天消息
 * @param {Object} data - 请求数据
 * @param {string} data.question - 用户问题
 * @param {string} [data.sessionId] - 会话ID（可选）
 * @returns {Promise<Object>} AI 回复
 */
export const sendChatMessage = (data) => {
  return request({
    url: '/ai/chat',
    method: 'post',
    data,
    timeout: 60000  // AI请求设置60秒超时
  })
}

/**
 * 获取对话历史
 * @param {string} sessionId - 会话ID
 * @returns {Promise<string>} 对话历史
 */
export const getChatHistory = (sessionId) => {
  return request({
    url: `/ai/history/${sessionId}`,
    method: 'get'
  })
}

/**
 * AI 服务健康检查
 * @returns {Promise<string>} 健康状态
 */
export const checkAiHealth = () => {
  return request({
    url: '/ai/health',
    method: 'get'
  })
}

/**
 * 流式聊天 - 实现类ChatGPT打字机效果
 * 使用 Server-Sent Events (SSE) 接收流式响应
 * 
 * @param {Object} data - 请求数据
 * @param {string} data.question - 用户问题
 * @param {string} [data.sessionId] - 会话ID
 * @param {string} [data.model] - 模型类型
 * @param {Function} onMessage - 收到消息片段时的回调 (content: string)
 * @param {Function} onError - 发生错误时的回调 (error: Error)
 * @param {Function} onComplete - 流式传输完成的回调 (sessionId: string)
 */
// ============ 聊天历史记录相关 API ============

/**
 * 获取用户的会话列表
 * @returns {Promise<Array>} 会话列表
 */
export const getChatSessions = () => {
  return request({
    url: '/ai/history/sessions',
    method: 'get'
  })
}

/**
 * 获取会话详情（包含消息）
 * @param {number} sessionId - 会话ID
 * @returns {Promise<Object>} 会话详情
 */
export const getChatSessionDetail = (sessionId) => {
  return request({
    url: `/ai/history/sessions/${sessionId}`,
    method: 'get'
  })
}

/**
 * 创建新会话
 * @param {Object} data - 请求数据
 * @param {string} data.model - 模型类型
 * @param {string} data.firstMessage - 第一条消息
 * @returns {Promise<Object>} 新会话信息
 */
export const createChatSession = (data) => {
  return request({
    url: '/ai/history/sessions',
    method: 'post',
    data
  })
}

/**
 * 保存消息到会话
 * @param {number} sessionId - 会话ID
 * @param {Object} data - 消息数据
 * @param {string} data.role - 角色 (user/assistant)
 * @param {string} data.content - 消息内容
 * @returns {Promise<void>}
 */
export const saveChatMessage = (sessionId, data) => {
  return request({
    url: `/ai/history/sessions/${sessionId}/messages`,
    method: 'post',
    data
  })
}

/**
 * 删除会话
 * @param {number} sessionId - 会话ID
 * @returns {Promise<void>}
 */
export const deleteChatSession = (sessionId) => {
  return request({
    url: `/ai/history/sessions/${sessionId}`,
    method: 'delete'
  })
}

/**
 * 更新会话标题
 * @param {number} sessionId - 会话ID
 * @param {string} title - 新标题
 * @returns {Promise<void>}
 */
export const updateChatSessionTitle = (sessionId, title) => {
  return request({
    url: `/ai/history/sessions/${sessionId}/title`,
    method: 'put',
    data: { title }
  })
}

// ============ Agent相关 API ============

/**
 * 执行Agent任务
 * @param {Object} data - 请求数据
 * @param {string} data.message - 任务描述
 * @param {string} [data.sessionId] - 会话ID
 * @returns {Promise<Object>} Agent执行结果
 */
export const executeAgentTask = (data) => {
  return request({
    url: '/ai/agent/execute',
    method: 'post',
    data,
    timeout: 120000  // Agent任务可能需要更长时间
  })
}

/**
 * 流式执行Agent任务
 * @param {Object} data - 请求数据
 * @param {Function} onEvent - 事件回调函数
 * @returns {EventSource} 事件源对象
 */
export const executeAgentTaskStream = (data, onEvent) => {
  const token = localStorage.getItem('token')
  const baseURL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'
  
  // 创建EventSource连接
  const eventSource = new EventSource(
    `${baseURL}/ai/agent/execute/stream?token=${token}`,
    { withCredentials: true }
  )
  
  // 监听各种事件
  eventSource.addEventListener('start', (event) => {
    const data = JSON.parse(event.data)
    onEvent('start', data)
  })
  
  eventSource.addEventListener('step', (event) => {
    const data = JSON.parse(event.data)
    onEvent('step', data)
  })
  
  eventSource.addEventListener('result', (event) => {
    const data = JSON.parse(event.data)
    onEvent('result', data)
  })
  
  eventSource.addEventListener('error', (event) => {
    const data = JSON.parse(event.data)
    onEvent('error', data)
    eventSource.close()
  })
  
  eventSource.addEventListener('complete', (event) => {
    const data = JSON.parse(event.data)
    onEvent('complete', data)
    eventSource.close()
  })
  
  return eventSource
}

/**
 * 获取Agent可用工具列表
 * @returns {Promise<Array>} 工具列表
 */
export const getAgentTools = () => {
  return request({
    url: '/ai/agent/tools',
    method: 'get'
  })
}

/**
 * 获取Agent能力介绍
 * @returns {Promise<Object>} 能力说明
 */
export const getAgentCapabilities = () => {
  return request({
    url: '/ai/agent/capabilities',
    method: 'get'
  })
}

/**
 * 清除Agent会话
 * @param {string} sessionId - 会话ID
 * @returns {Promise<void>}
 */
export const clearAgentSession = (sessionId) => {
  return request({
    url: `/ai/agent/session/${sessionId}`,
    method: 'delete'
  })
}

export const streamChat = async (data, onMessage, onError, onComplete) => {
  let reader = null
  
  try {
    const token = localStorage.getItem('token')
    const response = await fetch('/api/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        'Accept': 'text/event-stream'
      },
      body: JSON.stringify(data)
    })

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`请求失败: ${response.status} ${errorText}`)
    }

    reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      
      if (done) {
        break
      }
      
      // 解码数据块
      buffer += decoder.decode(value, { stream: true })
      
      // 按行处理 SSE 数据
      const lines = buffer.split('\n')
      buffer = lines.pop() || '' // 保留不完整的行
      
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const jsonStr = line.slice(5).trim()
          if (!jsonStr) continue
          
          try {
            const data = JSON.parse(jsonStr)
            
            if (data.error) {
              // AI服务返回错误
              onError && onError(new Error(data.content || 'AI服务错误'))
              return
            }
            
            if (data.done) {
              // 流式传输完成
              onComplete && onComplete(data.sessionId)
              return
            }
            
            if (data.content) {
              // 收到消息片段，触发回调
              onMessage && onMessage(data.content)
            }
          } catch (e) {
            console.debug('解析SSE数据失败:', jsonStr)
          }
        }
      }
    }
    
    // 正常结束
    onComplete && onComplete()
    
  } catch (error) {
    console.error('流式聊天错误:', error)
    onError && onError(error)
  } finally {
    if (reader) {
      try {
        reader.releaseLock()
      } catch (e) {
        // ignore
      }
    }
  }
}

// ============ 增强AI功能相关 API ============

/**
 * 生成文章摘要
 * @param {Object} data - 请求数据
 * @param {string} data.content - 文章内容
 * @param {number} [data.length] - 摘要长度
 * @param {string} [data.style] - 摘要风格
 * @returns {Promise<Object>} 摘要结果
 */
export const generateSummary = (data) => {
  return request({
    url: '/ai/enhanced/summary',
    method: 'post',
    data,
    timeout: 30000
  })
}

/**
 * 文本情感分析
 * @param {Object} data - 请求数据
 * @param {string} data.text - 待分析文本
 * @returns {Promise<Object>} 情感分析结果
 */
export const analyzeSentiment = (data) => {
  return request({
    url: '/ai/enhanced/sentiment',
    method: 'post',
    data,
    timeout: 30000
  })
}

/**
 * 批量生成摘要
 * @param {Object} data - 请求数据
 * @param {Array<string>} data.articles - 文章列表
 * @param {number} [data.length] - 摘要长度
 * @returns {Promise<Array>} 批量摘要结果
 */
export const batchGenerateSummaries = (data) => {
  return request({
    url: '/ai/enhanced/summary/batch',
    method: 'post',
    data,
    timeout: 60000
  })
}

/**
 * 生成标题建议
 * @param {Object} data - 请求数据
 * @param {string} data.content - 文章内容
 * @param {number} [data.count] - 生成数量
 * @returns {Promise<Array<string>>} 标题建议列表
 */
export const generateTitles = (data) => {
  return request({
    url: '/ai/enhanced/titles',
    method: 'post',
    data,
    timeout: 30000
  })
}

/**
 * 文章质量评估
 * @param {Object} data - 请求数据
 * @param {string} data.content - 文章内容
 * @returns {Promise<Object>} 质量评估结果
 */
export const evaluateArticleQuality = (data) => {
  return request({
    url: '/ai/enhanced/quality',
    method: 'post',
    data,
    timeout: 30000
  })
}
