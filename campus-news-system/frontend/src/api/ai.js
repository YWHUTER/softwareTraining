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
    data
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
 * 流式聊天（预留接口，后续实现 SSE）
 * 当后端支持流式输出时使用
 * 
 * @param {Object} data - 请求数据
 * @param {Function} onMessage - 消息回调
 * @param {Function} onError - 错误回调
 * @param {Function} onComplete - 完成回调
 */
export const streamChat = async (data, onMessage, onError, onComplete) => {
  try {
    const token = localStorage.getItem('token')
    const response = await fetch('/api/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      body: JSON.stringify(data)
    })

    if (!response.ok) {
      throw new Error('Stream request failed')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()

    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        onComplete && onComplete()
        break
      }
      
      const text = decoder.decode(value, { stream: true })
      onMessage && onMessage(text)
    }
  } catch (error) {
    onError && onError(error)
  }
}
