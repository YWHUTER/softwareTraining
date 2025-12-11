# 校园新闻系统 API 文档

> 版本: 1.0.0  
> 更新时间: 2025-12-11

## 目录

- [概述](#概述)
- [后端 API (Spring Boot)](#后端-api-spring-boot)
  - [认证接口](#认证接口)
  - [用户接口](#用户接口)
  - [文章接口](#文章接口)
  - [评论接口](#评论接口)
  - [关注接口](#关注接口)
  - [通知接口](#通知接口)
  - [标签接口](#标签接口)
  - [学院接口](#学院接口)
  - [文件上传接口](#文件上传接口)
  - [智能推荐接口](#智能推荐接口)
  - [AI助手接口](#ai助手接口)
  - [AI增强功能接口](#ai增强功能接口)
  - [AI Agent接口](#ai-agent接口)
  - [聊天历史接口](#聊天历史接口)
  - [管理后台接口](#管理后台接口)
- [算法服务 API (FastAPI)](#算法服务-api-fastapi)
  - [推荐接口](#推荐接口)
  - [用户画像接口](#用户画像接口)
  - [管理接口](#管理接口)

---

## 概述

### 服务地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 后端服务 | `http://localhost:8080/api` | Spring Boot 主服务 |
| 算法服务 | `http://localhost:5000` | Python FastAPI 推荐服务 |
| 前端服务 | `http://localhost:5173` | Vue3 前端开发服务 |

### 通用响应格式

#### 后端服务响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 状态码，200成功，其他为错误 |
| message | String | 响应消息 |
| data | Object | 响应数据 |

#### 算法服务响应格式
```json
{
  "success": true,
  "data": [ ... ],
  "message": "操作成功"
}
```

### 认证方式

后端服务使用 JWT Token 认证，需要在请求头中携带：
```
Authorization: Bearer <token>
```

---

## 后端 API (Spring Boot)

基础路径: `/api`

---

### 认证接口

#### 用户登录
```
POST /api/auth/login
```

**请求体:**
```json
{
  "username": "string",  // 必填，用户名
  "password": "string"   // 必填，密码
}
```

**响应:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "admin",
      "realName": "管理员",
      "avatar": "/api/file/image/xxx.jpg",
      "roles": ["ADMIN"]
    }
  }
}
```

---

#### 用户注册
```
POST /api/auth/register
```

**请求体:**
```json
{
  "username": "string",   // 必填，用户名
  "password": "string",   // 必填，密码
  "email": "string",      // 邮箱
  "realName": "string",   // 必填，真实姓名
  "phone": "string",      // 手机号
  "roleId": 1,            // 必填，角色ID
  "collegeId": 1,         // 学院ID
  "studentId": "string"   // 学号
}
```

---

### 用户接口

#### 获取当前用户信息
```
GET /api/user/info
```
**需要认证:** ✅

**响应:**
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "admin",
    "realName": "管理员",
    "email": "admin@example.com",
    "phone": "13800138000",
    "avatar": "/api/file/image/xxx.jpg",
    "collegeId": 1,
    "collegeName": "计算机学院",
    "status": 1,
    "roles": ["ADMIN"],
    "createdAt": "2025-01-01T00:00:00"
  }
}
```

---

#### 获取用户列表 (管理员)
```
GET /api/user/list
```
**需要认证:** ✅ (ADMIN)

**查询参数:**
| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| current | Long | 否 | 1 | 当前页码 |
| size | Long | 否 | 10 | 每页数量 |
| keyword | String | 否 | - | 搜索关键词 |
| collegeId | Long | 否 | - | 学院ID筛选 |
| status | Integer | 否 | - | 状态筛选 |

---

#### 更新用户状态 (管理员)
```
PUT /api/user/status/{userId}?status=1
```
**需要认证:** ✅ (ADMIN)

---

#### 搜索用户 (用于@提及)
```
GET /api/user/search?keyword=xxx
```
**需要认证:** ✅

---

#### 更新用户头像
```
PUT /api/user/avatar?avatar=/api/file/image/xxx.jpg
```
**需要认证:** ✅

---

#### 更新用户信息
```
PUT /api/user/update
```
**需要认证:** ✅

**请求体:**
```json
{
  "realName": "string",
  "email": "string",
  "phone": "string"
}
```

---

### 文章接口

#### 创建文章
```
POST /api/article/create
```
**需要认证:** ✅

**请求体:**
```json
{
  "title": "string",       // 必填，标题
  "content": "string",     // 必填，内容(HTML)
  "summary": "string",     // 摘要
  "coverImage": "string",  // 封面图URL
  "boardType": "CAMPUS",   // 必填，板块类型: OFFICIAL/CAMPUS/COLLEGE
  "collegeId": 1,          // 学院ID(学院新闻必填)
  "isPinned": 0,           // 是否置顶
  "tags": ["标签1", "标签2"] // 文章标签
}
```

---

#### 获取文章列表
```
GET /api/article/list
```

**查询参数:**
| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| current | Long | 否 | 1 | 当前页码 |
| size | Long | 否 | 10 | 每页数量 |
| keyword | String | 否 | - | 搜索关键词 |
| boardType | String | 否 | - | 板块类型 |
| collegeId | Long | 否 | - | 学院ID |
| authorId | Long | 否 | - | 作者ID |
| isApproved | Integer | 否 | 1 | 审核状态 |
| orderBy | String | 否 | createdAt | 排序字段 |

**响应:**
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "title": "文章标题",
        "summary": "文章摘要",
        "coverImage": "/api/file/image/xxx.jpg",
        "boardType": "CAMPUS",
        "authorId": 1,
        "authorName": "作者名",
        "authorAvatar": "/api/file/image/xxx.jpg",
        "viewCount": 100,
        "likeCount": 10,
        "commentCount": 5,
        "isLiked": false,
        "isFavorited": false,
        "isPinned": 0,
        "isApproved": 1,
        "createdAt": "2025-01-01T00:00:00",
        "tags": [{"id": 1, "name": "标签1"}]
      }
    ],
    "total": 100,
    "current": 1,
    "size": 10
  }
}
```

---

#### 获取文章详情
```
GET /api/article/detail/{id}
```

---

#### 更新文章
```
PUT /api/article/update/{id}
```
**需要认证:** ✅

---

#### 删除文章
```
DELETE /api/article/delete/{id}
```
**需要认证:** ✅

---

#### 点赞/取消点赞
```
POST /api/article/like/{id}
```
**需要认证:** ✅

**响应:** `true` 表示已点赞，`false` 表示已取消

---

#### 收藏/取消收藏
```
POST /api/article/favorite/{id}
```
**需要认证:** ✅

---

#### 置顶文章 (管理员)
```
PUT /api/article/pin/{id}?isPinned=1
```
**需要认证:** ✅ (ADMIN)

---

#### 审核文章 (管理员)
```
PUT /api/article/approve/{id}?isApproved=1
```
**需要认证:** ✅ (ADMIN)

| isApproved | 说明 |
|------------|------|
| 0 | 待审核 |
| 1 | 已通过 |
| 2 | 已拒绝 |

---

#### 获取公开统计数据
```
GET /api/article/public/stats
```

---

### 评论接口

#### 创建评论
```
POST /api/comment/create
```
**需要认证:** ✅

**请求体:**
```json
{
  "articleId": 1,      // 必填，文章ID
  "content": "string", // 必填，评论内容
  "parentId": null,    // 父评论ID(回复时填写)
  "replyToUserId": null // 回复的用户ID
}
```

---

#### 获取评论列表
```
GET /api/comment/list?articleId=1
```

**响应:** 返回树形结构的评论列表

---

#### 删除评论
```
DELETE /api/comment/delete/{id}
```
**需要认证:** ✅

---

#### 获取评论历史
```
GET /api/comment/history
```
**需要认证:** ✅

**查询参数:**
| 参数 | 类型 | 说明 |
|------|------|------|
| type | String | received(收到的)/sent(发出的) |
| current | Long | 页码 |
| size | Long | 每页数量 |

---

### 关注接口

#### 关注/取消关注用户
```
POST /api/follow/{userId}
```
**需要认证:** ✅

**响应:**
```json
{
  "code": 200,
  "data": {
    "isFollowing": true,
    "message": "关注成功"
  }
}
```

---

#### 检查是否已关注
```
GET /api/follow/check/{userId}
```
**需要认证:** ✅

---

#### 获取我的关注列表
```
GET /api/follow/following?current=1&size=10
```
**需要认证:** ✅

---

#### 获取指定用户的关注列表
```
GET /api/follow/following/{userId}?current=1&size=10
```

---

#### 获取我的粉丝列表
```
GET /api/follow/followers?current=1&size=10
```
**需要认证:** ✅

---

#### 获取指定用户的粉丝列表
```
GET /api/follow/followers/{userId}?current=1&size=10
```

---

#### 获取关注动态
```
GET /api/follow/feed?current=1&size=10
```
**需要认证:** ✅

---

#### 获取推荐关注用户
```
GET /api/follow/recommend?limit=5
```
**需要认证:** ✅

---

#### 获取用户统计信息
```
GET /api/follow/stats/{userId}
```

**响应:**
```json
{
  "code": 200,
  "data": {
    "followingCount": 10,
    "followerCount": 20,
    "articleCount": 5
  }
}
```

---

### 通知接口

#### 获取通知列表
```
GET /api/notification/list?current=1&size=10
```
**需要认证:** ✅

---

#### 获取未读通知数量
```
GET /api/notification/unread-count
```
**需要认证:** ✅

**响应:**
```json
{
  "code": 200,
  "data": {
    "count": 5
  }
}
```

---

#### 标记单个通知为已读
```
PUT /api/notification/read/{id}
```
**需要认证:** ✅

---

#### 标记所有通知为已读
```
PUT /api/notification/read-all
```
**需要认证:** ✅

---

### 标签接口

#### 获取热门标签
```
GET /api/tag/hot?limit=30
```

---

#### 获取所有标签
```
GET /api/tag/list
```

---

#### 获取文章的标签
```
GET /api/tag/article/{articleId}
```

---

### 学院接口

#### 获取学院列表
```
GET /api/college/list
```

---

#### 创建学院 (管理员)
```
POST /api/college/create
```
**需要认证:** ✅ (ADMIN)

**请求体:**
```json
{
  "name": "计算机学院",
  "code": "CS"
}
```

---

#### 更新学院 (管理员)
```
PUT /api/college/update
```
**需要认证:** ✅ (ADMIN)

---

#### 删除学院 (管理员)
```
DELETE /api/college/delete/{id}
```
**需要认证:** ✅ (ADMIN)

---

### 文件上传接口

#### 上传图片
```
POST /api/file/upload
Content-Type: multipart/form-data
```

**请求参数:**
| 参数 | 类型 | 说明 |
|------|------|------|
| file | File | 图片文件，最大5MB |

**响应:**
```json
{
  "code": 200,
  "data": "/api/file/image/2025-12-11/uuid.jpg"
}
```

---

#### 获取图片
```
GET /api/file/image/{date}/{filename}
```

---

### 智能推荐接口

#### 获取个性化推荐
```
GET /api/recommendation/personalized?count=10&excludeIds=1,2,3
```

**查询参数:**
| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| count | int | 10 | 推荐数量 |
| excludeIds | List | - | 排除的文章ID |

---

#### 获取相似文章
```
GET /api/recommendation/similar/{articleId}?count=6
```

---

#### 获取热门推荐
```
GET /api/recommendation/hot?count=10
```

---

#### 推荐服务状态
```
GET /api/recommendation/health
```

**响应:**
```json
{
  "code": 200,
  "data": {
    "status": "healthy",
    "fallback": false
  }
}
```

---

#### 重新训练模型 (管理员)
```
POST /api/recommendation/retrain
```
**需要认证:** ✅ (ADMIN)

---

### AI助手接口

#### AI聊天
```
POST /api/ai/chat
```

**请求体:**
```json
{
  "question": "string",    // 必填，问题内容
  "sessionId": "string",   // 会话ID(可选)
  "model": "kimi"          // 模型选择: kimi/deepseek
}
```

**响应:**
```json
{
  "code": 200,
  "data": {
    "answer": "AI回复内容",
    "sessionId": "xxx"
  }
}
```

---

#### AI流式聊天 (SSE)
```
POST /api/ai/chat/stream
Content-Type: application/json
Accept: text/event-stream
```

**请求体:** 同上

**响应:** Server-Sent Events 流

---

#### 获取对话历史
```
GET /api/ai/history/{sessionId}
```

---

#### AI服务健康检查
```
GET /api/ai/health
```

---

### AI增强功能接口

#### 生成文章摘要
```
POST /api/ai/enhanced/summary
```

**请求体:**
```json
{
  "content": "文章内容",
  "length": 200,           // 摘要长度
  "style": "professional"  // 风格: professional/casual
}
```

---

#### 文本情感分析
```
POST /api/ai/enhanced/sentiment
```

**请求体:**
```json
{
  "text": "待分析文本"
}
```

**响应:**
```json
{
  "code": 200,
  "data": {
    "sentiment": "positive",
    "score": 85,
    "keywords": ["关键词1", "关键词2"]
  }
}
```

---

#### 批量生成摘要
```
POST /api/ai/enhanced/summary/batch
```

**请求体:**
```json
{
  "articles": ["文章1内容", "文章2内容"],
  "length": 200
}
```

---

#### 生成标题建议
```
POST /api/ai/enhanced/titles
```

**请求体:**
```json
{
  "content": "文章内容",
  "count": 3
}
```

---

#### 文章质量评估
```
POST /api/ai/enhanced/quality
```

**请求体:**
```json
{
  "content": "文章内容"
}
```

**响应:**
```json
{
  "code": 200,
  "data": {
    "overallScore": 4.2,
    "readability": 4.5,
    "emotionalImpact": 85,
    "contentDensity": 4.0,
    "suggestions": ["建议1", "建议2"]
  }
}
```

---

### AI Agent接口

#### 执行Agent任务
```
POST /api/ai/agent/execute
```

**请求体:**
```json
{
  "message": "帮我搜索关于校园活动的新闻",
  "sessionId": "xxx"  // 可选，用于上下文
}
```

---

#### 流式执行Agent任务 (SSE)
```
POST /api/ai/agent/execute/stream
Accept: text/event-stream
```

**SSE事件类型:**
- `start`: 任务开始
- `step`: 执行步骤
- `result`: 执行结果
- `error`: 错误信息
- `complete`: 任务完成

---

#### 获取可用工具
```
GET /api/ai/agent/tools
```

---

#### 清除会话
```
DELETE /api/ai/agent/session/{sessionId}
```

---

#### 获取Agent能力
```
GET /api/ai/agent/capabilities
```

---

### 聊天历史接口

#### 获取会话列表
```
GET /api/ai/history/sessions
```
**需要认证:** ✅

---

#### 获取会话详情
```
GET /api/ai/history/sessions/{sessionId}
```
**需要认证:** ✅

---

#### 创建新会话
```
POST /api/ai/history/sessions
```
**需要认证:** ✅

**请求体:**
```json
{
  "model": "kimi",
  "firstMessage": "新对话"
}
```

---

#### 保存消息
```
POST /api/ai/history/sessions/{sessionId}/messages
```
**需要认证:** ✅

**请求体:**
```json
{
  "role": "user",      // user/assistant
  "content": "消息内容"
}
```

---

#### 删除会话
```
DELETE /api/ai/history/sessions/{sessionId}
```
**需要认证:** ✅

---

#### 更新会话标题
```
PUT /api/ai/history/sessions/{sessionId}/title
```
**需要认证:** ✅

**请求体:**
```json
{
  "title": "新标题"
}
```

---

### 管理后台接口

#### 获取系统统计数据
```
GET /api/admin/statistics
```

**响应:**
```json
{
  "code": 200,
  "data": {
    "userCount": 100,
    "articleCount": 500,
    "pendingCount": 10,
    "commentCount": 1000,
    "collegeCount": 20,
    "officialCount": 50,
    "campusCount": 300,
    "collegeArticleCount": 150,
    "totalViews": 50000
  }
}
```

---

#### 获取图表数据
```
GET /api/admin/chart-data
```

**响应:**
```json
{
  "code": 200,
  "data": {
    "categoryData": [
      {"name": "官方新闻", "value": 50},
      {"name": "全校新闻", "value": 300},
      {"name": "学院新闻", "value": 150}
    ],
    "trendDates": ["12-05", "12-06", "12-07", "12-08", "12-09", "12-10", "12-11"],
    "trendCounts": [10, 15, 8, 20, 12, 18, 25],
    "hotTitles": ["文章1", "文章2", "文章3", "文章4", "文章5"],
    "hotViews": [1000, 800, 600, 500, 400],
    "statusData": [
      {"name": "已通过", "value": 450},
      {"name": "待审核", "value": 30},
      {"name": "已拒绝", "value": 20}
    ]
  }
}
```

---

## 算法服务 API (FastAPI)

基础路径: `http://localhost:5000`

### 健康检查

#### 服务状态
```
GET /
```

**响应:**
```json
{
  "status": "ok",
  "service": "recommendation-service"
}
```

---

#### 详细健康检查
```
GET /health
```

**响应:**
```json
{
  "status": "healthy",
  "recommender_ready": true,
  "last_train_time": 1702300000
}
```

---

### 推荐接口

#### 获取个性化推荐 (POST)
```
POST /api/recommend
```

**请求体:**
```json
{
  "user_id": 1,           // 可选，未登录传null
  "top_n": 10,            // 推荐数量
  "exclude_ids": [1, 2]   // 排除的文章ID
}
```

**响应:**
```json
{
  "success": true,
  "data": [
    {
      "article_id": 10,
      "score": 0.95,
      "reason": "基于您的阅读偏好推荐"
    }
  ],
  "message": "成功获取10条推荐"
}
```

---

#### 获取热门推荐
```
GET /api/recommend/hot?top_n=10
```

---

#### 获取用户推荐 (GET)
```
GET /api/recommend/user/{user_id}?top_n=10&exclude=1,2,3
```

---

#### 获取相似文章 (POST)
```
POST /api/similar
```

**请求体:**
```json
{
  "article_id": 1,
  "top_n": 10
}
```

---

#### 获取相似文章 (GET)
```
GET /api/similar/{article_id}?top_n=10
```

---

### 用户画像接口

#### 获取用户画像
```
GET /api/profile/{user_id}
```

**响应:**
```json
{
  "success": true,
  "data": {
    "interest_tags": [
      {"tag": "科技", "weight": 0.8},
      {"tag": "教育", "weight": 0.6}
    ],
    "category_preference": {
      "CAMPUS": 0.5,
      "OFFICIAL": 0.3,
      "COLLEGE": 0.2
    },
    "activity_pattern": {
      "peak_hours": [10, 14, 20],
      "active_days": ["周一", "周三", "周五"]
    },
    "behavior_stats": {
      "total_reads": 100,
      "total_likes": 20,
      "total_comments": 10
    },
    "reading_level": "深度阅读",
    "user_type": "活跃用户"
  },
  "message": "用户画像分析完成"
}
```

---

#### 获取用户兴趣标签
```
GET /api/profile/{user_id}/interests?top_n=10
```

---

#### 获取用户活跃时间模式
```
GET /api/profile/{user_id}/activity
```

---

#### 获取相似用户
```
GET /api/profile/{user_id}/similar-users?top_n=5
```

**响应:**
```json
{
  "success": true,
  "data": [
    {
      "user_id": 5,
      "similarity": 0.85,
      "common_interests": ["科技", "教育"]
    }
  ],
  "message": "找到5个相似用户"
}
```

---

### 管理接口

#### 重新训练模型
```
POST /api/retrain
```

**响应:**
```json
{
  "success": true,
  "message": "模型重新训练完成"
}
```

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证/Token无效 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 503 | 服务不可用 |

---

## WebSocket 接口

### 实时通知
```
ws://localhost:8080/ws/notification?token=xxx
```

**消息格式:**
```json
{
  "type": "notification",
  "data": {
    "id": 1,
    "type": "COMMENT",
    "content": "xxx评论了你的文章",
    "isRead": false,
    "createdAt": "2025-01-01T00:00:00"
  }
}
```

---

## 前端调用示例

### Axios 配置
```javascript
import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器
request.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      // 跳转登录
    }
    return Promise.reject(error)
  }
)

export default request
```

### 调用推荐服务
```javascript
// 通过后端代理调用
const recommendations = await request.get('/recommendation/personalized', {
  params: { count: 10 }
})

// 直接调用算法服务
const response = await fetch('http://localhost:5000/api/recommend', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ user_id: 1, top_n: 10 })
})
```
