# 📚 API 接口文档

## Base URL
```
开发环境: http://localhost:8080/api
生产环境: https://campus-news.whut.edu.cn/api
```

## 认证方式
所有需要认证的接口都需要在请求头中携带 Token：
```
Authorization: Bearer {token}
```

## 一、AI Agent 接口

### 1. 执行Agent任务
```http
POST /ai/agent/execute
Content-Type: application/json

Request:
{
  "message": "帮我搜索关于AI的文章并评论",
  "sessionId": "optional-session-id"
}

Response:
{
  "success": true,
  "result": "找到文章《AI造福计算机学生》并成功评论",
  "steps": [...],
  "executionTime": 2800
}
```

### 2. 流式执行Agent任务（SSE）
```http
GET /ai/agent/execute/stream?message={message}&sessionId={sessionId}
Accept: text/event-stream

Response (SSE):
data: {"type":"step","content":"理解意图"}
data: {"type":"step","content":"搜索文章"}
data: {"type":"result","content":"执行完成"}
```

### 3. 获取Agent工具列表
```http
GET /ai/agent/tools

Response:
{
  "tools": [
    {
      "name": "searchAndComment",
      "description": "根据关键词搜索文章并评论",
      "parameters": ["keywords", "comment"]
    }
  ]
}
```

### 4. 获取Agent能力介绍
```http
GET /ai/agent/capabilities

Response:
{
  "description": "我是智能Agent助手，可以帮您...",
  "capabilities": ["搜索文章", "发表评论", "批量点赞", ...]
}
```

## 二、AI 对话接口

### 1. 普通对话
```http
POST /ai/chat
Content-Type: application/json

Request:
{
  "question": "什么是人工智能？",
  "model": "kimi",
  "sessionId": "optional"
}

Response:
{
  "answer": "人工智能是...",
  "sessionId": "generated-session-id"
}
```

### 2. 流式对话（SSE）
```http
GET /ai/chat/stream?question={question}&model={model}
Accept: text/event-stream

Response (SSE):
data: 人
data: 工
data: 智
data: 能
data: [DONE]
```

### 3. 获取对话历史
```http
GET /ai/chat/sessions

Response:
{
  "sessions": [
    {
      "id": "session-1",
      "title": "AI讨论",
      "model": "kimi",
      "createdAt": "2024-01-01T10:00:00Z"
    }
  ]
}
```

## 三、文章管理接口

### 1. 获取文章列表
```http
GET /article/list?current=1&size=10&boardType=CAMPUS&sortBy=date

Response:
{
  "records": [
    {
      "id": 1,
      "title": "校园新闻标题",
      "summary": "摘要内容",
      "authorId": 1,
      "viewCount": 100,
      "likeCount": 20,
      "commentCount": 5
    }
  ],
  "total": 100,
  "current": 1,
  "size": 10
}
```

### 2. 获取文章详情
```http
GET /article/{id}

Response:
{
  "id": 1,
  "title": "文章标题",
  "content": "文章内容",
  "author": {...},
  "tags": ["AI", "科技"],
  "createdAt": "2024-01-01T10:00:00Z"
}
```

### 3. 发布文章
```http
POST /article/publish
Content-Type: application/json
Authorization: Bearer {token}

Request:
{
  "title": "新文章标题",
  "content": "文章内容",
  "summary": "摘要",
  "boardType": "CAMPUS",
  "coverImage": "https://..."
}

Response:
{
  "id": 123,
  "message": "发布成功"
}
```

### 4. 点赞文章
```http
POST /article/{id}/like
Authorization: Bearer {token}

Response:
{
  "liked": true,
  "likeCount": 21
}
```

### 5. 评论文章
```http
POST /article/{id}/comment
Content-Type: application/json
Authorization: Bearer {token}

Request:
{
  "content": "评论内容"
}

Response:
{
  "id": 456,
  "content": "评论内容",
  "createdAt": "2024-01-01T10:00:00Z"
}
```

### 6. 收藏文章
```http
POST /article/{id}/favorite
Authorization: Bearer {token}

Response:
{
  "favorited": true,
  "message": "收藏成功"
}
```

## 四、用户管理接口

### 1. 用户注册
```http
POST /user/register
Content-Type: application/json

Request:
{
  "username": "testuser",
  "password": "password123",
  "email": "test@whut.edu.cn"
}

Response:
{
  "id": 1,
  "username": "testuser",
  "token": "jwt-token"
}
```

### 2. 用户登录
```http
POST /user/login
Content-Type: application/json

Request:
{
  "username": "testuser",
  "password": "password123"
}

Response:
{
  "user": {
    "id": 1,
    "username": "testuser",
    "realName": "张三"
  },
  "token": "jwt-token"
}
```

### 3. 获取用户信息
```http
GET /user/{id}

Response:
{
  "id": 1,
  "username": "testuser",
  "realName": "张三",
  "followerCount": 100,
  "followingCount": 50,
  "articleCount": 20
}
```

### 4. 关注用户
```http
POST /user/{id}/follow
Authorization: Bearer {token}

Response:
{
  "following": true,
  "followerCount": 101
}
```

### 5. 获取关注列表
```http
GET /user/{id}/following?page=1&size=10

Response:
{
  "users": [
    {
      "id": 2,
      "username": "user2",
      "realName": "李四"
    }
  ],
  "total": 50
}
```

## 五、数据统计接口

### 1. 获取系统统计
```http
GET /stats/system

Response:
{
  "totalArticles": 1000,
  "totalUsers": 500,
  "totalViews": 50000,
  "todayArticles": 10,
  "todayComments": 100
}
```

### 2. 获取热门文章
```http
GET /stats/hot-articles?limit=10

Response:
{
  "articles": [
    {
      "id": 1,
      "title": "热门文章",
      "viewCount": 10000
    }
  ]
}
```

### 3. 获取用户排行
```http
GET /stats/user-ranking?type=followers&limit=10

Response:
{
  "users": [
    {
      "id": 1,
      "username": "popular_user",
      "followerCount": 1000
    }
  ]
}
```

## 六、标签管理接口

### 1. 获取所有标签
```http
GET /tags

Response:
{
  "tags": [
    {
      "id": 1,
      "name": "AI",
      "articleCount": 50
    }
  ]
}
```

### 2. 根据标签获取文章
```http
GET /tags/{tagName}/articles?page=1&size=10

Response:
{
  "articles": [...],
  "total": 50
}
```

## 七、通知接口

### 1. 获取通知列表
```http
GET /notifications?page=1&size=10
Authorization: Bearer {token}

Response:
{
  "notifications": [
    {
      "id": 1,
      "type": "like",
      "content": "用户XXX点赞了您的文章",
      "read": false,
      "createdAt": "2024-01-01T10:00:00Z"
    }
  ],
  "unreadCount": 5
}
```

### 2. 标记已读
```http
PUT /notifications/{id}/read
Authorization: Bearer {token}

Response:
{
  "success": true
}
```

## 八、增强AI功能接口

### 1. 生成摘要
```http
POST /ai/enhanced/summary
Content-Type: application/json

Request:
{
  "content": "长文本内容",
  "length": 200,
  "style": "professional"
}

Response:
{
  "summary": "生成的摘要",
  "keyPoints": ["要点1", "要点2"],
  "quality": 0.85
}
```

### 2. 情感分析
```http
POST /ai/enhanced/sentiment
Content-Type: application/json

Request:
{
  "text": "待分析文本"
}

Response:
{
  "sentiment": "positive",
  "score": 0.8,
  "emotions": {
    "joy": 0.7,
    "trust": 0.5,
    "sadness": 0.1
  }
}
```

## 错误码说明

| 状态码 | 说明 |
|-------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 429 | 请求过于频繁 |
| 500 | 服务器内部错误 |

## 通用响应格式

成功响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {...}
}
```

错误响应：
```json
{
  "code": 400,
  "message": "错误信息",
  "data": null
}
```

## 分页参数说明

| 参数 | 类型 | 说明 | 默认值 |
|-----|------|------|--------|
| current/page | int | 当前页码 | 1 |
| size/limit | int | 每页条数 | 10 |
| sortBy | string | 排序字段 | createdAt |
| sortOrder | string | 排序方式(asc/desc) | desc |

---

📝 **注意事项**：
1. 所有时间格式均为 ISO 8601 格式
2. 文件上传使用 multipart/form-data
3. 部分接口支持批量操作，使用数组传参
4. 建议使用 HTTPS 协议确保数据安全
