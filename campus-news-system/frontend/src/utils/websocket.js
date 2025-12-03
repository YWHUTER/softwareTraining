/**
 * WebSocket 实时通知工具
 * 管理与服务器的WebSocket连接，接收实时消息推送
 */

class NotificationWebSocket {
  constructor() {
    this.ws = null
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 3000
    this.heartbeatInterval = null
    this.listeners = new Map()
  }

  /**
   * 连接WebSocket服务器
   */
  connect() {
    const token = localStorage.getItem('token')
    if (!token) {
      console.log('未登录，不建立WebSocket连接')
      return
    }

    // 构建WebSocket URL（自动适配开发/生产环境）
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    
    // 自动判断API路径前缀
    // - 开发环境(Vite代理): 通过 /api 代理到后端
    // - 生产环境(Nginx): 同样通过 /api 代理到后端
    const wsUrl = `${protocol}//${host}/api/ws/notification?token=${token}`
    
    console.log('🔌 尝试连接WebSocket:', wsUrl)

    try {
      this.ws = new WebSocket(wsUrl)

      this.ws.onopen = () => {
        console.log('🔗 WebSocket连接成功')
        this.reconnectAttempts = 0
        this.startHeartbeat()
        this.emit('connected')
      }

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          console.log('📨 收到WebSocket消息:', data)
          
          // 触发对应类型的事件
          if (data.type) {
            this.emit(data.type, data)
          }
          // 触发通用消息事件
          this.emit('message', data)
        } catch (e) {
          console.debug('WebSocket消息解析失败:', event.data)
        }
      }

      this.ws.onclose = (event) => {
        console.log('🔌 WebSocket连接关闭:', event.code, event.reason)
        this.stopHeartbeat()
        this.emit('disconnected')
        
        // 尝试重连
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          this.reconnectAttempts++
          console.log(`尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`)
          setTimeout(() => this.connect(), this.reconnectDelay)
        }
      }

      this.ws.onerror = (error) => {
        console.error('WebSocket错误:', error)
        this.emit('error', error)
      }

    } catch (error) {
      console.error('WebSocket连接失败:', error)
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    this.stopHeartbeat()
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.reconnectAttempts = this.maxReconnectAttempts // 阻止重连
  }

  /**
   * 发送消息
   */
  send(data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(typeof data === 'string' ? data : JSON.stringify(data))
    }
  }

  /**
   * 启动心跳
   */
  startHeartbeat() {
    this.heartbeatInterval = setInterval(() => {
      this.send('ping')
    }, 30000) // 每30秒发送一次心跳
  }

  /**
   * 停止心跳
   */
  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
  }

  /**
   * 注册事件监听器
   * @param {string} event - 事件类型: LIKE, COMMENT, FOLLOW, FAVORITE, SYSTEM, message, connected, disconnected
   * @param {Function} callback - 回调函数
   */
  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, [])
    }
    this.listeners.get(event).push(callback)
  }

  /**
   * 移除事件监听器
   */
  off(event, callback) {
    if (this.listeners.has(event)) {
      const callbacks = this.listeners.get(event)
      const index = callbacks.indexOf(callback)
      if (index > -1) {
        callbacks.splice(index, 1)
      }
    }
  }

  /**
   * 触发事件
   */
  emit(event, data) {
    if (this.listeners.has(event)) {
      this.listeners.get(event).forEach(callback => {
        try {
          callback(data)
        } catch (e) {
          console.error('WebSocket事件处理错误:', e)
        }
      })
    }
  }

  /**
   * 获取连接状态
   */
  isConnected() {
    return this.ws && this.ws.readyState === WebSocket.OPEN
  }
}

// 单例模式
const notificationWS = new NotificationWebSocket()

export default notificationWS
